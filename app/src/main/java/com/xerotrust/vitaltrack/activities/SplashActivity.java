package com.xerotrust.vitaltrack.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

/**
 * SplashActivity — Entry point
 * <p>
 * Flow logic (in order):
 * 1. Onboarding not done → OnboardingActivity
 * 2. Onboarding done, profile not done → ProfileSetupActivity
 * 3. Everything done → MainActivity
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        View decor = window.getDecorView();
        decor.post(() -> {
            window.setStatusBarColor(getResources().getColor(R.color.primary, getTheme()));
            WindowCompat.getInsetsController(window, decor).setAppearanceLightStatusBars(false);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ── Animate splash elements ──────────────────────────────────
        View layoutCenter = findViewById(R.id.layout_center);
        View ivHeart = findViewById(R.id.iv_heart);
        View ivDrop = findViewById(R.id.iv_drop);
        View ivChart = findViewById(R.id.iv_chart);
        View layoutDots = findViewById(R.id.layout_dots);
        ProgressBar progress = findViewById(R.id.progress_splash);

        layoutCenter.setAlpha(0f);
        ivHeart.setAlpha(0f);
        ivDrop.setAlpha(0f);
        ivChart.setAlpha(0f);
        layoutDots.setAlpha(0f);

        layoutCenter.animate().alpha(1f).setDuration(800).setStartDelay(200).start();
        ivHeart.animate().alpha(1f).setDuration(600).setStartDelay(500).start();
        ivDrop.animate().alpha(1f).setDuration(600).setStartDelay(600).start();
        ivChart.animate().alpha(1f).setDuration(600).setStartDelay(700).start();
        layoutDots.animate().alpha(1f).setDuration(600).setStartDelay(700).start();

        ObjectAnimator progressAnim = ObjectAnimator.ofInt(progress, "progress", 0, 100);
        progressAnim.setDuration(2200);
        progressAnim.setStartDelay(300);
        progressAnim.setInterpolator(new DecelerateInterpolator());
        progressAnim.start();

        // ── Navigate after 2.6 s ────────────────────────────────────
        new Handler().postDelayed(this::navigateNext, 2600);
    }

    private void navigateNext() {
        AppPrefs appPrefs = new AppPrefs(this);

        Intent intent;

        if (!appPrefs.isOnboardingDone()) {
            // ① Onboarding not done yet
            intent = new Intent(this, OnboardingActivity.class);
        } else if (!appPrefs.isProfileDone()) {
            // ② Onboarding done but profile not set up
            intent = new Intent(this, ProfileSetupActivity.class);
        } else {
            // ③ All done → Home
            intent = new Intent(this, MainActivity.class);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}