package com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.models.MedicalRecord;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MedicalRecordDetailFragment extends Fragment {

    private ImageView imgPetHeader, ivBarcode;
    private MaterialCardView btnBack;
    private TextView tvRecordId, tvRecordType, tvExamDate, tvSymptoms, tvClinicalSigns, tvDiagnosis, tvNote, tvRfid, tvPet;
    private MaterialButton btnEdit, btnDelete;

    private String petId;
    private String recordId;
    private String petImageUrl;

    // Lưu trữ record hiện tại để truyền sang màn Edit
    private MedicalRecord currentRecord;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Tham chiếu đúng layout Detail (layout chứa TextView)
        return inflater.inflate(R.layout.fragment_medical_record_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            recordId = getArguments().getString("RECORD_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");

            if (petImageUrl != null && !petImageUrl.isEmpty()) {
                Glide.with(this).load(petImageUrl).centerCrop()
                        .placeholder(R.drawable.ic_dog_manage).into(imgPetHeader);
            }

            if (recordId != null) {
                // Hiển thị mã hồ sơ rút gọn
                tvRecordId.setText(recordId.substring(0, Math.min(recordId.length(), 10)).toUpperCase());
                // Lắng nghe dữ liệu realtime để tự cập nhật khi từ màn Edit quay về
                listenToRecordChanges(recordId);
            }

            // Lấy thông tin RFID từ Firebase để tạo Barcode
            if (petId != null) {
                loadPetBasicInfo(petId);
            }
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        imgPetHeader = view.findViewById(R.id.imgPetHeader);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        btnBack = view.findViewById(R.id.btnBack);

        tvRecordId = view.findViewById(R.id.tvRecordId);
        tvRecordType = view.findViewById(R.id.tvRecordType);
        tvExamDate = view.findViewById(R.id.tvExamDate);
        tvSymptoms = view.findViewById(R.id.tvSymptoms);
        tvClinicalSigns = view.findViewById(R.id.tvClinicalSigns);
        tvDiagnosis = view.findViewById(R.id.tvDiagnosis);
        tvNote = view.findViewById(R.id.tvNote);
        tvRfid = view.findViewById(R.id.tvRfid);
        tvPet = view.findViewById(R.id.tvPet);
        btnEdit = view.findViewById(R.id.btnEdit);
        btnDelete = view.findViewById(R.id.btnDelete);
    }

    private void loadPetBasicInfo(String id) {
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(id);
        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    String rfid = snapshot.child("rfid_tag_id").getValue(String.class);
                    if (rfid == null) {
                        rfid = snapshot.child("rfid").getValue(String.class); // Fallback
                    }
                    String name = snapshot.child("name").getValue(String.class);
                    String petName = (name != null && !name.isEmpty()) ? name : "Chưa có tên";
                    if (tvPet != null) {
                        tvPet.setText(petName);
                    }
                    // Xử lý và tạo Barcode
                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) {
                            rfid = rfid.replace("tag_", "");
                        }

                        Bitmap barcode = generateBarcode(rfid);
                        if (barcode != null && ivBarcode != null) {
                            ivBarcode.setImageBitmap(barcode);
                        }
                        if (tvRfid != null) {
                            tvRfid.setText(rfid);
                        }
                    } else {
                        if (tvRfid != null) tvRfid.setText("Chưa có mã");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void listenToRecordChanges(String id) {
        FirebaseDatabase.getInstance().getReference("Medical_Records").child(id)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        if (snapshot.exists()) {
                            currentRecord = snapshot.getValue(MedicalRecord.class);
                            if (currentRecord != null) {
                                currentRecord.id = snapshot.getKey();
                                tvRecordType.setText(currentRecord.record_type != null ? currentRecord.record_type : "Chưa cập nhật");
                                tvExamDate.setText(currentRecord.date != null ? currentRecord.date : "Chưa cập nhật");
                                tvSymptoms.setText(currentRecord.symptoms != null ? currentRecord.symptoms : "Không có");
                                tvClinicalSigns.setText(currentRecord.clinical_signs != null ? currentRecord.clinical_signs : "Không có");
                                tvDiagnosis.setText(currentRecord.diagnosis != null ? currentRecord.diagnosis : "Không có");
                                tvNote.setText(currentRecord.note != null ? currentRecord.note : "Không có");
                            }
                        } else {
                            // Nếu record bị xoá (từ nơi khác hoặc thiết bị khác), đóng màn hình này
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        btnEdit.setOnClickListener(v -> {
            if (currentRecord != null) {
                openEditMedicalRecord(currentRecord);
            }
        });

        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void openEditMedicalRecord(MedicalRecord record) {
        EditMedicalRecordFragment editFragment = new EditMedicalRecordFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID", petId);
        args.putString("PET_IMAGE_URL", petImageUrl);
        args.putString("RECORD_ID", record.id);
        args.putString("RECORD_TYPE", record.record_type);
        args.putString("RECORD_DATE", record.date);
        args.putString("RECORD_SYMPTOMS", record.symptoms);
        args.putString("RECORD_CLINICAL", record.clinical_signs);
        args.putString("RECORD_DIAGNOSIS", record.diagnosis);
        args.putString("RECORD_NOTE", record.note);

        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                // Thêm animation chuyển cảnh cho đẹp mắt (tuỳ chọn)
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.main_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void confirmDelete() {
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Xoá bệnh án")
                .setMessage("Bạn có chắc muốn xoá bệnh án này không?")
                .setPositiveButton("Xoá", (d, w) -> deleteRecord())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteRecord() {
        if (recordId == null) return;
        FirebaseDatabase.getInstance().getReference("Medical_Records").child(recordId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã xoá bệnh án", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                });
    }

    private Bitmap generateBarcode(String text) {
        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(text, com.google.zxing.BarcodeFormat.CODE_128, 800, 300);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}