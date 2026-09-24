package com.example.rfidpetrescue.fragments.admin.category.donate;

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
import com.example.rfidpetrescue.adapters.CampaignAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminDonateCampaignBinding;
import com.example.rfidpetrescue.models.Campaign;
import com.example.rfidpetrescue.models.Donate;
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

public class DonateAdminCampaignFragment extends Fragment {

    private FragmentAdminDonateCampaignBinding binding;
    private CampaignAdapter adapter;
    private List<Campaign> campaignList = new ArrayList<>();
    private DatabaseReference mDatabase;
    private DatabaseReference donationsRef;
    private Map<String, Long> campaignTotals = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminDonateCampaignBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDatabase = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        setupRecyclerView();
        loadDonationsAndCampaigns();
        setupTabLogicWithoutAnimation();

        // Xử lý nút Back
        binding.btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // TẠO SỰ KIỆN CHUYỂN SANG MÀN HÌNH THÊM CHIẾN DỊCH Ở ĐÂY
        binding.btnAddCampaign.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AddCampaignAdminFragment()) // Chuyển Fragment
                    .addToBackStack(null) // Cho phép bấm nút back trên điện thoại để quay lại
                    .commit();
        });
    }

    private void setupRecyclerView() {
        adapter = new CampaignAdapter(getContext(), campaignList, true, campaign -> {
            CampaignDetailAdminFragment detailFragment = new CampaignDetailAdminFragment();
            Bundle bundle = new Bundle();
            bundle.putString("campaign_id", campaign.getId());
            detailFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        binding.rvCampaigns.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvCampaigns.setAdapter(adapter);
        binding.rvCampaigns.setNestedScrollingEnabled(false);
    }

    private void loadDonationsAndCampaigns() {
        // Lắng nghe donations trước để tính tổng tiền thật
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
                loadCampaignsFromFirebase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCampaignsFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (binding == null) return;
                campaignList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Campaign campaign = data.getValue(Campaign.class);
                    if (campaign != null) {
                        campaign.setId(data.getKey());
                        // Gán số tiền thật đã thu được từ map đã tính ở trên
                        long actualAmount = campaignTotals.getOrDefault(campaign.getId(), 0L);
                        campaign.setCurrent_amount(actualAmount);
                        campaignList.add(campaign);
                    }
                }

                // Sắp xếp: Đẩy "Ended" xuống cuối, các chiến dịch đang chạy thì mới nhất lên đầu
                Collections.sort(campaignList, (c1, c2) -> {
                    boolean isC1Ended = "Ended".equals(c1.getStatus());
                    boolean isC2Ended = "Ended".equals(c2.getStatus());

                    if (isC1Ended && !isC2Ended) return 1;
                    if (!isC1Ended && isC2Ended) return -1;

                    // Nếu cùng trạng thái (cùng kết thúc hoặc cùng đang chạy), sắp xếp mới nhất lên đầu
                    // (Firebase push ID có thể sort theo chuỗi để lấy thời gian tạo)
                    if (c1.getId() != null && c2.getId() != null) {
                        return c2.getId().compareTo(c1.getId());
                    }
                    return 0;
                });

                if (adapter != null) adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupTabLogicWithoutAnimation() {
        binding.tvTabNhaChung.setOnClickListener(v -> {
            if (isAdded()) {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.main_container, new DonateAdminFragment())
                        .commit();
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
        binding = null;
    }
}
