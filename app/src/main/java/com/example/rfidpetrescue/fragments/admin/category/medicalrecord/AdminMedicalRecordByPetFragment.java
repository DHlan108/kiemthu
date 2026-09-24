package com.example.rfidpetrescue.fragments.admin.category.medicalrecord;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminPetAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminMedicalRecordByPetBinding;
import com.example.rfidpetrescue.fragments.admin.scanrfid.BluetoothRfidFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.MedicalRecordFragment;
import com.example.rfidpetrescue.models.MedicalRecord;
import com.example.rfidpetrescue.models.Pet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminMedicalRecordByPetFragment extends Fragment {

    private FragmentAdminMedicalRecordByPetBinding binding;
    private AdminPetAdapter adapter;
    private final List<Pet> allPetsWithRecords = new ArrayList<>();
    private final List<Pet> filteredPets = new ArrayList<>();
    private final Map<String, Long> petLatestMedicalTime = new HashMap<>();
    private final Map<String, String> petSubtitleMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminMedicalRecordByPetBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupSearch();
        fetchLatestMedicalRecords(); // Tải bệnh án trước, sau đó mới tải thú cưng

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // --- NÚT QUÉT MÃ RFID ĐỂ ĐỊNH DANH BLUETOOTH ---
        if (binding.btnScan != null) {
            binding.btnScan.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new BluetoothRfidFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Nút chuyển lại Tab "Theo hồ sơ"
        binding.tvTabByRecord.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new AdminPetAdapter(filteredPets, getContext(), pet -> {
            MedicalRecordFragment medicalFragment = new MedicalRecordFragment();
            Bundle bundle = new Bundle();
            bundle.putString("PET_ID", pet.getId());
            bundle.putString("PET_IMAGE_URL", pet.getImageUrl());
            medicalFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, medicalFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.rvPets.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvPets.setAdapter(adapter);
    }

    private void fetchLatestMedicalRecords() {
        DatabaseReference medicalRef = FirebaseDatabase.getInstance().getReference("Medical_Records");
        medicalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                petLatestMedicalTime.clear();
                petSubtitleMap.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

                for (DataSnapshot data : snapshot.getChildren()) {
                    MedicalRecord record = data.getValue(MedicalRecord.class);
                    if (record != null && record.getPet_id() != null) {
                        try {
                            String dateStr = record.getDate();
                            if (dateStr != null) {
                                Date recordDate = sdf.parse(dateStr);
                                long recordTimeMillis = recordDate.getTime();
                                long currentLatest = petLatestMedicalTime.getOrDefault(record.getPet_id(), 0L);

                                if (recordTimeMillis > currentLatest) {
                                    petLatestMedicalTime.put(record.getPet_id(), recordTimeMillis);
                                    petSubtitleMap.put(record.getPet_id(), "Khám lần cuối: " + dateStr);
                                }
                            }
                        } catch (ParseException e) { e.printStackTrace(); }
                    }
                }
                loadPetsWithRecords();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPetsWithRecords() {
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets");
        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allPetsWithRecords.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pet pet = data.getValue(Pet.class);
                    if (pet != null) {
                        pet.setId(data.getKey());
                        if (petLatestMedicalTime.containsKey(pet.getId())) {
                            allPetsWithRecords.add(pet);
                        }
                    }
                }
                Collections.sort(allPetsWithRecords, (p1, p2) ->
                        Long.compare(petLatestMedicalTime.getOrDefault(p2.getId(), 0L), petLatestMedicalTime.getOrDefault(p1.getId(), 0L)));

                applyFilters();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilters() {
        String searchText = binding.etSearch.getText().toString().toLowerCase().trim();
        filteredPets.clear();
        for (Pet pet : allPetsWithRecords) {
            if ((pet.getName() != null && pet.getName().toLowerCase().contains(searchText)) ||
                    (pet.getRfid_tag_id() != null && pet.getRfid_tag_id().toLowerCase().contains(searchText))) {
                filteredPets.add(pet);
            }
        }
        if (adapter != null) {
            adapter.setPetSubtitles(petSubtitleMap); // Hiển thị ngày khám gần nhất
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }
}