package com.xerotrust.vitaltrack.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xerotrust.vitaltrack.models.MedicineReminder;
import com.xerotrust.vitaltrack.models.WaterAlarm;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;

import java.util.Calendar;
import java.util.List;

public class BootReceiver extends BroadcastReceiver {

    private static final int WATER_REQUEST_BASE = 5000; // must match WaterIntakeAlarmActivity.REQUEST_BASE

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        DatabaseHelper db = new DatabaseHelper(context);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // ── 1. Reschedule Medicine alarms ────────────────────────────────────
        List<MedicineReminder> medicines = db.getAllMedicines();
        for (MedicineReminder med : medicines) {
            if (!med.isEnabled()) continue;

            Intent alarmIntent = new Intent(context, AlarmReceiver.class);
            alarmIntent.putExtra("medicine_name", med.getName());
            alarmIntent.putExtra("dosage", med.getDosage());

            PendingIntent pi = PendingIntent.getBroadcast(context, med.getId(),
                    alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, med.getHour());
            cal.set(Calendar.MINUTE, med.getMinute());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        }

        // ── 2. Reschedule Water alarms ───────────────────────────────────────
        List<WaterAlarm> waterAlarms = db.getAllWaterAlarms();
        for (WaterAlarm alarm : waterAlarms) {
            if (!alarm.isEnabled()) continue;

            Intent waterIntent = new Intent(context, WaterAlarmReceiver.class);
            waterIntent.putExtra("alarm_id",   alarm.getId());
            waterIntent.putExtra("alert_type", alarm.getAlertType());

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    WATER_REQUEST_BASE + alarm.getId(),
                    waterIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
            cal.set(Calendar.MINUTE, alarm.getMinute());
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
        }

        db.close();
    }
}
