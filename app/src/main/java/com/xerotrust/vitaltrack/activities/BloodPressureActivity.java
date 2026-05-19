package com.xerotrust.vitaltrack.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.adapters.BloodPressureAdapter;
import com.xerotrust.vitaltrack.models.BloodPressureRecord;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.LanguageHelper;
import com.xerotrust.vitaltrack.utils.PremiumSnackbar;
import com.xerotrust.vitaltrack.utils.SnackbarType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BloodPressureActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextView tvLatestBP, tvBpStatus, tvEmpty;
    private RecyclerView recyclerBP;
    private DatabaseHelper db;
    private List<BloodPressureRecord> records;
    private BloodPressureAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        EdgeToEdge.enable(this);
        setContentView(R.layout.fragment_blood_pressure);

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

        tvLatestBP = findViewById(R.id.tv_latest_bp);
        tvBpStatus = findViewById(R.id.tv_bp_status);
        tvEmpty = findViewById(R.id.tv_empty);
        recyclerBP = findViewById(R.id.list_bp);

        db = new DatabaseHelper(this);
        records = new ArrayList<>();

        adapter = new BloodPressureAdapter(records, new BloodPressureAdapter.OnActionListener() {
            @Override
            public void onEdit(int pos) {
                showAddEditDialog(records.get(pos));
            }

            @Override
            public void onDelete(int pos) {
                showDeleteConfirm(records.get(pos));
            }
        });

        recyclerBP.setLayoutManager(new LinearLayoutManager(this));
        recyclerBP.setAdapter(adapter);
        recyclerBP.setNestedScrollingEnabled(false);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddEditDialog(null));

        loadRecords();
    }

    private void loadRecords() {
        records.clear();
        records.addAll(db.getAllBPRecords());
        adapter.notifyDataSetChanged();

        if (!records.isEmpty()) {
            BloodPressureRecord latest = records.get(0);
            tvLatestBP.setText(latest.getDisplayBP());
            tvBpStatus.setText(latest.getCategory());
            tvEmpty.setVisibility(View.GONE);
            recyclerBP.setVisibility(View.VISIBLE);
        } else {
            tvLatestBP.setText("--/--");
            tvBpStatus.setText(R.string.no_data);
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerBP.setVisibility(View.GONE);
        }
    }

    private void showAddEditDialog(BloodPressureRecord existing) {
        boolean isBn = LanguageHelper.isBangla(this);
        Dialog dialog = new Dialog(this, R.style.RoundedDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_bp);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextInputEditText etSystolic = dialog.findViewById(R.id.et_systolic);
        TextInputEditText etDiastolic = dialog.findViewById(R.id.et_diastolic);
        TextInputEditText etPulse = dialog.findViewById(R.id.et_pulse);
        TextInputEditText etNote = dialog.findViewById(R.id.et_note);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);

        boolean isEdit = existing != null;
        tvTitle.setText(isEdit ? (isBn ? "রক্তচাপ সম্পাদনা করুন" : "Edit Blood Pressure") : (isBn ? "রক্তচাপ যোগ করুন" : "Add Blood Pressure"));

        // Set hints
        etSystolic.setHint(isBn ? "সিস্টোলিক (মিমি/এইচজি)" : "Systolic (mmHg)");
        etDiastolic.setHint(isBn ? "ডায়াস্টোলিক (মিমি/এইচজি)" : "Diastolic (mmHg)");
        etPulse.setHint(isBn ? "পালস (bpm)" : "Pulse (bpm)");
        etNote.setHint(isBn ? "নোট (ঐচ্ছিক)" : "Note (optional)");

        btnSave.setText(isBn ? "সংরক্ষণ করুন" : "Save");
        btnCancel.setText(isBn ? "বাতিল" : "Cancel");

        if (isEdit) {
            etSystolic.setText(String.valueOf(existing.getSystolic()));
            etDiastolic.setText(String.valueOf(existing.getDiastolic()));
            etPulse.setText(String.valueOf(existing.getPulse()));
            etNote.setText(existing.getNote());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String sys = etSystolic.getText() != null ? etSystolic.getText().toString().trim() : "";
            String dia = etDiastolic.getText() != null ? etDiastolic.getText().toString().trim() : "";
            String pulse = etPulse.getText() != null ? etPulse.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (TextUtils.isEmpty(sys) || TextUtils.isEmpty(dia) || TextUtils.isEmpty(pulse)) {
                PremiumSnackbar.show(findViewById(android.R.id.content), isBn ? "সিস্টোলিক, ডায়াস্টোলিক ও পালস পূরণ করুন" : "Please fill Systolic, Diastolic and Pulse", SnackbarType.WARNING);
                return;
            }
            int sysVal, diaVal, pulseVal;
            try {
                sysVal = Integer.parseInt(normalizeDigits(sys));
                diaVal = Integer.parseInt(normalizeDigits(dia));
                pulseVal = Integer.parseInt(normalizeDigits(pulse));
            } catch (NumberFormatException e) {
                PremiumSnackbar.show(findViewById(android.R.id.content), isBn ? "অবৈধ সংখ্যা ফরম্যাট" : "Invalid number format", SnackbarType.ERROR);
                return;
            }

            if (isEdit) {
                existing.setSystolic(sysVal);
                existing.setDiastolic(diaVal);
                existing.setPulse(pulseVal);
                existing.setNote(note);
                db.updateBPRecord(existing);
                PremiumSnackbar.show(findViewById(android.R.id.content), isBn ? "রেকর্ড আপডেট হয়েছে!" : "Record updated!", SnackbarType.SUCCESS);
            } else {
                String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                db.addBPRecord(new BloodPressureRecord(sysVal, diaVal, pulseVal, date, note));
                PremiumSnackbar.show(findViewById(android.R.id.content), isBn ? "রেকর্ড সংরক্ষিত হয়েছে!" : "Record saved!", SnackbarType.SUCCESS);
            }
            dialog.dismiss();
            loadRecords();
        });

        dialog.show();
    }

    private String normalizeDigits(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) sb.append(Character.getNumericValue(c));
            else sb.append(c);
        }
        return sb.toString();
    }

    private void showDeleteConfirm(BloodPressureRecord record) {
        boolean isBn = LanguageHelper.isBangla(this);
        new AlertDialog.Builder(this, R.style.RoundedDialog).setTitle(isBn ? "রেকর্ড মুছুন" : "Delete Record").setMessage(isBn ? record.getDisplayBP() + " mmHg রিডিং মুছে ফেলবেন?" : "Delete " + record.getDisplayBP() + " mmHg reading?").setPositiveButton(isBn ? "মুছুন" : "Delete", (d, w) -> {
            db.deleteBPRecord(record.getId());
            loadRecords();
            PremiumSnackbar.show(findViewById(android.R.id.content), isBn ? "রেকর্ড মুছে গেছে" : "Record deleted", SnackbarType.ERROR);
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null).show();
    }
}