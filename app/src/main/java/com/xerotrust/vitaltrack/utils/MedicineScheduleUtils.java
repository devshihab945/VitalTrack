package com.xerotrust.vitaltrack.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.xerotrust.vitaltrack.models.MedicinePlan;
import com.xerotrust.vitaltrack.models.MedicineTime;
import com.xerotrust.vitaltrack.receivers.AlarmReceiver;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MedicineScheduleUtils {

    public static final String EXTRA_TYPE = "type"; // MAIN, PRE_10, PRE_60
    public static final String EXTRA_PLAN_ID = "plan_id";
    public static final String EXTRA_TIME_ID = "time_id";
    public static final String EXTRA_VIBRATION = "vibration_mode";

    public static final String TYPE_MAIN = "MAIN";
    public static final String TYPE_PRE_10 = "PRE_10";
    public static final String TYPE_PRE_60 = "PRE_60";

    // request code offsets to keep unique PI per plan/time/type
    private static final int OFF_MAIN = 0;
    private static final int OFF_PRE10 = 1000000;
    private static final int OFF_PRE60 = 2000000;

    private static int reqCode(int planId, int timeId, int off) {
        // keep within int range; stable unique
        return (planId * 10000) + (timeId % 10000) + off;
    }

    public static void scheduleAllForPlan(Context ctx, DatabaseHelper db, int planId) {
        MedicinePlan plan = db.getPlanFull(planId);
        if (plan == null || !plan.isEnabled()) return;

        for (MedicineTime t : plan.getTimes()) {
            scheduleForPlanTime(ctx, db, plan, t);
        }
    }

    public static void cancelAllForPlan(Context ctx, DatabaseHelper db, int planId) {
        MedicinePlan plan = db.getPlanFull(planId);
        if (plan == null) return;

        for (MedicineTime t : plan.getTimes()) {
            cancelOne(ctx, plan.getId(), t.getId(), OFF_MAIN);
            cancelOne(ctx, plan.getId(), t.getId(), OFF_PRE10);
            cancelOne(ctx, plan.getId(), t.getId(), OFF_PRE60);
        }
    }

    public static void rescheduleAfterFired(Context ctx, DatabaseHelper db, int planId, int timeId) {
        MedicinePlan plan = db.getPlanFull(planId);
        if (plan == null || !plan.isEnabled()) return;

        MedicineTime target = null;
        for (MedicineTime t : plan.getTimes()) {
            if (t.getId() == timeId) {
                target = t;
                break;
            }
        }
        if (target == null) return;

        scheduleForPlanTime(ctx, db, plan, target);
    }

    private static void scheduleForPlanTime(Context ctx, DatabaseHelper db, MedicinePlan plan, MedicineTime time) {
        Calendar nextMain = nextOccurrence(plan, time, Calendar.getInstance());
        if (nextMain == null) return;

        // MAIN
        scheduleExact(ctx, plan.getId(), time.getId(), OFF_MAIN, TYPE_MAIN, nextMain.getTimeInMillis(), plan.getVibrationMode());

        // PREs
        if (plan.isRemind10()) {
            Calendar pre10 = (Calendar) nextMain.clone();
            pre10.add(Calendar.MINUTE, -10);
            // if pre time is in past but main is future, don't schedule pre
            if (pre10.getTimeInMillis() > System.currentTimeMillis()) {
                scheduleExact(ctx, plan.getId(), time.getId(), OFF_PRE10, TYPE_PRE_10, pre10.getTimeInMillis(), plan.getVibrationMode());
            } else {
                cancelOne(ctx, plan.getId(), time.getId(), OFF_PRE10);
            }
        } else {
            cancelOne(ctx, plan.getId(), time.getId(), OFF_PRE10);
        }

        if (plan.isRemind60()) {
            Calendar pre60 = (Calendar) nextMain.clone();
            pre60.add(Calendar.MINUTE, -60);
            if (pre60.getTimeInMillis() > System.currentTimeMillis()) {
                scheduleExact(ctx, plan.getId(), time.getId(), OFF_PRE60, TYPE_PRE_60, pre60.getTimeInMillis(), plan.getVibrationMode());
            } else {
                cancelOne(ctx, plan.getId(), time.getId(), OFF_PRE60);
            }
        } else {
            cancelOne(ctx, plan.getId(), time.getId(), OFF_PRE60);
        }
    }

    private static void scheduleExact(Context ctx, int planId, int timeId, int off, String type, long triggerAt, String vibrationMode) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int rc = reqCode(planId, timeId, off);

        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra(EXTRA_TYPE, type);
        i.putExtra(EXTRA_PLAN_ID, planId);
        i.putExtra(EXTRA_TIME_ID, timeId);
        i.putExtra(EXTRA_VIBRATION, vibrationMode);

        PendingIntent pi = PendingIntent.getBroadcast(ctx, rc, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.cancel(pi);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    private static void cancelOne(Context ctx, int planId, int timeId, int off) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        int rc = reqCode(planId, timeId, off);
        PendingIntent pi = PendingIntent.getBroadcast(ctx, rc, new Intent(ctx, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    // =========================
    // Next occurrence calculator
    // =========================

    /**
     * Returns next valid datetime for given plan+time, strictly >= now.
     */
    public static Calendar nextOccurrence(MedicinePlan plan, MedicineTime t, Calendar now) {
        if (plan == null || t == null) return null;

        Calendar base = (Calendar) now.clone();
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);

        // start candidate at today's time
        Calendar cand = (Calendar) base.clone();
        cand.set(Calendar.HOUR_OF_DAY, t.getHour());
        cand.set(Calendar.MINUTE, t.getMinute());

        // if today's time already passed, move to next day initially
        if (cand.getTimeInMillis() <= base.getTimeInMillis()) {
            cand.add(Calendar.DAY_OF_YEAR, 1);
        }

        String ft = plan.getFrequencyType();
        String fp = plan.getFrequencyParam() == null ? "" : plan.getFrequencyParam().trim();

        if (MedicinePlan.FREQ_EVERYDAY.equals(ft)) {
            return cand;
        }

        if (MedicinePlan.FREQ_EVERY_X_DAYS.equals(ft)) {
            int x = safeInt(fp, 1);
            if (x < 1) x = 1;

            // We don't have a "start date" stored; simplest: from today forward, accept every x days based on dayKey modulo.
            // Use epoch day modulo. Stable across restarts.
            while (!matchesEveryXDays(cand, x)) {
                cand.add(Calendar.DAY_OF_YEAR, 1);
            }
            return cand;
        }

        if (MedicinePlan.FREQ_DAY_OF_WEEK.equals(ft)) {
            Set<Integer> allowed = parseWeekdays(fp);
            if (allowed.isEmpty()) return cand;

            // advance until weekday matches
            for (int i = 0; i < 14; i++) {
                int dow = cand.get(Calendar.DAY_OF_WEEK);
                if (allowed.contains(dow)) return cand;
                cand.add(Calendar.DAY_OF_YEAR, 1);
            }
            return cand;
        }

        if (MedicinePlan.FREQ_DAY_OF_MONTH.equals(ft)) {
            Set<Integer> doms = parseDom(fp);
            if (doms.isEmpty()) return cand;

            // advance until day-of-month matches (limit search 370 days)
            for (int i = 0; i < 370; i++) {
                int dom = cand.get(Calendar.DAY_OF_MONTH);
                if (doms.contains(dom)) return cand;
                cand.add(Calendar.DAY_OF_YEAR, 1);
            }
            return cand;
        }

        if (MedicinePlan.FREQ_X_ENABLE_Y_DISABLE.equals(ft)) {
            // fp = "X,Y"
            String[] parts = fp.split(",");
            int x = parts.length > 0 ? safeInt(parts[0].trim(), 1) : 1;
            int y = parts.length > 1 ? safeInt(parts[1].trim(), 0) : 0;
            if (x < 1) x = 1;
            if (y < 0) y = 0;

            while (!matchesEnableDisable(cand, x, y)) {
                cand.add(Calendar.DAY_OF_YEAR, 1);
            }
            return cand;
        }

        // fallback
        return cand;
    }

    private static int safeInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    private static boolean matchesEveryXDays(Calendar day, int x) {
        // epochDay = days since 1970-01-01
        long epochDay = day.getTimeInMillis() / 86400000L;
        return (epochDay % x) == 0;
    }

    private static boolean matchesEnableDisable(Calendar day, int xEnable, int yDisable) {
        // cycle length
        int cycle = xEnable + yDisable;
        if (cycle <= 0) return true;

        long epochDay = day.getTimeInMillis() / 86400000L;
        int pos = (int) (epochDay % cycle);
        return pos < xEnable;
    }

    private static Set<Integer> parseWeekdays(String fp) {
        // input "MON,WED,FRI"
        Set<Integer> set = new HashSet<>();
        String[] parts = fp.toUpperCase(Locale.US).split(",");
        for (String p : parts) {
            p = p.trim();
            if (p.equals("SUN")) set.add(Calendar.SUNDAY);
            if (p.equals("MON")) set.add(Calendar.MONDAY);
            if (p.equals("TUE")) set.add(Calendar.TUESDAY);
            if (p.equals("WED")) set.add(Calendar.WEDNESDAY);
            if (p.equals("THU")) set.add(Calendar.THURSDAY);
            if (p.equals("FRI")) set.add(Calendar.FRIDAY);
            if (p.equals("SAT")) set.add(Calendar.SATURDAY);
        }
        return set;
    }

    private static Set<Integer> parseDom(String fp) {
        // input "1,15,30"
        Set<Integer> set = new HashSet<>();
        String[] parts = fp.split(",");
        for (String p : parts) {
            int v = safeInt(p.trim(), -1);
            if (v >= 1 && v <= 31) set.add(v);
        }
        return set;
    }
}