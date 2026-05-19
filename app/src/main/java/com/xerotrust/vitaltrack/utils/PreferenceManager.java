package com.xerotrust.vitaltrack.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "HealthAppPrefs";
    private static final String KEY_FIRST_TIME = "first_time";
    private static final String KEY_NAME = "user_name";
    private static final String KEY_AGE = "user_age";
    private static final String KEY_HEIGHT = "user_height";
    private static final String KEY_WEIGHT = "user_weight";
    private static final String KEY_BLOOD_TYPE = "blood_type";
    private static final String KEY_BMI = "bmi";
    private static final String KEY_WATER_GOAL = "water_goal";
    private static final String KEY_CALORIE_GOAL = "calorie_goal";
    private static final String KEY_STEP_COUNT = "step_count";
    private static final String KEY_WATER_TODAY = "water_today";
    private static final String KEY_LAST_DATE = "last_date";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public PreferenceManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public boolean isFirstTime() { return prefs.getBoolean(KEY_FIRST_TIME, true); }
    public void setFirstTime(boolean val) { editor.putBoolean(KEY_FIRST_TIME, val).apply(); }

    public String getUserName() { return prefs.getString(KEY_NAME, "User"); }
    public void setUserName(String name) { editor.putString(KEY_NAME, name).apply(); }

    public int getAge() { return prefs.getInt(KEY_AGE, 0); }
    public void setAge(int age) { editor.putInt(KEY_AGE, age).apply(); }

    public float getHeight() { return prefs.getFloat(KEY_HEIGHT, 0f); }
    public void setHeight(float h) { editor.putFloat(KEY_HEIGHT, h).apply(); }

    public float getWeight() { return prefs.getFloat(KEY_WEIGHT, 0f); }
    public void setWeight(float w) { editor.putFloat(KEY_WEIGHT, w).apply(); }

    public String getBloodType() { return prefs.getString(KEY_BLOOD_TYPE, ""); }
    public void setBloodType(String bt) { editor.putString(KEY_BLOOD_TYPE, bt).apply(); }

    public float getBmi() { return prefs.getFloat(KEY_BMI, 0f); }
    public void setBmi(float bmi) { editor.putFloat(KEY_BMI, bmi).apply(); }

    public float getWaterGoal() { return prefs.getFloat(KEY_WATER_GOAL, 2.0f); }
    public void setWaterGoal(float g) { editor.putFloat(KEY_WATER_GOAL, g).apply(); }

    public float getWaterToday() { return prefs.getFloat(KEY_WATER_TODAY, 0f); }
    public void setWaterToday(float w) { editor.putFloat(KEY_WATER_TODAY, w).apply(); }

    public int getCalorieGoal() { return prefs.getInt(KEY_CALORIE_GOAL, 2000); }
    public void setCalorieGoal(int c) { editor.putInt(KEY_CALORIE_GOAL, c).apply(); }

    public int getStepCount() { return prefs.getInt(KEY_STEP_COUNT, 0); }
    public void setStepCount(int s) { editor.putInt(KEY_STEP_COUNT, s).apply(); }

    public String getLastDate() { return prefs.getString(KEY_LAST_DATE, ""); }
    public void setLastDate(String d) { editor.putString(KEY_LAST_DATE, d).apply(); }
}
