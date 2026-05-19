package com.xerotrust.vitaltrack.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.receivers.WaterAlarmReceiver;
import com.xerotrust.vitaltrack.services.WaterAlarmService;

/**
 * Full-screen alarm UI shown when a water reminder fires.
 * Buttons: Snooze 10m | Snooze 30m | Stop
 */
public class WaterAlarmRingActivity extends AppCompatActivity {

    public static final String EXTRA_ALARM_ID   = "alarm_id";
    public static final String EXTRA_LABEL      = "label";
    public static final String EXTRA_ALERT_TYPE = "alert_type";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show over lock-screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.activity_water_alarm_ring);

        final int    alarmId   = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);
        final String label     = getIntent().getStringExtra(EXTRA_LABEL);
        final String alertType = getIntent().getStringExtra(EXTRA_ALERT_TYPE);

        TextView tvTitle   = findViewById(R.id.tv_water_alarm_title);
        TextView tvLabel   = findViewById(R.id.tv_water_alarm_label);
        Button   btnSnooze10 = findViewById(R.id.btn_water_snooze);   // existing view — reused as 10m
        Button   btnSnooze30 = findViewById(R.id.btn_water_snooze_30); // new view (add to layout)
        Button   btnStop   = findViewById(R.id.btn_water_stop);

        tvTitle.setText("\uD83D\uDCA7 Water Reminder");
        tvLabel.setText(label != null && !label.isEmpty() ? label : "Time to drink water!");

        // Ensure service is running
        ensureServiceRunning(alarmId, label, alertType);

        btnStop.setOnClickListener(v -> {
            stopWaterAlarmService();
            finish();
        });

        if (btnSnooze10 != null) {
            btnSnooze10.setText("Snooze 10m");
            btnSnooze10.setOnClickListener(v -> {
                stopWaterAlarmService();
                WaterAlarmReceiver.scheduleSnooze(this, alarmId, alertType, 10);
                finish();
            });
        }

        if (btnSnooze30 != null) {
            btnSnooze30.setOnClickListener(v -> {
                stopWaterAlarmService();
                WaterAlarmReceiver.scheduleSnooze(this, alarmId, alertType, 30);
                finish();
            });
        }
    }

    private void ensureServiceRunning(int alarmId, String label, String alertType) {
        Intent s = new Intent(this, WaterAlarmService.class);
        s.setAction(WaterAlarmService.ACTION_START);
        s.putExtra(WaterAlarmService.EXTRA_ALARM_ID,   alarmId);
        s.putExtra(WaterAlarmService.EXTRA_LABEL,      label);
        s.putExtra(WaterAlarmService.EXTRA_ALERT_TYPE, alertType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s);
        else startService(s);
    }

    private void stopWaterAlarmService() {
        Intent stop = new Intent(this, WaterAlarmService.class);
        stop.setAction(WaterAlarmService.ACTION_STOP);
        startService(stop);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
