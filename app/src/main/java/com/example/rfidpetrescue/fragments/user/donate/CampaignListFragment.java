package com.example.rfidpetrescue.fragments.user.donate;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.CampaignAdapter;
import com.example.rfidpetrescue.models.Campaign;
import com.example.rfidpetrescue.models.Donate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampaignListFragment extends Fragment {

    private RecyclerView rvAllCampaigns;
    private CampaignAdapter campaignAdapter;
    private List<Campaign> campaignList;
    private DatabaseReference mDatabase, donationsRef;
    private Map<String, Long> campaignTotals = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_campaign_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        rvAllCampaigns = view.findViewById(R.id.rvAllCampaigns);
        rvAllCampaigns.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        campaignList = new ArrayList<>();
        campaignAdapter = new CampaignAdapter(getContext(), campaignList, false, true, campaign -> {
            DonateCampaignFragment detailFragment = new DonateCampaignFragment();
            Bundle bundle = new Bundle();
            bundle.putString("campaign_id", campaign.getId());
            detailFragment.setArguments(bundle);
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        rvAllCampaigns.setAdapter(campaignAdapter);

        mDatabase = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        loadDonationsAndCampaigns();
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
                loadAllCampaigns();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadAllCampaigns() {
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

                            boolean isEndedByAmount = (campaign.getTarget_amount() > 0 && campaign.getCurrent_amount() >= campaign.getTarget_amount());
                            boolean isEndedByStatus = "Ended".equalsIgnoreCase(campaign.getStatus());

                            if (isEndedByStatus || isEndedByAmount) {
                                completedCampaigns.add(campaign);
                            } else {
                                activeCampaigns.add(campaign);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("CampaignList", "Lỗi đọc Campaign", e);
                    }
                }

                campaignList.clear();
                campaignList.addAll(activeCampaigns);
                campaignList.addAll(completedCampaigns);

                // Ở màn hình này thì hiển thị TẤT CẢ, không dùng subList giới hạn số lượng nữa

                if (campaignAdapter != null) {
                    campaignAdapter.notifyDataSetChanged();
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
}