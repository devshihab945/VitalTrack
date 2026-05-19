package com.xerotrust.vitaltrack.activities;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.adapters.WaterAlarmAdapter;
import com.xerotrust.vitaltrack.models.WaterAlarm;
import com.xerotrust.vitaltrack.receivers.WaterAlarmReceiver;
import com.xerotrust.vitaltrack.services.WaterAlarmService;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

import java.util.Calendar;
import java.util.List;

public class WaterIntakeAlarmActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    /**
     * Base request code for water alarm PendingIntents.
     * Must stay in sync with BootReceiver.WATER_REQUEST_BASE.
     */
    public static final int REQUEST_BASE = 5000;

    private RecyclerView recycler;
    private TextView tvEmpty;
    private WaterAlarmAdapter adapter;
    private List<WaterAlarm> alarms;
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Status bar color before EdgeToEdge
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_water_intake_alarm);

        View decor = getWindow().getDecorView();
        decor.post(() -> {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
            WindowCompat.getInsetsController(getWindow(), decor).setAppearanceLightStatusBars(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = new DatabaseHelper(this);

        recycler = findViewById(R.id.recycler_water_alarms);
        tvEmpty = findViewById(R.id.tv_empty);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAlarmDialog(null));

        loadAlarms();
    }

    private void loadAlarms() {
        alarms = db.getAllWaterAlarms();
        adapter = new WaterAlarmAdapter(alarms, new WaterAlarmAdapter.Listener() {
            @Override
            public void onEdit(WaterAlarm alarm) {
                showAlarmDialog(alarm);
            }

            @Override
            public void onToggle(WaterAlarm alarm) {
                toggleAlarm(alarm);
            }

            @Override
            public void onDelete(WaterAlarm alarm) {
                confirmDelete(alarm);
            }
        });
        recycler.setAdapter(adapter);
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean empty = alarms.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Add / Edit Dialog ────────────────────────────────────────────────────

    private void showAlarmDialog(WaterAlarm existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_water_alarm, null);

        NumberPicker pickerHour = view.findViewById(R.id.picker_hour);
        NumberPicker pickerMinute = view.findViewById(R.id.picker_minute);
        NumberPicker pickerAmPm = view.findViewById(R.id.picker_ampm);
        com.google.android.material.textfield.TextInputEditText etLabel = view.findViewById(R.id.et_alarm_label);
        RadioButton rbRing = view.findViewById(R.id.rb_ring);
        RadioButton rbVib = view.findViewById(R.id.rb_vibrate);

        pickerHour.setMinValue(1);
        pickerHour.setMaxValue(12);
        pickerHour.setWrapSelectorWheel(true);

        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);
        pickerMinute.setFormatter(val -> String.format("%02d", val));
        pickerMinute.setWrapSelectorWheel(true);

        pickerAmPm.setMinValue(0);
        pickerAmPm.setMaxValue(1);
        pickerAmPm.setDisplayedValues(new String[]{"AM", "PM"});
        pickerAmPm.setWrapSelectorWheel(false);

        if (existing != null) {
            int h24 = existing.getHour();
            pickerHour.setValue(h24 == 0 ? 12 : h24 > 12 ? h24 - 12 : h24);
            pickerMinute.setValue(existing.getMinute());
            pickerAmPm.setValue(h24 >= 12 ? 1 : 0);
            if (existing.getLabel() != null) etLabel.setText(existing.getLabel());
            if ("vibrate".equalsIgnoreCase(existing.getAlertType())) rbVib.setChecked(true);
            else rbRing.setChecked(true);
        } else {
            Calendar now = Calendar.getInstance();
            int h24 = now.get(Calendar.HOUR_OF_DAY);
            pickerHour.setValue(h24 == 0 ? 12 : h24 > 12 ? h24 - 12 : h24);
            pickerMinute.setValue(now.get(Calendar.MINUTE));
            pickerAmPm.setValue(h24 >= 12 ? 1 : 0);
        }

        boolean isBn = LanguageHelper.isBangla(this);
        new AlertDialog.Builder(this).setTitle(existing == null ? (isBn ? "পানির অ্যালার্ম যোগ করুন" : "Add Water Alarm") : (isBn ? "পানির অ্যালার্ম সম্পাদনা করুন" : "Edit Water Alarm")).setView(view).setPositiveButton(isBn ? "সংরক্ষণ করুন" : "Save", (dlg, w) -> {
            int h24 = convertTo24(pickerHour.getValue(), pickerAmPm.getValue());
            int minute = pickerMinute.getValue();
            String label = etLabel.getText() != null ? etLabel.getText().toString().trim() : "";
            String type = rbVib.isChecked() ? "vibrate" : "ring";

            if (existing == null) {
                WaterAlarm alarm = new WaterAlarm(h24, minute, label, type);
                long id = db.addWaterAlarm(alarm);
                alarm.setId((int) id);
                scheduleAlarm(alarm);
                alarms.add(alarm);
                adapter.notifyItemInserted(alarms.size() - 1);
            } else {
                cancelAlarm(existing.getId());
                existing.setHour(h24);
                existing.setMinute(minute);
                existing.setLabel(label);
                existing.setAlertType(type);
                existing.setEnabled(true);
                db.updateWaterAlarm(existing);
                scheduleAlarm(existing);
                int idx = alarms.indexOf(existing);
                if (idx >= 0) adapter.notifyItemChanged(idx);
            }
            updateEmptyState();
            Toast.makeText(this, isBn ? "অ্যালার্ম সংরক্ষিত হয়েছে!" : "Alarm saved!", Toast.LENGTH_SHORT).show();
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null).show();
    }

    // ── Toggle ───────────────────────────────────────────────────────────────

    private void toggleAlarm(WaterAlarm alarm) {
        boolean isBn = LanguageHelper.isBangla(this);
        alarm.setEnabled(!alarm.isEnabled());
        db.updateWaterAlarmEnabled(alarm.getId(), alarm.isEnabled());
        if (alarm.isEnabled()) {
            scheduleAlarm(alarm);
            Toast.makeText(this, isBn ? "অ্যালার্ম চালু হয়েছে" : "Alarm enabled", Toast.LENGTH_SHORT).show();
        } else {
            cancelAlarm(alarm.getId());
            Toast.makeText(this, isBn ? "অ্যালার্ম বন্ধ হয়েছে" : "Alarm disabled", Toast.LENGTH_SHORT).show();
        }
        int idx = alarms.indexOf(alarm);
        if (idx >= 0) adapter.notifyItemChanged(idx);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    private void confirmDelete(WaterAlarm alarm) {
        boolean isBn = LanguageHelper.isBangla(this);
        new AlertDialog.Builder(this).setTitle(isBn ? "অ্যালার্ম মুছুন" : "Delete Alarm").setMessage(isBn ? "এই পানির রিমাইন্ডারটি মুছে ফেলবেন?" : "Delete this water reminder?").setPositiveButton(isBn ? "মুছুন" : "Delete", (d, w) -> {
            cancelAlarm(alarm.getId());
            db.deleteWaterAlarm(alarm.getId());
            int idx = alarms.indexOf(alarm);
            alarms.remove(idx);
            adapter.notifyItemRemoved(idx);
            updateEmptyState();
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null).show();
    }

    // ── Alarm Scheduling ─────────────────────────────────────────────────────

    /**
     * Schedule a daily exact alarm.
     * <p>
     * On Android 6+  setRepeating() is inexact (can drift many minutes).
     * We use setExactAndAllowWhileIdle() for the first fire, and the
     * WaterAlarmReceiver re-schedules the next day's alarm after each ring.
     */
    public static void scheduleAlarm(Context context, WaterAlarm alarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        PendingIntent pi = buildPendingIntent(context, alarm.getId(), alarm.getAlertType());

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        cal.set(Calendar.MINUTE, alarm.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // If that time has already passed today, schedule for tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                // Permission not granted — fall back to inexact (better than nothing)
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    /**
     * Instance version — delegates to static so BootReceiver can reuse.
     */
    private void scheduleAlarm(WaterAlarm alarm) {
        scheduleAlarm(this, alarm);
    }

    public static void cancelAlarm(Context context, int alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(buildPendingIntent(context, alarmId, "ring"));
        am.cancel(buildPendingIntent(context, alarmId, "vibrate"));

        // Also stop the service if currently running
        Intent stopIntent = new Intent(context, WaterAlarmService.class);
        stopIntent.setAction(WaterAlarmService.ACTION_STOP);
        context.startService(stopIntent);
    }

    private void cancelAlarm(int alarmId) {
        cancelAlarm(this, alarmId);
    }

    public static PendingIntent buildPendingIntent(Context context, int alarmId, String alertType) {
        Intent intent = new Intent(context, WaterAlarmReceiver.class);
        intent.putExtra("alarm_id", alarmId);
        intent.putExtra("alert_type", alertType);
        return PendingIntent.getBroadcast(context, REQUEST_BASE + alarmId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private int convertTo24(int h12, int ampm) {
        if (ampm == 0) return h12 == 12 ? 0 : h12;       // AM
        else return h12 == 12 ? 12 : h12 + 12; // PM
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}