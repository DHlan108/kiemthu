package com.example.rfidpetrescue;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.rfidpetrescue.activities.LoginActivity;
import com.example.rfidpetrescue.activities.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static org.hamcrest.Matchers.allOf;
import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule = new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() {
        // Khởi tạo Intents để kiểm tra việc chuyển màn hình
        Intents.init();
    }

    @After
    public void tearDown() {
        // Giải phóng Intents sau mỗi test case
        Intents.release();
    }

    /**
     * TC01, TC02, TC03: Kiểm tra trạng thái nút Đăng nhập khi thiếu thông tin
     * Nút Đăng nhập phải bị vô hiệu hóa (disabled) nếu 1 trong 2 trường trống
     */
    @Test
    public void testLoginButtonIsDisabled_WhenFieldsAreEmpty() {
        // TC01: Cả 2 đều trống -> Nút đăng nhập bị disable
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())));

        // TC02: Nhập password, bỏ trống email
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())));

        // Reset
        onView(withId(R.id.edtPassword)).perform(replaceText(""), closeSoftKeyboard());

        // TC03: Nhập email, bỏ trống password
        onView(withId(R.id.edtEmail)).perform(replaceText("thuy@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())));
    }

    /**
     * TC04: Đăng nhập với tài khoản (email) không tồn tại
     */
    @Test
    public void testLoginFailed_WithUnregisteredEmail() throws InterruptedException {
        onView(withId(R.id.edtEmail)).perform(replaceText("abc@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin)).perform(click());

        // Đợi Firebase xử lý (Nên dùng IdlingResource trong thực tế thay vì Thread.sleep)
        Thread.sleep(3000);

        // Nút đăng nhập sẽ được enable lại và đổi text lại thành "Đăng nhập" sau khi thất bại
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()));
    }

    /**
     * TC05: Đăng nhập sai mật khẩu
     */
    @Test
    public void testLoginFailed_WithWrongPassword() throws InterruptedException {
        onView(withId(R.id.edtEmail)).perform(replaceText("thuy@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("654321"), closeSoftKeyboard()); // Sai pass

        onView(withId(R.id.btnLogin)).perform(click());

        Thread.sleep(3000);

        // Nút đăng nhập được enable lại do lỗi
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()));
    }

    /**
     * TC06: Đăng nhập thành công với tài khoản Admin (dữ liệu theo Hình 1 và Hình 2)
     */
    @Test
    public void testLoginSuccess_WithAdminAccount() throws InterruptedException {
        // Nhập email và password hợp lệ (thuy@gmail.com / 123456)
        onView(withId(R.id.edtEmail)).perform(replaceText("thuy@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());

        // Click Đăng nhập
        onView(withId(R.id.btnLogin)).perform(click());

        // Đợi Firebase Auth và Database phản hồi (khoảng 3-5s)
        Thread.sleep(5000);

        // Kiểm tra xem Activity có chuyển sang MainActivity thành công hay không
        intended(hasComponent(MainActivity.class.getName()));
    }
    /**
     * TC07, TC08: Nhập khoảng trắng vào Email hoặc Password[cite: 1]
     * Do mã nguồn có sử dụng trim() nên khoảng trắng sẽ bị biến thành chuỗi rỗng.
     * Kết quả mong đợi: Nút Đăng nhập bị vô hiệu hóa[cite: 1].
     */
    @Test
    public void testLoginButtonIsDisabled_WithOnlySpaces() {
        // TC07: Email chỉ chứa khoảng trắng[cite: 1]
        onView(withId(R.id.edtEmail)).perform(replaceText("   "), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())));

        // Reset dữ liệu
        onView(withId(R.id.edtEmail)).perform(replaceText(""), closeSoftKeyboard());

        // TC08: Password chỉ chứa khoảng trắng[cite: 1]
        onView(withId(R.id.edtEmail)).perform(replaceText("thuy@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("   "), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).check(matches(not(isEnabled())));
    }

    /**
     * TC09, TC10: Chuỗi Email/Password chứa khoảng trắng ở giữa không hợp lệ[cite: 1].
     * Nút bấm vẫn được kích hoạt nhưng Firebase sẽ báo lỗi đăng nhập.
     */
    @Test
    public void testLoginFailed_WithSpacesInside() throws InterruptedException {
        // TC09: Khoảng trắng trong Email[cite: 1]
        onView(withId(R.id.edtEmail)).perform(replaceText("thuy @gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        Thread.sleep(3000);
        onView(withId(R.id.btnLogin)).check(matches(isEnabled())); // Reset lại button khi lỗi
    }

    /**
     * TC11: Email sai định dạng (thiếu @)[cite: 1].
     * Kết quả: Thông báo lỗi từ Firebase, đăng nhập không thành công[cite: 1].
     */
    @Test
    public void testLoginFailed_WithInvalidEmailFormat() throws InterruptedException {
        onView(withId(R.id.edtEmail)).perform(replaceText("thuygmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin)).perform(click());

        Thread.sleep(3000);
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()));
    }

    /**
     * TC12: Tài khoản hợp lệ nhưng không có quyền admin[cite: 1].
     * Dựa theo mã nguồn, ứng dụng vẫn chuyển sang MainActivity nhưng gửi kèm Intent "Is_Admin" = false.
     */
    @Test
    public void testLogin_WithNormalUserAccount() throws InterruptedException {
        // Thay bằng một tài khoản thực tế có role = "user" trên Firebase của bạn
        onView(withId(R.id.edtEmail)).perform(replaceText("user_thuong@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());

        onView(withId(R.id.btnLogin)).perform(click());
        Thread.sleep(5000);

        // Kiểm tra xem Activity có chuyển qua MainActivity với dữ liệu Is_Admin là false hay không
        intended(allOf(
                hasComponent(MainActivity.class.getName()),
                hasExtra("Is_Admin", false)
        ));
    }

    /**
     * TC13: Mất kết nối mạng (Sử dụng UIAutomator để ngắt mạng)[cite: 1].
     * Yêu cầu thêm thư viện UIAutomator.
     */
    @Test
    public void testLoginFailed_WithNoNetwork() throws InterruptedException, IOException {
        UiDevice uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Tắt Wifi và Data
        uiDevice.executeShellCommand("svc wifi disable");
        uiDevice.executeShellCommand("svc data disable");
        Thread.sleep(2000); // Chờ hệ thống ngắt mạng

        onView(withId(R.id.edtEmail)).perform(replaceText("thuy@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.edtPassword)).perform(replaceText("123456"), closeSoftKeyboard());
        onView(withId(R.id.btnLogin)).perform(click());

        Thread.sleep(3000);
        // Kiểm tra nút bật lại do Firebase Exception ném ra khi không có mạng[cite: 1]
        onView(withId(R.id.btnLogin)).check(matches(isEnabled()));

        // Bật lại Wifi để không ảnh hưởng các test khác
        uiDevice.executeShellCommand("svc wifi enable");
        uiDevice.executeShellCommand("svc data enable");
    }

    /**
     * TC14: Chọn Đăng ký[cite: 1].
     * Kết quả mong đợi: Chuyển hướng sang giao diện người dùng mới (SignUpActivity)[cite: 1].
     */
    @Test
    public void testNavigateToSignUp() {
        onView(withId(R.id.tvRegister)).perform(click());

        // Kiểm tra ứng dụng chuyển đúng sang SignUpActivity
        intended(hasComponent(com.example.rfidpetrescue.activities.SignUpActivity.class.getName()));
    }
}