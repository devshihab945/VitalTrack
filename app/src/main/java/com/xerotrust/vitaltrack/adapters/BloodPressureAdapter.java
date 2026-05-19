package com.xerotrust.vitaltrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.BloodPressureRecord;

import java.util.List;

public class BloodPressureAdapter extends RecyclerView.Adapter<BloodPressureAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<BloodPressureRecord> records;
    private final OnActionListener listener;

    public BloodPressureAdapter(List<BloodPressureRecord> records, OnActionListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bp_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BloodPressureRecord r = records.get(position);

        h.tvValue.setText(r.getDisplayBP());
        h.tvCategory.setText(r.getCategory());
        h.tvPulse.setText("💓 " + r.getPulse() + " bpm");
        h.tvDate.setText(r.getDate());

        int color = getCategoryColor(r.getCategory());
        h.tvValue.setTextColor(color);
        h.tvCategory.setTextColor(color);
        h.accentStripe.setBackgroundColor(color);

        if (r.getNote() != null && !r.getNote().isEmpty()) {
            h.tvNote.setText("📝 " + r.getNote());
            h.tvNote.setVisibility(View.VISIBLE);
        } else {
            h.tvNote.setVisibility(View.GONE);
        }

        h.btnEdit.setOnClickListener(v -> listener.onEdit(h.getAdapterPosition()));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(h.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    private int getCategoryColor(String category) {
        switch (category) {
            case "Normal":      return 0xFF4CAF50;
            case "Elevated":    return 0xFFFF9800;
            case "High Stage 1":return 0xFFF44336;
            case "High Stage 2":return 0xFFB71C1C;
            default:            return 0xFFE53935;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View accentStripe;
        TextView tvValue, tvCategory, tvPulse, tvDate, tvNote;
        ImageView btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            accentStripe = v.findViewById(R.id.accent_stripe);
            tvValue      = v.findViewById(R.id.tv_bp_value);
            tvCategory   = v.findViewById(R.id.tv_category);
            tvPulse      = v.findViewById(R.id.tv_pulse);
            tvDate       = v.findViewById(R.id.tv_date);
            tvNote       = v.findViewById(R.id.tv_note);
            btnEdit      = v.findViewById(R.id.btn_edit);
            btnDelete    = v.findViewById(R.id.btn_delete);
        }
    }
}
