package com.example.rfidpetrescue.fragments.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.LoginActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminProfileFragment extends Fragment {

    // Ánh xạ các View từ XML
    private ImageView imgAvatar;
    private TextView tvNameHeader, tvEmailHeader;
    private EditText edtName, edtPhone, edtEmail, edtAddress;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private ValueEventListener profileListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Khởi tạo ánh xạ View theo đúng ID trong XML của bạn
        imgAvatar = view.findViewById(R.id.imgAvatar);
        tvNameHeader = view.findViewById(R.id.tvName);
        tvEmailHeader = view.findViewById(R.id.tvEmailHeader);

        edtName = view.findViewById(R.id.name);
        edtPhone = view.findViewById(R.id.dienthoai);
        edtEmail = view.findViewById(R.id.email);
        edtAddress = view.findViewById(R.id.diachi);

        FrameLayout btnLogout = view.findViewById(R.id.rectangle_6);
        MaterialCardView btnEditHeader = view.findViewById(R.id.btnEditHeader);

        // 2. Cấu hình Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());

            // Lắng nghe dữ liệu thay đổi để cập nhật UI ngay lập tức
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isAdded()) return;

                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String address = snapshot.child("address").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);
                        String avatarUrl = snapshot.child("avatar").getValue(String.class);

                        // Cập nhật lên Header
                        if (name != null) tvNameHeader.setText(name);
                        if (email != null) tvEmailHeader.setText(email);

                        // Cập nhật vào các ô nhập liệu (đang bị disable)
                        if (name != null) edtName.setText(name);
                        if (phone != null) edtPhone.setText(phone);
                        if (email != null) edtEmail.setText(email);
                        if (address != null) edtAddress.setText(address);

                        // Tải ảnh đại diện
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(AdminProfileFragment.this)
                                    .load(avatarUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_rescue_logo)
                                    .into(imgAvatar);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            };
            userRef.addValueEventListener(profileListener);
        }

        // 3. Xử lý nút Chỉnh sửa
        btnEditHeader.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminEditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // 4. Xử lý nút Đăng xuất
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Xóa listener để tránh rò rỉ bộ nhớ khi fragment bị hủy
        if (userRef != null && profileListener != null) {
            userRef.removeEventListener(profileListener);
        }
    }
}