package com.example.rfidpetrescue.fragments.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.adapters.AdminNotificationAdapter;
import com.example.rfidpetrescue.models.Notification;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminNotificationFragment extends Fragment {

    private AdminNotificationAdapter adapter;
    private List<Notification> allNotifList = new ArrayList<>();
    private List<Notification> filteredNotifList = new ArrayList<>();
    private DatabaseReference adminNotifRef;
    private String currentTab = "All";

    private MaterialCardView btnBack;
    private RecyclerView rvNotifications;
    private TextView tabAll, tabVaccine, tabAdopt, tabDonate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Sử dụng layout fragment_admin_notification.xml
        return inflater.inflate(R.layout.fragment_admin_notification, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Ánh xạ view thủ công để tránh lỗi ViewBinding không nhận diện ID
        btnBack = view.findViewById(R.id.btnBack);
        rvNotifications = view.findViewById(R.id.rvNotifications);
        tabAll = view.findViewById(R.id.tabAll);
        tabVaccine = view.findViewById(R.id.tabVaccine);
        tabAdopt = view.findViewById(R.id.tabAdopt);
        tabDonate = view.findViewById(R.id.tabDonate);

        adminNotifRef = FirebaseDatabase.getInstance().getReference("Admin_Notifications");

        setupRecyclerView();
        setupTabClicks();
        loadNotifications();

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
    }

    private void setupRecyclerView() {
        adapter = new AdminNotificationAdapter(filteredNotifList, notification -> {
            // Đánh dấu đã đọc khi nhấn vào
            if (notification.getId() != null && !notification.isRead()) {
                adminNotifRef.child(notification.getId()).child("isRead").setValue(true);
            }
        });
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
            rvNotifications.setAdapter(adapter);
        }
    }

    private void setupTabClicks() {
        if (tabAll != null) tabAll.setOnClickListener(v -> selectTab("All", tabAll));
        if (tabVaccine != null) tabVaccine.setOnClickListener(v -> selectTab("Vaccine", tabVaccine));
        if (tabAdopt != null) tabAdopt.setOnClickListener(v -> selectTab("Adopt", tabAdopt));
        if (tabDonate != null) tabDonate.setOnClickListener(v -> selectTab("Donate", tabDonate));
    }

    private void selectTab(String tab, TextView textView) {
        currentTab = tab;
        
        // Reset tất cả tab về style trắng
        TextView[] tabs = {tabAll, tabVaccine, tabAdopt, tabDonate};
        for (TextView t : tabs) {
            if (t != null) {
                t.setBackgroundResource(R.drawable.bg_pill_white);
                t.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            }
        }

        // Active tab được chọn sang màu xanh
        if (textView != null) {
            textView.setBackgroundResource(R.drawable.bg_pill_green);
            textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        }

        filterNotifications();
    }

    private void loadNotifications() {
        adminNotifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                allNotifList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notification notif = ds.getValue(Notification.class);
                    if (notif != null) {
                        notif.setId(ds.getKey());
                        allNotifList.add(notif);
                    }
                }
                
                // Sắp xếp mới nhất lên đầu
                Collections.sort(allNotifList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                filterNotifications();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminNotif", "Error: " + error.getMessage());
            }
        });
    }

    private void filterNotifications() {
        filteredNotifList.clear();
        if (currentTab.equals("All")) {
            filteredNotifList.addAll(allNotifList);
        } else {
            String filterType = currentTab.toLowerCase();
            for (Notification n : allNotifList) {
                boolean matchesType = (n.getType() != null && n.getType().equalsIgnoreCase(filterType));
                boolean matchesTitle = (n.getTitle() != null && n.getTitle().toLowerCase().contains(filterType));
                
                if (matchesType || matchesTitle) {
                    filteredNotifList.add(n);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
