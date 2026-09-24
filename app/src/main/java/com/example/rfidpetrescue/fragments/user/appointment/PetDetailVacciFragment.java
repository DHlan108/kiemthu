package com.example.rfidpetrescue.fragments.user.appointment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.LoginActivity;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.VaccinationAdapter;
import com.example.rfidpetrescue.fragments.user.chat.ChatDetailFragment;
import com.example.rfidpetrescue.models.ChatMessage;
import com.example.rfidpetrescue.models.Pet;
import com.example.rfidpetrescue.models.Vaccination;
import com.example.rfidpetrescue.utils.BarcodeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PetDetailVacciFragment extends Fragment {

    private ImageButton btnBack, btnHeart;
    private ImageView imgHeader, ivBarcode;
    private TextView tvPetNameTitle, tvRfid;
    private RecyclerView rvVaccinations;

    // Tab views
    private TextView tvTabInfo, tvTabMedicalRecord, tvTabVaccination;

    // Bottom Actions
    private MaterialCardView btnCall, btnMessage;
    private MaterialButton btnMeet;

    private String petId;
    private String petImageUrl;
    private VaccinationAdapter adapter;
    private final List<Vaccination> vaccinationList = new ArrayList<>();
    
    // Đã thêm: Firebase sync cho yêu thích
    private boolean isFavorite = false;
    private DatabaseReference favoriteRef;

    private Pet currentPet;
    private final String ADMIN_ID = "TYmgr0bzspYSWB9LJQQ2rreht6j1";
    private final String SUPPORT_PHONE = "098 801 5445";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pet_info_vacci, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        initViews(view);
        setupRecyclerView();

        if (getArguments() != null) {
            petId = getArguments().getString("PET_ID");
            petImageUrl = getArguments().getString("PET_IMAGE_URL");

            if (petImageUrl != null && !petImageUrl.isEmpty() && imgHeader != null) {
                Glide.with(this)
                        .load(petImageUrl)
                        .placeholder(R.drawable.ic_dog_manage)
                        .centerCrop()
                        .into(imgHeader);
            }
            if (petId != null) {
                loadPetBasicInfo(petId);
                loadVaccinationRecords(petId);

                // Đồng bộ: Khởi tạo trạng thái Yêu thích từ Firebase
                String uid = FirebaseAuth.getInstance().getUid();
                if (uid != null) {
                    favoriteRef = FirebaseDatabase.getInstance().getReference("Users")
                            .child(uid).child("Favorites").child(petId);
                    checkFavoriteStatus();
                }
            }
        }

        setupClickListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        btnHeart = view.findViewById(R.id.btnHeart);
        imgHeader = view.findViewById(R.id.imgHeader);
        ivBarcode = view.findViewById(R.id.ivBarcode);
        tvRfid = view.findViewById(R.id.tvRfid);
        tvPetNameTitle = view.findViewById(R.id.tvPetNameTitle);
        rvVaccinations = view.findViewById(R.id.rvVaccinations);

        tvTabInfo = view.findViewById(R.id.tvTabInfo);
        tvTabMedicalRecord = view.findViewById(R.id.tvTabMedicalRecord);
        tvTabVaccination = view.findViewById(R.id.tvTabVaccination);

        btnCall = view.findViewById(R.id.btnCall);
        btnMessage = view.findViewById(R.id.btnMessage);
        btnMeet = view.findViewById(R.id.btnMeet);
    }

    private void setupRecyclerView() {
        // CẬP NHẬT: Truyền true để kích hoạt User Mode (đổi màu hồng/cyan)
        adapter = new VaccinationAdapter(vaccinationList, requireContext(), true);
        rvVaccinations.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvVaccinations.setAdapter(adapter);

        adapter.setOnItemClickListener(new VaccinationAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Vaccination vaccination) {
                openVaccinationDetail(vaccination);
            }

            @Override
            public void onEditClick(Vaccination vaccination) {
                Toast.makeText(requireContext(), "Bạn không có quyền chỉnh sửa!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDeleteClick(Vaccination vaccination) {
                Toast.makeText(requireContext(), "Bạn không có quyền xóa!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
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

        // --- Tabs ---
        if (tvTabInfo != null) {
            tvTabInfo.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        if (tvTabMedicalRecord != null) {
            tvTabMedicalRecord.setOnClickListener(v -> {
                if (petId != null) {
                    PetDetailMedicalFragment medFragment = new PetDetailMedicalFragment();
                    Bundle args = new Bundle();
                    args.putString("PET_ID", petId);
                    args.putString("PET_IMAGE_URL", petImageUrl);
                    medFragment.setArguments(args);

                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.main_container, medFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        // --- Bottom Actions ---
        if (btnCall != null) {
            btnCall.setOnClickListener(v -> {
                if (!checkLogin()) return;
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + SUPPORT_PHONE));
                startActivity(intent);
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
    }

    private void openVaccinationDetail(Vaccination vaccination) {
        Toast.makeText(getContext(), "Chi tiết tiêm: " + vaccination.vaccine_type, Toast.LENGTH_SHORT).show();
    }

    private void loadPetBasicInfo(String id) {
        DatabaseReference petRef = FirebaseDatabase.getInstance().getReference("Pets").child(id);
        petRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                if (snapshot.exists()) {
                    currentPet = snapshot.getValue(Pet.class);
                    if (currentPet != null) currentPet.setId(snapshot.getKey());

                    String rfid = snapshot.child("rfid_tag_id").getValue(String.class);
                    String name = snapshot.child("name").getValue(String.class);
                    if (tvPetNameTitle != null) tvPetNameTitle.setText(name != null ? name : "Chưa có tên");

                    if (rfid != null) {
                        if (rfid.startsWith("tag_")) rfid = rfid.replace("tag_", "");
                        Bitmap barcode = BarcodeUtils.generateBarcode(rfid);
                        if (barcode != null && ivBarcode != null) ivBarcode.setImageBitmap(barcode);
                        if (tvRfid != null) tvRfid.setText(rfid);
                    } else {
                        if (tvRfid != null) tvRfid.setText("Chưa có mã");
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadVaccinationRecords(String petId) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Vaccinations");
        ref.orderByChild("pet_id").equalTo(petId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                vaccinationList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Vaccination v = data.getValue(Vaccination.class);
                    if (v != null) {
                        v.id = data.getKey();
                        vaccinationList.add(v);
                    }
                }
                Collections.sort(vaccinationList, (v1, v2) -> {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        return sdf.parse(v2.vaccine_date).compareTo(sdf.parse(v1.vaccine_date));
                    } catch (Exception e) { return 0; }
                });
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void checkFavoriteStatus() {
        if (favoriteRef == null) return;
        favoriteRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                if (btnHeart != null) {
                    btnHeart.setImageResource(isFavorite ? R.drawable.ic_heart_active : R.drawable.ic_heart_unactive);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

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
                    roomData.put("participants", new HashMap<String, Boolean>() {{ put(senderId, true); put(receiverId, true); }});
                    roomData.put("pet_context", petCtx);
                    roomData.put("last_message", "[Thẻ thú cưng: " + currentPet.getName() + "]");
                    roomData.put("last_message_timestamp", System.currentTimeMillis());
                    chatRoomRef.setValue(roomData).addOnCompleteListener(task -> {
                        sendPetCardMessage(roomId, senderId);
                        navigateToChat(roomId);
                    });
                } else {
                    chatRoomRef.child("pet_context").setValue(petCtx);
                    sendPetCardMessage(roomId, senderId);
                    navigateToChat(roomId);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void sendPetCardMessage(String roomId, String senderId) {
        DatabaseReference messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(roomId);
        ChatMessage message = new ChatMessage(senderId, currentPet.getId(), currentPet.getName(),
                currentPet.getBreed(), currentPet.getImageUrl(), System.currentTimeMillis());
        message.setText("Tôi đang xem lịch trình tiêm chủng của bé này");
        message.setType(ChatMessage.TYPE_PET_CARD);
        messagesRef.push().setValue(message);
    }

    private void navigateToChat(String roomId) {
        ChatDetailFragment fragment = new ChatDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putString("ROOM_ID", roomId);
        fragment.setArguments(bundle);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private boolean checkLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
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