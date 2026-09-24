package com.example.rfidpetrescue.fragments.admin.scanrfid;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.google.android.material.button.MaterialButton;

public class IdNotFoundFragment extends Fragment {

    private MaterialButton btnBack, btnAddId;
    private String scannedRfid = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_id_not_found, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack = view.findViewById(R.id.btnBack);
        btnAddId = view.findViewById(R.id.btnAddId);

        if (getArguments() != null) {
            scannedRfid = getArguments().getString("SCANNED_RFID", "");
        }

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Chuyển sang màn hình form điền thông tin định danh mới
        btnAddId.setOnClickListener(v -> {
            AddIdFragment addIdFragment = new AddIdFragment();
            Bundle bundle = new Bundle();
            bundle.putString("NEW_RFID", scannedRfid); // Truyền mã RFID trắng sang form
            addIdFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, addIdFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}