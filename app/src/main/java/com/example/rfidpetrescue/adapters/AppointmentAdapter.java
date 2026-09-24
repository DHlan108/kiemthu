package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.user.appointment.AppointmentDetailFragment;
import com.example.rfidpetrescue.models.Appointment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.ViewHolder> {

    private List<Appointment> appointmentList;
    private Context context;

    public AppointmentAdapter(Context context, List<Appointment> appointmentList) {
        this.context = context;
        this.appointmentList = appointmentList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_appointment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Appointment apt = appointmentList.get(position);

        // 1. Xử lý dịch trạng thái sang Tiếng Việt và đổi màu
        String rawStatus = apt.getStatus() != null ? apt.getStatus() : "Pending";
        String displayStatus = rawStatus;

        if ("Pending".equals(rawStatus)) {
            displayStatus = "Đang chờ";
            holder.tvStatus.setTextColor(Color.parseColor("#BCBCBC"));
        } else if ("Interview".equals(rawStatus)) {
            displayStatus = "Phỏng vấn";
            holder.tvStatus.setTextColor(Color.parseColor("#000000"));
        } else if ("Approved".equals(rawStatus)) {
            displayStatus = "Đã duyệt";
            holder.tvStatus.setTextColor(Color.parseColor("#B2DF20"));
        } else if ("Rejected".equals(rawStatus)) {
            displayStatus = "Đã từ chối";
            holder.tvStatus.setTextColor(Color.parseColor("#D40000"));
        }

        // Gán chữ Tiếng Việt lên giao diện
        holder.tvStatus.setText(displayStatus);

        // 2. Hiển thị thông tin thời gian & địa điểm
        holder.tvTimeDate.setText((apt.getTime() != null ? apt.getTime() : "") + " - " + (apt.getDate() != null ? apt.getDate() : ""));
        holder.tvLocation.setText(apt.getLocation() != null ? apt.getLocation() : "Chưa cập nhật địa điểm");

        // Tải thông tin Thú cưng từ Firebase
        if (apt.getPetId() != null) {
            FirebaseDatabase.getInstance().getReference("Pets").child(apt.getPetId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                holder.tvPetName.setText(snapshot.child("name").getValue(String.class));
                                holder.tvPetBreed.setText(snapshot.child("breed").getValue(String.class));
                                String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                                if (imgUrl != null && !imgUrl.isEmpty()) {
                                    Glide.with(context).load(imgUrl).into(holder.imgPet);
                                }
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
        }
        holder.itemView.setOnClickListener(v -> {
            // Lấy thời gian hiện tại
            long currentTime = System.currentTimeMillis();
            long appointmentTime = parseAppointmentDate(apt.getDate());

            // Kiểm tra: Nếu là lịch chờ phỏng vấn / đã duyệt HOẶC thời gian chưa qua thì cho bấm
            if ("Interview".equals(apt.getStatus()) || "Approved".equals(apt.getStatus()) || appointmentTime >= currentTime) {

                AppointmentDetailFragment detailFragment = new AppointmentDetailFragment();
                android.os.Bundle bundle = new android.os.Bundle();
                bundle.putString("apt_id", apt.getId());
                detailFragment.setArguments(bundle);

                androidx.appcompat.app.AppCompatActivity activity = (androidx.appcompat.app.AppCompatActivity) context;
                activity.getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                android.widget.Toast.makeText(context, "Cuộc hẹn này đã kết thúc hoặc bị hủy!", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Hàm dịch chuỗi ngày Tiếng Việt thành số để sắp xếp
    private long parseAppointmentDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return 0;
        try {
            // Chuẩn hóa 1: Đọc định dạng Tiếng Việt (Ví dụ: "Thứ Tư, 29/04/2026")
            java.text.SimpleDateFormat sdfVi = new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN"));
            return sdfVi.parse(dateString).getTime();
        } catch (Exception e1) {
            try {
                // Dự phòng 2: Đọc định dạng ngày ngắn (Ví dụ: "26/05/2025")
                java.text.SimpleDateFormat sdfShort = new java.text.SimpleDateFormat("dd/MM/yyyy");
                return sdfShort.parse(dateString).getTime();
            } catch (Exception e2) {
                return 0; // Trả về 0 nếu dữ liệu bị hỏng
            }
        }
    }
    @Override
    public int getItemCount() {
        return appointmentList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvTimeDate, tvLocation, tvPetName, tvPetBreed;
        ImageView imgPet;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvTimeDate = itemView.findViewById(R.id.tvItemTimeDate);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvPetName = itemView.findViewById(R.id.tvItemPetName);
            tvPetBreed = itemView.findViewById(R.id.tvItemPetBreed);
            imgPet = itemView.findViewById(R.id.imgItemPet);
        }
    }
}