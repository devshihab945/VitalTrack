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
import com.xerotrust.vitaltrack.models.WaterAlarm;

import java.util.List;

public class WaterAlarmAdapter extends RecyclerView.Adapter<WaterAlarmAdapter.VH> {

    public interface Listener {
        void onEdit(WaterAlarm alarm);
        void onToggle(WaterAlarm alarm);
        void onDelete(WaterAlarm alarm);
    }

    private final List<WaterAlarm> items;
    private final Listener listener;

    public WaterAlarmAdapter(List<WaterAlarm> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_water_alarm_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        WaterAlarm alarm = items.get(position);
        Context ctx = h.itemView.getContext();

        h.tvTime.setText(alarm.hhmm());
        h.tvLabel.setText(alarm.getLabel() == null || alarm.getLabel().isEmpty()
                ? "Water Reminder" : alarm.getLabel());
        h.tvType.setText("ring".equalsIgnoreCase(alarm.getAlertType()) ? "🔔 Ring" : "📳 Vibrate");

        if (alarm.isEnabled()) {
            h.btnToggle.setText(ctx.getString(R.string.on));
            h.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_on_bg));
            h.btnToggle.setTextColor(ctx.getColor(R.color.toggle_on_text));
        } else {
            h.btnToggle.setText(ctx.getString(R.string.off));
            h.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_off_bg));
            h.btnToggle.setTextColor(ctx.getColor(R.color.toggle_off_text));
        }

        h.itemView.setOnClickListener(v -> listener.onEdit(alarm));
        h.btnToggle.setOnClickListener(v -> listener.onToggle(alarm));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(alarm));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime, tvLabel, tvType;
        MaterialButton btnToggle;
        ImageButton btnDelete;

        VH(@NonNull View v) {
            super(v);
            tvTime   = v.findViewById(R.id.tv_alarm_time);
            tvLabel  = v.findViewById(R.id.tv_alarm_label);
            tvType   = v.findViewById(R.id.tv_alarm_type);
            btnToggle = v.findViewById(R.id.btn_toggle);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
