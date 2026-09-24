package com.example.rfidpetrescue.fragments.admin.scanrfid;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.example.rfidpetrescue.utils.DropdownUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class AddIdFragment extends Fragment {

    // Views
    private ImageButton btnBack;
    private MaterialButton btnSubmit;
    private MaterialCardView btnChangeAvatar, btnSelectStatus, btnSelectPetType;
    private ImageView imgPetAvatar, ivBarcode;
    private EditText edtPetName, edtRescueHistory, edtPetNote;

    // Các trường chi tiết
    private EditText edtDetailBreed, edtDetailAge, edtDetailWeight, edtDetailColor;
    private TextView tvStatus, tvPetType, tvDetailType, tvDetailGender, tvDetailSterilized, tvRfidCode;

    private String newRfidCode = "";
    private Uri selectedImageUri = null;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(imgPetAvatar);
                } else {
                    Toast.makeText(getContext(), "Chưa chọn ảnh nào", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_madinhdanh, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            String rawRfid = getArguments().getString("NEW_RFID", "");
            // LỌC DỮ LIỆU TỪ MÁY QUÉT BLUETOOTH
            newRfidCode = rawRfid.replaceAll("[^a-zA-Z0-9]", "").trim();

            // ĐỒNG BỘ: Tạo và hiển thị Barcode
            if (!newRfidCode.isEmpty()) {
                tvRfidCode.setText(newRfidCode);
                Bitmap barcode = BarcodeUtils.generateBarcode(newRfidCode);
                if (barcode != null && ivBarcode != null) {
                    ivBarcode.setImageBitmap(barcode);
                }
            } else {
                tvRfidCode.setText("Lỗi mã RFID");
            }
        }

        setupClickListeners();
        setupTextWatchers();
        checkFormCompletion();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);
        btnSelectStatus = view.findViewById(R.id.btnSelectStatus);
        btnSelectPetType = view.findViewById(R.id.btnSelectPetType);
        imgPetAvatar = view.findViewById(R.id.imgPetAvatar);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        tvRfidCode = view.findViewById(R.id.tvRfidCode);

        edtPetName = view.findViewById(R.id.edtPetName);
        edtRescueHistory = view.findViewById(R.id.edtRescueHistory);
        edtPetNote = view.findViewById(R.id.edtPetNote);

        tvStatus = view.findViewById(R.id.tvStatus);
        tvPetType = view.findViewById(R.id.tvPetType);
        tvDetailType = view.findViewById(R.id.tvDetailType);
        tvDetailGender = view.findViewById(R.id.tvDetailGender);
        tvDetailSterilized = view.findViewById(R.id.tvDetailSterilized);

        edtDetailBreed = view.findViewById(R.id.edtDetailBreed);
        edtDetailAge = view.findViewById(R.id.edtDetailAge);
        edtDetailWeight = view.findViewById(R.id.edtDetailWeight);
        edtDetailColor = view.findViewById(R.id.edtDetailColor);
    }

    private void setupTextWatchers() {
        TextWatcher formWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkFormCompletion(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtPetName.addTextChangedListener(formWatcher);
        edtDetailBreed.addTextChangedListener(formWatcher);
        edtDetailAge.addTextChangedListener(formWatcher);
        edtDetailWeight.addTextChangedListener(formWatcher);
        edtDetailColor.addTextChangedListener(formWatcher);
        edtRescueHistory.addTextChangedListener(formWatcher);
    }

    private void checkFormCompletion() {
        String petName = edtPetName.getText().toString().trim();
        String breed = edtDetailBreed.getText().toString().trim();
        String age = edtDetailAge.getText().toString().trim();
        String weight = edtDetailWeight.getText().toString().trim();
        String color = edtDetailColor.getText().toString().trim();
        String rescueHistory = edtRescueHistory.getText().toString().trim();

        boolean isReady = !petName.isEmpty() &&
                !breed.isEmpty() &&
                !age.isEmpty() &&
                !weight.isEmpty() &&
                !color.isEmpty() &&
                !rescueHistory.isEmpty();

        if (btnSubmit != null) {
            btnSubmit.setEnabled(isReady);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().getSupportFragmentManager().popBackStack();
        });

        View.OnClickListener pickImageListener = v -> pickMedia.launch(
                new PickVisualMediaRequest.Builder().setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()
        );
        btnChangeAvatar.setOnClickListener(pickImageListener);
        imgPetAvatar.setOnClickListener(pickImageListener);

        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        btnSelectPetType.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), btnSelectPetType, new String[]{"Chó", "Mèo"}, selectedItem -> {
                    tvPetType.setText(selectedItem);
                    tvDetailType.setText(selectedItem);
                })
        );

        btnSelectStatus.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), btnSelectStatus, new String[]{"Đang điều trị", "Sẵn sàng nhận nuôi", "Đã nhận nuôi","Đang chờ phỏng vấn","Đang chờ nhận nuôi"}, selectedItem -> tvStatus.setText(selectedItem))
        );
        tvDetailGender.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), tvDetailGender, new String[]{"Đực", "Cái", "Chưa rõ"}, selectedItem -> tvDetailGender.setText(selectedItem))
        );
        tvDetailSterilized.setOnClickListener(v ->
                DropdownUtils.showRoundedDropdown(requireContext(), tvDetailSterilized, new String[]{"Đã triệt sản", "Chưa triệt sản", "Chưa rõ"}, selectedItem -> tvDetailSterilized.setText(selectedItem))
        );
    }

    private void validateAndSubmit() {
        String petName = edtPetName.getText().toString().trim();
        String breed = edtDetailBreed.getText().toString().trim();
        String age = edtDetailAge.getText().toString().trim();
        String weight = edtDetailWeight.getText().toString().trim();
        String color = edtDetailColor.getText().toString().trim();
        String rescueHistory = edtRescueHistory.getText().toString().trim();

        if (petName.isEmpty() || breed.isEmpty() || age.isEmpty() ||
                weight.isEmpty() || color.isEmpty() || rescueHistory.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đầy đủ thông tin kể cả lịch sử cứu hộ!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang xử lý...");

        if (selectedImageUri != null) {
            uploadImageToCloudinary();
        } else {
            saveToFirebase("");
        }
    }

    private void uploadImageToCloudinary() {
        btnSubmit.setText("Đang tải ảnh lên...");

        MediaManager.get().upload(selectedImageUri).callback(new UploadCallback() {
            @Override
            public void onStart(String requestId) {}
            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) {}

            @Override
            public void onSuccess(String requestId, Map resultData) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String secureUrl = (String) resultData.get("secure_url");
                    saveToFirebase(secureUrl);
                });
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                if (!isAdded() || getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Lỗi up ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("Thêm mã định danh");
                });
            }
            @Override
            public void onReschedule(String requestId, ErrorInfo error) {}
        }).dispatch();
    }

    private void saveToFirebase(String imageUrl) {
        btnSubmit.setText("Đang lưu dữ liệu...");

        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("Pets");
        String newPetId = petsRef.push().getKey();
        String formattedTag = "tag_" + newRfidCode;

        double weight = 0.0;
        try {
            // Fix lỗi người dùng nhập phẩy (ví dụ: 4,5) thay vì chấm (4.5)
            String weightStr = edtDetailWeight.getText().toString().trim().replace(",", ".");
            if (!weightStr.isEmpty()) {
                weight = Double.parseDouble(weightStr);
            }
        } catch (NumberFormatException e) {
            weight = 0.0;
        }

        String breed = edtDetailBreed.getText().toString().trim();
        String age = edtDetailAge.getText().toString().trim();
        String color = edtDetailColor.getText().toString().trim();

        HashMap<String, Object> petData = new HashMap<>();
        petData.put("id", newPetId);
        petData.put("rfid_tag_id", formattedTag);
        petData.put("name", edtPetName.getText().toString().trim());
        petData.put("species", tvPetType.getText().toString());
        petData.put("status", tvStatus.getText().toString());
        petData.put("breed", breed.isEmpty() ? "Chưa rõ" : breed);
        petData.put("gender", tvDetailGender.getText().toString());
        petData.put("age", age.isEmpty() ? "Chưa rõ" : age);
        petData.put("weight", weight);
        petData.put("color", color.isEmpty() ? "Chưa rõ" : color);

        boolean isSterilized = tvDetailSterilized.getText().toString().equals("Đã triệt sản");
        petData.put("isSterilized", isSterilized);

        petData.put("rescueHistory", edtRescueHistory.getText().toString().trim());
        petData.put("note", edtPetNote.getText().toString().trim());
        petData.put("imageUrl", imageUrl);

        petsRef.child(newPetId).setValue(petData).addOnCompleteListener(task -> {
            if (!isAdded() || getActivity() == null) return;

            btnSubmit.setEnabled(true);
            btnSubmit.setText("Thêm mã định danh");

            if (task.isSuccessful()) {
                Toast.makeText(getActivity(), "Đã thêm thú cưng mới thành công!", Toast.LENGTH_SHORT).show();
                getActivity().getSupportFragmentManager().popBackStack();
                getActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getActivity(), "Lỗi lưu dữ liệu: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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