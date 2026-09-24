package com.example.rfidpetrescue.fragments.admin.category;

import android.graphics.Color;
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
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminPetAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminPetListBinding;
import com.example.rfidpetrescue.fragments.admin.scanrfid.RfidTestFragment;
import com.example.rfidpetrescue.models.Pet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdminPetListFragment extends Fragment {

    private FragmentAdminPetListBinding binding;
    private DatabaseReference petsRef;
    private List<Pet> allPetsList = new ArrayList<>();
    private List<Pet> filteredPetsList = new ArrayList<>();
    private AdminPetAdapter adapter;

    // Lọc theo trạng thái thay vì loài vật
    private String currentStatusFilter = "All";
    private static final String TAG = "AdminPetListFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminPetListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        petsRef = FirebaseDatabase.getInstance().getReference("Pets");

        setupRecyclerView();
        setupSearch();
        setupTabs();
        loadPetsData();

        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.btnScan.setOnClickListener(v -> {
            RfidTestFragment scanFragment = new RfidTestFragment();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, scanFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new AdminPetAdapter(filteredPetsList, getContext());
        binding.rvAdminPets.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvAdminPets.setAdapter(adapter);
    }

    private void loadPetsData() {
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                allPetsList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Pet pet = data.getValue(Pet.class);
                        if (pet != null) {
                            pet.setId(data.getKey());
                            allPetsList.add(pet);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing pet: " + e.getMessage());
                    }
                }
                applyFilters();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
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
        binding.tvTabAll.setOnClickListener(v -> {
            currentStatusFilter = "All";
            updateTabUI(binding.tvTabAll);
            applyFilters();
        });
        binding.tvTabTreating.setOnClickListener(v -> {
            currentStatusFilter = "Đang điều trị";
            updateTabUI(binding.tvTabTreating);
            applyFilters();
        });
        binding.tvTabReady.setOnClickListener(v -> {
            currentStatusFilter = "Sẵn sàng nhận nuôi";
            updateTabUI(binding.tvTabReady);
            applyFilters();
        });
        binding.tvTabWaitingInterview.setOnClickListener(v -> {
            currentStatusFilter = "Đang chờ phỏng vấn";
            updateTabUI(binding.tvTabWaitingInterview);
            applyFilters();
        });
        binding.tvTabWaitingAdoption.setOnClickListener(v -> {
            currentStatusFilter = "Đang chờ nhận nuôi";
            updateTabUI(binding.tvTabWaitingAdoption);
            applyFilters();
        });
        binding.tvTabAdopted.setOnClickListener(v -> {
            currentStatusFilter = "Đã nhận nuôi";
            updateTabUI(binding.tvTabAdopted);
            applyFilters();
        });
    }

    private void updateTabUI(TextView selectedTab) {
        TextView[] tabs = {
                binding.tvTabAll,
                binding.tvTabTreating,
                binding.tvTabReady,
                binding.tvTabWaitingInterview,
                binding.tvTabWaitingAdoption,
                binding.tvTabAdopted
        };

        for (TextView tab : tabs) {
            tab.setBackground(null);
            tab.setTextColor(Color.parseColor("#666666"));
            tab.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        selectedTab.setBackgroundResource(R.drawable.bg_pill_blue);
        selectedTab.setTextColor(Color.WHITE);
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void applyFilters() {
        String searchText = binding.etSearch.getText() != null ?
                binding.etSearch.getText().toString().toLowerCase().trim() : "";
        filteredPetsList.clear();

        for (Pet pet : allPetsList) {
            // Lọc theo trạng thái
            boolean matchesStatus = currentStatusFilter.equals("All") ||
                    (pet.getStatus() != null && pet.getStatus().equalsIgnoreCase(currentStatusFilter));

            // Lọc theo tên hoặc giống loài
            boolean matchesSearch = (pet.getName() != null && pet.getName().toLowerCase().contains(searchText)) ||
                    (pet.getBreed() != null && pet.getBreed().toLowerCase().contains(searchText));

            if (matchesStatus && matchesSearch) {
                filteredPetsList.add(pet);
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