package com.example.rfidpetrescue.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class BarcodeUtils {

    /**
     * Hàm dùng chung để tạo ảnh mã vạch (Barcode) từ một chuỗi văn bản
     * @param text: Mã RFID hoặc chuỗi cần tạo mã vạch
     * @return Bitmap ảnh mã vạch để gán vào ImageView
     */
    public static Bitmap generateBarcode(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        try {
            com.google.zxing.MultiFormatWriter writer = new com.google.zxing.MultiFormatWriter();
            // Đặt kích thước mặc định là 800x300, bạn có thể chỉnh ở đây để áp dụng cho toàn app
            com.google.zxing.common.BitMatrix bitMatrix =
                    writer.encode(text, com.google.zxing.BarcodeFormat.CODE_128, 800, 300);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}