package com.example.rfidpetrescue.fragments.admin.category.schedule;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.adapters.AdminScheduleAdapter;
import com.example.rfidpetrescue.adapters.CalendarAdapter;
import com.example.rfidpetrescue.utils.DatePickerUtils;
import com.example.rfidpetrescue.databinding.FragmentAdminScheduleBinding;
import com.example.rfidpetrescue.fragments.admin.category.adopt.AdminAdoptDetailFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.vaccination.VaccinationRecordFragment;
import com.example.rfidpetrescue.models.CalendarDateModel;
import com.example.rfidpetrescue.models.ScheduleTask;
import com.example.rfidpetrescue.fragments.admin.category.schedule.receivers.ScheduleNotificationReceiver;
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
import java.util.List;
import java.util.Locale;

public class AdminScheduleFragment extends Fragment {

    private FragmentAdminScheduleBinding binding;
    private DatabaseReference scheduleRef;
    private DatabaseReference petsRef;

    private List<CalendarDateModel> dateList = new ArrayList<>();
    private CalendarAdapter calendarAdapter;

    private List<ScheduleTask> taskList = new ArrayList<>();
    private List<ScheduleTask> manualTasksList = new ArrayList<>();
    private List<ScheduleTask> autoVaccineList = new ArrayList<>();
    private List<ScheduleTask> adoptInterviewList = new ArrayList<>();
    private AdminScheduleAdapter scheduleAdapter;

    private Date selectedViewDate;
    private Calendar dialogCalendar = Calendar.getInstance();
    private String selectedTimeStr = "";
    private String selectedType = "Tiêm chủng";

    private ValueEventListener currentScheduleListener;
    private DatabaseReference currentScheduleRef;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(getContext(), "Ứng dụng cần quyền thông báo để nhắc lịch!", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminScheduleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Ẩn thanh Bottom Bar ngay khi vừa vào trang
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        scheduleRef = FirebaseDatabase.getInstance().getReference("Admin_Schedules");
        petsRef = FirebaseDatabase.getInstance().getReference("Pets");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (selectedViewDate == null) {
            selectedViewDate = Calendar.getInstance().getTime();
        }

        setupCalendar();
        setupScheduleRecyclerView();

        binding.btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());
        binding.btnAddSchedule.setOnClickListener(v -> showAddScheduleDialog());

        try {
            int card1Id = getResources().getIdentifier("card1", "id", requireContext().getPackageName());
            if (card1Id != 0) {
                View card1 = binding.getRoot().findViewById(card1Id);
                if (card1 != null) {
                    card1.setOnClickListener(v -> {
                        if (getParentFragmentManager() != null) {
                            VaccinationRecordFragment vaccineFragment = new VaccinationRecordFragment();
                            getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, vaccineFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                }
            }

            int card2Id = getResources().getIdentifier("card2", "id", requireContext().getPackageName());
            if (card2Id != 0) {
                View card2 = binding.getRoot().findViewById(card2Id);
                if (card2 != null) {
                    card2.setOnClickListener(v -> {
                        if (getParentFragmentManager() != null) {
                            AdminAdoptDetailFragment adoptionFragment = new AdminAdoptDetailFragment();
                            getParentFragmentManager().beginTransaction()
                                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                    .replace(R.id.main_container, adoptionFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (binding.btnMonthSelector != null) {
            binding.btnMonthSelector.setOnClickListener(v -> {
                // Tạo một TextView ảo để hứng dữ liệu dd/MM/yyyy từ DatePickerUtils
                TextView dummyTv = new TextView(getContext());
                dummyTv.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(selectedViewDate));

                DatePickerUtils.show(this, dummyTv, null, date -> {
                    try {
                        Date parsed = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date);
                        if (parsed != null) {
                            selectedViewDate = parsed;

                            // Cập nhật lại UI tháng và reload lịch
                            setupCalendar();
                            loadScheduleForDate(selectedViewDate);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        }
        selectedViewDate = Calendar.getInstance().getTime();
        loadScheduleForDate(selectedViewDate);
    }

    // ========================================================================
    // LOGIC HỘP THOẠI THÊM CÔNG VIỆC MỚI
    // ========================================================================
    private void showAddScheduleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_schedule, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View ivClose = dialogView.findViewById(R.id.ivClose);
        EditText etTaskTitle = dialogView.findViewById(R.id.etTaskTitle);
        EditText etTaskDesc = dialogView.findViewById(R.id.etTaskDesc);

        View btnSelectRecordType = dialogView.findViewById(R.id.btnSelectRecordType);
        TextView tvRecordType = dialogView.findViewById(R.id.tvRecordType);
        ImageView ivCategoryIcon = dialogView.findViewById(R.id.ivCategoryIcon);

        View btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        TextView tvDateDisplay = dialogView.findViewById(R.id.tvDate);

        View btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        TextView tvTimeDisplay = dialogView.findViewById(R.id.tvSelectTime);

        MaterialButton btnSaveTask = dialogView.findViewById(R.id.btnSaveTask);

        dialogCalendar.setTime(selectedViewDate);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        tvDateDisplay.setText(sdf.format(dialogCalendar.getTime()));

        selectedTimeStr = "";
        selectedType = "Tiêm chủng";
        tvRecordType.setText("Tiêm chủng");
        ivCategoryIcon.setImageResource(R.drawable.ic_vaccine);

        Runnable validateInputs = () -> {
            String title = etTaskTitle.getText().toString().trim();
            boolean isReady = !title.isEmpty() && !selectedTimeStr.isEmpty();
            btnSaveTask.setEnabled(isReady);
            btnSaveTask.setBackgroundTintList(ColorStateList.valueOf(isReady ? Color.parseColor("#1D1A9B") : Color.parseColor("#D9D9D9")));
        };

        validateInputs.run();
        etTaskTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateInputs.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        ivClose.setOnClickListener(v -> dialog.dismiss());

        btnSelectRecordType.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnSelectRecordType);
            popup.getMenu().add("Donate");
            popup.getMenu().add("Bệnh án");
            popup.getMenu().add("Tiêm chủng");
            popup.getMenu().add("Nhận nuôi");
            popup.getMenu().add("Lịch");

            popup.setOnMenuItemClickListener(item -> {
                selectedType = item.getTitle().toString();
                tvRecordType.setText(selectedType);
                switch (selectedType) {
                    case "Donate": ivCategoryIcon.setImageResource(R.drawable.ic_wallet); break;
                    case "Bệnh án": ivCategoryIcon.setImageResource(R.drawable.ic_medical); break;
                    case "Tiêm chủng": ivCategoryIcon.setImageResource(R.drawable.ic_vaccine); break;
                    case "Nhận nuôi": ivCategoryIcon.setImageResource(R.drawable.ic_heart_fill); break;
                    case "Lịch": ivCategoryIcon.setImageResource(R.drawable.ic_calendar); break;
                }
                return true;
            });
            popup.show();
        });

        btnSelectDate.setOnClickListener(v -> DatePickerUtils.show(this, tvDateDisplay, null, date -> {
            try {
                Date parsed = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date);
                if (parsed != null) dialogCalendar.setTime(parsed);
            } catch (Exception ignored) {}
            validateInputs.run();
        }));

        btnSelectTime.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(getLocalizedContext(requireContext()), R.style.BlueDialogTheme, (tp, h, m) -> {
                selectedTimeStr = String.format(Locale.US, "%02d:%02d", h, m);
                tvTimeDisplay.setText(selectedTimeStr);
                tvTimeDisplay.setTextColor(Color.BLACK);
                dialogCalendar.set(Calendar.HOUR_OF_DAY, h);
                dialogCalendar.set(Calendar.MINUTE, m);
                validateInputs.run();
            }, dialogCalendar.get(Calendar.HOUR_OF_DAY), dialogCalendar.get(Calendar.MINUTE), true); // "true" là bật chế độ 24h

            timePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", timePickerDialog);
            timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HỦY", (d, which) -> d.dismiss());

            timePickerDialog.setOnShowListener(dialogInterface -> {
                Window window = timePickerDialog.getWindow();
                if (window != null) {
                    GradientDrawable shape = new GradientDrawable();
                    shape.setCornerRadius(40f);
                    shape.setColor(Color.WHITE);
                    InsetDrawable insetDrawable = new InsetDrawable(shape, 40);
                    window.setBackgroundDrawable(insetDrawable);
                }
                Button posBtn = timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negBtn = timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (posBtn != null) posBtn.setTextColor(Color.parseColor("#1D1A9B"));
                if (negBtn != null) negBtn.setTextColor(Color.parseColor("#1D1A9B"));
            });
            timePickerDialog.show();
        });

        btnSaveTask.setOnClickListener(v -> {
            String title = etTaskTitle.getText().toString().trim();
            String desc = etTaskDesc.getText().toString().trim();

            String taskId = scheduleRef.push().getKey();
            if (taskId != null) {
                ScheduleTask task = new ScheduleTask();
                task.setId(taskId);
                task.setTitle(title);
                task.setDescription(desc);
                task.setType(selectedType);
                task.setTime(selectedTimeStr);
                task.setDate(new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(dialogCalendar.getTime()));
                task.setManual(true);
                task.setTimestamp(dialogCalendar.getTimeInMillis());

                scheduleRef.child(taskId).setValue(task).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        scheduleNotification(task);
                        Toast.makeText(requireContext(), "Đã thêm lịch trình thành công", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(requireContext(), "Lỗi khi lưu", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        dialog.show();
    }

    // ========================================================================
    // LOGIC HỘP THOẠI CHỈNH SỬA CÔNG VIỆC
    // ========================================================================
    private void showEditScheduleDialog(ScheduleTask task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_task, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View ivClose = dialogView.findViewById(R.id.ivClose);
        EditText etTaskName = dialogView.findViewById(R.id.etTaskName);
        View btnSelectCategory = dialogView.findViewById(R.id.btnSelectCategory);
        TextView tvTaskCategory = dialogView.findViewById(R.id.tvTaskCategory);
        ImageView ivTaskCategory = dialogView.findViewById(R.id.ivTaskCategory);
        View btnSelectDate = dialogView.findViewById(R.id.btnSelectDate);
        TextView tvTaskDate = dialogView.findViewById(R.id.tvTaskDate);
        View btnSelectTime = dialogView.findViewById(R.id.btnSelectTime);
        TextView tvTaskTime = dialogView.findViewById(R.id.tvTaskTime);
        EditText etTaskNote = dialogView.findViewById(R.id.etTaskNote);
        MaterialButton btnSaveEdit = dialogView.findViewById(R.id.btnSaveEdit);

        final String[] editSelectedType = {task.getType()};
        final String[] editSelectedTime = {task.getTime()};
        final Calendar editCalendar = Calendar.getInstance();

        try {
            Date oldDate = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(task.getDate());
            if (oldDate != null) editCalendar.setTime(oldDate);
            if (task.getTime() != null && task.getTime().contains(":")) {
                String[] timeParts = task.getTime().split("[: ]");
                editCalendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
                editCalendar.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            }
        } catch (Exception ignored) {}

        etTaskName.setText(task.getTitle());
        tvTaskCategory.setText(task.getType());
        tvTaskDate.setText(task.getDate());
        tvTaskTime.setText(task.getTime());
        etTaskNote.setText(task.getDescription());

        switch (task.getType()) {
            case "Donate": ivTaskCategory.setImageResource(R.drawable.ic_wallet); break;
            case "Bệnh án": ivTaskCategory.setImageResource(R.drawable.ic_medical); break;
            case "Tiêm chủng": ivTaskCategory.setImageResource(R.drawable.ic_vaccine); break;
            case "Nhận nuôi": ivTaskCategory.setImageResource(R.drawable.ic_heart_fill); break;
            case "Lịch": ivTaskCategory.setImageResource(R.drawable.ic_calendar); break;
        }

        Runnable validateInputs = () -> {
            String title = etTaskName.getText().toString().trim();
            boolean isReady = !title.isEmpty() && !editSelectedTime[0].isEmpty();
            btnSaveEdit.setEnabled(isReady);
            btnSaveEdit.setBackgroundTintList(ColorStateList.valueOf(isReady ? Color.parseColor("#1D1A9B") : Color.parseColor("#D9D9D9")));
        };

        etTaskName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateInputs.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        ivClose.setOnClickListener(v -> dialog.dismiss());

        btnSelectCategory.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), btnSelectCategory);
            popup.getMenu().add("Donate");
            popup.getMenu().add("Bệnh án");
            popup.getMenu().add("Tiêm chủng");
            popup.getMenu().add("Nhận nuôi");
            popup.getMenu().add("Lịch");

            popup.setOnMenuItemClickListener(item -> {
                editSelectedType[0] = item.getTitle().toString();
                tvTaskCategory.setText(editSelectedType[0]);
                switch (editSelectedType[0]) {
                    case "Donate": ivTaskCategory.setImageResource(R.drawable.ic_wallet); break;
                    case "Bệnh án": ivTaskCategory.setImageResource(R.drawable.ic_medical); break;
                    case "Tiêm chủng": ivTaskCategory.setImageResource(R.drawable.ic_vaccine); break;
                    case "Nhận nuôi": ivTaskCategory.setImageResource(R.drawable.ic_heart_fill); break;
                    case "Lịch": ivTaskCategory.setImageResource(R.drawable.ic_calendar); break;
                }
                return true;
            });
            popup.show();
        });

        btnSelectDate.setOnClickListener(v -> DatePickerUtils.show(this, tvTaskDate, null, date -> {
            try {
                Date parsed = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(date);
                if (parsed != null) {
                    Calendar tempCal = Calendar.getInstance();
                    tempCal.setTime(parsed);
                    editCalendar.set(Calendar.YEAR, tempCal.get(Calendar.YEAR));
                    editCalendar.set(Calendar.MONTH, tempCal.get(Calendar.MONTH));
                    editCalendar.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH));
                }
            } catch (Exception ignored) {}
            validateInputs.run();
        }));

        btnSelectTime.setOnClickListener(v -> {
            int h = editCalendar.get(Calendar.HOUR_OF_DAY);
            int m = editCalendar.get(Calendar.MINUTE);
            try {
                String[] parts = editSelectedTime[0].split(":");
                h = Integer.parseInt(parts[0]);
                m = Integer.parseInt(parts[1].split(" ")[0]);
            } catch (Exception ignored){}

            TimePickerDialog timePickerDialog = new TimePickerDialog(getLocalizedContext(requireContext()), R.style.BlueDialogTheme, (tp, hourOfDay, minute) -> {
                editSelectedTime[0] = String.format(Locale.US, "%02d:%02d", hourOfDay, minute);
                tvTaskTime.setText(editSelectedTime[0]);
                tvTaskTime.setTextColor(Color.BLACK);
                editCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                editCalendar.set(Calendar.MINUTE, minute);
                validateInputs.run();
            }, h, m, true);

            timePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", timePickerDialog);
            timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HỦY", (d, which) -> d.dismiss());

            timePickerDialog.setOnShowListener(dialogInterface -> {
                Window window = timePickerDialog.getWindow();
                if (window != null) {
                    GradientDrawable shape = new GradientDrawable();
                    shape.setCornerRadius(40f);
                    shape.setColor(Color.WHITE);
                    InsetDrawable insetDrawable = new InsetDrawable(shape, 40);
                    window.setBackgroundDrawable(insetDrawable);
                }
                Button posBtn = timePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                Button negBtn = timePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                if (posBtn != null) posBtn.setTextColor(Color.parseColor("#1D1A9B"));
                if (negBtn != null) negBtn.setTextColor(Color.parseColor("#1D1A9B"));
            });
            timePickerDialog.show();
        });

        btnSaveEdit.setOnClickListener(v -> {
            String newTitle = etTaskName.getText().toString().trim();
            String newDesc = etTaskNote.getText().toString().trim();
            String newDate = tvTaskDate.getText().toString().trim();

            cancelNotification(task);

            task.setTitle(newTitle);
            task.setDescription(newDesc);
            task.setType(editSelectedType[0]);
            task.setTime(editSelectedTime[0]);
            task.setDate(newDate);
            task.setTimestamp(editCalendar.getTimeInMillis());

            scheduleRef.child(task.getId()).setValue(task).addOnCompleteListener(t -> {
                if (t.isSuccessful()) {
                    scheduleNotification(task);
                    Toast.makeText(requireContext(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(requireContext(), "Lỗi khi cập nhật", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void scheduleNotification(ScheduleTask task) {
        try {
            Calendar notifyTime = Calendar.getInstance();
            notifyTime.setTimeInMillis(task.getTimestamp());

            // --- ÉP SANG HỆ 24 GIỜ (Cho các notification) ---
            String timeStr = task.getTime() != null ? task.getTime() : "00:00";
            if (timeStr.toLowerCase().contains("m")) {
                try {
                    SimpleDateFormat inFmt = new SimpleDateFormat("hh:mm a", Locale.US);
                    SimpleDateFormat outFmt = new SimpleDateFormat("HH:mm", Locale.US);
                    timeStr = outFmt.format(inFmt.parse(timeStr));
                } catch (Exception ignored) {}
            }

            String[] timeParts = timeStr.split("[: ]");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);

            notifyTime.set(Calendar.HOUR_OF_DAY, hour);
            notifyTime.set(Calendar.MINUTE, minute);
            notifyTime.set(Calendar.SECOND, 0);

            notifyTime.add(Calendar.MINUTE, -15);

            String notifTitle = (task.getTitle() != null && !task.getTitle().trim().isEmpty())
                    ? task.getTitle() : (task.getType() != null ? task.getType() : "Lịch trình sắp tới");

            // Cập nhật lại nội dung nhắc nhở
            String notifDesc = (task.getDescription() != null && !task.getDescription().trim().isEmpty())
                    ? task.getDescription() : "Còn 15 phút nữa là đến lịch " + task.getType() + " lúc " + timeStr;

            Intent intent = new Intent(requireContext(), ScheduleNotificationReceiver.class);
            intent.putExtra("task_title", notifTitle);
            intent.putExtra("task_desc", notifDesc);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    task.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, notifyTime.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Context getLocalizedContext(Context context) {
        Locale locale = new Locale("vi", "VN");
        Locale.setDefault(locale);
        if (context != null) {
            Resources res = context.getResources();
            Configuration config = new Configuration(res.getConfiguration());
            config.setLocale(locale);
            res.updateConfiguration(config, res.getDisplayMetrics());
        }
        return context;
    }

    private void cancelNotification(ScheduleTask task) {
        if (task.getId() == null) return;
        Intent intent = new Intent(requireContext(), ScheduleNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                task.getId().hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    private void saveTaskToFirebase(ScheduleTask task, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String dateKey = sdf.format(date);
        task.setDate(dateKey);
        String taskId = scheduleRef.child(dateKey).push().getKey();
        if (taskId == null) return;
        task.setId(taskId);
        scheduleRef.child(dateKey).child(taskId).setValue(task).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Đã thêm lịch trình!", Toast.LENGTH_SHORT).show();
            scheduleNotification(task);
        });
    }

    // ========================================================================
    // CÁC HÀM SETUP VÀ LOAD DỮ LIỆU
    // ========================================================================
    private void setupCalendar() {
        if (selectedViewDate == null) {
            selectedViewDate = Calendar.getInstance().getTime();
        }

        dateList.clear();
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedViewDate);

        Calendar todayCal = Calendar.getInstance();
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(selectedViewDate);

        cal.set(Calendar.DAY_OF_MONTH, 1);

        String monthText = "tháng " + (cal.get(Calendar.MONTH) + 1) + " " + cal.get(Calendar.YEAR);
        binding.tvCurrentMonth.setText(monthText);

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int selectedPos = 0;

        for (int i = 1; i <= daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i);

            boolean isSelected = (cal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH));

            if (isSelected) {
                selectedPos = i - 1;
            }

            boolean isTodayReal = (cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR));
            boolean isPast = cal.before(todayCal) && !isTodayReal;

            dateList.add(new CalendarDateModel(cal.getTime(), isSelected, isPast));
        }

        calendarAdapter = new CalendarAdapter(dateList);
        calendarAdapter.setOnDateClickListener(date -> {
            selectedViewDate = date;
            loadScheduleForDate(date);
        });
        binding.rvCalendar.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCalendar.setAdapter(calendarAdapter);

        binding.rvCalendar.scrollToPosition(selectedPos);
    }

    private void setupScheduleRecyclerView() {
        scheduleAdapter = new AdminScheduleAdapter(taskList, getContext());
        binding.rvSchedule.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvSchedule.setAdapter(scheduleAdapter);

        // MỞ HỘP THOẠI KHI CLICK VÀO ITEM
        scheduleAdapter.setOnItemClickListener(task -> {
            showEditScheduleDialog(task);
        });
    }

    private void loadScheduleForDate(Date date) {
        if (currentScheduleListener != null && currentScheduleRef != null) {
            currentScheduleRef.removeEventListener(currentScheduleListener);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        String dateStr = sdf.format(date);

        SimpleDateFormat displaySdf = new SimpleDateFormat("dd MMMM, yyyy", new Locale("vi", "VN"));
        binding.tvScheduleTitle.setText("Lịch trình " + displaySdf.format(date));

        currentScheduleRef = scheduleRef;
        currentScheduleListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                taskList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ScheduleTask task = ds.getValue(ScheduleTask.class);
                    if (task != null && dateStr.equals(task.getDate())) {
                        taskList.add(task);
                    }
                }

                Collections.sort(taskList, (o1, o2) -> Long.compare(o1.getTimestamp(), o2.getTimestamp()));

                scheduleAdapter.notifyDataSetChanged();
                binding.layoutEmpty.setVisibility(taskList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        currentScheduleRef.addValueEventListener(currentScheduleListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentScheduleListener != null && currentScheduleRef != null) {
            currentScheduleRef.removeEventListener(currentScheduleListener);
        }
        binding = null;
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}