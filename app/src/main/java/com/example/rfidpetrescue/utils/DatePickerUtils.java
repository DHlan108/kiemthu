package com.example.rfidpetrescue.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.rfidpetrescue.R;
import com.google.android.material.button.MaterialButton;

import java.util.Calendar;
import java.util.Locale;

public class DatePickerUtils {

    public static final String DATE_FORMAT = "dd/MM/yyyy";
    private static final int COLOR_SELECTED = Color.parseColor("#222222");
    private static final int COLOR_PLACEHOLDER = Color.parseColor("#CCCCCC");
    private static final int COLOR_ACTIVE_BTN = Color.parseColor("#1D1A9B");

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1 & 2 · Chọn ngày đơn giản (form Add / Edit)
    // ─────────────────────────────────────────────────────────────────────────

    public static Context getLocalizedContext(Context context) {
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

    public static void show(Fragment fragment, TextView targetView, Integer offsetDays) {
        show(fragment, targetView, offsetDays, null);
    }

    public static void show(Fragment fragment, TextView targetView, Integer offsetDays, OnDateSelectedListener listener) {
        if (fragment == null || targetView == null || !fragment.isAdded()) return;

        Calendar cal = Calendar.getInstance();
        String existing = targetView.getText().toString().trim();
        if (existing.matches("\\d{2}/\\d{2}/\\d{4}")) {
            try {
                String[] parts = existing.split("/");
                cal.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
            } catch (Exception ignored) {}
        }

        if (offsetDays != null && offsetDays != 0) {
            cal.add(Calendar.DAY_OF_MONTH, offsetDays);
        }

        DatePickerDialog dialog = new DatePickerDialog(
                getLocalizedContext(fragment.requireContext()),
                R.style.BlueDialogTheme,
                (picker, year, month, day) -> {
                    String date = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                    targetView.setText(date);
                    targetView.setTextColor(COLOR_SELECTED);
                    if (listener != null) listener.onDateSelected(date);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", dialog);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HỦY", (d, which) -> d.dismiss());

        dialog.setOnShowListener(d -> {
            // Bo tròn Góc Dialog
            Window window = dialog.getWindow();
            if (window != null) {
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(40f);
                shape.setColor(Color.WHITE);
                InsetDrawable insetDrawable = new InsetDrawable(shape, 40);
                window.setBackgroundDrawable(insetDrawable);
            }

            // Ép màu chữ xanh cho các nút ở chế độ thường để không bị tàng hình
            Button pos = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button neg = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (pos != null) pos.setTextColor(COLOR_ACTIVE_BTN);
            if (neg != null) neg.setTextColor(COLOR_ACTIVE_BTN);
        });

        dialog.show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3 · Filter dialog (Sử dụng nút Native An toàn 100%)
    // ─────────────────────────────────────────────────────────────────────────

    public static void showForFilter(Fragment fragment, TextView tvDateValue, MaterialButton btnSearch) {
        showForFilter(fragment, tvDateValue, btnSearch, null);
    }

    public static void showForFilter(Fragment fragment, TextView tvDateValue, MaterialButton btnSearch, OnDateSelectedListener listener) {
        if (fragment == null || tvDateValue == null || !fragment.isAdded()) return;

        Calendar calendar = Calendar.getInstance();
        String existing = tvDateValue.getText().toString().trim();
        if (existing.matches("\\d{2}/\\d{2}/\\d{4}")) {
            try {
                String[] parts = existing.split("/");
                calendar.set(Integer.parseInt(parts[2]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[0]));
            } catch (Exception ignored) {}
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getLocalizedContext(fragment.requireContext()),
                R.style.BlueDialogTheme,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                    tvDateValue.setText(selectedDate);
                    tvDateValue.setTextColor(COLOR_SELECTED);
                    if (btnSearch != null) btnSearch.setBackgroundTintList(ColorStateList.valueOf(COLOR_ACTIVE_BTN));
                    if (listener != null) listener.onDateSelected(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Khai báo các nút một cách chuẩn mực
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", datePickerDialog);
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "HỦY", (dialog, which) -> dialog.dismiss());
        datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "XÓA", (dialog, which) -> {
            tvDateValue.setText("dd/mm/yy");
            tvDateValue.setTextColor(COLOR_PLACEHOLDER);
            if (btnSearch != null) btnSearch.setBackgroundTintList(ColorStateList.valueOf(COLOR_ACTIVE_BTN));
            if (listener != null) listener.onDateSelected("");
        });

        datePickerDialog.setOnShowListener(dialogInterface -> {
            // Bo tròn Góc Dialog
            Window window = datePickerDialog.getWindow();
            if (window != null) {
                GradientDrawable shape = new GradientDrawable();
                shape.setCornerRadius(40f);
                shape.setColor(Color.WHITE);
                InsetDrawable insetDrawable = new InsetDrawable(shape, 40);
                window.setBackgroundDrawable(insetDrawable);
            }

            // Đổi màu chữ của các nút Filter sang xanh dương để hiện rõ trên nền trắng
            Button posBtn = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negBtn = datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            Button neuBtn = datePickerDialog.getButton(DialogInterface.BUTTON_NEUTRAL);

            if (posBtn != null) posBtn.setTextColor(COLOR_ACTIVE_BTN);
            if (negBtn != null) negBtn.setTextColor(COLOR_ACTIVE_BTN);
            if (neuBtn != null) neuBtn.setTextColor(COLOR_ACTIVE_BTN);
        });

        datePickerDialog.show();
    }
}