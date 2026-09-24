package com.example.rfidpetrescue.fragments.user.appointment;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AppointmentFragment extends Fragment {

    private LinearLayout layoutDatesContainer;
    private ImageButton btnBack;
    private GridLayout gridMorning, gridAfternoon;
    private MaterialButton btnConfirm;

    private View selectedDateView = null;
    private TextView selectedTimeView = null;

    private String selectedDateValue = "";
    private String selectedTimeValue = "";

    private DatabaseReference slotsRef;
    private SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        slotsRef = FirebaseDatabase.getInstance().getReference("Interview_Slots");

        layoutDatesContainer = view.findViewById(R.id.layoutDatesContainer);
        btnBack = view.findViewById(R.id.btnBack);
        gridMorning = view.findViewById(R.id.gridMorning);
        gridAfternoon = view.findViewById(R.id.gridAfternoon);

        // Ép GridLayout hiển thị 4 cột
        gridMorning.setColumnCount(4);
        gridAfternoon.setColumnCount(4);

        btnConfirm = view.findViewById(R.id.btnConfirm);

        btnConfirm.setEnabled(false);
        btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFCDD2")));

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String petId = getArguments() != null ? getArguments().getString("PET_ID") : null;

            Bundle bundle = new Bundle();
            bundle.putString("selected_date", selectedDateValue);
            bundle.putString("selected_time", selectedTimeValue);
            bundle.putString("PET_ID", petId);

            ConfirmFragment confirmFragment = new ConfirmFragment();
            confirmFragment.setArguments(bundle);

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_container, confirmFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        setupDateSelector();
    }

    private GradientDrawable getUnselectedDateBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(Color.WHITE);
        return bg;
    }

    private GradientDrawable getSelectedDateBg() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dpToPx(16));
        bg.setColor(Color.parseColor("#FF5C8D"));
        return bg;
    }

    private void setupDateSelector() {
        Calendar calendar = Calendar.getInstance();

        Locale localeVN = new Locale("vi", "VN");
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, dd/MM/yyyy", localeVN);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", localeVN);
        SimpleDateFormat dateFormat = new SimpleDateFormat("d", localeVN);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        for (int i = 0; i < 14; i++) {
            View dateView = inflater.inflate(R.layout.item_date, layoutDatesContainer, false);

            TextView tvDayOfWeek = dateView.findViewById(R.id.tvDayOfWeek);
            TextView tvDayOfMonth = dateView.findViewById(R.id.tvDayOfMonth);

            dateView.setBackground(getUnselectedDateBg());

            final String strFullDate = fullDateFormat.format(calendar.getTime());
            final String dateKeyForFirebase = dbDateFormat.format(calendar.getTime());

            tvDayOfWeek.setText(dayFormat.format(calendar.getTime()));
            tvDayOfMonth.setText(dateFormat.format(calendar.getTime()));

            dateView.setOnClickListener(v -> {
                if (selectedDateView != null) {
                    selectedDateView.setBackground(getUnselectedDateBg());
                    ((TextView)selectedDateView.findViewById(R.id.tvDayOfWeek)).setTextColor(Color.parseColor("#888888"));
                    ((TextView)selectedDateView.findViewById(R.id.tvDayOfMonth)).setTextColor(Color.parseColor("#222222"));
                }

                dateView.setBackground(getSelectedDateBg());
                tvDayOfWeek.setTextColor(Color.WHITE);
                tvDayOfMonth.setTextColor(Color.WHITE);

                selectedDateView = dateView;
                selectedDateValue = strFullDate;

                selectedTimeView = null;
                selectedTimeValue = "";
                btnConfirm.setEnabled(false);
                btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFCDD2")));

                loadTimesFromFirebase(dateKeyForFirebase);
            });

            if (i == 0) {
                dateView.performClick();
            }

            layoutDatesContainer.addView(dateView);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void loadTimesFromFirebase(String dateKey) {
        slotsRef.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                gridMorning.removeAllViews();
                gridAfternoon.removeAllViews();

                boolean hasMorning = false;
                boolean hasAfternoon = false;

                if (snapshot.exists()) {
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        String time = (ds.getValue() instanceof String) ? (String) ds.getValue() : ds.child("time").getValue(String.class);
                        if (time != null && !time.isEmpty()) {
                            boolean isMorning = true;
                            if (time.toLowerCase().contains("am")) {
                                isMorning = true;
                            } else if (time.toLowerCase().contains("pm")) {
                                isMorning = false;
                            } else {
                                try {
                                    int hour = Integer.parseInt(time.split(":")[0]);
                                    isMorning = (hour < 12);
                                } catch (Exception e) {
                                    isMorning = true;
                                }
                            }

                            if (isMorning) {
                                addTimeSlotToGrid(time, gridMorning);
                                hasMorning = true;
                            } else {
                                addTimeSlotToGrid(time, gridAfternoon);
                                hasAfternoon = true;
                            }
                        }
                    }
                }

                if (!hasMorning) addEmptyMessage(gridMorning, "Không có lịch trống");
                if (!hasAfternoon) addEmptyMessage(gridAfternoon, "Không có lịch trống");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private int dpToPx(int dp) {
        if (getContext() == null) return 0;
        float density = getContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void addTimeSlotToGrid(String time, GridLayout gridLayout) {
        TextView tv = new TextView(getContext());
        tv.setText(time);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14f); // Tăng size lên một chút cho dễ nhìn
        tv.setTextColor(Color.parseColor("#666666"));
        tv.setPadding(0, 0, 0, 0); // Đưa padding về 0 để chữ có không gian hiển thị đầy đủ
        tv.setMaxLines(1); // Ép hiển thị trên 1 dòng duy nhất

        GradientDrawable unselectedBg = new GradientDrawable();
        unselectedBg.setShape(GradientDrawable.RECTANGLE);
        unselectedBg.setCornerRadius(100f);
        unselectedBg.setColor(Color.WHITE);
        unselectedBg.setStroke(dpToPx(1), Color.parseColor("#E0E0E0"));

        GradientDrawable selectedBg = new GradientDrawable();
        selectedBg.setShape(GradientDrawable.RECTANGLE);
        selectedBg.setCornerRadius(100f);
        selectedBg.setColor(Color.parseColor("#FF5C8D"));

        tv.setBackground(unselectedBg);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        // SỬ DỤNG WEIGHT ĐỂ AUTO CHIA ĐỀU 4 CỘT (Không lo tràn viền)
        params.width = 0;
        params.height = dpToPx(38);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        int marginPx = dpToPx(6);
        params.setMargins(marginPx, marginPx, marginPx, marginPx);

        tv.setLayoutParams(params);

        tv.setOnClickListener(v -> {
            if (selectedTimeView != null) {
                selectedTimeView.setBackground(unselectedBg);
                selectedTimeView.setTextColor(Color.parseColor("#666666"));
            }

            tv.setBackground(selectedBg);
            tv.setTextColor(Color.WHITE);

            selectedTimeView = tv;
            String session = (gridLayout == gridMorning) ? "Sáng" : "Chiều";
            selectedTimeValue = session + " (" + time + ")";

            checkEnableConfirmButton();
        });

        gridLayout.addView(tv);
    }

    private void addEmptyMessage(GridLayout gridLayout, String message) {
        TextView tv = new TextView(getContext());
        tv.setText(message);
        tv.setTextColor(Color.parseColor("#CCCCCC"));
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, dpToPx(16), 0, dpToPx(16));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = GridLayout.LayoutParams.MATCH_PARENT;
        params.columnSpec = GridLayout.spec(0, 4);
        tv.setLayoutParams(params);
        gridLayout.addView(tv);
    }

    private void checkEnableConfirmButton() {
        if (selectedDateView != null && selectedTimeView != null) {
            btnConfirm.setEnabled(true);
            btnConfirm.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF5C8D")));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}