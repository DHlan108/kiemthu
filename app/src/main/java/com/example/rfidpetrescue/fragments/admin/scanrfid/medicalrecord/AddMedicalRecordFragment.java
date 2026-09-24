package com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord;

import android.graphics.Bitmap;
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
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.utils.DropdownUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMedicalRecordFragment extends Fragment {

    private static final String[] RECORD_TYPES = {"Khám bệnh", "Tiểu phẫu", "Phẫu thuật"};

    private ImageView imgPetHeader, ivBarcode;
    private MaterialCardView btnBack, btnSelectRecordType, btnSelectDate;
    private TextView tvRecordId, tvRecordType, tvDate, tvRfid, tvPet;
    private EditText edtSymptoms, edtClinicalSigns, edtDiagnosis, edtNote;
    private MaterialButton btnSubmit;

    private String petId;
    private String baseRandomCode;
    private String generatedRecordId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_medical_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            String imageUrl = getArguments().getString("PET_IMAGE_URL");

            if (imageUrl != null && !imageUrl.isEmpty() && imgPetHeader != null) {
                Glide.with(this).load(imageUrl).centerCrop().into(imgPetHeader);
            }

            if (petId != null) {
                loadPetBasicInfo(petId);
            }
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Medical_Records");
        String pushKey = dbRef.push().getKey();
        if (pushKey != null) {
            baseRandomCode = pushKey.substring(1, 8).toUpperCase();
        } else {
            baseRandomCode = String.valueOf(System.currentTimeMillis()).substring(6);
        }

        // Giá trị mặc định
        tvRecordType.setText("Khám bệnh");
        updateRecordIdDisplay("Khám bệnh");
        tvDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        setupClickListeners();
        setupTextWatchers();
        checkFormCompletion();
    }

    private void initViews(View view) {
        imgPetHeader = view.findViewById(R.id.imgPetHeader);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        btnBack = view.findViewById(R.id.btnBack);

        tvRecordId = view.findViewById(R.id.tvRecordId);
        btnSelectRecordType = view.findViewById(R.id.btnSelectRecordType);
        tvRecordType = view.findViewById(R.id.tvRecordType);
        btnSelectDate = view.findViewById(R.id.btnSelectDate);
        tvDate = view.findViewById(R.id.tvDate);
        tvRfid = view.findViewById(R.id.tvRfid);
        tvPet = view.findViewById(R.id.tvPet);

        edtSymptoms = view.findViewById(R.id.edtSymptoms);
        edtClinicalSigns = view.findViewById(R.id.edtClinicalSigns);
        edtDiagnosis = view.findViewById(R.id.edtDiagnosis);
        edtNote = view.findViewById(R.id.edtNote);
        btnSubmit = view.findViewById(R.id.btnSubmit);
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
                        rfid = snapshot.child("rfid").getValue(String.class);
                    }
                    String name = snapshot.child("name").getValue(String.class);
                    String petName = (name != null && !name.isEmpty()) ? name : "Chưa có tên";
                    if (tvPet != null) {
                        tvPet.setText(petName);
                    }

                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) rfid = rfid.replace("tag_", "");

                        // ĐỒNG BỘ: Sử dụng BarcodeUtils dùng chung
                        Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
                        if (barcode != null && ivBarcode != null) ivBarcode.setImageBitmap(barcode);
                        if (tvRfid != null) tvRfid.setText(rfid);
                    } else {
                        if (tvRfid != null) tvRfid.setText("Chưa có mã");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTextWatchers() {
        TextWatcher formWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkFormCompletion(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtSymptoms.addTextChangedListener(formWatcher);
        edtDiagnosis.addTextChangedListener(formWatcher);
    }

    // ĐỒNG BỘ: Dùng XML Selector để tự đổi màu (chỉ cần enable/disable)
    private void checkFormCompletion() {
        String symptoms = edtSymptoms.getText().toString().trim();
        String diagnosis = edtDiagnosis.getText().toString().trim();

        boolean isReady = !symptoms.isEmpty() && !diagnosis.isEmpty();

        if (btnSubmit != null) {
            btnSubmit.setEnabled(isReady);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // ĐỒNG BỘ: Sử dụng DropdownUtils thả xuống bo cong mượt mà
        btnSelectRecordType.setOnClickListener(v -> {
            DropdownUtils.showRoundedDropdown(requireContext(), btnSelectRecordType, RECORD_TYPES, selectedItem -> {
                tvRecordType.setText(selectedItem);
                updateRecordIdDisplay(selectedItem);
            });
        });

        btnSelectDate.setOnClickListener(v -> DatePickerUtils.show(this, tvDate, null));
        tvDate.setOnClickListener(v -> DatePickerUtils.show(this, tvDate, null));
        btnSubmit.setOnClickListener(v -> submitData());
    }

    private void updateRecordIdDisplay(String type) {
        String prefix;
        switch (type) {
            case "Tiểu phẫu": prefix = "TP"; break;
            case "Phẫu thuật": prefix = "PT"; break;
            case "Khám bệnh":
            default: prefix = "KB"; break;
        }

        generatedRecordId = prefix + "-" + baseRandomCode;
        tvRecordId.setText(generatedRecordId); // Xóa chữ "Mã hồ sơ: " đi vì XML đã có
    }

    private void submitData() {
        String symptoms = edtSymptoms.getText().toString().trim();
        String diagnosis = edtDiagnosis.getText().toString().trim();

        if (symptoms.isEmpty() || diagnosis.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ Triệu chứng và Chuẩn đoán!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (petId == null || generatedRecordId == null) {
            Toast.makeText(getContext(), "Lỗi hệ thống: Không xác định được thú cưng", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = tvRecordType.getText().toString();
        String date = tvDate.getText().toString();
        String clinical = edtClinicalSigns.getText().toString().trim();
        String note = edtNote.getText().toString().trim();

        MedicalRecord record = new MedicalRecord(
                generatedRecordId, petId, type, date, symptoms, clinical, diagnosis, note, System.currentTimeMillis()
        );

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang lưu...");

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Medical_Records");
        dbRef.child(generatedRecordId).setValue(record).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Thêm bệnh án thành công!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Thêm bệnh án");
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}