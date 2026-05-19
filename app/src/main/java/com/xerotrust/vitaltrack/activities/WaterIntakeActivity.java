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

public class WaterIntakeActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private TextInputEditText etWeight;
    private CardView cardResult, cardTracking;
    private TextView tvWaterResult, tvGlasses, tvProgressText, tvGlassCount;
    private ProgressBar progressWater;
    private AppPrefs prefs;
    private int totalGlasses = 8;
    private int currentGlasses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIX 1: Set status bar color BEFORE EdgeToEdge
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_water_intake);

        // Force re-apply your custom status bar color AFTER setContentView
        View decor = getWindow().getDecorView();
        decor.post(() -> {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
            WindowCompat.getInsetsController(getWindow(), decor).setAppearanceLightStatusBars(false); // false = white icons
        });

        // WindowInsets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        etWeight = findViewById(R.id.et_weight);
        cardResult = findViewById(R.id.card_result);
        cardTracking = findViewById(R.id.card_tracking);
        tvWaterResult = findViewById(R.id.tv_water_result);
        tvGlasses = findViewById(R.id.tv_glasses);
        tvProgressText = findViewById(R.id.tv_progress_text);
        tvGlassCount = findViewById(R.id.tv_glass_count);
        progressWater = findViewById(R.id.progress_water);

        prefs = new AppPrefs(this);
        if (prefs.getWeight() > 0) etWeight.setText(String.valueOf(prefs.getWeight()));
        currentGlasses = prefs.getWaterGlasses();

        calculate();

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_calculate).setOnClickListener(v -> calculate());
        findViewById(R.id.btn_add_glass).setOnClickListener(v -> addGlass());
        findViewById(R.id.btn_remove_glass).setOnClickListener(v -> removeGlass());
    }

    private void calculate() {
        String weightStr = etWeight.getText().toString().trim();
        if (TextUtils.isEmpty(weightStr)) {
            Toast.makeText(this, "Please enter your weight", Toast.LENGTH_SHORT).show();
            return;
        }

        float weight = Float.parseFloat(weightStr);
        // 35ml per kg body weight
        float dailyWaterMl = weight * 35f;
        float dailyWaterL = dailyWaterMl / 1000f;
        totalGlasses = Math.round(dailyWaterMl / 250f);

        tvWaterResult.setText(String.format("%.1f L", dailyWaterL));
        tvGlasses.setText(getString(R.string.about_n_glasses, totalGlasses));
        cardResult.setVisibility(View.VISIBLE);
        cardTracking.setVisibility(View.VISIBLE);
        updateTracking();
    }

    private void addGlass() {
        if (currentGlasses < totalGlasses + 4) {
            currentGlasses++;
            prefs.setWaterGlasses(currentGlasses);
            updateTracking();
        }
    }

    private void removeGlass() {
        if (currentGlasses > 0) {
            currentGlasses--;
            prefs.setWaterGlasses(currentGlasses);
            updateTracking();
        }
    }

    private void updateTracking() {
        tvGlassCount.setText(String.valueOf(currentGlasses));
        tvProgressText.setText(currentGlasses + " / " + totalGlasses + " glasses");
        int percent = totalGlasses > 0 ? Math.min((currentGlasses * 100) / totalGlasses, 100) : 0;
        progressWater.setProgress(percent);
    }
}
