package com.xerotrust.vitaltrack.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.activities.WaterAlarmRingActivity;
import com.xerotrust.vitaltrack.receivers.WaterAlarmReceiver;

public class WaterAlarmService extends Service {

    public static final String ACTION_START     = "com.technetia.healthapp.WATER_ALARM_START";
    public static final String ACTION_STOP      = "com.technetia.healthapp.WATER_ALARM_STOP";
    public static final String ACTION_SNOOZE_10 = "com.technetia.healthapp.WATER_ALARM_SNOOZE_10";
    public static final String ACTION_SNOOZE_30 = "com.technetia.healthapp.WATER_ALARM_SNOOZE_30";

    public static final String EXTRA_ALARM_ID  = "alarm_id";
    public static final String EXTRA_LABEL     = "label";
    public static final String EXTRA_ALERT_TYPE = "alert_type"; // "ring" or "vibrate"
    /** Pass true when starting from a snooze — suppresses re-launching the ring UI */
    public static final String EXTRA_IS_SNOOZE = "is_snooze";

    private static final String FG_CHANNEL_ID   = "water_alarm_fg";
    private static final String FG_CHANNEL_NAME = "Water Alarm (Foreground)";
    private static final int    FG_NOTIF_ID     = 9902;

    // Single-shot vibration patterns (no loop), max ~2.5 sec total on-time
    private static final long[] VIBRATION_ONCE = {0, 700, 200, 700, 200, 500}; // 2.1s on

    private MediaPlayer player;
    private Vibrator    vibrator;

    private final Handler  handler  = new Handler(Looper.getMainLooper());
    private final Runnable autoStop = this::stopNow;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        final String action = intent.getAction();

        if (ACTION_STOP.equals(action)) {
            stopNow();
            return START_NOT_STICKY;
        }

        if (ACTION_SNOOZE_10.equals(action) || ACTION_SNOOZE_30.equals(action)) {
            int alarmId   = intent.getIntExtra(EXTRA_ALARM_ID, -1);
            String alertType = intent.getStringExtra(EXTRA_ALERT_TYPE);
            int minutes   = ACTION_SNOOZE_30.equals(action) ? 30 : 10;

            stopNow();

            if (alarmId >= 0) {
                WaterAlarmReceiver.scheduleSnooze(this, alarmId, alertType, minutes);
            }
            return START_NOT_STICKY;
        }

        // ACTION_START
        int    alarmId   = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        String label     = intent.getStringExtra(EXTRA_LABEL);
        String alertType = intent.getStringExtra(EXTRA_ALERT_TYPE);
        boolean isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false);
        if (label == null || label.isEmpty()) label = "Time to drink water! \uD83D\uDCA7";
        if (alertType == null) alertType = "ring";

        createChannelIfNeeded();
        startForeground(FG_NOTIF_ID, buildFgNotification(alarmId, label, alertType));

        // Only launch full-screen ring UI on original fire (not snooze re-fire)
        if (!isSnooze) {
            launchRingActivity(alarmId, label, alertType);
        }

        // Vibrate ONCE — max 2-3 sec, NO loop
        startVibrationOnce();

        // Sound for "ring" mode
        if (!"vibrate".equalsIgnoreCase(alertType)) {
            startSoundLoop();
        }

        // Auto-stop after 60s
        handler.removeCallbacks(autoStop);
        handler.postDelayed(autoStop, 60_000);

        return START_STICKY;
    }

    // ── Launch ring UI ────────────────────────────────────────────────────

    private void launchRingActivity(int alarmId, String label, String alertType) {
        Intent activityIntent = new Intent(this, WaterAlarmRingActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activityIntent.putExtra(WaterAlarmRingActivity.EXTRA_ALARM_ID,   alarmId);
        activityIntent.putExtra(WaterAlarmRingActivity.EXTRA_LABEL,      label);
        activityIntent.putExtra(WaterAlarmRingActivity.EXTRA_ALERT_TYPE, alertType);
        startActivity(activityIntent);
    }

    // ── Notification with 3 action buttons ────────────────────────────────

    private Notification buildFgNotification(int alarmId, String label, String alertType) {
        Intent tapIntent = new Intent(this, WaterAlarmRingActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        tapIntent.putExtra(WaterAlarmRingActivity.EXTRA_ALARM_ID,   alarmId);
        tapIntent.putExtra(WaterAlarmRingActivity.EXTRA_LABEL,      label);
        tapIntent.putExtra(WaterAlarmRingActivity.EXTRA_ALERT_TYPE, alertType);

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentPi = PendingIntent.getActivity(this, alarmId, tapIntent, piFlags);

        // Snooze 10m
        Intent s10 = new Intent(this, WaterAlarmService.class);
        s10.setAction(ACTION_SNOOZE_10);
        s10.putExtra(EXTRA_ALARM_ID,   alarmId);
        s10.putExtra(EXTRA_ALERT_TYPE, alertType);
        PendingIntent pi10 = PendingIntent.getService(this, FG_NOTIF_ID + 10, s10, piFlags);

        // Snooze 30m
        Intent s30 = new Intent(this, WaterAlarmService.class);
        s30.setAction(ACTION_SNOOZE_30);
        s30.putExtra(EXTRA_ALARM_ID,   alarmId);
        s30.putExtra(EXTRA_ALERT_TYPE, alertType);
        PendingIntent pi30 = PendingIntent.getService(this, FG_NOTIF_ID + 30, s30, piFlags);

        // Stop
        Intent sStop = new Intent(this, WaterAlarmService.class);
        sStop.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, FG_NOTIF_ID + 1, sStop, piFlags);

        return new NotificationCompat.Builder(this, FG_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_drop)
                .setContentTitle("\uD83D\uDCA7 Water Reminder")
                .setContentText(label)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(label))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setFullScreenIntent(contentPi, true)
                .setContentIntent(contentPi)
                .addAction(R.drawable.ic_bell_small, "Snooze 10m", pi10)
                .addAction(R.drawable.ic_bell_small, "Snooze 30m", pi30)
                .addAction(R.drawable.ic_delete,     "Stop",       piStop)
                .build();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(FG_CHANNEL_ID) != null) return;

        NotificationChannel ch = new NotificationChannel(
                FG_CHANNEL_ID, FG_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    // ── Sound ─────────────────────────────────────────────────────────────

    private void startSoundLoop() {
        stopSound();
        Uri[] candidates = {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        };
        for (Uri uri : candidates) {
            if (uri == null) continue;
            try {
                player = new MediaPlayer();
                player.setDataSource(this, uri);
                AudioAttributes attrs = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                player.setAudioAttributes(attrs);
                player.setLooping(true);
                player.prepare();
                player.start();
                return;
            } catch (Exception e) { stopSound(); }
        }
    }

    private void stopSound() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.reset(); player.release();
            }
        } catch (Exception ignored) { } finally { player = null; }
    }

    // ── Vibration — single burst, max ~2-3 sec, NO loop ───────────────────

    private void startVibrationOnce() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) vibrator = vm.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator()) return;

        // repeat = -1 → single shot, no loop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION_ONCE, -1));
        } else {
            vibrator.vibrate(VIBRATION_ONCE, -1);
        }
    }

    private void stopVibration() {
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) { }
    }

    // ── stopNow: stops sound/vibration; notification stays until user taps ─

    private void stopNow() {
        handler.removeCallbacks(autoStop);
        stopVibration();
        stopSound();
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() { stopNow(); super.onDestroy(); }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
