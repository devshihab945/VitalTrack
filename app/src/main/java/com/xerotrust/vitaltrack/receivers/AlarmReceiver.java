package com.xerotrust.vitaltrack.receivers;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.activities.AlarmActivity;
import com.xerotrust.vitaltrack.activities.MainActivity;
import com.xerotrust.vitaltrack.models.MedicinePlan;
import com.xerotrust.vitaltrack.models.MedicineScheduleItem;
import com.xerotrust.vitaltrack.models.MedicineTime;
import com.xerotrust.vitaltrack.services.MedicineAlarmService;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.MedicineScheduleUtils;

import java.util.List;
import java.util.Locale;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID_NORMAL = "medicine_reminder_normal";
    private static final String CHANNEL_ID_ALARM = "medicine_reminder_alarm";

    /** Extra flag: true when this MAIN fire is a snooze re-fire (not the original schedule) */
    public static final String EXTRA_IS_SNOOZE = "is_snooze";

    @Override
    public void onReceive(Context context, Intent intent) {

        // =========
        // Legacy snooze/old alarms support (optional)
        // =========
        // If someone triggers old style alarm with "medicine_name" extras, we still open AlarmActivity.
        if (intent != null && intent.hasExtra("medicine_name") && !intent.hasExtra(MedicineScheduleUtils.EXTRA_PLAN_ID)) {
            handleLegacy(context, intent);
            return;
        }

        // =========
        // Plan based flow
        // =========
        String type = intent.getStringExtra(MedicineScheduleUtils.EXTRA_TYPE);
        if (type == null) type = MedicineScheduleUtils.TYPE_MAIN;

        int planId = intent.getIntExtra(MedicineScheduleUtils.EXTRA_PLAN_ID, 0);
        int timeId = intent.getIntExtra(MedicineScheduleUtils.EXTRA_TIME_ID, 0);

        DatabaseHelper db = new DatabaseHelper(context);
        MedicinePlan plan = db.getPlanFull(planId);
        if (plan == null || !plan.isEnabled()) return;

        MedicineTime firedTime = null;
        for (MedicineTime t : plan.getTimes()) {
            if (t.getId() == timeId) {
                firedTime = t;
                break;
            }
        }
        if (firedTime == null) return;

        // PRE reminders: notification only
        if (!MedicineScheduleUtils.TYPE_MAIN.equals(type)) {
            createChannels(context);

            String whenText = MedicineScheduleUtils.TYPE_PRE_10.equals(type) ? "in 10 minutes" : "in 1 hour";
            String detail = buildDoseText(plan, firedTime, whenText);

            showPreReminderNotification(context, plan.getVibrationMode(), planId, timeId, plan.getName(), detail);
            // pre reminders get recreated when MAIN reschedules next occurrence
            return;
        }

        // MAIN: start service + open AlarmActivity full screen
        String detail = buildDoseText(plan, firedTime, null);

        Intent s = new Intent(context, MedicineAlarmService.class);
        s.setAction(MedicineAlarmService.ACTION_START);
        s.putExtra(MedicineAlarmService.EXTRA_NAME, plan.getName());
        s.putExtra(MedicineAlarmService.EXTRA_DOSAGE, detail);
        s.putExtra(MedicineAlarmService.EXTRA_MODE, plan.getVibrationMode());
        // Pass IDs so notification snooze actions work without the AlarmActivity open
        s.putExtra(MedicineAlarmService.EXTRA_PLAN_ID, planId);
        s.putExtra(MedicineAlarmService.EXTRA_TIME_ID, timeId);
        s.putExtra(MedicineAlarmService.EXTRA_MED_ID, planId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(s);
        else context.startService(s);

        Intent fs = new Intent(context, AlarmActivity.class);
        fs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fs.putExtra("medicine_name", plan.getName());
        fs.putExtra("dosage", detail);
        fs.putExtra("vibration_mode", plan.getVibrationMode());

        // IMPORTANT: send plan ids for plan-based snooze
        fs.putExtra(MedicineScheduleUtils.EXTRA_PLAN_ID, planId);
        fs.putExtra(MedicineScheduleUtils.EXTRA_TIME_ID, timeId);

        // legacy field kept (some screens use it)
        fs.putExtra("med_id", planId);

        context.startActivity(fs);

        // Reschedule next occurrence only for the original (non-snooze) fire.
        // If this is a snooze re-fire, next occurrence was already scheduled when
        // the original alarm fired — scheduling it again would double-book it.
        boolean isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false);
        if (!isSnooze) {
            MedicineScheduleUtils.rescheduleAfterFired(context, db, planId, timeId);
        }
    }

    // =========================
    // Build detail: "After Breakfast • 500mg — 07:00"
    // =========================
    private String buildDoseText(MedicinePlan plan, MedicineTime time, String whenText) {
        String label = inferLabelFromTime(time.getHour());
        String dosage = findDosageForLabel(plan.getScheduleItems(), label);

        if (dosage == null || dosage.trim().isEmpty()) dosage = "Dose";

        String base = String.format(Locale.getDefault(), "%s • %s — %s", label, dosage, time.hhmm());

        if (whenText != null) return base + " (" + whenText + ")";
        return base;
    }

    // Option B mapping
    private String inferLabelFromTime(int hour24) {
        if (hour24 >= 4 && hour24 <= 11) return "After Breakfast";
        if (hour24 >= 12 && hour24 <= 16) return "After Lunch";
        if (hour24 >= 17 && hour24 <= 23) return "After Dinner";
        return "Dose";
    }

    private String findDosageForLabel(List<MedicineScheduleItem> items, String label) {
        if (items == null || items.isEmpty()) return "";
        for (MedicineScheduleItem it : items) {
            if (it.getLabel() != null && it.getLabel().equalsIgnoreCase(label)) {
                return it.getDosage();
            }
        }
        return items.get(0).getDosage();
    }

    // =========================
    // Pre-reminder Notification
    // =========================
    private void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;

        Uri notifSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes notifAttr = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

        if (nm.getNotificationChannel(CHANNEL_ID_NORMAL) == null) {
            NotificationChannel normal = new NotificationChannel(CHANNEL_ID_NORMAL, "Medicine Reminder", NotificationManager.IMPORTANCE_HIGH);
            normal.setSound(notifSound, notifAttr);
            normal.enableVibration(true);
            normal.setVibrationPattern(new long[]{0, 1000});
            nm.createNotificationChannel(normal);
        }

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes alarmAttr = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ALARM).build();

        if (nm.getNotificationChannel(CHANNEL_ID_ALARM) == null) {
            NotificationChannel alarm = new NotificationChannel(CHANNEL_ID_ALARM, "Medicine Pre-reminder (Alarm)", NotificationManager.IMPORTANCE_HIGH);
            alarm.setSound(alarmSound, alarmAttr);
            alarm.enableVibration(true);
            alarm.setVibrationPattern(new long[]{0, 400, 200, 600});
            nm.createNotificationChannel(alarm);
        }
    }

    private void showPreReminderNotification(Context context, String vibrationMode, int planId, int timeId, String titleName, String body) {

        boolean isAlarmMode = "alarm".equalsIgnoreCase(vibrationMode);
        String channelId = isAlarmMode ? CHANNEL_ID_ALARM : CHANNEL_ID_NORMAL;

        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        tapIntent.putExtra("open_medicine", true);

        int notifId = (planId * 10000) + (timeId % 10000) + 777;

        PendingIntent pi = PendingIntent.getActivity(context, notifId, tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_pill).setContentTitle("Upcoming: " + titleName).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pi);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(notifId, b.build());
    }

    // =========================
    // Snooze Support (plan-based + legacy)
    // =========================

    /**
     * Plan-based snooze: re-fire MAIN after X minutes
     */
    public static void scheduleSnoozePlan(Context context, int planId, int timeId, int minutes) {
        long triggerAt = System.currentTimeMillis() + (minutes * 60_000L);

        Intent i = new Intent(context, AlarmReceiver.class);
        i.putExtra(MedicineScheduleUtils.EXTRA_TYPE, MedicineScheduleUtils.TYPE_MAIN);
        i.putExtra(MedicineScheduleUtils.EXTRA_PLAN_ID, planId);
        i.putExtra(MedicineScheduleUtils.EXTRA_TIME_ID, timeId);
        i.putExtra(EXTRA_IS_SNOOZE, true);  // tells receiver NOT to reschedule next occurrence

        int rc = 7000000 + (planId * 10000) + (timeId % 10000);

        PendingIntent pi = PendingIntent.getBroadcast(context, rc, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    /**
     * Legacy snooze support (for old med_id based alarms)
     */
    public static void scheduleSnooze(Context context, int medId, String name, String dosage, String mode, int minutes) {

        long triggerAt = System.currentTimeMillis() + (minutes * 60_000L);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("type", "MAIN");
        intent.putExtra("medicine_name", name);
        intent.putExtra("dosage", dosage);
        intent.putExtra("vibration_mode", mode);
        intent.putExtra("med_id", medId);
        intent.putExtra("notif_id", 900000 + medId);

        PendingIntent pi = PendingIntent.getBroadcast(context, 900000 + medId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private void handleLegacy(Context context, Intent intent) {
        // Minimal legacy handler: open AlarmActivity + start service
        String name = intent.getStringExtra("medicine_name");
        String dosage = intent.getStringExtra("dosage");
        String mode = intent.getStringExtra("vibration_mode");
        if (mode == null) mode = "normal";

        Intent s = new Intent(context, MedicineAlarmService.class);
        s.setAction(MedicineAlarmService.ACTION_START);
        s.putExtra(MedicineAlarmService.EXTRA_NAME, name);
        s.putExtra(MedicineAlarmService.EXTRA_DOSAGE, dosage);
        s.putExtra(MedicineAlarmService.EXTRA_MODE, mode);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(s);
        else context.startService(s);

        Intent fs = new Intent(context, AlarmActivity.class);
        fs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        fs.putExtra("medicine_name", name);
        fs.putExtra("dosage", dosage);
        fs.putExtra("vibration_mode", mode);
        fs.putExtra("med_id", intent.getIntExtra("med_id", 0));
        context.startActivity(fs);
    }
}