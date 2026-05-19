package com.xerotrust.vitaltrack.activities;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.adapters.MedicineAdapter;
import com.xerotrust.vitaltrack.dialogs.VibrationDialogFragment;
import com.xerotrust.vitaltrack.models.MedicineReminder;
import com.xerotrust.vitaltrack.receivers.AlarmReceiver;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MedicineReminderActivity extends AppCompatActivity implements MedicineAdapter.MedicineActionListener {

    private RecyclerView recyclerView;
    private View tvEmpty;
    ImageView iv_back;
    private DatabaseHelper db;

    private final List<MedicineReminder> medicines = new ArrayList<>();
    private MedicineAdapter adapter;

    private MedicineReminder pendingMedicine;
    private boolean pendingIsEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindow();
        setContentView(R.layout.fragment_medicine_reminder);
        applyWindowInsets();

        createNotificationChannels();

        db = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        setupActions();

        loadMedicines();
        rescheduleAllEnabledMedicines();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        EdgeToEdge.enable(this);
    }

    private void applyWindowInsets() {
        View root = findViewById(R.id.main);
        if (root == null) return;

        root.post(() -> {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_medicines);
        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void setupRecyclerView() {
        adapter = new MedicineAdapter(medicines, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);
    }

    private void setupActions() {
        iv_back = findViewById(R.id.iv_back);
        iv_back.setVisibility(View.VISIBLE);
        iv_back.setOnClickListener(v -> finish());

        FloatingActionButton fab = findViewById(R.id.fab_add_medicine);
        fab.setOnClickListener(v -> showMedicineDialog(null));
    }

    // ─── Dialogs ─────────────────────────────────────────────────────────────

    private void showMedicineDialog(MedicineReminder existing) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null);

        TextInputEditText etName = dialogView.findViewById(R.id.et_medicine_name);
        TextInputEditText etDosage = dialogView.findViewById(R.id.et_dosage);
        NumberPicker pickerHour = dialogView.findViewById(R.id.picker_hour);
        NumberPicker pickerMinute = dialogView.findViewById(R.id.picker_minute);
        NumberPicker pickerAmPm = dialogView.findViewById(R.id.picker_ampm);

        if (existing != null) {
            etName.setText(existing.getName());
            etDosage.setText(existing.getDosage());

            int hour24 = existing.getHour();
            int hour12 = hour24 == 0 ? 12 : (hour24 > 12 ? hour24 - 12 : hour24);
            int ampm = hour24 < 12 ? 0 : 1;
            setupPickers(pickerHour, pickerMinute, pickerAmPm, hour12, existing.getMinute(), ampm);
        } else {
            setupPickers(pickerHour, pickerMinute, pickerAmPm, 8, 0, 0);
        }

        boolean isBn = LanguageHelper.isBangla(this);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.RoundedDialog).setTitle(existing == null ? (isBn ? "নতুন ওষুধ যোগ করুন" : "Add New Medicine") : (isBn ? "ওষুধ সম্পাদনা করুন" : "Edit Medicine")).setView(dialogView).setPositiveButton(existing == null ? (isBn ? "পরবর্তী" : "Next") : (isBn ? "সংরক্ষণ করুন" : "Save"), (d, w) -> {
            String name = getText(etName);
            String dosage = getText(etDosage);

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(this, isBn ? "অনুগ্রহ করে ওষুধের নাম লিখুন" : "Please enter medicine name", Toast.LENGTH_SHORT).show();
                return;
            }

            MedicineReminder medicine = buildMedicine(existing == null ? -1 : existing.getId(), name, dosage, pickerHour, pickerMinute, pickerAmPm, existing == null || existing.isEnabled());

            if (existing != null) {
                medicine.setVibrationMode(existing.getVibrationMode());
            }

            showVibrationDialog(medicine, existing != null);
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null);

        if (existing != null) {
            builder.setNeutralButton(isBn ? "মুছুন" : "Delete", (d, w) -> confirmDelete(existing));
        }

        builder.show();
    }

    @Override
    public void onEditClicked(MedicineReminder med) {
        showMedicineDialog(med);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @Override
    public void onToggleClicked(MedicineReminder med) {
        boolean newState = !med.isEnabled();
        med.setEnabled(newState);
        db.updateMedicineEnabled(med.getId(), newState);

        if (newState) {
            scheduleAlarm(med);
        } else {
            cancelAlarm(med.getId());
        }

        int pos = medicines.indexOf(med);
        if (pos >= 0) {
            adapter.notifyItemChanged(pos);
        }
    }

    @Override
    public void onDeleteClicked(MedicineReminder med) {
        confirmDelete(med);
    }

    private void confirmDelete(@NonNull MedicineReminder med) {
        boolean isBn = LanguageHelper.isBangla(this);
        new MaterialAlertDialogBuilder(this).setTitle(isBn ? "রিমাইন্ডার মুছুন" : "Delete Reminder").setMessage((isBn ? "মুছে ফেলবেন " : "Delete ") + med.getName() + "?").setPositiveButton(isBn ? "মুছুন" : "Delete", (d, w) -> {
            cancelAlarm(med.getId());
            db.deleteMedicine(med.getId());

            int pos = medicines.indexOf(med);
            if (pos >= 0) {
                medicines.remove(pos);
                adapter.notifyItemRemoved(pos);
                updateEmptyState();
            }
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null).show();
    }

    // ─── Vibration ───────────────────────────────────────────────────────────

    private void showVibrationDialog(MedicineReminder med, boolean isEdit) {
        pendingMedicine = med;
        pendingIsEdit = isEdit;

        VibrationDialogFragment dialog = VibrationDialogFragment.newInstance(med.getName());

        dialog.setOnVibrationSelectedListener(mode -> {
            if (pendingMedicine == null) return;

            pendingMedicine.setVibrationMode(mode);

            if (pendingIsEdit) {
                cancelAlarm(pendingMedicine.getId());
                db.updateMedicine(pendingMedicine);
                updateMedicineInList(pendingMedicine);
            } else {
                long id = db.addMedicine(pendingMedicine);
                pendingMedicine.setId((int) id);
                medicines.add(0, pendingMedicine);
                adapter.notifyItemInserted(0);
                recyclerView.scrollToPosition(0);
            }

            if (pendingMedicine.isEnabled()) {
                scheduleAlarm(pendingMedicine);
            }

            updateEmptyState();

            Toast.makeText(this, pendingIsEdit ? (LanguageHelper.isBangla(this) ? "রিমাইন্ডার আপডেট হয়েছে!" : "Reminder updated!") : (LanguageHelper.isBangla(this) ? pendingMedicine.getTime() + " এর জন্য রিমাইন্ডার সেট হয়েছে" : "Reminder set for " + pendingMedicine.getTime()), Toast.LENGTH_SHORT).show();

            pendingMedicine = null;
        });

        dialog.show(getSupportFragmentManager(), "vibration_dialog");
    }

    private void updateMedicineInList(MedicineReminder medicine) {
        int pos = indexOfMedicineId(medicine.getId());
        if (pos >= 0) {
            medicines.set(pos, medicine);
            adapter.notifyItemChanged(pos);
        }
    }

    // ─── Data ────────────────────────────────────────────────────────────────

    @SuppressLint("NotifyDataSetChanged")
    private void loadMedicines() {
        medicines.clear();
        medicines.addAll(db.getAllMedicines());
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = medicines.isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void rescheduleAllEnabledMedicines() {
        for (MedicineReminder med : db.getAllMedicines()) {
            if (med.isEnabled()) {
                scheduleAlarm(med);
            } else {
                cancelAlarm(med.getId());
            }
        }
    }

    private int indexOfMedicineId(int id) {
        for (int i = 0; i < medicines.size(); i++) {
            if (medicines.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    // ─── Builders ────────────────────────────────────────────────────────────

    private MedicineReminder buildMedicine(int id, String name, String dosage, NumberPicker pickerHour, NumberPicker pickerMinute, NumberPicker pickerAmPm, boolean enabled) {
        int hour12 = pickerHour.getValue();
        int minute = pickerMinute.getValue();
        int ampm = pickerAmPm.getValue();

        int hour24 = ampm == 0 ? (hour12 == 12 ? 0 : hour12) : (hour12 == 12 ? 12 : hour12 + 12);

        String time = String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, ampm == 0 ? "AM" : "PM");

        MedicineReminder med = new MedicineReminder(name, dosage, time, hour24, minute);

        if (id != -1) {
            med.setId(id);
        }

        med.setEnabled(enabled);
        return med;
    }

    private void setupPickers(NumberPicker hour, NumberPicker minute, NumberPicker amPm, int h, int m, int ap) {
        hour.setMinValue(1);
        hour.setMaxValue(12);
        hour.setValue(h);

        minute.setMinValue(0);
        minute.setMaxValue(59);
        minute.setValue(m);
        minute.setFormatter(v -> String.format(Locale.getDefault(), "%02d", v));

        amPm.setMinValue(0);
        amPm.setMaxValue(1);
        amPm.setDisplayedValues(new String[]{"AM", "PM"});
        amPm.setValue(ap);
    }

    // ─── Alarm Scheduling ────────────────────────────────────────────────────

    private void scheduleAlarm(@NonNull MedicineReminder med) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pi = createAlarmPendingIntent(med);
        Calendar triggerTime = buildTriggerTime(med);

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    private PendingIntent createAlarmPendingIntent(MedicineReminder med) {
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicine_name", med.getName());
        intent.putExtra("dosage", med.getDosage());
        intent.putExtra("vibration_mode", med.getVibrationMode());
        intent.putExtra("notif_id", med.getId());

        return PendingIntent.getBroadcast(this, med.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private Calendar buildTriggerTime(MedicineReminder med) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, med.getHour());
        cal.set(Calendar.MINUTE, med.getMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        return cal;
    }

    private void cancelAlarm(int id) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pi = PendingIntent.getBroadcast(this, id, new Intent(this, AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        alarmManager.cancel(pi);
    }

    // ─── Notification Channels ───────────────────────────────────────────────

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        nm.createNotificationChannel(createNormalChannel());
        nm.createNotificationChannel(createAlarmChannel());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createNormalChannel() {
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes attrs = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION).build();

        NotificationChannel channel = new NotificationChannel("medicine_reminder_normal", "Medicine Reminder", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Daily medicine reminder");
        channel.setSound(sound, attrs);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 1000});
        return channel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel createAlarmChannel() {
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes attrs = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_ALARM).build();

        NotificationChannel channel = new NotificationChannel("medicine_reminder_alarm", "Medicine Alarm", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Strong alarm-style medicine reminder");
        channel.setSound(sound, attrs);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 400, 200, 600});
        return channel;
    }
}