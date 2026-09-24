package com.example.rfidpetrescue.fragments.user.home;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.PetAdapter;
import com.example.rfidpetrescue.models.Pet;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private EditText edtSearchInput;
    private ImageView btnBack;
    private RecyclerView rvResults;
    private LinearLayout layoutEmpty; // Khai báo layout Empty

    private PetAdapter searchAdapter;
    private List<Pet> allPets;
    private List<Pet> filteredList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadAllPetsFromFirebase();

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        edtSearchInput.requestFocus();
        showKeyboard();

        edtSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initViews(View view) {
        edtSearchInput = view.findViewById(R.id.edtSearchInput);
        btnBack = view.findViewById(R.id.btnBack);
        rvResults = view.findViewById(R.id.rvRecentSearches);
        layoutEmpty = view.findViewById(R.id.layoutEmpty); // Ánh xạ an toàn
    }

    private void setupRecyclerView() {
        allPets = new ArrayList<>();
        filteredList = new ArrayList<>();
        searchAdapter = new PetAdapter(filteredList, getContext());
        rvResults.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResults.setAdapter(searchAdapter);
    }

    private void loadAllPetsFromFirebase() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Pets");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allPets.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pet pet = data.getValue(Pet.class);
                    if (pet != null) {
                        pet.setId(data.getKey());
                        allPets.add(pet);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filter(String text) {
        filteredList.clear();

        // Nếu người dùng xóa trắng ô tìm kiếm
        if (text.isEmpty()) {
            searchAdapter.notifyDataSetChanged();
            if (rvResults != null) rvResults.setVisibility(View.VISIBLE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            return;
        }

        String query = text.toLowerCase().trim();
        for (Pet pet : allPets) {
            String name = pet.getName() != null ? pet.getName().toLowerCase() : "";
            String breed = pet.getBreed() != null ? pet.getBreed().toLowerCase() : "";


            boolean matchName = name.startsWith(query) || name.contains(query);
            boolean matchBreed = breed.startsWith(query) || breed.contains(query);

            if (matchName || matchBreed) {
                filteredList.add(pet);
            }
        }
        searchAdapter.notifyDataSetChanged();

        if (filteredList.isEmpty()) {
            if (rvResults != null) rvResults.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            if (rvResults != null) rvResults.setVisibility(View.VISIBLE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(edtSearchInput, InputMethodManager.SHOW_IMPLICIT);
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
    }
}