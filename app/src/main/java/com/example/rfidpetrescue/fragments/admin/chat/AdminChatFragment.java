package com.example.rfidpetrescue.fragments.admin.chat;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.ChatAdapter;
import com.example.rfidpetrescue.models.ChatChannel;
import com.example.rfidpetrescue.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminChatFragment extends Fragment {

    private RecyclerView rvAdminChats;
    private ChatAdapter chatAdapter;
    private final List<ChatChannel> chatList = new ArrayList<>();
    private final List<ChatChannel> filteredList = new ArrayList<>();

    private EditText etSearch;
    private DatabaseReference chatRoomsRef;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAdminChats = view.findViewById(R.id.rvAdminChats);
        etSearch = view.findViewById(R.id.etSearch);

        currentUserId = FirebaseAuth.getInstance().getUid();
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("Chat_Rooms");

        setupRecyclerView();
        loadChatRooms();
        setupSearch();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(filteredList, getContext(), true, chatChannel -> {
            if (currentUserId != null && chatChannel.getRoomId() != null) {
                FirebaseDatabase.getInstance().getReference("Chat_Rooms")
                        .child(chatChannel.getRoomId())
                        .child("unread_count")
                        .child(currentUserId)
                        .setValue(0);
            }

            AdminChatScreenFragment detailFragment = new AdminChatScreenFragment();
            Bundle bundle = new Bundle();
            bundle.putString("ROOM_ID", chatChannel.getRoomId());
            detailFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        chatAdapter.setOnChatLongClickListener(chatChannel -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Xóa cuộc trò chuyện")
                    .setMessage("Bạn có chắc chắn muốn xóa cuộc trò chuyện với " + chatChannel.getName() + "?")
                    .setPositiveButton("Xóa", (dialog, which) -> deleteChatRoom(chatChannel.getRoomId()))
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        rvAdminChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAdminChats.setAdapter(chatAdapter);
    }

    private void deleteChatRoom(String roomId) {
        if (roomId == null) return;
        chatRoomsRef.child(roomId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseDatabase.getInstance().getReference("Messages").child(roomId).removeValue();
                Toast.makeText(getContext(), "Đã xóa cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            } else {
                String error = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(getContext(), "Lỗi khi xóa: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadChatRooms() {
        if (currentUserId == null) return;

        chatRoomsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<DataSnapshot> roomSnaps = new ArrayList<>();
                for (DataSnapshot roomSnap : snapshot.getChildren()) {
                    if (roomSnap.child("participants").child(currentUserId).exists()) {
                        roomSnaps.add(roomSnap);
                    }
                }

                roomSnaps.sort((a, b) -> {
                    Long t1 = a.child("last_message_timestamp").getValue(Long.class);
                    Long t2 = b.child("last_message_timestamp").getValue(Long.class);
                    if (t1 == null) t1 = 0L;
                    if (t2 == null) t2 = 0L;
                    return t2.compareTo(t1);
                });

                if (roomSnaps.isEmpty()) {
                    chatList.clear();
                    updateFilteredList(etSearch.getText().toString());
                    return;
                }

                final ChatChannel[] tempArray = new ChatChannel[roomSnaps.size()];
                final int[] loadedCount = {0};

                for (int i = 0; i < roomSnaps.size(); i++) {
                    final int index = i;
                    DataSnapshot roomSnap = roomSnaps.get(i);
                    String roomId = roomSnap.getKey();
                    String lastMessage = roomSnap.child("last_message").getValue(String.class);
                    Long timestamp = roomSnap.child("last_message_timestamp").getValue(Long.class);
                    
                    Object unreadVal = roomSnap.child("unread_count").child(currentUserId).getValue();
                    final boolean isUnreadFinal = (unreadVal != null && 
                            ((unreadVal instanceof Long && (Long) unreadVal > 0) || 
                             (unreadVal instanceof Integer && (Integer) unreadVal > 0)));
                    
                    String timeFormatted = formatTime(timestamp);

                    String otherUserId = "";
                    for (DataSnapshot participant : roomSnap.child("participants").getChildren()) {
                        String key = participant.getKey();
                        if (key != null && !key.equals(currentUserId)) {
                            otherUserId = key;
                            break;
                        }
                    }

                    if (!otherUserId.isEmpty()) {
                        FirebaseDatabase.getInstance().getReference("Users").child(otherUserId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                                        User user = userSnap.getValue(User.class);
                                        String chatName = "Khách hàng";
                                        String avatarUrl = "https://cdn-icons-png.flaticon.com/512/149/149071.png";

                                        if (user != null) {
                                            if (user.getName() != null && !user.getName().isEmpty()) {
                                                chatName = user.getName();
                                            }
                                            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                                                avatarUrl = user.getAvatar();
                                            }
                                        }

                                        tempArray[index] = new ChatChannel(roomId, chatName,
                                                lastMessage != null ? lastMessage : "",
                                                timeFormatted,
                                                avatarUrl,
                                                isUnreadFinal);

                                        checkAndRefreshList(tempArray, loadedCount, roomSnaps.size());
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        checkAndRefreshList(tempArray, loadedCount, roomSnaps.size());
                                    }
                                });
                    } else {
                        checkAndRefreshList(tempArray, loadedCount, roomSnaps.size());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private synchronized void checkAndRefreshList(ChatChannel[] tempArray, int[] loadedCount, int totalSize) {
        loadedCount[0]++;
        if (loadedCount[0] == totalSize) {
            chatList.clear();
            for (ChatChannel c : tempArray) {
                if (c != null) {
                    chatList.add(c);
                }
            }
            updateFilteredList(etSearch.getText().toString());
        }
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateFilteredList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateFilteredList(String query) {
        filteredList.clear();
        if (query == null || query.isEmpty()) {
            filteredList.addAll(chatList);
        } else {
            for (ChatChannel channel : chatList) {
                if (channel.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(channel);
                }
            }
        }
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }

    private String formatTime(Long timestamp) {
        if (timestamp == null || timestamp == 0) return "";

        Calendar now = Calendar.getInstance();
        Calendar msgTime = Calendar.getInstance();
        msgTime.setTimeInMillis(timestamp);

        if (now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR)) {
            if (now.get(Calendar.DATE) == msgTime.get(Calendar.DATE) &&
                now.get(Calendar.MONTH) == msgTime.get(Calendar.MONTH)) {
                return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
            } else {
                return new SimpleDateFormat("dd/MM", Locale.getDefault()).format(new Date(timestamp));
            }
        } else {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
