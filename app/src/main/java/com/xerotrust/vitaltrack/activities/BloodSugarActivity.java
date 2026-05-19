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
import com.xerotrust.vitaltrack.adapters.BloodSugarAdapter;
import com.xerotrust.vitaltrack.models.BloodSugarRecord;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.LanguageHelper;
import com.xerotrust.vitaltrack.utils.PremiumSnackbar;
import com.xerotrust.vitaltrack.utils.SnackbarType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BloodSugarActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextView tvLatestSugar, tvSugarStatus, tvEmpty;
    private RecyclerView recyclerSugar;
    private DatabaseHelper db;
    private List<BloodSugarRecord> records;
    private BloodSugarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blood_sugar);

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

        tvLatestSugar = findViewById(R.id.tv_latest_sugar);
        tvSugarStatus = findViewById(R.id.tv_sugar_status);
        tvEmpty = findViewById(R.id.tv_empty);
        recyclerSugar = findViewById(R.id.list_sugar);

        db = new DatabaseHelper(this);
        records = new ArrayList<>();

        adapter = new BloodSugarAdapter(records, new BloodSugarAdapter.OnActionListener() {
            @Override
            public void onEdit(int pos) {
                showAddEditDialog(records.get(pos));
            }

            @Override
            public void onDelete(int pos) {
                showDeleteConfirm(records.get(pos));
            }
        });

        recyclerSugar.setLayoutManager(new LinearLayoutManager(this));
        recyclerSugar.setAdapter(adapter);
        recyclerSugar.setNestedScrollingEnabled(false);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> showAddEditDialog(null));

        loadRecords();
    }

    private void loadRecords() {
        records.clear();
        records.addAll(db.getAllSugarRecords());
        adapter.notifyDataSetChanged();

        if (!records.isEmpty()) {
            BloodSugarRecord latest = records.get(0);
            tvLatestSugar.setText(String.format("%.0f", latest.getValue()));
            tvSugarStatus.setText(latest.getStatus() + "  •  " + getString(R.string.normal_range_sugar));
            tvEmpty.setVisibility(View.GONE);
            recyclerSugar.setVisibility(View.VISIBLE);
        } else {
            tvLatestSugar.setText("--");
            tvSugarStatus.setText(getString(R.string.normal_range_sugar));
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerSugar.setVisibility(View.GONE);
        }
    }

    private void showAddEditDialog(BloodSugarRecord existing) {
        boolean isBn = LanguageHelper.isBangla(this);

        Dialog dialog = new Dialog(this, R.style.RoundedDialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_add_sugar);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        TextInputEditText etSugar = dialog.findViewById(R.id.et_sugar);
        TextInputEditText etNote = dialog.findViewById(R.id.et_note);
        MaterialButton btnSave = dialog.findViewById(R.id.btn_save);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_cancel);

        boolean isEdit = existing != null;
        tvTitle.setText(isEdit
                ? (isBn ? "রক্তে শর্করা সম্পাদনা করুন" : "Edit Blood Sugar")
                : (isBn ? "রক্তে শর্করা যোগ করুন" : "Add Blood Sugar"));

        if (isEdit) {
            etSugar.setText(String.format("%.0f", existing.getValue()));
            etNote.setText(existing.getNote());
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String sugarStr = etSugar.getText() != null ? etSugar.getText().toString().trim() : "";
            String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";

            if (TextUtils.isEmpty(sugarStr)) {
                PremiumSnackbar.show(findViewById(android.R.id.content),
                        isBn ? "রক্তে শর্করার মান লিখুন" : "Please enter blood sugar value",
                        SnackbarType.WARNING);
                return;
            }

            float value;
            try {
                value = Float.parseFloat(normalizeDigits(sugarStr));
            } catch (NumberFormatException e) {
                PremiumSnackbar.show(findViewById(android.R.id.content),
                        isBn ? "সংখ্যার ফরম্যাট সঠিক নয়" : "Invalid number format",
                        SnackbarType.ERROR);
                return;
            }

            if (isEdit) {
                existing.setValue(value);
                existing.setNote(note);
                db.updateSugarRecord(existing);
                PremiumSnackbar.show(findViewById(android.R.id.content),
                        isBn ? "রেকর্ড আপডেট হয়েছে!" : "Record updated!",
                        SnackbarType.SUCCESS);
            } else {
                String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
                db.addSugarRecord(new BloodSugarRecord(value, date, note));
                PremiumSnackbar.show(findViewById(android.R.id.content),
                        isBn ? "রেকর্ড সংরক্ষিত হয়েছে!" : "Record saved!",
                        SnackbarType.SUCCESS);
            }

            dialog.dismiss();
            loadRecords();
        });

        dialog.show();
    }

    private void showDeleteConfirm(BloodSugarRecord record) {
        boolean isBn = LanguageHelper.isBangla(this);
        new AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle(isBn ? "রেকর্ড মুছুন" : "Delete Record")
                .setMessage(isBn
                        ? String.format("%.0f", record.getValue()) + " mg/dL রিডিং মুছে ফেলবেন?"
                        : "Delete " + String.format("%.0f", record.getValue()) + " mg/dL reading?")
                .setPositiveButton(isBn ? "মুছুন" : "Delete", (d, w) -> {
                    db.deleteSugarRecord(record.getId());
                    loadRecords();
                    PremiumSnackbar.show(findViewById(android.R.id.content),
                            isBn ? "রেকর্ড মুছে গেছে" : "Record deleted",
                            SnackbarType.ERROR);
                })
                .setNegativeButton(isBn ? "বাতিল" : "Cancel", null)
                .show();
    }

    /**
     * Normalizes Bengali/any Unicode digit characters to ASCII
     * so Float.parseFloat() doesn't crash on localized numerals.
     */
    private String normalizeDigits(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) sb.append(Character.getNumericValue(c));
            else sb.append(c);
        }
        return sb.toString();
    }
}