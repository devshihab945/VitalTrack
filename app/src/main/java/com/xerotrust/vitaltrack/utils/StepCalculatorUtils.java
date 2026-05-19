package com.xerotrust.vitaltrack.utils;

public class StepCalculatorUtils {

    // Average step length ratios
    private static final float WALK_STEP_LENGTH_CM = 76.2f; // ~76cm per step for walking
    private static final float RUN_STEP_LENGTH_CM = 91.4f;  // ~91cm per step for running

    // Calories per step (MET-based estimates)
    private static final float WALK_CAL_PER_STEP_PER_KG = 0.000535f;
    private static final float RUN_CAL_PER_STEP_PER_KG = 0.000850f;

    /**
     * Calculate distance in km based on steps and mode.
     * Uses height-based step length if heightCm > 0.
     */
    public static float getDistanceKm(int steps, String mode, float heightCm) {
        float stepLengthM;
        if (heightCm > 0) {
            // Estimated step length = height * ratio
            if ("run".equals(mode)) {
                stepLengthM = heightCm * 0.0055f; // running stride
            } else {
                stepLengthM = heightCm * 0.0043f; // walking stride
            }
        } else {
            stepLengthM = "run".equals(mode) ? RUN_STEP_LENGTH_CM / 100f : WALK_STEP_LENGTH_CM / 100f;
        }
        return (steps * stepLengthM) / 1000f;
    }

    /**
     * Calculate calories burned.
     * weightKg > 0 for personalized result, else fallback to 70kg estimate.
     */
    public static float getCalories(int steps, String mode, float weightKg) {
        float weight = weightKg > 0 ? weightKg : 70f;
        float calPerStep = "run".equals(mode) ? RUN_CAL_PER_STEP_PER_KG : WALK_CAL_PER_STEP_PER_KG;
        return steps * calPerStep * weight;
    }

    /**
     * Format duration from milliseconds to "HH:mm:ss" or "mm:ss"
     */
    public static String formatDuration(long elapsedMs) {
        if (elapsedMs <= 0) return "0 min";
        long totalSec = elapsedMs / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return totalSec + "s";
    }
}
