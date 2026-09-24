package com.example.rfidpetrescue.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.rfidpetrescue.models.Notification;
import com.example.rfidpetrescue.models.ScheduleTask;
import com.example.rfidpetrescue.models.Vaccination;
import com.example.rfidpetrescue.fragments.admin.category.schedule.receivers.ScheduleNotificationReceiver;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class quản lý toàn bộ logic:
 * 1. Thêm/cập nhật lịch tái chủng vào Admin_Schedules
 * 2. Ghi thông báo vào Admin_Notifications
 * 3. Đặt AlarmManager để push notification thiết bị khi đến ngày
 *
 * Dùng chung cho AddVaccinationFragment và EditVaccinationFragment.
 */
public class VaccinationScheduleHelper {

    private static final String TAG = "VaccScheduleHelper";

    // ─── Múi giờ thông báo mặc định (9:00 sáng ngày tái chủng) ──────────────
    private static final int NOTIFY_HOUR   = 9;
    private static final int NOTIFY_MINUTE = 0;

    /**
     * Tạo mới lịch tái chủng.
     * Gọi sau khi lưu Vaccination thành công (Add).
     *
     * @param context     Context để đặt AlarmManager
     * @param vaccination Bản ghi tiêm chủng vừa lưu
     * @param petName     Tên thú cưng
     */
    public static void createOrUpdate(Context context,
                                      Vaccination vaccination,
                                      String petName) {
        createOrUpdate(context, vaccination, petName, null);
    }

    /**
     * Cập nhật lịch tái chủng (khi Edit thay đổi revaccine_date).
     * Xóa lịch cũ trước nếu cần, rồi ghi lịch mới.
     *
     * @param context        Context để đặt AlarmManager
     * @param vaccination    Bản ghi tiêm chủng đã cập nhật
     * @param petName        Tên thú cưng
     * @param oldRevaccDate  Ngày tái chủng cũ (dd/MM/yyyy), null nếu không đổi ngày
     */
    public static void createOrUpdate(Context context,
                                      Vaccination vaccination,
                                      String petName,
                                      String oldRevaccDate) {
        try {
            SimpleDateFormat displayFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            SimpleDateFormat keyFmt     = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            // Xóa lịch cũ nếu ngày tái chủng bị thay đổi
            if (oldRevaccDate != null && !oldRevaccDate.isEmpty()
                    && !oldRevaccDate.equals(vaccination.getRevaccine_date())) {
                Date oldDate = displayFmt.parse(oldRevaccDate);
                if (oldDate != null) {
                    String oldDateKey = keyFmt.format(oldDate);
                    FirebaseDatabase.getInstance()
                            .getReference("Admin_Schedules")
                            .child(oldDateKey)
                            .child(vaccination.getId())
                            .removeValue();
                    Log.d(TAG, "Đã xóa lịch cũ: " + oldDateKey);
                }
            }

            // Parse ngày tái chủng mới
            String revaccDateStr = vaccination.getRevaccine_date();
            if (revaccDateStr == null || revaccDateStr.isEmpty()) return;

            Date revaccDate = displayFmt.parse(revaccDateStr);
            if (revaccDate == null) return;

            String dateKey = keyFmt.format(revaccDate);

            // ── 1. Lưu ScheduleTask vào Admin_Schedules ──────────────────────
            ScheduleTask task = buildScheduleTask(vaccination, petName, dateKey);
            FirebaseDatabase.getInstance()
                    .getReference("Admin_Schedules")
                    .child(dateKey)
                    .child(vaccination.getId())
                    .setValue(task)
                    .addOnSuccessListener(unused ->
                            Log.d(TAG, "Đã lưu lịch tái chủng: " + dateKey))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Lỗi lưu lịch: " + e.getMessage()));

            // ── 2. Ghi Admin_Notifications ────────────────────────────────────
            writeAdminNotification(vaccination, petName, revaccDateStr, dateKey);

            // ── 3. Đặt AlarmManager (push notification thiết bị) ─────────────
            if (context != null) {
                scheduleDeviceAlarm(context, vaccination, petName, revaccDate);
            }

        } catch (Exception e) {
            Log.e(TAG, "createOrUpdate error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Tạo ScheduleTask từ thông tin vaccination */
    private static ScheduleTask buildScheduleTask(Vaccination vaccination,
                                                  String petName,
                                                  String dateKey) {
        ScheduleTask task = new ScheduleTask();
        task.setId(vaccination.getId());
        task.setType("Tiêm chủng");
        task.setTitle("Tái chủng: " + vaccination.getVaccine_type());
        task.setDescription("Loại thuốc: " + vaccination.getMedicine_name()
                + (vaccination.getNote() != null && !vaccination.getNote().isEmpty()
                ? " | Ghi chú: " + vaccination.getNote() : ""));
        task.setTime(String.format(Locale.US, "%02d:%02d %s",
                NOTIFY_HOUR > 12 ? NOTIFY_HOUR - 12 : NOTIFY_HOUR,
                NOTIFY_MINUTE,
                NOTIFY_HOUR < 12 ? "am" : "pm"));
        task.setDate(dateKey);
        task.setPetId(vaccination.getPet_id());
        task.setPetName(petName != null ? petName : "");
        task.setVaccineType(vaccination.getMedicine_name());
        return task;
    }

    /**
     * Ghi thông báo vào Firebase node "Admin_Notifications".
     * Thông báo được ghi ngay khi admin chọn ngày tái chủng,
     * và một thông báo nhắc lịch trước 1 ngày sẽ được tạo thêm.
     */
    private static void writeAdminNotification(Vaccination vaccination,
                                               String petName,
                                               String revaccDateDisplay,
                                               String revaccDateKey) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance()
                .getReference("Admin_Notifications");

        // Thông báo 1: Xác nhận lịch tái chủng đã được tạo
        String notifId1 = "revaccine_" + vaccination.getId();
        Notification confirmNotif = new Notification();
        confirmNotif.setTitle("📅 Lịch tái chủng đã được tạo");
        confirmNotif.setMessage(
                "Thú cưng: " + (petName != null ? petName : "Không rõ")
                        + "\nLoại tiêm: " + vaccination.getVaccine_type()
                        + " | Thuốc: " + vaccination.getMedicine_name()
                        + "\nNgày tái chủng: " + revaccDateDisplay);
        confirmNotif.setTimestamp(System.currentTimeMillis());
        confirmNotif.setRead(false);
        confirmNotif.setType("vaccine");

        notifRef.child(notifId1).setValue(confirmNotif)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Đã ghi thông báo xác nhận"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Lỗi ghi thông báo: " + e.getMessage()));

        // Thông báo 2: Nhắc trước 1 ngày (lưu timestamp để hiển thị đúng lúc)
        try {
            SimpleDateFormat keyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date revaccDate = keyFmt.parse(revaccDateKey);
            if (revaccDate != null) {
                Calendar reminderCal = Calendar.getInstance();
                reminderCal.setTime(revaccDate);
                reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                reminderCal.set(Calendar.HOUR_OF_DAY, NOTIFY_HOUR);
                reminderCal.set(Calendar.MINUTE, NOTIFY_MINUTE);

                String notifId2 = "revaccine_remind_" + vaccination.getId();
                Notification remindNotif = new Notification();
                remindNotif.setTitle("⏰ Nhắc nhở: Tái chủng vào ngày mai");
                remindNotif.setMessage(
                        "Thú cưng: " + (petName != null ? petName : "Không rõ")
                                + "\nLoại tiêm: " + vaccination.getVaccine_type()
                                + " | Thuốc: " + vaccination.getMedicine_name()
                                + "\nNgày tái chủng: " + revaccDateDisplay);
                remindNotif.setTimestamp(reminderCal.getTimeInMillis());
                remindNotif.setRead(false);
                remindNotif.setType("vaccine");

                notifRef.child(notifId2).setValue(remindNotif)
                        .addOnSuccessListener(unused ->
                                Log.d(TAG, "Đã ghi thông báo nhắc 1 ngày trước"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Lỗi ghi thông báo nhắc: " + e.getMessage()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi tạo thông báo nhắc: " + e.getMessage());
        }
    }

    /**
     * Đặt AlarmManager để hiện push notification trên thiết bị vào:
     * - 9:00 sáng ngày tái chủng
     * - 9:00 sáng ngày hôm trước (nhắc trước 1 ngày)
     */
    private static void scheduleDeviceAlarm(Context context,
                                            Vaccination vaccination,
                                            String petName,
                                            Date revaccDate) {
        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        String titleStr   = "💉 Tái chủng hôm nay";
        String descStr    = (petName != null ? petName : "Thú cưng")
                + " cần " + vaccination.getVaccine_type()
                + " (" + vaccination.getMedicine_name() + ")";

        String titleRemind = "⏰ Nhắc: Tái chủng vào ngày mai";
        String descRemind  = (petName != null ? petName : "Thú cưng")
                + " cần tái chủng " + vaccination.getVaccine_type()
                + " vào ngày mai";

        // ── Alarm ngày tái chủng ──────────────────────────────────────────────
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.setTime(revaccDate);
        alarmCal.set(Calendar.HOUR_OF_DAY, NOTIFY_HOUR);
        alarmCal.set(Calendar.MINUTE, NOTIFY_MINUTE);
        alarmCal.set(Calendar.SECOND, 0);

        if (alarmCal.getTimeInMillis() > System.currentTimeMillis()) {
            setAlarm(context, alarmManager, vaccination.getId().hashCode(),
                    titleStr, descStr, alarmCal.getTimeInMillis());
        }

        // ── Alarm nhắc trước 1 ngày ───────────────────────────────────────────
        Calendar reminderCal = Calendar.getInstance();
        reminderCal.setTime(revaccDate);
        reminderCal.add(Calendar.DAY_OF_MONTH, -1);
        reminderCal.set(Calendar.HOUR_OF_DAY, NOTIFY_HOUR);
        reminderCal.set(Calendar.MINUTE, NOTIFY_MINUTE);
        reminderCal.set(Calendar.SECOND, 0);

        if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
            // Dùng hashCode khác để không ghi đè alarm chính
            setAlarm(context, alarmManager, ("remind_" + vaccination.getId()).hashCode(),
                    titleRemind, descRemind, reminderCal.getTimeInMillis());
        }
    }

    /** Đặt một AlarmManager exact */
    private static void setAlarm(Context context, AlarmManager alarmManager,
                                 int requestCode, String title, String desc,
                                 long triggerAtMillis) {
        Intent intent = new Intent(context, ScheduleNotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("desc", desc);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            Log.d(TAG, "Đặt alarm thành công: " + title + " lúc " + triggerAtMillis);
        } catch (SecurityException se) {
            Log.w(TAG, "Không có quyền đặt alarm chính xác, dùng set() thay thế");
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }
}
