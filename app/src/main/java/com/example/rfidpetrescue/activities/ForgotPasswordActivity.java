package com.example.rfidpetrescue.activities;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rfidpetrescue.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private Button btnResetPassword, btnBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgotpassword);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Ánh xạ view
        etEmail = findViewById(R.id.etEmail);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBackToLogin = findViewById(R.id.btnBackToLogin);

        // Xử lý sự kiện bấm nút Gửi
        btnResetPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Vui lòng nhập email!");
                etEmail.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Định dạng email không hợp lệ!");
                etEmail.requestFocus();
                return;
            }

            // Gọi Firebase gửi email reset
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Liên kết đặt lại mật khẩu đã được gửi!\nVui lòng kiểm tra hộp thư (cả mục Spam).",
                                    Toast.LENGTH_LONG).show();
                            finish(); // Đóng màn hình này, tự động quay lại LoginActivity
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Lỗi: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Xử lý nút quay lại
        btnBackToLogin.setOnClickListener(v -> finish());
    }
}
