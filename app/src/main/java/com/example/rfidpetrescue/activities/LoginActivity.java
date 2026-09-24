package com.example.rfidpetrescue.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.rfidpetrescue.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.firebase.auth.*;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView tvRegister, tvForgot;
    private ImageButton btnGoogle, btnFacebook;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private CallbackManager callbackManager;

    private static final int RC_SIGN_IN = 100;
    private boolean isLoggingIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        callbackManager = CallbackManager.Factory.create();
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvForgot = findViewById(R.id.tvForgot);
        btnLogin.setEnabled(false);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkInput();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        };

        edtEmail.addTextChangedListener(textWatcher);
        edtPassword.addTextChangedListener(textWatcher);

        btnLogin.setOnClickListener(v -> {
            if (isLoggingIn) return;
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            isLoggingIn = true;
            btnLogin.setEnabled(false);
            btnLogin.setText("Đang đăng nhập...");

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            checkUserRole(mAuth.getCurrentUser().getUid());
                        } else {
                            isLoggingIn = false;
                            btnLogin.setText("Đăng nhập");
                            checkInput();
                            Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        tvForgot.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                    "com.example.rfidpetrescue", // Thay bằng package name của bạn
                    android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                android.util.Log.d("KeyHash:", android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT));
            }
        } catch (Exception e) { }
        // Đăng ký sự kiện lắng nghe kết quả từ Facebook
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken().getToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Đã hủy đăng nhập Facebook", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Lỗi Facebook: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý khi bấm nút custom Facebook
        btnFacebook.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this, java.util.Arrays.asList("email", "public_profile"));
        });
    }

    private void checkInput() {
        if (isLoggingIn) return;
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        btnLogin.setEnabled(!email.isEmpty() && !password.isEmpty());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Lỗi Google Sign In: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void firebaseAuthWithGoogle(String idToken) {
        isLoggingIn = true;
        btnGoogle.setEnabled(false);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser().getUid());
                    } else {
                        isLoggingIn = false;
                        btnGoogle.setEnabled(true);
                        Toast.makeText(this, "Lỗi Authentication", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // Hàm xác thực Facebook Token với Firebase
    private void handleFacebookAccessToken(String token) {
        isLoggingIn = true;
        btnFacebook.setEnabled(false);
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        checkUserRole(mAuth.getCurrentUser().getUid());
                    } else {
                        isLoggingIn = false;
                        btnFacebook.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    // KIỂM TRA QUYỀN VÀ LƯU THÔNG TIN NGƯỜI DÙNG MỚI
    private void checkUserRole(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                if (snapshot.exists()) {
                    String role = snapshot.child("role").getValue(String.class);
                    intent.putExtra("Is_Admin", "admin".equals(role));
                } else {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        String email = currentUser.getEmail() != null ? currentUser.getEmail() : "";
                        String name = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng mới";

                        // Lấy link ảnh avatar
                        String avatarUrl = "";
                        if (currentUser.getPhotoUrl() != null) {
                            avatarUrl = currentUser.getPhotoUrl().toString();
                            if (avatarUrl.contains("facebook.com")) {
                                if (avatarUrl.contains("?")) {
                                    avatarUrl += "&type=large";
                                } else {
                                    avatarUrl += "?type=large";
                                }

                                com.facebook.AccessToken fbToken = com.facebook.AccessToken.getCurrentAccessToken();
                                if (fbToken != null) {
                                    avatarUrl += "&access_token=" + fbToken.getToken();
                                }
                            }
                        }

                        userRef.child("email").setValue(email);
                        userRef.child("role").setValue("user");
                        userRef.child("name").setValue(name);
                        userRef.child("avatar").setValue(avatarUrl);
                    }
                    intent.putExtra("Is_Admin", false);
                }
                startActivity(intent);
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Lỗi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}