package com.example.rfidpetrescue.fragments.profile;

import android.net.Uri;
import android.os.Bundle;
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
import com.example.rfidpetrescue.models.User; // IMPORT MODEL USER
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

public class MyProfileFragment extends Fragment {

    private ImageView imgAvatar, btnBack;
    private MaterialCardView btnCamera;
    private EditText edtName, edtPhone, edtEmail, edtAddress;
    private MaterialButton btnSave;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    private Uri selectedImageUri = null;

    // Trình chọn ảnh an toàn của Android (Photo Picker)
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    // Hiển thị ảnh vừa chọn lên UI ngay lập tức
                    Glide.with(this).load(uri).circleCrop().into(imgAvatar);
                } else {
                    Toast.makeText(getContext(), "Chưa chọn ảnh nào", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        btnBack = view.findViewById(R.id.btnBack);
        imgAvatar = view.findViewById(R.id.avatar);
        btnCamera = view.findViewById(R.id.btnCamera);
        edtName = view.findViewById(R.id.edtProfileName);
        edtPhone = view.findViewById(R.id.edtProfilePhone);
        edtEmail = view.findViewById(R.id.edtProfileEmail);
        edtAddress = view.findViewById(R.id.edtProfileAddress);
        btnSave = view.findViewById(R.id.btnSave);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            loadCurrentProfileData();
        }

        // Bắt sự kiện chọn ảnh khi nhấn vào nút Camera
        btnCamera.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Bắt sự kiện Lưu
        btnSave.setOnClickListener(v -> saveAllChanges());
    }

    // --- BƯỚC 1: TẢI DỮ LIỆU BẰNG MODEL USER ---
    private void loadCurrentProfileData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                if (snapshot.exists()) {
                    // Ép kiểu toàn bộ data về User
                    User userProfile = snapshot.getValue(User.class);

                    if (userProfile != null) {
                        if (userProfile.getName() != null) edtName.setText(userProfile.getName());
                        if (userProfile.getPhone() != null) edtPhone.setText(userProfile.getPhone());
                        if (userProfile.getAddress() != null) edtAddress.setText(userProfile.getAddress());

                        // Hiển thị Email đang lưu
                        if (userProfile.getEmail() != null) {
                            edtEmail.setText(userProfile.getEmail());
                        } else if (currentUser != null && currentUser.getEmail() != null) {
                            edtEmail.setText(currentUser.getEmail());
                        }

                        // Lấy ảnh qua getAvatar()
                        if (userProfile.getAvatar() != null && !userProfile.getAvatar().isEmpty()) {
                            Glide.with(MyProfileFragment.this)
                                    .load(userProfile.getAvatar())
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_dog_manage)
                                    .into(imgAvatar);
                        }
                    }
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

    // --- BƯỚC 2: BẮT ĐẦU LUỒNG LƯU DỮ LIỆU ---
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

        // Xử lý đổi Email trước
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
            // Không đổi email -> Tải ảnh luôn
            handleImageUpload(newName, newPhone, newAddress, newEmail);
        }
    }

    // --- BƯỚC 3: TẢI ẢNH LÊN CLOUDINARY ---
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
            // Không có ảnh mới -> Cập nhật Text
            updateDatabase(name, phone, address, email, null);
        }
    }

    // --- BƯỚC 4: LƯU VÀO FIREBASE REALTIME DATABASE THEO MODEL ---
    private void updateDatabase(String name, String phone, String address, String email, String newImageUrl) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("email", email);

        // LƯU Ý: Lưu bằng key "avatar" để chuẩn với class User
        if (newImageUrl != null) {
            updates.put("avatar", newImageUrl);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (!isAdded() || getContext() == null) return;

            btnSave.setEnabled(true);
            btnSave.setText("Lưu thay đổi");

            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                getParentFragmentManager().popBackStack();
            } else {
                Toast.makeText(getContext(), "Lỗi cập nhật: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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