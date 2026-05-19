package com.xerotrust.vitaltrack.activities;

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
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

public class CaloriesBurnedActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextInputEditText etWeight, etSteps;
    private CardView cardResult;
    private TextView tvCaloriesResult, tvBasedOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calories_burned);

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

        etWeight = findViewById(R.id.et_weight);
        etSteps = findViewById(R.id.et_steps);
        cardResult = findViewById(R.id.card_result);
        tvCaloriesResult = findViewById(R.id.tv_calories_result);
        tvBasedOn = findViewById(R.id.tv_based_on);

        AppPrefs prefs = new AppPrefs(this);

        // Pre-fill weight from profile
        if (prefs.getWeight() > 0) etWeight.setText(String.valueOf(prefs.getWeight()));

        // Auto-fill steps: walk + run combined from Step Counter
        int walkSteps = prefs.getModeSteps("walk");
        int runSteps = prefs.getModeSteps("run");
        int totalActivitySteps = walkSteps + runSteps;

        if (totalActivitySteps > 0) {
            etSteps.setText(String.valueOf(totalActivitySteps));
        } else {
            // Fallback: use general today step count
            int todaySteps = prefs.getTodaySteps();
            if (todaySteps > 0) etSteps.setText(String.valueOf(todaySteps));
        }

        // Auto-calculate if both fields are filled
        if (etSteps.getText() != null && !etSteps.getText().toString().isEmpty() && etWeight.getText() != null && !etWeight.getText().toString().isEmpty()) {
            calculate();
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_calculate).setOnClickListener(v -> calculate());
    }

    private void calculate() {
        String weightStr = etWeight.getText().toString().trim();
        String stepsStr = etSteps.getText().toString().trim();

        if (TextUtils.isEmpty(weightStr) || TextUtils.isEmpty(stepsStr)) {
            Toast.makeText(this, LanguageHelper.isBangla(this) ? "অনুগ্রহ করে সকল ঘর পূরণ করুন" : "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        float weight = Float.parseFloat(weightStr);
        int steps = Integer.parseInt(stepsStr);

        // Distance-based calorie calculation (more accurate)
        // Avg stride length ~0.762m, MET factor 1.036 for walking
        float distanceKm = steps * 0.000762f;
        float calories = distanceKm * weight * 1.036f;

        tvCaloriesResult.setText(String.format("%.0f", calories));
        tvBasedOn.setText(String.format("Based on %.0fkg · %,d steps (walk + run)", weight, steps));
        cardResult.setVisibility(View.VISIBLE);
    }
}