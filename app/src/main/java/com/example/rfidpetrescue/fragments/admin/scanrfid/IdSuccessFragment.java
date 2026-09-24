package com.example.rfidpetrescue.fragments.admin.scanrfid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetSummaryAdminFragment;

public class IdSuccessFragment extends Fragment {

    private String scannedRfid = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rfid_success_admin, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            scannedRfid = getArguments().getString("SCANNED_RFID", "");
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isAdded() || getActivity() == null) return;

            PetSummaryAdminFragment summaryFragment = new PetSummaryAdminFragment();
            Bundle bundle = new Bundle();
            bundle.putString("SCANNED_RFID", scannedRfid);
            summaryFragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().popBackStack();

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, summaryFragment)
                    .addToBackStack(null)
                    .commit();

        }, 1500);
    }
    @Override
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