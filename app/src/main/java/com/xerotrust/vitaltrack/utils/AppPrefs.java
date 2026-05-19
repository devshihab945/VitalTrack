package com.xerotrust.vitaltrack.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;

public class AppPrefs {
    private static final String PREF_NAME = "HealthAppPrefs";

    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_PROFILE_DONE = "profile_done";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_HEIGHT = "user_height";
    private static final String KEY_WEIGHT = "user_weight";
    private static final String KEY_BLOOD_TYPE = "user_blood_type";
    private static final String KEY_STEP_COUNT = "step_count_today";
    private static final String KEY_WATER_GLASSES = "water_glasses_today";
    private static final String KEY_LAST_DATE = "last_date";
    private static final String KEY_BMI = "last_bmi";
    private static final String KEY_PROFILE_PIC = "profile_pic_base64";
    private static final String KEY_CALORIES_BURNED = "calories_burned";

    // Step counter core
    private static final String KEY_STEPS_DAY_KEY = "steps_day_key";
    private static final String KEY_STEP_COUNTER_BASELINE = "step_counter_baseline";

    // Walk mode
    private static final String KEY_WALK_STEPS = "walk_steps_today";
    private static final String KEY_WALK_GOAL = "walk_goal";
    private static final String KEY_WALK_ACTIVE = "walk_active";
    private static final String KEY_WALK_SESSION_START = "walk_session_start_ms";

    // Run mode
    private static final String KEY_RUN_STEPS = "run_steps_today";
    private static final String KEY_RUN_GOAL = "run_goal";
    private static final String KEY_RUN_ACTIVE = "run_active";
    private static final String KEY_RUN_SESSION_START = "run_session_start_ms";

    // Weekly history — 7 days rolling (0=oldest, 6=latest)
    // Each day stores: total, walk, run, walk_goal, run_goal, walk_km, run_km
    // Key pattern: week_<field>_<dayIndex>
    private static final String KEY_WEEK_TOTAL = "week_total_";
    private static final String KEY_WEEK_WALK = "week_walk_";
    private static final String KEY_WEEK_RUN = "week_run_";
    private static final String KEY_WEEK_WALK_GOAL = "week_walk_goal_";
    private static final String KEY_WEEK_RUN_GOAL = "week_run_goal_";
    private static final String KEY_WEEK_WALK_KM = "week_walk_km_";
    private static final String KEY_WEEK_RUN_KM = "week_run_km_";
    private static final String KEY_WEEK_WALK_TIME = "week_walk_time_";
    private static final String KEY_WEEK_RUN_TIME = "week_run_time_";
    private static final String KEY_WEEK_DATE_KEY = "week_date_key_"; // yyyyMMdd

    private final SharedPreferences prefs;

    public AppPrefs(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Profile ────────────────────────────────────────────────────────────

    public int getCaloriesBurned() { return prefs.getInt(KEY_CALORIES_BURNED, 0); }
    public void setCaloriesBurned(int cal) { prefs.edit().putInt(KEY_CALORIES_BURNED, cal).apply(); }
    public void setProfilePicBase64(String base64) { prefs.edit().putString(KEY_PROFILE_PIC, base64).apply(); }
    public String getProfilePicBase64() { return prefs.getString(KEY_PROFILE_PIC, ""); }
    public boolean isFirstLaunch() { return prefs.getBoolean(KEY_FIRST_LAUNCH, true); }
    public void setFirstLaunch(boolean val) { prefs.edit().putBoolean(KEY_FIRST_LAUNCH, val).apply(); }
    public boolean isOnboardingDone() { return prefs.getBoolean(KEY_ONBOARDING_DONE, false); }
    public void setOnboardingDone(boolean val) { prefs.edit().putBoolean(KEY_ONBOARDING_DONE, val).apply(); }
    public boolean isProfileDone() { return prefs.getBoolean(KEY_PROFILE_DONE, false); }
    public void setProfileDone(boolean val) { prefs.edit().putBoolean(KEY_PROFILE_DONE, val).apply(); }
    public String getName() { return prefs.getString(KEY_NAME, ""); }
    public void setName(String val) { prefs.edit().putString(KEY_NAME, val).apply(); }
    public int getAge() { return prefs.getInt(KEY_AGE, 0); }
    public void setAge(int val) { prefs.edit().putInt(KEY_AGE, val).apply(); }
    public float getHeight() { return prefs.getFloat(KEY_HEIGHT, 0); }
    public void setHeight(float val) { prefs.edit().putFloat(KEY_HEIGHT, val).apply(); }
    public float getWeight() { return prefs.getFloat(KEY_WEIGHT, 0); }
    public void setWeight(float val) { prefs.edit().putFloat(KEY_WEIGHT, val).apply(); }
    public String getBloodType() { return prefs.getString(KEY_BLOOD_TYPE, ""); }
    public void setBloodType(String val) { prefs.edit().putString(KEY_BLOOD_TYPE, val).apply(); }
    public int getStepCount() { return prefs.getInt(KEY_STEP_COUNT, 0); }
    public void setStepCount(int val) { prefs.edit().putInt(KEY_STEP_COUNT, val).apply(); }
    public int getWaterGlasses() { return prefs.getInt(KEY_WATER_GLASSES, 0); }
    public void setWaterGlasses(int val) { prefs.edit().putInt(KEY_WATER_GLASSES, val).apply(); }
    public String getLastDate() { return prefs.getString(KEY_LAST_DATE, ""); }
    public void setLastDate(String val) { prefs.edit().putString(KEY_LAST_DATE, val).apply(); }

    /**
     * Automatically resets daily step counters if a new calendar day has begun.
     * Uses the same stepsDayKey mechanism as StepCounterService.ensureDayBoundary().
     * Call this on fragment resume when the service is NOT running.
     * Returns true if a reset happened.
     */
    public boolean checkAndResetIfNewDay() {
        int todayKey = currentDayKey();
        int savedKey = getStepsDayKey();
        if (savedKey == todayKey) return false;

        // Push current day data to history before resetting
        int prevWalk = getModeSteps("walk");
        int prevRun  = getModeSteps("run");
        pushDayToHistory(prevWalk + prevRun, prevWalk, prevRun,
                getModeGoal("walk"), getModeGoal("run"));

        // Reset daily counters
        setModeSteps("walk", 0);
        setModeSteps("run", 0);
        setModeActive("walk", false);
        setModeActive("run", false);
        setModeSessionStartMs("walk", 0L);
        setModeSessionStartMs("run", 0L);
        setStepsDayKey(todayKey);
        setTodaySteps(0);
        return true;
    }
    public float getLastBmi() { return prefs.getFloat(KEY_BMI, 0); }
    public void setLastBmi(float val) { prefs.edit().putFloat(KEY_BMI, val).apply(); }

    // ─── Total Steps (backward compat) ──────────────────────────────────────

    public int getTodaySteps() { return prefs.getInt(KEY_STEP_COUNT, 0); }
    public void setTodaySteps(int val) { prefs.edit().putInt(KEY_STEP_COUNT, val).apply(); }

    public int getStepsDayKey() {
        int v = prefs.getInt(KEY_STEPS_DAY_KEY, 0);
        if (v == 0) { v = currentDayKey(); prefs.edit().putInt(KEY_STEPS_DAY_KEY, v).apply(); }
        return v;
    }
    public void setStepsDayKey(int dayKey) { prefs.edit().putInt(KEY_STEPS_DAY_KEY, dayKey).apply(); }

    public int getStepCounterBaseline() { return prefs.getInt(KEY_STEP_COUNTER_BASELINE, -1); }
    public void setStepCounterBaseline(int baseline) { prefs.edit().putInt(KEY_STEP_COUNTER_BASELINE, baseline).apply(); }

    // Legacy (kept for backward compat)
    public long getSessionStartMs() { return prefs.getLong(KEY_WALK_SESSION_START, 0L); }
    public void setSessionStartMs(long ms) { prefs.edit().putLong(KEY_WALK_SESSION_START, ms).apply(); }

    // ─── Walk / Run Mode ─────────────────────────────────────────────────────

    public int getModeSteps(String mode) {
        return prefs.getInt("walk".equals(mode) ? KEY_WALK_STEPS : KEY_RUN_STEPS, 0);
    }
    public void setModeSteps(String mode, int steps) {
        prefs.edit().putInt("walk".equals(mode) ? KEY_WALK_STEPS : KEY_RUN_STEPS, steps).apply();
    }
    public void addModeSteps(String mode, int delta) {
        int cur = getModeSteps(mode);
        setModeSteps(mode, cur + delta);
    }

    public int getModeGoal(String mode) {
        // Default: walk=8000, run=5000
        return prefs.getInt("walk".equals(mode) ? KEY_WALK_GOAL : KEY_RUN_GOAL,
                "walk".equals(mode) ? 8000 : 5000);
    }
    public void setModeGoal(String mode, int goal) {
        prefs.edit().putInt("walk".equals(mode) ? KEY_WALK_GOAL : KEY_RUN_GOAL, goal).apply();
    }

    public boolean isModeActive(String mode) {
        return prefs.getBoolean("walk".equals(mode) ? KEY_WALK_ACTIVE : KEY_RUN_ACTIVE, false);
    }
    public void setModeActive(String mode, boolean active) {
        prefs.edit().putBoolean("walk".equals(mode) ? KEY_WALK_ACTIVE : KEY_RUN_ACTIVE, active).apply();
    }

    public long getModeSessionStartMs(String mode) {
        return prefs.getLong("walk".equals(mode) ? KEY_WALK_SESSION_START : KEY_RUN_SESSION_START, 0L);
    }
    public void setModeSessionStartMs(String mode, long ms) {
        prefs.edit().putLong("walk".equals(mode) ? KEY_WALK_SESSION_START : KEY_RUN_SESSION_START, ms).apply();
    }

    // ─── Weekly History ──────────────────────────────────────────────────────

    /**
     * Push today's completed data into rolling 7-day history (shift left, append at end).
     */
    public void pushDayToHistory(int totalSteps, int walkSteps, int runSteps,
                                  int walkGoal, int runGoal) {
        float height = getHeight();
        float weight = getWeight();
        float walkKm = StepCalculatorUtils.getDistanceKm(walkSteps, "walk", height);
        float runKm = StepCalculatorUtils.getDistanceKm(runSteps, "run", height);

        long walkSessionMs = getModeSessionStartMs("walk");
        long runSessionMs = getModeSessionStartMs("run");
        long now = System.currentTimeMillis();
        long walkElapsed = isModeActive("walk") && walkSessionMs > 0 ? (now - walkSessionMs) : 0L;
        long runElapsed = isModeActive("run") && runSessionMs > 0 ? (now - runSessionMs) : 0L;

        SharedPreferences.Editor ed = prefs.edit();
        // Shift left (index 0 = oldest day to discard, 6 = today)
        for (int i = 0; i < 6; i++) {
            ed.putInt(KEY_WEEK_TOTAL + i, prefs.getInt(KEY_WEEK_TOTAL + (i+1), 0));
            ed.putInt(KEY_WEEK_WALK + i, prefs.getInt(KEY_WEEK_WALK + (i+1), 0));
            ed.putInt(KEY_WEEK_RUN + i, prefs.getInt(KEY_WEEK_RUN + (i+1), 0));
            ed.putInt(KEY_WEEK_WALK_GOAL + i, prefs.getInt(KEY_WEEK_WALK_GOAL + (i+1), 8000));
            ed.putInt(KEY_WEEK_RUN_GOAL + i, prefs.getInt(KEY_WEEK_RUN_GOAL + (i+1), 5000));
            ed.putFloat(KEY_WEEK_WALK_KM + i, prefs.getFloat(KEY_WEEK_WALK_KM + (i+1), 0f));
            ed.putFloat(KEY_WEEK_RUN_KM + i, prefs.getFloat(KEY_WEEK_RUN_KM + (i+1), 0f));
            ed.putLong(KEY_WEEK_WALK_TIME + i, prefs.getLong(KEY_WEEK_WALK_TIME + (i+1), 0L));
            ed.putLong(KEY_WEEK_RUN_TIME + i, prefs.getLong(KEY_WEEK_RUN_TIME + (i+1), 0L));
            ed.putInt(KEY_WEEK_DATE_KEY + i, prefs.getInt(KEY_WEEK_DATE_KEY + (i+1), 0));
        }
        // Write today at index 6
        ed.putInt(KEY_WEEK_TOTAL + 6, totalSteps);
        ed.putInt(KEY_WEEK_WALK + 6, walkSteps);
        ed.putInt(KEY_WEEK_RUN + 6, runSteps);
        ed.putInt(KEY_WEEK_WALK_GOAL + 6, walkGoal);
        ed.putInt(KEY_WEEK_RUN_GOAL + 6, runGoal);
        ed.putFloat(KEY_WEEK_WALK_KM + 6, walkKm);
        ed.putFloat(KEY_WEEK_RUN_KM + 6, runKm);
        ed.putLong(KEY_WEEK_WALK_TIME + 6, walkElapsed);
        ed.putLong(KEY_WEEK_RUN_TIME + 6, runElapsed);
        ed.putInt(KEY_WEEK_DATE_KEY + 6, currentDayKey());
        ed.apply();
    }

    /** Legacy pushDayToHistory for backward compat */
    public void pushDayToHistory(int steps) {
        pushDayToHistory(steps, steps, 0, getModeGoal("walk"), getModeGoal("run"));
    }

    // Returns total steps for each of last 7 days
    public int[] getWeeklyHistory() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = prefs.getInt(KEY_WEEK_TOTAL + i, 0);
        return r;
    }

    public int[] getWeeklyWalkSteps() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = prefs.getInt(KEY_WEEK_WALK + i, 0);
        return r;
    }

    public int[] getWeeklyRunSteps() {
        int[] r = new int[7];
        for (int i = 0; i < 7; i++) r[i] = prefs.getInt(KEY_WEEK_RUN + i, 0);
        return r;
    }

    /** Get full detail for a specific day index (0-6) */
    public DayDetail getDayDetail(int dayIndex) {
        DayDetail d = new DayDetail();
        d.totalSteps = prefs.getInt(KEY_WEEK_TOTAL + dayIndex, 0);
        d.walkSteps = prefs.getInt(KEY_WEEK_WALK + dayIndex, 0);
        d.runSteps = prefs.getInt(KEY_WEEK_RUN + dayIndex, 0);
        d.walkGoal = prefs.getInt(KEY_WEEK_WALK_GOAL + dayIndex, 8000);
        d.runGoal = prefs.getInt(KEY_WEEK_RUN_GOAL + dayIndex, 5000);
        d.walkKm = prefs.getFloat(KEY_WEEK_WALK_KM + dayIndex, 0f);
        d.runKm = prefs.getFloat(KEY_WEEK_RUN_KM + dayIndex, 0f);
        d.walkTimeMs = prefs.getLong(KEY_WEEK_WALK_TIME + dayIndex, 0L);
        d.runTimeMs = prefs.getLong(KEY_WEEK_RUN_TIME + dayIndex, 0L);
        d.dateKey = prefs.getInt(KEY_WEEK_DATE_KEY + dayIndex, 0);
        return d;
    }

    public static class DayDetail {
        public int totalSteps, walkSteps, runSteps;
        public int walkGoal, runGoal;
        public float walkKm, runKm;
        public long walkTimeMs, runTimeMs;
        public int dateKey; // yyyyMMdd
    }

    private int currentDayKey() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }
}
