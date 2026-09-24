package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.ChatChannel;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatChannel> chatList;
    private Context context;
    private OnChatClickListener listener;
    private OnChatLongClickListener longListener;
    private boolean isAdmin;

    public interface OnChatClickListener {
        void onChatClick(ChatChannel chatChannel);
    }

    public interface OnChatLongClickListener {
        void onChatLongClick(ChatChannel chatChannel);
    }

    public ChatAdapter(List<ChatChannel> chatList, Context context, boolean isAdmin, OnChatClickListener listener) {
        this.chatList = chatList;
        this.context = context;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void setOnChatLongClickListener(OnChatLongClickListener longListener) {
        this.longListener = longListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatChannel chat = chatList.get(position);
        holder.tvName.setText(chat.getName());
        holder.tvLastMsg.setText(chat.getLastMessage());
        holder.tvTime.setText(chat.getTime());

        Glide.with(context)
                .load(chat.getAvatarUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_unactive)
                .into(holder.imgAvatar);

        // Xử lý giao diện tin nhắn chưa đọc
        if (chat.isUnread()) {
            holder.dotUnread.setVisibility(View.VISIBLE);
            
            // In đậm tên và tin nhắn cuối bằng Typeface.BOLD
            holder.tvName.setTypeface(null, Typeface.BOLD);
            holder.tvLastMsg.setTypeface(null, Typeface.BOLD);
            holder.tvLastMsg.setTextColor(Color.BLACK);

            if (isAdmin) {
                // Admin: Chấm màu xanh, Tên màu xanh đậm
                holder.dotUnread.setBackgroundResource(R.drawable.bg_dot_unread_admin);
                holder.tvName.setTextColor(Color.parseColor("#1D1A9E"));
            } else {
                // User: Chấm màu hồng, Tên màu hồng
                holder.dotUnread.setBackgroundResource(R.drawable.bg_dot_unread);
                holder.tvName.setTextColor(Color.parseColor("#FF5C8D"));
            }
        } else {
            // Trạng thái bình thường khi đã đọc
            holder.dotUnread.setVisibility(View.GONE);
            holder.tvName.setTypeface(null, Typeface.NORMAL);
            holder.tvName.setTextColor(Color.parseColor("#333333"));
            holder.tvLastMsg.setTypeface(null, Typeface.NORMAL);
            holder.tvLastMsg.setTextColor(Color.GRAY);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChatClick(chat);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onChatLongClick(chat);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() { return chatList.size(); }

    class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName, tvLastMsg, tvTime;
        View dotUnread;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgUserChat);
            tvName = itemView.findViewById(R.id.tvUserNameChat);
            tvLastMsg = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            dotUnread = itemView.findViewById(R.id.dotUnread);
        }
    }
}
