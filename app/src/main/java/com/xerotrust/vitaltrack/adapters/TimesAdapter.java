package com.xerotrust.vitaltrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.MedicineTime;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimesAdapter extends RecyclerView.Adapter<TimesAdapter.VH> {

    public interface Listener {
        void onDelete(int position);

        void onEdit(int position);
    }

    private final List<MedicineTime> times;
    private final Listener listener;

    public TimesAdapter(List<MedicineTime> times, Listener listener) {
        this.times = times;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MedicineTime t = times.get(position);
        h.tvTime.setText(format12Hour(t.getHour(), t.getMinute()));

        h.btnEdit.setOnClickListener(v -> {
            int adapterPosition = h.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onEdit(adapterPosition);
            }
        });

        h.btnDelete.setOnClickListener(v -> {
            int adapterPosition = h.getAdapterPosition();
            if (listener != null && adapterPosition != RecyclerView.NO_POSITION) {
                listener.onDelete(adapterPosition);
            }
        });
    }

    private String format12Hour(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.getTime());
    }

    @Override
    public int getItemCount() {
        return times.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTime;
        ImageView btnEdit, btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnEdit = itemView.findViewById(R.id.btn_edit_time);
            btnDelete = itemView.findViewById(R.id.btn_delete_time);
        }
    }
}