package com.example.rfidpetrescue.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Notification;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.ViewHolder> {

    private List<Notification> notifList;
    private OnNotifClickListener listener;

    public interface OnNotifClickListener {
        void onNotifClick(Notification notification);
    }

    public AdminNotificationAdapter(List<Notification> notifList, OnNotifClickListener listener) {
        this.notifList = notifList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification model = notifList.get(position);
        holder.tvTitle.setText(model.getTitle());
        holder.tvContent.setText(model.getMessage());

        // --- ĐÃ SỬA: XÓA NĂM (yyyy) ĐỂ TIẾT KIỆM DIỆN TÍCH HIỂN THỊ ---
        if (model.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(model.getTimestamp())));
        }

        // Logic background, alpha và chấm đỏ cho read/unread
        if (model.isRead()) {
            holder.cardNotification.setCardBackgroundColor(Color.WHITE);
            holder.imgNotifIcon.setAlpha(0.3f);
            if (holder.dotUnread != null) holder.dotUnread.setVisibility(View.GONE);
        } else {
            holder.cardNotification.setCardBackgroundColor(Color.parseColor("#E8EAF6"));
            holder.imgNotifIcon.setAlpha(1.0f);
            if (holder.dotUnread != null) holder.dotUnread.setVisibility(View.VISIBLE);
        }

        // Icon based on type
        if ("donate".equals(model.getType())) {
            holder.imgNotifIcon.setImageResource(R.drawable.ic_wallet);
        } else if ("adopt".equals(model.getType())) {
            holder.imgNotifIcon.setImageResource(R.drawable.ic_home_active);
        } else {
            holder.imgNotifIcon.setImageResource(R.drawable.ic_info);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotifClick(model);
        });
    }

    @Override
    public int getItemCount() {
        return notifList != null ? notifList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        ImageView imgNotifIcon;
        MaterialCardView cardNotification;
        View dotUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvContent = itemView.findViewById(R.id.tvNotifContent);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            imgNotifIcon = itemView.findViewById(R.id.imgNotifIcon);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}