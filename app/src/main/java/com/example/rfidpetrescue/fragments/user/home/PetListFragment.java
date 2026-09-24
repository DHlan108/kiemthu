package com.example.rfidpetrescue.fragments.user.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
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
import java.util.Collections;
import java.util.List;

public class PetListFragment extends Fragment {

    private RecyclerView rvPetList;
    private PetAdapter adapter;
    private List<Pet> petList;

    private String selectedSpecies = "";
    private String listType = "";
    private boolean showAll = false;

    private TextView tvTitle;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Nhận tín hiệu từ HomeFragment
        if (getArguments() != null) {
            selectedSpecies = getArguments().getString("SELECTED_SPECIES", "");
            listType = getArguments().getString("LIST_TYPE", "");
            showAll = getArguments().getBoolean("SHOW_ALL", false);
        }

        tvTitle = view.findViewById(R.id.tvTitle);
        btnBack = view.findViewById(R.id.btnBack);
        rvPetList = view.findViewById(R.id.rvPetList);

        tvTitle.setText(!selectedSpecies.isEmpty() ? selectedSpecies : "Thú cưng");

        petList = new ArrayList<>();
        adapter = new PetAdapter(petList, getContext());
        rvPetList.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvPetList.setAdapter(adapter);

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Gọi chung 1 hàm tải và lọc dữ liệu
        loadPets();
    }

    private void loadPets() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Pets");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                petList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Pet pet = data.getValue(Pet.class);
                        if (pet != null) {
                            pet.setId(data.getKey());

                            // 1. NẾU BẤM VÀO "BÉ CHỜ NHẬN NUÔI" TỪ HOME
                            if ("Adoption".equals(listType) || "Bé chờ nhận nuôi".equals(selectedSpecies)) {
                                if ("Sẵn sàng nhận nuôi".equalsIgnoreCase(pet.getStatus())) {
                                    petList.add(pet);
                                }
                            }
                            // 2. NẾU BẤM VÀO BOX "CHÓ" HOẶC "MÈO"
                            else if (!showAll && !selectedSpecies.isEmpty()) {
                                // ĐÃ BỔ SUNG ĐIỀU KIỆN: Phải đúng loài VÀ Sẵn sàng nhận nuôi
                                if (selectedSpecies.equalsIgnoreCase(pet.getSpecies()) && "Sẵn sàng nhận nuôi".equalsIgnoreCase(pet.getStatus())) {
                                    petList.add(pet);
                                }
                            }
                            // 3. TRƯỜNG HỢP CÒN LẠI (Hiển thị tất cả)
                            else {
                                petList.add(pet);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Đảo ngược danh sách để các bé mới được thêm vào sẽ hiện lên trên cùng
                Collections.reverse(petList);

                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
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