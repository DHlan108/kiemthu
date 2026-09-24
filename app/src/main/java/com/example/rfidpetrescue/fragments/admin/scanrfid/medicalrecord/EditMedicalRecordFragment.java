package com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.rfidpetrescue.utils.BarcodeUtils; // Bổ sung tiện ích Mã vạch
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.utils.DropdownUtils; // Bổ sung tiện ích Dropdown bo cong
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

public class EditMedicalRecordFragment extends Fragment {

    private ImageView imgPetHeader, ivBarcode;
    private MaterialCardView btnBack, cardSelectRecordType, cardSelectExamDate;
    private TextView tvRecordId, tvRecordType, tvExamDate, tvRfid, tvPet;
    private EditText edtSymptom, edtClinical, edtDiagnosis, edtNote;
    private MaterialButton btnSaveRecord, btnDeleteRecord;

    private String petId;
    private String recordId;

    // Các biến lưu trữ dữ liệu gốc để đối chiếu xem có sự thay đổi hay không
    private String originalType = "";
    private String originalDate = "";
    private String originalSymptoms = "";
    private String originalClinical = "";
    private String originalDiagnosis = "";
    private String originalNote = "";

    // Biến cờ kiểm tra trạng thái chỉnh sửa
    private boolean hasChanges = false;

    private static final String[] RECORD_TYPES = {"Khám bệnh", "Tiểu phẫu", "Phẫu thuật"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_medical_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            recordId = getArguments().getString("RECORD_ID");
            String imageUrl = getArguments().getString("PET_IMAGE_URL");

            if (imageUrl != null && !imageUrl.isEmpty() && imgPetHeader != null) {
                Glide.with(this).load(imageUrl).centerCrop()
                        .placeholder(R.drawable.ic_dog_manage).into(imgPetHeader);
            }

            // GỌI HIỂN THỊ DỮ LIỆU TỪ BUNDLE NGAY LẬP TỨC
            populateFormFromBundle(getArguments());

            // Gọi Firebase để load mã vạch RFID và thông tin chi tiết bệnh án
            if (petId != null) loadPetBasicInfo(petId);
            if (recordId != null) loadRecordData(recordId);
        }

        setupTextWatchers();
        setupClickListeners();

        // Chạy kiểm tra màu nút lần đầu tiên
        checkForChanges();
    }

    private void initViews(View view) {
        imgPetHeader       = view.findViewById(R.id.imgPetHeader);
        btnBack            = view.findViewById(R.id.btnBack);
        tvRecordId         = view.findViewById(R.id.tvRecordId);
        ivBarcode          = view.findViewById(R.id.ivBarcode);
        tvRfid             = view.findViewById(R.id.tvRfid);
        tvPet              = view.findViewById(R.id.tvPet);

        cardSelectRecordType = view.findViewById(R.id.cardSelectRecordType);
        tvRecordType       = view.findViewById(R.id.tvRecordType);

        cardSelectExamDate = view.findViewById(R.id.cardSelectExamDate);
        tvExamDate         = view.findViewById(R.id.tvExamDate);

        edtSymptom         = view.findViewById(R.id.edtSymptom);
        edtClinical        = view.findViewById(R.id.edtClinical);
        edtDiagnosis       = view.findViewById(R.id.edtDiagnosis);
        edtNote            = view.findViewById(R.id.edtNote);

        btnSaveRecord      = view.findViewById(R.id.btnSaveRecord);
        btnDeleteRecord    = view.findViewById(R.id.btnDeleteRecord);
    }

    private void populateFormFromBundle(Bundle bundle) {
        if (tvRecordId != null && recordId != null) {
            tvRecordId.setText(recordId.substring(0, Math.min(recordId.length(), 12)).toUpperCase());
        }

        originalType = bundle.getString("RECORD_TYPE", "");
        originalDate = bundle.getString("RECORD_DATE", "");
        originalSymptoms = bundle.getString("RECORD_SYMPTOMS", "");
        originalClinical = bundle.getString("RECORD_CLINICAL", "");
        originalDiagnosis = bundle.getString("RECORD_DIAGNOSIS", "");
        originalNote = bundle.getString("RECORD_NOTE", "");

        if (!originalType.isEmpty()) tvRecordType.setText(originalType);
        if (!originalDate.isEmpty()) tvExamDate.setText(originalDate);

        edtSymptom.setText(originalSymptoms);
        edtClinical.setText(originalClinical);
        edtDiagnosis.setText(originalDiagnosis);
        edtNote.setText(originalNote);
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

                    // Tải tên pet
                    String name = snapshot.child("name").getValue(String.class);
                    String petName = (name != null && !name.isEmpty()) ? name : "Chưa có tên";
                    if (tvPet != null) {
                        tvPet.setText(petName);
                    }

                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) {
                            rfid = rfid.replace("tag_", "");
                        }

                        // ĐỒNG BỘ: Sử dụng BarcodeUtils
                        Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
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

    private void loadRecordData(String id) {
        FirebaseDatabase.getInstance().getReference("Medical_Records").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        MedicalRecord r = snapshot.getValue(MedicalRecord.class);
                        if (r != null) {
                            if (r.record_type != null) { tvRecordType.setText(r.record_type); originalType = r.record_type; }
                            if (r.date != null) { tvExamDate.setText(r.date); originalDate = r.date; }
                            if (r.symptoms != null) { edtSymptom.setText(r.symptoms); originalSymptoms = r.symptoms; }
                            if (r.clinical_signs != null) { edtClinical.setText(r.clinical_signs); originalClinical = r.clinical_signs; }
                            if (r.diagnosis != null) { edtDiagnosis.setText(r.diagnosis); originalDiagnosis = r.diagnosis; }
                            if (r.note != null) { edtNote.setText(r.note); originalNote = r.note; }

                            checkForChanges();
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupTextWatchers() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkForChanges();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtSymptom.addTextChangedListener(watcher);
        edtClinical.addTextChangedListener(watcher);
        edtDiagnosis.addTextChangedListener(watcher);
        edtNote.addTextChangedListener(watcher);
    }

    private void checkForChanges() {
        String currentType = tvRecordType.getText().toString().trim();
        String currentDate = tvExamDate.getText().toString().trim();
        String currentSymptoms = edtSymptom.getText().toString().trim();
        String currentClinical = edtClinical.getText().toString().trim();
        String currentDiagnosis = edtDiagnosis.getText().toString().trim();
        String currentNote = edtNote.getText().toString().trim();

        hasChanges = !currentType.equals(originalType) ||
                !currentDate.equals(originalDate) ||
                !currentSymptoms.equals(originalSymptoms) ||
                !currentClinical.equals(originalClinical) ||
                !currentDiagnosis.equals(originalDiagnosis) ||
                !currentNote.equals(originalNote);

        // Giữ nguyên trạng thái để có thể bấm hiện Toast
        if (hasChanges) {
            btnSaveRecord.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B"))); // Xanh đậm
        } else {
            btnSaveRecord.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9"))); // Xám mờ
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        cardSelectRecordType.setOnClickListener(v -> {
            DropdownUtils.showRoundedDropdown(requireContext(), cardSelectRecordType, RECORD_TYPES, selectedItem -> {
                tvRecordType.setText(selectedItem);
                checkForChanges();
            });
        });

        cardSelectExamDate.setOnClickListener(v -> DatePickerUtils.show(this, tvExamDate, null, date -> checkForChanges()));
        tvExamDate.setOnClickListener(v -> DatePickerUtils.show(this, tvExamDate, null, date -> checkForChanges()));
        btnSaveRecord.setOnClickListener(v -> saveChanges());
        btnDeleteRecord.setOnClickListener(v -> confirmDelete());
    }

    private void saveChanges() {
        if (!hasChanges) {
            Toast.makeText(getContext(), "Chưa có gì chỉnh sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        String symptoms = edtSymptom.getText().toString().trim();
        String diagnosis = edtDiagnosis.getText().toString().trim();

        if (symptoms.isEmpty() || diagnosis.isEmpty()) {
            Toast.makeText(getContext(), "Triệu chứng và Chuẩn đoán không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recordId == null) return;

        String type = tvRecordType.getText().toString().trim();
        String date = tvExamDate.getText().toString().trim();
        String clinical = edtClinical.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("record_type", type);
        updates.put("date", date);
        updates.put("symptoms", symptoms);
        updates.put("clinical_signs", clinical);
        updates.put("diagnosis", diagnosis);
        updates.put("note", note);

        btnSaveRecord.setEnabled(false);
        btnSaveRecord.setText("Đang lưu...");

        FirebaseDatabase.getInstance().getReference("Medical_Records").child(recordId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã cập nhật bệnh án!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        btnSaveRecord.setEnabled(true);
                        btnSaveRecord.setText("Lưu chỉnh sửa");
                    }
                });
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
                        // Quay về danh sách
                        requireActivity().getSupportFragmentManager().popBackStack();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Xoá thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity)
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }
}