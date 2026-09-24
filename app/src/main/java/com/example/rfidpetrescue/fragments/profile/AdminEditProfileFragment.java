package com.example.rfidpetrescue.fragments.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class AdminEditProfileFragment extends Fragment {

    private ImageView btnBack;
    private MaterialButton btnSave;
    private EditText edtName, edtPhone, edtEmail, edtAddress;

    private ImageView imgAvatar;
    private MaterialCardView btnChangeAvatar;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    private Uri selectedImageUri = null;

    // Các biến lưu trữ dữ liệu gốc để so sánh
    private String originalName = "";
    private String originalPhone = "";
    private String originalEmail = "";
    private String originalAddress = "";

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(imgAvatar);
                    // ĐỒNG BỘ: Kiểm tra lại trạng thái nút khi có ảnh mới
                    checkFormChanges();
                } else {
                    Toast.makeText(getContext(), "Chưa chọn ảnh nào", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        btnBack = view.findViewById(R.id.btnBack);
        btnSave = view.findViewById(R.id.btnSave);
        edtName = view.findViewById(R.id.edtName);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtEmail = view.findViewById(R.id.edtEmail);
        edtAddress = view.findViewById(R.id.edtAddress);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnChangeAvatar = view.findViewById(R.id.btnChangeAvatar);

        // Mặc định làm mờ nút Lưu khi vừa vào trang
        btnSave.setEnabled(false);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            loadCurrentProfileData();
        }

        btnChangeAvatar.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnSave.setOnClickListener(v -> saveAllChanges());
    }

    // --- BƯỚC 1: TẢI VÀ LƯU DỮ LIỆU GỐC ---
    private void loadCurrentProfileData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String phone = snapshot.child("phone").getValue(String.class);
                    String address = snapshot.child("address").getValue(String.class);
                    String avatarUrl = snapshot.child("avatar").getValue(String.class);
                    String email = currentUser.getEmail();

                    // Gán vào biến gốc (Nếu null thì cho bằng chuỗi rỗng để tránh lỗi)
                    originalName = (name != null) ? name : "";
                    originalPhone = (phone != null) ? phone : "";
                    originalAddress = (address != null) ? address : "";
                    originalEmail = (email != null) ? email : "";

                    edtName.setText(originalName);
                    edtPhone.setText(originalPhone);
                    edtAddress.setText(originalAddress);
                    edtEmail.setText(originalEmail);

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(AdminEditProfileFragment.this)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_rescue_logo)
                                .into(imgAvatar);
                    }

                    // ĐỒNG BỘ: Gọi TextWatcher sau khi đã set Text xong để tránh lỗi bật nút sai
                    setupTextWatchers();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // --- ĐỒNG BỘ LẮNG NGHE SỰ THAY ĐỔI ---
    private void setupTextWatchers() {
        TextWatcher formWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { checkFormChanges(); }
            @Override public void afterTextChanged(Editable s) {}
        };

        edtName.addTextChangedListener(formWatcher);
        edtPhone.addTextChangedListener(formWatcher);
        edtEmail.addTextChangedListener(formWatcher);
        edtAddress.addTextChangedListener(formWatcher);
    }

    // --- KIỂM TRA ĐIỀU KIỆN ĐỂ BẬT NÚT LƯU ---
    private void checkFormChanges() {
        String currentName = edtName.getText().toString().trim();
        String currentPhone = edtPhone.getText().toString().trim();
        String currentEmail = edtEmail.getText().toString().trim();
        String currentAddress = edtAddress.getText().toString().trim();

        // 1. Kiểm tra xem có gì khác so với lúc mới tải về không?
        boolean isDataChanged = !currentName.equals(originalName) ||
                !currentPhone.equals(originalPhone) ||
                !currentEmail.equals(originalEmail) ||
                !currentAddress.equals(originalAddress) ||
                selectedImageUri != null; // Có chọn ảnh mới

        // 2. Điều kiện bắt buộc (Tên và Email không được xóa trắng)
        boolean isFormValid = !currentName.isEmpty() && !currentEmail.isEmpty();

        // Bật nút nếu thỏa mãn cả 2: Có thay đổi VÀ form hợp lệ
        if (btnSave != null) {
            btnSave.setEnabled(isDataChanged && isFormValid);
        }
    }

    private void saveAllChanges() {
        if (currentUser == null) return;

        String newName = edtName.getText().toString().trim();
        String newPhone = edtPhone.getText().toString().trim();
        String newAddress = edtAddress.getText().toString().trim();
        String newEmail = edtEmail.getText().toString().trim();

        if (newName.isEmpty() || newEmail.isEmpty()) {
            Toast.makeText(getContext(), "Tên và Email không được để trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang xử lý...");

        if (!newEmail.equals(currentUser.getEmail())) {
            currentUser.updateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    handleImageUpload(newName, newPhone, newAddress, newEmail);
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    Toast.makeText(getContext(), "Cần đăng xuất và đăng nhập lại để đổi Email.", Toast.LENGTH_LONG).show();
                }
            });
        } else {
            handleImageUpload(newName, newPhone, newAddress, newEmail);
        }
    }

    private void handleImageUpload(String name, String phone, String address, String email) {
        if (selectedImageUri != null) {
            btnSave.setText("Đang tải ảnh lên...");

            MediaManager.get().upload(selectedImageUri).callback(new UploadCallback() {
                @Override
                public void onStart(String requestId) {}

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {}

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    String uploadedImageUrl = (String) resultData.get("secure_url");
                    updateDatabase(name, phone, address, email, uploadedImageUrl);
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Lỗi tải ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {}
            }).dispatch();
        } else {
            updateDatabase(name, phone, address, email, null);
        }
    }

    private void updateDatabase(String name, String phone, String address, String email, String newImageUrl) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("email", email);

        if (newImageUrl != null) {
            updates.put("avatar", newImageUrl);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;

            btnSave.setEnabled(true);
            btnSave.setText("Lưu thay đổi");

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Đã cập nhật toàn bộ thông tin!", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Cập nhật Database thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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