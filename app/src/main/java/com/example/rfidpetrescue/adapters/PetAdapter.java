package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.user.appointment.PetDetailFragment;
import com.example.rfidpetrescue.models.Pet;

import java.util.List;
import android.util.Log;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private final List<Pet> pets;
    private final Context context;

    public PetAdapter(List<Pet> pets, Context context) {
        this.pets = pets;
        this.context = context;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = pets.get(position);

        holder.txtName.setText(pet.getName() != null ? pet.getName() : "Unknown");
        holder.txtBreed.setText(pet.getBreed() != null ? pet.getBreed() : "Unknown");

        String imageUrl = pet.getImageUrl();
        Log.d("KIEM_TRA_ANH", "Tên Pet: " + pet.getName() + " | Link ảnh: " + imageUrl);

        if (context != null) {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.img_happy)
                        .error(R.drawable.img_bored)
                        .into(holder.imgPet);
            } else {
                // Trường hợp dữ liệu trên Firebase để trống ""
                holder.imgPet.setImageResource(R.drawable.img_happy);
            }
        }

        if (pet.getGender() != null) {
            if (pet.getGender().equalsIgnoreCase("Female") || pet.getGender().equalsIgnoreCase("Cái")) {
                holder.imgGender.setImageResource(R.drawable.ic_female);
                holder.imgGender.setColorFilter(Color.parseColor("#FF5C8D"));
            } else {
                holder.imgGender.setImageResource(R.drawable.ic_male);
                holder.imgGender.setColorFilter(Color.parseColor("#4A90E2"));
            }
        }

        // Sự kiện Click mở trang chi tiết
        holder.itemView.setOnClickListener(v -> {
            if (pet.getId() != null) {
                PetDetailFragment fragment = new PetDetailFragment();
                Bundle bundle = new Bundle();
                bundle.putString("PET_ID", pet.getId());
                fragment.setArguments(bundle);

                AppCompatActivity activity = (AppCompatActivity) v.getContext();
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                if (context != null) {
                    Toast.makeText(context, "Pet ID is missing!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public int getItemCount() { return (pets != null) ? pets.size() : 0; }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPet, imgGender;
        TextView txtName, txtBreed;

        PetViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPet = itemView.findViewById(R.id.imgPetAvatar);
            txtName = itemView.findViewById(R.id.tvPetNameItem);
            txtBreed = itemView.findViewById(R.id.tvBreedItem);
            imgGender = itemView.findViewById(R.id.icGenderItem);
        }
    }
}