package com.example.rfidpetrescue.fragments.user.appointment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
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
import com.example.rfidpetrescue.fragments.user.chat.ChatDetailFragment;
import com.example.rfidpetrescue.models.ChatMessage;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PetDetailFragment extends Fragment {

    // ─── Views ────────────────────────────────────────────────────────────────
    private ImageView imgHeader, ivBarcode, ivHeartIcon;
    private MaterialCardView btnBack, btnHeart, btnCall, btnMessage, cardStatusBannerContainer;
    private MaterialButton btnMeet;

    private TextView tvRfid, tvPetNameTitle, tvStatusBanner;
    private TextView tvTabInfo, tvTabMedicalRecord, tvTabVaccination;
    private TextView tvDetailBreed, tvDetailGender, tvDetailAge, tvDetailWeight, tvDetailColor, tvDetailSterilized;
    private TextView tvRescueHistory, tvNote;

    // ─── Firebase & Logic ─────────────────────────────────────────────────────
    private boolean isFavorite = false;
    private DatabaseReference favoriteRef, databaseReference;
    private String petId, petImageUrl;
    private Pet currentPet;

    private final String ADMIN_ID = "TYmgr0bzspYSWB9LJQQ2rreht6j1";
    private final String SUPPORT_PHONE = "098 801 5445";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ẩn thanh menu ngay khi View được tạo
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        initViews(view);
        databaseReference = FirebaseDatabase.getInstance().getReference("Pets");

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");
        }

        if (petId != null) {
            loadPetData(petId);
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid != null) {
                favoriteRef = FirebaseDatabase.getInstance().getReference("Users")
                        .child(uid).child("Favorites").child(petId);
                checkFavoriteStatus();
            }
        }
        setupClickListeners();
    }

    private void initViews(View view) {
        imgHeader = view.findViewById(R.id.imgHeader);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        tvRfid = view.findViewById(R.id.tvRfid);
        tvPetNameTitle = view.findViewById(R.id.tvPetNameTitle);

        btnBack = view.findViewById(R.id.btnBack);
        btnHeart = view.findViewById(R.id.btnHeart);
        // Lấy ImageView bên trong btnHeart để đổi icon
        ivHeartIcon = (ImageView) btnHeart.getChildAt(0);

        tvTabInfo = view.findViewById(R.id.tvTabInfo);
        tvTabMedicalRecord = view.findViewById(R.id.tvTabMedicalRecord);
        tvTabVaccination = view.findViewById(R.id.tvTabVaccination);

        tvStatusBanner = view.findViewById(R.id.tvStatusBanner);
        cardStatusBannerContainer = (MaterialCardView) tvStatusBanner.getParent();

        tvDetailBreed = view.findViewById(R.id.tvDetailBreed);
        tvDetailGender = view.findViewById(R.id.tvDetailGender);
        tvDetailAge = view.findViewById(R.id.tvDetailAge);
        tvDetailWeight = view.findViewById(R.id.tvDetailWeight);
        tvDetailColor = view.findViewById(R.id.tvDetailColor);
        tvDetailSterilized = view.findViewById(R.id.tvDetailSterilized);

        tvRescueHistory = view.findViewById(R.id.tvRescueHistory);
        tvNote = view.findViewById(R.id.tvNote);

        btnCall = view.findViewById(R.id.btnCall);
        btnMessage = view.findViewById(R.id.btnMessage);
        btnMeet = view.findViewById(R.id.btnMeet);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        }

        // --- Logic Đổi Tab ---
        if (tvTabMedicalRecord != null) {
            tvTabMedicalRecord.setOnClickListener(v -> {
                if (petId != null) {
                    PetDetailMedicalFragment medicalFragment = new  PetDetailMedicalFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("PET_ID", petId);
                    bundle.putString("PET_IMAGE_URL", petImageUrl);
                    medicalFragment.setArguments(bundle);

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, medicalFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (tvTabVaccination != null) {
            tvTabVaccination.setOnClickListener(v -> {
                if (petId != null) {
                    PetDetailVacciFragment vacFragment = new PetDetailVacciFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("PET_ID", petId);
                    bundle.putString("PET_IMAGE_URL", petImageUrl);
                    vacFragment.setArguments(bundle);

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.main_container, vacFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // --- Logic Bottom Bar ---
        if (btnMeet != null) {
            btnMeet.setOnClickListener(v -> {
                if (!checkLogin()) return;
                if (petId == null) return;
                AppointmentFragment appointmentFragment = new AppointmentFragment();
                Bundle bundle = new Bundle();
                bundle.putString("PET_ID", petId);
                appointmentFragment.setArguments(bundle);
                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, appointmentFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        if (btnMessage != null) {
            btnMessage.setOnClickListener(v -> {
                if (!checkLogin()) return;
                String currentUserId = FirebaseAuth.getInstance().getUid();
                if (currentUserId == null || currentPet == null) return;
                String roomId = (currentUserId.compareTo(ADMIN_ID) < 0)
                        ? currentUserId + "_" + ADMIN_ID : ADMIN_ID + "_" + currentUserId;
                handleChatRoomSetup(roomId, ADMIN_ID, currentUserId);
            });
        }

        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                if (!checkLogin()) return;
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + SUPPORT_PHONE));
                startActivity(intent);
            });
        }

        if (btnHeart != null) {
            btnHeart.setOnClickListener(v -> {
                if (!checkLogin()) return;
                if (favoriteRef == null || currentPet == null) return;
                if (isFavorite) {
                    favoriteRef.removeValue().addOnSuccessListener(aVoid -> {
                        if (isAdded() && getContext() != null)
                            Toast.makeText(getContext(), "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    favoriteRef.setValue(true).addOnSuccessListener(aVoid -> {
                        if (isAdded() && getContext() != null)
                            Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    // ─── Lấy dữ liệu và Gán vào UI ────────────────────────────────────────────

    private void loadPetData(String id) {
        databaseReference.child(id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    currentPet = snapshot.getValue(Pet.class);
                    if (currentPet != null) {
                        currentPet.setId(snapshot.getKey());
                        petImageUrl = currentPet.getImageUrl();
                        populateUI(currentPet);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateUI(Pet pet) {
        if (!isAdded() || pet == null) return;

        // 1. Ảnh
        if (imgHeader != null && petImageUrl != null && !petImageUrl.isEmpty()) {
            Glide.with(this).load(petImageUrl).centerCrop().placeholder(R.drawable.ic_dog_manage).into(imgHeader);
        }

        // 2. Barcode
        if (pet.getRfid_tag_id() != null && !pet.getRfid_tag_id().isEmpty()) {
            String rfid = pet.getRfid_tag_id();
            if (rfid.startsWith("tag_")) {
                rfid = rfid.replace("tag_", "");
            }

            Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
            if (barcode != null && ivBarcode != null) {
                ivBarcode.setImageBitmap(barcode);
            }
            if (tvRfid != null) tvRfid.setText(rfid);
        } else {
            if (tvRfid != null) tvRfid.setText("Chưa có mã");
        }

        // 3. Thông tin cơ bản
        tvPetNameTitle.setText(pet.getName() != null && !pet.getName().isEmpty() ? pet.getName() : "Chưa cập nhật tên");

        String status = pet.getStatus() != null ? pet.getStatus() : "Chưa rõ";
        tvStatusBanner.setText("Tình trạng: " + status);
        updateStatusColor(status);

        // 4. Bảng thông tin chi tiết
        tvDetailBreed.setText(nullSafe(pet.getBreed()));
        tvDetailGender.setText(nullSafe(pet.getGender()));
        tvDetailAge.setText(nullSafe(pet.getAge()));
        tvDetailWeight.setText(pet.getWeight() + " kg");
        tvDetailColor.setText(nullSafe(pet.getColor()));

        String sterilizedStr = "Chưa rõ";
        if (pet.isSterilized() != null) {
            sterilizedStr = pet.isSterilized() ? "Đã triệt sản" : "Chưa";
        }
        tvDetailSterilized.setText(sterilizedStr);

        // 5. Lịch sử & Ghi chú
        tvRescueHistory.setText(pet.getRescueHistory() != null && !pet.getRescueHistory().isEmpty() ? pet.getRescueHistory() : "Chưa có thông tin cứu hộ");
        tvNote.setText(pet.getNote() != null && !pet.getNote().isEmpty() ? pet.getNote() : "Không có ghi chú");
    }

    private void updateStatusColor(String status) {
        if (cardStatusBannerContainer == null || tvStatusBanner == null) return;
        int bgColor;

        // Màu mặc định (cho các trạng thái khác)
        bgColor = Color.parseColor("#19E8DB"); // Xanh ngọc gốc của bạn

        switch (status) {
            case "Đang điều trị":
                bgColor = Color.parseColor("#FF5C8D"); // Màu hồng nếu đang trị bệnh
                break;
            case "Đang chờ nhận nuôi":
                bgColor = Color.parseColor("#627B12");
                break;
            case "Đã nhận nuôi":
                bgColor = Color.parseColor("#B2DF20");
                break;
        }
        cardStatusBannerContainer.setCardBackgroundColor(bgColor);
    }

    private String nullSafe(String value) {
        return (value != null && !value.isEmpty()) ? value : "Chưa rõ";
    }

    private void checkFavoriteStatus() {
        if (favoriteRef == null) return;
        favoriteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                if (ivHeartIcon != null) {
                    ivHeartIcon.setImageResource(isFavorite ? R.drawable.ic_heart_active : R.drawable.ic_heart_unactive);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ─── Logic Chat Firebase ──────────────────────────────────────────────────
    private void handleChatRoomSetup(String roomId, String receiverId, String senderId) {
        DatabaseReference chatRoomRef = FirebaseDatabase.getInstance().getReference("Chat_Rooms").child(roomId);
        Map<String, Object> petCtx = new HashMap<>();
        petCtx.put("pet_id", currentPet.getId());
        petCtx.put("name", currentPet.getName());
        petCtx.put("imageUrl", currentPet.getImageUrl());
        petCtx.put("breed", currentPet.getBreed());
        petCtx.put("age", currentPet.getAge());

        chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Map<String, Object> roomData = new HashMap<>();
                    roomData.put("roomId", roomId);
                    roomData.put("participants", new HashMap<String, Boolean>() {{
                        put(senderId, true);
                        put(receiverId, true);
                    }});
                    roomData.put("pet_context", petCtx);
                    roomData.put("last_message", "[Thẻ thú cưng: " + currentPet.getName() + "]");
                    roomData.put("last_message_timestamp", System.currentTimeMillis());

                    chatRoomRef.setValue(roomData).addOnCompleteListener(task -> {
                        sendPetCardMessage(roomId, senderId, null);
                        navigateToChat(roomId);
                    });
                } else {
                    String oldPetId = snapshot.child("pet_context").child("pet_id").getValue(String.class);
                    String oldPetName = snapshot.child("pet_context").child("name").getValue(String.class);

                    if (oldPetId == null || !oldPetId.equals(currentPet.getId())) {
                        chatRoomRef.child("pet_context").setValue(petCtx);
                        chatRoomRef.child("last_message").setValue("[Thẻ thú cưng: " + currentPet.getName() + "]");
                        chatRoomRef.child("last_message_timestamp").setValue(System.currentTimeMillis());
                        sendPetCardMessage(roomId, senderId, oldPetName);
                    }
                    navigateToChat(roomId);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendPetCardMessage(String roomId, String senderId, String oldPetName) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(roomId);
        ChatMessage message = new ChatMessage(
                senderId, currentPet.getId(), currentPet.getName(),
                currentPet.getBreed(), currentPet.getImageUrl(), System.currentTimeMillis()
        );

        if (oldPetName != null) {
            message.setText("Đã từng hỏi về bé " + oldPetName);
        } else {
            message.setText("Tôi đang quan tâm đến bé này");
        }

        message.setType(ChatMessage.TYPE_PET_CARD);
        messagesRef.push().setValue(message);
    }

    private void navigateToChat(String roomId) {
        if (!isAdded()) return;
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ROOM_ID", roomId);
        fragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private boolean checkLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}