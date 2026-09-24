package com.example.rfidpetrescue.fragments.admin.category.donate;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.activities.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class AddCampaignAdminFragment extends Fragment {

    private MaterialCardView btnBack, btnChangeImg;
    private ImageView imgCampaignThumb;
    private EditText etCampaignName, etCampaignContent, etTargetAmount, etDescription, etPurpose;
    private RelativeLayout layoutStartDate, layoutEndDate;
    private TextView tvStartDate, tvEndDate;
    private MaterialButton btnLaunchCampaign;

    private Uri selectedImageUri = null;

    // Trình chọn ảnh
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(imgCampaignThumb);
                    checkRequiredFields(); // Kiểm tra lại sau khi chọn ảnh
                } else {
                    Toast.makeText(getContext(), "Chưa chọn ảnh nào", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_campaign, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        btnBack = view.findViewById(R.id.btnBack);
        btnChangeImg = view.findViewById(R.id.btnchangeimg);
        imgCampaignThumb = view.findViewById(R.id.imgCampaignThumb);
        etCampaignName = view.findViewById(R.id.etCampaignName);
        etCampaignContent = view.findViewById(R.id.etCampaignContent);
        etTargetAmount = view.findViewById(R.id.etTargetAmount);
        etDescription = view.findViewById(R.id.etDescription);
        etPurpose = view.findViewById(R.id.etPurpose);
        layoutStartDate = view.findViewById(R.id.layoutStartDate);
        layoutEndDate = view.findViewById(R.id.layoutEndDate);
        tvStartDate = view.findViewById(R.id.tvStartDate);
        tvEndDate = view.findViewById(R.id.tvEndDate);
        btnLaunchCampaign = view.findViewById(R.id.btnLaunchCampaign);

        // Bắt sự kiện click
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        btnChangeImg.setOnClickListener(v -> pickMedia.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()
        ));

        layoutStartDate.setOnClickListener(v -> DatePickerUtils.show(this, tvStartDate, null, date -> checkRequiredFields()));
        layoutEndDate.setOnClickListener(v -> DatePickerUtils.show(this, tvEndDate, null, date -> checkRequiredFields()));

        btnLaunchCampaign.setOnClickListener(v -> validateAndSubmitCampaign());

        // Gắn bộ lắng nghe thay đổi Text cho các ô nhập liệu quan trọng
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkRequiredFields(); // Kiểm tra liên tục khi người dùng gõ phím
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etCampaignName.addTextChangedListener(textWatcher);
        etTargetAmount.addTextChangedListener(textWatcher);

        // Chạy kiểm tra lần đầu tiên để vô hiệu hóa nút khi mới vào màn hình
        checkRequiredFields();
    }

    // --- HÀM KIỂM TRA DỮ LIỆU ĐỂ ĐỔI MÀU NÚT ---
    private void checkRequiredFields() {
        String name = etCampaignName.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String startDate = tvStartDate.getText().toString().trim();
        String endDate = tvEndDate.getText().toString().trim();

        // Điều kiện: Đã có ảnh + có tên + có tiền + đã chọn cả 2 ngày
        boolean isValid = selectedImageUri != null
                && !name.isEmpty()
                && !targetAmountStr.isEmpty()
                && !startDate.equals("dd/mm/yy")
                && !endDate.equals("dd/mm/yy");

        if (isValid) {
            btnLaunchCampaign.setEnabled(true);
            // Đổi sang màu sáng (ví dụ: Màu xanh của app)
            btnLaunchCampaign.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#007BFF")));
        } else {
            btnLaunchCampaign.setEnabled(false);
            // Đổi sang màu xám nhạt khi chưa đủ thông tin
            btnLaunchCampaign.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }
    }

    // --- BƯỚC 1: XỬ LÝ LƯU ---
    private void validateAndSubmitCampaign() {
        String name = etCampaignName.getText().toString().trim();
        String content = etCampaignContent.getText().toString().trim();
        String targetAmountStr = etTargetAmount.getText().toString().trim();
        String startDate = tvStartDate.getText().toString().trim();
        String endDate = tvEndDate.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String purpose = etPurpose.getText().toString().trim();

        long targetAmount = 0;
        try {
            // Lọc bỏ ký tự thừa (dấu chấm, phẩy...)
            String cleanAmountStr = targetAmountStr.replaceAll("[^0-9]", "");
            if (cleanAmountStr.isEmpty()) {
                Toast.makeText(getContext(), "Số tiền không hợp lệ!", Toast.LENGTH_SHORT).show();
                return;
            }
            targetAmount = Long.parseLong(cleanAmountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số tiền quá lớn hoặc không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLaunchCampaign.setEnabled(false);
        btnLaunchCampaign.setText("Đang xử lý...");

        uploadImageToCloudinary(name, content, targetAmount, startDate, endDate, description, purpose);
    }

    // --- BƯỚC 2: UPLOAD ẢNH LÊN CLOUDINARY ---
    private void uploadImageToCloudinary(String name, String content, long targetAmount,
                                         String startDate, String endDate, String description, String purpose) {
        MediaManager.get().upload(selectedImageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String imageUrl = (String) resultData.get("secure_url");
                saveCampaignToFirebase(name, content, targetAmount, startDate, endDate, description, purpose, imageUrl);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Lỗi tải ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                btnLaunchCampaign.setEnabled(true);
                btnLaunchCampaign.setText("Phát hành chiến dịch");
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    // --- BƯỚC 3: LƯU VÀO FIREBASE ---
    private void saveCampaignToFirebase(String name, String content, long targetAmount,
                                        String startDate, String endDate, String description, String purpose, String imageUrl) {
        DatabaseReference campaignsRef = FirebaseDatabase.getInstance().getReference("Campaigns");
        String campaignId = campaignsRef.push().getKey();

        HashMap<String, Object> campaignMap = new HashMap<>();
        campaignMap.put("id", campaignId);
        campaignMap.put("title", name);
        campaignMap.put("sub_title", content);
        campaignMap.put("target_amount", targetAmount);
        campaignMap.put("current_amount", 0L);
        campaignMap.put("start_date", startDate);
        campaignMap.put("end_date", endDate);
        campaignMap.put("description", description);
        campaignMap.put("usage_purpose", purpose);
        campaignMap.put("image_url", imageUrl);
        campaignMap.put("status", "Active");
        campaignMap.put("timestamp", System.currentTimeMillis());

        if (campaignId != null) {
            campaignsRef.child(campaignId).setValue(campaignMap).addOnCompleteListener(task -> {
                if (!isAdded()) return;

                btnLaunchCampaign.setEnabled(true);
                btnLaunchCampaign.setText("Phát hành chiến dịch");

                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Tạo chiến dịch thành công!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(), "Lỗi lưu dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
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