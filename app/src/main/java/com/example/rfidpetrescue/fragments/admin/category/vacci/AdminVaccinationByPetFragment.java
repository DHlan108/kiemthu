package com.example.rfidpetrescue.fragments.admin.category.vacci;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminPetAdapter;
import com.example.rfidpetrescue.databinding.FragmentVaccinationByPetAdminBinding;
import com.example.rfidpetrescue.fragments.admin.scanrfid.BluetoothRfidFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.models.Vaccination;
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

public class AdminVaccinationByPetFragment extends Fragment {

    private FragmentVaccinationByPetAdminBinding binding;
    private AdminPetAdapter adapter;
    private final List<Pet> allPetsWithRecords = new ArrayList<>();
    private final List<Pet> filteredPets = new ArrayList<>();

    // Map lưu trữ: Pet ID -> Thời gian tiêm gần nhất (tính bằng mili-giây)
    private final Map<String, Long> petLatestVaccineTime = new HashMap<>();

    // Các biến cho bảng thống kê
    private TextView tvTotalVaccination, tvRabiesCount, tvDiseaseCount, tvWormCount, tvParasiteCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVaccinationByPetAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các thành phần của bảng thống kê
        tvTotalVaccination = view.findViewById(R.id.tvTotalVaccination);
        tvRabiesCount = view.findViewById(R.id.tvRabiesCount);
        tvDiseaseCount = view.findViewById(R.id.tvDiseaseCount);
        tvWormCount = view.findViewById(R.id.tvWormCount);
        tvParasiteCount = view.findViewById(R.id.tvParasiteCount);

        setupRecyclerView();
        setupSearch();
        setupClickListeners();

        // Tải dữ liệu từ Firebase
        fetchLatestVaccinationRecords();
    }

    private void setupClickListeners() {
        // Nút Back
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // Chuyển lại Tab Theo Hồ Sơ
        binding.tvTabByRecord.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // NÚT QUÉT MÃ RFID ĐỂ ĐỊNH DANH BLUETOOTH
        binding.btnScan.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new BluetoothRfidFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new AdminPetAdapter(filteredPets, getContext(), pet -> {
            VaccinationRecordFragment vaccineFragment = new VaccinationRecordFragment();
            Bundle bundle = new Bundle();
            bundle.putString("PET_ID", pet.getId());
            bundle.putString("PET_IMAGE_URL", pet.getImageUrl());
            vaccineFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, vaccineFragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.rvPets.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvPets.setAdapter(adapter);
    }

    private void fetchLatestVaccinationRecords() {
        DatabaseReference vaccineRef = FirebaseDatabase.getInstance().getReference("Vaccinations");
        vaccineRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                petLatestVaccineTime.clear();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

                // Khởi tạo các biến đếm
                int countTotal = 0;
                int countRabies = 0;
                int countDisease = 0;
                int countDeworm = 0;
                int countParasite = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Vaccination record = data.getValue(Vaccination.class);
                    if (record != null) {

                        // --- 1. THỰC HIỆN ĐẾM SỐ LƯỢNG TIÊM CHỦNG ---
                        countTotal++;
                        if (record.vaccine_type != null) {
                            String type = record.vaccine_type.trim();
                            if (type.equalsIgnoreCase("Ngừa dại")) {
                                countRabies++;
                            } else if (type.equalsIgnoreCase("Ngừa bệnh")) {
                                countDisease++;
                            } else if (type.equalsIgnoreCase("Sổ giun")) {
                                countDeworm++;
                            } else if (type.equalsIgnoreCase("Ngoại ký sinh") || type.equalsIgnoreCase("Ngoại kí sinh")) {
                                countParasite++;
                            }
                        }

                        // --- 2. TÌM THỜI GIAN TIÊM MỚI NHẤT CHO TỪNG THÚ CƯNG ---
                        if (record.pet_id != null) {
                            try {
                                String dateStr = (record.vaccine_date != null && !record.vaccine_date.isEmpty()) ? record.vaccine_date : "01/01/1970";
                                Date recordDate = sdf.parse(dateStr);

                                if (recordDate != null) {
                                    long recordTimeMillis = recordDate.getTime();
                                    long currentLatest = petLatestVaccineTime.getOrDefault(record.pet_id, 0L);

                                    if (recordTimeMillis > currentLatest) {
                                        petLatestVaccineTime.put(record.pet_id, recordTimeMillis);
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e("AdminVaccineByPet", "Lỗi parse ngày tháng: " + e.getMessage());
                            }
                        }
                    }
                }

                // --- 3. ĐẨY DỮ LIỆU ĐÃ ĐẾM LÊN UI (BẢNG THỐNG KÊ) ---
                if (tvTotalVaccination != null) tvTotalVaccination.setText(String.valueOf(countTotal));
                if (tvRabiesCount != null) tvRabiesCount.setText(String.valueOf(countRabies));
                if (tvDiseaseCount != null) tvDiseaseCount.setText(String.valueOf(countDisease));
                if (tvWormCount != null) tvWormCount.setText(String.valueOf(countDeworm));
                if (tvParasiteCount != null) tvParasiteCount.setText(String.valueOf(countParasite));

                // --- 4. TIẾN HÀNH TẢI DANH SÁCH THÚ CƯNG LÊN RECYCLERVIEW ---
                loadPetsWithRecords();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminVaccineByPet", "Lỗi tải Vaccinations: " + error.getMessage());
            }
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
                        // Chỉ hiển thị các bé có lịch sử tiêm chủng
                        if (petLatestVaccineTime.containsKey(pet.getId())) {
                            allPetsWithRecords.add(pet);
                        }
                    }
                }

                // Sắp xếp thú cưng mới tiêm lên trên cùng
                Collections.sort(allPetsWithRecords, (pet1, pet2) -> {
                    long time1 = petLatestVaccineTime.getOrDefault(pet1.getId(), 0L);
                    long time2 = petLatestVaccineTime.getOrDefault(pet2.getId(), 0L);
                    return Long.compare(time2, time1);
                });

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminVaccineByPet", "Lỗi tải Pets: " + error.getMessage());
            }
        });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilters() {
        String searchText = binding.etSearch.getText() != null ?
                binding.etSearch.getText().toString().toLowerCase().trim() : "";
        filteredPets.clear();

        for (Pet pet : allPetsWithRecords) {
            boolean matchesSearch = (pet.getName() != null && pet.getName().toLowerCase().contains(searchText)) ||
                    (pet.getRfid_tag_id() != null && pet.getRfid_tag_id().toLowerCase().contains(searchText));

            if (matchesSearch) {
                filteredPets.add(pet);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}