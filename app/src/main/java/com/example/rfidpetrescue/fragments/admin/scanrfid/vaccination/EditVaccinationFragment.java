package com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.rfidpetrescue.models.Vaccination;
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
import java.util.HashMap;
import java.util.Locale;

public class EditVaccinationFragment extends Fragment {

    // ─── Dữ liệu loại tiêm & thuốc ───────────────────────────────────────────
    private static final String[] VACCINE_TYPES = {
            "Ngừa dại", "Ngừa bệnh", "Sổ giun", "Ngoại kí sinh"
    };

    private static final String[] MEDICINES_NGUA_DAI = {
            "Rabisin (Pháp)", "Rabiva (Hanvet)", "Rabies (Zoetis)"
    };

    private static final String[] MEDICINES_NGUA_BENH = {
            "7 bệnh Zoetis", "4 bệnh Zoetis", "3 bệnh Nobivac Tricat Trio", "4 bệnh Purevax", "3 bệnh Purevax"
    };

    private static final String[] MEDICINES_SO_GIUN = {
            "Sanpet", "Bio Rantel", "Drontal"
    };

    private static final String[] MEDICINES_NGOAI_KI_SINH = {
            "Ivermectin", "Dectomax", "Bovimec L.A", "Virbamec LA", "Dextovet"
    };

    // ─── Views ────────────────────────────────────────────────────────────────
    private ImageView        imgPetHeader, ivBarcode;
    private MaterialCardView btnBack, cvVaccineSelector, cvMedicineSelector, cvVaccineDate, cvReVaccineDate;
    private TextView         tvRecordIdTitle, tvVaccineType, tvMedicineName, tvVaccineDate, tvReVaccineDate, tvRfid, tvPet;
    private EditText         edtNote;
    private MaterialButton   btnSaveRecord, btnDeleteRecord;

    // ─── State ────────────────────────────────────────────────────────────────
    private String petId;
    private String petImageUrl;
    private String vaccinationId;

    // Lưu trữ dữ liệu gốc để đối chiếu thay đổi
    private String originalType = "";
    private String originalMedicine = "";
    private String originalDate = "";
    private String originalReDate = "";
    private String originalNote = "";
    private boolean hasChanges = false;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_vaccination_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupTextWatchers();

        if (getArguments() != null) {
            petId         = getArguments().getString("PET_ID");
            petImageUrl   = getArguments().getString("PET_IMAGE_URL");
            vaccinationId = getArguments().getString("VACCINATION_ID");

            if (petImageUrl != null && !petImageUrl.isEmpty() && imgPetHeader != null) {
                Glide.with(this).load(petImageUrl).centerCrop()
                        .placeholder(R.drawable.ic_dog_manage).into(imgPetHeader);
            }

            populateFormFromBundle(getArguments());

            if (petId != null) {
                loadPetBasicInfo(petId);
            }
            if (vaccinationId != null) {
                loadVaccinationData(vaccinationId);
            }
        }

        setupClickListeners();
        checkForChanges();
    }

    private void initViews(View view) {
        imgPetHeader       = view.findViewById(R.id.imgPetHeader);
        ivBarcode          = view.findViewById(R.id.ivBarcode); // (Cần ánh xạ nếu XML bạn đã thêm)
        btnBack            = view.findViewById(R.id.btnBack);
        tvRecordIdTitle    = view.findViewById(R.id.tvRecordIdTitle);
        tvRfid             = view.findViewById(R.id.tvRfid);
        tvPet              = view.findViewById(R.id.tvPetDescription); // Ánh xạ theo XML

        cvVaccineSelector  = view.findViewById(R.id.cvVaccineSelector);
        tvVaccineType      = view.findViewById(R.id.tvVaccineType);

        cvMedicineSelector = view.findViewById(R.id.cvMedicineSelector);
        tvMedicineName     = view.findViewById(R.id.tvMedicineName);

        cvVaccineDate      = view.findViewById(R.id.cvVaccineDate);
        tvVaccineDate      = view.findViewById(R.id.tvVaccineDate);
        cvReVaccineDate    = view.findViewById(R.id.cvReVaccineDate);
        tvReVaccineDate    = view.findViewById(R.id.tvReVaccineDate);

        edtNote            = view.findViewById(R.id.edtNote);
        btnSaveRecord      = view.findViewById(R.id.btnSaveRecord);
        btnDeleteRecord    = view.findViewById(R.id.btnDeleteRecord);
    }

    private String[] getMedicinesForType(String vaccineType) {
        if (vaccineType == null) return MEDICINES_NGUA_DAI;
        switch (vaccineType) {
            case "Ngừa dại":      return MEDICINES_NGUA_DAI;
            case "Ngừa bệnh":     return MEDICINES_NGUA_BENH;
            case "Sổ giun":       return MEDICINES_SO_GIUN;
            case "Ngoại kí sinh": return MEDICINES_NGOAI_KI_SINH;
            default:              return MEDICINES_NGUA_DAI;
        }
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
                    if (tvPet != null) tvPet.setText(petName);

                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) rfid = rfid.replace("tag_", "");

                        // Gọi file tiện ích tạo mã vạch
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

    private void populateFormFromBundle(Bundle bundle) {
        if (tvRecordIdTitle != null && vaccinationId != null) {
            tvRecordIdTitle.setText(vaccinationId.substring(0, Math.min(vaccinationId.length(), 13)).toUpperCase());
        }

        originalType = bundle.getString("VACCINE_TYPE", "");
        originalMedicine = bundle.getString("MEDICINE_NAME", "");
        originalDate = bundle.getString("VACCINE_DATE", "");
        originalReDate = bundle.getString("REVACCINE_DATE", "");
        originalNote = bundle.getString("VACCINE_NOTE", "");

        if (!originalType.isEmpty() && tvVaccineType != null) tvVaccineType.setText(originalType);
        if (!originalMedicine.isEmpty() && tvMedicineName != null) tvMedicineName.setText(originalMedicine);
        if (!originalDate.isEmpty() && tvVaccineDate != null) tvVaccineDate.setText(originalDate);
        if (!originalReDate.isEmpty() && tvReVaccineDate != null) tvReVaccineDate.setText(originalReDate);
        if (edtNote != null) edtNote.setText(originalNote);

        checkForChanges();
    }

    private void loadVaccinationData(String id) {
        FirebaseDatabase.getInstance().getReference("Vaccinations").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        Vaccination v = snapshot.getValue(Vaccination.class);
                        if (v == null) return;

                        if (v.vaccine_type != null && tvVaccineType != null) {
                            tvVaccineType.setText(v.vaccine_type);
                            originalType = v.vaccine_type;
                        }
                        if (v.medicine_name != null && tvMedicineName != null) {
                            tvMedicineName.setText(v.medicine_name);
                            originalMedicine = v.medicine_name;
                        }
                        if (v.vaccine_date != null && tvVaccineDate != null) {
                            tvVaccineDate.setText(v.vaccine_date);
                            originalDate = v.vaccine_date;
                        }
                        if (v.revaccine_date != null && tvReVaccineDate != null) {
                            tvReVaccineDate.setText(v.revaccine_date);
                            originalReDate = v.revaccine_date;
                        }
                        if (v.note != null && edtNote != null && edtNote.getText().toString().isEmpty()) {
                            edtNote.setText(v.note);
                            originalNote = v.note;
                        }

                        checkForChanges();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupTextWatchers() {
        if (edtNote == null) return;
        edtNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { checkForChanges(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkForChanges() {
        if (tvVaccineType == null || tvMedicineName == null || tvVaccineDate == null || tvReVaccineDate == null || edtNote == null) return;

        String currentType = tvVaccineType.getText().toString().trim();
        String currentMedicine = tvMedicineName.getText().toString().trim();
        String currentDate = tvVaccineDate.getText().toString().trim();
        String currentReDate = tvReVaccineDate.getText().toString().trim();
        String currentNote = edtNote.getText().toString().trim();

        hasChanges = !currentType.equals(originalType) ||
                !currentMedicine.equals(originalMedicine) ||
                !currentDate.equals(originalDate) ||
                !currentReDate.equals(originalReDate) ||
                !currentNote.equals(originalNote);

        if (btnSaveRecord != null) {
            if (hasChanges) {
                btnSaveRecord.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B"))); // Xanh đậm
            } else {
                btnSaveRecord.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A0A0A0"))); // Xám
            }
        }
    }

    private void setupClickListeners() {
        if (btnBack != null)
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        if (cvVaccineSelector != null)
            cvVaccineSelector.setOnClickListener(v -> {
                DropdownUtils.showRoundedDropdown(requireContext(), cvVaccineSelector, VACCINE_TYPES, selectedItem -> {
                    tvVaccineType.setText(selectedItem);
                    // Đồng bộ lại thuốc: chọn loại tiêm mới thì nhảy thuốc về mục đầu tiên của loại đó
                    String[] meds = getMedicinesForType(selectedItem);
                    if (meds.length > 0) tvMedicineName.setText(meds[0]);
                    checkForChanges();
                });
            });

        if (cvMedicineSelector != null)
            cvMedicineSelector.setOnClickListener(v -> {
                String currentType = tvVaccineType.getText().toString();
                DropdownUtils.showRoundedDropdown(requireContext(), cvMedicineSelector, getMedicinesForType(currentType), selectedItem -> {
                    tvMedicineName.setText(selectedItem);
                    checkForChanges();
                });
            });

        if (cvVaccineDate != null)
            cvVaccineDate.setOnClickListener(v -> DatePickerUtils.show(this, tvVaccineDate, null, date -> checkForChanges()));
        if (cvReVaccineDate != null)
            cvReVaccineDate.setOnClickListener(v -> DatePickerUtils.show(this, tvReVaccineDate, 30, date -> checkForChanges()));

        if (btnSaveRecord != null) btnSaveRecord.setOnClickListener(v -> saveChanges());
        if (btnDeleteRecord != null) btnDeleteRecord.setOnClickListener(v -> confirmDelete());
    }

    private void saveChanges() {
        if (!hasChanges) {
            Toast.makeText(getContext(), "Chưa có gì chỉnh sửa", Toast.LENGTH_SHORT).show();
            return;
        }

        if (vaccinationId == null) return;

        String vaccineType   = tvVaccineType.getText().toString().trim();
        String medicineName  = tvMedicineName.getText().toString().trim();
        String vaccineDate   = tvVaccineDate.getText().toString().trim();
        String reVaccineDate = (tvReVaccineDate != null) ? tvReVaccineDate.getText().toString().trim() : "";
        String note          = (edtNote != null) ? edtNote.getText().toString().trim() : "";

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("vaccine_type",   vaccineType);
        updates.put("medicine_name",  medicineName);
        updates.put("vaccine_date",   vaccineDate);
        updates.put("revaccine_date", reVaccineDate);
        updates.put("note",           note);

        btnSaveRecord.setEnabled(false);
        btnSaveRecord.setText("Đang lưu...");

        FirebaseDatabase.getInstance().getReference("Vaccinations")
                .child(vaccinationId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã cập nhật tiêm chủng!", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        Toast.makeText(getContext(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                        btnSaveRecord.setEnabled(true);
                        btnSaveRecord.setText("Lưu chỉnh sửa");
                    }
                });
    }

    private void confirmDelete() {
        if (vaccinationId == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Xoá hồ sơ")
                .setMessage("Bạn có chắc muốn xoá hồ sơ tiêm chủng này không?")
                .setPositiveButton("Xoá", (dialog, which) -> deleteRecord())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteRecord() {
        FirebaseDatabase.getInstance().getReference("Vaccinations")
                .child(vaccinationId)
                .removeValue()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Đã xoá hồ sơ", Toast.LENGTH_SHORT).show();
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
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}