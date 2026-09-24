package com.example.rfidpetrescue.fragments.user.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.CampaignAdapter;
import com.example.rfidpetrescue.adapters.PetAdapter;
import com.example.rfidpetrescue.fragments.user.appointment.PetDetailFragment;
import com.example.rfidpetrescue.fragments.user.donate.DonateCampaignFragment;
import com.example.rfidpetrescue.fragments.user.donate.DonateFragment;
import com.example.rfidpetrescue.fragments.user.notification.NotificationFragment;
import com.example.rfidpetrescue.models.Campaign;
import com.example.rfidpetrescue.models.Donate;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private TextView tvUserName;
    private ImageView imgAvatar;
    private View btnNotification;
    private View dotUnread;
    private View boxCat, boxDog, layoutSearch;
    private ImageView btnNextAdoption, btnNextRescue;

    // My Pet Card views
    private CardView cardMyPet;
    private ImageView imgMyPet;
    private TextView tvMyPetName, tvMyPetBreed;
    private Button btnViewMyPet;

    private RecyclerView rvPets, rvPetsNearYou;
    private PetAdapter petAdapter;
    private CampaignAdapter urgentAdapter;
    private List<Pet> adoptionList;
    private List<Campaign> urgentList;
    private List<Campaign> rawCampaignList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private DatabaseReference userRef, petsRef, campaignsRef, notifRef, donationsRef;
    private Animation shakeAnim;
    private boolean isNotifScreenOpened = false;
    private Map<String, Long> campaignTotals = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        shakeAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shake);

        setupBox(boxCat, "Mèo", R.drawable.ic_cat);
        setupBox(boxDog, "Chó", R.drawable.ic_dog);

        setupRecyclerViews();

        mAuth = FirebaseAuth.getInstance();
        petsRef = FirebaseDatabase.getInstance().getReference("Pets");
        campaignsRef = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUser.getUid());
            loadUserInfo();
            checkNewNotifications();
            loadMyPet(currentUser.getUid());
        } else {
            tvUserName.setText("Xin chào, Khách");
            if (cardMyPet != null) cardMyPet.setVisibility(View.GONE);
        }

        loadDataForHome();
        setupClickListeners();
    }

    private void initViews(View view) {
        tvUserName = view.findViewById(R.id.tvUserName);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnNotification = view.findViewById(R.id.btnNotification);
        dotUnread = view.findViewById(R.id.dotUnread);
        boxCat = view.findViewById(R.id.boxCat);
        boxDog = view.findViewById(R.id.boxDog);
        layoutSearch = view.findViewById(R.id.layoutSearch);
        btnNextAdoption = view.findViewById(R.id.btnNextAdoption);
        btnNextRescue = view.findViewById(R.id.btnNextRescue);
        rvPets = view.findViewById(R.id.rcvPets);
        rvPetsNearYou = view.findViewById(R.id.rcvPetsNearYou);

        TextView tvRescueTitle = view.findViewById(R.id.tvTitleRescue);
        if (tvRescueTitle != null) {
            tvRescueTitle.setText("Cần giúp đỡ khẩn cấp");
        }
    }

    private void setupRecyclerViews() {
        adoptionList = new ArrayList<>();
        petAdapter = new PetAdapter(adoptionList, getContext());
        rvPets.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPets.setAdapter(petAdapter);

        urgentList = new ArrayList<>();

        urgentAdapter = new CampaignAdapter(getContext(), urgentList, campaign -> {
            if (getParentFragmentManager() != null) {
                DonateCampaignFragment fragment = new DonateCampaignFragment();
                Bundle bundle = new Bundle();
                bundle.putString("campaign_id", campaign.getId());
                fragment.setArguments(bundle);

                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        rvPetsNearYou.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPetsNearYou.setAdapter(urgentAdapter);
    }

    private void setupClickListeners() {
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                isNotifScreenOpened = true;
                btnNotification.clearAnimation();
                if (dotUnread != null) dotUnread.setVisibility(android.view.View.GONE);
                markAllAsRead();

                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.main_container, new NotificationFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        btnNextAdoption.setOnClickListener(v -> openFullPetList("Bé chờ nhận nuôi"));

        btnNextRescue.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new DonateFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        boxCat.setOnClickListener(v -> openPetList("Mèo"));
        boxDog.setOnClickListener(v -> openPetList("Chó"));

        if (layoutSearch != null) {
            layoutSearch.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
                }
                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, new SearchFragment())
                            .addToBackStack("SearchFragment")
                            .commit();
                }
            });
        }
    }

    private void loadDataForHome() {
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                adoptionList.clear();

                List<Pet> tempReadyPets = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Pet pet = data.getValue(Pet.class);
                        if (pet != null) {
                            if ("Sẵn sàng nhận nuôi".equalsIgnoreCase(pet.getStatus())) {
                                pet.setId(data.getKey());
                                tempReadyPets.add(pet);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Lỗi đọc dữ liệu Pet: " + data.getKey(), e);
                    }
                }

                Collections.reverse(tempReadyPets);

                int count = 0;
                for (Pet p : tempReadyPets) {
                    if (count >= 5) break;
                    adoptionList.add(p);
                    count++;
                }

                if (petAdapter != null) petAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Lỗi tải thú cưng", error.toException());
            }
        });

        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (campaignTotals == null) campaignTotals = new HashMap<>();
                campaignTotals.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Donate donate = data.getValue(Donate.class);
                        if (donate != null && "CD".equals(donate.getType()) && donate.getCampaignId() != null) {
                            String cid = donate.getCampaignId();
                            Long current = campaignTotals.get(cid);
                            if (current == null) current = 0L;
                            campaignTotals.put(cid, current + donate.getAmount());
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Lỗi đọc dữ liệu Donate", e);
                    }
                }
                updateCampaignUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        campaignsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (rawCampaignList == null) rawCampaignList = new ArrayList<>();
                rawCampaignList.clear();

                for (DataSnapshot data : snapshot.getChildren()) {
                    try {
                        Campaign campaign = data.getValue(Campaign.class);
                        if (campaign != null) {
                            campaign.setId(data.getKey());
                            rawCampaignList.add(campaign);
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Lỗi cấu trúc Campaign ID: " + data.getKey(), e);
                    }
                }
                updateCampaignUI();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCampaignUI() {
        if (!isAdded()) return;
        try {
            if (urgentList == null) urgentList = new ArrayList<>();
            urgentList.clear();
            List<Campaign> completedCampaigns = new ArrayList<>();
            List<Campaign> activeCampaigns = new ArrayList<>();

            if (rawCampaignList != null) {
                for (Campaign campaign : rawCampaignList) {
                    if (campaignTotals != null) {
                        Long actualAmount = campaignTotals.get(campaign.getId());
                        if (actualAmount == null) actualAmount = 0L;
                        campaign.setCurrent_amount(actualAmount);
                    }

                    boolean isEndedByAmount = false;
                    try {
                        isEndedByAmount = campaign.getCurrent_amount() >= campaign.getTarget_amount();
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Lỗi so sánh tiền", e);
                    }

                    boolean isEndedByStatus = false;
                    if (campaign.getStatus() != null) {
                        isEndedByStatus = "Ended".equalsIgnoreCase(campaign.getStatus());
                    }

                    if (isEndedByStatus || isEndedByAmount) {
                        completedCampaigns.add(campaign);
                    } else {
                        activeCampaigns.add(campaign);
                    }
                }
            }

            urgentList.addAll(activeCampaigns);
            urgentList.addAll(completedCampaigns);

            if (urgentList.size() > 5) {
                urgentList.subList(5, urgentList.size()).clear();
            }

            if (urgentAdapter != null) {
                urgentAdapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e("HomeFragment", "Lỗi update UI", e);
        }
    }

    private void loadMyPet(String userId) {
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                boolean foundPet = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Pet pet = ds.getValue(Pet.class);
                        if (pet != null && userId.equals(pet.getOwner_id())) {
                            pet.setId(ds.getKey());
                            foundPet = true;
                            displayMyPet(pet);
                            break;
                        }
                    } catch (Exception e) {
                        Log.e("HomeFragment", "Lỗi đọc Pet trong loadMyPet với ID: " + ds.getKey(), e);
                    }
                }

                if (cardMyPet != null) {
                    cardMyPet.setVisibility(foundPet ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "loadMyPet:onCancelled", error.toException());
            }
        });
    }

    private void displayMyPet(Pet pet) {
        if (tvMyPetName != null) tvMyPetName.setText(pet.getName());
        if (tvMyPetBreed != null) tvMyPetBreed.setText(pet.getBreed() != null ? pet.getBreed() : pet.getSpecies());

        if (getContext() != null && pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            if (imgMyPet != null) {
                Glide.with(getContext()).load(pet.getImageUrl()).circleCrop().into(imgMyPet);
            }
        } else if (imgMyPet != null) {
            imgMyPet.setImageResource(R.drawable.ic_dog);
        }

        if (btnViewMyPet != null) {
            btnViewMyPet.setOnClickListener(v -> {
                PetDetailFragment fragment = new PetDetailFragment();
                Bundle bundle = new Bundle();
                bundle.putString("pet_id", pet.getId());
                fragment.setArguments(bundle);

                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    private void checkNewNotifications() {
        if (notifRef == null) return;
        notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                boolean hasUnread = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                    if (isRead != null && !isRead) {
                        hasUnread = true;
                        break;
                    }
                }
                if (hasUnread && !isNotifScreenOpened) {
                    if (btnNotification != null) btnNotification.startAnimation(shakeAnim);
                    if (dotUnread != null) dotUnread.setVisibility(android.view.View.VISIBLE);
                } else {
                    if (btnNotification != null) btnNotification.clearAnimation();
                    if (dotUnread != null) dotUnread.setVisibility(android.view.View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void markAllAsRead() {
        if (notifRef == null) return;
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (Boolean.FALSE.equals(ds.child("isRead").getValue(Boolean.class))) {
                        ds.getRef().child("isRead").setValue(true);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupBox(View box, String title, int iconRes) {
        if (box == null) return;
        TextView txt = box.findViewById(R.id.txtTitle);
        ImageView img = box.findViewById(R.id.imgIcon);
        if (txt != null) txt.setText(title);
        if (img != null) img.setImageResource(iconRes);
    }

    private void openFullPetList(String title) {
        PetListFragment fragment = new PetListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("SELECTED_SPECIES", title);
        bundle.putBoolean("SHOW_ALL", true);
        bundle.putString("LIST_TYPE", "Adoption");
        fragment.setArguments(bundle);

        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void openPetList(String species) {
        PetListFragment fragment = new PetListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("SELECTED_SPECIES", species);
        fragment.setArguments(bundle);

        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, fragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadUserInfo() {
        if (userRef == null) return;
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (user.getName() != null) {
                            tvUserName.setText("Xin chào, " + user.getName());
                        }

                        // Lấy avatar thông qua hàm getAvatar()
                        String avatarUrl = user.getAvatar();
                        if (avatarUrl != null && !avatarUrl.isEmpty() && getContext() != null) {
                            Glide.with(getContext())
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_avatar_active)
                                    .error(R.drawable.ic_avatar_unactive)
                                    .into(imgAvatar);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("HomeFragment", "Lỗi tải thông tin user: " + error.getMessage());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        isNotifScreenOpened = false;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}