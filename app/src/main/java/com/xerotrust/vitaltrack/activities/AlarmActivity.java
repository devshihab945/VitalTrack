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
import com.xerotrust.vitaltrack.receivers.AlarmReceiver;
import com.xerotrust.vitaltrack.services.MedicineAlarmService;
import com.xerotrust.vitaltrack.utils.MedicineScheduleUtils;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

public class AlarmActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        setContentView(R.layout.activity_alarm);

        final String name   = getIntent().getStringExtra("medicine_name");
        final String detail = getIntent().getStringExtra("dosage");
        String modeExtra    = getIntent().getStringExtra("vibration_mode");
        final String mode   = (modeExtra != null) ? modeExtra : "normal";

        final int planId = getIntent().getIntExtra(MedicineScheduleUtils.EXTRA_PLAN_ID, 0);
        final int timeId = getIntent().getIntExtra(MedicineScheduleUtils.EXTRA_TIME_ID, 0);
        final int medId  = getIntent().getIntExtra("med_id", 0);

        TextView tvTitle    = findViewById(R.id.tv_alarm_title);
        TextView tvBody     = findViewById(R.id.tv_alarm_body);
        Button   btnStop    = findViewById(R.id.btn_alarm_stop);
        Button   btnSnooze  = findViewById(R.id.btn_alarm_snooze);      // Snooze 10m
        Button   btnSnooze30 = findViewById(R.id.btn_alarm_snooze_30);  // Snooze 30m (new view)

        tvTitle.setText("Medicine Reminder");

        if (detail != null && !detail.trim().isEmpty()) {
            tvBody.setText("Take: " + name + "\n" + detail);
        } else {
            tvBody.setText("Take: " + name);
        }

        // Ensure service is running
        Intent s = new Intent(this, MedicineAlarmService.class);
        s.setAction(MedicineAlarmService.ACTION_START);
        s.putExtra(MedicineAlarmService.EXTRA_NAME,    name);
        s.putExtra(MedicineAlarmService.EXTRA_DOSAGE,  detail);
        s.putExtra(MedicineAlarmService.EXTRA_MODE,    mode);
        s.putExtra(MedicineAlarmService.EXTRA_PLAN_ID, planId);
        s.putExtra(MedicineAlarmService.EXTRA_TIME_ID, timeId);
        s.putExtra(MedicineAlarmService.EXTRA_MED_ID,  medId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(s);
        else startService(s);

        btnStop.setOnClickListener(v -> {
            stopAlarmService();
            finish();
        });

        if (btnSnooze != null) {
            btnSnooze.setText("Snooze 10m");
            btnSnooze.setOnClickListener(v -> {
                stopAlarmService();
                scheduleSnooze(planId, timeId, medId, name, detail, mode, 10);
                finish();
            });
        }

        if (btnSnooze30 != null) {
            btnSnooze30.setOnClickListener(v -> {
                stopAlarmService();
                scheduleSnooze(planId, timeId, medId, name, detail, mode, 30);
                finish();
            });
        }
    }

    private void scheduleSnooze(int planId, int timeId, int medId,
                                String name, String detail, String mode, int minutes) {
        if (planId != 0 && timeId != 0) {
            AlarmReceiver.scheduleSnoozePlan(this, planId, timeId, minutes);
        } else if (medId != 0) {
            AlarmReceiver.scheduleSnooze(this, medId, name, detail, mode, minutes);
        }
    }

    private void stopAlarmService() {
        Intent stop = new Intent(this, MedicineAlarmService.class);
        stop.setAction(MedicineAlarmService.ACTION_STOP);
        startService(stop);
    }
}
