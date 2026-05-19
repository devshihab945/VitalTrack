package com.xerotrust.vitaltrack.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.MedicineReminder;

import java.util.List;
import java.util.Locale;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.ViewHolder> {

    public interface MedicineActionListener {
        void onEditClicked(MedicineReminder med);

        void onToggleClicked(MedicineReminder med);

        void onDeleteClicked(MedicineReminder med);
    }

    private final List<MedicineReminder> items;
    private final MedicineActionListener listener;

    public MedicineAdapter(List<MedicineReminder> items, MedicineActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicineReminder med = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(med.getName());
        holder.tvDosage.setText(med.getDosage());
        holder.tvDosage.setVisibility(med.getDosage().isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvTime.setText(formatMetaTo12Hour(med.getTime()));

        // Toggle button state
        if (med.isEnabled()) {
            holder.btnToggle.setText(ctx.getString(R.string.on));
            holder.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_on_bg));
            holder.btnToggle.setTextColor(ctx.getColor(R.color.toggle_on_text));
        } else {
            holder.btnToggle.setText(ctx.getString(R.string.off));
            holder.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_off_bg));
            holder.btnToggle.setTextColor(ctx.getColor(R.color.toggle_off_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onEditClicked(med));
        holder.btnToggle.setOnClickListener(v -> listener.onToggleClicked(med));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(med));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDosage, tvTime;
        MaterialButton btnToggle;
        ImageButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_med_name);
            tvDosage = itemView.findViewById(R.id.tv_med_dosage);
            tvTime = itemView.findViewById(R.id.tv_med_time);
            btnToggle = itemView.findViewById(R.id.btn_toggle);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    /**
     * Converts any HH:mm occurrences inside a string into 12-hour format.
     * Example: "After Breakfast • 14:30" -> "After Breakfast • 2:30 PM"
     */
    private String formatMetaTo12Hour(String text) {
        if (text == null || text.trim().isEmpty()) return "";

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            matcher.appendReplacement(sb, formatTime12Hour(hour, minute));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String formatTime12Hour(int hour24, int minute) {
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;

        String amPm = hour24 < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm);
    }
}