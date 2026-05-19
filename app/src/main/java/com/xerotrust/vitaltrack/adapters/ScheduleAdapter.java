package com.xerotrust.vitaltrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.MedicineScheduleItem;

import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    public interface Listener {
        void onDelete(int position);
        void onEdit(int position);
    }

    private final List<MedicineScheduleItem> items;
    private final Listener listener;

    public ScheduleAdapter(List<MedicineScheduleItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MedicineScheduleItem it = items.get(position);

        h.tvLabelIcon.setText(emojiForLabel(it.getLabel()));
        h.tvLabel.setText(it.getLabel() == null ? "" : it.getLabel());

        String dosage = it.getDosage();
        if (dosage == null || dosage.trim().isEmpty()) {
            h.tvDosage.setVisibility(View.GONE);
        } else {
            h.tvDosage.setVisibility(View.VISIBLE);
            h.tvDosage.setText(dosage);
        }

        h.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(h.getAdapterPosition());
        });
        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(h.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String emojiForLabel(String label) {
        if (label == null) return "💊";
        String l = label.toLowerCase();
        if (l.contains("breakfast")) return "🌅";
        if (l.contains("lunch"))     return "☀️";
        if (l.contains("dinner"))    return "🌙";
        if (l.contains("sleep"))     return "😴";
        if (l.contains("before"))    return "⏱️";
        return "💊";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvLabelIcon, tvLabel, tvDosage;
        ImageView btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvLabelIcon = itemView.findViewById(R.id.tv_label_icon);
            tvLabel     = itemView.findViewById(R.id.tv_label);
            tvDosage    = itemView.findViewById(R.id.tv_dosage);
            btnEdit     = itemView.findViewById(R.id.btn_edit);
            btnDelete   = itemView.findViewById(R.id.btn_delete);
        }
    }
}
