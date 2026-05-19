package com.xerotrust.vitaltrack.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.Objects;

public class CalorieCalculatorActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextInputEditText etAge, etWeight;

    // Height inputs
    private TextInputEditText etHeightCm;
    private TextInputEditText etHeightFeet, etHeightInch;
    private TextInputLayout tilHeightCm;
    private View layoutHeightFeet;
    private TextView btnUnitCm, btnUnitFeet;

    private Spinner spinnerGender, spinnerActivity;
    private CardView cardResult;
    private TextView tvMaintain, tvMildLoss, tvLoss, tvGain;

    private boolean isUnitCm = false;

    private final float[] activityFactors = {1.2f, 1.375f, 1.55f, 1.725f, 1.9f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calorie_calculator);

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

        etAge = findViewById(R.id.et_age);
        etWeight = findViewById(R.id.et_weight);

        // Height unit toggle
        etHeightCm = findViewById(R.id.et_height_cm);
        etHeightFeet = findViewById(R.id.et_height_feet);
        etHeightInch = findViewById(R.id.et_height_inch);
        tilHeightCm = findViewById(R.id.til_height_cm);
        layoutHeightFeet = findViewById(R.id.layout_height_feet);
        btnUnitCm = findViewById(R.id.btn_unit_cm);
        btnUnitFeet = findViewById(R.id.btn_unit_feet);

        spinnerGender = findViewById(R.id.spinner_gender);
        spinnerActivity = findViewById(R.id.spinner_activity);
        cardResult = findViewById(R.id.card_result);
        tvMaintain = findViewById(R.id.tv_maintain);
        tvMildLoss = findViewById(R.id.tv_mild_loss);
        tvLoss = findViewById(R.id.tv_loss);
        tvGain = findViewById(R.id.tv_gain);

        btnUnitCm.setOnClickListener(v -> setHeightUnit(true));
        btnUnitFeet.setOnClickListener(v -> setHeightUnit(false));

        // Pre-fill from profile
        AppPrefs prefs = new AppPrefs(this);
        if (prefs.getAge() > 0) etAge.setText(String.valueOf(prefs.getAge()));
        if (prefs.getWeight() > 0) etWeight.setText(String.valueOf(prefs.getWeight()));

        // Default Feet & Inch then pre-fill saved height
        setHeightUnit(false);
        if (prefs.getHeight() > 0) {
            float totalInches = prefs.getHeight() / 2.54f;
            int feet = (int) (totalInches / 12);
            float inch = totalInches - (feet * 12);
            etHeightFeet.setText(String.valueOf(feet));
            etHeightInch.setText(String.format(java.util.Locale.getDefault(), "%.1f", inch));
        }

        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this, R.array.gender_options, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        ArrayAdapter<CharSequence> activityAdapter = ArrayAdapter.createFromResource(this, R.array.activity_levels, android.R.layout.simple_spinner_item);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerActivity.setAdapter(activityAdapter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
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
        // Normalize inputs before parsing to handle Bengali/localized numerals
        String ageStr = normalizeDigits(Objects.requireNonNull(etAge.getText()).toString().trim());
        String weightStr = normalizeDigits(Objects.requireNonNull(etWeight.getText()).toString().trim());

        if (TextUtils.isEmpty(ageStr) || TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, isBn ? "সকল ঘর পূরণ করুন" : "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

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

        int genderPos = spinnerGender.getSelectedItemPosition();
        int activityPos = spinnerActivity.getSelectedItemPosition();

        if (genderPos == 0 || activityPos == 0) {
            Toast.makeText(this, isBn ? "অনুগ্রহ করে লিঙ্গ ও কার্যকলাপের মাত্রা নির্বাচন করুন" : "Please select gender and activity level", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageStr);
        float height = resolveHeightCm();   // always in cm for Mifflin-St Jeor
        float weight = Float.parseFloat(weightStr);

        // Mifflin-St Jeor Equation (height in cm, weight in kg)
        float bmr;
        if (genderPos == 1) { // Male
            bmr = (10f * weight) + (6.25f * height) - (5f * age) + 5f;
        } else {              // Female
            bmr = (10f * weight) + (6.25f * height) - (5f * age) - 161f;
        }

        float activityFactor = activityFactors[activityPos - 1];
        float tdee = bmr * activityFactor;

        tvMaintain.setText(String.format("%.0f kcal/day", tdee));
        tvMildLoss.setText(String.format("%.0f kcal/day", tdee - 250));
        tvLoss.setText(String.format("%.0f kcal/day", tdee - 500));
        tvGain.setText(String.format("%.0f kcal/day", tdee + 250));
        cardResult.setVisibility(View.VISIBLE);
    }
}