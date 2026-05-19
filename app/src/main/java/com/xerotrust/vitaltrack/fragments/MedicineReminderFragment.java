package com.xerotrust.vitaltrack.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.adapters.MedicinePlanAdapter;
import com.xerotrust.vitaltrack.models.MedicinePlan;
import com.xerotrust.vitaltrack.models.MedicineScheduleItem;
import com.xerotrust.vitaltrack.models.MedicineTime;
import com.xerotrust.vitaltrack.ui.MedicinePlanWizard;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.MedicineScheduleUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MedicineReminderFragment extends Fragment implements MedicinePlanAdapter.PlanActionListener {

    private RecyclerView recyclerView;
    private View tvEmpty;
    private DatabaseHelper db;

    private final List<MedicinePlan> plans = new ArrayList<>();
    private MedicinePlanAdapter adapter;
    ImageView iv_back;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medicine_reminder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = new DatabaseHelper(requireContext());

        recyclerView = view.findViewById(R.id.recycler_medicines);
        tvEmpty      = view.findViewById(R.id.tv_empty);
        iv_back      = view.findViewById(R.id.iv_back);

        iv_back.setVisibility(View.GONE);

        adapter = new MedicinePlanAdapter(plans, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_medicine);
        fab.setOnClickListener(v -> {
            MedicinePlanWizard wizard = new MedicinePlanWizard(requireActivity(), db);
            wizard.startCreateFlow(planId -> {
                MedicineScheduleUtils.cancelAllForPlan(requireContext(), db, planId);
                MedicineScheduleUtils.scheduleAllForPlan(requireContext(), db, planId);
                MedicinePlan newPlan = db.getPlanFull(planId);
                if (newPlan != null) {
                    fillComputed(newPlan);
                    plans.add(0, newPlan);
                    adapter.notifyItemInserted(0);
                    recyclerView.scrollToPosition(0);
                    updateEmptyState();
                }
                Toast.makeText(requireContext(), "Reminder added!", Toast.LENGTH_SHORT).show();
            });
        });

        loadPlans();
        scheduleAllEnabledPlans();
    }

    // ─── Load ────────────────────────────────────────────────────────────────

    @SuppressLint("NotifyDataSetChanged")
    private void loadPlans() {
        plans.clear();
        List<MedicinePlan> loaded = db.getAllPlansBasic();
        for (MedicinePlan p : loaded) {
            MedicinePlan full = db.getPlanFull(p.getId());
            if (full == null) full = p;
            fillComputed(full);
            plans.add(full);
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        tvEmpty.setVisibility(plans.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(plans.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void scheduleAllEnabledPlans() {
        for (MedicinePlan p : db.getAllPlansBasic()) {
            if (p.isEnabled())
                MedicineScheduleUtils.scheduleAllForPlan(requireContext(), db, p.getId());
            else
                MedicineScheduleUtils.cancelAllForPlan(requireContext(), db, p.getId());
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────

    @Override
    public void onEditClicked(MedicinePlan plan) {
        MedicinePlanWizard wizard = new MedicinePlanWizard(requireActivity(), db);
        wizard.startEditFlow(plan.getId(), planId -> {
            MedicineScheduleUtils.cancelAllForPlan(requireContext(), db, planId);
            MedicineScheduleUtils.scheduleAllForPlan(requireContext(), db, planId);

            int pos = indexOfPlanId(planId);
            if (pos >= 0) {
                MedicinePlan updated = db.getPlanFull(planId);
                if (updated != null) {
                    fillComputed(updated);          // compute BEFORE notify
                    plans.set(pos, updated);
                    adapter.notifyItemChanged(pos); // adapter reads from plan object → instant update
                }
            }
            Toast.makeText(requireContext(), "Reminder updated!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onToggleClicked(MedicinePlan plan) {
        boolean newState = !plan.isEnabled();
        plan.setEnabled(newState);
        db.updatePlanEnabled(plan.getId(), newState);

        if (newState) MedicineScheduleUtils.scheduleAllForPlan(requireContext(), db, plan.getId());
        else          MedicineScheduleUtils.cancelAllForPlan(requireContext(), db, plan.getId());

        int pos = plans.indexOf(plan);
        if (pos >= 0) adapter.notifyItemChanged(pos);
    }

    @Override
    public void onDeleteClicked(MedicinePlan plan) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Delete " + plan.getName() + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    MedicineScheduleUtils.cancelAllForPlan(requireContext(), db, plan.getId());
                    db.deletePlan(plan.getId());
                    int pos = plans.indexOf(plan);
                    if (pos >= 0) {
                        plans.remove(pos);
                        adapter.notifyItemRemoved(pos);
                        updateEmptyState();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Resume ──────────────────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        // Refresh computed texts for all visible cards (time may have crossed midnight, etc.)
        refreshAllComputed();
    }

    // ─── Computed text helpers ────────────────────────────────────────────────

    /**
     * Computes nextDose + meta strings and stores them directly on the plan object.
     * The adapter reads these fields in onBindViewHolder → no stale tag problem.
     */
    private void fillComputed(MedicinePlan plan) {
        plan.setComputedNextDose(computeNextDoseText(plan));
        plan.setComputedMeta(computeMetaText(plan));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refreshAllComputed() {
        for (MedicinePlan p : plans) {
            fillComputed(p);
        }
        adapter.notifyDataSetChanged();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private int indexOfPlanId(int planId) {
        for (int i = 0; i < plans.size(); i++) {
            if (plans.get(i).getId() == planId) return i;
        }
        return -1;
    }

    private String computeMetaText(MedicinePlan plan) {
        if (plan == null) return "";
        String freq   = freqHuman(plan);
        int tCount    = plan.getTimes() == null ? 0 : plan.getTimes().size();
        return freq + " • " + tCount + " time" + (tCount == 1 ? "" : "s");
    }

    private String computeNextDoseText(MedicinePlan plan) {
        if (plan == null || plan.getTimes() == null || plan.getTimes().isEmpty()) return "";
        Calendar now      = Calendar.getInstance();
        Calendar best     = null;
        MedicineTime bestTime = null;

        for (MedicineTime t : plan.getTimes()) {
            Calendar c = MedicineScheduleUtils.nextOccurrence(plan, t, now);
            if (c == null) continue;
            if (best == null || c.getTimeInMillis() < best.getTimeInMillis()) {
                best     = c;
                bestTime = t;
            }
        }
        if (best == null || bestTime == null) return "";

        String dayWord = isSameDay(best, now) ? "Today" : "Tomorrow";
        String label   = inferLabelFromHour(bestTime.getHour());
        String dosage  = findDosageForLabel(plan.getScheduleItems(), label);
        if (dosage == null || dosage.trim().isEmpty()) dosage = "Dose";
        return String.format(Locale.getDefault(), "%s %s (%s • %s)", dayWord, bestTime.hhmm(), label, dosage);
    }

    private boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private String inferLabelFromHour(int h) {
        if (h >= 4  && h <= 11) return "After Breakfast";
        if (h >= 12 && h <= 16) return "After Lunch";
        if (h >= 17 && h <= 23) return "After Dinner";
        return "Dose";
    }

    private String findDosageForLabel(List<MedicineScheduleItem> items, String label) {
        if (items == null || items.isEmpty()) return "";
        for (MedicineScheduleItem it : items) {
            if (it.getLabel() != null && it.getLabel().equalsIgnoreCase(label))
                return it.getDosage();
        }
        return items.get(0).getDosage();
    }

    private String freqHuman(MedicinePlan p) {
        String t     = p.getFrequencyType();
        String param = p.getFrequencyParam() == null ? "" : p.getFrequencyParam();
        if (MedicinePlan.FREQ_EVERYDAY.equals(t))          return "Everyday";
        if (MedicinePlan.FREQ_EVERY_X_DAYS.equals(t))      return "Every " + param + " days";
        if (MedicinePlan.FREQ_DAY_OF_WEEK.equals(t))       return "Weekly (" + param + ")";
        if (MedicinePlan.FREQ_DAY_OF_MONTH.equals(t))      return "Monthly (" + param + ")";
        if (MedicinePlan.FREQ_X_ENABLE_Y_DISABLE.equals(t))return "Cycle (" + param + ")";
        return "Custom";
    }
}
