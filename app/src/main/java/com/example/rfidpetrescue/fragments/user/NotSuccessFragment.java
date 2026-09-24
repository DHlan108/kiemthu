package com.example.rfidpetrescue.fragments.user;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.admin.AdminHomeFragment; // Import file Home của bạn
import com.google.android.material.button.MaterialButton;

public class NotSuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Nạp giao diện từ file XML
        return inflater.inflate(R.layout.fragment_no_success, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ nút bấm
        MaterialButton btnBackToHome = view.findViewById(R.id.btnBackToHome);

        // Xử lý sự kiện khi bấm nút "Trang chủ"
        btnBackToHome.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Cách 1: Dọn dẹp toàn bộ các màn hình xếp chồng (back stack) để quay về màn hình gốc
                getActivity().getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                // Cách 2: (Dùng nếu cách 1 không hoạt động) Chuyển hướng trực tiếp về AdminHomeFragment
                /*
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, new AdminHomeFragment())
                        .commit();
                */
            }
        });
    }
}
