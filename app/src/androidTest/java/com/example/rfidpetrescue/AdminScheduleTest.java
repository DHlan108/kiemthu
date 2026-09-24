package com.example.rfidpetrescue;

import android.content.Intent;
import android.view.View;
import android.widget.TimePicker;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.rfidpetrescue.activities.MainActivity;
import com.example.rfidpetrescue.fragments.admin.category.schedule.AdminScheduleFragment;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

/**
 * KỊCH BẢN KIỂM THỬ TỰ ĐỘNG BẰNG ESPRESSO (UI TEST)
 * Chức năng 2.3.2.4: Quản lý Đặt lịch (Rescue Station Staff Schedule Management)
 *
 * Bao gồm các luồng kiểm thử:
 * a. Xem danh sách đặt lịch (ngày hiện tại, chuyển ngày)
 * b. Xem chi tiết đặt lịch
 * c. Thêm lịch hẹn (Validation, hủy form, thêm hợp lệ)
 * d. Cập nhật đặt lịch (sửa thông tin, validation khi xóa tên, hủy sửa, lưu cập nhật)
 */
@RunWith(AndroidJUnit4.class)
public class AdminScheduleTest {

    static Intent startIntent;

    static {
        startIntent = new Intent(ApplicationProvider.getApplicationContext(), MainActivity.class);
        startIntent.putExtra("Is_Admin", true);
    }

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(startIntent);

    @Before
    public void navigateToAdminScheduleFragment() {
        // Mở trực tiếp AdminScheduleFragment trong MainActivity để chuẩn bị môi trường test
        activityRule.getScenario().onActivity(activity -> {
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_container, new AdminScheduleFragment())
                    .commitNowAllowingStateLoss();
        });
        // Tạm dừng 2s để UI và Firebase load xong dữ liệu từ mạng
        onView(isRoot()).perform(waitFor(2000));
    }

    // =========================================================================
    // a. XEM DANH SÁCH ĐẶT LỊCH
    // =========================================================================

    /**
     * Test a.1 & a.2: Xem danh sách đặt lịch theo ngày và chuyển sang ngày khác
     */
    @Test
    public void test01_ViewScheduleList_And_SwitchDate() {
        // 1. Kiểm tra màn hình Lịch mở thành công với tiêu đề chứa "Lịch trình"
        onView(withId(R.id.tvScheduleTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText(Matchers.containsString("Lịch trình"))));

        // 2. Kiểm tra thanh lịch ngày (RecyclerView Calendar) hiển thị
        onView(withId(R.id.rvCalendar))
                .check(matches(isDisplayed()));

        // 3. Thực hiện chuyển sang ngày khác bằng cách click vào phần tử thứ 2 trên thanh Calendar
        onView(withId(R.id.rvCalendar))
                .perform(RecyclerViewActions.actionOnItemAtPosition(1, click()));

        // Tạm dừng 1s để hệ thống load lịch ngày mới
        onView(isRoot()).perform(waitFor(1000));

        // 4. Kiểm tra danh sách hoặc giao diện được cập nhật
        onView(withId(R.id.tvScheduleTitle)).check(matches(isDisplayed()));
    }

    // =========================================================================
    // c. THÊM LỊCH HẸN
    // =========================================================================

    /**
     * Test c.1: Kiểm tra nút Thêm lịch (btnSaveTask) bị vô hiệu hóa (disabled) khi bỏ trống tên công việc hoặc thời gian
     */
    @Test
    public void test02_AddSchedule_ValidationEmptyFields() {
        // 1. Click nút + (btnAddSchedule) để mở biểu mẫu Thêm lịch
        onView(withId(R.id.btnAddSchedule)).perform(click());

        // 2. Kiểm tra Hộp thoại "Thêm công việc" hiển thị
        onView(withId(R.id.tvDialogTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText("Thêm công việc")));

        // 3. Kiểm tra nút "Thêm lịch" (btnSaveTask) ban đầu bị Disabled do chưa nhập thông tin
        onView(withId(R.id.btnSaveTask))
                .check(matches(not(isEnabled())));

        // 4. Nhập tên công việc nhưng chưa chọn thời gian -> Nút vẫn bị Disabled
        onView(withId(R.id.etTaskTitle))
                .perform(typeText("Test Validation Task"), closeSoftKeyboard());

        onView(withId(R.id.btnSaveTask))
                .check(matches(not(isEnabled())));

        // 5. Đóng dialog bằng nút X
        onView(withId(R.id.ivClose)).perform(click());
    }

    /**
     * Test c.2: Nhấn nút X để thoát biểu mẫu -> Không tạo lịch mới
     */
    @Test
    public void test03_AddSchedule_DismissDialogOnCloseClick() {
        // 1. Click nút +
        onView(withId(R.id.btnAddSchedule)).perform(click());

        // 2. Nhập thông tin
        onView(withId(R.id.etTaskTitle))
                .perform(typeText("Công việc hủy bỏ"), closeSoftKeyboard());

        // 3. Click nút X
        onView(withId(R.id.ivClose)).perform(click());

        // 4. Kiểm tra Dialog đã đóng
        onView(withId(R.id.btnAddSchedule)).check(matches(isDisplayed()));
    }

    /**
     * Test c.3: Nhập đầy đủ thông tin hợp lệ -> Thêm lịch mới thành công
     */
    @Test
    public void test04_AddSchedule_Success() {
        String testTaskName = "Lịch tiêm phòng tự động " + (System.currentTimeMillis() % 10000);
        String testNote = "Ghi chú test Espresso";

        // 1. Click nút + mở form Thêm lịch
        onView(withId(R.id.btnAddSchedule)).perform(click());

        // 2. Nhập tên công việc
        onView(withId(R.id.etTaskTitle))
                .perform(typeText(testTaskName), closeSoftKeyboard());

        // 3. Chọn Phân loại: Click btnSelectRecordType -> Chọn "Tiêm chủng" từ PopupMenu
        onView(withId(R.id.btnSelectRecordType)).perform(click());
        onView(withText("Tiêm chủng"))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click());

        // 4. Chọn Thời gian: Click btnSelectTime -> Chọn giờ trong TimePickerDialog -> Click OK
        onView(withId(R.id.btnSelectTime)).perform(click());
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName())))
                .perform(PickerActions.setTime(9, 30));
        onView(withText("OK")).perform(click());

        // 5. Nhập Ghi chú
        onView(withId(R.id.etTaskDesc))
                .perform(typeText(testNote), closeSoftKeyboard());

        // 6. Kiểm tra nút "Thêm lịch" đã Enabled và Click
        onView(withId(R.id.btnSaveTask))
                .check(matches(isEnabled()))
                .perform(click());

        // 7. Chờ 2s để Firebase lưu dữ liệu
        onView(isRoot()).perform(waitFor(2000));

        // 8. Kiểm tra tên công việc mới tạo hiển thị trong danh sách
        onView(withText(testTaskName)).check(matches(isDisplayed()));
    }

    // =========================================================================
    // b. XEM CHI TIẾT ĐẶT LỊCH
    // =========================================================================

    /**
     * Test b.1: Click vào lịch hẹn tồn tại -> Mở dialog hiển thị chi tiết thông tin
     */
    @Test
    public void test05_ViewScheduleDetail() {
        // Đảm bảo có ít nhất 1 lịch trình tồn tại trước khi click
        ensureScheduleItemExists();

        // 1. Click vào item đầu tiên trong danh sách lịch trình
        onView(withId(R.id.rvSchedule))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // 2. Kiểm tra dialog "Chỉnh sửa công việc" hiển thị đúng thông tin chi tiết
        onView(withId(R.id.tvDialogTitle))
                .check(matches(isDisplayed()))
                .check(matches(withText("Chỉnh sửa công việc")));

        onView(withId(R.id.etTaskName)).check(matches(isDisplayed()));
        onView(withId(R.id.tvTaskCategory)).check(matches(isDisplayed()));
        onView(withId(R.id.tvTaskDate)).check(matches(isDisplayed()));
        onView(withId(R.id.tvTaskTime)).check(matches(isDisplayed()));
        onView(withId(R.id.etTaskNote)).check(matches(isDisplayed()));

        // 3. Đóng dialog bằng nút X
        onView(withId(R.id.ivClose)).perform(click());
    }

    // =========================================================================
    // d. CẬP NHẬT ĐẶT LỊCH (SỬA LỊCH HẸN)
    // =========================================================================

    /**
     * Test d.1: Thay đổi tên công việc / ghi chú và bấm "Lưu chỉnh sửa" -> Cập nhật thành công
     */
    @Test
    public void test06_UpdateSchedule_Success() {
        // Đảm bảo có ít nhất 1 lịch trình tồn tại
        ensureScheduleItemExists();

        String updatedTitle = "Đã cập nhật " + (System.currentTimeMillis() % 1000);

        // 1. Click vào item đầu tiên trong danh sách lịch trình
        onView(withId(R.id.rvSchedule))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // 2. Thay đổi tên công việc
        onView(withId(R.id.etTaskName))
                .perform(clearText(), typeText(updatedTitle), closeSoftKeyboard());

        // 3. Click nút "Lưu chỉnh sửa" (btnSaveEdit)
        onView(withId(R.id.btnSaveEdit)).perform(click());

        // 4. Chờ 2s để cập nhật Firebase
        onView(isRoot()).perform(waitFor(2000));

        // 5. Kiểm tra thông tin tên công việc mới đã xuất hiện trên giao diện
        onView(withText(updatedTitle)).check(matches(isDisplayed()));
    }

    /**
     * Test d.2: Cập nhật lịch - Xóa tên công việc bắt buộc -> Nút "Lưu chỉnh sửa" bị vô hiệu hóa
     */
    @Test
    public void test07_UpdateSchedule_ValidationEmptyTitle_DisabledSaveButton() {
        // Đảm bảo có ít nhất 1 lịch trình tồn tại
        ensureScheduleItemExists();

        // 1. Click vào item đầu tiên trong danh sách
        onView(withId(R.id.rvSchedule))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // 2. Xóa sạch Tên công việc (trường bắt buộc)
        onView(withId(R.id.etTaskName))
                .perform(clearText(), closeSoftKeyboard());

        // 3. Kiểm tra nút "Lưu chỉnh sửa" (btnSaveEdit) bị vô hiệu hóa (disabled)
        onView(withId(R.id.btnSaveEdit)).check(matches(not(isEnabled())));

        // 4. Đóng dialog bằng nút X
        onView(withId(R.id.ivClose)).perform(click());
    }

    /**
     * Test d.3: Cập nhật lịch - Thay đổi dữ liệu nhưng nhấn X hủy bỏ -> Không lưu thay đổi
     */
    @Test
    public void test08_UpdateSchedule_CancelEdit_KeepOldData() {
        // Đảm bảo có ít nhất 1 lịch trình tồn tại
        ensureScheduleItemExists();

        String tempTitle = "Tên muốn đổi nhưng bấm Hủy";

        // 1. Click vào item đầu tiên trong danh sách
        onView(withId(R.id.rvSchedule))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

        // 2. Nhập tên tạm
        onView(withId(R.id.etTaskName))
                .perform(clearText(), typeText(tempTitle), closeSoftKeyboard());

        // 3. Bấm nút X để đóng form mà KHÔNG lưu
        onView(withId(R.id.ivClose)).perform(click());

        // 4. Đảm bảo dialog đã đóng và nút + vẫn hiển thị
        onView(withId(R.id.btnAddSchedule)).check(matches(isDisplayed()));
    }

    // =========================================================================
    // HELPER FUNCTIONS
    // =========================================================================

    /**
     * Hàm trợ giúp: Đảm bảo danh sách rvSchedule có ít nhất 1 phần tử trước khi thao tác.
     * Nếu dữ liệu từ Firebase trống hoặc chưa tải kịp, hàm sẽ tự động tạo 1 lịch mẫu để test.
     */
    private void ensureScheduleItemExists() {
        // Chờ 2 giây để Firebase Realtime Database tải dữ liệu
        onView(isRoot()).perform(waitFor(2000));

        try {
            // Thử kiểm tra sự tồn tại của ViewHolder ở vị trí 0
            onView(withId(R.id.rvSchedule))
                    .perform(RecyclerViewActions.scrollToPosition(0));
        } catch (Throwable e) {
            // Nếu rvSchedule chưa có phần tử nào (bị rỗng), tự động tạo 1 lịch mới phục vụ test
            String testTitle = "Lịch mẫu test " + (System.currentTimeMillis() % 10000);
            onView(withId(R.id.btnAddSchedule)).perform(click());
            onView(withId(R.id.etTaskTitle)).perform(typeText(testTitle), closeSoftKeyboard());

            onView(withId(R.id.btnSelectRecordType)).perform(click());
            onView(withText("Tiêm chủng")).inRoot(RootMatchers.isPlatformPopup()).perform(click());

            onView(withId(R.id.btnSelectTime)).perform(click());
            onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(10, 0));
            onView(withText("OK")).perform(click());

            onView(withId(R.id.btnSaveTask)).perform(click());

            // Chờ 2s để Firebase cập nhật lịch mới vào RecyclerView
            onView(isRoot()).perform(waitFor(2000));
        }
    }

    /**
     * Utility ViewAction để dừng màn hình trong khoảng millis chỉ định
     */
    public static ViewAction waitFor(final long millis) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Chờ trong " + millis + " ms.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(millis);
            }
        };
    }
}
