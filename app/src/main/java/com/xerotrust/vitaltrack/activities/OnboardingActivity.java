package com.xerotrust.vitaltrack.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

public class OnboardingActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    private ViewPager2 viewPager;
    CardView btnNext;
    private TextView tvNextText, tvSkip, tvTerms;
    private ImageView ivBack;
    private LinearLayout dotsLayout;

    private String[] titles;
    private String[] descriptions;
    private final int[] images = {R.drawable.onboard_1, R.drawable.onboard_2, R.drawable.onboard_3};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // FIX 1: Set status bar color BEFORE EdgeToEdge
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

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

        viewPager = findViewById(R.id.viewpager);
        btnNext = findViewById(R.id.btn_next);
        tvNextText = findViewById(R.id.tv_next_text);
        tvSkip = findViewById(R.id.tv_skip);
        tvTerms = findViewById(R.id.tv_terms);
        ivBack = findViewById(R.id.iv_back);
        dotsLayout = findViewById(R.id.layout_dots);

        boolean isBn = LanguageHelper.isBangla(this);

        titles = isBn ? new String[]{
                "আপনার দৈনিক কার্যকলাপ ট্র্যাক করুন",
                "প্রতিদিন পানি পান করুন",
                "আপনার স্বাস্থ্য পর্যবেক্ষণ করুন"
        } : new String[]{
                "Track Your Daily Activity",
                "Stay Hydrated Every Day",
                "Monitor Your Health"
        };

        descriptions = isBn ? new String[]{
                "প্রতিদিনের পদক্ষেপ, ক্যালোরি ও দূরত্ব ট্র্যাক করুন। সক্রিয় থাকুন এবং সুস্থ জীবনের দিকে এগিয়ে যান।",
                "প্রতিদিনের পানি গ্রহণ ট্র্যাক করুন এবং হাইড্রেশনের লক্ষ্য পূরণ করুন। সুস্থ হাইড্রেশন শরীরকে সতেজ রাখে।",
                "হার্ট রেট, রক্তচাপ ও ওষুধের রিমাইন্ডার এক জায়গায় ট্র্যাক করুন। সচেতন থাকুন ও স্বাস্থ্য নিয়ন্ত্রণ করুন।"
        } : new String[]{
                "Monitor your steps, calories burned, and distance every day. Stay active and keep your body moving toward a healthier lifestyle.",
                "Track your daily water intake and reach your hydration goals. Healthy hydration helps keep your body energized and refreshed.",
                "Track heart rate, blood pressure, and medicine reminders all in one place. Stay informed and take control of your health."
        };

        viewPager.setAdapter(new OnboardingAdapter());
        viewPager.post(() -> {
            setupDots(0);
            updateUI(0);
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                setupDots(position);
                updateUI(position);
            }
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < titles.length - 1) {
                viewPager.setCurrentItem(current + 1);
            } else {
                finishOnboarding();
            }
        });

        ivBack.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1);
            }
        });

        tvSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void updateUI(int position) {
        boolean isLast = position == titles.length - 1;
        boolean isFirst = position == 0;

        ivBack.setVisibility(isFirst ? View.GONE : View.VISIBLE);
        tvSkip.setVisibility(isLast ? View.GONE : View.VISIBLE);
        tvNextText.setText(isLast
                ? (LanguageHelper.isBangla(this) ? "শুরু করুন" : "Get Started")
                : (LanguageHelper.isBangla(this) ? "পরবর্তী →" : "Next →"));
        tvTerms.setVisibility(isLast ? View.VISIBLE : View.GONE);
    }

    private void finishOnboarding() {
        new AppPrefs(this).setOnboardingDone(true);
        startActivity(new Intent(this, ProfileSetupActivity.class));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }

    private void setupDots(int currentPage) {
        dotsLayout.removeAllViews();
        int margin = dpToPx(5);

        for (int i = 0; i < titles.length; i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params;

            if (i == currentPage) {
                params = new LinearLayout.LayoutParams(dpToPx(24), dpToPx(8));
                dot.setBackgroundResource(R.drawable.dot_active);
            } else {
                params = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
                dot.setBackgroundResource(R.drawable.dot_inactive);
            }

            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dotsLayout.addView(dot);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.VH> {

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_onboarding, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            holder.image.setImageResource(images[position]);
            holder.title.setText(titles[position]);
            holder.desc.setText(descriptions[position]);
        }

        @Override
        public int getItemCount() {
            return titles.length;
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, desc;

            VH(@NonNull View v) {
                super(v);
                image = v.findViewById(R.id.iv_onboard_image);
                title = v.findViewById(R.id.tv_title);
                desc = v.findViewById(R.id.tv_description);
            }
        }
    }
}