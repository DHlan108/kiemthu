package com.example.rfidpetrescue.fragments.admin.category.adopt;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.databinding.FragmentAdoptionInterviewScheduleBinding;
import com.example.rfidpetrescue.models.TimeSlot;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.google.android.material.button.MaterialButton;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

interface OnAdoptDayClickListener {
    void onDayClick(Date date);
}

public class AdminAdoptInterviewScheduleFragment extends Fragment {

    private FragmentAdoptionInterviewScheduleBinding binding;
    private DatabaseReference interviewScheduleRef;

    private List<Date> dateList = new ArrayList<>();
    private DayAdapter dayAdapter;

    private List<TimeSlot> morningSlots = new ArrayList<>();
    private List<TimeSlot> afternoonSlots = new ArrayList<>();

    // ĐÃ THAY ĐỔI: Sử dụng ViewOnlyTimeAdapter để không cho phép bấm
    private ViewOnlyTimeAdapter morningAdapter, afternoonAdapter;

    private Date selectedDate;
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdoptionInterviewScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        interviewScheduleRef = FirebaseDatabase.getInstance().getReference("Interview_Slots");

        setupCalendar(Calendar.getInstance().getTime());
        setupTimeSlots();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        binding.btnSubmit.setText("Thêm lịch phỏng vấn mới");
        binding.btnSubmit.setOnClickListener(v -> showAddScheduleDialog());

        binding.btnEdit.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminEditInterviewFragment())
                    .addToBackStack(null)
                    .commit();
        });

        View.OnClickListener pickDateListener = v -> {
            TextView dummyTextView = new TextView(getContext());
            if (selectedDate != null) {
                dummyTextView.setText(new SimpleDateFormat(DatePickerUtils.DATE_FORMAT, Locale.US).format(selectedDate));
            }
            DatePickerUtils.show(this, dummyTextView, 0, dateStr -> {
                try {
                    Date date = new SimpleDateFormat(DatePickerUtils.DATE_FORMAT, Locale.US).parse(dateStr);
                    setupCalendar(date);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        };

        binding.tvCurrentMonth.setOnClickListener(pickDateListener);

        try {
            ImageView imgBack = binding.getRoot().findViewById(R.id.imgBack);
            ImageView imgNext = binding.getRoot().findViewById(R.id.imgNext);
            if (imgBack != null) imgBack.setOnClickListener(pickDateListener);
            if (imgNext != null) imgNext.setOnClickListener(pickDateListener);
        } catch (Exception ignored) {}
    }

    private void setupCalendar(Date startDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);

        dateList.clear();
        for (int i = 0; i < 30; i++) {
            dateList.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        dayAdapter = new DayAdapter(dateList, date -> selectDate(date));
        binding.rvDays.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvDays.setAdapter(dayAdapter);

        selectDate(startDate);
    }

    private void selectDate(Date date) {
        if (date == null) return;
        selectedDate = date;

        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM, yyyy", Locale.US);
        binding.tvCurrentMonth.setText("Tháng " + monthYearFormat.format(date));

        if (dayAdapter != null) {
            dayAdapter.setSelectedDate(date);
            dayAdapter.notifyDataSetChanged();
        }

        loadScheduleForDate(date);
    }

    private void setupTimeSlots() {
        morningAdapter = new ViewOnlyTimeAdapter(morningSlots);
        binding.rvMorningSlots.setLayoutManager(new GridLayoutManager(getContext(), 4));
        binding.rvMorningSlots.setAdapter(morningAdapter);

        afternoonAdapter = new ViewOnlyTimeAdapter(afternoonSlots);
        binding.rvAfternoonSlots.setLayoutManager(new GridLayoutManager(getContext(), 4));
        binding.rvAfternoonSlots.setAdapter(afternoonAdapter);
    }

    private void loadScheduleForDate(Date date) {
        String dateKey = dbDateFormat.format(date);

        interviewScheduleRef.child(dateKey).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                morningSlots.clear();
                afternoonSlots.clear();

                if (snapshot.exists()) {
                    List<String> rawTimes = new ArrayList<>();
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String time = (ds.getValue() instanceof String) ? (String) ds.getValue() : ds.child("time").getValue(String.class);
                        if (time != null && !time.isEmpty()) rawTimes.add(time);
                    }

                    Collections.sort(rawTimes);

                    for (String t : rawTimes) {
                        boolean isMorning = true;
                        if (t.toLowerCase().contains("am")) {
                            isMorning = true;
                        } else if (t.toLowerCase().contains("pm")) {
                            isMorning = false;
                        } else {
                            try {
                                int hour = Integer.parseInt(t.split(":")[0]);
                                isMorning = (hour < 12);
                            } catch (Exception e) {
                                isMorning = true;
                            }
                        }

                        if (isMorning) {
                            morningSlots.add(new TimeSlot(t));
                        } else {
                            afternoonSlots.add(new TimeSlot(t));
                        }
                    }
                }
                morningAdapter.notifyDataSetChanged();
                afternoonAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showAddScheduleDialog() {
        if (getContext() == null || selectedDate == null) return;

        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_add_interview_schedule);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView btnClose = dialog.findViewById(R.id.btnClose);

        TextView lblDate = dialog.findViewById(R.id.lblDate);
        LinearLayout btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        if (lblDate != null) lblDate.setVisibility(View.GONE);
        if (btnSelectDate != null) btnSelectDate.setVisibility(View.GONE);

        LinearLayout btnSelectTime = dialog.findViewById(R.id.btnSelectTime);
        TextView tvSelectedTime = dialog.findViewById(R.id.tvSelectedTime);
        MaterialButton btnConfirmAdd = dialog.findViewById(R.id.btnConfirmAdd);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnSelectTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();

            TimePickerDialog timePickerDialog = new TimePickerDialog(DatePickerUtils.getLocalizedContext(requireContext()), R.style.BlueDialogTheme, (view, hourOfDay, minute) -> {
                String time = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                tvSelectedTime.setText(time);
                tvSelectedTime.setTextColor(Color.parseColor("#222222"));
                btnConfirmAdd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D1A9B")));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true);

            timePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", timePickerDialog);
            timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HỦY", (d, which) -> d.dismiss());

            timePickerDialog.setOnShowListener(dialogInterface -> {
                Button posBtn = timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negBtn = timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (posBtn != null) posBtn.setTextColor(Color.parseColor("#1D1A9B"));
                if (negBtn != null) negBtn.setTextColor(Color.parseColor("#1D1A9B"));
            });

            timePickerDialog.show();
        });

        btnConfirmAdd.setOnClickListener(v -> {
            String selectedT = tvSelectedTime.getText().toString();

            if (selectedT.equals("--:--")) {
                Toast.makeText(getContext(), "Vui lòng chọn đầy đủ giờ", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                interviewScheduleRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> multiDayUpdates = new HashMap<>();

                        Calendar loopCal = Calendar.getInstance();
                        loopCal.setTime(selectedDate);

                        for (int i = 0; i < 30; i++) {
                            String dateKey = dbDateFormat.format(loopCal.getTime());
                            List<String> currentTimes = new ArrayList<>();

                            if (snapshot.hasChild(dateKey)) {
                                for (DataSnapshot ds : snapshot.child(dateKey).getChildren()) {
                                    String t = (ds.getValue() instanceof String) ? (String) ds.getValue() : ds.child("time").getValue(String.class);
                                    if (t != null) currentTimes.add(t);
                                }
                            }

                            if (!currentTimes.contains(selectedT)) {
                                currentTimes.add(selectedT);
                                Collections.sort(currentTimes);
                            }

                            Map<String, String> safeFirebaseMap = new HashMap<>();
                            for (int j = 0; j < currentTimes.size(); j++) {
                                safeFirebaseMap.put(String.valueOf(j), currentTimes.get(j));
                            }

                            multiDayUpdates.put(dateKey, safeFirebaseMap);
                            loopCal.add(Calendar.DAY_OF_MONTH, 1);
                        }

                        interviewScheduleRef.updateChildren(multiDayUpdates)
                                .addOnSuccessListener(aVoid -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Thành công! Lịch áp dụng cho 30 ngày.", Toast.LENGTH_SHORT).show();
                                    }
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Lỗi Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        dialog.show();
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return 0;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐÃ THÊM: ADAPTER CHỈ ĐỂ XEM (KHÔNG BẤM ĐƯỢC) CHO KHUNG GIỜ
    // ─────────────────────────────────────────────────────────────────────────
    class ViewOnlyTimeAdapter extends RecyclerView.Adapter<ViewOnlyTimeAdapter.TimeViewHolder> {
        private List<TimeSlot> timeSlots;

        public ViewOnlyTimeAdapter(List<TimeSlot> timeSlots) {
            this.timeSlots = timeSlots;
        }

        @NonNull
        @Override
        public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(14f);
            tv.setTextColor(Color.parseColor("#666666"));
            tv.setMaxLines(1);
            tv.setPadding(0, 0, 0, 0);

            // Giao diện bo tròn, màu xám nhạt (Chỉ xem, không chọn được)
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(100f);
            bg.setColor(Color.WHITE);
            bg.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));
            tv.setBackground(bg);

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(38)
            );
            int margin = dpToPx(6);
            params.setMargins(margin, margin, margin, margin);
            tv.setLayoutParams(params);

            return new TimeViewHolder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull TimeViewHolder holder, int position) {
            holder.tv.setText(timeSlots.get(position).getTime());
            // KHÔNG gắn setOnClickListener -> Hoàn toàn vô hiệu hóa việc bấm!
        }

        @Override
        public int getItemCount() {
            return timeSlots.size();
        }

        class TimeViewHolder extends RecyclerView.ViewHolder {
            TextView tv;
            public TimeViewHolder(@NonNull View itemView) {
                super(itemView);
                tv = (TextView) itemView;
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADAPTER LỊCH NỘI BỘ
    // ─────────────────────────────────────────────────────────────────────────
    class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
        private List<Date> dates;
        private Date selected;
        private OnAdoptDayClickListener listener;
        private SimpleDateFormat dm = new SimpleDateFormat("d", Locale.US);

        public DayAdapter(List<Date> dates, OnAdoptDayClickListener listener) {
            this.dates = dates;
            this.listener = listener;
        }

        public void setSelectedDate(Date date) { this.selected = date; }

        @NonNull
        @Override
        public DayViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new DayViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_calendar_day, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull DayViewHolder h, int p) {
            Date d = dates.get(p);

            Calendar c = Calendar.getInstance();
            c.setTime(d);
            int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
            String dayStr = "";
            switch (dayOfWeek) {
                case Calendar.SUNDAY: dayStr = "CN"; break;
                case Calendar.MONDAY: dayStr = "T.2"; break;
                case Calendar.TUESDAY: dayStr = "T.3"; break;
                case Calendar.WEDNESDAY: dayStr = "T.4"; break;
                case Calendar.THURSDAY: dayStr = "T.5"; break;
                case Calendar.FRIDAY: dayStr = "T.6"; break;
                case Calendar.SATURDAY: dayStr = "T.7"; break;
            }

            h.tvW.setText(dayStr);
            h.tvM.setText(dm.format(d));

            boolean isSel = selected != null && dbDateFormat.format(d).equals(dbDateFormat.format(selected));

            if (isSel) {
                GradientDrawable selectedBg = new GradientDrawable();
                selectedBg.setShape(GradientDrawable.RECTANGLE);
                selectedBg.setCornerRadius(dpToPx(16));
                selectedBg.setColor(Color.parseColor("#B2DF20"));

                h.itemView.setBackground(selectedBg);
                h.tvW.setTextColor(Color.WHITE);
                h.tvM.setTextColor(Color.WHITE);
            } else {
                h.itemView.setBackgroundColor(Color.TRANSPARENT);
                h.tvW.setTextColor(Color.GRAY);
                h.tvM.setTextColor(Color.BLACK);
            }

            h.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onDayClick(d);
            });
        }

        @Override public int getItemCount() { return dates.size(); }

        class DayViewHolder extends RecyclerView.ViewHolder {
            TextView tvW, tvM;
            public DayViewHolder(@NonNull View v) {
                super(v);
                tvW = v.findViewById(R.id.tvDayOfWeek);
                tvM = v.findViewById(R.id.tvDateNumber);
            }
        }
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