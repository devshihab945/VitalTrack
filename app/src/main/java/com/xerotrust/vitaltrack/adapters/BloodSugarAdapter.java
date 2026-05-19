package com.xerotrust.vitaltrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.BloodSugarRecord;

import java.util.List;

public class BloodSugarAdapter extends RecyclerView.Adapter<BloodSugarAdapter.ViewHolder> {

    public interface OnActionListener {
        void onEdit(int position);
        void onDelete(int position);
    }

    private final List<BloodSugarRecord> records;
    private final OnActionListener listener;

    public BloodSugarAdapter(List<BloodSugarRecord> records, OnActionListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sugar_record, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BloodSugarRecord r = records.get(position);

        h.tvValue.setText(String.format("%.0f", r.getValue()));
        h.tvStatus.setText(r.getStatus());
        h.tvDate.setText(r.getDate());

        int color = getStatusColor(r.getStatus());
        h.tvValue.setTextColor(color);
        h.tvStatus.setTextColor(color);
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

    private int getStatusColor(String status) {
        switch (status) {
            case "Low":                 return 0xFF2196F3;
            case "Normal (Fasting)":
            case "Normal (Post-meal)": return 0xFF4CAF50;
            case "Pre-diabetic":       return 0xFFFF9800;
            case "High":               return 0xFFF44336;
            default:                   return 0xFF9C27B0;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View accentStripe;
        TextView tvValue, tvStatus, tvDate, tvNote;
        ImageView btnEdit, btnDelete;

        ViewHolder(View v) {
            super(v);
            accentStripe = v.findViewById(R.id.accent_stripe);
            tvValue      = v.findViewById(R.id.tv_sugar_value);
            tvStatus     = v.findViewById(R.id.tv_status);
            tvDate       = v.findViewById(R.id.tv_date);
            tvNote       = v.findViewById(R.id.tv_note);
            btnEdit      = v.findViewById(R.id.btn_edit);
            btnDelete    = v.findViewById(R.id.btn_delete);
        }
    }
}
