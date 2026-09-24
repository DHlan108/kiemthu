package com.example.rfidpetrescue.fragments.user.donate;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.user.appointment.SuccessFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DonateAmountFragment extends Fragment {

    private EditText edtAmount;
    private TextView tv50k, tv100k, tv200k, tvQrStatus, tvTimer;
    private Button btnConfirm;
    private ImageView imgQrCode;
    private ImageButton btnBack;

    private final Handler qrHandler = new Handler();
    private Runnable qrRunnable;
    private long timeLeftInMillis;

    private boolean isQrGenerated = false;
    private String currentUserName = "Guest";
    private String donationType = "CH";
    private String campaignId = null;
    private String campaignTitle = "";
    private final DecimalFormat df = new DecimalFormat("#,###");
    private String currentAmountString = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_donate_amount, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            donationType = getArguments().getString("donation_type", "CH");
            campaignId = getArguments().getString("campaign_id");
        }

        edtAmount = view.findViewById(R.id.edtAmount);
        tv50k = view.findViewById(R.id.tv50k);
        tv100k = view.findViewById(R.id.tv100k);
        tv200k = view.findViewById(R.id.tv200k);
        tvQrStatus = view.findViewById(R.id.tvQrStatus);
        tvTimer = view.findViewById(R.id.tvTimer);
        btnConfirm = view.findViewById(R.id.btnConfirm);
        imgQrCode = view.findViewById(R.id.imgQrCode);
        btnBack = view.findViewById(R.id.btnBack);

        // Khởi tạo giá trị trống và cho phép nhập
        edtAmount.setText("");
        edtAmount.setHint("Nhập số tiền");
        edtAmount.setFocusableInTouchMode(true);
        edtAmount.requestFocus();


        edtAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(currentAmountString)) {
                    edtAmount.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");
                    if (cleanString.length() > 0) {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = df.format(parsed).replace(",", ".");
                        currentAmountString = formatted;
                        edtAmount.setText(formatted);
                        edtAmount.setSelection(formatted.length());
                    } else {
                        currentAmountString = "";
                        edtAmount.setText("");
                    }

                    if (isQrGenerated) {
                        isQrGenerated = false;
                        btnConfirm.setText("Xác nhận");
                        imgQrCode.setAlpha(0.2f);
                    }

                    edtAmount.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadCurrentUserName();
        if (campaignId != null) {
            loadCampaignTitle();
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        if (tv50k != null) tv50k.setOnClickListener(v -> {
            edtAmount.setText("50000");
        });
        if (tv100k != null) tv100k.setOnClickListener(v -> {
            edtAmount.setText("100000");
        });
        if (tv200k != null) tv200k.setOnClickListener(v -> {
            edtAmount.setText("200000");
        });

        btnConfirm.setOnClickListener(v -> {
            String amountStr = edtAmount.getText().toString().trim();
            amountStr = amountStr.replaceAll("[^\\d]", "");

            if (amountStr.isEmpty() || amountStr.equals("0")) {
                Toast.makeText(getContext(), "Vui lòng nhập số tiền hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isQrGenerated) {
                generateVietQR(amountStr);
                isQrGenerated = true;
                btnConfirm.setText("Đã chuyển khoản");
            } else {
                saveDonationToFirebase(amountStr);
            }
        });

        imgQrCode.setOnClickListener(v -> {
            if (tvQrStatus != null && tvQrStatus.getVisibility() == View.VISIBLE) {
                String amountStr = edtAmount.getText().toString().trim().replaceAll("[^\\d]", "");
                generateVietQR(amountStr);
            }
        });
    }

    private void loadCampaignTitle() {
        FirebaseDatabase.getInstance().getReference("Campaigns").child(campaignId).child("title")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            campaignTitle = snapshot.getValue(String.class);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void saveDonationToFirebase(String amount) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            navigateToSuccess();
            return;
        }

        DatabaseReference donationRef = FirebaseDatabase.getInstance().getReference("Donations");
        String donationId = donationRef.push().getKey();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String currentTime = sdf.format(new Date());

        String safeName = removeAccent(currentUserName).replace(" ", "_").toUpperCase();
        
        String content;
        if (campaignId != null) {
            String shortId = campaignId.length() > 5 ? campaignId.substring(campaignId.length() - 5) : campaignId;
            content = "CD_" + shortId + "_" + safeName;
        } else {
            content = "DONATE_" + donationType + "_" + safeName;
        }

        Map<String, Object> donationData = new HashMap<>();
        donationData.put("user_id", uid);
        donationData.put("user_name", currentUserName);
        donationData.put("amount", Long.parseLong(amount));
        donationData.put("type", donationType);
        if (campaignId != null) {
            donationData.put("campaign_id", campaignId);
            if (!campaignTitle.isEmpty()) {
                donationData.put("campaign_title", campaignTitle);
            }
        }
        donationData.put("content", content);
        donationData.put("timestamp", currentTime);
        donationData.put("status", "Submitted");

        if (donationId != null) {
            btnConfirm.setEnabled(false);
            donationRef.child(donationId).setValue(donationData)
                    .addOnSuccessListener(aVoid -> {
                        sendDonateNotification(uid, amount);
                        sendAdminNotification(amount, content);
                        if ("CD".equals(donationType) && campaignId != null) {
                            updateCampaignCurrentAmount(campaignId, Long.parseLong(amount));
                        } else {
                            navigateToSuccess();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        navigateToSuccess();
                    });
        }
    }

    private void sendDonateNotification(String uid, String amount) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);
        String notifId = notifRef.push().getKey();

        String target = "nhà chung";
        if (campaignId != null && !campaignTitle.isEmpty()) {
            target = "chiến dịch \"" + campaignTitle + "\"";
        }

        String formattedAmount = df.format(Double.parseDouble(amount)).replace(",", ".") + " VND";
        String message = "Cảm ơn " + currentUserName + " đã ủng hộ cho " + target + " số tiền " + formattedAmount + ". Sự đóng góp của bạn rất có ý nghĩa!";

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", notifId);
        notifData.put("title", "Cảm ơn bạn đã đóng góp!");
        notifData.put("message", message);
        notifData.put("timestamp", System.currentTimeMillis());
        notifData.put("isRead", false);
        notifData.put("type", "donate");

        if (notifId != null) {
            notifRef.child(notifId).setValue(notifData);
        }
    }

    private void sendAdminNotification(String amount, String content) {
        DatabaseReference adminNotifRef = FirebaseDatabase.getInstance().getReference("Admin_Notifications");
        String notifId = adminNotifRef.push().getKey();

        String formattedAmount = df.format(Double.parseDouble(amount)).replace(",", ".") + " VND";
        String message = "Biến động: + " + formattedAmount + "\nNội dung: " + content;

        Map<String, Object> notifData = new HashMap<>();
        notifData.put("id", notifId);
        notifData.put("title", "Có khoản quyên góp mới!");
        notifData.put("message", message);
        notifData.put("timestamp", System.currentTimeMillis());
        notifData.put("isRead", false);
        notifData.put("type", "donate");

        if (notifId != null) {
            adminNotifRef.child(notifId).setValue(notifData);
        }
    }

    private void updateCampaignCurrentAmount(String cId, long donatedAmount) {
        DatabaseReference campaignRef = FirebaseDatabase.getInstance().getReference("Campaigns").child(cId);
        campaignRef.child("current_amount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long current = 0;
                if (snapshot.exists() && snapshot.getValue() != null) {
                    current = snapshot.getValue(Long.class);
                }
                campaignRef.child("current_amount").setValue(current + donatedAmount)
                        .addOnCompleteListener(task -> navigateToSuccess());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                navigateToSuccess();
            }
        });
    }

    private void generateVietQR(String amount) {
        String bankId = "VCB";
        String accountNo = "106040267́8";

        String safeName = removeAccent(currentUserName).replace(" ", "_").toUpperCase();
        
        // Cập nhật nội dung QR để Admin dễ track
        String content;
        if (campaignId != null) {
            String shortId = campaignId.length() > 5 ? campaignId.substring(campaignId.length() - 5) : campaignId;
            content = "CD_" + shortId + "_" + safeName;
        } else {
            content = "DONATE_" + donationType + "_" + safeName;
        }

        String qrUrl = "https://img.vietqr.io/image/" + bankId + "-" + accountNo + "-compact.png" +
                "?amount=" + amount + "&addInfo=" + content;

        imgQrCode.setVisibility(View.VISIBLE);
        imgQrCode.setAlpha(1.0f);
        if (tvQrStatus != null) tvQrStatus.setVisibility(View.GONE);
        if (tvTimer != null) tvTimer.setVisibility(View.VISIBLE);

        Glide.with(this).load(qrUrl).into(imgQrCode);

        timeLeftInMillis = 3 * 60 * 1000;
        startCountDown();
    }

    private void navigateToSuccess() {
         DonateSuccessFragment donatesuccessFragment = new DonateSuccessFragment();
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, donatesuccessFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void startCountDown() {
        if (qrRunnable != null) qrHandler.removeCallbacks(qrRunnable);
        qrRunnable = new Runnable() {
            @Override
            public void run() {
                if (timeLeftInMillis <= 0) {
                    imgQrCode.setAlpha(0.2f);
                    if (tvTimer != null) tvTimer.setVisibility(View.GONE);
                    if (tvQrStatus != null) {
                        tvQrStatus.setVisibility(View.VISIBLE);
                        tvQrStatus.setText("QR hết hạn. Nhấn để tải lại.");
                    }
                    isQrGenerated = false;
                    btnConfirm.setText("Xác nhận");
                } else {
                    int mins = (int) (timeLeftInMillis / 1000) / 60;
                    int secs = (int) (timeLeftInMillis / 1000) % 60;
                    if (tvTimer != null) {
                        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                    }
                    timeLeftInMillis -= 1000;
                    qrHandler.postDelayed(this, 1000);
                }
            }
        };
        qrHandler.post(qrRunnable);
    }

    private String removeAccent(String s) {
        if (s == null) return "GUEST";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    private void loadCurrentUserName() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.child("name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        currentUserName = snapshot.getValue(String.class);
                    } else {
                        currentUserName = "Guest";
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            currentUserName = "Guest";
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