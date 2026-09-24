package com.example.rfidpetrescue.fragments.user.donate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.CampaignAdapter;
import com.example.rfidpetrescue.fragments.user.notification.NotificationFragment;
import com.example.rfidpetrescue.models.Campaign;
import com.example.rfidpetrescue.models.Donate;
import com.example.rfidpetrescue.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonateFragment extends Fragment {

    private RecyclerView rvUrgentNeed;
    private CampaignAdapter campaignAdapter;
    private List<Campaign> campaignList;
    private DatabaseReference mDatabase;
    private DatabaseReference donationsRef;
    private DatabaseReference notifRef;
    private Button btnDonateHouse;
    private ImageView btnNotification, imgAvatar, btnNext;
    private View dotUnread;
    private TextView tvUserName;
    private Map<String, Long> campaignTotals = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUrgentNeed = view.findViewById(R.id.rvUrgentNeed);
        rvUrgentNeed.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvUserName = view.findViewById(R.id.tvUserName);
        dotUnread = view.findViewById(R.id.dotUnread);
        btnNext = view.findViewById(R.id.btnNext);

        campaignList = new ArrayList<>();
        campaignAdapter = new CampaignAdapter(getContext(), campaignList, false, false, this::goToCampaignDetail);
        rvUrgentNeed.setAdapter(campaignAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUid);
        }

        loadUserData();
        loadDonationsAndCampaigns();
        checkNewNotifications();
        btnDonateHouse = view.findViewById(R.id.btnDonateNow);
        if (btnDonateHouse != null) {
            btnDonateHouse.setOnClickListener(v -> {
                DonateAmountFragment fragment = new DonateAmountFragment();
                Bundle bundle = new Bundle();
                bundle.putString("donation_type", "CH");
                fragment.setArguments(bundle);
                switchFragment(fragment);
            });
        }

        btnNotification = view.findViewById(R.id.btnNotification);
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                if (dotUnread != null) dotUnread.setVisibility(View.GONE);
                markAllAsRead();
                switchFragment(new NotificationFragment());
            });
        }

        // --- CHUYỂN SANG TRANG DANH SÁCH CHIẾN DỊCH TỔNG ---
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> switchFragment(new CampaignListFragment()));
        }
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(uid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists() && isAdded()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    if (user.getName() != null) {
                                        tvUserName.setText("Xin chào, " + user.getName());
                                    }

                                    String avatarUrl = user.getAvatar();
                                    if (avatarUrl != null && !avatarUrl.isEmpty() && getContext() != null) {
                                        Glide.with(getContext())
                                                .load(avatarUrl)
                                                .placeholder(R.mipmap.ic_launcher_round)
                                                .error(R.mipmap.ic_launcher_round)
                                                .circleCrop()
                                                .into(imgAvatar);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
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
                if (hasUnread) {
                    if (dotUnread != null) dotUnread.setVisibility(View.VISIBLE);
                } else {
                    if (dotUnread != null) dotUnread.setVisibility(View.GONE);
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

    private void loadDonationsAndCampaigns() {
        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                campaignTotals.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Donate donate = data.getValue(Donate.class);
                    if (donate != null && "CD".equals(donate.getType()) && donate.getCampaignId() != null) {
                        String cid = donate.getCampaignId();
                        long current = campaignTotals.getOrDefault(cid, 0L);
                        campaignTotals.put(cid, current + donate.getAmount());
                    }
                }
                loadUrgentCampaigns();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUrgentCampaigns() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null || !isAdded()) return;

                List<Campaign> activeCampaigns = new ArrayList<>();
                List<Campaign> completedCampaigns = new ArrayList<>();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Campaign campaign = ds.getValue(Campaign.class);
                        if (campaign != null) {
                            campaign.setId(ds.getKey());

                            long actualAmount = campaignTotals.getOrDefault(campaign.getId(), 0L);
                            campaign.setCurrent_amount(actualAmount);

                            boolean isEndedByAmount = false;
                            if (campaign.getTarget_amount() > 0 && campaign.getCurrent_amount() >= campaign.getTarget_amount()) {
                                isEndedByAmount = true;
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
                    } catch (Exception e) {
                        Log.e("DonateFragment", "Lỗi đọc Campaign", e);
                    }
                }

                campaignList.clear();
                campaignList.addAll(activeCampaigns);
                campaignList.addAll(completedCampaigns);

                // --- ÉP CHỈ HIỂN THỊ 3 CHIẾN DỊCH ---
                if (campaignList.size() > 3) {
                    campaignList.subList(3, campaignList.size()).clear();
                }

                if (campaignAdapter != null) {
                    campaignAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void goToCampaignDetail(Campaign campaign) {
        DonateCampaignFragment detailFragment = new DonateCampaignFragment();
        Bundle bundle = new Bundle();
        bundle.putString("campaign_id", campaign.getId());
        detailFragment.setArguments(bundle);
        switchFragment(detailFragment);
    }

    private void switchFragment(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}