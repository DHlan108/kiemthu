package com.example.rfidpetrescue.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.example.rfidpetrescue.R;
import com.example.rfidpetrescue.fragments.admin.AdminHomeFragment;
import com.example.rfidpetrescue.fragments.admin.category.CategoryFragment;
import com.example.rfidpetrescue.fragments.admin.chat.AdminChatFragment;
import com.example.rfidpetrescue.fragments.user.LoveListFragment;
import com.example.rfidpetrescue.fragments.user.chat.ChatFragment;
import com.example.rfidpetrescue.fragments.user.donate.DonateFragment;
import com.example.rfidpetrescue.fragments.user.home.HomeFragment;
import com.example.rfidpetrescue.fragments.profile.AdminProfileFragment;
import com.example.rfidpetrescue.fragments.profile.ProfileFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.BluetoothRfidFragment;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDListener;
import com.example.rfidpetrescue.fragments.admin.scanrfid.rfid.RFIDManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RFIDManager rfidManager;
    private ImageView menuHome, menuFavorite, menuChat, menuProfile, menuDonate;
    private com.google.android.material.imageview.ShapeableImageView btnAdminQR;
    private boolean isAdmin = false;
    private DatabaseReference userStatusRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isAdmin = getIntent().getBooleanExtra("Is_Admin", false);

        if (!isAdmin && FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference("Users").child(uid).child("role")
                    .get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && "admin".equals(String.valueOf(task.getResult().getValue()))) {
                            isAdmin = true;
                            updateUIByRole();
                            openAdminHome();
                        }
                    });
        }

        rfidManager = new RFIDManager();
        initMenuViews();
        setupMenuClicks();
        updateUIByRole();

        if (savedInstanceState == null) {
            selectTab(isAdmin ? 0 : 2);
            if (isAdmin) {
                openAdminHome();
            } else {
                openUserHome();
            }
        }
        // --- KHỞI TẠO CLOUDINARY ---
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dpnqtwqnv");
            config.put("api_key", "354146731588931");
            config.put("api_secret", "P8tYpmOFzXo0TNSBxvkBjAbL84o");

            MediaManager.init(this, config);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            userStatusRef = FirebaseDatabase.getInstance().getReference("Users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("status");
        }
    }

    private void updateStatus(String status) {
        if (userStatusRef != null) {
            userStatusRef.setValue(status);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateStatus("offline");
    }

    private void initMenuViews() {
        menuDonate = findViewById(R.id.btnDonate);
        menuFavorite = findViewById(R.id.btnHeart);
        menuHome = findViewById(R.id.btnHome);
        menuChat = findViewById(R.id.btnChat);
        menuProfile = findViewById(R.id.btnUser);
        btnAdminQR = findViewById(R.id.btnAdminQR);
    }

    private void updateUIByRole() {
        if (isAdmin) {
            menuDonate.setImageResource(R.drawable.ic_home_unactive);
            menuFavorite.setImageResource(R.drawable.ic_grid_unactive);
            btnAdminQR.setVisibility(View.VISIBLE);
            menuHome.setVisibility(View.INVISIBLE);
            btnAdminQR.setOnClickListener(v -> { selectTab(2); openRfidScan(); });
        } else {
            btnAdminQR.setVisibility(View.GONE);
            menuHome.setVisibility(View.VISIBLE);
        }
        selectTab(isAdmin ? 0 : 2);
    }

    private void selectTab(int index) {
        menuHome.setImageResource(R.drawable.ic_home_unactive);
        menuFavorite.setImageResource(isAdmin ? R.drawable.ic_grid_unactive : R.drawable.ic_heart_unactive);
        menuChat.setImageResource(R.drawable.ic_chat_unactive);
        menuProfile.setImageResource(R.drawable.ic_avatar_unactive);
        menuDonate.setImageResource(isAdmin ? R.drawable.ic_home_unactive : R.drawable.ic_donate_unactive);

        int activeColor = isAdmin ? Color.parseColor("#1D1A9B") : Color.parseColor("#FF80AB");

        // Gọi hàm reset (đã được cấu hình để loại trừ nút Donate của User)
        resetIconColors(Color.parseColor("#BCBCBC"));

        switch (index) {
            case 0:
                // --- ĐÃ SỬA CHỖ NÀY ---
                if (isAdmin) {
                    menuDonate.setImageResource(R.drawable.ic_home_active);
                    menuDonate.setColorFilter(activeColor);
                } else {
                    menuDonate.setImageResource(R.drawable.ic_donate_active);
                    menuDonate.clearColorFilter(); // Xóa bộ lọc màu để trả lại hình ảnh thật của Donate
                }
                break;
            case 1:
                if (isAdmin) menuFavorite.setImageResource(R.drawable.ic_grid_active);
                else menuFavorite.setImageResource(R.drawable.ic_heart_active);
                menuFavorite.setColorFilter(activeColor);
                break;
            case 2:
                menuHome.setImageResource(R.drawable.ic_home_active);
                menuHome.setColorFilter(activeColor);
                break;
            case 3:
                menuChat.setImageResource(R.drawable.ic_chat_active);
                menuChat.setColorFilter(activeColor);
                break;
            case 4:
                menuProfile.setImageResource(R.drawable.ic_avatar_active);
                menuProfile.setColorFilter(activeColor);
                break;
        }
    }

    private void resetIconColors(int color) {
        // --- ĐÃ SỬA CHỖ NÀY ---
        if (isAdmin) {
            menuDonate.setColorFilter(color);
        } else {
            // Không phủ màu xám lên nút Donate của User để tránh biến nó thành hình vuông
            menuDonate.clearColorFilter();
        }

        menuFavorite.setColorFilter(color);
        menuHome.setColorFilter(color);
        menuChat.setColorFilter(color);
        menuProfile.setColorFilter(color);
    }

    private void setupMenuClicks() {
        menuDonate.setOnClickListener(v -> {
            if (checkLogin()) {
                selectTab(0);
                if (isAdmin) openAdminHome();
                else openDonate();
            }
        });
        menuFavorite.setOnClickListener(v -> { if (checkLogin()) { selectTab(1); if (isAdmin) openAdminGrid(); else openLoveList(); } });
        menuHome.setOnClickListener(v -> { selectTab(2); openUserHome(); });
        menuChat.setOnClickListener(v -> { if (checkLogin()) { selectTab(3); openChat(); } });
        menuProfile.setOnClickListener(v -> { if (checkLogin()) { selectTab(4); openMyProfile(); } });
    }

    private boolean checkLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    public void updateBottomNavSelection(int menuId) {
        if (menuId == R.id.btnDonate) selectTab(0);
        else if (menuId == R.id.btnHeart) selectTab(1);
        else if (menuId == R.id.btnHome) selectTab(2);
        else if (menuId == R.id.btnChat) selectTab(3);
        else if (menuId == R.id.btnUser) selectTab(4);
    }

    public void openAdminHome() {
        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new AdminHomeFragment()).commit();
    }
    public void openAdminGrid() {
        getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new CategoryFragment()).commit();
    }
    public void openRfidScan() { getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new BluetoothRfidFragment()).addToBackStack(null).commit(); }
    public void openChat() {
        if (isAdmin) {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new AdminChatFragment()).commit();
        } else {
            getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new ChatFragment()).commit();
        }
    }
    public void openUserHome() { getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new HomeFragment()).commit(); }
    public void openDonate() { getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new DonateFragment()).addToBackStack(null).commit(); }
    public void openLoveList() { getSupportFragmentManager().beginTransaction().replace(R.id.main_container, new LoveListFragment()).addToBackStack(null).commit(); }
    public void openMyProfile() { if (isAdmin) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new AdminProfileFragment())
                .addToBackStack(null)
                .commit();
    } else {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, new ProfileFragment())
                .addToBackStack(null)
                .commit();
    } }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (rfidManager != null && rfidManager.processEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    public void setRFIDListener(RFIDListener listener) { if (rfidManager != null) rfidManager.setListener(listener); }

    public void setBottomNavigationVisibility(int visibility) {
        View bottomBarContainer = findViewById(R.id.bottomBarContainer);
        if (bottomBarContainer != null) {
            bottomBarContainer.setVisibility(visibility);
        }
    }
}
