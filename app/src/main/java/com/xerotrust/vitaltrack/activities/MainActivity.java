package com.xerotrust.vitaltrack.activities;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.fragments.HomeFragment;
import com.xerotrust.vitaltrack.fragments.MedicineReminderFragment;
import com.xerotrust.vitaltrack.fragments.ProfileFragment;
import com.xerotrust.vitaltrack.fragments.StepCounterFragment;
import com.xerotrust.vitaltrack.fragments.WaterIntakeAlarmFragment;
import com.xerotrust.vitaltrack.utils.LanguageHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LanguageHelper.applyLanguage(newBase));
    }

    /**
     * Back-navigation history: stores previously selected nav item IDs
     */
    private final java.util.Deque<Integer> navHistory = new java.util.ArrayDeque<>();

    // Runtime permission launcher (AndroidX)
    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        // আপনি চাইলে এখানে result check করে toast দেখাতে পারেন
        // Boolean notifGranted = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, true);
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(false);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // 1) Ask runtime permissions needed by app features
        requestNeededPermissions();

        // 2) Prompt exact-alarm allow screen (Android 12+)
        ensureExactAlarmsAllowedIfNeeded();

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        boolean isBn = LanguageHelper.isBangla(this);
        if (isBn) {
            bottomNav.getMenu().findItem(R.id.nav_home).setTitle("হোম");
            bottomNav.getMenu().findItem(R.id.nav_reminders).setTitle("রিমাইন্ডার");
            bottomNav.getMenu().findItem(R.id.nav_activity).setTitle("অ্যাক্টিভিটি");
            bottomNav.getMenu().findItem(R.id.nav_water).setTitle("পানি");
            bottomNav.getMenu().findItem(R.id.nav_profile).setTitle("প্রোফাইল");
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            // Push current tab to history before switching (skip if same tab)
            int currentId = bottomNav.getSelectedItemId();
            if (currentId != id) {
                navHistory.push(currentId);
            }
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_activity) {
                loadFragment(new StepCounterFragment());
                return true;
            } else if (id == R.id.nav_water) {
                loadFragment(new WaterIntakeAlarmFragment());
                return true;
            } else if (id == R.id.nav_reminders) {
                loadFragment(new MedicineReminderFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        // Handle back gesture / button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int currentId = bottomNav.getSelectedItemId();

                if (currentId == R.id.nav_home) {
                    // On Home → show exit confirmation dialog
                    showExitDialog();
                } else {
                    // On any other tab → go back to previous tab, or home if no history
                    Integer prevId = navHistory.isEmpty() ? null : navHistory.poll();
                    int targetId = (prevId != null) ? prevId : R.id.nav_home;
                    // Clear history up to home to avoid stale entries
                    if (targetId == R.id.nav_home) navHistory.clear();
                    bottomNav.setSelectedItemId(targetId);
                }
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        var tx = getSupportFragmentManager().beginTransaction().setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out).replace(R.id.fragment_container, fragment);

        tx.commit();
    }

    // -------------------------------------------------------------------------
    // Exit dialog (shown when back is pressed on Home)
    // -------------------------------------------------------------------------

    private void showExitDialog() {
        new MaterialAlertDialogBuilder(this).setTitle("Exit App").setMessage("Are you sure you want to exit?").setPositiveButton("Yes, Exit", (dialog, which) -> finishAffinity()).setNegativeButton("Cancel", null).show();
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private void requestNeededPermissions() {
        List<String> toRequest = new ArrayList<>();

        // Android 13+ notification runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // Step Counter / activity recognition runtime permission (Android 10+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        }

        if (!toRequest.isEmpty()) {
            permissionLauncher.launch(toRequest.toArray(new String[0]));
        }
    }

    /**
     * Android 12+ এ exact alarm runtime permission dialog হয় না।
     * User কে settings screen এ নিয়ে গিয়ে allow করাতে হয়।
     */
    private void ensureExactAlarmsAllowedIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (am == null) return;

        if (!am.canScheduleExactAlarms()) {
            new MaterialAlertDialogBuilder(this).setTitle("Allow Exact Alarms").setMessage("To ring exactly on time (even in Doze), please allow Exact Alarms for this app.").setPositiveButton("Open Settings", (d, w) -> {
                Intent i = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(i);
            }).setNegativeButton("Later", null).show();
        }
    }
}