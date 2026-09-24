package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment;
import com.example.rfidpetrescue.models.Pet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPetAdapter extends RecyclerView.Adapter<AdminPetAdapter.ViewHolder> {

    private final List<Pet> petList;
    private final Context context;
    private OnPetClickListener listener;
    private Map<String, String> petSubtitleMap = new HashMap<>(); // Lưu thông tin phụ (vd: Ngày khám)

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public AdminPetAdapter(List<Pet> petList, Context context) {
        this.petList = petList;
        this.context = context;
    }

    public AdminPetAdapter(List<Pet> petList, Context context, OnPetClickListener listener) {
        this.petList = petList;
        this.context = context;
        this.listener = listener;
    }

    // Hàm để cập nhật thông tin phụ từ bên ngoài (như ngày khám gần nhất)
    public void setPetSubtitles(Map<String, String> subtitles) {
        this.petSubtitleMap = subtitles;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pet_admin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Pet pet = petList.get(position);

        String petName = (pet.getName() != null && !pet.getName().isEmpty()) ? pet.getName() : "Chưa có tên";
        
        // Đảo lại: Tên thú cưng in đậm ở trên
        holder.tvRFID.setText(petName);
        
        // Hiển thị thông tin phụ: Nếu có ngày khám thì hiện ngày khám, không thì hiện mã RFID
        if (petSubtitleMap != null && petSubtitleMap.containsKey(pet.getId())) {
            holder.tvDescription.setText(petSubtitleMap.get(pet.getId()));
            holder.tvDescription.setTextColor(context.getResources().getColor(R.color.blue_dark)); // Làm nổi bật ngày khám
        } else {
            String rfid = pet.getRfid_tag_id() != null ? pet.getRfid_tag_id() : "No RFID";
            holder.tvDescription.setText("Mã: " + rfid);
            holder.tvDescription.setTextColor(0xFF999999);
        }

        if (pet.getStatus() != null && !pet.getStatus().isEmpty()) {
            holder.tvStatusTag.setVisibility(View.VISIBLE);
            holder.tvStatusTag.setText(pet.getStatus());
        } else {
            holder.tvStatusTag.setVisibility(View.GONE);
        }

        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(pet.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_dog_manage)
                    .into(holder.imgPet);
        } else {
            holder.imgPet.setImageResource(R.drawable.ic_dog_manage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPetClick(pet);
            } else {
                PetInfoAdminFragment fragment = new PetInfoAdminFragment();
                Bundle args = new Bundle();
                args.putString("PET_ID", pet.getId());
                args.putString("PET_IMAGE_URL", pet.getImageUrl());
                fragment.setArguments(args);

                ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPet;
        TextView tvRFID, tvDescription, tvStatusTag;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPet = itemView.findViewById(R.id.imgPet);
            tvRFID = itemView.findViewById(R.id.tvRFID); // Trong layout này ID là tvRFID nhưng ta dùng hiện Tên
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatusTag = itemView.findViewById(R.id.tvStatusTag);
        }
    }
}
