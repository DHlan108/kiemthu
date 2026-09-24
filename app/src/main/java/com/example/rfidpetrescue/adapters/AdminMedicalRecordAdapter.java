package com.example.rfidpetrescue.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.models.MedicalRecord;
import java.util.List;

public class AdminMedicalRecordAdapter extends RecyclerView.Adapter<AdminMedicalRecordAdapter.ViewHolder> {

    private final List<MedicalRecord> recordList;
    private final Context context;

    // --- 1. KHAI BÁO INTERFACE ĐỂ BẮT SỰ KIỆN CLICK ---
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MedicalRecord record);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    // ------------------------------------------------

    public AdminMedicalRecordAdapter(List<MedicalRecord> recordList, Context context) {
        this.recordList = recordList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_medical_record_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MedicalRecord record = recordList.get(position);

        holder.tvMedicalRecordId.setText("Mã hồ sơ: " + (record.id != null ? record.id : "N/A"));
        holder.tvDate.setText(record.date != null ? record.date : "N/A");
        holder.tvRecordType.setText(record.record_type != null ? record.record_type : "N/A");
        holder.tvSymptoms.setText(record.symptoms != null ? record.symptoms : "N/A");
        holder.tvClinicalSigns.setText(record.clinical_signs != null ? record.clinical_signs : "N/A");
        holder.tvDiagnosis.setText(record.diagnosis != null ? record.diagnosis : "N/A");
        holder.tvNotes.setText(record.note != null ? record.note : "N/A");

        // --- 2. LẮNG NGHE SỰ KIỆN KHI NGƯỜI DÙNG BẤM VÀO ITEM ---
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(record);
            }
        });
        // ---------------------------------------------------------
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedicalRecordId, tvDate, tvRecordType, tvSymptoms, tvClinicalSigns, tvDiagnosis, tvNotes;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedicalRecordId = itemView.findViewById(R.id.tvMedicalRecordId);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvRecordType = itemView.findViewById(R.id.tvRecordType);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvClinicalSigns = itemView.findViewById(R.id.tvClinicalSigns);
            tvDiagnosis = itemView.findViewById(R.id.tvDiagnosis);
            tvNotes = itemView.findViewById(R.id.tvNotes);
        }
    }
}