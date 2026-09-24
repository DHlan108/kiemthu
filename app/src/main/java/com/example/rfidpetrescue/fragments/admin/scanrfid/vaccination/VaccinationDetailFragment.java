package com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination;

import android.graphics.Bitmap;
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
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class VaccinationDetailFragment extends Fragment {

    private ImageView imgPetHeader, ivBarcode;
    private MaterialCardView btnBack;
    private TextView tvRecordId, tvVaccineType, tvMedicineName, tvVaccineDate, tvReVaccineDate, tvNote, tvRfid, tvPet;
    private MaterialButton btnDelete, btnEdit;

    private String petId;
    private String petImageUrl;
    private String vaccinationId;
    private String vaccineType = "";
    private String medicineName = "";
    private String vaccineDate = "";
    private String reVaccineDate = "";
    private String note = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vaccination_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ép ẩn Bottom Bar ngay lập tức để chống chớp giật
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        initViews(view);

        if (getArguments() != null) {
            petId           = getArguments().getString("PET_ID");
            petImageUrl     = getArguments().getString("PET_IMAGE_URL");
            vaccinationId   = getArguments().getString("VACCINATION_ID");

            // Lấy dữ liệu tạm từ Bundle để hiển thị ngay lập tức
            vaccineType  = getArguments().getString("VACCINE_TYPE", "");
            medicineName = getArguments().getString("MEDICINE_NAME", "");
            vaccineDate  = getArguments().getString("VACCINE_DATE", "");
            reVaccineDate = getArguments().getString("REVACCINE_DATE", "");
            note         = getArguments().getString("VACCINE_NOTE", "");

            populateUI();

            // Load ảnh thú cưng
            if (petImageUrl != null && !petImageUrl.isEmpty() && imgPetHeader != null) {
                Glide.with(this)
                        .load(petImageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_dog_manage)
                        .into(imgPetHeader);
            }

            // Gọi Firebase lấy Tên, RFID và tạo Barcode
            if (petId != null) {
                loadPetBasicInfo(petId);
            }

            // Tải lại từ Firebase để lắng nghe thay đổi (tự động update khi từ màn Edit quay về)
            if (vaccinationId != null) {
                listenToRecordChanges(vaccinationId);
            }
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        imgPetHeader    = view.findViewById(R.id.imgPetHeader);
        ivBarcode       = view.findViewById(R.id.ivBarcode);
        btnBack         = view.findViewById(R.id.btnBack);
        tvRecordId      = view.findViewById(R.id.tvRecordId);

        tvRfid          = view.findViewById(R.id.tvRfid);
        tvPet           = view.findViewById(R.id.tvPetDescription);

        tvVaccineType   = view.findViewById(R.id.tvVaccineType);
        tvMedicineName  = view.findViewById(R.id.tvMedicineName);
        tvVaccineDate   = view.findViewById(R.id.tvVaccineDate);
        tvReVaccineDate = view.findViewById(R.id.tvReVaccineDate);
        tvNote          = view.findViewById(R.id.tvNote);

        btnDelete       = view.findViewById(R.id.btnDelete);
        btnEdit         = view.findViewById(R.id.btnEdit);
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
                        if (rfid.startsWith("tag_")) {
                            rfid = rfid.replace("tag_", "");
                        }

                        // Gọi file tiện ích tạo mã vạch
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

    /** Điền dữ liệu lên UI từ các biến đã có */
    private void populateUI() {
        if (tvRecordId != null && vaccinationId != null) {
            String shortId = vaccinationId.substring(0, Math.min(vaccinationId.length(), 13)).toUpperCase();
            tvRecordId.setText(shortId);
        }
        if (tvVaccineType   != null) tvVaccineType.setText(vaccineType.isEmpty()   ? "Chưa xác định" : vaccineType);
        if (tvMedicineName  != null) tvMedicineName.setText(medicineName.isEmpty()  ? "--" : medicineName);
        if (tvVaccineDate   != null) tvVaccineDate.setText(vaccineDate.isEmpty()    ? "--" : vaccineDate);
        if (tvReVaccineDate != null) tvReVaccineDate.setText(reVaccineDate.isEmpty() ? "Chưa đặt" : reVaccineDate);
        if (tvNote          != null) tvNote.setText(note.isEmpty() ? "Không có ghi chú" : note);
    }

    private void listenToRecordChanges(String id) {
        FirebaseDatabase.getInstance().getReference("Vaccinations").child(id)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        if (snapshot.exists()) {
                            com.example.rfidpetrescue.models.Vaccination v = snapshot.getValue(com.example.rfidpetrescue.models.Vaccination.class);
                            if (v != null) {
                                // Cập nhật biến local để dùng khi sang Edit và hiển thị UI
                                vaccineType  = v.vaccine_type != null ? v.vaccine_type : "";
                                medicineName = v.medicine_name != null ? v.medicine_name : "";
                                vaccineDate  = v.vaccine_date != null ? v.vaccine_date : "";
                                reVaccineDate = v.revaccine_date != null ? v.revaccine_date : "";
                                note         = v.note != null ? v.note : "";
                                populateUI();
                            }
                        } else {
                            // Nếu record bị xoá, đóng màn hình này
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> openEditVaccination());
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
    }
    private void openEditVaccination() {
        EditVaccinationFragment fragment = new EditVaccinationFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID",         petId);
        args.putString("PET_IMAGE_URL",  petImageUrl);
        args.putString("VACCINATION_ID", vaccinationId);
        args.putString("VACCINE_TYPE",   vaccineType);
        args.putString("MEDICINE_NAME",  medicineName);
        args.putString("VACCINE_DATE",   vaccineDate);
        args.putString("REVACCINE_DATE", reVaccineDate);
        args.putString("VACCINE_NOTE",   note);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
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
                        Toast.makeText(getContext(), "Đã xoá hồ sơ tiêm chủng", Toast.LENGTH_SHORT).show();
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