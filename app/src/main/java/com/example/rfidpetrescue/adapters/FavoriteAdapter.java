package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Pet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class FavoriteAdapter extends RecyclerView.Adapter<FavoriteAdapter.PetViewHolder> {
    private List<Pet> petList;
    private Context context;

    // --- THÊM INTERFACE ĐỂ XỬ LÝ CLICK ---
    public interface OnItemClickListener {
        void onItemClick(Pet pet);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public FavoriteAdapter(List<Pet> petList, Context context) {
        this.petList = petList;
        this.context = context;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_favorite_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.tvName.setText(pet.getName());
        holder.tvBreed.setText(pet.getBreed());
        holder.tvAge.setText(pet.getAge());
        holder.tvLocation.setText(pet.getLocation());

        if (context != null) {
            Glide.with(context)
                    .load(pet.getImageUrl())
                    .placeholder(R.drawable.img_happy)
                    .error(R.drawable.img_bored)
                    .centerCrop()
                    .into(holder.imgPet);
        }

        // ---  LOGIC HIỂN THỊ ICON GIỚI TÍNH ---
        String gender = pet.getGender();
        if (gender != null) {
            if (gender.equalsIgnoreCase("Đực") || gender.equalsIgnoreCase("Male")) {
                holder.imgSexIcon.setImageResource(R.drawable.ic_male);
                holder.imgSexIcon.setColorFilter(android.graphics.Color.parseColor("#1D1A9B"));
                holder.imgSexIcon.setVisibility(View.VISIBLE);
            } else if (gender.equalsIgnoreCase("Cái") || gender.equalsIgnoreCase("Female")) {
                holder.imgSexIcon.setImageResource(R.drawable.ic_female);
                holder.imgSexIcon.setColorFilter(android.graphics.Color.parseColor("#FF6D8A"));
                holder.imgSexIcon.setVisibility(View.VISIBLE);
            } else {
                holder.imgSexIcon.setVisibility(View.GONE);
            }
        } else {
            holder.imgSexIcon.setVisibility(View.GONE);
        }

        // --- LOGIC XÓA YÊU THÍCH ---
        holder.imgFavoriteHeart.setOnClickListener(v -> {
            if (pet.getId() == null) return;

            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child("Favorites")
                    .child(pet.getId());

            ref.removeValue().addOnSuccessListener(aVoid -> {
                if (context != null) {
                    Toast.makeText(context, "Removed from favorites", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // --- THÊM SỰ KIỆN CLICK VÀO TOÀN BỘ CARD ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return petList != null ? petList.size() : 0;
    }

    class PetViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvBreed, tvAge, tvLocation;
        ImageView imgPet, imgFavoriteHeart, imgSexIcon;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvPetName);
            tvBreed = itemView.findViewById(R.id.tvPetBreed);
            tvAge = itemView.findViewById(R.id.tvPetAge);
            tvLocation = itemView.findViewById(R.id.tvPetLocation);
            imgPet = itemView.findViewById(R.id.imgPet);
            imgFavoriteHeart = itemView.findViewById(R.id.imgFavoriteHeart);
            imgSexIcon = itemView.findViewById(R.id.imgSexIcon);
        }
    }
}