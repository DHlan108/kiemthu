package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.Vaccination;

import java.util.List;

public class VaccinationAdapter extends RecyclerView.Adapter<VaccinationAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Vaccination vaccination);
        void onEditClick(Vaccination vaccination);
        void onDeleteClick(Vaccination vaccination);
    }

    private final List<Vaccination> list;
    private final Context context;
    private OnItemClickListener listener;
    private boolean isUserMode = false;

    public VaccinationAdapter(List<Vaccination> list, Context context) {
        this.list = list;
        this.context = context;
    }

    // Thêm constructor dành cho User Mode
    public VaccinationAdapter(List<Vaccination> list, Context context, boolean isUserMode) {
        this.list = list;
        this.context = context;
        this.isUserMode = isUserMode;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_vaccination_record_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vaccination v = list.get(position);

        // Hiển thị mã hồ sơ
        if (v.id != null) {
            holder.tvRecordId.setText("Mã hồ sơ: " + v.id.substring(0, Math.min(v.id.length(), 12)).toUpperCase());
        } else {
            holder.tvRecordId.setText("Mã hồ sơ: N/A");
        }

        // Đổi màu nền mã hồ sơ cho User Mode (Hồng FF6D8A)
        if (isUserMode && holder.tvRecordId != null) {
            holder.tvRecordId.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6D8A")));
        }

        // Gán dữ liệu vào các TextView
        holder.tvVaccinationType.setText(v.vaccine_type != null ? v.vaccine_type : "Chưa rõ");
        holder.tvDate.setText(v.vaccine_date != null ? v.vaccine_date : "--");
        holder.tvVaccineName.setText(v.medicine_name != null ? v.medicine_name : "--");
        holder.tvNextDate.setText(v.revaccine_date != null && !v.revaccine_date.isEmpty() ? v.revaccine_date : "Không có");
        holder.tvNotes.setText(v.note != null && !v.note.isEmpty() ? v.note : "Không có ghi chú");

        // Logic đổi màu theo User Mode hoặc Admin Mode
        if (isUserMode) {
            // Bộ màu dành cho Người dùng
            if ("Ngừa dại".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#19E8DB"))); // Xanh Cyan
            } else if ("Ngừa bệnh".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB8C9"))); // Hồng nhạt
            } else if ("Sổ giun".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6D8A"))); // Hồng đậm
            } else if ("Ngoại kí sinh".equals(v.vaccine_type) || "Ngoại ký sinh".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BA304F"))); // Đỏ đô
            } else {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            }

            // Đổi màu chữ Thời gian và Ngày tái chủng sang Cyan (19E8DB)
            if (holder.tvDate != null) holder.tvDate.setTextColor(Color.parseColor("#19E8DB"));
            if (holder.tvNextDate != null) holder.tvNextDate.setTextColor(Color.parseColor("#19E8DB"));

        } else {
            // Bộ màu gốc của Admin
            if ("Ngừa dại".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B2DF20"))); // Xanh nõn chuối
            } else if ("Ngừa bệnh".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C9BDF4"))); // Tím nhạt
            } else if ("Sổ giun".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B"))); // Xanh dương đậm
            } else if ("Ngoại kí sinh".equals(v.vaccine_type) || "Ngoại ký sinh".equals(v.vaccine_type)) {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#627B12"))); // Xanh rêu
            } else {
                holder.tvVaccinationType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            }
        }

        // Gán sự kiện click
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onItemClick(v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecordId, tvVaccinationType, tvDate, tvVaccineName, tvNextDate, tvNotes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ View
            tvRecordId        = itemView.findViewById(R.id.tvRecordId);
            tvVaccinationType = itemView.findViewById(R.id.tvVaccinationType);
            tvDate            = itemView.findViewById(R.id.tvDate);
            tvVaccineName     = itemView.findViewById(R.id.tvVaccineName);
            tvNextDate        = itemView.findViewById(R.id.tvNextDate);
            tvNotes           = itemView.findViewById(R.id.tvNotes);
        }
    }
}
