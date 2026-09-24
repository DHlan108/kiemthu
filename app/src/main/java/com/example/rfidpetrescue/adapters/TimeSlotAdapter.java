package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.TimeSlot;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    private final List<TimeSlot> slots;
    private final Context context;

    public TimeSlotAdapter(List<TimeSlot> slots, Context context) {
        this.slots = slots;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimeSlot slot = slots.get(position);
        holder.tvTime.setText(slot.getTime());

        if (slot.isSelected()) {
            holder.cardTimeSlot.setStrokeColor(Color.parseColor("#1D1A9B"));
            holder.cardTimeSlot.setStrokeWidth(4);
            holder.cardTimeSlot.setCardBackgroundColor(Color.parseColor("#DFE2FF"));
            holder.tvTime.setTextColor(Color.parseColor("#1D1A9B"));
            holder.tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            holder.cardTimeSlot.setStrokeColor(Color.parseColor("#E0E0E0"));
            holder.cardTimeSlot.setStrokeWidth(1);
            holder.cardTimeSlot.setCardBackgroundColor(Color.WHITE);
            holder.tvTime.setTextColor(Color.parseColor("#666666"));
            holder.tvTime.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        holder.itemView.setOnClickListener(v -> {
            slot.setSelected(!slot.isSelected());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTimeSlot;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTimeSlot = itemView.findViewById(R.id.cardTimeSlot);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}