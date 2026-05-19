package com.xerotrust.vitaltrack.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.services.StepCounterService;
import com.xerotrust.vitaltrack.utils.AppPrefs;
import com.xerotrust.vitaltrack.utils.LanguageHelper;
import com.xerotrust.vitaltrack.utils.StepCalculatorUtils;
import com.xerotrust.vitaltrack.views.WeeklyStepsChartView;

import java.util.Calendar;
import java.util.Locale;

public class StepCounterFragment extends Fragment {

    // ─── Views ───────────────────────────────────────────────────────────────
    private TextView btnWalkTab, btnRunTab;
    private TextView tvSteps, tvPercent, tvDistance, tvCalories, tvDuration;
    private TextView tvGoalLabel, tvModeTitle;
    private TextView tvStartStop;
    private ProgressBar progressCircle;
    private View cardPermission;
    private WeeklyStepsChartView weeklyChart;
    private View cardDayDetail;
    private TextView tvDetailDate;

    // ─── Legend views (Walk ● / Run ●) in chart card header ─────────────────
    private View legendDotWalk;
    private View legendTextWalk;
    private View legendDotRun;
    private View legendTextRun;

    // Walk detail views
    private View rowDetailWalk;
    private TextView tvDetailWalkGoal, tvDetailWalkAchieve, tvDetailWalkKm, tvDetailWalkTime;

    // Run detail views
    private View rowDetailRun;
    private TextView tvDetailRunGoal, tvDetailRunAchieve, tvDetailRunKm, tvDetailRunTime;

    // ─── State ───────────────────────────────────────────────────────────────
    private String currentMode = StepCounterService.MODE_WALK;
    private AppPrefs prefs;
    private StepCounterService boundService;
    private boolean serviceBound = false;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            timerHandler.postDelayed(this, 1000);
        }
    };

    // ─── Permission ──────────────────────────────────────────────────────────
    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
        if (granted) doStartMode();
        else showPermissionCard();
    });

    // ─── Broadcast receiver ───────────────────────────────────────────────────
    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (StepCounterService.BROADCAST_UPDATE.equals(intent.getAction())) {
                updateUI();
                updateWeeklyChart();
            }
        }
    };

    // ─── Service connection ───────────────────────────────────────────────────
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            boundService = ((StepCounterService.StepBinder) service).getService();
            serviceBound = true;
            currentMode = boundService.getCurrentMode();
            updateTabSelection();
            updateUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            boundService = null;
        }
    };

    // ─────────────────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_step_counter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View root, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(root, savedInstanceState);
        prefs = new AppPrefs(requireContext());
        bindViews(root);
        setupTabs();
        setupStartStop();
        setupGoalEdit(root);
        setupPermissionBtn(root);
        setupWeeklyChartClick();
        updateTabSelection();
        updateWeeklyChart();
        updateUI();
        requireContext().bindService(new Intent(requireContext(), StepCounterService.class), serviceConnection, 0);
    }

    // ─── Bind views ──────────────────────────────────────────────────────────

    private void bindViews(View v) {
        btnWalkTab = v.findViewById(R.id.btn_tab_walk);
        btnRunTab = v.findViewById(R.id.btn_tab_run);
        tvSteps = v.findViewById(R.id.tv_steps);
        tvPercent = v.findViewById(R.id.tv_percent);
        tvDistance = v.findViewById(R.id.tv_distance);
        tvCalories = v.findViewById(R.id.tv_calories);
        tvDuration = v.findViewById(R.id.tv_duration);
        tvGoalLabel = v.findViewById(R.id.tv_goal_label);
        tvModeTitle = v.findViewById(R.id.tv_mode_title);
        progressCircle = v.findViewById(R.id.progress_circle);
        cardPermission = v.findViewById(R.id.card_permission);
        weeklyChart = v.findViewById(R.id.chart_weekly);
        tvStartStop = v.findViewById(R.id.tv_start_stop_label);
        cardDayDetail = v.findViewById(R.id.card_day_detail);
        tvDetailDate = v.findViewById(R.id.tv_detail_date);

        // Walk row — the parent LinearLayout wrapping the walk section
        rowDetailWalk = v.findViewById(R.id.row_detail_walk);
        tvDetailWalkGoal = v.findViewById(R.id.tv_detail_walk_goal);
        tvDetailWalkAchieve = v.findViewById(R.id.tv_detail_walk_achieve);
        tvDetailWalkKm = v.findViewById(R.id.tv_detail_walk_km);
        tvDetailWalkTime = v.findViewById(R.id.tv_detail_walk_time);

        // Run row
        rowDetailRun = v.findViewById(R.id.row_detail_run);
        tvDetailRunGoal = v.findViewById(R.id.tv_detail_run_goal);
        tvDetailRunAchieve = v.findViewById(R.id.tv_detail_run_achieve);
        tvDetailRunKm = v.findViewById(R.id.tv_detail_run_km);
        tvDetailRunTime = v.findViewById(R.id.tv_detail_run_time);

        // Legend dots/labels — IDs added to fragment_step_counter.xml
        legendDotWalk  = findLegendView(v, "legend_dot_walk");
        legendTextWalk = findLegendView(v, "legend_text_walk");
        legendDotRun   = findLegendView(v, "legend_dot_run");
        legendTextRun  = findLegendView(v, "legend_text_run");
    }

    // ─── Tabs ────────────────────────────────────────────────────────────────

    /**
     * Looks up a View by resource name string (avoids hard-coding R.id at call sites).
     * Returns null silently if the ID doesn't exist.
     */
    private View findLegendView(View root, String name) {
        int id = getResources().getIdentifier(name, "id", requireContext().getPackageName());
        return id != 0 ? root.findViewById(id) : null;
    }

    private void setupTabs() {
        if (btnWalkTab != null)
            btnWalkTab.setOnClickListener(v -> switchMode(StepCounterService.MODE_WALK));
        if (btnRunTab != null)
            btnRunTab.setOnClickListener(v -> switchMode(StepCounterService.MODE_RUN));
    }

    private void switchMode(String mode) {
        currentMode = mode;
        updateTabSelection();
        if (weeklyChart != null) weeklyChart.setCurrentMode(mode);
        updateUI();
        // If detail card is already visible, refresh it to reflect new mode filter
        if (cardDayDetail != null && cardDayDetail.getVisibility() == View.VISIBLE) {
            refreshDetailVisibility();
        }
    }

    private void updateTabSelection() {
        boolean isWalk = StepCounterService.MODE_WALK.equals(currentMode);
        if (btnWalkTab != null) {
            btnWalkTab.setBackgroundResource(isWalk ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
            btnWalkTab.setTextColor(isWalk ? Color.WHITE : 0xFF6B7280);
        }
        if (btnRunTab != null) {
            btnRunTab.setBackgroundResource(!isWalk ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
            btnRunTab.setTextColor(!isWalk ? Color.WHITE : 0xFF6B7280);
        }
        if (tvModeTitle != null)
            tvModeTitle.setText(isWalk ? (LanguageHelper.isBangla(requireContext()) ? "আজকের হাঁটা" : "Walking Today") : (LanguageHelper.isBangla(requireContext()) ? "আজকের দৌড়" : "Running Today"));

        // Show only the legend dot+text that matches the active tab
        int walkVis = isWalk ? View.VISIBLE : View.GONE;
        int runVis  = isWalk ? View.GONE    : View.VISIBLE;
        if (legendDotWalk  != null) legendDotWalk .setVisibility(walkVis);
        if (legendTextWalk != null) legendTextWalk.setVisibility(walkVis);
        if (legendDotRun   != null) legendDotRun  .setVisibility(runVis);
        if (legendTextRun  != null) legendTextRun .setVisibility(runVis);
    }

    // ─── Start / Stop ────────────────────────────────────────────────────────

    private void setupStartStop() {
        if (tvStartStop != null) tvStartStop.setOnClickListener(v -> onStartStopClicked());
    }

    private void onStartStopClicked() {
        if (prefs.isModeActive(currentMode)) {
            stopCurrentMode();
        } else {
            String other = StepCounterService.MODE_WALK.equals(currentMode) ? StepCounterService.MODE_RUN : StepCounterService.MODE_WALK;
            if (prefs.isModeActive(other)) {
                boolean isBn = LanguageHelper.isBangla(requireContext());
                boolean otherIsWalk = StepCounterService.MODE_WALK.equals(other);
                String label = isBn ? (otherIsWalk ? "হাঁটা" : "দৌড়") : (otherIsWalk ? "Walking" : "Running");
                Toast.makeText(requireContext(), isBn ? label + " এখন চলছে। আগে সেটি বন্ধ করুন।" : label + " is currently active. Stop it first.", Toast.LENGTH_SHORT).show();
                return;
            }
            checkPermissionAndStart();
        }
    }

    private void checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                showPermissionCard();
                return;
            }
        }
        doStartMode();
    }

    private void doStartMode() {
        Context ctx = requireContext();
        prefs.setModeActive(currentMode, true);
        if (prefs.getModeSessionStartMs(currentMode) == 0L)
            prefs.setModeSessionStartMs(currentMode, System.currentTimeMillis());

        Intent intent = new Intent(ctx, StepCounterService.class);
        intent.setAction(StepCounterService.MODE_WALK.equals(currentMode) ? StepCounterService.ACTION_START_WALK : StepCounterService.ACTION_START_RUN);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(intent);
        else ctx.startService(intent);

        if (!serviceBound) ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        updateUI();
    }

    private void stopCurrentMode() {
        prefs.setModeActive(currentMode, false);
        Intent intent = new Intent(requireContext(), StepCounterService.class);
        intent.setAction(StepCounterService.ACTION_STOP);
        requireContext().startService(intent);
        if (serviceBound) {
            try {
                requireContext().unbindService(serviceConnection);
            } catch (Exception ignored) {
            }
            serviceBound = false;
            boundService = null;
        }
        updateUI();
    }

    // ─── Goal edit ───────────────────────────────────────────────────────────

    private void setupGoalEdit(View root) {
        View btn = root.findViewById(R.id.btn_edit_goal);
        if (btn != null) btn.setOnClickListener(v -> showGoalDialog());
        if (tvGoalLabel != null) tvGoalLabel.setOnClickListener(v -> showGoalDialog());
    }

    private void showGoalDialog() {
        boolean isBn = LanguageHelper.isBangla(requireContext());
        boolean isWalk = StepCounterService.MODE_WALK.equals(currentMode);
        String label = isBn ? (isWalk ? "হাঁটা" : "দৌড়") : (isWalk ? "Walking" : "Running");
        EditText et = new EditText(requireContext());
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setText(String.valueOf(prefs.getModeGoal(currentMode)));
        et.setSelectAllOnFocus(true);

        int p = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout wrap = new LinearLayout(requireContext());
        wrap.setPadding(p * 2, p, p * 2, 0);
        wrap.addView(et, new LinearLayout.LayoutParams(-1, -2));

        String dialogTitle = isBn ? label + " লক্ষ্য নির্ধারণ করুন (পদক্ষেপ)" : "Set " + label + " Goal (steps)";
        new AlertDialog.Builder(requireContext()).setTitle(dialogTitle).setView(wrap).setPositiveButton(isBn ? "সংরক্ষণ করুন" : "Save", (d, w) -> {
            String s = et.getText().toString().trim();
            if (!s.isEmpty()) {
                int g = Math.min(Math.max(Integer.parseInt(s), 100), 100_000);
                prefs.setModeGoal(currentMode, g);
                updateUI();
                String msg = isBn ? label + " লক্ষ্য → " + String.format(Locale.getDefault(), "%,d", g) + " পদক্ষেপ" : label + " goal → " + String.format(Locale.getDefault(), "%,d", g) + " steps";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }).setNegativeButton(isBn ? "বাতিল" : "Cancel", null).show();
    }

    // ─── Permission card ─────────────────────────────────────────────────────

    private void setupPermissionBtn(View root) {
        View btn = root.findViewById(R.id.btn_grant_permission);
        if (btn != null)
            btn.setOnClickListener(v -> permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION));
    }

    private void showPermissionCard() {
        if (cardPermission != null) cardPermission.setVisibility(View.VISIBLE);
    }

    // ─── updateUI ────────────────────────────────────────────────────────────

    private void updateUI() {
        int steps = prefs.getModeSteps(currentMode);
        int goal = prefs.getModeGoal(currentMode);
        float h = prefs.getHeight();
        float w = prefs.getWeight();
        boolean active = prefs.isModeActive(currentMode);
        boolean isWalk = StepCounterService.MODE_WALK.equals(currentMode);

        if (tvSteps != null) tvSteps.setText(String.format(Locale.getDefault(), "%,d", steps));

        int pct = goal > 0 ? Math.min(steps * 100 / goal, 100) : 0;
        if (tvPercent != null) tvPercent.setText(pct + "%");
        if (progressCircle != null) progressCircle.setProgress(pct);

        if (tvGoalLabel != null)
            tvGoalLabel.setText(getString(R.string.of_n_steps_goal, String.format(Locale.getDefault(), "%,d", goal)));

        float km = StepCalculatorUtils.getDistanceKm(steps, currentMode, h);
        float kcal = StepCalculatorUtils.getCalories(steps, currentMode, w);

        if (tvDistance != null)
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", km));
        if (tvCalories != null)
            tvCalories.setText(String.format(Locale.getDefault(), "%.0f kcal", kcal));

        long sessionStart = prefs.getModeSessionStartMs(currentMode);
        long elapsed = (active && sessionStart > 0) ? System.currentTimeMillis() - sessionStart : 0L;
        if (tvDuration != null) tvDuration.setText(StepCalculatorUtils.formatDuration(elapsed));

        if (tvStartStop != null) {
            if (active) {
                tvStartStop.setText(isWalk ? getString(R.string.stop_walking_btn) : getString(R.string.stop_running_btn));
                tvStartStop.setBackgroundResource(R.drawable.bg_btn_stop);
            } else {
                tvStartStop.setText(isWalk ? getString(R.string.start_walking_btn) : getString(R.string.start_running_btn));
                tvStartStop.setBackgroundResource(R.drawable.bg_btn_start);
            }
        }
    }

    // ─── Weekly chart ────────────────────────────────────────────────────────

    private void updateWeeklyChart() {
        if (weeklyChart == null) return;
        weeklyChart.setCurrentMode(currentMode);
        int[] walk = prefs.getWeeklyWalkSteps();
        int[] run = prefs.getWeeklyRunSteps();
        int[] goals = new int[7];
        for (int i = 0; i < 7; i++) {
            AppPrefs.DayDetail d = prefs.getDayDetail(i);
            goals[i] = d.walkGoal + d.runGoal;
        }
        // Live today
        walk[6] = prefs.getModeSteps(StepCounterService.MODE_WALK);
        run[6] = prefs.getModeSteps(StepCounterService.MODE_RUN);
        goals[6] = prefs.getModeGoal(StepCounterService.MODE_WALK) + prefs.getModeGoal(StepCounterService.MODE_RUN);

        weeklyChart.setDayLabels(buildDayLabels());
        weeklyChart.setWeeklyData(walk, run, goals);
    }

    private void setupWeeklyChartClick() {
        if (weeklyChart != null) weeklyChart.setOnDayClickListener(this::showDayDetail);
    }

    private String[] buildDayLabels() {
        String[] all = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        Calendar c = Calendar.getInstance();
        int dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        String[] labels = new String[7];
        String todayLabel = LanguageHelper.isBangla(requireContext()) ? "আজ" : "Today";
        for (int i = 0; i < 7; i++) {
            int dayIdx = ((dow + (i - 6)) % 7 + 7) % 7;
            labels[i] = (i == 6) ? todayLabel : all[dayIdx];
        }
        return labels;
    }

    // ─── Day detail panel ────────────────────────────────────────────────────

    private void showDayDetail(int dayIndex) {
        if (cardDayDetail == null) return;

        AppPrefs.DayDetail d;
        String dateLabel;

        if (dayIndex == 6) {
            // Live today data
            d = new AppPrefs.DayDetail();
            d.walkSteps = prefs.getModeSteps(StepCounterService.MODE_WALK);
            d.runSteps = prefs.getModeSteps(StepCounterService.MODE_RUN);
            d.totalSteps = d.walkSteps + d.runSteps;
            d.walkGoal = prefs.getModeGoal(StepCounterService.MODE_WALK);
            d.runGoal = prefs.getModeGoal(StepCounterService.MODE_RUN);
            float ht = prefs.getHeight();
            d.walkKm = StepCalculatorUtils.getDistanceKm(d.walkSteps, "walk", ht);
            d.runKm = StepCalculatorUtils.getDistanceKm(d.runSteps, "run", ht);
            long now = System.currentTimeMillis();
            long ws = prefs.getModeSessionStartMs(StepCounterService.MODE_WALK);
            long rs = prefs.getModeSessionStartMs(StepCounterService.MODE_RUN);
            d.walkTimeMs = prefs.isModeActive(StepCounterService.MODE_WALK) && ws > 0 ? now - ws : 0;
            d.runTimeMs = prefs.isModeActive(StepCounterService.MODE_RUN) && rs > 0 ? now - rs : 0;
            dateLabel = LanguageHelper.isBangla(requireContext()) ? "আজ" : "Today";
        } else {
            d = prefs.getDayDetail(dayIndex);
            dateLabel = buildDayLabels()[dayIndex];
        }

        cardDayDetail.setVisibility(View.VISIBLE);
        set(tvDetailDate, dateLabel);

        // Populate both rows
        set(tvDetailWalkGoal, fmt(d.walkGoal) + " steps");
        set(tvDetailWalkAchieve, fmt(d.walkSteps) + " steps");
        set(tvDetailWalkKm, String.format(Locale.getDefault(), "%.2f km", d.walkKm));
        set(tvDetailWalkTime, StepCalculatorUtils.formatDuration(d.walkTimeMs));

        set(tvDetailRunGoal, fmt(d.runGoal) + " steps");
        set(tvDetailRunAchieve, fmt(d.runSteps) + " steps");
        set(tvDetailRunKm, String.format(Locale.getDefault(), "%.2f km", d.runKm));
        set(tvDetailRunTime, StepCalculatorUtils.formatDuration(d.runTimeMs));

        // Show only the row that matches the active tab
        refreshDetailVisibility();
    }

    /**
     * Shows only the walk OR run detail row depending on currentMode.
     * Called both after populating the card and whenever the tab is switched.
     */
    private void refreshDetailVisibility() {
        boolean isWalk = StepCounterService.MODE_WALK.equals(currentMode);
        if (rowDetailWalk != null) rowDetailWalk.setVisibility(isWalk ? View.VISIBLE : View.GONE);
        if (rowDetailRun != null) rowDetailRun.setVisibility(isWalk ? View.GONE : View.VISIBLE);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void set(TextView tv, String text) {
        if (tv != null) tv.setText(text);
    }

    private String fmt(int n) {
        return String.format(Locale.getDefault(), "%,d", n);
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        // Auto-reset steps if a new day has begun
        if (prefs != null) {
            boolean reset = prefs.checkAndResetIfNewDay();
            if (reset && serviceBound && boundService != null) {
                // Notify service that steps have been reset
                Intent stopIntent = new Intent(requireContext(), StepCounterService.class);
                stopIntent.setAction(StepCounterService.ACTION_STOP);
                requireContext().startService(stopIntent);
                serviceBound = false;
                boundService = null;
            }
        }
        IntentFilter f = new IntentFilter(StepCounterService.BROADCAST_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            requireContext().registerReceiver(updateReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else
            ContextCompat.registerReceiver(requireContext(), updateReceiver, f, ContextCompat.RECEIVER_EXPORTED);
        timerHandler.post(timerRunnable);
        updateUI();
        updateWeeklyChart();
    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            requireContext().unregisterReceiver(updateReceiver);
        } catch (Exception ignored) {
        }
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (serviceBound) {
            try {
                requireContext().unbindService(serviceConnection);
            } catch (Exception ignored) {
            }
            serviceBound = false;
        }
        timerHandler.removeCallbacks(timerRunnable);
    }
}