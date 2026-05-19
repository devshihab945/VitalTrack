package com.xerotrust.vitaltrack.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.views.WeeklyStepsChartView;

import java.util.Calendar;

public class StepCounterActivity extends AppCompatActivity implements SensorEventListener {

    private static final int GOAL = 10000;

    private SensorManager sensorManager;
    private Sensor stepCounterSensor;   // TYPE_STEP_COUNTER
    private Sensor stepDetectorSensor;  // TYPE_STEP_DETECTOR
    private boolean hasStepCounter = false;
    private boolean hasStepDetector = false;

    private TextView tvSteps, tvPercent, tvDistance, tvCalories, tvDuration;
    private ProgressBar progressCircle;
    private View cardPermission;
    private WeeklyStepsChartView weeklyChart;

    private AppPrefs prefs;
    private int todaySteps = 0;

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) startSensors();
        else showPermissionCard();
    });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        EdgeToEdge.enable(this);

        // IMPORTANT: must use the same layout used by Fragment (your modern UI)
        setContentView(R.layout.fragment_step_counter);

        // Status bar icons (white)
        View decor = getWindow().getDecorView();
        decor.post(() -> {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
            WindowCompat.getInsetsController(getWindow(), decor).setAppearanceLightStatusBars(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // views (IDs must match new layout)
        tvSteps = findViewById(R.id.tv_steps);
        tvPercent = findViewById(R.id.tv_percent);
        tvDistance = findViewById(R.id.tv_distance);
        tvCalories = findViewById(R.id.tv_calories);
        tvDuration = findViewById(R.id.tv_duration);
        progressCircle = findViewById(R.id.progress_circle);
        cardPermission = findViewById(R.id.card_permission);
        weeklyChart = findViewById(R.id.chart_weekly);

        // back button visible in Activity
        View back = findViewById(R.id.iv_back);
        if (back != null) {
            back.setVisibility(View.VISIBLE);
            back.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btn_grant_permission).setOnClickListener(v -> permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION));

        prefs = new AppPrefs(this);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            hasStepCounter = stepCounterSensor != null;
            hasStepDetector = stepDetectorSensor != null;
        }

        ensureDayBoundary();
        updateWeeklyChart();
        checkPermissionAndStart();
        updateUI();
    }

    private void checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                showPermissionCard();
                return;
            }
        }
        startSensors();
    }

    private void showPermissionCard() {
        if (cardPermission != null) cardPermission.setVisibility(View.VISIBLE);
    }

    private void startSensors() {
        if (cardPermission != null) cardPermission.setVisibility(View.GONE);

        if (sensorManager == null) {
            Toast.makeText(this, "Sensor manager not available", Toast.LENGTH_LONG).show();
            return;
        }

        if (!hasStepCounter && !hasStepDetector) {
            Toast.makeText(this, "Step sensor not available on this device", Toast.LENGTH_LONG).show();
            return;
        }

        // Prefer STEP_COUNTER (more accurate)
        if (hasStepCounter) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    /**
     * Same as Fragment: detect day change, archive previous day, reset baseline.
     */
    private void ensureDayBoundary() {
        int todayKey = dayKeyNow();
        int savedKey = prefs.getStepsDayKey();

        if (savedKey != todayKey) {
            int prevSteps = prefs.getTodaySteps();
            prefs.pushDayToHistory(prevSteps);

            prefs.setStepsDayKey(todayKey);
            prefs.setTodaySteps(0);
            prefs.setStepCounterBaseline(-1);
            prefs.setSessionStartMs(System.currentTimeMillis());
        }

        todaySteps = prefs.getTodaySteps();
        if (prefs.getSessionStartMs() == 0L) {
            prefs.setSessionStartMs(System.currentTimeMillis());
        }
    }

    private int dayKeyNow() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ensureDayBoundary();

        if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            todaySteps += 1;
            prefs.setTodaySteps(todaySteps);
        } else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSinceBoot = (int) event.values[0];

            int baseline = prefs.getStepCounterBaseline();
            if (baseline < 0) {
                baseline = totalSinceBoot;
                prefs.setStepCounterBaseline(baseline);
            }

            todaySteps = Math.max(0, totalSinceBoot - baseline);
            prefs.setTodaySteps(todaySteps);
        }

        updateWeeklyChart();
        updateUI();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateWeeklyChart() {
        if (weeklyChart != null) {
            weeklyChart.setWeeklySteps(prefs.getWeeklyHistory());
        }
    }

    private void updateUI() {
        if (tvSteps != null) tvSteps.setText(String.format("%,d", todaySteps));

        int percent = Math.min((todaySteps * 100) / GOAL, 100);
        if (tvPercent != null) tvPercent.setText(percent + "%");
        if (progressCircle != null) progressCircle.setProgress(percent);

        float distanceKm = todaySteps * 0.000762f;
        if (tvDistance != null) tvDistance.setText(String.format("%.1f km", distanceKm));

        float calories = todaySteps * 0.04f;
        if (tvCalories != null) tvCalories.setText(String.format("%.0f kcal", calories));

        long minutes = (System.currentTimeMillis() - prefs.getSessionStartMs()) / 60000;
        if (minutes < 0) minutes = 0;
        if (tvDuration != null) tvDuration.setText(minutes + " min");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureDayBoundary();
        checkPermissionAndStart();
        updateUI();
    }
}