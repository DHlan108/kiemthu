package com.example.rfidpetrescue.fragments.user.chat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.MessageAdapter;
import com.example.rfidpetrescue.models.ChatMessage;
import com.example.rfidpetrescue.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatDetailFragment extends Fragment {
    private String roomId;
    private RecyclerView rvMessages;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> messageList;
    private EditText etMessage;
    private ImageView btnSend, btnCamera, btnGallery, btnCall, imgAvatarHeader;
    private ImageButton btnBack;
    private TextView tvChatName, tvStatus;

    private View layoutPetCard;
    private ImageView imgPet, imgPetGender, btnFavorite;
    private TextView tvPetName, tvPetBreed, tvPetAge, tvPetLocation;

    private DatabaseReference chatRoomRef, messagesRef, favoriteRef;
    private String currentUserId;
    private String receiverId = null;
    private String receiverPhone = null;
    private String currentPetId = null;
    private boolean isFavorite = false;

    private ValueEventListener messagesListener, favoriteListener;

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int CAPTURE_IMAGE_REQUEST = 102;
    private static final int CAMERA_PERMISSION_CODE = 201;
    private static final int STORAGE_PERMISSION_CODE = 202;

    private Uri photoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_screen, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) roomId = getArguments().getString("ROOM_ID");
        if (roomId == null) return;

        initViews(view);
        setupFirebase();
        loadParticipants();
        loadPetContext();
        loadMessages();

        btnSend.setOnClickListener(v -> sendMessage(null));
        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnGallery.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnCamera.setOnClickListener(v -> checkPermissionAndOpenCamera());
        btnCall.setOnClickListener(v -> makeCall());

        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> toggleFavorite());
        }

        clearUnreadCount();
    }

    private void clearUnreadCount() {
        if (roomId != null && currentUserId != null) {
            FirebaseDatabase.getInstance().getReference("Chat_Rooms")
                    .child(roomId).child("unread_count").child(currentUserId).setValue(0);
        }
    }

    private void initViews(View v) {
        rvMessages = v.findViewById(R.id.rvMessages);
        etMessage = v.findViewById(R.id.etMessage);
        btnSend = v.findViewById(R.id.btnSend);
        btnBack = v.findViewById(R.id.btnBack);
        btnCamera = v.findViewById(R.id.btnCamera);
        btnGallery = v.findViewById(R.id.btnGallery);
        btnCall = v.findViewById(R.id.btnCall);
        tvChatName = v.findViewById(R.id.tvChatName);
        tvStatus = v.findViewById(R.id.tvStatus);
        imgAvatarHeader = v.findViewById(R.id.imgAvatarHeader);

        layoutPetCard = v.findViewById(R.id.layoutPetCard);
        if (layoutPetCard != null) {
            imgPet = layoutPetCard.findViewById(R.id.imgPet);
            imgPetGender = layoutPetCard.findViewById(R.id.imgPetGender);
            btnFavorite = layoutPetCard.findViewById(R.id.btnFavorite);
            tvPetName = layoutPetCard.findViewById(R.id.tvPetName);
            tvPetBreed = layoutPetCard.findViewById(R.id.tvPetBreed);
            tvPetAge = layoutPetCard.findViewById(R.id.tvPetAge);
            tvPetLocation = layoutPetCard.findViewById(R.id.tvPetLocation);
        }

        messageList = new ArrayList<>();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        messageAdapter = new MessageAdapter(messageList, currentUserId, false);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void checkPermissionAndOpenGallery() {
        String permission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, STORAGE_PERMISSION_CODE);
        } else {
            openGallery();
        }
    }

    private void checkPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void makeCall() {
        if (receiverPhone != null && !receiverPhone.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + receiverPhone));
            startActivity(intent);
        } else {
            Toast.makeText(getContext(), "Chưa có thông tin liên lạc...", Toast.LENGTH_SHORT).show();
            if (receiverId != null) fetchReceiverInfo(receiverId);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == STORAGE_PERMISSION_CODE) openGallery();
            else if (requestCode == CAMERA_PERMISSION_CODE) openCamera();
        } else {
            Toast.makeText(getContext(), "Vui lòng cấp quyền để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            try {
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Không tìm thấy ứng dụng máy ảnh", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null) {
                uploadImageToCloudinary(data.getData());
            } else if (requestCode == CAPTURE_IMAGE_REQUEST) {
                uploadImageToCloudinary(photoUri);
            }
        }
    }

    private void uploadImageToCloudinary(Uri imageUri) {
        if (imageUri == null) return;
        Toast.makeText(getContext(), "Đang gửi ảnh...", Toast.LENGTH_SHORT).show();
        MediaManager.get().upload(imageUri)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}
                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}
                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        sendMessage(imageUrl);
                    }
                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Lỗi tải ảnh: " + error.getDescription(), Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                }).dispatch();
    }

    private void setupFirebase() {
        chatRoomRef = FirebaseDatabase.getInstance().getReference("Chat_Rooms").child(roomId);
        messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(roomId);
        favoriteRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("Favorites");
    }

    private void loadParticipants() {
        chatRoomRef.child("participants").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String participantId = child.getKey();
                    if (participantId != null && !participantId.equals(currentUserId)) {
                        receiverId = participantId;
                        fetchReceiverInfo(receiverId);
                        break;
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchReceiverInfo(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (isAdded()) {
                            String name = "Người dùng";
                            String avatarUrl = "";
                            String status = "offline";

                            if (snapshot.exists()) {
                                User user = snapshot.getValue(User.class);
                                if (user != null) {
                                    receiverPhone = user.getPhone();
                                    name = (user.getName() != null && !user.getName().isEmpty()) ? user.getName() : "Người dùng";
                                    avatarUrl = user.getAvatar();
                                    status = user.getStatus();
                                }
                            }

                            if (tvChatName != null) tvChatName.setText(name);

                            if (tvStatus != null) {
                                if ("online".equals(status)) {
                                    tvStatus.setText("● Hoạt động");
                                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                                } else {
                                    tvStatus.setText("● Ngoại tuyến");
                                    tvStatus.setTextColor(Color.parseColor("#888888"));
                                }
                            }

                            if (imgAvatarHeader != null) {
                                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                    Glide.with(ChatDetailFragment.this)
                                            .load(avatarUrl)
                                            .placeholder(R.drawable.ic_avatar_unactive)
                                            .circleCrop()
                                            .into(imgAvatarHeader);
                                } else {
                                    imgAvatarHeader.setImageResource(R.drawable.ic_avatar_unactive);
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadPetContext() {
        chatRoomRef.child("pet_context").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String petId = snapshot.child("pet_id").getValue(String.class);
                    if (petId != null && !petId.isEmpty()) {
                        currentPetId = petId;
                        fetchPetDetailsFromDB(petId);
                        checkFavoriteStatus(petId);
                    } else {
                        displayCachedPetInfo(snapshot);
                    }
                } else {
                    if (layoutPetCard != null) layoutPetCard.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchPetDetailsFromDB(String petId) {
        FirebaseDatabase.getInstance().getReference("Pets").child(petId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded() && layoutPetCard != null) {
                            layoutPetCard.setVisibility(View.VISIBLE);
                            String name = snapshot.child("name").getValue(String.class);
                            String breed = snapshot.child("breed").getValue(String.class);
                            String age = snapshot.child("age").getValue(String.class);
                            String location = snapshot.child("living_environment").getValue(String.class);
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);
                            String gender = snapshot.child("gender").getValue(String.class);

                            if (tvPetName != null) tvPetName.setText(name);
                            if (tvPetBreed != null) tvPetBreed.setText(breed);
                            if (tvPetAge != null) tvPetAge.setText(age);
                            if (tvPetLocation != null) tvPetLocation.setText(location != null ? location : "Trạm cứu hộ");

                            if (imgPet != null) {
                                Glide.with(ChatDetailFragment.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.img_happy)
                                        .into(imgPet);
                            }

                            if (imgPetGender != null) {
                                if ("Đực".equalsIgnoreCase(gender) || "Male".equalsIgnoreCase(gender)) {
                                    imgPetGender.setImageResource(R.drawable.ic_male);
                                    imgPetGender.setColorFilter(Color.parseColor("#2196F3"));
                                } else {
                                    imgPetGender.setImageResource(R.drawable.ic_female);
                                    imgPetGender.setColorFilter(Color.parseColor("#FF6D8A"));
                                }
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void displayCachedPetInfo(DataSnapshot snapshot) {
        if (layoutPetCard == null) return;
        layoutPetCard.setVisibility(View.VISIBLE);
        String name = snapshot.child("name").getValue(String.class);
        String breed = snapshot.child("breed").getValue(String.class);
        String age = snapshot.child("age").getValue(String.class);
        String imageUrl = snapshot.child("imageUrl").getValue(String.class);
        String gender = snapshot.child("gender").getValue(String.class);
        String petId = snapshot.child("pet_id").getValue(String.class);

        if (petId != null) {
            currentPetId = petId;
            checkFavoriteStatus(petId);
        }

        if (tvPetName != null) tvPetName.setText(name);
        if (tvPetBreed != null) tvPetBreed.setText(breed);
        if (tvPetAge != null) tvPetAge.setText(age);
        if (tvPetLocation != null) tvPetLocation.setText("Đang cập nhật");

        if (imgPet != null) {
            Glide.with(ChatDetailFragment.this).load(imageUrl).into(imgPet);
        }

        if (imgPetGender != null) {
            if ("Đực".equalsIgnoreCase(gender) || "Male".equalsIgnoreCase(gender)) {
                imgPetGender.setImageResource(R.drawable.ic_male);
                imgPetGender.setColorFilter(Color.parseColor("#2196F3"));
            } else {
                imgPetGender.setImageResource(R.drawable.ic_female);
                imgPetGender.setColorFilter(Color.parseColor("#FF6D8A"));
            }
        }
    }

    private void checkFavoriteStatus(String petId) {
        if (favoriteListener != null) {
            favoriteRef.child(petId).removeEventListener(favoriteListener);
        }
        favoriteListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isFavorite = snapshot.exists();
                if (btnFavorite != null) {
                    btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_active : R.drawable.ic_heart_unactive);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        favoriteRef.child(petId).addValueEventListener(favoriteListener);
    }

    private void toggleFavorite() {
        if (currentPetId == null) return;
        if (isFavorite) {
            favoriteRef.child(currentPetId).removeValue();
            Toast.makeText(getContext(), "Đã bỏ yêu thích", Toast.LENGTH_SHORT).show();
        } else {
            favoriteRef.child(currentPetId).setValue(true);
            Toast.makeText(getContext(), "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messageList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage msg = data.getValue(ChatMessage.class);
                    if (msg != null) messageList.add(msg);
                }
                messageAdapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    rvMessages.scrollToPosition(messageList.size() - 1);
                }
                clearUnreadCount();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        messagesRef.addValueEventListener(messagesListener);
    }

    private void sendMessage(String imageUrl) {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty() && imageUrl == null) return;

        long timestamp = System.currentTimeMillis();
        ChatMessage message;
        if (imageUrl != null) {
            message = new ChatMessage(currentUserId, "[Hình ảnh]", timestamp, imageUrl);
        } else {
            message = new ChatMessage(currentUserId, content, timestamp);
        }

        messagesRef.push().setValue(message);
        etMessage.setText("");

        if (chatRoomRef != null) {
            chatRoomRef.child("last_message").setValue(imageUrl != null ? "[Hình ảnh]" : content);
            chatRoomRef.child("last_message_timestamp").setValue(timestamp);

            if (receiverId != null) {
                chatRoomRef.child("unread_count").child(receiverId).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Integer count = currentData.getValue(Integer.class);
                        if (count == null) currentData.setValue(1);
                        else currentData.setValue(count + 1);
                        return Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {}
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
        clearUnreadCount();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
        if (favoriteRef != null && currentPetId != null && favoriteListener != null) {
            favoriteRef.child(currentPetId).removeEventListener(favoriteListener);
        }
    }
}
