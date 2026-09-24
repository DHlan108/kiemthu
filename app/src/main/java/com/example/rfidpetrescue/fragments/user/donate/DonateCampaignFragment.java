package com.example.rfidpetrescue.fragments.user.donate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.models.Donate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class DonateCampaignFragment extends Fragment {

    private ImageView imgCampaign;
    private ImageButton btnBack;
    private TextView tvTitle, tvSubTitle, tvPercent, tvProgressAmount, tvTime, tvDescription, tvUsage;
    private ProgressBar progressCampaign;
    private Button btnDonate;

    private String campaignId;
    private DatabaseReference mDatabase, donationsRef;
    private DecimalFormat df = new DecimalFormat("#,###");
    private long actualCurrentAmount = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate_chiendich, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            campaignId = getArguments().getString("campaign_id");
        }

        mDatabase = FirebaseDatabase.getInstance().getReference("Campaigns");
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");

        loadRealtimeDonations();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnDonate.setOnClickListener(v -> {
            DonateAmountFragment donateAmountFragment = new DonateAmountFragment();
            Bundle bundle = new Bundle();
            bundle.putString("donation_type", "CD");
            bundle.putString("campaign_id", campaignId);
            donateAmountFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, donateAmountFragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void initViews(View view) {
        imgCampaign = view.findViewById(R.id.imgCampaignUrgent);
        btnBack = view.findViewById(R.id.btnBack);
        tvTitle = view.findViewById(R.id.tvCampaignTitle);
        tvSubTitle = view.findViewById(R.id.tvSubTitle);
        tvPercent = view.findViewById(R.id.tvCampaignPercent);
        tvProgressAmount = view.findViewById(R.id.tvProgressAmount);
        tvTime = view.findViewById(R.id.tvTime);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvUsage = view.findViewById(R.id.tvUsage);
        progressCampaign = view.findViewById(R.id.pbCampaign);
        btnDonate = view.findViewById(R.id.btnDonate);
    }

    private void loadRealtimeDonations() {
        if (campaignId == null) return;

        donationsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                actualCurrentAmount = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Donate d = ds.getValue(Donate.class);
                    if (d != null && campaignId.equals(d.getCampaignId())) {
                        actualCurrentAmount += d.getAmount();
                    }
                }
                // Sau khi có số tiền thực tế, load thông tin chiến dịch để hiển thị target
                loadCampaignData();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadCampaignData() {
        mDatabase.child(campaignId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String title = snapshot.child("title").getValue(String.class);
                    String subTitle = snapshot.child("sub_title").getValue(String.class);
                    String desc = snapshot.child("description").getValue(String.class);
                    String usage = snapshot.child("usage_purpose").getValue(String.class);
                    String imgUrl = snapshot.child("image_url").getValue(String.class);
                    String status = snapshot.child("status").getValue(String.class);
                    Long targetLong = snapshot.child("target_amount").getValue(Long.class);

                    long target = targetLong != null ? targetLong : 0;
                    long current = actualCurrentAmount;

                    String start = snapshot.child("start_date").getValue(String.class);
                    String end = snapshot.child("end_date").getValue(String.class);

                    if (tvTitle != null) tvTitle.setText(title);
                    if (tvSubTitle != null) tvSubTitle.setText(subTitle);
                    if (tvDescription != null) tvDescription.setText(desc);

                    // ==========================================
                    // ĐÃ SỬA: XỬ LÝ XUỐNG DÒNG CHO tvUsage
                    // ==========================================
                    if (tvUsage != null) {
                        if (usage != null) {
                            // Tách đoạn văn dựa vào chữ " • " (khoảng trắng + chấm tròn + khoảng trắng)
                            // và thay bằng ký tự xuống dòng "\n• "
                            String formattedUsage = usage.replace(" • ", "\n• ").trim();

                            // Đảm bảo chữ đầu tiên nếu là dấu chấm thì không bị xuống dòng dư ở trên cùng
                            if (formattedUsage.startsWith("\n")) {
                                formattedUsage = formattedUsage.substring(1);
                            }

                            tvUsage.setText(formattedUsage);
                        } else {
                            tvUsage.setText("");
                        }
                    }
                    // ==========================================

                    if (target > 0) {
                        int percentage = (int) ((current * 100) / target);
                        // Cấu hình không cho thanh tiến trình vượt quá 100%
                        if (percentage > 100) percentage = 100;

                        if (tvPercent != null) tvPercent.setText(percentage + "%");
                        if (progressCampaign != null) progressCampaign.setProgress(percentage);
                    } else {
                        if (tvPercent != null) tvPercent.setText("0%");
                        if (progressCampaign != null) progressCampaign.setProgress(0);
                    }

                    String progressText = "Tiến độ " + df.format(current).replace(",", ".") + " / " + df.format(target).replace(",", ".");
                    if (tvProgressAmount != null) tvProgressAmount.setText(progressText);

                    String timeText = "Thời gian: " + (start != null ? start : "") + " - " + (end != null ? end : "");
                    if (tvTime != null) tvTime.setText(timeText);

                    if (getContext() != null && imgUrl != null && !imgUrl.isEmpty() && imgCampaign != null) {
                        Glide.with(getContext()).load(imgUrl).into(imgCampaign);
                    }

                    if ("Ended".equals(status) || current >= target) {
                        btnDonate.setEnabled(false);
                        btnDonate.setText("Đã kết thúc");
                        btnDonate.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY));
                    } else {
                        btnDonate.setEnabled(true);
                        btnDonate.setText("Quyên góp ngay");
                        btnDonate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF748D")));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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