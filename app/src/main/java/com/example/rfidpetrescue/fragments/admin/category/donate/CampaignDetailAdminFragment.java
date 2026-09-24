package com.example.rfidpetrescue.fragments.admin.category.donate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.databinding.FragmentAdminCampaignDetailBinding;
import com.example.rfidpetrescue.models.Campaign;
import com.example.rfidpetrescue.models.Donate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class CampaignDetailAdminFragment extends Fragment {

    private FragmentAdminCampaignDetailBinding binding;
    private String campaignId;
    private DatabaseReference mDatabase;
    private DatabaseReference donationsRef;
    private DecimalFormat df = new DecimalFormat("#,###");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminCampaignDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            campaignId = getArguments().getString("campaign_id");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        loadCampaignAndActualDonations();

        binding.btnBackDetail.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        binding.btnEndCampaign.setOnClickListener(v -> {
            showEndCampaignDialog();
        });
    }

    private void showEndCampaignDialog() {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Kết thúc chiến dịch")
                .setMessage("Bạn có chắc chắn muốn kết thúc chiến dịch này không? Sau khi kết thúc, người dùng sẽ không thể quyên góp được nữa.")
                .setPositiveButton("Kết thúc", (dialog, which) -> endCampaign())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void endCampaign() {
        if (campaignId == null) return;
        mDatabase.child(campaignId).child("status").setValue("Ended")
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Chiến dịch đã được kết thúc", Toast.LENGTH_SHORT).show();
                        // Chú ý: Không cần update UI ở đây vì ValueEventListener bên dưới sẽ tự động bắt sự kiện data thay đổi
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadCampaignAndActualDonations() {
        if (campaignId == null) return;

        mDatabase.child(campaignId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot campaignSnapshot) {
                if (campaignSnapshot.exists() && isAdded()) {
                    Campaign campaign = campaignSnapshot.getValue(Campaign.class);
                    if (campaign != null) {
                        campaign.setId(campaignSnapshot.getKey());

                        // Cập nhật trạng thái nút kết thúc và màu sắc tag thời gian
                        if ("Ended".equals(campaignSnapshot.child("status").getValue(String.class))) {
                            binding.btnEndCampaign.setVisibility(View.GONE);
                            // Set màu nền thành xám (#D9D9D9)
                            binding.tvTimeRangeDetail.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
                        } else {
                            binding.btnEndCampaign.setVisibility(View.VISIBLE);
                            // Trả về null để khôi phục màu xanh gốc của background xml
                            binding.tvTimeRangeDetail.setBackgroundTintList(null);
                        }

                        // Sau khi có dữ liệu chiến dịch, lấy tổng tiền thật từ Donations
                        donationsRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot donationsSnapshot) {
                                long totalActual = 0;
                                for (DataSnapshot ds : donationsSnapshot.getChildren()) {
                                    Donate donate = ds.getValue(Donate.class);
                                    if (donate != null && "CD".equals(donate.getType())
                                            && campaignId.equals(donate.getCampaignId())) {
                                        totalActual += donate.getAmount();
                                    }
                                }
                                campaign.setCurrent_amount(totalActual);
                                displayData(campaign);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void displayData(Campaign campaign) {
        if (binding == null) return;

        binding.tvCampaignIdTitle.setText(campaign.getTitle());
        binding.tvShortDesc.setText("Mô tả: " + campaign.getSub_title());

        long target = campaign.getTarget_amount();
        long current = campaign.getCurrent_amount();

        if (target > 0) {
            int progress = (int) ((current * 100) / target);
            binding.pbDetail.setProgress(progress);
            binding.tvPercentDetail.setText(progress + "%");
        }

        String progressText = "Tiến độ " + df.format(current).replace(",", ".") + " / " + df.format(target).replace(",", ".");
        binding.tvProgressLabel.setText(progressText);

        String timeText = "Thời gian: " + campaign.getStart_date() + " - " + campaign.getEnd_date();
        binding.tvTimeRangeDetail.setText(timeText);

        // Hiển thị mô tả đầy đủ và mục đích sử dụng từ Firebase
        if (campaign.getDescription() != null) {
            binding.tvFullDescription.setText(campaign.getDescription());
        }
        if (campaign.getUsage_purpose() != null) {
            binding.tvUsagePurpose.setText(campaign.getUsage_purpose());
        }

        if (getContext() != null && campaign.getImage_url() != null && !campaign.getImage_url().isEmpty()) {
            Glide.with(getContext()).load(campaign.getImage_url()).into(binding.ivCampaignDetail);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}