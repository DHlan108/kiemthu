package com.example.rfidpetrescue.fragments.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminScheduleAdapter;
import com.example.rfidpetrescue.adapters.CalendarAdapter;
import com.example.rfidpetrescue.databinding.FragmentAdminHomeBinding;
import com.example.rfidpetrescue.fragments.admin.category.donate.DonateAdminFragment;
import com.example.rfidpetrescue.fragments.admin.category.schedule.AdminScheduleFragment;
import com.example.rfidpetrescue.models.CalendarDateModel;
import com.example.rfidpetrescue.models.ScheduleTask;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminHomeFragment extends Fragment {

    private FragmentAdminHomeBinding binding;
    private DatabaseReference donationsRef, adminNotifRef, userRef, scheduleRef;
    private ValueEventListener donationsListener, notifListener, petStatsListener, userListener, scheduleListener;
    private Animation shakeAnim;
    private boolean isNotifScreenOpened = false;
    private android.view.View dotUnread;
    private AdminScheduleAdapter scheduleAdapter;
    private List<ScheduleTask> scheduleList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shakeAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        adminNotifRef = FirebaseDatabase.getInstance().getReference("Admin_Notifications");
        dotUnread = binding.getRoot().findViewById(R.id.dotUnread);

        View.OnClickListener toDonateDetail = v -> navigateToDonateDetail();
        binding.cardDonate.setOnClickListener(toDonateDetail);
        binding.btnViewDonateDetails.setOnClickListener(toDonateDetail);

        // Chuyển sang màn hình danh sách thú cưng
        binding.btnViewChartDetails.setOnClickListener(v -> {
            com.example.rfidpetrescue.fragments.admin.category.AdminPetListFragment petListFragment =
                    new com.example.rfidpetrescue.fragments.admin.category.AdminPetListFragment();

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, petListFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });
        if (binding.btnViewFullSchedule != null) {
            binding.btnViewFullSchedule.setOnClickListener(v -> {
                com.example.rfidpetrescue.fragments.admin.category.schedule.AdminScheduleFragment scheduleFragment =
                        new com.example.rfidpetrescue.fragments.admin.category.schedule.AdminScheduleFragment();

                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                            .replace(R.id.main_container, scheduleFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });
        }

        if (binding.btnNotification != null) {
            binding.btnNotification.setOnClickListener(v -> {
                isNotifScreenOpened = true;
                binding.btnNotification.clearAnimation();
                if (dotUnread != null) dotUnread.setVisibility(android.view.View.GONE);
                markAllAsRead();

                getParentFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                        .replace(R.id.main_container, new AdminNotificationFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        loadUserProfileInfo();
        setupCalendar();
        loadTotalDonations();
        loadPetStatistics();
        checkNewNotifications();
        loadTodaySchedule();
    }

    private void loadTodaySchedule() {
        scheduleList = new ArrayList<>();
        scheduleAdapter = new AdminScheduleAdapter(scheduleList, getContext());

        if (binding.rvAdminSchedule != null) {
            binding.rvAdminSchedule.setLayoutManager(new LinearLayoutManager(getContext()));
            binding.rvAdminSchedule.setAdapter(scheduleAdapter);
        }

        // Xử lý sự kiện click vào item lịch (hoặc nút đen) để chuyển sang màn hình AdminScheduleFragment
        scheduleAdapter.setOnItemClickListener(task -> {
            AdminScheduleFragment scheduleFragment = new AdminScheduleFragment();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.main_container, scheduleFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        String todayDateString = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new Date());

        scheduleRef = FirebaseDatabase.getInstance().getReference("Admin_Schedules");

        scheduleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                scheduleList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        ScheduleTask task = ds.getValue(ScheduleTask.class);
                        // Lọc và chỉ lấy các công việc có ngày trùng với ngày hôm nay
                        if (task != null && todayDateString.equals(task.getDate())) {
                            task.setId(ds.getKey());
                            scheduleList.add(task);
                        }
                    }

                    // (Tùy chọn) Sắp xếp lịch theo thời gian thực hiện
                    Collections.sort(scheduleList, (o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));
                }
                if (scheduleAdapter != null) {
                    scheduleAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminHomeFragment", "Lỗi tải lịch: " + error.getMessage());
            }
        };
        scheduleRef.addValueEventListener(scheduleListener);
    }

    private void loadUserProfileInfo() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        userRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId);
        userListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;

                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String avatarUrl = snapshot.child("avatar").getValue(String.class);

                    if (name != null && !name.isEmpty()) {
                        binding.tvUserName.setText("Xin chào! " + name + "!");
                    } else {
                        binding.tvUserName.setText("Xin chào! Admin!");
                    }

                    if (avatarUrl != null && !avatarUrl.isEmpty() && binding.imgAvatar != null) {
                        Glide.with(AdminHomeFragment.this)
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.ic_avatar_active)
                                .into(binding.imgAvatar);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userRef.addValueEventListener(userListener);
    }

    private void checkNewNotifications() {
        notifListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || binding == null) return;
                boolean hasUnread = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Boolean isRead = ds.child("isRead").getValue(Boolean.class);
                    if (isRead != null && !isRead) {
                        hasUnread = true;
                        break;
                    }
                }
                if (hasUnread && !isNotifScreenOpened) {
                    if (binding.btnNotification != null) binding.btnNotification.startAnimation(shakeAnim);
                    if (dotUnread != null) dotUnread.setVisibility(android.view.View.VISIBLE);
                } else {
                    if (binding.btnNotification != null) binding.btnNotification.clearAnimation();
                    if (dotUnread != null) dotUnread.setVisibility(android.view.View.GONE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        adminNotifRef.addValueEventListener(notifListener);
    }

    private void markAllAsRead() {
        adminNotifRef.addListenerForSingleValueEvent(new ValueEventListener() {
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

    private void navigateToDonateDetail() {
        DonateAdminFragment detailFragment = new DonateAdminFragment();
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void loadTotalDonations() {
        donationsRef = FirebaseDatabase.getInstance().getReference("Donations");
        donationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long totalSum = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Long amount = data.child("amount").getValue(Long.class);
                    if (amount != null) totalSum += amount;
                }
                if (binding != null) {
                    DecimalFormat formatter = new DecimalFormat("#,###");
                    String formattedMoney = formatter.format(totalSum).replace(",", ".") + " vnđ";
                    binding.tvAccountBalance.setText(formattedMoney);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        donationsRef.addValueEventListener(donationsListener);
    }

    private void loadPetStatistics() {
        DatabaseReference petsRef = FirebaseDatabase.getInstance().getReference("Pets");
        petStatsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int total = (int) snapshot.getChildrenCount();
                if (binding == null) return;
                if (total == 0) {
                    updateUI(0, 0, 0, 0, 0);
                    return;
                }

                int treating = 0, adopted = 0, ready = 0, wait = 0, interview = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String status = ds.child("status").getValue(String.class);
                    if (status == null) continue;

                    switch (status) {
                        case "Đang điều trị": treating++; break;
                        case "Đã nhận nuôi": adopted++; break;
                        case "Sẵn sàng nhận nuôi": ready++; break;
                        case "Đang chờ nhận nuôi": wait++; break;
                        case "Đang chờ phỏng vấn": interview++; break;
                    }
                }

                updateUI(treating, adopted, ready, wait, interview);

                float pTreat = (float) treating * 100 / total;
                float pAdopt = (float) adopted * 100 / total;
                float pReady = (float) ready * 100 / total;
                float pWait = (float) wait * 100 / total;
                float pInter = (float) interview * 100 / total;

                binding.petPieChart.setData(pTreat, pAdopt, pReady, pWait, pInter);
            }

            private void updateUI(int t, int a, int r, int w, int i) {
                int total = t + a + r + w + i;
                if (total == 0) total = 1;

                binding.tvPercentTreating.setText((t * 100 / total) + "%");
                binding.tvPercentAdopted.setText((a * 100 / total) + "%");
                binding.tvPercentReady.setText((r * 100 / total) + "%");
                binding.tvPercentWait.setText((w * 100 / total) + "%");
                binding.tvPercentWaitInterView.setText((i * 100 / total) + "%");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        petsRef.addValueEventListener(petStatsListener);
    }

    private void setupCalendar() {
        List<CalendarDateModel> dateList = new ArrayList<>();
        Calendar todayCal = Calendar.getInstance();

        if (binding != null) {
            // Cập nhật text tháng/năm hiện tại
            SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMM, yyyy", new Locale("vi", "VN"));
            String formattedDate = monthYearFormat.format(todayCal.getTime());
            formattedDate = formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1);
            binding.tvCurrentMonthYear.setText(formattedDate);
        }

        // --- LOGIC LẤY 7 NGÀY CỦA TUẦN HIỆN TẠI ---
        Calendar cal = Calendar.getInstance();

        // Tìm ra ngày Thứ 2 của tuần này
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysToSubtract = (dayOfWeek == Calendar.SUNDAY) ? 6 : (dayOfWeek - Calendar.MONDAY);
        cal.add(Calendar.DAY_OF_MONTH, -daysToSubtract);

        for (int i = 0; i < 7; i++) {
            boolean isToday = isSameDay(cal, todayCal);
            boolean isPast = cal.getTimeInMillis() < todayCal.getTimeInMillis() && !isToday;

            dateList.add(new CalendarDateModel(cal.getTime(), isToday, isPast));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        CalendarAdapter calendarAdapter = new CalendarAdapter(dateList);
        binding.rvCalendar.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCalendar.setAdapter(calendarAdapter);
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onResume() {
        super.onResume();
        isNotifScreenOpened = false;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (donationsRef != null && donationsListener != null) donationsRef.removeEventListener(donationsListener);
        if (adminNotifRef != null && notifListener != null) adminNotifRef.removeEventListener(notifListener);
        if (petStatsListener != null) FirebaseDatabase.getInstance().getReference("Pets").removeEventListener(petStatsListener);
        if (userRef != null && userListener != null) userRef.removeEventListener(userListener);
        if (scheduleRef != null && scheduleListener != null) scheduleRef.removeEventListener(scheduleListener);
        binding = null;
    }
}