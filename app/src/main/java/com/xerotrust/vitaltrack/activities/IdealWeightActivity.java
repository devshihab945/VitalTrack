package com.xerotrust.vitaltrack.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

public class IdealWeightActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    // Height inputs
    private TextInputEditText etHeightCm;
    private TextInputEditText etHeightFeet, etHeightInch;
    private TextInputLayout tilHeightCm;
    private View layoutHeightFeet;
    private TextView btnUnitCm, btnUnitFeet;

    private Spinner spinnerGender;
    private CardView cardResult;
    private TextView tvIdealWeight, tvFormulaName;

    private boolean isUnitCm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ideal_weight);

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

        // Height unit toggle
        etHeightCm = findViewById(R.id.et_height_cm);
        etHeightFeet = findViewById(R.id.et_height_feet);
        etHeightInch = findViewById(R.id.et_height_inch);
        tilHeightCm = findViewById(R.id.til_height_cm);
        layoutHeightFeet = findViewById(R.id.layout_height_feet);
        btnUnitCm = findViewById(R.id.btn_unit_cm);
        btnUnitFeet = findViewById(R.id.btn_unit_feet);

        spinnerGender = findViewById(R.id.spinner_gender);
        cardResult = findViewById(R.id.card_result);
        tvIdealWeight = findViewById(R.id.tv_ideal_weight);
        tvFormulaName = findViewById(R.id.tv_formula_name);

        btnUnitCm.setOnClickListener(v -> setHeightUnit(true));
        btnUnitFeet.setOnClickListener(v -> setHeightUnit(false));

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // Pre-fill from profile – default Feet & Inch
        setHeightUnit(false);
        AppPrefs prefs = new AppPrefs(this);
        if (prefs.getHeight() > 0) {
            float totalInches = prefs.getHeight() / 2.54f;
            int feet = (int) (totalInches / 12);
            float inch = totalInches - (feet * 12);
            etHeightFeet.setText(String.valueOf(feet));
            etHeightInch.setText(String.format(java.util.Locale.getDefault(), "%.1f", inch));
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(adapter);

        findViewById(R.id.btn_calculate).setOnClickListener(v -> calculate());
    }

    /**
     * Normalizes any Unicode/localized digit characters (e.g. Bengali ০-৯)
     * to their ASCII equivalents so Float.parseFloat() doesn't crash.
     */
    private String normalizeDigits(String input) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(Character.getNumericValue(c));
            } else {
                sb.append(c); // preserve '.', '-', etc.
            }
        }
        return sb.toString();
    }

    private void setHeightUnit(boolean cm) {
        isUnitCm = cm;
        btnUnitCm.setBackgroundResource(cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitCm.setTextColor(cm ? 0xFFFFFFFF : getColor(R.color.text_secondary));
        btnUnitFeet.setBackgroundResource(!cm ? R.drawable.rounded_button : android.R.color.transparent);
        btnUnitFeet.setTextColor(!cm ? 0xFFFFFFFF : getColor(R.color.text_secondary));
        tilHeightCm.setVisibility(cm ? View.VISIBLE : View.GONE);
        layoutHeightFeet.setVisibility(cm ? View.GONE : View.VISIBLE);
    }

    private float resolveHeightCm() {
        if (isUnitCm) {
            String s = normalizeDigits(etHeightCm.getText() != null ? etHeightCm.getText().toString().trim() : "");
            return s.isEmpty() ? 0 : Float.parseFloat(s);
        } else {
            String fStr = normalizeDigits(etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "0");
            String iStr = normalizeDigits(etHeightInch.getText() != null ? etHeightInch.getText().toString().trim() : "0");
            float feet = fStr.isEmpty() ? 0 : Float.parseFloat(fStr);
            float inches = iStr.isEmpty() ? 0 : Float.parseFloat(iStr);
            return (feet * 12f + inches) * 2.54f;
        }
    }

    @SuppressLint("DefaultLocale")
    private void calculate() {
        boolean isBn = LanguageHelper.isBangla(this);
        // Validate height
        if (!isUnitCm) {
            String fStr = normalizeDigits(etHeightFeet.getText() != null ? etHeightFeet.getText().toString().trim() : "");
            if (fStr.isEmpty()) {
                etHeightFeet.setError(isBn ? "ফুট লিখুন" : "Enter feet");
                return;
            }
        } else {
            String s = normalizeDigits(etHeightCm.getText() != null ? etHeightCm.getText().toString().trim() : "");
            if (s.isEmpty()) {
                etHeightCm.setError(isBn ? "উচ্চতা লিখুন" : "Enter height");
                return;
            }
        }

        String gender = spinnerGender.getSelectedItem().toString();
        if (gender.startsWith("Select") || gender.startsWith("নির্বাচন")) {
            Toast.makeText(this, isBn ? "অনুগ্রহ করে লিঙ্গ নির্বাচন করুন" : "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }

        float heightCm = resolveHeightCm();
        float heightInches = heightCm / 2.54f;

        // Devine Formula
        float idealMin, idealMax;
        if (gender.equals("Male")) {
            idealMin = 50f + 2.3f * (heightInches - 60);
            idealMax = idealMin + 5f;
        } else {
            idealMin = 45.5f + 2.3f * (heightInches - 60);
            idealMax = idealMin + 5f;
        }

        idealMin = Math.max(idealMin, 40f);

        tvIdealWeight.setText(String.format("%.0f – %.0f kg", idealMin, idealMax));
        boolean isMale = gender.equals("Male") || gender.equals("পুরুষ");
        tvFormulaName.setText(isMale ? getString(R.string.based_on_divine_male) : getString(R.string.based_on_divine_female));
        cardResult.setVisibility(View.VISIBLE);
    }
}