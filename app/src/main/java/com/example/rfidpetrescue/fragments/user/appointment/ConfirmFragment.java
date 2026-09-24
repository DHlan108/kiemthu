package com.example.rfidpetrescue.fragments.user.appointment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.user.NotSuccessFragment;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ConfirmFragment extends Fragment {

    private EditText edtName, edtPhone, edtEmail;
    private TextView tvAptTime, tvAptDate, tvAptLocation, tvAptContact;
    private ImageView imgPet, imgGender;
    private TextView tvPetName, tvPetBreedAge, tvPetLocationConfirm;
    private MaterialButton btnBook;
    private ImageButton btnBack;

    private EditText edtAddress, edtOccupation;
    private TextView tvHousingType, tvReasonNoOldPet, tvReasonAdoption;
    private RadioGroup rgPetExperience, rgChildren;
    private RadioButton rbExpYes, rbExpNo, rbChildYes, rbChildNo;

    private String petId, selectedDate, selectedTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_confirm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            selectedDate = getArguments().getString("selected_date");
            selectedTime = getArguments().getString("selected_time");
        }

        initViews(view);
        setupDropdownListeners();

        tvAptDate.setText("Ngày: " + (selectedDate != null ? selectedDate : "N/A"));
        tvAptTime.setText("Thời gian: " + (selectedTime != null ? selectedTime : "N/A"));
        tvAptLocation.setText("Địa điểm: Trạm cứu hộ Sân nhà nhiều chó, Hà Nội");
        tvAptContact.setText("Liên hệ: +84 988 015 445");

        loadPetInfo();

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnBook.setOnClickListener(v -> pushAppointmentToFirebase());
    }

    private void initViews(View view) {
        edtName = view.findViewById(R.id.edtName);
        edtPhone = view.findViewById(R.id.edtPhone);
        edtEmail = view.findViewById(R.id.edtEmail);
        tvAptTime = view.findViewById(R.id.tvAptTime);
        tvAptDate = view.findViewById(R.id.tvAptDate);
        tvAptLocation = view.findViewById(R.id.tvAptLocation);
        tvAptContact = view.findViewById(R.id.tvAptContact);
        imgPet = view.findViewById(R.id.imgPetConfirm);
        imgGender = view.findViewById(R.id.imgGender);
        tvPetName = view.findViewById(R.id.tvPetNameConfirm);
        tvPetBreedAge = view.findViewById(R.id.tvPetBreedAgeConfirm);
        tvPetLocationConfirm = view.findViewById(R.id.tvPetLocationConfirm);
        btnBook = view.findViewById(R.id.btnBook);
        btnBack = view.findViewById(R.id.btnBack);

        edtAddress = view.findViewById(R.id.edtAddress);
        edtOccupation = view.findViewById(R.id.edtOccupation);
        tvHousingType = view.findViewById(R.id.tvHousingType);
        tvReasonNoOldPet = view.findViewById(R.id.tvReasonNoOldPet);
        tvReasonAdoption = view.findViewById(R.id.tvReasonAdoption);

        rgPetExperience = view.findViewById(R.id.rgPetExperience);
        rbExpYes = view.findViewById(R.id.rbExpYes);
        rbExpNo = view.findViewById(R.id.rbExpNo);

        rgChildren = view.findViewById(R.id.rgChildren);
        rbChildYes = view.findViewById(R.id.rbChildYes);
        rbChildNo = view.findViewById(R.id.rbChildNo);
    }

    private void setupDropdownListeners() {
        tvHousingType.setOnClickListener(v -> {
            String[] options = {"Nhà mặt đất", "Chung cư", "Phòng trọ", "Biệt thự"};
            showSelectionDialog("Chọn loại nhà ở", options, tvHousingType);
        });

        tvReasonNoOldPet.setOnClickListener(v -> {
            String[] options = {"Thú cưng đã mất", "Chưa từng nuôi", "Tặng cho người khác", "Lý do khác"};
            showSelectionDialog("Lý do không nuôi thú cưng cũ", options, tvReasonNoOldPet);
        });

        tvReasonAdoption.setOnClickListener(v -> {
            String[] options = {"Tìm bầu bạn", "Giữ nhà", "Tặng người thân", "Yêu thương động vật"};
            showSelectionDialog("Lý do nhận nuôi", options, tvReasonAdoption);
        });
    }

    private void showSelectionDialog(String title, String[] options, TextView targetView) {
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(options, (dialog, which) -> {
                    targetView.setText(options[which]);
                    targetView.setTextColor(getResources().getColor(android.R.color.black));
                })
                .show();
    }

    private void loadPetInfo() {
        if (petId == null || petId.isEmpty()) return;

        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(petId);
        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String breed = snapshot.child("breed").getValue(String.class);
                    String age = snapshot.child("age").getValue(String.class);
                    String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                    String livingEnv = snapshot.child("living_environment").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);

                    tvPetName.setText(name);
                    tvPetBreedAge.setText((breed != null ? breed : "Không rõ") + " | " + (age != null ? age : ""));
                    if (livingEnv != null) tvPetLocationConfirm.setText(livingEnv);

                    if (gender != null) {
                        imgGender.setVisibility(View.VISIBLE);
                        if (gender.equalsIgnoreCase("Đực") || gender.equalsIgnoreCase("Male")) {
                            imgGender.setImageResource(R.drawable.ic_male);
                            imgGender.setColorFilter(android.graphics.Color.parseColor("#1D1A9B"));
                        } else if (gender.equalsIgnoreCase("Cái") || gender.equalsIgnoreCase("Female")) {
                            imgGender.setImageResource(R.drawable.ic_female);
                            imgGender.setColorFilter(android.graphics.Color.parseColor("#28A5FF"));
                        } else {
                            imgGender.setVisibility(View.GONE);
                        }
                    } else {
                        imgGender.setVisibility(View.GONE);
                    }

                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(ConfirmFragment.this)
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_dog_manage)
                                .into(imgPet);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void pushAppointmentToFirebase() {
        String name = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String address = edtAddress.getText().toString().trim();
        String occupation = edtOccupation.getText().toString().trim();

        String housingType = tvHousingType.getText().toString();
        String reasonNoOld = tvReasonNoOldPet.getText().toString();
        String reasonAdopt = tvReasonAdoption.getText().toString();

        String petExperience = rbExpYes.isChecked() ? "Có" : (rbExpNo.isChecked() ? "Không" : "");
        String hasChildren = rbChildYes.isChecked() ? "Có" : (rbChildNo.isChecked() ? "Không" : "");

        if (name.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập đủ Tên, SĐT và Địa chỉ!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBook.setEnabled(false);
        btnBook.setText("Đang xử lý...");

        DatabaseReference aptRef = FirebaseDatabase.getInstance().getReference("Appointments");
        String aptId = aptRef.push().getKey();
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "guest_user";

        Map<String, Object> appointmentData = new HashMap<>();
        appointmentData.put("userId", userId);
        appointmentData.put("petId", petId);
        appointmentData.put("adopterName", name);
        appointmentData.put("adopterPhone", phone);
        appointmentData.put("adopterEmail", email);
        appointmentData.put("adopterAddress", address);
        appointmentData.put("housingType", housingType.contains("Loại nhà ở") ? "" : housingType);
        appointmentData.put("petExperience", petExperience);
        appointmentData.put("reasonNoOldPet", reasonNoOld.contains("Lý do") ? "" : reasonNoOld);
        appointmentData.put("hasChildren", hasChildren);
        appointmentData.put("occupation", occupation);
        appointmentData.put("reasonAdoption", reasonAdopt.contains("Lý do") ? "" : reasonAdopt);
        appointmentData.put("date", selectedDate);
        appointmentData.put("time", selectedTime);
        appointmentData.put("location", "Trạm cứu hộ Sân nhà nhiều chó");
        appointmentData.put("status", "Pending");
        appointmentData.put("createdAt", String.valueOf(System.currentTimeMillis()));

        if (aptId != null) {
            aptRef.child(aptId).setValue(appointmentData).addOnCompleteListener(task -> {
                if (!isAdded() || getContext() == null) return;

                if (task.isSuccessful()) {
                    // TỰ ĐỘNG CẬP NHẬT TRẠNG THÁI THÚ CƯNG TRONG NODE PETS
                    updatePetStatus(petId, "Đang chờ phỏng vấn");

                    Toast.makeText(getContext(), "Đặt lịch thành công!", Toast.LENGTH_SHORT).show();
                    sendNotificationToAdmin(name, petId);
                    navigateToSuccess();
                } else {
                    navigateToNotSuccess();
                }
            });
        }
    }
    private void navigateToNotSuccess() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                // Thay "NotSuccessFragment" bằng tên Fragment lỗi thực tế của bạn (ví dụ: ErrorFragment)
                .replace(R.id.main_container, new NotSuccessFragment())
                .commit();
    }
    private void updatePetStatus(String pid, String status) {
        if (pid == null || pid.isEmpty()) return;
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(pid);
        petRef.child("status").setValue(status);
    }

    private void sendNotificationToAdmin(String userName, String petId) {
        DatabaseReference adminNotifRef = FirebaseDatabase.getInstance().getReference("Admin_Notifications");
        String notifId = adminNotifRef.push().getKey();

        if (notifId != null) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("id", notifId);
            notifData.put("title", "Đơn đăng ký nhận nuôi mới!");
            notifData.put("message", "Người dùng " + userName + " vừa gửi đơn đăng ký nhận nuôi cho thú cưng có ID: " + petId);
            notifData.put("timestamp", System.currentTimeMillis());
            notifData.put("isRead", false);
            notifData.put("type", "adopt");

            adminNotifRef.child(notifId).setValue(notifData);
        }
    }

    private void navigateToSuccess() {
        if (!isAdded()) return;
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new SuccessFragment())
                .commit();
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