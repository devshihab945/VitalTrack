package com.xerotrust.vitaltrack.ui;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.adapters.ScheduleAdapter;
import com.xerotrust.vitaltrack.adapters.TimesAdapter;
import com.xerotrust.vitaltrack.models.MedicinePlan;
import com.xerotrust.vitaltrack.models.MedicineScheduleItem;
import com.xerotrust.vitaltrack.models.MedicineTime;
import com.xerotrust.vitaltrack.utils.DatabaseHelper;
import com.xerotrust.vitaltrack.utils.PremiumSnackbar;
import com.xerotrust.vitaltrack.utils.SnackbarType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicinePlanWizard {

    public interface OnSavedListener {
        void onSaved(int planId);
    }

    private final FragmentActivity activity;
    private final DatabaseHelper db;

    public MedicinePlanWizard(FragmentActivity activity, DatabaseHelper db) {
        this.activity = activity;
        this.db = db;
    }

    public void startCreateFlow(OnSavedListener listener) {
        showStep1(null, listener);
    }

    public void startEditFlow(int planId, OnSavedListener listener) {
        MedicinePlan plan = db.getPlanFull(planId);
        showStep1(plan, listener);
    }

    // ─── Step 1: Name + Frequency + Alarm Mode + Remind Before ──────────────

    @SuppressLint("SetTextI18n")
    private void showStep1(MedicinePlan existing, OnSavedListener listener) {
        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_med_plan_step1, null);

        EditText etName = v.findViewById(R.id.et_plan_name);
        AutoCompleteTextView etFreq = v.findViewById(R.id.et_frequency);
        TextInputLayout tilEveryX = v.findViewById(R.id.til_every_x_days);
        EditText etEveryX = v.findViewById(R.id.et_every_x_days);
        View layoutEnableDisable = v.findViewById(R.id.layout_enable_disable);
        EditText etEnable = v.findViewById(R.id.et_enable_days);
        EditText etDisable = v.findViewById(R.id.et_disable_days);
        View chipsWeek = v.findViewById(R.id.chips_weekdays);
        Chip mon = v.findViewById(R.id.chip_mon);
        Chip tue = v.findViewById(R.id.chip_tue);
        Chip wed = v.findViewById(R.id.chip_wed);
        Chip thu = v.findViewById(R.id.chip_thu);
        Chip fri = v.findViewById(R.id.chip_fri);
        Chip sat = v.findViewById(R.id.chip_sat);
        Chip sun = v.findViewById(R.id.chip_sun);
        TextInputLayout tilDom = v.findViewById(R.id.til_day_of_month);
        EditText etDom = v.findViewById(R.id.et_day_of_month);

        LinearLayout cardRing = v.findViewById(R.id.card_mode_ring);
        LinearLayout cardVibrate = v.findViewById(R.id.card_mode_vibrate);
        LinearLayout cardRemind10 = v.findViewById(R.id.card_remind_10);
        LinearLayout cardRemind60 = v.findViewById(R.id.card_remind_60);

        // ── State ──
        final boolean[] isRing = {false};
        final boolean[] remind10 = {false};
        final boolean[] remind60 = {false};

        @SuppressLint("UseCompatLoadingForDrawables") Runnable applyRingStyle = () -> {
            cardRing.setBackground(activity.getDrawable(isRing[0] ? R.drawable.bg_remind_chip_selected : R.drawable.bg_remind_chip_unselected));
            cardVibrate.setBackground(activity.getDrawable(isRing[0] ? R.drawable.bg_remind_chip_unselected : R.drawable.bg_remind_chip_selected));
        };
        @SuppressLint("UseCompatLoadingForDrawables") Runnable applyRemindStyle = () -> {
            cardRemind10.setBackground(activity.getDrawable(remind10[0] ? R.drawable.bg_remind_chip_selected : R.drawable.bg_remind_chip_unselected));
            cardRemind60.setBackground(activity.getDrawable(remind60[0] ? R.drawable.bg_remind_chip_selected : R.drawable.bg_remind_chip_unselected));
        };

        cardRing.setOnClickListener(b -> {
            isRing[0] = true;
            applyRingStyle.run();
        });
        cardVibrate.setOnClickListener(b -> {
            isRing[0] = false;
            applyRingStyle.run();
        });
        cardRemind10.setOnClickListener(b -> {
            remind10[0] = !remind10[0];
            applyRemindStyle.run();
        });
        cardRemind60.setOnClickListener(b -> {
            remind60[0] = !remind60[0];
            applyRemindStyle.run();
        });

        // ── Frequency adapter ──
        String[] freqItems = {"Everyday", "Every X days", "Day of the Week", "Day of Month", "X day Enable / Y day Disable"};
        etFreq.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, freqItems));
        etFreq.setText("Everyday", false);

        Runnable refreshFreq = () -> {
            String sel = etFreq.getText() == null ? "Everyday" : etFreq.getText().toString();
            tilEveryX.setVisibility(sel.equals("Every X days") ? View.VISIBLE : View.GONE);
            chipsWeek.setVisibility(sel.equals("Day of the Week") ? View.VISIBLE : View.GONE);
            tilDom.setVisibility(sel.equals("Day of Month") ? View.VISIBLE : View.GONE);
            layoutEnableDisable.setVisibility(sel.equals("X day Enable / Y day Disable") ? View.VISIBLE : View.GONE);
        };
        etFreq.setOnItemClickListener((a, b, c, d) -> refreshFreq.run());
        refreshFreq.run();

        // ── Pre-fill for edit ──
        if (existing != null) {
            etName.setText(existing.getName());
            String ft = existing.getFrequencyType();
            if (MedicinePlan.FREQ_EVERYDAY.equals(ft)) etFreq.setText("Everyday", false);
            else if (MedicinePlan.FREQ_EVERY_X_DAYS.equals(ft))
                etFreq.setText("Every X days", false);
            else if (MedicinePlan.FREQ_DAY_OF_WEEK.equals(ft))
                etFreq.setText("Day of the Week", false);
            else if (MedicinePlan.FREQ_DAY_OF_MONTH.equals(ft))
                etFreq.setText("Day of Month", false);
            else if (MedicinePlan.FREQ_X_ENABLE_Y_DISABLE.equals(ft))
                etFreq.setText("X day Enable / Y day Disable", false);
            refreshFreq.run();

            String param = existing.getFrequencyParam() == null ? "" : existing.getFrequencyParam();
            if (MedicinePlan.FREQ_EVERY_X_DAYS.equals(ft)) etEveryX.setText(param);
            else if (MedicinePlan.FREQ_DAY_OF_WEEK.equals(ft)) {
                String p = param.toUpperCase(Locale.US);
                mon.setChecked(p.contains("MON"));
                tue.setChecked(p.contains("TUE"));
                wed.setChecked(p.contains("WED"));
                thu.setChecked(p.contains("THU"));
                fri.setChecked(p.contains("FRI"));
                sat.setChecked(p.contains("SAT"));
                sun.setChecked(p.contains("SUN"));
            } else if (MedicinePlan.FREQ_DAY_OF_MONTH.equals(ft)) etDom.setText(param);
            else if (MedicinePlan.FREQ_X_ENABLE_Y_DISABLE.equals(ft)) {
                String[] parts = param.split(",");
                if (parts.length >= 2) {
                    etEnable.setText(parts[0].trim());
                    etDisable.setText(parts[1].trim());
                }
            }

            isRing[0] = "alarm".equalsIgnoreCase(existing.getVibrationMode());
            remind10[0] = existing.isRemind10();
            remind60[0] = existing.isRemind60();
        }

        applyRingStyle.run();
        applyRemindStyle.run();

        new MaterialAlertDialogBuilder(activity, R.style.RoundedDialog).setTitle(existing == null ? "New Reminder" : "Edit Reminder").setView(v).setPositiveButton("Next →", (d, w) -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                PremiumSnackbar.show(activity.findViewById(android.R.id.content), "Input All Field!", SnackbarType.WARNING);
                return;
            }

            String sel = etFreq.getText() == null ? "Everyday" : etFreq.getText().toString();
            String freqType, freqParam = "";

            switch (sel) {
                case "Everyday":
                    freqType = MedicinePlan.FREQ_EVERYDAY;
                    break;
                case "Every X days":
                    freqType = MedicinePlan.FREQ_EVERY_X_DAYS;
                    freqParam = etEveryX.getText() == null ? "" : etEveryX.getText().toString().trim();
                    if (TextUtils.isEmpty(freqParam)) return;
                    break;
                case "Day of the Week":
                    freqType = MedicinePlan.FREQ_DAY_OF_WEEK;
                    List<String> days = new ArrayList<>();
                    if (mon.isChecked()) days.add("MON");
                    if (tue.isChecked()) days.add("TUE");
                    if (wed.isChecked()) days.add("WED");
                    if (thu.isChecked()) days.add("THU");
                    if (fri.isChecked()) days.add("FRI");
                    if (sat.isChecked()) days.add("SAT");
                    if (sun.isChecked()) days.add("SUN");
                    if (days.isEmpty()) return;
                    freqParam = TextUtils.join(",", days);
                    break;
                case "Day of Month":
                    freqType = MedicinePlan.FREQ_DAY_OF_MONTH;
                    freqParam = etDom.getText() == null ? "" : etDom.getText().toString().trim();
                    if (TextUtils.isEmpty(freqParam)) return;
                    break;
                default:
                    freqType = MedicinePlan.FREQ_X_ENABLE_Y_DISABLE;
                    String x = etEnable.getText() == null ? "" : etEnable.getText().toString().trim();
                    String y = etDisable.getText() == null ? "" : etDisable.getText().toString().trim();
                    if (TextUtils.isEmpty(x) || TextUtils.isEmpty(y)) return;
                    freqParam = x + "," + y;
                    break;
            }

            MedicinePlan plan = (existing != null) ? existing : new MedicinePlan();
            plan.setName(name);
            plan.setFrequencyType(freqType);
            plan.setFrequencyParam(freqParam);
            plan.setVibrationMode(isRing[0] ? "alarm" : "normal");
            plan.setRemind10(remind10[0]);
            plan.setRemind60(remind60[0]);

            if (existing == null) {
                plan.setEnabled(true);
                plan.setScheduleItems(new ArrayList<>());
                plan.setTimes(new ArrayList<>());
            }
            showStep2(plan, listener);
        }).setNegativeButton("Cancel", null).show();
    }

    // ─── Step 2: Schedule + Times ───────────────────────────────────────────

    private void showStep2(MedicinePlan plan, OnSavedListener listener) {
        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_med_plan_step2, null);

        RecyclerView rvSchedule = v.findViewById(R.id.rv_schedule);
        RecyclerView rvTimes = v.findViewById(R.id.rv_times);

        List<MedicineScheduleItem> schedule = plan.getScheduleItems() == null ? new ArrayList<>() : new ArrayList<>(plan.getScheduleItems());
        List<MedicineTime> times = plan.getTimes() == null ? new ArrayList<>() : new ArrayList<>(plan.getTimes());

        if (schedule.isEmpty())
            schedule.add(new MedicineScheduleItem(0, plan.getId(), "After Breakfast", ""));
        if (times.isEmpty()) times.add(new MedicineTime(0, plan.getId(), 7, 0));

        // ── Schedule adapter with edit ──
        final ScheduleAdapter[] scheduleAdapterHolder = {null};
        scheduleAdapterHolder[0] = new ScheduleAdapter(schedule, new ScheduleAdapter.Listener() {
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= schedule.size()) return;
                schedule.remove(position);
                scheduleAdapterHolder[0].notifyItemRemoved(position);
            }

            @Override
            public void onEdit(int position) {
                if (position < 0 || position >= schedule.size()) return;
                showEditScheduleDialog(schedule, position, scheduleAdapterHolder[0]);
            }
        });

        // ── Times adapter ──
        final TimesAdapter[] timesAdapterHolder = {null};
        timesAdapterHolder[0] = new TimesAdapter(times, new TimesAdapter.Listener() {
            @Override
            public void onDelete(int position) {
                if (position < 0 || position >= times.size()) return;
                times.remove(position);
                timesAdapterHolder[0].notifyItemRemoved(position);
            }

            @Override
            public void onEdit(int position) {
                if (position < 0 || position >= times.size()) return;

                MedicineTime current = times.get(position);

                // ★ false = 12H AM/PM format
                new TimePickerDialog(activity, (tp, hour, minute) -> {
                    // Prevent duplicate times
                    for (int i = 0; i < times.size(); i++) {
                        if (i != position && times.get(i).getHour() == hour && times.get(i).getMinute() == minute) {
                            return;
                        }
                    }
                    current.setHour(hour);
                    current.setMinute(minute);
                    timesAdapterHolder[0].notifyItemChanged(position);
                }, current.getHour(), current.getMinute(), false).show(); // ★ 12H
            }
        });

        rvSchedule.setLayoutManager(new LinearLayoutManager(activity));
        rvSchedule.setAdapter(scheduleAdapterHolder[0]);
        rvSchedule.setNestedScrollingEnabled(false);

        rvTimes.setLayoutManager(new LinearLayoutManager(activity));
        rvTimes.setAdapter(timesAdapterHolder[0]);
        rvTimes.setNestedScrollingEnabled(false);

        v.findViewById(R.id.btn_add_schedule).setOnClickListener(btn -> showAddScheduleDialog(schedule, scheduleAdapterHolder[0]));
        v.findViewById(R.id.btn_add_time).setOnClickListener(btn -> showAddTimeDialog(times, timesAdapterHolder[0]));

        new MaterialAlertDialogBuilder(activity, R.style.RoundedDialog).setTitle("Schedule & Alarms").setView(v).setPositiveButton("Save", (d, w) -> {
            if (plan.getId() <= 0) {
                long id = db.insertPlan(plan);
                plan.setId((int) id);
            } else {
                db.updatePlan(plan);
            }
            for (MedicineScheduleItem it : schedule) it.setPlanId(plan.getId());
            for (MedicineTime t : times) t.setPlanId(plan.getId());
            db.replaceScheduleItems(plan.getId(), schedule);
            db.replaceTimes(plan.getId(), times);
            if (listener != null) listener.onSaved(plan.getId());
        }).setNegativeButton("← Back", (d, w) -> showStep1(plan, listener)).show();
    }

    // ─── Add / Edit Schedule Dialog ─────────────────────────────────────────

    private static final String[] PRESET_LABELS = {"After Breakfast", "After Lunch", "After Dinner", "Before Breakfast", "Before Lunch", "Before Sleep", "Custom..."};

    private void showAddScheduleDialog(List<MedicineScheduleItem> schedule, ScheduleAdapter adapter) {
        showScheduleItemDialog(null, -1, schedule, adapter);
    }

    private void showEditScheduleDialog(List<MedicineScheduleItem> schedule, int position, ScheduleAdapter adapter) {
        showScheduleItemDialog(schedule.get(position), position, schedule, adapter);
    }

    @SuppressLint("SetTextI18n")
    private void showScheduleItemDialog(MedicineScheduleItem existing, int editPosition, List<MedicineScheduleItem> schedule, ScheduleAdapter adapter) {

        View v = LayoutInflater.from(activity).inflate(R.layout.dialog_add_schedule_item, null);
        AutoCompleteTextView etLabel = v.findViewById(R.id.et_label);
        View tilCustomLabel = v.findViewById(R.id.til_custom_label);
        EditText etCustomLabel = v.findViewById(R.id.et_custom_label);
        EditText etDosage = v.findViewById(R.id.et_dosage_text);

        etLabel.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, PRESET_LABELS));

        if (existing != null) {
            String existingLabel = existing.getLabel() == null ? "" : existing.getLabel();
            boolean isPreset = false;
            for (String p : PRESET_LABELS) {
                if (p.equals(existingLabel)) {
                    isPreset = true;
                    break;
                }
            }
            boolean isCustomExisting = !isPreset || existingLabel.isEmpty();

            if (isCustomExisting) {
                etLabel.setText("Custom...", false);
                tilCustomLabel.setVisibility(View.VISIBLE);
                etCustomLabel.setText(existingLabel);
            } else {
                etLabel.setText(existingLabel, false);
            }
            etDosage.setText(existing.getDosage() == null ? "" : existing.getDosage());
        } else {
            etLabel.setText("After Breakfast", false);
        }

        etLabel.setOnItemClickListener((parent, itemView, pos, id) -> {
            if ("Custom...".equals(PRESET_LABELS[pos])) {
                tilCustomLabel.setVisibility(View.VISIBLE);
                etCustomLabel.requestFocus();
            } else {
                tilCustomLabel.setVisibility(View.GONE);
                etCustomLabel.setText("");
            }
        });

        String title = (editPosition >= 0) ? "Edit Schedule" : "Add Schedule";

        new MaterialAlertDialogBuilder(activity, R.style.RoundedDialog).setTitle(title).setView(v).setPositiveButton(editPosition >= 0 ? "Update" : "Add", (d, w) -> {
            String dropdown = etLabel.getText() == null ? "" : etLabel.getText().toString().trim();
            String label;
            if ("Custom...".equals(dropdown)) {
                label = etCustomLabel.getText() == null ? "" : etCustomLabel.getText().toString().trim();
            } else {
                label = dropdown;
            }
            String dosage = etDosage.getText() == null ? "" : etDosage.getText().toString().trim();
            if (TextUtils.isEmpty(label)) return;

            if (editPosition >= 0) {
                MedicineScheduleItem item = schedule.get(editPosition);
                item.setLabel(label);
                item.setDosage(dosage);
                adapter.notifyItemChanged(editPosition);
            } else {
                schedule.add(new MedicineScheduleItem(0, 0, label, dosage));
                adapter.notifyItemInserted(schedule.size() - 1);
            }
        }).setNegativeButton("Cancel", null).show();
    }

    // ─── Add Time Dialog ────────────────────────────────────────────────────

    private void showAddTimeDialog(List<MedicineTime> times, TimesAdapter adapter) {
        int defH = 7, defM = 0;
        if (!times.isEmpty()) {
            MedicineTime last = times.get(times.size() - 1);
            defH = last.getHour();
            defM = last.getMinute();
        }
        // ★ false = 12H AM/PM format
        new TimePickerDialog(activity, (tp, hour, minute) -> {
            for (MedicineTime t : times) {
                if (t.getHour() == hour && t.getMinute() == minute) return;
            }
            times.add(new MedicineTime(0, 0, hour, minute));
            adapter.notifyItemInserted(times.size() - 1);
        }, defH, defM, false).show(); // ★ 12H
    }
}