package com.example.rfidpetrescue.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.rfidpetrescue.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPassword, edtConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");

        btnRegister.setOnClickListener(v -> registerUser());

        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {

        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if(name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            Toast.makeText(this,"Hãy điền đầy đủ thông tin",Toast.LENGTH_SHORT).show();
            return;
        }

        if(!password.equals(confirmPassword)){
            Toast.makeText(this,"Mật khẩu không khớp",Toast.LENGTH_SHORT).show();
            return;
        }

        if(password.length() < 6){
            Toast.makeText(this,"Mật khẩu phải từ 6 ký tự trở lên",Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {

                    if(task.isSuccessful()){

                        String userId = mAuth.getCurrentUser().getUid();

                        HashMap<String,Object> userMap = new HashMap<>();
                        userMap.put("name",name);
                        userMap.put("email",email);
                        userMap.put("role", "user");
                        database.child(userId).setValue(userMap)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        sendWelcomeNotification(userId, name);
                                        Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                                        finish();
                                    }
                                });

                    }else{

                        Toast.makeText(this,
                                task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();

                    }

                });

    }

    private void sendWelcomeNotification(String userId, String userName) {
        DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("Notifications").child(userId);
        String notifId = notifRef.push().getKey();

        if (notifId != null) {
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("id", notifId);
            notifData.put("title", "Chào mừng bạn!");
            notifData.put("message", "Chào mừng " + userName + " đã gia nhập Cứu hộ SNNC. Chúc bạn có những trải nghiệm tuyệt vời cùng chúng tôi!");
            notifData.put("timestamp", System.currentTimeMillis());
            notifData.put("isRead", false);
            notifData.put("type", "welcome");

            notifRef.child(notifId).setValue(notifData);
        }
    }
}