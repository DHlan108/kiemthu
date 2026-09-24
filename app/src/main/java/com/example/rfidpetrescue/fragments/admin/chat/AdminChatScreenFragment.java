package com.example.rfidpetrescue.fragments.admin.chat;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.MessageAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminChatScreenBinding;
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

public class AdminChatScreenFragment extends Fragment {

    private FragmentAdminChatScreenBinding binding;
    private String roomId;
    private String currentUserId;
    private String otherUserId = null;
    private DatabaseReference chatRoomRef, messagesRef;
    private MessageAdapter messageAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();

    private static final int PICK_IMAGE_REQUEST = 101;
    private static final int CAPTURE_IMAGE_REQUEST = 102;
    private static final int CAMERA_PERMISSION_CODE = 201;
    private static final int STORAGE_PERMISSION_CODE = 202;

    private Uri photoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminChatScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            roomId = getArguments().getString("ROOM_ID");
        }

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (roomId != null) {
            chatRoomRef = FirebaseDatabase.getInstance().getReference("Chat_Rooms").child(roomId);
            messagesRef = FirebaseDatabase.getInstance().getReference("Messages").child(roomId);

            setupRecyclerView();
            loadMessages();
            loadUserInfo();
            loadPetContext();
            
            // Xóa thông báo chưa đọc khi Admin vào xem
            clearUnreadCount();
        }

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnSent.setOnClickListener(v -> sendMessage(null));
        binding.btnGallery.setOnClickListener(v -> checkPermissionAndOpenGallery());
        binding.btnCamera.setOnClickListener(v -> checkPermissionAndOpenCamera());
    }

    private void clearUnreadCount() {
        if (roomId != null && currentUserId != null) {
            FirebaseDatabase.getInstance().getReference("Chat_Rooms")
                    .child(roomId).child("unread_count").child(currentUserId).setValue(0);
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList, currentUserId, true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(messageAdapter);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == STORAGE_PERMISSION_CODE) openGallery();
            else if (requestCode == CAMERA_PERMISSION_CODE) openCamera();
        } else {
            Toast.makeText(getContext(), "Vui lòng cấp quyền để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadMessages() {
        if (messagesRef == null) return;

        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;

                messageList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ChatMessage msg = ds.getValue(ChatMessage.class);
                    if (msg != null) {
                        messageList.add(msg);
                    }
                }

                if (messageAdapter != null) {
                    messageAdapter.notifyDataSetChanged();
                    if (!messageList.isEmpty()) {
                        binding.rvMessages.scrollToPosition(messageList.size() - 1);
                    }
                }
                // Xóa unread khi nhận tin mới mà đang xem màn hình
                clearUnreadCount();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadUserInfo() {
        if (chatRoomRef == null) return;
        chatRoomRef.child("participants").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (!ds.getKey().equals(currentUserId)) {
                        otherUserId = ds.getKey();
                        break;
                    }
                }
                if (otherUserId != null) {
                    FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
                            .addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    if (snapshot.exists() && isAdded() && binding != null) {
                                        User user = snapshot.getValue(User.class);
                                        if (user != null) {
                                            binding.tvChatName.setText(user.getName() != null ? user.getName() : "Khách hàng");
                                            String avatarUrl = user.getAvatar();
                                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                                Glide.with(AdminChatScreenFragment.this)
                                                        .load(avatarUrl)
                                                        .placeholder(R.drawable.ic_avatar_unactive)
                                                        .circleCrop()
                                                        .into(binding.imgAvatarHeader);
                                            } else {
                                                binding.imgAvatarHeader.setImageResource(R.drawable.ic_avatar_unactive);
                                            }

                                            // Đồng bộ trạng thái hoạt động
                                            if ("online".equals(user.getStatus())) {
                                                binding.tvStatus.setText("● Hoạt động");
                                                binding.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                                            } else {
                                                binding.tvStatus.setText("● Ngoại tuyến");
                                                binding.tvStatus.setTextColor(Color.parseColor("#888888"));
                                            }
                                        }
                                    }
                                }
                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {}
                            });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPetContext() {
        if (chatRoomRef == null) return;
        chatRoomRef.child("pet_context").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded() && binding != null) {
                    String petId = snapshot.child("pet_id").getValue(String.class);
                    if (petId != null && !petId.isEmpty()) {
                        fetchPetDetailsFromDB(petId);
                    } else {
                        displayCachedPetInfo(snapshot);
                    }
                } else if (binding != null) {
                    binding.layoutPetCard.setVisibility(View.GONE);
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
                        if (snapshot.exists() && isAdded() && binding != null) {
                            binding.layoutPetCard.setVisibility(View.VISIBLE);
                            String name = snapshot.child("name").getValue(String.class);
                            String breed = snapshot.child("breed").getValue(String.class);
                            String age = snapshot.child("age").getValue(String.class);
                            String location = snapshot.child("living_environment").getValue(String.class);
                            String imageUrl = snapshot.child("imageUrl").getValue(String.class);

                            binding.tvPetName.setText(name != null ? name : "Thú cưng");
                            binding.tvPetBreed.setText(breed != null ? breed : "Đang cập nhật");
                            binding.tvPetAge.setText(age != null ? age : "Đang cập nhật");
                            binding.tvPetLocation.setText(location != null ? location : "Trạm cứu hộ");

                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(AdminChatScreenFragment.this)
                                        .load(imageUrl)
                                        .placeholder(R.drawable.ic_dog_manage)
                                        .into(binding.imgPet);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void displayCachedPetInfo(DataSnapshot snapshot) {
        if (binding == null) return;
        binding.layoutPetCard.setVisibility(View.VISIBLE);
        String name = snapshot.child("name").getValue(String.class);
        String breed = snapshot.child("breed").getValue(String.class);
        String age = snapshot.child("age").getValue(String.class);
        String imageUrl = snapshot.child("imageUrl").getValue(String.class);

        binding.tvPetName.setText(name != null ? name : "Thú cưng");
        binding.tvPetBreed.setText(breed != null ? breed : "Đang cập nhật");
        binding.tvPetAge.setText(age != null ? age : "Đang cập nhật");
        binding.tvPetLocation.setText("Đang cập nhật");

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(AdminChatScreenFragment.this).load(imageUrl).into(binding.imgPet);
        }
    }

    private void sendMessage(String imageUrl) {
        String content = binding.etMessage.getText().toString().trim();
        if (content.isEmpty() && imageUrl == null) return;

        long timestamp = System.currentTimeMillis();
        ChatMessage message;
        if (imageUrl != null) {
            message = new ChatMessage(currentUserId, "[Hình ảnh]", timestamp, imageUrl);
        } else {
            message = new ChatMessage(currentUserId, content, timestamp);
        }

        messagesRef.push().setValue(message);
        binding.etMessage.setText("");

        if (chatRoomRef != null) {
            chatRoomRef.child("last_message").setValue(imageUrl != null ? "[Hình ảnh]" : content);
            chatRoomRef.child("last_message_timestamp").setValue(timestamp);

            // Tăng unread_count cho người nhận (User)
            if (otherUserId != null) {
                chatRoomRef.child("unread_count").child(otherUserId).runTransaction(new Transaction.Handler() {
                    @NonNull
                    @Override
                    public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                        Integer count = currentData.getValue(Integer.class);
                        if (count == null) {
                            currentData.setValue(1);
                        } else {
                            currentData.setValue(count + 1);
                        }
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
