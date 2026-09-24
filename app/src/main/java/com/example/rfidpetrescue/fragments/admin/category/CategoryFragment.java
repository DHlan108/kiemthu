package com.example.rfidpetrescue.fragments.admin.category;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.category.adopt.AdminAdoptManagementFragment;
import com.example.rfidpetrescue.fragments.admin.category.medicalrecord.AdminMedicalRecordFragment;
import com.example.rfidpetrescue.fragments.admin.category.vacci.AdminVaccinationRecordFragment;
import com.example.rfidpetrescue.fragments.admin.category.donate.DonateAdminFragment;
import com.example.rfidpetrescue.fragments.admin.category.schedule.AdminScheduleFragment;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class CategoryFragment extends Fragment {

    private View cardCategoryPet, cardCategoryDonate, cardCategoryAdoption;
    private View cardCategoryMedical, cardCategoryVaccine, cardCategoryCalendar;
    private View searchBarCategory;
    private EditText etSearchCategory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ các View
        etSearchCategory = view.findViewById(R.id.etSearchCategory);
        searchBarCategory = view.findViewById(R.id.searchBarCategory);

        cardCategoryPet = view.findViewById(R.id.cardCategoryPet);
        cardCategoryDonate = view.findViewById(R.id.cardCategoryDonate);
        cardCategoryAdoption = view.findViewById(R.id.cardCategoryAdoption);
        cardCategoryMedical = view.findViewById(R.id.cardCategoryMedical);
        cardCategoryVaccine = view.findViewById(R.id.cardCategoryVaccine);
        cardCategoryCalendar = view.findViewById(R.id.cardCategoryCalendar);

        // 1. Xử lý bấm vào thanh trắng để bật bàn phím
        searchBarCategory.setOnClickListener(v -> {
            etSearchCategory.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchCategory, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        // 2. Xử lý sự kiện gõ phím tìm kiếm
        setupSearch();

        // Các sự kiện chuyển trang
        cardCategoryPet.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminPetListFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardCategoryDonate.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new DonateAdminFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardCategoryAdoption.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminAdoptManagementFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardCategoryMedical.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminMedicalRecordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardCategoryVaccine.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminVaccinationRecordFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cardCategoryCalendar.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupSearch() {
        etSearchCategory.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Lấy từ khóa và loại bỏ dấu tiếng Việt để so sánh cho chuẩn
                String query = removeAccents(s.toString().toLowerCase().trim());

                // Kiểm tra từng thẻ xem có chứa từ khóa không (ẩn/hiện tương ứng)
                cardCategoryPet.setVisibility(removeAccents("Thú cưng").contains(query) ? View.VISIBLE : View.GONE);
                cardCategoryDonate.setVisibility(removeAccents("Donate").contains(query) ? View.VISIBLE : View.GONE);
                cardCategoryAdoption.setVisibility(removeAccents("Nhận nuôi").contains(query) ? View.VISIBLE : View.GONE);
                cardCategoryMedical.setVisibility(removeAccents("Bệnh án").contains(query) ? View.VISIBLE : View.GONE);
                cardCategoryVaccine.setVisibility(removeAccents("Tiêm chủng").contains(query) ? View.VISIBLE : View.GONE);
                cardCategoryCalendar.setVisibility(removeAccents("Lịch").contains(query) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    // Hàm hỗ trợ loại bỏ dấu Tiếng Việt (Ví dụ: "thú cưng" -> "thu cung")
    private String removeAccents(String str) {
        try {
            String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
            Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
            return pattern.matcher(temp).replaceAll("").toLowerCase().replaceAll("đ", "d");
        } catch (Exception e) {
            return str.toLowerCase();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}