package com.example.rfidpetrescue.fragments.admin.category.adopt;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.databinding.FragmentAdoptionDetailBinding;
import com.example.rfidpetrescue.fragments.admin.scanrfid.info.PetInfoAdminFragment;
import com.example.rfidpetrescue.models.AdoptionApplication;
import com.example.rfidpetrescue.models.ScheduleTask;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAdoptDetailFragment extends Fragment {

    private FragmentAdoptionDetailBinding binding;
    private String appointmentId;
    private DatabaseReference appointmentRef;
    private AdoptionApplication currentApp;
    private String petName = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdoptionDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            appointmentId = getArguments().getString("APPOINTMENT_ID");
        }

        if (appointmentId != null) {
            appointmentRef = FirebaseDatabase.getInstance().getReference("Appointments").child(appointmentId);
            loadAppointmentDetail();
        }

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void loadAppointmentDetail() {
        appointmentRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists()) return;

                currentApp = snapshot.getValue(AdoptionApplication.class);
                if (currentApp != null) {
                    currentApp.setId(snapshot.getKey());
                    displayData(currentApp);
                    fetchPetName(currentApp.getPetId());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPetName(String petId) {
        if (petId == null || petId.isEmpty()) return;
        FirebaseDatabase.getInstance().getReference("Pets").child(petId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded() && binding != null) {
                            if (snapshot.exists()) {
                                petName = snapshot.getValue(String.class);
                                binding.tvDetailPetIdValue.setText(petName);
                            } else {
                                // Nếu thú cưng bị xóa, fallback hiện lại ID
                                binding.tvDetailPetIdValue.setText(petId);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        if (isAdded() && binding != null) binding.tvDetailPetIdValue.setText(petId);
                    }
                });
    }

    private void displayData(AdoptionApplication app) {
        binding.tvDetailApplicantName.setText(app.getAdopterName());
        binding.tvDetailStatus.setText(getStatusText(app.getStatus()));
        binding.tvDetailPetIdValue.setText("Đang tải...");
        binding.tvDetailPetIdValue.setPaintFlags(binding.tvDetailPetIdValue.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        binding.tvDetailPetIdValue.setOnClickListener(v -> {
            if (app.getPetId() != null) {
                PetInfoAdminFragment petInfoFragment = new PetInfoAdminFragment();
                Bundle bundle = new Bundle();
                bundle.putString("PET_ID", app.getPetId());
                petInfoFragment.setArguments(bundle);

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, petInfoFragment)
                            .addToBackStack(null)
                            .commit();
                }
            }
        });
        if (app.getCreatedAt() != null) {
            try {
                long time = Long.parseLong(app.getCreatedAt());
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                binding.tvDetailRegDateValue.setText(sdf.format(new Date(time)));
            } catch (Exception e) {
                binding.tvDetailRegDateValue.setText(app.getCreatedAt());
            }
        }

        binding.tvDetailPhoneValue.setText(app.getAdopterPhone());
        binding.tvDetailEmailValue.setText(app.getAdopterEmail());
        binding.tvDetailAddressValue.setText(app.getAdopterAddress());
        binding.tvDetailHouseValue.setText(app.getHousingType());
        binding.tvDetailExpValue.setText(app.getPetExperience());
        binding.tvDetailChildValue.setText(app.getHasChildren());
        binding.tvDetailJobValue.setText(app.getOccupation());
        binding.tvDetailReasonValue.setText(app.getReasonAdoption());

        binding.tvDetailDateValue.setText(app.getDate());
        binding.tvDetailTimeValue.setText(app.getTime());

        setupActionButtons(app.getStatus());
    }

    private void setupActionButtons(String status) {
        if (status == null) status = "Pending";

        if (status.equals("Pending")) {
            binding.btnConfirmInterview.setVisibility(View.VISIBLE);
            binding.btnRejectApplication.setVisibility(View.VISIBLE);
            binding.btnConfirmInterview.setText("Xác nhận phỏng vấn");
            binding.btnConfirmInterview.setOnClickListener(v -> updateStatus("Interview"));
            binding.btnRejectApplication.setOnClickListener(v -> updateStatus("Rejected"));
        } else if (status.equals("Interview")) {
            binding.btnConfirmInterview.setVisibility(View.VISIBLE);
            binding.btnRejectApplication.setVisibility(View.VISIBLE);
            binding.btnConfirmInterview.setText("Duyệt đơn");
            binding.btnConfirmInterview.setOnClickListener(v -> updateStatus("Approved"));
            binding.btnRejectApplication.setOnClickListener(v -> updateStatus("Rejected"));
        } else {
            binding.btnConfirmInterview.setVisibility(View.GONE);
            binding.btnRejectApplication.setVisibility(View.GONE);
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "Đang chờ";
        switch (status) {
            case "Pending": return "Đang chờ";
            case "Interview": return "Phỏng vấn";
            case "Approved": return "Đã duyệt";
            case "Rejected": return "Đã từ chối";
            default: return status;
        }
    }

    private void updateStatus(String newStatus) {
        if (appointmentRef != null && currentApp != null) {
            appointmentRef.child("status").setValue(newStatus).addOnSuccessListener(aVoid -> {

                Toast.makeText(getContext(), "Đã cập nhật trạng thái đơn: " + getStatusText(newStatus), Toast.LENGTH_SHORT).show();
                sendNotificationToUser(currentApp.getUserId(), newStatus);

                // GỌI HÀM CẬP NHẬT TRẠNG THÁI THÚ CƯNG BÊN NODE PETS
                updatePetStatusInDb(currentApp.getPetId(), newStatus);

                if ("Interview".equals(newStatus)) {
                    writeToAppointmentsNode();
                    addInterviewToAdminSchedule();
                }
            });
        }
    }

    private void addInterviewToAdminSchedule() {
        if (currentApp == null) return;

        String dateStr = currentApp.getDate();
        String timeStr = currentApp.getTime();

        if (dateStr == null || dateStr.isEmpty() || timeStr == null || timeStr.isEmpty()) return;

        try {
            SimpleDateFormat inputDateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            SimpleDateFormat outputDateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date date = inputDateFmt.parse(dateStr);
            String dateKey = outputDateFmt.format(date);

            String formattedTime = timeStr;
            try {
                SimpleDateFormat inputTimeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
                SimpleDateFormat outputTimeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());
                Date t = inputTimeFmt.parse(timeStr);
                formattedTime = outputTimeFmt.format(t);
            } catch (Exception e) {}

            DatabaseReference scheduleRef = FirebaseDatabase.getInstance().getReference("Admin_Schedules").child(dateKey);
            String taskId = scheduleRef.push().getKey();

            if (taskId != null) {
                ScheduleTask task = new ScheduleTask(
                        taskId,
                        "Phỏng vấn: " + currentApp.getAdopterName(),
                        "Phỏng vấn nhận nuôi bé: " + (petName != null && !petName.isEmpty() ? petName : currentApp.getPetId()) + "\nNgười phỏng vấn: Admin",
                        formattedTime,
                        dateKey,
                        "Nhận nuôi"
                );
                task.setPetId(currentApp.getPetId());
                task.setAdopterName(currentApp.getAdopterName());
                task.setTargetId(appointmentId);

                scheduleRef.child(taskId).setValue(task);
            }
        } catch (Exception e) {
            Log.e("AdminAdoptDetail", "Lỗi thêm lịch phỏng vấn: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM XỬ LÝ CHUYỂN ĐỔI TRẠNG THÁI THÚ CƯNG TRÊN FIREBASE
    // =========================================================================
    private void updatePetStatusInDb(String pid, String appointmentStatus) {
        if (pid == null || pid.isEmpty()) return;
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(pid);

        String newPetStatus = "";

        // Map logic trạng thái Đơn nhận nuôi -> Trạng thái Thú cưng
        if ("Interview".equals(appointmentStatus)) {
            newPetStatus = "Đang chờ phỏng vấn";
        } else if ("Approved".equals(appointmentStatus)) {
            newPetStatus = "Đã được nhận nuôi"; // Khi duyệt xong, bé đã có chủ
        } else if ("Rejected".equals(appointmentStatus)) {
            newPetStatus = "Sẵn sàng nhận nuôi"; // Trả bé lại danh sách nếu từ chối
        }

        if (!newPetStatus.isEmpty()) {
            final String finalStatus = newPetStatus;
            petRef.child("status").setValue(newPetStatus).addOnSuccessListener(aVoid -> {
                // Hiển thị thông báo nhỏ để Admin biết thú cưng cũng đã được cập nhật thành công
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Trạng thái thú cưng đã chuyển thành: " + finalStatus, Toast.LENGTH_LONG).show();
                }
            }).addOnFailureListener(e -> {
                Log.e("PetStatusUpdate", "Lỗi khi đổi trạng thái thú cưng: " + e.getMessage());
            });
        }
    }

    private void writeToAppointmentsNode() {
        if (currentApp == null || appointmentId == null) return;

        DatabaseReference aptRef = FirebaseDatabase.getInstance()
                .getReference("Appointments").child(appointmentId);

        java.util.Map<String, Object> aptData = new java.util.HashMap<>();
        aptData.put("adopterName",  currentApp.getAdopterName());
        aptData.put("userPhone", currentApp.getAdopterPhone());
        aptData.put("userEmail", currentApp.getAdopterEmail());
        aptData.put("userId",    currentApp.getUserId());
        aptData.put("petId",     currentApp.getPetId());
        aptData.put("date",      currentApp.getDate());
        aptData.put("time",  currentApp.getTime());
        aptData.put("status",    "Interview");
        aptData.put("adoptionApplicationId", appointmentId);

        aptRef.updateChildren(aptData);
    }

    private void sendNotificationToUser(String userId, String status) {
        if (userId == null || userId.isEmpty()) return;

        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(userId);
        String notifId = notifRef.push().getKey();

        String title = "";
        String message = "";
        String type = "adopt";

        String pName = (petName != null && !petName.isEmpty()) ? petName : (currentApp != null ? currentApp.getPetId() : "");

        switch (status) {
            case "Interview":
                title = "Lịch hẹn đã được xác nhận!";
                message = "Lịch hẹn phỏng vấn nhận nuôi bé " + pName + " đã được xác nhận.";
                break;
            case "Approved":
                title = "Đơn nhận nuôi đã được duyệt!";
                message = "Chúc mừng! Đơn đăng ký nhận nuôi của bạn đã được duyệt thành công.";
                break;
            case "Rejected":
                title = "Thông báo về đơn nhận nuôi";
                message = "Rất tiếc, đơn đăng ký nhận nuôi của bạn không được duyệt lần này.";
                break;
        }

        if (notifId != null && !title.isEmpty()) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("id", notifId);
            notifData.put("title", title);
            notifData.put("message", message);
            notifData.put("timestamp", System.currentTimeMillis());
            notifData.put("isRead", false);
            notifData.put("type", type);

            notifRef.child(notifId).setValue(notifData);
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