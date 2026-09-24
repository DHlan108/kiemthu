package com.example.rfidpetrescue.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment;
import com.example.rfidpetrescue.fragments.user.appointment.PetDetailFragment;
import com.example.rfidpetrescue.models.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final int TYPE_PET_CARD = 3;

    private List<ChatMessage> messageList;
    private String currentUserId;
    private boolean isAdmin;

    public MessageAdapter(List<ChatMessage> messageList, String currentUserId, boolean isAdmin) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
        this.isAdmin = isAdmin;
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        if (message != null && ChatMessage.TYPE_PET_CARD.equals(message.getType())) {
            return TYPE_PET_CARD;
        }
        if (message != null && message.getSender_id() != null && message.getSender_id().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PET_CARD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_pet_card, parent, false);
            return new PetCardViewHolder(view, isAdmin);
        } else if (viewType == TYPE_SENT) {
            int layoutId = isAdmin ? R.layout.item_admin_message_sent : R.layout.item_message_sent;
            View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        if (message == null) return;

        if (holder instanceof PetCardViewHolder) {
            ((PetCardViewHolder) holder).bind(message);
        } else if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            displayMessage(sentHolder.tvMessage, sentHolder.imgSent, message);
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            displayMessage(receivedHolder.tvMessage, receivedHolder.imgReceived, message);
        }
    }

    private void displayMessage(TextView textView, ImageView imageView, ChatMessage message) {
        String imageUrl = message.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            textView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
            Glide.with(imageView.getContext())
                    .load(imageUrl.trim())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_grid_unactive)
                    .centerCrop()
                    .into(imageView);
        } else {
            textView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            textView.setText(message.getText());
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView imgSent;
        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageSent);
            imgSent = v.findViewById(R.id.imgSent);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView imgReceived;
        ReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageReceived);
            imgReceived = v.findViewById(R.id.imgReceived);
        }
    }

    static class PetCardViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView tvPetName, tvPetBreed, tvTime, btnViewPet;
        boolean isAdmin;

        PetCardViewHolder(View v, boolean isAdmin) {
            super(v);
            this.isAdmin = isAdmin;
            imgPet = v.findViewById(R.id.imgPet);
            tvPetName = v.findViewById(R.id.tvPetName);
            tvPetBreed = v.findViewById(R.id.tvPetBreed);
            tvTime = v.findViewById(R.id.tvTime);
            btnViewPet = v.findViewById(R.id.btnViewPet);
        }

        void bind(ChatMessage message) {
            tvPetName.setText(message.getPetName());
            tvPetBreed.setText(message.getPetBreed());
            
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                tvTime.setText(sdf.format(new Date(message.getTimestamp())));
            }

            Glide.with(imgPet.getContext())
                    .load(message.getPetImage())
                    .placeholder(R.drawable.on2)
                    .into(imgPet);

            // Đồng bộ màu nút theo vai trò
            if (isAdmin) {
                btnViewPet.setBackgroundResource(R.drawable.bg_pill_dark_blue);
            } else {
                btnViewPet.setBackgroundResource(R.drawable.bg_button_enabled);
            }

            btnViewPet.setOnClickListener(v -> {
                if (v.getContext() instanceof AppCompatActivity) {
                    Fragment fragment;
                    if (isAdmin) {
                        fragment = new PetInfoAdminFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("PET_ID", message.getPetId());
                        bundle.putString("PET_IMAGE_URL", message.getPetImage());
                        fragment.setArguments(bundle);
                    } else {
                        fragment = new PetDetailFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("PET_ID", message.getPetId());
                        fragment.setArguments(bundle);
                    }
                    
                    ((AppCompatActivity) v.getContext()).getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }
}
