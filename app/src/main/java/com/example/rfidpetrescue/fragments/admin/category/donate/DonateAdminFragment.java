package com.example.rfidpetrescue.fragments.admin.category.donate;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.DonateHistoryAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminDonateBinding;
import com.example.rfidpetrescue.models.Donate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DonateAdminFragment extends Fragment {

    private FragmentAdminDonateBinding binding;
    private DonateHistoryAdapter adapter;
    private List<Donate> donateList = new ArrayList<>();
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDonateBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference("Donations");

        setupRecyclerView();
        loadDonationsFromFirebase();
        setupTabLogicWithoutAnimation();

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void setupTabLogicWithoutAnimation() {
        binding.tvTabChienDich.setOnClickListener(v -> {
            updateTabUI(false);

            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new DonateAdminCampaignFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Sự kiện khi ấn vào tab NHÀ CHUNG
        binding.tvTabNhaChung.setOnClickListener(v -> {
            updateTabUI(true);
            // Vẫn ở Fragment hiện tại nên không cần logic chuyển màn hình
        });
    }

    private void updateTabUI(boolean isNhaChung) {
        if (isNhaChung) {
            binding.tvTabNhaChung.setTextColor(Color.WHITE);
            binding.tvTabNhaChung.setTypeface(null, Typeface.BOLD);
            binding.tvTabChienDich.setTextColor(Color.BLACK);
            binding.tvTabChienDich.setTypeface(null, Typeface.NORMAL);
        } else {
            binding.tvTabChienDich.setTextColor(Color.WHITE);
            binding.tvTabChienDich.setTypeface(null, Typeface.BOLD);
            binding.tvTabNhaChung.setTextColor(Color.BLACK);
            binding.tvTabNhaChung.setTypeface(null, Typeface.NORMAL);
        }
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
        binding = null;
    }

    private void setupRecyclerView() {
        adapter = new DonateHistoryAdapter(donateList);
        binding.rvDonateHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDonateHistory.setAdapter(adapter);
        binding.rvDonateHistory.setNestedScrollingEnabled(false);
    }

    private void loadDonationsFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                donateList.clear();
                long totalCHAmount = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Donate donate = data.getValue(Donate.class);
                    if (donate != null && "CH".equals(donate.getType())) {
                        donateList.add(donate);
                        totalCHAmount += donate.getAmount();
                    }
                }

                Collections.sort(donateList, (d1, d2) -> {
                    String time1 = d1.getTimestamp() != null ? d1.getTimestamp() : "";
                    String time2 = d2.getTimestamp() != null ? d2.getTimestamp() : "";

                    return time2.compareTo(time1);
                });

                if (adapter != null) {
                    adapter.notifyDataSetChanged();

                    if (!donateList.isEmpty() && binding != null) {
                        binding.nestedScrollDonate.scrollTo(0, 0);
                    }
                }
                updateTotalBalance(totalCHAmount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updateTotalBalance(long total) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        String formatted = formatter.format(total).replace(",", ".") + " vnđ";
        if (binding != null) binding.tvBalanceAmount.setText(formatted);
    }
}