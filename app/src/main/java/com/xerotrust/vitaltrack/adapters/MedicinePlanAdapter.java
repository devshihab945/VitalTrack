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
import com.xerotrust.vitaltrack.models.MedicinePlan;

import java.util.List;

public class MedicinePlanAdapter extends RecyclerView.Adapter<MedicinePlanAdapter.ViewHolder> {

    public interface PlanActionListener {
        void onEditClicked(MedicinePlan plan);

        void onToggleClicked(MedicinePlan plan);

        void onDeleteClicked(MedicinePlan plan);
    }

    private final List<MedicinePlan> items;
    private final PlanActionListener listener;

    public MedicinePlanAdapter(List<MedicinePlan> items, PlanActionListener listener) {
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
        MedicinePlan plan = items.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(plan.getName());

        // Read computed strings directly from the plan object (set by fragment before notify)
        String nextDose = plan.getComputedNextDose();
        String meta     = plan.getComputedMeta();

        holder.tvDosage.setText(nextDose);
        holder.tvDosage.setVisibility(nextDose.isEmpty() ? View.GONE : View.VISIBLE);
        holder.tvTime.setText(meta);

        if (plan.isEnabled()) {
            holder.btnToggle.setText(ctx.getString(R.string.on));
            holder.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_on_bg));
            holder.btnToggle.setTextColor(ctx.getColor(R.color.toggle_on_text));
        } else {
            holder.btnToggle.setText(ctx.getString(R.string.off));
            holder.btnToggle.setBackgroundTintList(ctx.getColorStateList(R.color.toggle_off_bg));
            holder.btnToggle.setTextColor(ctx.getColor(R.color.toggle_off_text));
        }

        holder.itemView.setOnClickListener(v -> listener.onEditClicked(plan));
        holder.btnToggle.setOnClickListener(v -> listener.onToggleClicked(plan));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClicked(plan));
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
            tvName    = itemView.findViewById(R.id.tv_med_name);
            tvDosage  = itemView.findViewById(R.id.tv_med_dosage);
            tvTime    = itemView.findViewById(R.id.tv_med_time);
            btnToggle = itemView.findViewById(R.id.btn_toggle);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
