package com.example.rfidpetrescue.fragments.user.notification;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.NotificationAdapter;
import com.example.rfidpetrescue.databinding.FragmentNotificationBinding;
import com.example.rfidpetrescue.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationFragment extends Fragment {

    private FragmentNotificationBinding binding;
    private NotificationAdapter adapter;
    private List<Notification> allNotifList = new ArrayList<>();
    private List<Notification> filteredNotifList = new ArrayList<>();
    private DatabaseReference notifRef;
    private String currentTab = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // ĐƯỜNG DẪN CHUẨN: Notifications/{UID}
        notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(uid);

        setupRecyclerView();
        setupTabClicks();
        loadNotifications();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(filteredNotifList);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvNotifications.setAdapter(adapter);
    }

    private void setupTabClicks() {
        binding.tabAll.setOnClickListener(v -> selectTab("All", binding.tabAll));
        binding.tabAdopt.setOnClickListener(v -> selectTab("Adopt", binding.tabAdopt));
        binding.tabDonate.setOnClickListener(v -> selectTab("Donate", binding.tabDonate));
    }

    private void selectTab(String tab, TextView textView) {
        currentTab = tab;
        TextView[] tabs = {binding.tabAll, binding.tabAdopt, binding.tabDonate};
        for (TextView t : tabs) {
            t.setBackgroundResource(R.drawable.bg_pill_white);
            t.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));
        }
        textView.setBackgroundResource(R.drawable.bg_pill_xanhlouser);
        textView.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        filterNotifications();
    }

    private void loadNotifications() {
        if (notifRef == null) return;
        notifRef.addValueEventListener(new ValueEventListener() {
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
                Collections.sort(allNotifList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                filterNotifications();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void filterNotifications() {
        filteredNotifList.clear();
        if (currentTab.equals("All")) {
            filteredNotifList.addAll(allNotifList);
        } else {
            for (Notification n : allNotifList) {
                String type = n.getType() != null ? n.getType().toLowerCase() : "";
                String title = n.getTitle() != null ? n.getTitle().toLowerCase() : "";

                if (currentTab.equals("Adopt") && (type.contains("adopt") || title.contains("nhận nuôi"))) {
                    filteredNotifList.add(n);
                } else if (currentTab.equals("Donate") && (type.contains("donate") || title.contains("quyên góp"))) {
                    filteredNotifList.add(n);
                }
            }
        }
        adapter.notifyDataSetChanged();
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