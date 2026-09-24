package com.example.rfidpetrescue.fragments.user.appointment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AppointmentAdapter;
import com.example.rfidpetrescue.models.Appointment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyAppointmentFragment extends Fragment {

    private RecyclerView rcvUpcoming, rcvPast;
    private TextView tvUpcoming, tvPast;
    private View layoutHasData;
    private LinearLayout layoutNoData;
    private ImageView btnBack;

    private AppointmentAdapter upcomingAdapter, pastAdapter;
    private List<Appointment> upcomingList, pastList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rcvUpcoming = view.findViewById(R.id.rcvUpcoming);
        rcvPast = view.findViewById(R.id.rcvPast);
        tvUpcoming = view.findViewById(R.id.tvUpcoming);
        tvPast = view.findViewById(R.id.tvPast);
        layoutHasData = view.findViewById(R.id.layoutHasData);
        layoutNoData = view.findViewById(R.id.layoutNoData);
        btnBack = view.findViewById(R.id.btnBack);

        upcomingList = new ArrayList<>();
        pastList = new ArrayList<>();

        upcomingAdapter = new AppointmentAdapter(getContext(), upcomingList);
        pastAdapter = new AppointmentAdapter(getContext(), pastList);

        rcvUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvUpcoming.setAdapter(upcomingAdapter);

        rcvPast.setLayoutManager(new LinearLayoutManager(getContext()));
        rcvPast.setAdapter(pastAdapter);

        loadAllAppointments();

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        View btnGoHome = view.findViewById(R.id.btnGoHome);
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                getParentFragmentManager().popBackStack();
            });
        }
    }

    private void loadAllAppointments() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Appointments");

        ref.orderByChild("userId").equalTo(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                upcomingList.clear();
                pastList.clear();

                long currentTime = System.currentTimeMillis();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    try {
                        Appointment apt = ds.getValue(Appointment.class);
                        if (apt != null) {
                            apt.setId(ds.getKey());

                            // Phân loại
                            long aptTime = parseAppointmentDate(apt.getDate());

                            // Lịch sắp đến: Chưa tới giờ HOẶC đang chờ duyệt (Pending)
                            if (aptTime >= currentTime || "Pending".equals(apt.getStatus())) {
                                upcomingList.add(apt);
                            } else {
                                // Lịch đã qua
                                pastList.add(apt);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("MyAppointment", "Lỗi parse lịch hẹn", e);
                    }
                }

                if (upcomingList.size() > 0 || pastList.size() > 0) {
                    layoutNoData.setVisibility(View.GONE);
                    layoutHasData.setVisibility(View.VISIBLE);

                    // Sắp xếp lịch Sắp đến (Tăng dần - Càng gần hiện tại càng lên đầu)
                    Collections.sort(upcomingList, (a1, a2) -> Long.compare(parseAppointmentDate(a1.getDate()), parseAppointmentDate(a2.getDate())));

                    // Sắp xếp lịch Đã qua (Giảm dần - Vừa mới qua sẽ lên đầu)
                    Collections.sort(pastList, (a1, a2) -> Long.compare(parseAppointmentDate(a2.getDate()), parseAppointmentDate(a1.getDate())));

                    // Ẩn hiện tiêu đề nếu trống
                    tvUpcoming.setVisibility(upcomingList.isEmpty() ? View.GONE : View.VISIBLE);
                    rcvUpcoming.setVisibility(upcomingList.isEmpty() ? View.GONE : View.VISIBLE);

                    tvPast.setVisibility(pastList.isEmpty() ? View.GONE : View.VISIBLE);
                    rcvPast.setVisibility(pastList.isEmpty() ? View.GONE : View.VISIBLE);

                    upcomingAdapter.notifyDataSetChanged();
                    pastAdapter.notifyDataSetChanged();
                } else {
                    layoutNoData.setVisibility(View.VISIBLE);
                    layoutHasData.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private long parseAppointmentDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return 0;
        try {
            return new java.text.SimpleDateFormat("EEEE, dd/MM/yyyy", new java.util.Locale("vi", "VN")).parse(dateString).getTime();
        } catch (Exception e1) {
            try { return new java.text.SimpleDateFormat("dd/MM/yyyy").parse(dateString).getTime(); }
            catch (Exception e2) { return 0; }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
    }
}