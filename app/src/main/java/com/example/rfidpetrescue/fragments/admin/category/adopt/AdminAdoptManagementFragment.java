package com.example.rfidpetrescue.fragments.admin.category.adopt;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminAdoptionAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdoptionManagementBinding;
import com.example.rfidpetrescue.models.AdoptionApplication;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminAdoptManagementFragment extends Fragment {

    private FragmentAdoptionManagementBinding binding;
    private DatabaseReference appointmentsRef;
    private final List<AdoptionApplication> allAppList = new ArrayList<>();
    private final List<AdoptionApplication> filteredAppList = new ArrayList<>();
    private AdminAdoptionAdapter adapter;
    private String currentStatusFilter = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdoptionManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appointmentsRef = FirebaseDatabase.getInstance().getReference("Appointments");

        setupRecyclerView();
        setupSearch();
        setupTabs();
        loadAdoptionData();

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // CHUYỂN SANG MÀN HÌNH LỊCH PHỎNG VẤN
        binding.btnCalendar.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminAdoptInterviewScheduleFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new AdminAdoptionAdapter(filteredAppList, getContext());
        binding.rvAdoptions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvAdoptions.setAdapter(adapter);
    }

    private void loadAdoptionData() {
        appointmentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allAppList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    AdoptionApplication app = data.getValue(AdoptionApplication.class);
                    if (app != null) {
                        app.setId(data.getKey());
                        allAppList.add(app);
                    }
                }
                applyFilters();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminAdoption", "Database Error: " + error.getMessage());
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

    private void setupTabs() {
        binding.tabAll.setOnClickListener(v -> {
            currentStatusFilter = "All";
            updateTabUI(binding.tabAll);
            applyFilters();
        });
        binding.tabPending.setOnClickListener(v -> {
            currentStatusFilter = "Pending";
            updateTabUI(binding.tabPending);
            applyFilters();
        });
        binding.tabInterview.setOnClickListener(v -> {
            currentStatusFilter = "Interview";
            updateTabUI(binding.tabInterview);
            applyFilters();
        });
        binding.tabRejected.setOnClickListener(v -> {
            currentStatusFilter = "Rejected";
            updateTabUI(binding.tabRejected);
            applyFilters();
        });
        binding.tabApproved.setOnClickListener(v -> {
            currentStatusFilter = "Approved";
            updateTabUI(binding.tabApproved);
            applyFilters();
        });
    }

    private void updateTabUI(TextView selectedTab) {
        TextView[] tabs = {binding.tabAll, binding.tabPending, binding.tabInterview, binding.tabRejected, binding.tabApproved};
        for (TextView tab : tabs) {
            tab.setBackgroundResource(R.drawable.bg_pill_white);
            tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }
        selectedTab.setBackgroundResource(R.drawable.bg_pill_blue);
        selectedTab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }

    private void applyFilters() {
        String searchText = binding.etSearch.getText() != null ?
                binding.etSearch.getText().toString().toLowerCase().trim() : "";
        filteredAppList.clear();

        for (AdoptionApplication app : allAppList) {
            boolean matchesStatus = currentStatusFilter.equals("All") ||
                    (app.getStatus() != null && app.getStatus().equalsIgnoreCase(currentStatusFilter));
            boolean matchesSearch = (app.getAdopterName() != null && app.getAdopterName().toLowerCase().contains(searchText)) ||
                    (app.getPetId() != null && app.getPetId().toLowerCase().contains(searchText));

            if (matchesStatus && matchesSearch) {
                filteredAppList.add(app);
            }
        }

        Collections.sort(filteredAppList, (a, b) -> {
            String timeA = a.getCreatedAt() != null ? a.getCreatedAt() : "0";
            String timeB = b.getCreatedAt() != null ? b.getCreatedAt() : "0";
            return timeB.compareTo(timeA);
        });

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        // Khi màn hình chi tiết MỞ LÊN -> Báo MainActivity ẨN Bottom Bar đi
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Khi màn hình chi tiết ĐÓNG LẠI -> Báo MainActivity HIỆN Bottom Bar lại
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}