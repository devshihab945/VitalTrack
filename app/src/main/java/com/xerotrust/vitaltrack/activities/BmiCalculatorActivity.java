package com.xerotrust.vitaltrack.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

public class BmiCalculatorActivity extends AppCompatActivity {

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

    private TextInputEditText etWeight;
    private CardView cardResult;
    private TextView tvBmiValue, tvBmiCategory;

    private boolean isUnitCm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bmi_calculator);

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

        // Height unit toggle views
        etHeightCm = findViewById(R.id.et_height_cm);
        etHeightFeet = findViewById(R.id.et_height_feet);
        etHeightInch = findViewById(R.id.et_height_inch);
        tilHeightCm = findViewById(R.id.til_height_cm);
        layoutHeightFeet = findViewById(R.id.layout_height_feet);
        btnUnitCm = findViewById(R.id.btn_unit_cm);
        btnUnitFeet = findViewById(R.id.btn_unit_feet);

        etWeight = findViewById(R.id.et_weight);
        cardResult = findViewById(R.id.card_result);
        tvBmiValue = findViewById(R.id.tv_bmi_value);
        tvBmiCategory = findViewById(R.id.tv_bmi_category);

        btnUnitCm.setOnClickListener(v -> setHeightUnit(true));
        btnUnitFeet.setOnClickListener(v -> setHeightUnit(false));

        ImageView ivBack = findViewById(R.id.iv_back);
        ivBack.setOnClickListener(v -> finish());

        // Pre-fill from profile
        AppPrefs prefs = new AppPrefs(this);
        if (prefs.getWeight() > 0) etWeight.setText(String.valueOf(prefs.getWeight()));

        // Default to Feet & Inch then pre-fill saved height
        setHeightUnit(false);
        if (prefs.getHeight() > 0) {
            float totalInches = prefs.getHeight() / 2.54f;
            int feet = (int) (totalInches / 12);
            float inch = totalInches - (feet * 12);
            etHeightFeet.setText(String.valueOf(feet));
            etHeightInch.setText(String.format(java.util.Locale.getDefault(), "%.1f", inch));
        }

        calculateBmi();

        CardView btnCalculate = findViewById(R.id.btn_calculate);
        btnCalculate.setOnClickListener(v -> calculateBmi());
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
                // getNumericValue returns the correct 0-9 integer for any Unicode digit
                sb.append(Character.getNumericValue(c));
            } else {
                sb.append(c); // preserve '.', '-', whitespace, etc.
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

    private void calculateBmi() {
        boolean isBn = LanguageHelper.isBangla(this);
        // Normalize weight string before parsing to handle Bengali/localized numerals
        String weightStr = normalizeDigits(etWeight.getText().toString().trim());

        if (TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, isBn ? "অনুগ্রহ করে ওজন লিখুন" : "Please enter weight", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate height fields
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

        float heightCm = resolveHeightCm();
        float weightKg = Float.parseFloat(weightStr);

        if (heightCm <= 0 || weightKg <= 0) {
            Toast.makeText(this, isBn ? "সঠিক মান লিখুন" : "Please enter valid values", Toast.LENGTH_SHORT).show();
            return;
        }

        float heightM = heightCm / 100f;
        float bmi = weightKg / (heightM * heightM);

        String category;
        int color;
        if (bmi < 18.5f) {
            category = isBn ? "কম ওজন" : "Underweight";
            color = getResources().getColor(R.color.bmi_underweight, null);
        } else if (bmi < 25f) {
            category = isBn ? "স্বাভাবিক ওজন" : "Normal Weight";
            color = getResources().getColor(R.color.bmi_normal, null);
        } else if (bmi < 30f) {
            category = isBn ? "অতিরিক্ত ওজন" : "Overweight";
            color = getResources().getColor(R.color.bmi_overweight, null);
        } else {
            category = isBn ? "স্থূলতা" : "Obese";
            color = getResources().getColor(R.color.bmi_obese, null);
        }

        tvBmiValue.setText(String.format("%.1f", bmi));
        tvBmiCategory.setText(category);
        tvBmiCategory.setTextColor(color);
        tvBmiValue.setTextColor(color);
        cardResult.setVisibility(View.VISIBLE);

        new AppPrefs(this).setLastBmi(bmi);
    }
}