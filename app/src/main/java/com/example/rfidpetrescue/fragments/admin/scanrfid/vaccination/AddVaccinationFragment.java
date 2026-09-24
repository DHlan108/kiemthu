package com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination;

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
import com.example.rfidpetrescue.models.ScheduleTask;
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
import java.util.Locale;

public class AddVaccinationFragment extends Fragment {

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

    private ImageView        imgPetHeader, ivBarcode;
    private MaterialCardView btnBack, btnSelectVaccineType, btnSelectMedicine, btnSelectDate, btnSelectReVaccineDate;
    private TextView         tvRecordId, tvVaccineType, tvMedicineName, tvVaccineDate, tvReVaccineDate, tvRfid, tvPet;
    private EditText         edtNote;
    private MaterialButton   btnSubmit;

    private String petId;
    private String baseRandomCode;
    private String generatedId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_vaccination, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            String imageUrl = getArguments().getString("PET_IMAGE_URL");
            if (imageUrl != null && !imageUrl.isEmpty() && imgPetHeader != null) {
                Glide.with(this).load(imageUrl).centerCrop().placeholder(R.drawable.ic_dog_manage).into(imgPetHeader);
            }
            if (petId != null) loadPetBasicInfo(petId);
        }

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Vaccinations");
        String pushKey = dbRef.push().getKey();
        baseRandomCode = (pushKey != null) ? pushKey.substring(1, Math.min(pushKey.length(), 8)).toUpperCase() : String.valueOf(System.currentTimeMillis()).substring(6);

        tvVaccineType.setText("Ngừa dại");
        tvMedicineName.setText("Chọn tên thuốc");
        tvVaccineDate.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date()));
        tvReVaccineDate.setText("");

        updateRecordIdDisplay("Ngừa dại");
        setupTextWatchers();
        setupClickListeners();
        checkFormCompletion();
    }

    private void initViews(View view) {
        imgPetHeader = view.findViewById(R.id.imgPetHeader);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        btnBack = view.findViewById(R.id.btnBack);
        tvRecordId = view.findViewById(R.id.tvRecordId);
        tvRfid = view.findViewById(R.id.tvRfid);
        tvPet = view.findViewById(R.id.tvPetDescription);
        btnSelectVaccineType = view.findViewById(R.id.btnSelectVaccineType);
        tvVaccineType = view.findViewById(R.id.tvVaccineType);
        btnSelectMedicine = view.findViewById(R.id.btnSelectMedicine);
        tvMedicineName = view.findViewById(R.id.tvMedicineName);
        btnSelectDate = view.findViewById(R.id.btnSelectDate);
        tvVaccineDate = view.findViewById(R.id.tvVaccineDate);
        btnSelectReVaccineDate = view.findViewById(R.id.btnSelectReVaccineDate);
        tvReVaccineDate = view.findViewById(R.id.tvReVaccineDate);
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
                    if (rfid == null) rfid = snapshot.child("rfid").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    if (tvPet != null) tvPet.setText((name != null && !name.isEmpty()) ? name : "Chưa có tên");
                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) rfid = rfid.replace("tag_", "");
                        Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
                        if (barcode != null && ivBarcode != null) ivBarcode.setImageBitmap(barcode);
                        if (tvRfid != null) tvRfid.setText(rfid);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateRecordIdDisplay(String type) {
        String prefix;
        switch (type) {
            case "Ngừa dại": prefix = "ND"; break;
            case "Ngừa bệnh": prefix = "NB"; break;
            case "Sổ giun": prefix = "SG"; break;
            case "Ngoại kí sinh": prefix = "NK"; break;
            default: prefix = "VC"; break;
        }
        generatedId = prefix + "-" + baseRandomCode;
        if (tvRecordId != null) tvRecordId.setText(generatedId);
    }

    private void setupTextWatchers() {
        if (edtNote == null) return;
        edtNote.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { checkFormCompletion(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkFormCompletion() {
        if (tvVaccineType == null || tvMedicineName == null || tvVaccineDate == null || tvReVaccineDate == null) return;
        String type = tvVaccineType.getText().toString().trim();
        String medicine = tvMedicineName.getText().toString().trim();
        String date = tvVaccineDate.getText().toString().trim();
        String reDate = tvReVaccineDate.getText().toString().trim();
        boolean isReady = !type.isEmpty() && !type.equals("Chọn loại tiêm") && !medicine.isEmpty() && !medicine.equals("Chọn tên thuốc") && !date.isEmpty() && !reDate.isEmpty();
        if (btnSubmit != null) btnSubmit.setEnabled(isReady);
    }

    private void setupClickListeners() {
        if (btnBack != null) btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        if (btnSelectVaccineType != null) btnSelectVaccineType.setOnClickListener(v -> DropdownUtils.showRoundedDropdown(requireContext(), btnSelectVaccineType, VACCINE_TYPES, selectedItem -> {
            tvVaccineType.setText(selectedItem);
            updateRecordIdDisplay(selectedItem);
            tvMedicineName.setText("Chọn tên thuốc");
            checkFormCompletion();
        }));
        if (btnSelectMedicine != null) btnSelectMedicine.setOnClickListener(v -> {
            String currentType = tvVaccineType.getText().toString();
            String[] medicines;
            switch (currentType) {
                case "Ngừa dại": medicines = MEDICINES_NGUA_DAI; break;
                case "Ngừa bệnh": medicines = MEDICINES_NGUA_BENH; break;
                case "Sổ giun": medicines = MEDICINES_SO_GIUN; break;
                case "Ngoại kí sinh": medicines = MEDICINES_NGOAI_KI_SINH; break;
                default: medicines = MEDICINES_NGUA_DAI; break;
            }
            DropdownUtils.showRoundedDropdown(requireContext(), btnSelectMedicine, medicines, selectedItem -> {
                tvMedicineName.setText(selectedItem);
                checkFormCompletion();
            });
        });
        if (btnSelectDate != null) btnSelectDate.setOnClickListener(v -> DatePickerUtils.show(this, tvVaccineDate, null, date -> checkFormCompletion()));
        if (btnSelectReVaccineDate != null) btnSelectReVaccineDate.setOnClickListener(v -> DatePickerUtils.show(this, tvReVaccineDate, 30, date -> checkFormCompletion()));
        if (btnSubmit != null) btnSubmit.setOnClickListener(v -> submitData());
    }

    private void submitData() {
        if (petId == null || generatedId == null) return;
        String vaccineType = tvVaccineType.getText().toString().trim();
        String medicineName = tvMedicineName.getText().toString().trim();
        String vaccineDate = tvVaccineDate.getText().toString().trim();
        String reVaccineDate = tvReVaccineDate.getText().toString().trim();
        String note = (edtNote != null) ? edtNote.getText().toString().trim() : "";

        Vaccination vaccination = new Vaccination(generatedId, petId, vaccineType, medicineName, vaccineDate, reVaccineDate, note, System.currentTimeMillis());
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang lưu...");

        FirebaseDatabase.getInstance().getReference("Vaccinations").child(generatedId).setValue(vaccination).addOnCompleteListener(task -> {
            if (!isAdded()) return;
            if (task.isSuccessful()) {
                createScheduleTask(vaccination);
                Toast.makeText(getContext(), "Thêm tiêm chủng thành công!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Thêm tiêm chủng");
            }
        });
    }

    private void createScheduleTask(Vaccination vaccination) {
        try {
            SimpleDateFormat inputFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            SimpleDateFormat outputFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date dateObj = inputFmt.parse(vaccination.getRevaccine_date());
            if (dateObj == null) return;

            String dateKey = outputFmt.format(dateObj);
            DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference("Admin_Schedules").child(dateKey);

            ScheduleTask scheduleTask = new ScheduleTask();
            scheduleTask.setId(vaccination.getId()); // SỬ DỤNG ID TIÊM CHỦNG LÀM ID LỊCH TRÌNH
            scheduleTask.setType("Tiêm chủng");
            scheduleTask.setTitle("Tái chủng");
            scheduleTask.setDescription("Tái chủng định kỳ: " + vaccination.getMedicine_name());
            scheduleTask.setTime("11:00 AM");
            scheduleTask.setDate(dateKey);
            scheduleTask.setPetName(tvPet.getText().toString());
            scheduleTask.setPetId(petId);
            scheduleTask.setVaccineType(vaccination.getMedicine_name());

            scheduleRef.child(vaccination.getId()).setValue(scheduleTask);
        } catch (Exception e) {
            Log.e("AddVaccination", "Error creating schedule: " + e.getMessage());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }
}