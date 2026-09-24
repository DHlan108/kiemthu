package com.example.rfidpetrescue.utils;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListPopupWindow;

public class DropdownUtils {
    public interface OnItemSelectedListener {
        void onItemSelected(String item);
    }

    /**
     * Hàm hiển thị Menu thả xuống bo cong
     * @param context: Môi trường hiện tại (requireContext())
     * @param anchor: View để neo cái menu thả xuống (nút bấm)
     * @param items: Mảng dữ liệu hiển thị (String[])
     * @param listener: Lắng nghe hành động người dùng click vào
     */
    public static void showRoundedDropdown(Context context, View anchor, String[] items, OnItemSelectedListener listener) {
        ListPopupWindow listPopupWindow = new ListPopupWindow(context);
        listPopupWindow.setAnchorView(anchor);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);
        listPopupWindow.setAdapter(adapter);

        // Thiết lập background bo cong
        GradientDrawable roundedBackground = new GradientDrawable();
        roundedBackground.setColor(Color.WHITE);
        roundedBackground.setCornerRadius(24f); // Độ bo cong
        roundedBackground.setStroke(2, Color.parseColor("#E0E0E0")); // Viền xám nhạt

        listPopupWindow.setBackgroundDrawable(roundedBackground);

        // Lắng nghe sự kiện click
        listPopupWindow.setOnItemClickListener((parent, view, position, id) -> {
            String selectedItem = items[position];
            // Trả kết quả về cho màn hình đang gọi nó
            if (listener != null) {
                listener.onItemSelected(selectedItem);
            }
            listPopupWindow.dismiss();
        });

        listPopupWindow.setModal(true);
        listPopupWindow.show();
    }
}