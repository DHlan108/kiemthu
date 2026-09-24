package com.example.rfidpetrescue.fragments.profile;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.LoginActivity;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.user.appointment.MyAppointmentFragment;
import com.example.rfidpetrescue.models.User; // Import Model User
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvUsername;
    private ImageView imgAvatar;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ánh xạ View
        tvName = view.findViewById(R.id.tvName);
        tvUsername = view.findViewById(R.id.tvUsername);
        imgAvatar = view.findViewById(R.id.imgAvatar);

        // Khởi tạo Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUser.getUid());
            loadUserInfo();
        }

        // Thiết lập sự kiện cho các nút menu
        setupMenuItems(view);
    }

    private void setupMenuItems(View view) {
        // --- Mục 1: My Profile ---
        View btnMyProfile = view.findViewById(R.id.btnMyProfile);
        if (btnMyProfile != null) {
            updateItemMenu(btnMyProfile, "Thông tin cá nhân", R.drawable.ic_avatar_unactive);
            btnMyProfile.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, new MyProfileFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // --- Mục 2: My Appointment ---
        View btnAppointment = view.findViewById(R.id.btnAppointment);
        if (btnAppointment != null) {
            updateItemMenu(btnAppointment, "Lịch hẹn của tôi", R.drawable.ic_calendar);
            btnAppointment.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, new MyAppointmentFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // --- Mục 3: About Us ---
        View btnAbout = view.findViewById(R.id.btnAbout);
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, new AboutUsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // --- Mục 4: Log Out ---
        View btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            updateItemMenu(btnLogout, "Đăng xuất", R.drawable.ic_logout);

            // Đổi màu Icon đăng xuất
            ImageView imgLogout = btnLogout.findViewById(R.id.menuIcon);
            if (imgLogout != null) {
                imgLogout.setColorFilter(Color.parseColor("#FF5C8D"));
            }

            btnLogout.setOnClickListener(v -> {
                mAuth.signOut();
                if (getActivity() != null) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    // Cập nhật text và icon cho các card menu
    private void updateItemMenu(View itemView, String title, int iconRes) {
        if (itemView != null) {
            TextView tvTitle = itemView.findViewById(R.id.menuTitle);
            ImageView ivIcon = itemView.findViewById(R.id.menuIcon);
            if (tvTitle != null) tvTitle.setText(title);
            if (ivIcon != null) ivIcon.setImageResource(iconRes);
        }
    }

    // --- HÀM LOAD DỮ LIỆU ĐÃ ĐƯỢC CẬP NHẬT THEO MODEL USER ---
    private void loadUserInfo() {
        if (userRef == null) return;

        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                if (snapshot.exists()) {
                    // Ép kiểu trực tiếp toàn bộ dữ liệu về class User
                    User userProfile = snapshot.getValue(User.class);

                    if (userProfile != null) {
                        // Set Tên
                        if (userProfile.getName() != null && tvName != null) {
                            tvName.setText(userProfile.getName());
                        }

                        // Set Username (Nếu không có username thì lấy phần đầu của email)
                        if (tvUsername != null) {
                            if (userProfile.getUsername() != null && !userProfile.getUsername().trim().isEmpty()) {
                                tvUsername.setText("@" + userProfile.getUsername());
                            } else if (userProfile.getEmail() != null) {
                                tvUsername.setText("@" + userProfile.getEmail().split("@")[0]);
                            }
                        }

                        // Set Ảnh đại diện (Sử dụng getAvatar() theo đúng model)
                        if (userProfile.getAvatar() != null && !userProfile.getAvatar().isEmpty() && imgAvatar != null) {
                            Glide.with(requireContext())
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
                    Toast.makeText(getContext(), "Lỗi tải thông tin: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        // Khi quay trở lại trang Profile -> BẬT thanh bar lên
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}