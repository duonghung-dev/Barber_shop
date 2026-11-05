
package com.example.barber_shop.ui.barber.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BarberMainActivity extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO HẰNG SỐ
    // ========================================

    private static final String TEXT_NOT_LOADED = "—";

    private static final Locale VIETNAM_LOCALE =
            new Locale("vi", "VN");

    // ========================================
    // 2. KHAI BÁO VIEW - HEADER
    // ========================================

    private TextView txtBarberAvatar;
    private TextView txtBarberName;
    private TextView txtBarberTodayDate;

    private View btnBarberNotifications;

    private View cardBarberNotificationBadge;
    private TextView txtBarberNotificationCount;

    // ========================================
    // 3. KHAI BÁO VIEW - THỐNG KÊ
    // ========================================

    private TextView txtBarberTotalBookingsToday;
    private TextView txtBarberPendingBookingsToday;
    private TextView txtBarberConfirmedBookingsToday;
    private TextView txtBarberCompletedBookingsToday;

    // ========================================
    // 4. KHAI BÁO VIEW - LỊCH LÀM VIỆC
    // ========================================

    private TextView txtBarberWorkStatus;
    private TextView txtBarberShiftTime;

    private View btnBarberViewSchedule;

    // ========================================
    // 5. KHAI BÁO VIEW - LỊCH HẸN SẮP TỚI
    // ========================================

    private TextView txtBarberUpcomingCount;

    private LinearLayout containerBarberUpcomingBookings;

    private View layoutBarberUpcomingEmpty;

    private TextView txtBarberUpcomingEmptyTitle;
    private TextView txtBarberUpcomingEmptyMessage;

    private View btnBarberSeeAllBookings;

    // ========================================
    // 6. KHAI BÁO VIEW - TRUY CẬP NHANH
    // ========================================

    private View cardBarberTodayBookings;
    private View cardBarberAllBookings;
    private View cardBarberAvailability;
    private View cardBarberServices;
    private View cardBarberReviews;
    private View cardBarberStatistics;
    private View cardBarberTimeOff;
    private View cardBarberReschedule;

    // ========================================
    // 7. KHAI BÁO VIEW - BOTTOM NAVIGATION
    // ========================================

    private View navBarberHome;
    private View navBarberBookings;
    private View navBarberSchedule;
    private View navBarberNotifications;
    private View navBarberProfile;

    // ========================================
    // 8. KHAI BÁO VIEW - TRẠNG THÁI MÀN HÌNH
    // ========================================

    private NestedScrollView scrollBarberDashboardContent;

    private ProgressBar progressBarberDashboard;

    private LinearLayout layoutBarberDashboardError;
    private TextView txtBarberDashboardError;
    private View btnBarberDashboardRetry;

    // ========================================
    // 9. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập.
    private SessionManager sessionManager;

    // Ngăn điều hướng Login nhiều lần.
    private boolean isRedirectingToLogin = false;

    // ========================================
    // 10. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ========================================
        // 10.1. KHỞI TẠO SESSION
        // ========================================

        sessionManager = new SessionManager(this);

        // ========================================
        // 10.2. KIỂM TRA PHÂN QUYỀN BARBER
        // ========================================

        // Customer không được truy cập Dashboard Barber.

        if (!hasValidBarberSession()) {

            openLogin();
            return;
        }

        // ========================================
        // 10.3. KÍCH HOẠT EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 10.4. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_barber_main
        );

        // ========================================
        // 10.5. KHỞI TẠO THÀNH PHẦN
        // ========================================

        setupWindowInsets();

        initViews();

        setupListeners();

        setupBottomNavigation();
    }

    // ========================================
    // 11. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {

        super.onResume();

        if (isFinishing() || isRedirectingToLogin) {
            return;
        }

        // ========================================
        // 11.1. KIỂM TRA QUYỀN BARBER
        // ========================================

        if (!hasValidBarberSession()) {

            openLogin();
            return;
        }

        // ========================================
        // 11.2. CẬP NHẬT THÔNG TIN HEADER
        // ========================================

        setupBarberHeader();

        // ========================================
        // 11.3. CẬP NHẬT TRẠNG THÁI DASHBOARD
        // ========================================

        loadDashboard();
    }

    // ========================================
    // 12. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.barberMainRoot
        );

        // Lưu padding ban đầu để tránh cộng dồn.

        final int initialLeft =
                root.getPaddingLeft();

        final int initialTop =
                root.getPaddingTop();

        final int initialRight =
                root.getPaddingRight();

        final int initialBottom =
                root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets insets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            initialLeft + insets.left,
                            initialTop + insets.top,
                            initialRight + insets.right,
                            initialBottom + insets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    // ========================================
    // 13. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // ========================================
        // 13.1. HEADER
        // ========================================

        txtBarberAvatar = findViewById(
                R.id.txtBarberAvatar
        );

        txtBarberName = findViewById(
                R.id.txtBarberName
        );

        txtBarberTodayDate = findViewById(
                R.id.txtBarberTodayDate
        );

        btnBarberNotifications = findViewById(
                R.id.btnBarberNotifications
        );

        cardBarberNotificationBadge = findViewById(
                R.id.cardBarberNotificationBadge
        );

        txtBarberNotificationCount = findViewById(
                R.id.txtBarberNotificationCount
        );

        // ========================================
        // 13.2. THỐNG KÊ
        // ========================================

        txtBarberTotalBookingsToday = findViewById(
                R.id.txtBarberTotalBookingsToday
        );

        txtBarberPendingBookingsToday = findViewById(
                R.id.txtBarberPendingBookingsToday
        );

        txtBarberConfirmedBookingsToday = findViewById(
                R.id.txtBarberConfirmedBookingsToday
        );

        txtBarberCompletedBookingsToday = findViewById(
                R.id.txtBarberCompletedBookingsToday
        );

        // ========================================
        // 13.3. LỊCH LÀM VIỆC
        // ========================================

        txtBarberWorkStatus = findViewById(
                R.id.txtBarberWorkStatus
        );

        txtBarberShiftTime = findViewById(
                R.id.txtBarberShiftTime
        );

        btnBarberViewSchedule = findViewById(
                R.id.btnBarberViewSchedule
        );

        // ========================================
        // 13.4. LỊCH HẸN SẮP TỚI
        // ========================================

        txtBarberUpcomingCount = findViewById(
                R.id.txtBarberUpcomingCount
        );

        containerBarberUpcomingBookings = findViewById(
                R.id.containerBarberUpcomingBookings
        );

        layoutBarberUpcomingEmpty = findViewById(
                R.id.layoutBarberUpcomingEmpty
        );

        txtBarberUpcomingEmptyTitle = findViewById(
                R.id.txtBarberUpcomingEmptyTitle
        );

        txtBarberUpcomingEmptyMessage = findViewById(
                R.id.txtBarberUpcomingEmptyMessage
        );

        btnBarberSeeAllBookings = findViewById(
                R.id.btnBarberSeeAllBookings
        );

        // ========================================
        // 13.5. TRUY CẬP NHANH
        // ========================================

        cardBarberTodayBookings = findViewById(
                R.id.cardBarberTodayBookings
        );

        cardBarberAllBookings = findViewById(
                R.id.cardBarberAllBookings
        );

        cardBarberAvailability = findViewById(
                R.id.cardBarberAvailability
        );

        cardBarberServices = findViewById(
                R.id.cardBarberServices
        );

        cardBarberReviews = findViewById(
                R.id.cardBarberReviews
        );

        cardBarberStatistics = findViewById(
                R.id.cardBarberStatistics
        );

        cardBarberTimeOff = findViewById(
                R.id.cardBarberTimeOff
        );

        cardBarberReschedule = findViewById(
                R.id.cardBarberReschedule
        );

        // ========================================
        // 13.6. BOTTOM NAVIGATION
        // ========================================

        navBarberHome = findViewById(
                R.id.navBarberHome
        );

        navBarberBookings = findViewById(
                R.id.navBarberBookings
        );

        navBarberSchedule = findViewById(
                R.id.navBarberSchedule
        );

        navBarberNotifications = findViewById(
                R.id.navBarberNotifications
        );

        navBarberProfile = findViewById(
                R.id.navBarberProfile
        );

        // ========================================
        // 13.7. TRẠNG THÁI DASHBOARD
        // ========================================

        scrollBarberDashboardContent = findViewById(
                R.id.scrollBarberDashboardContent
        );

        progressBarberDashboard = findViewById(
                R.id.progressBarberDashboard
        );

        layoutBarberDashboardError = findViewById(
                R.id.layoutBarberDashboardError
        );

        txtBarberDashboardError = findViewById(
                R.id.txtBarberDashboardError
        );

        btnBarberDashboardRetry = findViewById(
                R.id.btnBarberDashboardRetry
        );
    }

    // ========================================
    // 14. HIỂN THỊ THÔNG TIN BARBER
    // ========================================

    private void setupBarberHeader() {

        // ========================================
        // 14.1. LẤY TÊN BARBER TỪ SESSION
        // ========================================

        String barberName =
                sessionManager.getUserName();

        if (barberName == null
                || barberName.trim().isEmpty()) {

            barberName = "Barber";
        }

        barberName = barberName.trim();

        // ========================================
        // 14.2. HIỂN THỊ TÊN
        // ========================================

        txtBarberName.setText(barberName);

        // ========================================
        // 14.3. HIỂN THỊ AVATAR
        // ========================================

        txtBarberAvatar.setText(
                getNameInitials(barberName)
        );

        // ========================================
        // 14.4. HIỂN THỊ NGÀY HIỆN TẠI
        // ========================================

        txtBarberTodayDate.setText(
                getTodayDisplayText()
        );
    }

    // ========================================
    // 15. ĐỊNH DẠNG NGÀY HÔM NAY
    // ========================================

    private String getTodayDisplayText() {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE, dd/MM/yyyy",
                        VIETNAM_LOCALE
                );

        String dateText = dateFormat.format(
                new Date()
        );

        // Viết hoa chữ cái đầu của ngày.
        //
        // Ví dụ:
        // "thứ năm, 05/11/2026"
        // -> "Thứ năm, 05/11/2026"

        if (dateText.isEmpty()) {
            return "";
        }

        return dateText.substring(0, 1)
                .toUpperCase(VIETNAM_LOCALE)
                + dateText.substring(1);
    }

    // ========================================
    // 16. LẤY CHỮ CÁI ĐẠI DIỆN
    // ========================================

    private String getNameInitials(String fullName) {

        if (fullName == null
                || fullName.trim().isEmpty()) {

            return "B";
        }

        String[] nameParts =
                fullName.trim().split("\\s+");

        String firstPart =
                nameParts[0];

        String firstInitial =
                firstPart.substring(0, 1);

        if (nameParts.length == 1) {

            return firstInitial.toUpperCase(
                    VIETNAM_LOCALE
            );
        }

        String lastPart =
                nameParts[nameParts.length - 1];

        String lastInitial =
                lastPart.substring(0, 1);

        return (firstInitial + lastInitial)
                .toUpperCase(VIETNAM_LOCALE);
    }

    // ========================================
    // 17. TẢI DỮ LIỆU DASHBOARD
    // ========================================

    private void loadDashboard() {

        // ========================================
        // 17.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureBarberSession()) {
            return;
        }

        // ========================================
        // 17.2. CHUẨN BỊ DỮ LIỆU
        // ========================================

        // Hiện tại BookingRepository mới có
        // chức năng đọc Booking theo Customer.
        //
        // Chưa có phương thức:
        //
        // getBookingsForBarber(context, barberId)
        //
        // Vì vậy, không được lấy lịch hẹn
        // của Customer để làm thống kê Barber.
        //
        // Cũng không tự tạo số liệu demo.
        //
        // Khi phát triển Barber Booking,
        // chúng ta sẽ bổ sung API Repository
        // rồi hoàn thiện phần này.

        showDashboardPendingData();

        // ========================================
        // 17.3. HIỂN THỊ NỘI DUNG
        // ========================================

        showDashboardContent();
    }

    // ========================================
    // 18. HIỂN THỊ DỮ LIỆU CHƯA KẾT NỐI
    // ========================================

    private void showDashboardPendingData() {

        // ========================================
        // 18.1. THỐNG KÊ BOOKING
        // ========================================

        // Sử dụng dấu "—" thay vì 0.
        //
        // Vì 0 có nghĩa là đã thống kê
        // và không có lịch hẹn.
        //
        // Trong khi hiện tại chúng ta
        // chưa có nguồn dữ liệu cho Barber.

        txtBarberTotalBookingsToday.setText(
                TEXT_NOT_LOADED
        );

        txtBarberPendingBookingsToday.setText(
                TEXT_NOT_LOADED
        );

        txtBarberConfirmedBookingsToday.setText(
                TEXT_NOT_LOADED
        );

        txtBarberCompletedBookingsToday.setText(
                TEXT_NOT_LOADED
        );

        // ========================================
        // 18.2. LỊCH LÀM VIỆC
        // ========================================

        txtBarberWorkStatus.setText(
                "Chưa có dữ liệu lịch làm việc"
        );

        txtBarberShiftTime.setText(
                "Ca làm việc sẽ được cập nhật khi "
                        + "hoàn thiện chức năng lịch làm việc."
        );

        // ========================================
        // 18.3. LỊCH HẸN SẮP TỚI
        // ========================================

        txtBarberUpcomingCount.setText(
                "Chưa kết nối dữ liệu lịch hẹn Barber"
        );

        containerBarberUpcomingBookings.removeAllViews();

        containerBarberUpcomingBookings.setVisibility(
                View.GONE
        );

        layoutBarberUpcomingEmpty.setVisibility(
                View.VISIBLE
        );

        txtBarberUpcomingEmptyTitle.setText(
                "Chưa kết nối dữ liệu lịch hẹn"
        );

        txtBarberUpcomingEmptyMessage.setText(
                "Danh sách lịch hẹn sẽ được hiển thị "
                        + "sau khi hoàn thiện Booking "
                        + "Repository dành cho Barber."
        );

        // ========================================
        // 18.4. THÔNG BÁO CHƯA ĐỌC
        // ========================================

        // Chưa có Notification Repository
        // dành cho Barber.
        //
        // Không tự hiển thị số 0 vì chưa
        // thực hiện truy vấn thông báo.

        txtBarberNotificationCount.setText("0");

        cardBarberNotificationBadge.setVisibility(
                View.GONE
        );
    }

    // ========================================
    // 19. HIỂN THỊ NỘI DUNG DASHBOARD
    // ========================================

    private void showDashboardContent() {

        progressBarberDashboard.setVisibility(
                View.GONE
        );

        layoutBarberDashboardError.setVisibility(
                View.GONE
        );

        scrollBarberDashboardContent.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 20. THIẾT LẬP CÁC NÚT DASHBOARD
    // ========================================

    private void setupListeners() {

        // ========================================
        // 20.1. NÚT THÔNG BÁO HEADER
        // ========================================

        btnBarberNotifications.setOnClickListener(
                v -> openNotifications()
        );

        // ========================================
        // 20.2. NÚT XEM LỊCH LÀM VIỆC
        // ========================================

        btnBarberViewSchedule.setOnClickListener(
                v -> openSchedule()
        );

        // ========================================
        // 20.3. NÚT XEM TẤT CẢ BOOKING
        // ========================================

        btnBarberSeeAllBookings.setOnClickListener(
                v -> openAllBookings()
        );

        // ========================================
        // 20.4. LỊCH HẸN HÔM NAY
        // ========================================

        cardBarberTodayBookings.setOnClickListener(
                v -> openTodayBookings()
        );

        // ========================================
        // 20.5. TẤT CẢ LỊCH HẸN
        // ========================================

        cardBarberAllBookings.setOnClickListener(
                v -> openAllBookings()
        );

        // ========================================
        // 20.6. QUẢN LÝ THỜI GIAN RẢNH
        // ========================================

        cardBarberAvailability.setOnClickListener(
                v -> openAvailability()
        );

        // ========================================
        // 20.7. DỊCH VỤ CỦA TÔI
        // ========================================

        cardBarberServices.setOnClickListener(
                v -> openServices()
        );

        // ========================================
        // 20.8. ĐÁNH GIÁ
        // ========================================

        cardBarberReviews.setOnClickListener(
                v -> openReviews()
        );

        // ========================================
        // 20.9. THỐNG KÊ
        // ========================================

        cardBarberStatistics.setOnClickListener(
                v -> openStatistics()
        );

        // ========================================
        // 20.10. ĐĂNG KÝ NGHỈ
        // ========================================

        cardBarberTimeOff.setOnClickListener(
                v -> openTimeOff()
        );

        // ========================================
        // 20.11. ĐỀ XUẤT ĐỔI LỊCH
        // ========================================

        cardBarberReschedule.setOnClickListener(
                v -> openRescheduleRequest()
        );

        // ========================================
        // 20.12. THỬ TẢI LẠI DASHBOARD
        // ========================================

        btnBarberDashboardRetry.setOnClickListener(
                v -> loadDashboard()
        );
    }

    // ========================================
    // 21. THIẾT LẬP BOTTOM NAVIGATION
    // ========================================

    private void setupBottomNavigation() {

        // ========================================
        // 21.1. TRANG CHỦ
        // ========================================

        navBarberHome.setOnClickListener(v -> {

            if (!ensureBarberSession()) {
                return;
            }

            // Đang ở Dashboard:
            // Cuộn trở lại đầu trang.

            scrollBarberDashboardContent.smoothScrollTo(
                    0,
                    0
            );

            setupBarberHeader();
        });

        // ========================================
        // 21.2. LỊCH HẸN
        // ========================================

        navBarberBookings.setOnClickListener(
                v -> openAllBookings()
        );

        // ========================================
        // 21.3. LỊCH LÀM VIỆC
        // ========================================

        navBarberSchedule.setOnClickListener(
                v -> openSchedule()
        );

        // ========================================
        // 21.4. THÔNG BÁO
        // ========================================

        navBarberNotifications.setOnClickListener(
                v -> openNotifications()
        );

        // ========================================
        // 21.5. HỒ SƠ BARBER
        // ========================================

        navBarberProfile.setOnClickListener(
                v -> openProfile()
        );
    }

    // ========================================
    // 22. MỞ LỊCH HẸN HÔM NAY
    // ========================================

    private void openTodayBookings() {

        openPendingScreen(
                "Lịch hẹn hôm nay"
        );

        // Kết nối BarberTodayBookingActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 23. MỞ TẤT CẢ LỊCH HẸN
    // ========================================

    private void openAllBookings() {

        openPendingScreen(
                "Tất cả lịch hẹn"
        );

        // Kết nối BarberBookingActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 24. MỞ LỊCH LÀM VIỆC
    // ========================================

    private void openSchedule() {

        openPendingScreen(
                "Lịch làm việc"
        );

        // Kết nối BarberScheduleActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 25. MỞ QUẢN LÝ THỜI GIAN RẢNH
    // ========================================

    private void openAvailability() {

        openPendingScreen(
                "Quản lý thời gian rảnh"
        );

        // Kết nối BarberAvailabilityActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 26. MỞ DỊCH VỤ CỦA TÔI
    // ========================================

    private void openServices() {

        openPendingScreen(
                "Dịch vụ của tôi"
        );

        // Kết nối BarberServiceActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 27. MỞ ĐÁNH GIÁ
    // ========================================

    private void openReviews() {

        openPendingScreen(
                "Đánh giá khách hàng"
        );

        // Kết nối BarberReviewActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 28. MỞ THỐNG KÊ
    // ========================================

    private void openStatistics() {

        openPendingScreen(
                "Thống kê cá nhân"
        );

        // Kết nối BarberStatisticsActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 29. MỞ ĐĂNG KÝ NGHỈ
    // ========================================

    private void openTimeOff() {

        openPendingScreen(
                "Đăng ký nghỉ"
        );

        // Kết nối BarberTimeOffActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 30. MỞ ĐỀ XUẤT ĐỔI LỊCH
    // ========================================

    private void openRescheduleRequest() {

        openPendingScreen(
                "Đề xuất đổi lịch"
        );

        // Kết nối BarberRescheduleRequestActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 31. MỞ THÔNG BÁO
    // ========================================

    private void openNotifications() {

        openPendingScreen(
                "Thông báo Barber"
        );

        // Kết nối BarberNotificationActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 32. MỞ HỒ SƠ BARBER
    // ========================================

    private void openProfile() {

        openPendingScreen(
                "Hồ sơ Barber"
        );

        // Kết nối BarberProfileActivity
        // khi Activity này được tạo.
    }

    // ========================================
    // 33. XỬ LÝ MÀN HÌNH CHƯA TRIỂN KHAI
    // ========================================

    private void openPendingScreen(String screenName) {

        // ========================================
        // 33.1. KIỂM TRA PHÂN QUYỀN
        // ========================================

        if (!ensureBarberSession()) {
            return;
        }

        // ========================================
        // 33.2. THÔNG BÁO TẠM THỜI
        // ========================================

        // Các Activity Barber khác chưa được tạo.
        //
        // Không tham chiếu trực tiếp đến class
        // chưa tồn tại để tránh lỗi biên dịch.
        //
        // Khi xây dựng màn hình tương ứng,
        // thay lời gọi này bằng Intent.

        Toast.makeText(
                this,
                screenName
                        + " sẽ được triển khai ở bước tiếp theo.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ========================================
    // 34. KIỂM TRA PHIÊN BARBER
    // ========================================

    private boolean hasValidBarberSession() {

        // ========================================
        // 34.1. KIỂM TRA ROLE
        // ========================================

        if (!sessionManager.isBarber()) {
            return false;
        }

        // ========================================
        // 34.2. KIỂM TRA USER ID
        // ========================================

        // Một phiên Barber phải có User ID.
        //
        // Sau này, Backend sẽ xác thực
        // và xác định Barber ID tương ứng.

        String userId =
                sessionManager.getUserId();

        return userId != null
                && !userId.trim().isEmpty();
    }

    // ========================================
    // 35. ĐẢM BẢO QUYỀN TRUY CẬP
    // ========================================

    private boolean ensureBarberSession() {

        if (hasValidBarberSession()) {
            return true;
        }

        openLogin();
        return false;
    }

    // ========================================
    // 36. CHUYỂN VỀ LOGIN
    // ========================================

    private void openLogin() {

        // ========================================
        // 36.1. TRÁNH ĐIỀU HƯỚNG TRÙNG
        // ========================================

        if (isRedirectingToLogin) {
            return;
        }

        isRedirectingToLogin = true;

        // ========================================
        // 36.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                BarberMainActivity.this,
                com.example.barber_shop.ui.auth.login.LoginActivity.class
        );

        // ========================================
        // 36.3. XÓA BACK STACK
        // ========================================

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        // ========================================
        // 36.4. MỞ LOGIN
        // ========================================

        startActivity(intent);

        finish();
    }
}
