package com.example.rfidpetrescue.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.adapters.OnboardingPagerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private LinearLayout dotsLayout;
    private Button btnStart;

    private static final int TOTAL_DOTS = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Kiểm tra Onboarding đã hoàn thành chưa
        if (isOnboardingFinished()) {
            checkUserLoginStatus();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        // Ánh xạ
        viewPager = findViewById(R.id.viewPagerOnboarding);
        dotsLayout = findViewById(R.id.layoutDots);
        btnStart = findViewById(R.id.btnStart);

        // Gán adapter
        viewPager.setAdapter(new OnboardingPagerAdapter(this));
        dotsLayout.setVisibility(View.GONE);
        btnStart.setVisibility(View.GONE);

        // Lắng nghe chuyển trang
        viewPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);

                        if (position == 0) {
                            // Fragment 1
                            dotsLayout.setVisibility(View.GONE);
                            btnStart.setVisibility(View.GONE);
                        } else {
                            // Fragment 2 → 5
                            dotsLayout.setVisibility(View.VISIBLE);
                            btnStart.setVisibility(View.VISIBLE);

                            // position 1 → dot 0
                            setupDots(position - 1);
                        }
                    }
                }
        );

        // Click Let’s Start -> Vào thẳng MainActivity (Home) thay vì Login
        btnStart.setOnClickListener(v -> {
            saveOnboardingFinished();
            startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
            finish();
        });
    }

    // Kiểm tra trạng thái đăng nhập
    private void checkUserLoginStatus() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Đã đăng nhập -> Kiểm tra Role và vào MainActivity
            checkUserRoleAndNavigate(currentUser.getUid());
        } else {
            // Chưa đăng nhập nhưng đã xong onboarding -> Vào Home (MainActivity) với tư cách khách
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkUserRoleAndNavigate(String uid) {
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
                        if (snapshot.exists()) {
                            String role = snapshot.getValue(String.class);
                            intent.putExtra("Is_Admin", "admin".equals(role));
                        }
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        startActivity(new Intent(OnboardingActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }

    // ================= SHARED PREF =================

    private void saveOnboardingFinished() {
        SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        prefs.edit().putBoolean("finished", true).apply();
    }

    private boolean isOnboardingFinished() {
        SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        return prefs.getBoolean("finished", false);
    }

    private void setupDots(int activeIndex) {
        dotsLayout.removeAllViews();
        for (int i = 0; i < TOTAL_DOTS; i++) {
            ImageView dot = new ImageView(this);
            dot.setImageResource(i == activeIndex ? R.drawable.dot_active : R.drawable.dot_inactive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dot, params);
        }
    }

    public void goToNextPage() {
        int next = viewPager.getCurrentItem() + 1;
        if (next < 5) {
            viewPager.setCurrentItem(next, true);
        }
    }
}
