package com.example.rfidpetrescue.fragments.user.donate;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;

public class DonateSuccessFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate_success, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleBottomNavigation(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ nút bấm
        Button btnBackToHome = view.findViewById(R.id.btnBackToHome);

        if (btnBackToHome != null) {
            btnBackToHome.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    mainActivity.openUserHome();
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