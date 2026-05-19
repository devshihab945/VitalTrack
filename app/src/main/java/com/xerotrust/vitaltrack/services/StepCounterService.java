package com.xerotrust.vitaltrack.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.activities.MainActivity;
import com.xerotrust.vitaltrack.utils.AppPrefs;

import java.util.Calendar;

/**
 * Step Counter Service
 *
 * WALK mode  → Uses TYPE_STEP_DETECTOR (hardware step detector).
 *              Counts every detected step.
 *
 * RUN mode   → Uses TYPE_ACCELEROMETER + RunningStepDetector.
 *              Only counts steps when the vertical acceleration impact
 *              exceeds the running threshold (≥2.5 g). Pure walking
 *              (~1.2–1.8 g impact) is ignored, so steps are NOT
 *              counted if the user is just walking while in run mode.
 */
public class StepCounterService extends Service implements SensorEventListener {

    public static final String CHANNEL_ID        = "step_counter_channel";
    public static final String ACTION_START_WALK = "ACTION_START_WALK";
    public static final String ACTION_START_RUN  = "ACTION_START_RUN";
    public static final String ACTION_STOP       = "ACTION_STOP";
    public static final String EXTRA_MODE        = "extra_mode";
    public static final String MODE_WALK         = "walk";
    public static final String MODE_RUN          = "run";

    public static final String BROADCAST_UPDATE  = "com.technetia.healthapp.STEP_UPDATE";

    // ── Sensors ──────────────────────────────────────────────────────────────
    private SensorManager sensorManager;

    // Walk sensors (hardware-level step counting)
    private Sensor stepDetectorSensor;
    private Sensor stepCounterSensor;
    private boolean hasStepDetector = false;
    private boolean hasStepCounter  = false;

    // Run sensor (raw accelerometer for impact detection)
    private Sensor accelerometerSensor;
    private boolean hasAccelerometer = false;

    // ── Run step detection state ──────────────────────────────────────────────
    /**
     * Minimum vertical acceleration magnitude (m/s²) to count as a running step.
     * Human walking peak = ~12–18 m/s²  (~1.2–1.8 g)
     * Human running peak = ~25–40 m/s²  (~2.5–4.0 g)
     *
     * Threshold at 22 m/s² (~2.2 g) catches slow jogging while excluding fast walking.
     * Adjust down to 20 if users report missed steps at slow jog pace.
     */
    private static final float RUN_ACCEL_THRESHOLD_MS2 = 22.0f; // m/s²

    /** Minimum time between two counted run steps (ms). Prevents double-counting one impact. */
    private static final long  RUN_MIN_STEP_INTERVAL_MS = 250;  // ~max 4 steps/sec

    /** Low-pass filter coefficient for gravity removal (0 = no filter, 1 = full filter). */
    private static final float LOW_PASS_ALPHA = 0.85f;

    private float[] gravity = {0f, 0f, 0f};
    private long    lastRunStepTimeMs = 0L;
    private boolean inPeak = false; // true while acceleration is above threshold (debounce)

    // ── General state ─────────────────────────────────────────────────────────
    private AppPrefs prefs;
    private String   currentMode = MODE_WALK;
    private boolean  isTracking  = false;
    private int      todaySteps  = 0;

    // ── Binder ────────────────────────────────────────────────────────────────
    public class StepBinder extends Binder {
        public StepCounterService getService() { return StepCounterService.this; }
    }
    private final IBinder binder = new StepBinder();

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = new AppPrefs(this);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepDetectorSensor  = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            stepCounterSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            hasStepDetector  = stepDetectorSensor  != null;
            hasStepCounter   = stepCounterSensor   != null;
            hasAccelerometer = accelerometerSensor != null;
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case ACTION_START_WALK:
                currentMode = MODE_WALK;
                startTracking();
                break;
            case ACTION_START_RUN:
                currentMode = MODE_RUN;
                startTracking();
                break;
            case ACTION_STOP:
                stopTracking();
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    // ── Start / Stop ──────────────────────────────────────────────────────────

    private void startTracking() {
        isTracking = true;
        ensureDayBoundary();
        todaySteps = getCurrentModeSteps();

        if (prefs.getModeSessionStartMs(currentMode) == 0L) {
            prefs.setModeSessionStartMs(currentMode, System.currentTimeMillis());
        }

        // Reset run-detection state for a fresh session
        gravity[0] = gravity[1] = gravity[2] = 0f;
        lastRunStepTimeMs = 0L;
        inPeak = false;

        registerSensors();
        startForeground(1, buildNotification());
        broadcastUpdate();
    }

    private void stopTracking() {
        isTracking = false;
        unregisterSensors();
        prefs.setModeActive(currentMode, false);
        broadcastUpdate();
    }

    // ── Sensor registration ───────────────────────────────────────────────────

    private void registerSensors() {
        unregisterSensors();
        if (sensorManager == null) return;

        if (MODE_RUN.equals(currentMode)) {
            // Run mode: use accelerometer for impact-based detection
            if (hasAccelerometer) {
                // SENSOR_DELAY_GAME = ~50Hz — good balance of responsiveness vs. battery
                sensorManager.registerListener(this, accelerometerSensor,
                        SensorManager.SENSOR_DELAY_GAME);
            }
            // Fallback: if no accelerometer, fall back to step detector
            // (won't filter walking but at least counts something)
            else if (hasStepDetector) {
                sensorManager.registerListener(this, stepDetectorSensor,
                        SensorManager.SENSOR_DELAY_GAME);
            } else if (hasStepCounter) {
                sensorManager.registerListener(this, stepCounterSensor, 50_000, 0);
            }
        } else {
            // Walk mode: hardware step sensors are accurate enough
            if (hasStepDetector) {
                sensorManager.registerListener(this, stepDetectorSensor,
                        SensorManager.SENSOR_DELAY_GAME);
            } else if (hasStepCounter) {
                sensorManager.registerListener(this, stepCounterSensor, 50_000, 0);
            }
        }
    }

    private void unregisterSensors() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    // ── Sensor events ─────────────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        ensureDayBoundary();

        switch (event.sensor.getType()) {

            // ── Walk mode: hardware step detector ─────────────────────────
            case Sensor.TYPE_STEP_DETECTOR:
                // Each event = exactly 1 step
                todaySteps += 1;
                prefs.addModeSteps(currentMode, 1);
                break;

            // ── Walk mode fallback: hardware step counter ─────────────────
            case Sensor.TYPE_STEP_COUNTER: {
                int totalSinceBoot = (int) event.values[0];
                int baseline = prefs.getStepCounterBaseline();
                if (baseline < 0) {
                    baseline = totalSinceBoot;
                    prefs.setStepCounterBaseline(baseline);
                }
                int newTotal = Math.max(0, totalSinceBoot - baseline);
                int delta = newTotal - todaySteps;
                if (delta > 0) {
                    prefs.addModeSteps(currentMode, delta);
                    todaySteps = newTotal;
                }
                break;
            }

            // ── Run mode: accelerometer-based impact detection ────────────
            case Sensor.TYPE_ACCELEROMETER:
                handleRunAccelerometer(event);
                break;
        }

        // Keep total (walk + run) in sync
        int totalToday = prefs.getModeSteps(MODE_WALK) + prefs.getModeSteps(MODE_RUN);
        prefs.setTodaySteps(totalToday);

        updateNotification();
        broadcastUpdate();
    }

    /**
     * Detects a running step from raw accelerometer data.
     *
     * Algorithm:
     *  1. Remove gravity with a low-pass filter → linear acceleration
     *  2. Compute magnitude of linear acceleration vector
     *  3. If magnitude >= RUN_ACCEL_THRESHOLD_MS2 AND minimum interval passed
     *     AND we are not already in a peak → count 1 step and mark "in peak"
     *  4. When magnitude drops back below threshold → clear "in peak" flag
     *
     * This produces one count per foot-strike (the large downward impact when
     * the foot hits the ground during running), which matches what hardware
     * step sensors count during running.
     */
    private void handleRunAccelerometer(SensorEvent event) {
        // Step 1: Low-pass filter to isolate gravity component
        gravity[0] = LOW_PASS_ALPHA * gravity[0] + (1f - LOW_PASS_ALPHA) * event.values[0];
        gravity[1] = LOW_PASS_ALPHA * gravity[1] + (1f - LOW_PASS_ALPHA) * event.values[1];
        gravity[2] = LOW_PASS_ALPHA * gravity[2] + (1f - LOW_PASS_ALPHA) * event.values[2];

        // Step 2: Linear acceleration = raw - gravity
        float linX = event.values[0] - gravity[0];
        float linY = event.values[1] - gravity[1];
        float linZ = event.values[2] - gravity[2];

        // Step 3: Magnitude of linear acceleration vector
        float magnitude = (float) Math.sqrt(linX * linX + linY * linY + linZ * linZ);

        long now = System.currentTimeMillis();

        if (magnitude >= RUN_ACCEL_THRESHOLD_MS2) {
            // We are at or above the running impact threshold
            if (!inPeak && (now - lastRunStepTimeMs) >= RUN_MIN_STEP_INTERVAL_MS) {
                // Rising edge of a new peak: count one step
                todaySteps += 1;
                prefs.addModeSteps(MODE_RUN, 1);
                lastRunStepTimeMs = now;
                inPeak = true;
            }
        } else {
            // Below threshold: ready for next peak
            inPeak = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // ── Day boundary ──────────────────────────────────────────────────────────

    private void ensureDayBoundary() {
        int todayKey = dayKeyNow();
        int savedKey = prefs.getStepsDayKey();
        if (savedKey != todayKey) {
            int prevWalk  = prefs.getModeSteps(MODE_WALK);
            int prevRun   = prefs.getModeSteps(MODE_RUN);
            int prevTotal = prevWalk + prevRun;
            prefs.pushDayToHistory(prevTotal, prevWalk, prevRun,
                    prefs.getModeGoal(MODE_WALK), prefs.getModeGoal(MODE_RUN));
            prefs.setStepsDayKey(todayKey);
            prefs.setTodaySteps(0);
            prefs.setModeSteps(MODE_WALK, 0);
            prefs.setModeSteps(MODE_RUN, 0);
            prefs.setStepCounterBaseline(-1);
            prefs.setModeSessionStartMs(MODE_WALK, 0L);
            prefs.setModeSessionStartMs(MODE_RUN, 0L);
            todaySteps = 0;
        }
    }

    private int getCurrentModeSteps() { return prefs.getModeSteps(currentMode); }

    private int dayKeyNow() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 10000
                + (c.get(Calendar.MONTH) + 1) * 100
                + c.get(Calendar.DAY_OF_MONTH);
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private Notification buildNotification() {
        String modeLabel = MODE_WALK.equals(currentMode) ? "Walking" : "Running";
        int steps = prefs.getModeSteps(currentMode);
        int goal  = prefs.getModeGoal(currentMode);

        Intent notifIntent = new Intent(this, MainActivity.class);
        notifIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notifIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Counter - " + modeLabel)
                .setContentText(steps + " / " + goal + " steps")
                .setSmallIcon(R.drawable.ic_step)
                .setContentIntent(pi)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(1, buildNotification());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Step Counter", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Tracks your walking and running steps");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void broadcastUpdate() {
        sendBroadcast(new Intent(BROADCAST_UPDATE));
    }

    // ── Binder / Lifecycle ────────────────────────────────────────────────────

    @Nullable @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensors();
        isTracking = false;
    }

    // ── Public APIs for Fragment binding ─────────────────────────────────────
    public boolean isRunning()      { return isTracking; }
    public String  getCurrentMode() { return currentMode; }

    /**
     * Returns the current run detection threshold in m/s².
     * Exposed so UI can optionally show/debug the value.
     */
    public static float getRunThreshold() { return RUN_ACCEL_THRESHOLD_MS2; }
}
