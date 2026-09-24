package com.example.rfidpetrescue.adapters;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.ScheduleTask;
import com.example.rfidpetrescue.fragments.admin.category.schedule.receivers.ScheduleNotificationReceiver;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminScheduleAdapter extends RecyclerView.Adapter<AdminScheduleAdapter.ViewHolder> {

    private OnItemClickListener listener;
    private final List<ScheduleTask> taskList;
    private final Context context;

    public AdminScheduleAdapter(List<ScheduleTask> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    // Giao diện để Fragment bắt sự kiện click
    public interface OnItemClickListener {
        void onItemClick(ScheduleTask task);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleTask task = taskList.get(position);

        // Xử lý hiển thị hệ 24 giờ
        String time = task.getTime() != null ? task.getTime() : "--:--";
        if (time.toLowerCase().contains("m")) {
            try {
                SimpleDateFormat inFmt = new SimpleDateFormat("hh:mm a", Locale.US);
                SimpleDateFormat outFmt = new SimpleDateFormat("HH:mm", Locale.US);
                time = outFmt.format(inFmt.parse(time));
            } catch (Exception ignored) {}
        }
        holder.tvTime.setText(time);

        // --- 1. DÒNG TRÊN CÙNG (Chữ đậm): Luôn để Phân loại ---
        String type = task.getType() != null ? task.getType() : "Công việc";
        holder.tvTaskTitle.setText(type);

        // --- 2. DÒNG BÊN DƯỚI (Chữ nhỏ): Hiển thị Tên công việc (tiêm Noi) ---
        String bottomText = task.getTitle();

        // Nếu Tên công việc bị bỏ trống, thì thử lấy Ghi chú
        if (bottomText == null || bottomText.trim().isEmpty()) {
            bottomText = task.getDescription();
        }

        // Nếu cả Tên và Ghi chú đều trống, tự động tạo mô tả mặc định
        if (bottomText == null || bottomText.trim().isEmpty()) {
            if ("Tiêm chủng".equals(type)) {
                String pet = (task.getPetName() != null && !task.getPetName().isEmpty()) ? task.getPetName() : "";
                String vac = (task.getVaccineType() != null && !task.getVaccineType().isEmpty()) ? task.getVaccineType() : "";
                if (!pet.isEmpty() || !vac.isEmpty()) {
                    bottomText = "Tái chủng " + vac + " cho bé " + pet;
                } else {
                    bottomText = "Lịch tiêm chủng định kỳ";
                }
            } else if ("Nhận nuôi".equals(type)) {
                String adopter = (task.getAdopterName() != null && !task.getAdopterName().isEmpty()) ? task.getAdopterName() : "";
                String pet = (task.getPetName() != null && !task.getPetName().isEmpty()) ? task.getPetName() : "";
                if (!pet.isEmpty() || !adopter.isEmpty()) {
                    bottomText = "Phỏng vấn " + adopter + " nhận nuôi bé " + pet;
                } else {
                    bottomText = "Lịch phỏng vấn nhận nuôi";
                }
            } else {
                bottomText = "Chưa có nội dung";
            }
        }
        holder.tvTaskDesc.setText(bottomText);

        // Set Icon tương ứng
        if ("Tiêm chủng".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_vaccine);
        } else if ("Nhận nuôi".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_heart_fill);
        } else if ("Donate".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_wallet);
        } else if ("Bệnh án".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_medical);
        } else if ("Lịch".equals(type)) {
            holder.ivIcon.setImageResource(R.drawable.ic_calendar);
        }

        // Kiểm tra quá giờ để đổi màu
        boolean isReached = checkIfTimeReached(task.getDate(), time);
        if (isReached) {
            holder.tvTime.setTextColor(Color.parseColor("#1D1A9B"));
            holder.tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.cardTask.setCardBackgroundColor(Color.parseColor("#E8E8FF"));
            holder.ivIcon.setColorFilter(Color.parseColor("#1D1A9B"));
            holder.tvTaskTitle.setTextColor(Color.parseColor("#1D1A9B"));
            holder.tvTaskDesc.setTextColor(Color.parseColor("#1D1A9B"));
        } else {
            holder.tvTime.setTextColor(Color.parseColor("#858585"));
            holder.tvTime.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.cardTask.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            holder.ivIcon.setColorFilter(Color.parseColor("#858585"));
            holder.tvTaskTitle.setTextColor(Color.parseColor("#858585"));
            holder.tvTaskDesc.setTextColor(Color.parseColor("#858585"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(task);
        });

        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Xóa lịch trình")
                    .setMessage("Bạn có chắc chắn muốn xóa công việc này?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteTask(task))
                    .setNegativeButton("Hủy", null)
                    .show();
            return true;
        });
    }

    private void deleteTask(ScheduleTask task) {
        if (task.getDate() == null || task.getId() == null) return;
        FirebaseDatabase.getInstance().getReference("Admin_Schedules")
                .child(task.getDate())
                .child(task.getId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Đã xóa lịch trình", Toast.LENGTH_SHORT).show();
                    cancelNotification(task);
                });
    }

    private void cancelNotification(ScheduleTask task) {
        if (task.getId() == null) return;
        Intent intent = new Intent(context, ScheduleNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                task.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private boolean checkIfTimeReached(String dateStr, String timeStr) {
        if (dateStr == null || timeStr == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
            Date taskDateTime = sdf.parse(dateStr + " " + timeStr);
            if (taskDateTime != null) {
                return new Date().after(taskDateTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvTaskTitle, tvTaskDesc;
        MaterialCardView cardTask;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvTaskDesc = itemView.findViewById(R.id.tvTaskDesc);
            cardTask = itemView.findViewById(R.id.cardTask);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}