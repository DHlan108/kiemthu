package com.example.rfidpetrescue.fragments.admin.category.adopt;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
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

interface OnDayClickListener {
    void onDayClick(Date date);
}

interface OnTimeDeleteListener {
    void onDelete(int position);
}

public class AdminEditInterviewFragment extends Fragment {

    private MaterialCardView btnBack;
    private TextView tvCurrentMonth;
    private ImageView imgBack, imgNext;
    private RecyclerView rvDays, rvEditTimes;
    private MaterialButton btnSave, btnAddMore;

    private DatabaseReference slotsRef;
    private List<Date> dateList = new ArrayList<>();
    private List<String> timeList = new ArrayList<>();
    private Date selectedDate;

    private DayAdapter dayAdapter;
    private TimeSlotAdapter timeAdapter;

    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private boolean isChanged = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_adoption_interview_schedule_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        slotsRef = FirebaseDatabase.getInstance().getReference("Interview_Slots");

        initViews(view);
        setupCalendar(Calendar.getInstance().getTime());
        setupTimeRecyclerView();
        setupClickListeners();
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth);
        imgBack = view.findViewById(R.id.imgBack);
        imgNext = view.findViewById(R.id.imgNext);
        rvDays = view.findViewById(R.id.rvDays);
        rvEditTimes = view.findViewById(R.id.rvEditTimes);
        btnSave = view.findViewById(R.id.btnSave);
        btnAddMore = view.findViewById(R.id.btnAddMore);
        markAsChanged(false);
    }

    private void setupCalendar(Date startDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        dateList.clear();
        for (int i = 0; i < 30; i++) {
            dateList.add(cal.getTime());
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        if (dayAdapter == null) {
            dayAdapter = new DayAdapter(dateList, date -> selectDate(date));
            rvDays.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
            rvDays.setAdapter(dayAdapter);
        } else {
            dayAdapter.notifyDataSetChanged();
        }
        selectDate(startDate);
    }

    private void setupTimeRecyclerView() {
        timeAdapter = new TimeSlotAdapter(timeList, position -> {
            if (position >= 0 && position < timeList.size()) {
                timeList.remove(position);
                timeAdapter.notifyDataSetChanged();
                markAsChanged(true);
            }
        });
        rvEditTimes.setLayoutManager(new LinearLayoutManager(getContext()));
        rvEditTimes.setAdapter(timeAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        btnAddMore.setOnClickListener(v -> showCustomAddDialog());

        btnSave.setOnClickListener(v -> {
            if (isChanged) {
                saveToFirebase();
            } else {
                Toast.makeText(getContext(), "Chưa có thay đổi nào để lưu!", Toast.LENGTH_SHORT).show();
            }
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

        tvCurrentMonth.setOnClickListener(pickDateListener);
        imgBack.setOnClickListener(pickDateListener);
        imgNext.setOnClickListener(pickDateListener);
    }

    private void selectDate(Date date) {
        if (date == null) return;
        selectedDate = date;
        SimpleDateFormat monthYearFormat = new SimpleDateFormat("MM, yyyy", Locale.US);
        tvCurrentMonth.setText("Tháng " + monthYearFormat.format(date));
        if (dayAdapter != null) {
            dayAdapter.setSelectedDate(date);
            dayAdapter.notifyDataSetChanged();
        }
        markAsChanged(false);
        loadFromFirebase(date);
    }

    private void loadFromFirebase(Date date) {
        String dateKey = dbDateFormat.format(date);
        slotsRef.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                timeList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String time = (ds.getValue() instanceof String) ? (String) ds.getValue() : ds.child("time").getValue(String.class);
                        if (time != null && !time.isEmpty()) timeList.add(time);
                    }
                }
                Collections.sort(timeList);
                if (timeAdapter != null) timeAdapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOGIC LƯU CHO TẤT CẢ CÁC NGÀY TIẾP THEO
    // ─────────────────────────────────────────────────────────────────────────
    private void saveToFirebase() {
        if (selectedDate == null) return;

        Collections.sort(timeList);
        Map<String, Object> multiDayUpdates = new HashMap<>();

        // Chuẩn bị Map dữ liệu giờ (để Firebase lưu dạng index: value an toàn)
        Map<String, String> safeTimeMap = new HashMap<>();
        for (int j = 0; j < timeList.size(); j++) {
            safeTimeMap.put(String.valueOf(j), timeList.get(j));
        }

        Calendar loopCal = Calendar.getInstance();
        loopCal.setTime(selectedDate);

        // Áp dụng danh sách này cho 30 ngày tính từ ngày đang chọn
        for (int i = 0; i < 30; i++) {
            String dateKey = dbDateFormat.format(loopCal.getTime());

            if (timeList.isEmpty()) {
                multiDayUpdates.put(dateKey, null); // Xóa lịch nếu danh sách trống
            } else {
                multiDayUpdates.put(dateKey, safeTimeMap);
            }
            loopCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        slotsRef.updateChildren(multiDayUpdates).addOnSuccessListener(aVoid -> {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Đã cập nhật lịch cho 30 ngày tới!", Toast.LENGTH_SHORT).show();
                markAsChanged(false);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void showCustomAddDialog() {
        if (getContext() == null) return;
        Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.dialog_add_interview_schedule);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView btnClose = dialog.findViewById(R.id.btnClose);
        LinearLayout btnSelectDate = dialog.findViewById(R.id.btnSelectDate);
        TextView tvSelectedDate = dialog.findViewById(R.id.tvSelectedDate);
        LinearLayout btnSelectTime = dialog.findViewById(R.id.btnSelectTime);
        TextView tvSelectedTime = dialog.findViewById(R.id.tvSelectedTime);
        MaterialButton btnConfirmAdd = dialog.findViewById(R.id.btnConfirmAdd);

        String currentDateStr = new SimpleDateFormat(DatePickerUtils.DATE_FORMAT, Locale.US).format(selectedDate);
        tvSelectedDate.setText(currentDateStr);
        tvSelectedDate.setTextColor(Color.parseColor("#222222"));

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnSelectDate.setOnClickListener(v -> DatePickerUtils.show(this, tvSelectedDate, 0, null));

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
            timePickerDialog.show();
        });

        btnConfirmAdd.setOnClickListener(v -> {
            String selectedT = tvSelectedTime.getText().toString();
            if (selectedT.equals("--:--")) {
                Toast.makeText(getContext(), "Vui lòng chọn giờ", Toast.LENGTH_SHORT).show();
                return;
            }
            addTimeToList(selectedT);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void addTimeToList(String time) {
        if (!timeList.contains(time)) {
            timeList.add(time);
            Collections.sort(timeList);
            timeAdapter.notifyDataSetChanged();
            markAsChanged(true);
        } else {
            Toast.makeText(getContext(), "Giờ này đã có trong danh sách", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAsChanged(boolean changed) {
        if (btnSave == null) return;
        this.isChanged = changed;
        if (isChanged) {
            btnSave.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#1D1A9B")));
            btnSave.setTextColor(Color.parseColor("#1D1A9B"));
        } else {
            btnSave.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
            btnSave.setTextColor(Color.parseColor("#D9D9D9"));
        }
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return 0;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {
        private List<Date> dates;
        private Date selected;
        private OnDayClickListener listener;
        private SimpleDateFormat dm = new SimpleDateFormat("d", Locale.US);

        public DayAdapter(List<Date> dates, OnDayClickListener listener) {
            this.dates = dates; this.listener = listener;
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
            Calendar c = Calendar.getInstance(); c.setTime(d);
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
            h.itemView.setOnClickListener(v -> { if (listener != null) listener.onDayClick(d); });
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

    class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeViewHolder> {
        private List<String> times;
        private OnTimeDeleteListener listener;

        public TimeSlotAdapter(List<String> times, OnTimeDeleteListener listener) {
            this.times = times; this.listener = listener;
        }

        @NonNull
        @Override
        public TimeViewHolder onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new TimeViewHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_time_edit, p, false));
        }

        @Override
        public void onBindViewHolder(@NonNull TimeViewHolder h, int p) {
            if (p < times.size()) {
                h.tv.setText(times.get(p));
                h.btn.setOnClickListener(v -> {
                    int currentPos = h.getAdapterPosition();
                    if (currentPos != RecyclerView.NO_POSITION && listener != null) listener.onDelete(currentPos);
                });
            }
        }

        @Override public int getItemCount() { return times.size(); }

        class TimeViewHolder extends RecyclerView.ViewHolder {
            TextView tv; ImageView btn;
            public TimeViewHolder(@NonNull View v) {
                super(v);
                tv = v.findViewById(R.id.tvTime);
                btn = v.findViewById(R.id.btnDelete);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
    }
}