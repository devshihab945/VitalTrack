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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.receivers.AlarmReceiver;
import com.xerotrust.vitaltrack.utils.MedicineScheduleUtils;

public class MedicineAlarmService extends Service {

    public static final String ACTION_START     = "com.technetia.healthapp.ALARM_START";
    public static final String ACTION_STOP      = "com.technetia.healthapp.ALARM_STOP";
    public static final String ACTION_SNOOZE_10 = "com.technetia.healthapp.ALARM_SNOOZE_10";
    public static final String ACTION_SNOOZE_30 = "com.technetia.healthapp.ALARM_SNOOZE_30";

    public static final String EXTRA_NAME    = "medicine_name";
    public static final String EXTRA_DOSAGE  = "dosage";
    public static final String EXTRA_MODE    = "vibration_mode";
    public static final String EXTRA_PLAN_ID = MedicineScheduleUtils.EXTRA_PLAN_ID;
    public static final String EXTRA_TIME_ID = MedicineScheduleUtils.EXTRA_TIME_ID;
    public static final String EXTRA_MED_ID  = "med_id";

    private static final String FG_CHANNEL_ID = "medicine_alarm_fg";
    private static final int    FG_ID         = 9901;

    // Single-shot vibration patterns (no loop), total on-time = 1s
    private static final long[] VIBRATION_NORMAL = {0, 1000};               // 1s total
    private static final long[] VIBRATION_ALARM  = {0, 400, 200, 600};      // 1s total

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
            int planId  = intent.getIntExtra(EXTRA_PLAN_ID, 0);
            int timeId  = intent.getIntExtra(EXTRA_TIME_ID, 0);
            int medId   = intent.getIntExtra(EXTRA_MED_ID,  0);
            String name   = intent.getStringExtra(EXTRA_NAME);
            String dosage = intent.getStringExtra(EXTRA_DOSAGE);
            String mode   = intent.getStringExtra(EXTRA_MODE);
            int minutes   = ACTION_SNOOZE_30.equals(action) ? 30 : 10;

            stopNow();

            if (planId != 0 && timeId != 0) {
                AlarmReceiver.scheduleSnoozePlan(this, planId, timeId, minutes);
            } else if (medId != 0) {
                AlarmReceiver.scheduleSnooze(this, medId, name, dosage, mode, minutes);
            }
            return START_NOT_STICKY;
        }

        // ACTION_START
        String name   = intent.getStringExtra(EXTRA_NAME);
        String dosage = intent.getStringExtra(EXTRA_DOSAGE);
        String mode   = intent.getStringExtra(EXTRA_MODE);
        int planId    = intent.getIntExtra(EXTRA_PLAN_ID, 0);
        int timeId    = intent.getIntExtra(EXTRA_TIME_ID, 0);
        int medId     = intent.getIntExtra(EXTRA_MED_ID, 0);
        if (mode == null) mode = "normal";

        createChannelIfNeeded();
        startForeground(FG_ID, buildFgNotification(name, dosage, mode, planId, timeId, medId));

        // Vibrate ONCE — max 2-3 sec, NO loop (repeat = -1)
        startVibrationOnce(mode);

        // Sound only for "alarm" mode
        if ("alarm".equals(mode)) {
            startSoundLoopAlarm();
        }

        // Auto-stop sound/vibration after 60s; notification stays until user acts
        handler.removeCallbacks(autoStop);
        handler.postDelayed(autoStop, 60_000);

        return START_STICKY;
    }

    private Notification buildFgNotification(String name, String dosage, String mode,
                                              int planId, int timeId, int medId) {
        String body = (dosage != null && !dosage.isEmpty())
                ? "Reminder: " + name + " — " + dosage
                : "Reminder: " + name;

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;

        // Snooze 10m
        Intent s10 = new Intent(this, MedicineAlarmService.class);
        s10.setAction(ACTION_SNOOZE_10);
        s10.putExtra(EXTRA_PLAN_ID, planId); s10.putExtra(EXTRA_TIME_ID, timeId);
        s10.putExtra(EXTRA_MED_ID, medId);   s10.putExtra(EXTRA_NAME, name);
        s10.putExtra(EXTRA_DOSAGE, dosage);  s10.putExtra(EXTRA_MODE, mode);
        PendingIntent pi10 = PendingIntent.getService(this, FG_ID + 10, s10, piFlags);

        // Snooze 30m
        Intent s30 = new Intent(this, MedicineAlarmService.class);
        s30.setAction(ACTION_SNOOZE_30);
        s30.putExtra(EXTRA_PLAN_ID, planId); s30.putExtra(EXTRA_TIME_ID, timeId);
        s30.putExtra(EXTRA_MED_ID, medId);   s30.putExtra(EXTRA_NAME, name);
        s30.putExtra(EXTRA_DOSAGE, dosage);  s30.putExtra(EXTRA_MODE, mode);
        PendingIntent pi30 = PendingIntent.getService(this, FG_ID + 30, s30, piFlags);

        // Stop
        Intent sStop = new Intent(this, MedicineAlarmService.class);
        sStop.setAction(ACTION_STOP);
        PendingIntent piStop = PendingIntent.getService(this, FG_ID + 1, sStop, piFlags);

        return new NotificationCompat.Builder(this, FG_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle("Medicine Reminder")
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .addAction(R.drawable.ic_bell_small, "Snooze 10m", pi10)
                .addAction(R.drawable.ic_bell_small, "Snooze 30m", pi30)
                .addAction(R.drawable.ic_delete,     "Stop",       piStop)
                .build();
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(FG_CHANNEL_ID) == null) {
            NotificationChannel ch = new NotificationChannel(
                    FG_CHANNEL_ID, "Medicine Alarm (Foreground)", NotificationManager.IMPORTANCE_HIGH);
            ch.setSound(null, null);
            ch.enableVibration(false);
            nm.createNotificationChannel(ch);
        }
    }

    private void startSoundLoopAlarm() {
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

    /** Single-shot vibration — repeat=-1 means NO loop */
    private void startVibrationOnce(String mode) {
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = "alarm".equals(mode) ? VIBRATION_ALARM : VIBRATION_NORMAL;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        } else {
            vibrator.vibrate(pattern, -1);
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

    private void stopVibration() {
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) { }
    }

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
