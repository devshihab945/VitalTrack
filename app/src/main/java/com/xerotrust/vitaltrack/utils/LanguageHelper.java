package com.xerotrust.vitaltrack.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

/**
 * LanguageHelper — manages app language (English / Bangla)
 *
 * Usage:
 *   - Call applyLanguage(context) in every Activity's attachBaseContext()
 *   - Call setLanguage(context, "bn") or setLanguage(context, "en") to switch
 *   - Call restartActivity(activity) after switching to reload UI
 */
public class LanguageHelper {

    public static final String LANG_EN = "en";
    public static final String LANG_BN = "bn";

    private static final String PREF_NAME  = "HealthAppPrefs";
    private static final String KEY_LANG   = "app_language";

    /** Returns the saved language code ("en" or "bn"), defaulting to "en" */
    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, LANG_EN);
    }

    /** Saves the chosen language code */
    public static void setLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, langCode).apply();
    }

    /** Returns true if current saved language is Bangla */
    public static boolean isBangla(Context context) {
        return LANG_BN.equals(getSavedLanguage(context));
    }

    /**
     * Applies the saved language locale to the given context.
     * Call this inside every Activity's attachBaseContext().
     */
    public static Context applyLanguage(Context context) {
        String lang = getSavedLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Restarts the given activity so the new language takes effect immediately.
     */
    public static void restartActivity(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        activity.startActivity(activity.getIntent());
    }
}
