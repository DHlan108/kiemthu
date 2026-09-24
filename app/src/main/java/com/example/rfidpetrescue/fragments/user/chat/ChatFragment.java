package com.example.rfidpetrescue.fragments.user.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.ChatAdapter;
import com.example.rfidpetrescue.fragments.user.notification.NotificationFragment;
import com.example.rfidpetrescue.models.ChatChannel;
import com.example.rfidpetrescue.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatFragment extends Fragment {

    private RecyclerView rvChats;
    private ChatAdapter chatAdapter;
    private final List<ChatChannel> chatList = new ArrayList<>();

    private TextView tvUserName;
    private ImageView imgAvatar;
    private View btnNotification, dotUnread;

    private FirebaseAuth mAuth;
    private DatabaseReference chatRoomsRef, notifRef;
    private ValueEventListener chatRoomsListener, notifListener;
    private String currentUserId;
    private Animation shakeAnim;
    private boolean isNotifScreenOpened = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvUserName = view.findViewById(R.id.tvUserName);
        imgAvatar = view.findViewById(R.id.imgAvatar);
        btnNotification = view.findViewById(R.id.btnNotification);
        dotUnread = view.findViewById(R.id.dotUnread);
        rvChats = view.findViewById(R.id.rvChats);

        shakeAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shake);

        mAuth = FirebaseAuth.getInstance();
        chatRoomsRef = FirebaseDatabase.getInstance().getReference("Chat_Rooms");

        setupRecyclerView();
        setupClickListeners();
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(chatList, getContext(), false, chatChannel -> {
            // CHỈ RESET KHI ẤN VÀO
            if (chatChannel.isUnread() && currentUserId != null) {
                chatRoomsRef.child(chatChannel.getRoomId()).child("unread_count").child(currentUserId).setValue(0);
            }

            ChatDetailFragment detailFragment = new ChatDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putString("ROOM_ID", chatChannel.getRoomId());
            detailFragment.setArguments(bundle);

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, detailFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        rvChats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvChats.setAdapter(chatAdapter);
    }

    private void setupClickListeners() {
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v -> {
                isNotifScreenOpened = true;
                btnNotification.clearAnimation();
                if (dotUnread != null) dotUnread.setVisibility(View.GONE);
                markAllAsRead();

                if (getParentFragmentManager() != null) {
                    getParentFragmentManager().beginTransaction()
                            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                            .replace(R.id.main_container, new NotificationFragment())
                            .addToBackStack(null)
                            .commit();
                }
            });
        }
    }

    private void refreshData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(currentUserId);
            loadUserInfo();
            loadChatRooms();
            checkNewNotifications();
        } else {
            currentUserId = null;
            tvUserName.setText("Xin chào, Khách");
            chatList.clear();
            chatAdapter.notifyDataSetChanged();
        }
    }

    private void loadUserInfo() {
        FirebaseDatabase.getInstance().getReference("Users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && isAdded()) {
                            User u = snapshot.getValue(User.class);
                            if (u != null && u.getName() != null) {
                                String[] parts = u.getName().split(" ");
                                tvUserName.setText("Xin chào, " + parts[parts.length - 1]);
                            }
                            if (u != null && u.getAvatar() != null) {
                                Glide.with(ChatFragment.this).load(u.getAvatar()).circleCrop()
                                        .placeholder(R.drawable.ic_loading).into(imgAvatar);
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadChatRooms() {
        if (currentUserId == null) return;
        
        if (chatRoomsListener != null) {
            chatRoomsRef.removeEventListener(chatRoomsListener);
        }

        chatRoomsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                List<DataSnapshot> roomSnaps = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.child("participants").child(currentUserId).exists()) {
                        roomSnaps.add(ds);
                    }
                }

                roomSnaps.sort((a, b) -> {
                    Long t1 = a.child("last_message_timestamp").getValue(Long.class);
                    Long t2 = b.child("last_message_timestamp").getValue(Long.class);
                    return Long.compare(t2 != null ? t2 : 0, t1 != null ? t1 : 0);
                });

                if (roomSnaps.isEmpty()) {
                    chatList.clear();
                    chatAdapter.notifyDataSetChanged();
                    return;
                }

                final ChatChannel[] temp = new ChatChannel[roomSnaps.size()];
                final int[] loaded = {0};
                final int total = roomSnaps.size();

                for (int i = 0; i < total; i++) {
                    final int idx = i;
                    DataSnapshot rs = roomSnaps.get(i);
                    String rid = rs.getKey();
                    String lastMsg = rs.child("last_message").getValue(String.class);
                    Long ts = rs.child("last_message_timestamp").getValue(Long.class);

                    // Đọc unread_count đồng bộ
                    long count = 0;
                    DataSnapshot uCountSnap = rs.child("unread_count").child(currentUserId);
                    if (uCountSnap.exists() && uCountSnap.getValue() != null) {
                        try {
                            count = ((Number) uCountSnap.getValue()).longValue();
                        } catch (Exception e) {
                            count = 0;
                        }
                    }
                    
                    final boolean isUnread = count > 0;
                    String time = formatTime(ts);
                    
                    String otherUid = "";
                    for (DataSnapshot p : rs.child("participants").getChildren()) {
                        if (!currentUserId.equals(p.getKey())) { otherUid = p.getKey(); break; }
                    }

                    if (!otherUid.isEmpty()) {
                        FirebaseDatabase.getInstance().getReference("Users").child(otherUid)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot uSnap) {
                                        if (!isAdded()) return;
                                        User u = uSnap.getValue(User.class);
                                        temp[idx] = new ChatChannel(rid, u != null ? u.getName() : "Người dùng", 
                                                lastMsg != null ? lastMsg : "", time, u != null ? u.getAvatar() : "", isUnread);
                                        checkRefresh(temp, loaded, total);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) { checkRefresh(temp, loaded, total); }
                                });
                    } else { checkRefresh(temp, loaded, total); }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatRoomsRef.addValueEventListener(chatRoomsListener);
    }

    private synchronized void checkRefresh(ChatChannel[] temp, int[] count, int total) {
        count[0]++;
        if (count[0] == total) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    chatList.clear();
                    for (ChatChannel c : temp) if (c != null) chatList.add(c);
                    chatAdapter.notifyDataSetChanged();
                });
            }
        }
    }

    private void checkNewNotifications() {
        if (notifRef == null) return;
        if (notifListener != null) notifRef.removeEventListener(notifListener);
        
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                boolean hasUnread = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                    if (isRead != null && !isRead) {
                        hasUnread = true;
                        break;
                    }
                }
                if (hasUnread && !isNotifScreenOpened) {
                    if (btnNotification != null) btnNotification.startAnimation(shakeAnim);
                    if (dotUnread != null) dotUnread.setVisibility(View.VISIBLE);
                } else {
                    if (btnNotification != null) btnNotification.clearAnimation();
                    if (dotUnread != null) dotUnread.setVisibility(View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        notifRef.addValueEventListener(notifListener);
    }

    private void markAllAsRead() {
        if (notifRef == null) return;
        notifRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (Boolean.FALSE.equals(ds.child("isRead").getValue(Boolean.class))) {
                        ds.getRef().child("isRead").setValue(true);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String formatTime(Long ts) {
        if (ts == null || ts == 0) return "";
        Calendar n = Calendar.getInstance();
        Calendar m = Calendar.getInstance(); m.setTimeInMillis(ts);
        SimpleDateFormat sdf = (n.get(Calendar.DATE) == m.get(Calendar.DATE)) ? new SimpleDateFormat("HH:mm") : new SimpleDateFormat("dd/MM");
        return sdf.format(new Date(ts));
    }

    @Override
    public void onResume() {
        super.onResume();
        isNotifScreenOpened = false;
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        refreshData();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (chatRoomsRef != null && chatRoomsListener != null) {
            chatRoomsRef.removeEventListener(chatRoomsListener);
        }
        if (notifRef != null && notifListener != null) notifRef.removeEventListener(notifListener);
    }
}
