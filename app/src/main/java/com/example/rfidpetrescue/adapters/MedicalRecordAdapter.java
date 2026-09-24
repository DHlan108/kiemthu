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
import com.example.rfidpetrescue.models.MedicalRecord;

import java.util.List;

public class MedicalRecordAdapter extends RecyclerView.Adapter<MedicalRecordAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(MedicalRecord record);
    }

    private final List<MedicalRecord> list;
    private final Context context;
    private OnItemClickListener listener;
    private boolean isUserMode = false;

    public MedicalRecordAdapter(List<MedicalRecord> list, Context context) {
        this.list = list;
        this.context = context;
    }

    // Thêm constructor mới cho User Mode
    public MedicalRecordAdapter(List<MedicalRecord> list, Context context, boolean isUserMode) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_medical_record_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalRecord r = list.get(position);

        if (holder.tvMedicalRecordId != null) {
            if (r.id != null) {
                holder.tvMedicalRecordId.setText(r.id.substring(0, Math.min(r.id.length(), 12)).toUpperCase());
            } else {
                holder.tvMedicalRecordId.setText("N/A");
            }
        }

        // Đổi màu nền mã hồ sơ cho User Mode
        if (isUserMode && holder.llRecordIdContainer != null) {
            holder.llRecordIdContainer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF6D8A")));
        }

        if (holder.tvRecordType != null) {
            String type = r.record_type != null ? r.record_type : "Khám bệnh";
            holder.tvRecordType.setText(type);

            if (isUserMode) {
                if ("Khám bệnh".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#19E8DB")));
                } else if ("Tiểu phẫu".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFB8C9")));
                } else if ("Phẫu thuật".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#BA304F")));
                } else {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
                }
            } else {
                if ("Khám bệnh".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#B2DF20")));
                } else if ("Tiểu phẫu".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#C9BDF4")));
                } else if ("Phẫu thuật".equals(type)) {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B")));
                } else {
                    holder.tvRecordType.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
                }
            }
        }

        if (holder.tvDate != null) {
            holder.tvDate.setText(r.date != null ? r.date : "--");
            if (isUserMode) {
                holder.tvDate.setTextColor(Color.parseColor("#19E8DB"));
            }
        }

        if (holder.tvSymptoms != null) holder.tvSymptoms.setText(r.symptoms != null && !r.symptoms.isEmpty() ? r.symptoms : "--");
        if (holder.tvClinicalSigns != null) holder.tvClinicalSigns.setText(r.clinical_signs != null && !r.clinical_signs.isEmpty() ? r.clinical_signs : "--");
        if (holder.tvDiagnosis != null) holder.tvDiagnosis.setText(r.diagnosis != null && !r.diagnosis.isEmpty() ? r.diagnosis : "--");
        if (holder.tvNotes != null) holder.tvNotes.setText(r.note != null && !r.note.isEmpty() ? r.note : "--");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(r);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicalRecordId, tvRecordType, tvDate, tvSymptoms, tvClinicalSigns, tvDiagnosis, tvNotes;
        View llRecordIdContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicalRecordId = itemView.findViewById(R.id.tvMedicalRecordId);
            tvRecordType      = itemView.findViewById(R.id.tvRecordType);
            tvDate            = itemView.findViewById(R.id.tvDate);
            tvSymptoms        = itemView.findViewById(R.id.tvSymptoms);
            tvClinicalSigns   = itemView.findViewById(R.id.tvClinicalSigns);
            tvDiagnosis       = itemView.findViewById(R.id.tvDiagnosis);
            tvNotes           = itemView.findViewById(R.id.tvNotes);
            llRecordIdContainer = itemView.findViewById(R.id.llRecordIdContainer);
        }
    }
}
