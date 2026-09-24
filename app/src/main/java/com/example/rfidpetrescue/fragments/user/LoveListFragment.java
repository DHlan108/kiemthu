package com.example.rfidpetrescue.fragments.user;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.FavoriteAdapter;
import com.example.rfidpetrescue.fragments.user.appointment.PetDetailFragment;
import com.example.rfidpetrescue.models.Pet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LoveListFragment extends Fragment {
    private RecyclerView rvFavoritePets;
    private FavoriteAdapter adapter;
    private List<Pet> petList;
    private DatabaseReference favoriteRef;
    private DatabaseReference petsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lovelist, container, false);

        rvFavoritePets = view.findViewById(R.id.rvFavoritePets);
        rvFavoritePets.setLayoutManager(new LinearLayoutManager(getContext()));

        petList = new ArrayList<>();
        adapter = new FavoriteAdapter(petList, getContext());

        adapter.setOnItemClickListener(pet -> {
            PetDetailFragment petInfoFragment = new PetDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("PET_ID", pet.getId());
            petInfoFragment.setArguments(bundle);

            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, petInfoFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        rvFavoritePets.setAdapter(adapter);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoriteRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("Favorites");
        petsRef = FirebaseDatabase.getInstance().getReference("Pets");

        loadFavoritePets();
        return view;
    }

    private void loadFavoritePets() {
        favoriteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                petList.clear();
                if (!snapshot.exists()) {
                    adapter.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot data : snapshot.getChildren()) {
                    String petId = data.getKey();
                    if (petId != null) {
                        fetchPetDetail(petId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LoveListFragment", "Error favoriteRef: " + error.getMessage());
            }
        });
    }

    private void fetchPetDetail(String petId) {
        petsRef.child(petId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Pet pet = snapshot.getValue(Pet.class);
                if (pet != null) {
                    pet.setId(snapshot.getKey());

                    int index = -1;
                    for (int i = 0; i < petList.size(); i++) {
                        if (petList.get(i).getId().equals(pet.getId())) {
                            index = i;
                            break;
                        }
                    }

                    if (index != -1) {
                        petList.set(index, pet);
                    } else {
                        petList.add(pet);
                    }
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("LoveListFragment", "Error fetching pet: " + error.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // HIỆN LẠI THANH MENU KHI QUAY LẠI LOVE LIST
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
