package com.example.rfidpetrescue.fragments.user.appointment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.google.android.material.button.MaterialButton;

public class SuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_success, container, false);
    }
    @Override
    public void onResume() {
        super.onResume();
        // Gọi ẩn ở onResume để đảm bảo mỗi khi quay lại fragment này, bar sẽ bị ẩn ngay lập tức
        toggleBottomNavigation(false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialButton btnBackToHome = view.findViewById(R.id.btnBackToHome);

        if (btnBackToHome != null) {
            btnBackToHome.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    // 1. Xóa sạch BackStack để không bị dính màn hình cũ khi nhấn nút Back của hệ thống
                    mainActivity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    // 2. Mở lại màn hình Home chính thức
                    mainActivity.openUserHome();
                    // 3. Cập nhật lại thanh điều hướng (nếu cần)
                    mainActivity.updateBottomNavSelection(R.id.btnHome);
                }
            });
        }
    }

    private void toggleBottomNavigation(boolean show) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        toggleBottomNavigation(true);
    }
}