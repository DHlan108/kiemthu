package com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord;

import android.graphics.Bitmap;
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
import com.example.rfidpetrescue.adapters.MedicalRecordAdapter;
import com.example.rfidpetrescue.models.MedicalRecord;
import com.example.rfidpetrescue.utils.BarcodeUtils; // Sử dụng tiện ích chung
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicalRecordFragment extends Fragment {

    private ImageButton btnBack;
    private MaterialButton btnAction;
    private TextView tvTabInfo, tvTabVaccination;
    private ImageView imgHeader, ivBarcode;
    private TextView tvPetNameTitle, tvRfid;
    private RecyclerView rvMedicalRecords;

    private String petId;
    private String petImageUrl;
    private MedicalRecordAdapter adapter;
    private final List<MedicalRecord> medicalRecordList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medical_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ép ẩn Bottom Bar ngay lập tức
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        initViews(view);
        setupRecyclerView();

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
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
                loadMedicalRecords(petId);
            }
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnAction = view.findViewById(R.id.btnAction);
        tvTabInfo = view.findViewById(R.id.tvTabInfo);
        tvTabVaccination = view.findViewById(R.id.tvTabVaccination);
        imgHeader = view.findViewById(R.id.imgHeader);
        tvPetNameTitle = view.findViewById(R.id.tvPetNameTitle);
        rvMedicalRecords = view.findViewById(R.id.rvMedicalRecords);

        ivBarcode = view.findViewById(R.id.ivBarcode);
        tvRfid = view.findViewById(R.id.tvRfid);
    }

    private void setupRecyclerView() {
        adapter = new MedicalRecordAdapter(medicalRecordList, requireContext());
        rvMedicalRecords.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMedicalRecords.setAdapter(adapter);

        adapter.setOnItemClickListener(new MedicalRecordAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MedicalRecord record) {
                openDetailMedicalRecord(record);
            }
        });
    }

    private void loadPetBasicInfo(String id) {
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(id);
        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name != null && tvPetNameTitle != null) {
                        tvPetNameTitle.setText(name);
                    }

                    String rfid = snapshot.child("rfid_tag_id").getValue(String.class);
                    if (rfid == null) {
                        rfid = snapshot.child("rfid").getValue(String.class);
                    }

                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) rfid = rfid.replace("tag_", "");

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

    private void loadMedicalRecords(String petId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Medical_Records");
        ref.orderByChild("pet_id").equalTo(petId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        medicalRecordList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            MedicalRecord r = data.getValue(MedicalRecord.class);
                            if (r != null) {
                                r.id = data.getKey();
                                medicalRecordList.add(r);
                            }
                        }
                        Collections.sort(medicalRecordList, new Comparator<MedicalRecord>() {
                            SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                            @Override
                            public int compare(MedicalRecord r1, MedicalRecord r2) {
                                try {
                                    if (r1.date == null || r2.date == null) return 0;
                                    Date date1 = format.parse(r1.date);
                                    Date date2 = format.parse(r2.date);
                                    return date2.compareTo(date1); // Giảm dần
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return 0;
                                }
                            }
                        });

                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        if (btnAction != null) {
            btnAction.setOnClickListener(v -> openAddMedicalRecord());
        }

        // ĐÃ SỬA: Chuyển hướng sang màn hình Thông tin (PetInfoAdminFragment) thay vì bị lùi lại
        if (tvTabInfo != null) {
            tvTabInfo.setOnClickListener(v -> {
                if (petId != null && getActivity() != null) {
                    com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment infoFragment =
                            new com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment();

                    Bundle args = new Bundle();
                    args.putString("PET_ID", petId);
                    if (petImageUrl != null) args.putString("PET_IMAGE_URL", petImageUrl);
                    infoFragment.setArguments(args);

                    // Xoá tab Bệnh án hiện tại khỏi ngăn xếp
                    getActivity().getSupportFragmentManager().popBackStack();

                    // Mở tab Thông tin
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, infoFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (tvTabVaccination != null) {
            tvTabVaccination.setOnClickListener(v -> {
                if (petId != null && getActivity() != null) {
                    com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment vacFragment =
                            new com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment();
                    Bundle args = new Bundle();
                    args.putString("PET_ID", petId);
                    args.putString("PET_IMAGE_URL", petImageUrl);
                    vacFragment.setArguments(args);

                    getActivity().getSupportFragmentManager().popBackStack();
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, vacFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    private void openDetailMedicalRecord(MedicalRecord record) {
        MedicalRecordDetailFragment detailFragment = new MedicalRecordDetailFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID", petId);
        args.putString("PET_IMAGE_URL", petImageUrl);
        args.putString("RECORD_ID", record.id);
        detailFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.main_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void openAddMedicalRecord() {
        if (petId == null) {
            Toast.makeText(getContext(), "Không xác định được thú cưng", Toast.LENGTH_SHORT).show();
            return;
        }
        AddMedicalRecordFragment addFragment = new AddMedicalRecordFragment();
        Bundle args = new Bundle();
        args.putString("PET_ID", petId);
        args.putString("PET_IMAGE_URL", petImageUrl);
        addFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, addFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}