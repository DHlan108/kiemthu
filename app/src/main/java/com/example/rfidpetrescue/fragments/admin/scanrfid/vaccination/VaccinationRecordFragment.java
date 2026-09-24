package com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.VaccinationAdapter;
import com.example.rfidpetrescue.models.Vaccination;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class VaccinationRecordFragment extends Fragment {

    private ImageButton btnBack;
    private ImageButton btnEdit;
    private ImageView   imgHeader, ivBarcode;
    private TextView    tvPetNameTitle, tvRfid;
    private MaterialButton btnAddVaccination;
    private RecyclerView rvVaccinations;

    // Tab views
    private TextView tvTabInfo, tvTabMedical, tvTabVaccination;

    private String petId;
    private String petImageUrl;
    private VaccinationAdapter adapter;
    private final List<Vaccination> vaccinationList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_vaccination_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ép ẩn Bottom Bar ngay lập tức để chống chớp giật
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        initViews(view);
        setupRecyclerView();

        if (getArguments() != null) {
            petId       = getArguments().getString("PET_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");

            // Tải ảnh ngay lập tức để không bị trễ
            if (petImageUrl != null && !petImageUrl.isEmpty() && imgHeader != null) {
                Glide.with(this)
                        .load(petImageUrl)
                        .placeholder(R.drawable.ic_dog_manage)
                        .centerCrop()
                        .into(imgHeader);
            }
            if (petId != null) {
                loadPetBasicInfo(petId);
                loadVaccinationRecords(petId);
            }
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        btnBack           = view.findViewById(R.id.btnBack);
        btnEdit           = view.findViewById(R.id.btnEdit);
        imgHeader         = view.findViewById(R.id.imgHeader);
        ivBarcode         = view.findViewById(R.id.ivBarcode);
        tvRfid            = view.findViewById(R.id.tvRfid);
        tvPetNameTitle    = view.findViewById(R.id.tvPetNameTitle);
        btnAddVaccination = view.findViewById(R.id.btnAction);
        rvVaccinations    = view.findViewById(R.id.rvVaccinations);

        tvTabInfo         = view.findViewById(R.id.tvTabInfo);
        tvTabMedical      = view.findViewById(R.id.tvTabMedical);
        tvTabVaccination  = view.findViewById(R.id.tvTabVaccination);
    }

    private void setupRecyclerView() {
        adapter = new VaccinationAdapter(vaccinationList, requireContext());
        rvVaccinations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVaccinations.setAdapter(adapter);

        adapter.setOnItemClickListener(new VaccinationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vaccination vaccination) {
                openVaccinationDetail(vaccination);
            }

            @Override
            public void onEditClick(Vaccination vaccination) {
                openVaccinationDetail(vaccination);
            }

            @Override
            public void onDeleteClick(Vaccination vaccination) {
                deleteVaccination(vaccination);
            }
        });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().popBackStack());
        }

        // Chuyển hướng sang màn hình Thông tin (PetInfoAdminFragment)
        if (tvTabInfo != null) {
            tvTabInfo.setOnClickListener(v -> {
                if (petId != null) {
                    // Gọi đúng Fragment của tab Thông tin
                    com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment infoFragment =
                            new com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment();

                    Bundle args = new Bundle();
                    args.putString("PET_ID", petId);
                    if (petImageUrl != null) args.putString("PET_IMAGE_URL", petImageUrl);
                    infoFragment.setArguments(args);

                    // Xoá tab Vaccination hiện tại khỏi ngăn xếp để tránh lỗi back vòng tròn
                    requireActivity().getSupportFragmentManager().popBackStack();

                    // Mở tab Thông tin
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, infoFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (tvTabMedical != null) {
            tvTabMedical.setOnClickListener(v -> {
                if (petId != null) {
                    com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.MedicalRecordFragment medFragment =
                            new com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.MedicalRecordFragment();
                    Bundle args = new Bundle();
                    args.putString("PET_ID", petId);
                    if (petImageUrl != null) args.putString("PET_IMAGE_URL", petImageUrl);
                    medFragment.setArguments(args);

                    requireActivity().getSupportFragmentManager().popBackStack();
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, medFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (btnAddVaccination != null) {
            btnAddVaccination.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFF1D1A9B));
            btnAddVaccination.setOnClickListener(v -> openAddVaccination());
        }
    }

    private void openVaccinationDetail(Vaccination vaccination) {
        VaccinationDetailFragment fragment = new VaccinationDetailFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID",         petId);
        args.putString("PET_IMAGE_URL",  petImageUrl);
        args.putString("VACCINATION_ID", vaccination.id);
        args.putString("VACCINE_TYPE",   vaccination.vaccine_type);
        args.putString("MEDICINE_NAME",  vaccination.medicine_name);
        args.putString("VACCINE_DATE",   vaccination.vaccine_date);
        args.putString("REVACCINE_DATE", vaccination.revaccine_date);
        args.putString("VACCINE_NOTE",   vaccination.note);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void openAddVaccination() {
        AddVaccinationFragment fragment = new AddVaccinationFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID", petId);
        if (petImageUrl != null) args.putString("PET_IMAGE_URL", petImageUrl);
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    // --- LOGIC LẤY RFID VÀ TẠO MÃ VẠCH ---
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
                    if (tvPetNameTitle != null) {
                        tvPetNameTitle.setText(petName);
                    }

                    // Tạo và hiển thị Barcode
                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) {
                            rfid = rfid.replace("tag_", "");
                        }

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

    private void loadVaccinationRecords(String petId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Vaccinations");

        ref.orderByChild("pet_id").equalTo(petId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        vaccinationList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Vaccination v = data.getValue(Vaccination.class);
                            if (v != null) {
                                v.id = data.getKey();
                                vaccinationList.add(v);
                            }
                        }

                        java.util.Collections.sort(vaccinationList, new java.util.Comparator<Vaccination>() {
                            // Format ngày phải khớp với định dạng "dd/MM/yyyy" mà bạn đang lưu
                            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());

                            @Override
                            public int compare(Vaccination v1, Vaccination v2) {
                                try {
                                    if (v1.vaccine_date == null || v2.vaccine_date == null) return 0;
                                    java.util.Date date1 = format.parse(v1.vaccine_date);
                                    java.util.Date date2 = format.parse(v2.vaccine_date);

                                    return date2.compareTo(date1);
                                } catch (java.text.ParseException e) {
                                    e.printStackTrace();
                                    return 0; // Giữ nguyên nếu lỗi định dạng ngày
                                }
                            }
                        });
                        // ---------------------------------------------------

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void deleteVaccination(Vaccination vaccination) {
        if (vaccination.id == null) return;
        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Xoá hồ sơ")
                .setMessage("Bạn có chắc muốn xoá hồ sơ tiêm chủng này không?")
                .setPositiveButton("Xoá", (dialog, which) ->
                        FirebaseDatabase.getInstance()
                                .getReference("Vaccinations")
                                .child(vaccination.id)
                                .removeValue()
                                .addOnCompleteListener(task -> {
                                    if (!isAdded()) return;
                                    Toast.makeText(getContext(),
                                            task.isSuccessful() ? "Đã xoá hồ sơ tiêm chủng" : "Xoá thất bại",
                                            Toast.LENGTH_SHORT).show();
                                }))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}