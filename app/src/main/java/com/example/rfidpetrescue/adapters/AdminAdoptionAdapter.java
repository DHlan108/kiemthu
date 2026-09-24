package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.admin.category.adopt.AdminAdoptDetailFragment;
import com.example.rfidpetrescue.models.AdoptionApplication;

// --- Thêm các import của Firebase ---
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminAdoptionAdapter extends RecyclerView.Adapter<AdminAdoptionAdapter.ViewHolder> {

    private List<AdoptionApplication> appList;
    private Context context;

    public AdminAdoptionAdapter(List<AdoptionApplication> appList, Context context) {
        this.appList = appList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_adoption_application, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdoptionApplication app = appList.get(position);

        holder.tvApplicantName.setText(app.getAdopterName() != null ? app.getAdopterName() : "Không rõ tên");
        holder.tvEmailValue.setText(app.getAdopterEmail());
        holder.tvPhoneValue.setText(app.getAdopterPhone());
        holder.tvAddressValue.setText(app.getLocation());

        // --- BẮT ĐẦU ĐOẠN XỬ LÝ LẤY TÊN THÚ CƯNG ---
        String petId = app.getPetId();
        if (petId != null && !petId.isEmpty()) {
            holder.tvPetIdValue.setText("Đang tải..."); // Hứng UI trong lúc chờ mạng

            DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(petId);
            petRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String petName = snapshot.child("name").getValue(String.class);
                        // Có tên thì hiện tên, không có thì hiện ID
                        holder.tvPetIdValue.setText(petName != null ? petName : petId);
                    } else {
                        // Con vật bị xóa khỏi DB thì vẫn hiện ID cũ
                        holder.tvPetIdValue.setText(petId);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    holder.tvPetIdValue.setText(petId);
                }
            });
        } else {
            holder.tvPetIdValue.setText("N/A");
        }
        // --- KẾT THÚC ĐOẠN XỬ LÝ LẤY TÊN THÚ CƯNG ---

        // Định dạng ngày (Firebase lưu createdAt là chuỗi milli)
        if (app.getCreatedAt() != null) {
            try {
                long time = Long.parseLong(app.getCreatedAt());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                holder.tvRegDateValue.setText(sdf.format(new Date(time)));
            } catch (Exception e) {
                holder.tvRegDateValue.setText(app.getCreatedAt()); // Fallback nếu chuỗi không phải số
            }
        }

        // Cập nhật giao diện Status Chip
        String status = app.getStatus();
        holder.tvStatusChip.setText(status);
        GradientDrawable bgShape = (GradientDrawable) holder.tvStatusChip.getBackground();

        if (status == null) status = "Pending";

        switch (status) {
            case "Pending":
                holder.tvStatusChip.setText("Đang chờ");
                bgShape.setColor(Color.parseColor("#BCBCBC")); // Xám
                break;
            case "Interview":
                holder.tvStatusChip.setText("Chờ phỏng vấn");
                bgShape.setColor(Color.parseColor("#100E55")); // Xanh dương
                break;
            case "Approved":
                holder.tvStatusChip.setText("Đã duyệt");
                bgShape.setColor(Color.parseColor("#B2DF20")); // Xanh lá
                break;
            case "Rejected":
                holder.tvStatusChip.setText("Đã từ chối");
                bgShape.setColor(Color.parseColor("#D40000")); // Đỏ
                break;
            default:
                holder.tvStatusChip.setText(status);
                bgShape.setColor(Color.parseColor("#BCBCBC")); // Xám
                break;
        }

        // Xử lý sự kiện click để chuyển sang màn hình chi tiết
        holder.itemView.setOnClickListener(v -> {
            AdminAdoptDetailFragment fragment = new AdminAdoptDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("APPOINTMENT_ID", app.getId());
            fragment.setArguments(bundle);

            if (context instanceof AppCompatActivity) {
                ((AppCompatActivity) context).getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvApplicantName, tvStatusChip, tvRegDateValue, tvPetIdValue;
        TextView tvEmailValue, tvAddressValue, tvPhoneValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvApplicantName = itemView.findViewById(R.id.tvApplicantName);
            tvStatusChip = itemView.findViewById(R.id.tvStatusChip);
            tvRegDateValue = itemView.findViewById(R.id.tvRegDateValue);
            tvPetIdValue = itemView.findViewById(R.id.tvPetIdValue);
            tvEmailValue = itemView.findViewById(R.id.tvEmailValue);
            tvAddressValue = itemView.findViewById(R.id.tvAddressValue);
            tvPhoneValue = itemView.findViewById(R.id.tvPhoneValue);
        }
    }
}