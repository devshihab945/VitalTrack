package com.xerotrust.vitaltrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.xerotrust.vitaltrack.activities.WaterIntakeAlarmActivity;
import com.xerotrust.vitaltrack.models.WaterAlarm;
import com.xerotrust.vitaltrack.services.WaterAlarmService;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;

public class WaterAlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int     alarmId   = intent.getIntExtra("alarm_id", -1);
        String  alertType = intent.getStringExtra("alert_type"); // "ring" or "vibrate"
        boolean isSnooze  = intent.getBooleanExtra("is_snooze", false);

        if (alarmId < 0) return;

        // Fetch alarm from DB — verify it's still enabled
        DatabaseHelper db    = new DatabaseHelper(context);
        WaterAlarm     alarm = null;
        String         label = "Time to drink water! \uD83D\uDCA7";

        for (WaterAlarm a : db.getAllWaterAlarms()) {
            if (a.getId() == alarmId) {
                alarm = a;
                if (a.getLabel() != null && !a.getLabel().isEmpty()) label = a.getLabel();
                if (alertType == null || alertType.isEmpty()) alertType = a.getAlertType();
                break;
            }
        }
        db.close();

        if (alarm == null || !alarm.isEnabled()) return;

        // Start foreground service
        Intent serviceIntent = new Intent(context, WaterAlarmService.class);
        serviceIntent.setAction(WaterAlarmService.ACTION_START);
        serviceIntent.putExtra(WaterAlarmService.EXTRA_ALARM_ID,   alarmId);
        serviceIntent.putExtra(WaterAlarmService.EXTRA_LABEL,      label);
        serviceIntent.putExtra(WaterAlarmService.EXTRA_ALERT_TYPE, alertType != null ? alertType : "ring");
        serviceIntent.putExtra(WaterAlarmService.EXTRA_IS_SNOOZE,  isSnooze);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }

        // Only reschedule next day's alarm for real (non-snooze) triggers
        if (!isSnooze) {
            WaterIntakeAlarmActivity.scheduleAlarm(context, alarm);
        }
    }

    /**
     * Schedule a one-shot snooze alarm (called from WaterAlarmService notification action
     * or from WaterAlarmRingActivity).
     */
    public static void scheduleSnooze(Context context, int alarmId, String alertType, int minutes) {
        if (alarmId < 0) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, WaterAlarmReceiver.class);
        intent.putExtra("alarm_id",   alarmId);
        intent.putExtra("alert_type", alertType != null ? alertType : "ring");
        intent.putExtra("is_snooze",  true);
        intent.putExtra(WaterAlarmService.EXTRA_IS_SNOOZE, true);

        int rc = 8000 + alarmId + (minutes * 100); // unique request code per snooze duration
        PendingIntent pi = PendingIntent.getBroadcast(
                context, rc, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long triggerAt = System.currentTimeMillis() + (minutes * 60_000L);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}
