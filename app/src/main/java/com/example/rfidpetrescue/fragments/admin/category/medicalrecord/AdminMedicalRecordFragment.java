package com.example.rfidpetrescue.fragments.admin.category.medicalrecord;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminMedicalRecordAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminMedicalRecordBinding;
import com.example.rfidpetrescue.fragments.admin.category.medicalrecord.AdminMedicalRecordByPetFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.medicalrecord.MedicalRecordFragment;
import com.example.rfidpetrescue.models.MedicalRecord;
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

public class AdminMedicalRecordFragment extends Fragment {

    private FragmentAdminMedicalRecordBinding binding;
    private DatabaseReference medicalRef;
    private final List<MedicalRecord> allRecords = new ArrayList<>();
    private final List<MedicalRecord> filteredRecords = new ArrayList<>();
    private AdminMedicalRecordAdapter adapter;
    private String currentFilterDate = "";
    private String currentFilterType = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminMedicalRecordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        medicalRef = FirebaseDatabase.getInstance().getReference("Medical_Records");

        setupRecyclerView();
        setupSearch();
        loadMedicalRecords();

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.btnFilter.setOnClickListener(v -> showFilterDialog());

        // Nút chuyển sang Tab "Theo thú cưng"
        binding.tvTabByPet.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminMedicalRecordByPetFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
    private void showFilterDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_filter_medical_record);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        android.widget.TextView tvDateValue = dialog.findViewById(R.id.tvDateValue);
        android.widget.TextView tvTypeValue = dialog.findViewById(R.id.tvTypeValue);
        View btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        View btnSelectType = dialog.findViewById(R.id.btnSelectType);
        com.google.android.material.button.MaterialButton btnSearch = dialog.findViewById(R.id.btnSearch);
        View btnClose = dialog.findViewById(R.id.btnClose);

        // Đổ lại dữ liệu cũ nếu trước đó đang có bộ lọc
        if (!currentFilterDate.isEmpty()) {
            tvDateValue.setText(currentFilterDate);
            tvDateValue.setTextColor(android.graphics.Color.BLACK);
        } else {
            // Trả về giao diện mặc định nếu không có bộ lọc ngày
            tvDateValue.setText("dd/mm/yy");
            tvDateValue.setTextColor(android.graphics.Color.parseColor("#CCCCCC"));
        }

        if (!currentFilterType.isEmpty()) {
            tvTypeValue.setText(currentFilterType);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSelectDate.setOnClickListener(v -> DatePickerUtils.showForFilter(this, tvDateValue, btnSearch));

        btnSelectType.setOnClickListener(v -> {
            android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(requireContext(), btnSelectType);
            popupMenu.getMenu().add("Tất cả");
            popupMenu.getMenu().add("Khám bệnh");
            popupMenu.getMenu().add("Tiểu phẫu");
            popupMenu.getMenu().add("Phẫu thuật");

            popupMenu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if (title.equals("Tất cả")) {
                    tvTypeValue.setText(""); // Xóa nội dung để hiểu là tìm tất cả
                } else {
                    tvTypeValue.setText(title);
                }
                btnSearch.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1D1A9B")));
                return true;
            });
            popupMenu.show();
        });

        btnSearch.setOnClickListener(v -> {
            String date = tvDateValue.getText().toString().trim();
            String type = tvTypeValue.getText().toString().trim();

            // Nếu chữ đang hiển thị là dd/mm/yy thì hệ thống sẽ hiểu là chuỗi rỗng (không lọc ngày)
            if (date.equals("dd/mm/yy")) date = "";

            currentFilterDate = date;
            currentFilterType = type;

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void setupRecyclerView() {
        adapter = new AdminMedicalRecordAdapter(filteredRecords, getContext());

        adapter.setOnItemClickListener(record -> {
            MedicalRecordFragment detailFragment = new MedicalRecordFragment();
            Bundle bundle = new Bundle();
            bundle.putString("RECORD_ID", record.getId());

            String petId = record.getPet_id();
            bundle.putString("PET_ID", petId);

            if (petId != null && !petId.isEmpty()) {
                // Truy vấn nhanh link ảnh của thú cưng từ Firebase trước khi chuyển trang
                DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(petId);
                petRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                            bundle.putString("PET_IMAGE_URL", imageUrl); // Thêm link ảnh vào Bundle
                        }

                        // Sau khi lấy xong ảnh thì thực hiện chuyển trang
                        detailFragment.setArguments(bundle);
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, detailFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        detailFragment.setArguments(bundle);
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, detailFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                });
            }
        });

        binding.rvMedicalRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMedicalRecords.setAdapter(adapter);
    }

    private void loadMedicalRecords() {
        medicalRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allRecords.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        MedicalRecord record = data.getValue(MedicalRecord.class);
                        if (record != null) {
                            if (record.getId() == null || record.getId().isEmpty()) {
                                record.setId(data.getKey());
                            }
                            allRecords.add(record);
                        }
                    } catch (Exception e) {
                        Log.e("AdminMedical", "Lỗi khi đọc dữ liệu bệnh án: " + e.getMessage());
                    }
                }
                Collections.sort(allRecords, (a, b) -> {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    try {
                        String dateStrA = a.getDate() != null ? a.getDate() : "01/01/1970";
                        String dateStrB = b.getDate() != null ? b.getDate() : "01/01/1970";

                        Date dateA = sdf.parse(dateStrA);
                        Date dateB = sdf.parse(dateStrB);

                        // Sắp xếp giảm dần (Mới nhất lên đầu)
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
                Log.e("AdminMedical", "Database Error: " + error.getMessage());
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
        filteredRecords.clear();

        for (MedicalRecord record : allRecords) {
            // Điều kiện 1: Khớp với thanh tìm kiếm (Tìm ID hồ sơ, Mã Pet, hoặc Chẩn đoán)
            boolean matchesSearch = (record.getId() != null && record.getId().toLowerCase().contains(searchText)) ||
                    (record.getDiagnosis() != null && record.getDiagnosis().toLowerCase().contains(searchText)) ||
                    (record.getPet_id() != null && record.getPet_id().toLowerCase().contains(searchText));

            // Điều kiện 2: Khớp ngày (Nếu currentFilterDate rỗng -> bỏ qua lọc ngày)
            boolean matchesDate = currentFilterDate.isEmpty() ||
                    (record.getDate() != null && record.getDate().equals(currentFilterDate));

            // Điều kiện 3: Khớp loại hồ sơ (Nếu currentFilterType rỗng -> bỏ qua lọc loại)
            boolean matchesType = currentFilterType.isEmpty() ||
                    (record.getRecord_type() != null && record.getRecord_type().equalsIgnoreCase(currentFilterType));

            // Nếu thỏa mãn CẢ 3 điều kiện thì mới hiển thị
            if (matchesSearch && matchesDate && matchesType) {
                filteredRecords.add(record);
            }
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}