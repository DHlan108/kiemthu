package com.example.rfidpetrescue.fragments.admin.scanrfid.rfid;

import android.view.KeyEvent;

public class RFIDManager {
    // Dùng StringBuilder để gom từng ký tự số lại
    private StringBuilder codeBuffer = new StringBuilder();
    private RFIDListener listener;

    public RFIDManager() {
    }
    public void setListener(RFIDListener listener) {
        this.listener = listener;
    }

    // Hàm xử lý sự kiện phím bấm từ Activity chuyển sang
    public boolean processEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (codeBuffer.length() > 0) {
                    String finalCode = codeBuffer.toString();

                    if (listener != null) {
                        listener.onTagScanned(finalCode);
                    }
                    codeBuffer.setLength(0);
                    return true;
                }
            } else {
                char charRead = (char) event.getUnicodeChar();
                if (Character.isDigit(charRead)) {
                    codeBuffer.append(charRead);
                    return true;
                }
            }
        }
        return false;
    }
}

