package com.xerotrust.vitaltrack.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.xerotrust.vitaltrack.R;
import com.xerotrust.vitaltrack.models.QuickActionItem;

import java.util.List;

public class QuickActionAdapter extends RecyclerView.Adapter<QuickActionAdapter.VH> {

    public interface OnActionClickListener {
        void onActionClick(QuickActionItem item, int position);
    }

    private final List<QuickActionItem> items;
    private final OnActionClickListener listener;

    public QuickActionAdapter(List<QuickActionItem> items, OnActionClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quick_action, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        QuickActionItem item = items.get(position);

        h.tvTitle.setText(item.getTitle());
        h.ivIcon.setImageResource(item.getIconRes());
        h.iconContainer.setBackgroundResource(item.getIconBackgroundRes());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(item, position);
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        RelativeLayout iconContainer;
        ImageView ivIcon;
        TextView tvTitle;

        VH(@NonNull View itemView) {
            super(itemView);
            iconContainer = itemView.findViewById(R.id.icon_container);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvTitle = itemView.findViewById(R.id.tv_title);
        }
    }
}