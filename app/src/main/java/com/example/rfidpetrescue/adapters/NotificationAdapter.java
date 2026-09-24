package com.example.rfidpetrescue.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifList;

    public NotificationAdapter(List<Notification> notifList) {
        this.notifList = notifList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification model = notifList.get(position);
        holder.tvTitle.setText(model.getTitle());
        holder.tvContent.setText(model.getMessage());

        // Định dạng thời gian
        if (model.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault());
            holder.tvTime.setText(sdf.format(new Date(model.getTimestamp())));
            holder.tvTime.setVisibility(View.VISIBLE);
        } else {
            holder.tvTime.setVisibility(View.GONE);
        }

        // Chấm đỏ + màu nền: Chưa đọc = hồng nhạt + chấm đỏ, Đã đọc = trắng + ẩn chấm
        if (!model.isRead()) {
            holder.cardNotification.setCardBackgroundColor(android.graphics.Color.parseColor("#FFE0E6"));
            if (holder.dotUnread != null) holder.dotUnread.setVisibility(View.VISIBLE);
        } else {
            holder.cardNotification.setCardBackgroundColor(android.graphics.Color.WHITE);
            if (holder.dotUnread != null) holder.dotUnread.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return notifList != null ? notifList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvContent, tvTime;
        com.google.android.material.card.MaterialCardView cardNotification;
        View dotUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotifTitle);
            tvContent = itemView.findViewById(R.id.tvNotifContent);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            cardNotification = itemView.findViewById(R.id.cardNotification);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}