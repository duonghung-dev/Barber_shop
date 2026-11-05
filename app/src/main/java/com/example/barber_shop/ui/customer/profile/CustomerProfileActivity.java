
package com.example.barber_shop.ui.customer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

import com.example.barber_shop.data.model.User;
import com.example.barber_shop.data.repository.UserRepository;

import com.example.barber_shop.ui.auth.login.LoginActivity;

import com.example.barber_shop.ui.customer.booking.CustomerMyBookingsActivity;
import com.example.barber_shop.ui.customer.booking.CustomerBookingHistoryActivity;

import com.example.barber_shop.ui.customer.barber.CustomerFavoriteBarbersActivity;

import com.example.barber_shop.ui.customer.notification.CustomerNotificationsActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerProfileActivity extends AppCompatActivity {

    // ========================================
    // 1. HẰNG SỐ
    // ========================================

    private static final String TAG =
            "CustomerProfileActivity";

    private static final String TEXT_NOT_UPDATED =
            "Chưa cập nhật";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    // Nút quay lại
    private ImageButton btnCustomerProfileBack;

    // Nội dung hồ sơ
    private NestedScrollView scrollCustomerProfileContent;

    // Thẻ giới thiệu Customer
    private TextView txtCustomerProfileAvatar;
    private TextView txtCustomerProfileName;
    private TextView txtCustomerProfileEmail;

    // Thông tin cá nhân chi tiết
    private TextView txtCustomerProfileFullNameValue;
    private TextView txtCustomerProfileEmailValue;
    private TextView txtCustomerProfilePhoneValue;

    // Nút chỉnh sửa hồ sơ
    private MaterialButton btnCustomerProfileEdit;

    // Các tiện ích tài khoản
    private LinearLayout rowCustomerProfileBookings;
    private LinearLayout rowCustomerProfileHistory;
    private LinearLayout rowCustomerProfileFavorites;
    private LinearLayout rowCustomerProfileNotifications;

    // Nút đăng xuất
    private MaterialButton btnCustomerProfileLogout;

    // Trạng thái tải dữ liệu
    private ProgressBar progressCustomerProfile;

    // Trạng thái lỗi
    private LinearLayout layoutCustomerProfileError;
    private TextView txtCustomerProfileError;
    private MaterialButton btnCustomerProfileRetry;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Thông tin User hiện tại
    private User currentUser;

    // Đọc dữ liệu trên luồng nền
    private final ExecutorService profileExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt các lần tải dữ liệu
    private int loadRequestVersion = 0;

    // Tránh điều hướng Login nhiều lần
    private boolean isRedirectingToLogin = false;

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ========================================
        // 4.1. KHỞI TẠO SESSION
        // ========================================

        sessionManager = new SessionManager(this);

        // ========================================
        // 4.2. KIỂM TRA QUYỀN CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 4.3. KÍCH HOẠT EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 4.4. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_customer_profile
        );

        // ========================================
        // 4.5. KHỞI TẠO CÁC THÀNH PHẦN
        // ========================================

        setupWindowInsets();

        initViews();

        setupListeners();
    }

    // ========================================
    // 5. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        // Lấy Layout gốc của màn hình
        View root = findViewById(
                R.id.customerProfileRoot
        );

        // Lưu Padding ban đầu
        final int initialLeft =
                root.getPaddingLeft();

        final int initialTop =
                root.getPaddingTop();

        final int initialRight =
                root.getPaddingRight();

        final int initialBottom =
                root.getPaddingBottom();

        // Xử lý khoảng cách với thanh trạng thái
        // và thanh điều hướng Android.

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

        // Yêu cầu Android áp dụng Insets
        ViewCompat.requestApplyInsets(root);
    }

    // ========================================
    // 6. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // ========================================
        // 6.1. THANH TIÊU ĐỀ
        // ========================================

        btnCustomerProfileBack = findViewById(
                R.id.btnCustomerProfileBack
        );

        // ========================================
        // 6.2. NỘI DUNG HỒ SƠ
        // ========================================

        scrollCustomerProfileContent = findViewById(
                R.id.scrollCustomerProfileContent
        );

        // ========================================
        // 6.3. THẺ GIỚI THIỆU
        // ========================================

        txtCustomerProfileAvatar = findViewById(
                R.id.txtCustomerProfileAvatar
        );

        txtCustomerProfileName = findViewById(
                R.id.txtCustomerProfileName
        );

        txtCustomerProfileEmail = findViewById(
                R.id.txtCustomerProfileEmail
        );

        // ========================================
        // 6.4. THÔNG TIN CÁ NHÂN
        // ========================================

        txtCustomerProfileFullNameValue = findViewById(
                R.id.txtCustomerProfileFullNameValue
        );

        txtCustomerProfileEmailValue = findViewById(
                R.id.txtCustomerProfileEmailValue
        );

        txtCustomerProfilePhoneValue = findViewById(
                R.id.txtCustomerProfilePhoneValue
        );

        // ========================================
        // 6.5. NÚT CHỈNH SỬA
        // ========================================

        btnCustomerProfileEdit = findViewById(
                R.id.btnCustomerProfileEdit
        );

        // ========================================
        // 6.6. CÁC TIỆN ÍCH TÀI KHOẢN
        // ========================================

        rowCustomerProfileBookings = findViewById(
                R.id.rowCustomerProfileBookings
        );

        rowCustomerProfileHistory = findViewById(
                R.id.rowCustomerProfileHistory
        );

        rowCustomerProfileFavorites = findViewById(
                R.id.rowCustomerProfileFavorites
        );

        rowCustomerProfileNotifications = findViewById(
                R.id.rowCustomerProfileNotifications
        );

        // ========================================
        // 6.7. NÚT ĐĂNG XUẤT
        // ========================================

        btnCustomerProfileLogout = findViewById(
                R.id.btnCustomerProfileLogout
        );

        // ========================================
        // 6.8. LOADING
        // ========================================

        progressCustomerProfile = findViewById(
                R.id.progressCustomerProfile
        );

        // ========================================
        // 6.9. TRẠNG THÁI LỖI
        // ========================================

        layoutCustomerProfileError = findViewById(
                R.id.layoutCustomerProfileError
        );

        txtCustomerProfileError = findViewById(
                R.id.txtCustomerProfileError
        );

        btnCustomerProfileRetry = findViewById(
                R.id.btnCustomerProfileRetry
        );
    }

    // ========================================
    // 7. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 7.1. NÚT QUAY LẠI
        // ========================================

        btnCustomerProfileBack.setOnClickListener(
                v -> finish()
        );

        // ========================================
        // 7.2. NÚT CHỈNH SỬA HỒ SƠ
        // ========================================

        btnCustomerProfileEdit.setOnClickListener(
                v -> openEditProfile()
        );

        // ========================================
        // 7.3. LỊCH HẸN CỦA TÔI
        // ========================================

        rowCustomerProfileBookings.setOnClickListener(
                v -> openMyBookings()
        );

        // ========================================
        // 7.4. LỊCH SỬ ĐẶT LỊCH
        // ========================================

        rowCustomerProfileHistory.setOnClickListener(
                v -> openBookingHistory()
        );

        // ========================================
        // 7.5. BARBER YÊU THÍCH
        // ========================================

        rowCustomerProfileFavorites.setOnClickListener(
                v -> openFavoriteBarbers()
        );

        // ========================================
        // 7.6. THÔNG BÁO
        // ========================================

        rowCustomerProfileNotifications.setOnClickListener(
                v -> openNotifications()
        );

        // ========================================
        // 7.7. NÚT ĐĂNG XUẤT
        // ========================================

        btnCustomerProfileLogout.setOnClickListener(
                v -> showLogoutConfirmation()
        );

        // ========================================
        // 7.8. NÚT THỬ LẠI
        // ========================================

        btnCustomerProfileRetry.setOnClickListener(
                v -> loadUserProfile()
        );
    }

    // ========================================
    // 8. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {

        super.onResume();

        // Không xử lý nếu Activity đang đóng
        if (isFinishing() || isRedirectingToLogin) {

            return;
        }

        // ========================================
        // 8.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 8.2. TẢI LẠI HỒ SƠ
        // ========================================

        // onResume() được gọi khi:
        //
        // - Mở Profile lần đầu.
        // - Quay lại từ Edit Profile.
        // - Quay lại từ màn hình khác.
        //
        // Nhờ đó thông tin hồ sơ luôn
        // được làm mới khi quay lại.

        loadUserProfile();
    }

    // ========================================
    // 9. TẢI THÔNG TIN CUSTOMER
    // ========================================

    private void loadUserProfile() {

        // ========================================
        // 9.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 9.2. KIỂM TRA EXECUTOR
        // ========================================

        if (profileExecutor.isShutdown()) {

            return;
        }

        // ========================================
        // 9.3. ĐÁNH DẤU LẦN TẢI DỮ LIỆU
        // ========================================

        final int requestVersion =
                ++loadRequestVersion;

        // ========================================
        // 9.4. HIỂN THỊ LOADING
        // ========================================

        showLoading();

        // ========================================
        // 9.5. ĐỌC HỒ SƠ TRÊN LUỒNG NỀN
        // ========================================

        profileExecutor.execute(() -> {

            try {

                // UserRepository sẽ:
                //
                // 1. Xác định Customer hiện tại.
                // 2. Tìm User theo Customer ID.
                // 3. Khởi tạo hồ sơ nếu chưa có.
                // 4. Đồng bộ thông tin vào Session.

                UserRepository.UserResult result =
                        UserRepository.getOrCreateCurrentCustomer(
                                getApplicationContext()
                        );

                // ========================================
                // 9.6. TRẢ KẾT QUẢ VỀ MAIN THREAD
                // ========================================

                runOnUiThread(() -> {

                    // Bỏ qua nếu Activity đã đóng
                    if (isFinishing() || isDestroyed()) {

                        return;
                    }

                    // Bỏ qua kết quả cũ
                    if (requestVersion != loadRequestVersion) {

                        return;
                    }

                    // ========================================
                    // 9.7. KIỂM TRA KẾT QUẢ
                    // ========================================

                    if (!result.isSuccess()
                            || result.getUser() == null) {

                        showError(result.getMessage());
                        return;
                    }

                    // ========================================
                    // 9.8. CẬP NHẬT USER HIỆN TẠI
                    // ========================================

                    currentUser = result.getUser();

                    // ========================================
                    // 9.9. HIỂN THỊ HỒ SƠ
                    // ========================================

                    displayUserProfile(currentUser);
                });

            } catch (RuntimeException exception) {

                // ========================================
                // 9.10. XỬ LÝ LỖI NGOÀI DỰ KIẾN
                // ========================================

                Log.e(
                        TAG,
                        "Không thể tải hồ sơ Customer.",
                        exception
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {

                        return;
                    }

                    if (requestVersion != loadRequestVersion) {

                        return;
                    }

                    showError(
                            "Đã xảy ra lỗi khi tải thông tin cá nhân."
                    );
                });
            }
        });
    }

    // ========================================
    // 10. HIỂN THỊ THÔNG TIN USER
    // ========================================

    private void displayUserProfile(User user) {

        if (user == null) {

            showError(
                    "Không tìm thấy thông tin khách hàng."
            );

            return;
        }

        // ========================================
        // 10.1. LẤY THÔNG TIN
        // ========================================

        String fullName = user.getFullName();

        String email = user.getEmail();

        String phoneNumber = user.getPhoneNumber();

        // ========================================
        // 10.2. HIỂN THỊ AVATAR
        // ========================================

        txtCustomerProfileAvatar.setText(
                getNameInitials(fullName)
        );

        // ========================================
        // 10.3. HIỂN THỊ THẺ GIỚI THIỆU
        // ========================================

        txtCustomerProfileName.setText(
                getDisplayValue(fullName)
        );

        txtCustomerProfileEmail.setText(
                getDisplayValue(email)
        );

        // ========================================
        // 10.4. HIỂN THỊ HỌ VÀ TÊN
        // ========================================

        txtCustomerProfileFullNameValue.setText(
                getDisplayValue(fullName)
        );

        // ========================================
        // 10.5. HIỂN THỊ EMAIL
        // ========================================

        txtCustomerProfileEmailValue.setText(
                getDisplayValue(email)
        );

        // ========================================
        // 10.6. HIỂN THỊ SỐ ĐIỆN THOẠI
        // ========================================

        // Nếu chưa có số điện thoại:
        // Hiển thị "Chưa cập nhật".

        txtCustomerProfilePhoneValue.setText(
                getDisplayValue(phoneNumber)
        );

        // ========================================
        // 10.7. HIỂN THỊ NỘI DUNG CHÍNH
        // ========================================

        showContent();
    }

    // ========================================
    // 11. LẤY CHỮ CÁI ĐẠI DIỆN TỪ HỌ TÊN
    // ========================================

    private String getNameInitials(String fullName) {

        // ========================================
        // 11.1. KIỂM TRA HỌ TÊN
        // ========================================

        if (fullName == null
                || fullName.trim().isEmpty()) {

            return "KH";
        }

        // ========================================
        // 11.2. TÁCH HỌ TÊN
        // ========================================

        String[] nameParts =
                fullName.trim().split("\\s+");

        // ========================================
        // 11.3. LẤY CHỮ CÁI ĐẦU
        // ========================================

        String firstNamePart =
                nameParts[0];

        String firstInitial =
                firstNamePart.substring(0, 1);

        // ========================================
        // 11.4. TRƯỜNG HỢP CHỈ CÓ MỘT TỪ
        // ========================================

        if (nameParts.length == 1) {

            return firstInitial.toUpperCase(
                    new Locale("vi", "VN")
            );
        }

        // ========================================
        // 11.5. LẤY CHỮ CÁI ĐẦU CỦA TỪ CUỐI
        // ========================================

        String lastNamePart =
                nameParts[nameParts.length - 1];

        String lastInitial =
                lastNamePart.substring(0, 1);

        // ========================================
        // 11.6. GHÉP HAI CHỮ CÁI
        // ========================================

        // Ví dụ:
        //
        // "Nguyễn Văn A" -> "NA"
        // "Khách hàng demo" -> "KD"

        return (firstInitial + lastInitial)
                .toUpperCase(
                        new Locale("vi", "VN")
                );
    }

    // ========================================
    // 12. XỬ LÝ GIÁ TRỊ HIỂN THỊ
    // ========================================

    private String getDisplayValue(String value) {

        // Nếu dữ liệu null hoặc rỗng:
        // Hiển thị trạng thái chưa cập nhật.

        if (value == null
                || value.trim().isEmpty()) {

            return TEXT_NOT_UPDATED;
        }

        return value.trim();
    }

    // ========================================
    // 13. HIỂN THỊ TRẠNG THÁI LOADING
    // ========================================

    private void showLoading() {

        progressCustomerProfile.setVisibility(
                View.VISIBLE
        );

        scrollCustomerProfileContent.setVisibility(
                View.GONE
        );

        layoutCustomerProfileError.setVisibility(
                View.GONE
        );
    }

    // ========================================
    // 14. HIỂN THỊ NỘI DUNG HỒ SƠ
    // ========================================

    private void showContent() {

        progressCustomerProfile.setVisibility(
                View.GONE
        );

        layoutCustomerProfileError.setVisibility(
                View.GONE
        );

        scrollCustomerProfileContent.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 15. HIỂN THỊ TRẠNG THÁI LỖI
    // ========================================

    private void showError(String message) {

        // ========================================
        // 15.1. ẨN NỘI DUNG
        // ========================================

        progressCustomerProfile.setVisibility(
                View.GONE
        );

        scrollCustomerProfileContent.setVisibility(
                View.GONE
        );

        // ========================================
        // 15.2. HIỂN THỊ THÔNG BÁO LỖI
        // ========================================

        txtCustomerProfileError.setText(
                getErrorMessage(message)
        );

        layoutCustomerProfileError.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 16. CHUẨN HÓA THÔNG BÁO LỖI
    // ========================================

    private String getErrorMessage(String message) {

        if (message == null
                || message.trim().isEmpty()) {

            return "Đã xảy ra lỗi khi tải hồ sơ.";
        }

        return message;
    }

    // ========================================
    // 17. KIỂM TRA QUYỀN TRUY CẬP
    // ========================================

    private boolean ensureCustomerSession() {

        if (sessionManager.isCustomer()) {

            return true;
        }

        // Không cho phép tiếp tục thao tác
        // khi phiên đăng nhập không hợp lệ.

        openLogin();

        return false;
    }

    // ========================================
    // 18. MỞ MÀN HÌNH CHỈNH SỬA HỒ SƠ
    // ========================================

    private void openEditProfile() {

        // ========================================
        // 18.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 18.2. KIỂM TRA HỒ SƠ
        // ========================================

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Vui lòng tải lại hồ sơ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 18.3. MỞ EDIT PROFILE
        // ========================================

        // CustomerEditProfileActivity sẽ được
        // tạo trong bước STT 23.
        //
        // Activity đó tự lấy Customer ID
        // từ SessionManager.
        //
        // Không cần truyền thông tin nhạy cảm
        // hoặc User ID qua Intent.

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                CustomerEditProfileActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 19. MỞ LỊCH HẸN CỦA TÔI
    // ========================================

    private void openMyBookings() {

        // ========================================
        // 19.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 19.2. MỞ CUSTOMER MY BOOKINGS
        // ========================================

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                CustomerMyBookingsActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 20. MỞ LỊCH SỬ ĐẶT LỊCH
    // ========================================

    private void openBookingHistory() {

        // ========================================
        // 20.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 20.2. MỞ BOOKING HISTORY
        // ========================================

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                CustomerBookingHistoryActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 21. MỞ BARBER YÊU THÍCH
    // ========================================

    private void openFavoriteBarbers() {

        // ========================================
        // 21.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 21.2. MỞ FAVORITE BARBERS
        // ========================================

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                CustomerFavoriteBarbersActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 22. MỞ THÔNG BÁO CUSTOMER
    // ========================================

    private void openNotifications() {

        // ========================================
        // 22.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 22.2. MỞ CUSTOMER NOTIFICATIONS
        // ========================================

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                CustomerNotificationsActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 23. HIỂN THỊ XÁC NHẬN ĐĂNG XUẤT
    // ========================================

    private void showLogoutConfirmation() {

        // ========================================
        // 23.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 23.2. HIỂN THỊ DIALOG
        // ========================================

        new MaterialAlertDialogBuilder(this)

                .setTitle(
                        "Đăng xuất"
                )

                .setMessage(
                        "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản?"
                )

                .setNegativeButton(
                        "Hủy",
                        (dialog, which) ->
                                dialog.dismiss()
                )

                .setPositiveButton(
                        "Đăng xuất",
                        (dialog, which) ->
                                performLogout()
                )

                .show();
    }

    // ========================================
    // 24. THỰC HIỆN ĐĂNG XUẤT
    // ========================================

    private void performLogout() {

        // ========================================
        // 24.1. HỦY HIỆU LỰC KẾT QUẢ ĐANG TẢI
        // ========================================

        loadRequestVersion++;

        // ========================================
        // 24.2. XÓA PHIÊN ĐĂNG NHẬP
        // ========================================

        // SessionManager.logout() chỉ xóa
        // SharedPreferences của Session.
        //
        // Không xóa hồ sơ đã lưu trong
        // UserRepository.
        //
        // Khi đăng nhập lại, Customer vẫn
        // có thể sử dụng hồ sơ đã chỉnh sửa.

        sessionManager.logout();

        // ========================================
        // 24.3. XÓA THAM CHIẾU USER
        // ========================================

        currentUser = null;

        // ========================================
        // 24.4. ĐIỀU HƯỚNG VỀ LOGIN
        // ========================================

        openLogin();
    }

    // ========================================
    // 25. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        // ========================================
        // 25.1. TRÁNH ĐIỀU HƯỚNG TRÙNG
        // ========================================

        if (isRedirectingToLogin) {

            return;
        }

        isRedirectingToLogin = true;

        // ========================================
        // 25.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerProfileActivity.this,
                LoginActivity.class
        );

        // ========================================
        // 25.3. XÓA BACK STACK
        // ========================================

        // Không cho phép nhấn Back để quay lại
        // các màn hình Customer sau khi đăng xuất.

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        // ========================================
        // 25.4. MỞ LOGIN
        // ========================================

        startActivity(intent);

        finish();
    }

    // ========================================
    // 26. GIẢI PHÓNG TÀI NGUYÊN
    // ========================================

    @Override
    protected void onDestroy() {

        // ========================================
        // 26.1. HỦY HIỆU LỰC KẾT QUẢ CŨ
        // ========================================

        loadRequestVersion++;

        // ========================================
        // 26.2. DỪNG EXECUTOR
        // ========================================

        profileExecutor.shutdownNow();

        // ========================================
        // 26.3. GỌI LỚP CHA
        // ========================================

        super.onDestroy();
    }
}
