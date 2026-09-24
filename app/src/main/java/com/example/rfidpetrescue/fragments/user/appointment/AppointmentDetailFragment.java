package com.example.rfidpetrescue.fragments.user.appointment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AppointmentDetailFragment extends Fragment {

    private String aptId;

    // Các biến giao diện chi tiết
    private EditText edtName, edtPhone, edtEmail;
    private TextView tvTime, tvDate, tvPetName, tvPetBreedAge;
    private ImageView imgPet;
    private ImageView btnBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_appointment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (getArguments() != null) {
            aptId = getArguments().getString("apt_id");
        }

        if (aptId != null && !aptId.isEmpty()) {
            loadAppointmentDetail();
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        // Ánh xạ thông tin cá nhân
        edtName = view.findViewById(R.id.edtName);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtEmail = view.findViewById(R.id.edtEmail);

        // Ánh xạ thông tin cuộc hẹn
        tvTime = view.findViewById(R.id.tvAptTime);
        tvDate = view.findViewById(R.id.tvAptDate);

        // Ánh xạ thông tin thú cưng
        tvPetName = view.findViewById(R.id.tvPetNameConfirm);
        tvPetBreedAge = view.findViewById(R.id.tvPetBreedAgeConfirm);
        imgPet = view.findViewById(R.id.imgPetConfirm);

        // Ánh xạ nút bấm
        btnBack = view.findViewById(R.id.btnBack);
    }

    private void loadAppointmentDetail() {
        if (aptId == null) return;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments").child(aptId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    // Đổ dữ liệu lịch hẹn
                    if (edtName != null) {
                        edtName.setText(snapshot.child("adopterName").getValue(String.class));
                        edtName.setEnabled(false);
                    }
                    if (edtPhone != null) {
                        edtPhone.setText(snapshot.child("adopterPhone").getValue(String.class));
                        edtPhone.setEnabled(false);
                    }
                    if (edtEmail != null) {
                        edtEmail.setText(snapshot.child("adopterEmail").getValue(String.class));
                        edtEmail.setEnabled(false);
                    }
                    if (tvTime != null) tvTime.setText("Thời gian: " + snapshot.child("time").getValue(String.class));
                    if (tvDate != null) tvDate.setText("Ngày: " + snapshot.child("date").getValue(String.class));

                    // Load thông tin Pet từ petId nằm trong Appointment
                    String petId = snapshot.child("petId").getValue(String.class);
                    if (petId != null) loadPetInfo(petId);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPetInfo(String id) {
        FirebaseDatabase.getInstance().getReference("Pets").child(id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded()) {
                            if (tvPetName != null) tvPetName.setText(snapshot.child("name").getValue(String.class));
                            String breed = snapshot.child("breed").getValue(String.class);
                            String age = snapshot.child("age").getValue(String.class);
                            if (tvPetBreedAge != null) tvPetBreedAge.setText(breed + " | " + age);

                            String imgUrl = snapshot.child("imageUrl").getValue(String.class);
                            if (imgPet != null && imgUrl != null) Glide.with(requireContext()).load(imgUrl).into(imgPet);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (isAdded() && getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Ẩn thanh Bottom Bar khi vào màn hình chi tiết
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}