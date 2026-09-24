package com.example.rfidpetrescue.fragments.admin.category.vacci;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.VaccinationAdapter;
import com.example.rfidpetrescue.databinding.FragmentVaccinationRecordAdminBinding;
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
import java.util.List;
import java.util.Locale;

public class AdminVaccinationRecordFragment extends Fragment {
    private TextView tvTotalVaccination, tvRabiesCount, tvDiseaseCount, tvWormCount, tvParasiteCount;
    private FragmentVaccinationRecordAdminBinding binding;
    private DatabaseReference vaccineRef;
    private final List<Vaccination> allRecords = new ArrayList<>();
    private final List<Vaccination> filteredRecords = new ArrayList<>();
    private VaccinationAdapter adapter;

    // --- BIẾN LƯU TRỮ TRẠNG THÁI BỘ LỌC ---
    private String currentFilterDate = "";
    private String currentFilterType = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentVaccinationRecordAdminBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvTotalVaccination = view.findViewById(R.id.tvTotalVaccination);
        tvRabiesCount = view.findViewById(R.id.tvRabiesCount);
        tvDiseaseCount = view.findViewById(R.id.tvDiseaseCount);
        tvWormCount = view.findViewById(R.id.tvWormCount);
        tvParasiteCount = view.findViewById(R.id.tvParasiteCount);
        super.onViewCreated(view, savedInstanceState);

        // Trỏ vào bảng chứa dữ liệu tiêm chủng trên Firebase
        vaccineRef = FirebaseDatabase.getInstance().getReference("Vaccinations");

        setupRecyclerView();
        setupSearch();
        loadVaccinationRecords();

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        // --- GỌI HÀM HIỂN THỊ DIALOG LỌC ---
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // Chuyển sang Tab Theo thú cưng
        binding.tvTabByPet.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminVaccinationByPetFragment())
                    .addToBackStack(null)
                    .commit();
        });

    }

    private void setupRecyclerView() {
        adapter = new VaccinationAdapter(filteredRecords, getContext());

        adapter.setOnItemClickListener(new VaccinationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vaccination vaccination) {
                if (vaccination.pet_id != null && !vaccination.pet_id.isEmpty()) {

                    // --- BỔ SUNG: TRUY VẤN LẤY ẢNH TỪ BẢNG PETS TRƯỚC KHI CHUYỂN TRANG ---
                    DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(vaccination.pet_id);
                    petRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String imageUrl = "";
                            if (snapshot.exists()) {
                                imageUrl = snapshot.child("imageUrl").getValue(String.class);
                            }

                            com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment vaccineFragment =
                                    new com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment();

                            Bundle bundle = new Bundle();
                            bundle.putString("PET_ID", vaccination.pet_id);
                            bundle.putString("PET_IMAGE_URL", imageUrl != null ? imageUrl : ""); // Gói thêm link ảnh vào đây
                            vaccineFragment.setArguments(bundle);

                            if (getActivity() != null) {
                                getActivity().getSupportFragmentManager().beginTransaction()
                                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                        .replace(R.id.main_container, vaccineFragment)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(getContext(), "Lỗi khi tải thông tin thú cưng", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(getContext(), "Không tìm thấy thông tin thú cưng của hồ sơ này", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onEditClick(Vaccination vaccination) {
                // Xử lý nút sửa nếu có
            }

            @Override
            public void onDeleteClick(Vaccination vaccination) {
                // Xử lý nút xóa nếu có
            }
        });

        binding.rvVaccinations.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvVaccinations.setAdapter(adapter);
    }

    private void loadVaccinationRecords() {
        vaccineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allRecords.clear();
                int countTotal = 0, countRabies = 0, countDisease = 0, countWorm = 0, countParasite = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Vaccination record = data.getValue(Vaccination.class);
                        if (record != null) {
                            if (record.id == null || record.id.isEmpty()) {
                                record.id = data.getKey();
                            }
                            allRecords.add(record);
                            countTotal++;
                            if (record.vaccine_type != null) {
                                String type = record.vaccine_type.trim();
                                if (type.equalsIgnoreCase("Ngừa dại")) countRabies++;
                                else if (type.equalsIgnoreCase("Ngừa bệnh")) countDisease++;
                                else if (type.equalsIgnoreCase("Sổ giun")) countWorm++;
                                else if (type.equalsIgnoreCase("Ngoại ký sinh") || type.equalsIgnoreCase("Ngoại kí sinh")) countParasite++;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("AdminVaccine", "Lỗi đọc dữ liệu: " + e.getMessage());
                    }
                }

                if (tvTotalVaccination != null) tvTotalVaccination.setText(String.valueOf(countTotal));
                if (tvRabiesCount != null) tvRabiesCount.setText(String.valueOf(countRabies));
                if (tvDiseaseCount != null) tvDiseaseCount.setText(String.valueOf(countDisease));
                if (tvWormCount != null) tvWormCount.setText(String.valueOf(countWorm));
                if (tvParasiteCount != null) tvParasiteCount.setText(String.valueOf(countParasite));

                // Sắp xếp theo ngày tiêm (vaccine_date) mới nhất lên đầu
                Collections.sort(allRecords, (a, b) -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        String dateStrA = (a.vaccine_date != null && !a.vaccine_date.isEmpty()) ? a.vaccine_date : "01/01/1970";
                        String dateStrB = (b.vaccine_date != null && !b.vaccine_date.isEmpty()) ? b.vaccine_date : "01/01/1970";
                        Date dateA = sdf.parse(dateStrA);
                        Date dateB = sdf.parse(dateStrB);
                        return dateB.compareTo(dateA);
                    } catch (ParseException e) {
                        e.printStackTrace();
                        return 0;
                    }
                });

                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminVaccine", "Database Error: " + error.getMessage());
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

    // --- HÀM LỌC MỚI KẾT HỢP SEARCH VÀ DIALOG ---
    private void applyFilters() {
        String searchText = binding.etSearch.getText() != null ?
                binding.etSearch.getText().toString().toLowerCase().trim() : "";
        filteredRecords.clear();

        for (Vaccination record : allRecords) {
            // 1. Kiểm tra chữ tìm kiếm
            boolean matchesSearch = (record.id != null && record.id.toLowerCase().contains(searchText)) ||
                    (record.medicine_name != null && record.medicine_name.toLowerCase().contains(searchText)) ||
                    (record.vaccine_type != null && record.vaccine_type.toLowerCase().contains(searchText));
            // || (record.pet_id != null && record.pet_id.toLowerCase().contains(searchText));

            // 2. Kiểm tra lọc ngày
            boolean matchesDate = currentFilterDate.isEmpty() ||
                    (record.vaccine_date != null && record.vaccine_date.equals(currentFilterDate));

            // 3. Kiểm tra lọc loại vắc xin
            boolean matchesType = currentFilterType.isEmpty() ||
                    (record.vaccine_type != null && record.vaccine_type.equalsIgnoreCase(currentFilterType));

            // Nếu khớp cả 3 điều kiện mới thêm vào list
            if (matchesSearch && matchesDate && matchesType) {
                filteredRecords.add(record);
            }
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    // --- HÀM HIỂN THỊ DIALOG LỌC ---
    private void showFilterDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        // Tái sử dụng lại layout dialog của bên medical vì giao diện giống hệt nhau
        dialog.setContentView(R.layout.dialog_filter_vaccination);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.TextView tvDateValue = dialog.findViewById(R.id.tvDateValue);
        android.widget.TextView tvTypeValue = dialog.findViewById(R.id.tvTypeValue);
        View btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        View btnSelectType = dialog.findViewById(R.id.btnSelectType);
        com.google.android.material.button.MaterialButton btnSearch = dialog.findViewById(R.id.btnSearch);
        View btnClose = dialog.findViewById(R.id.btnClose);

        // Đổ lại dữ liệu nếu trước đó đang lọc
        if (!currentFilterDate.isEmpty()) {
            tvDateValue.setText(currentFilterDate);
            tvDateValue.setTextColor(android.graphics.Color.BLACK);
        } else {
            tvDateValue.setText("dd/mm/yy");
            tvDateValue.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
        }

        if (!currentFilterType.isEmpty()) {
            tvTypeValue.setText(currentFilterType);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Chọn ngày
        btnSelectDate.setOnClickListener(v -> DatePickerUtils.showForFilter(this, tvDateValue, btnSearch));

        // Chọn Loại Vắc Xin
        btnSelectType.setOnClickListener(v -> {
            android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnSelectType);
            popupMenu.getMenu().add("Tất cả");
            popupMenu.getMenu().add("Ngừa dại");
            popupMenu.getMenu().add("Ngừa bệnh");
            popupMenu.getMenu().add("Sổ giun");
            popupMenu.getMenu().add("Ngoại ký sinh");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals("Tất cả")) {
                    tvTypeValue.setText("");
                } else {
                    tvTypeValue.setText(title);
                }
                btnSearch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1D1A9B")));
                return true;
            });
            popupMenu.show();
        });

        // Xác nhận tìm kiếm
        btnSearch.setOnClickListener(v -> {
            String date = tvDateValue.getText().toString().trim();
            String type = tvTypeValue.getText().toString().trim();

            if (date.equals("dd/mm/yy")) date = "";

            currentFilterDate = date;
            currentFilterType = type;

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}