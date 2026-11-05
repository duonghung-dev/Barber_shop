
package com.example.barber_shop.ui.auth.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barber_shop.R;

import com.example.barber_shop.core.session.SessionManager;

import com.example.barber_shop.data.model.User;
import com.example.barber_shop.data.repository.UserRepository;

import com.example.barber_shop.ui.auth.forgotpassword.ForgotPasswordActivity;
import com.example.barber_shop.ui.auth.register.RegisterActivity;

import com.example.barber_shop.ui.customer.home.CustomerHomeActivity;
import com.example.barber_shop.ui.guest.home.GuestHomeActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO HẰNG SỐ
    // ========================================

    private static final String TAG =
            "LoginActivity";

    // ========================================
    // 1.1. TÀI KHOẢN DEMO
    // ========================================

    // Tài khoản này chỉ phục vụ giai đoạn FE.
    //
    // Khi tích hợp Backend:
    // - Không xác thực bằng thông tin hardcode.
    // - Không lưu mật khẩu trong source code.
    // - Đăng nhập sẽ thông qua API.

    private static final String DEMO_CUSTOMER_ID =
            "CUS_DEMO_001";

    private static final String DEMO_CUSTOMER_NAME =
            "Khách hàng demo";

    private static final String DEMO_CUSTOMER_EMAIL =
            "customer.demo@example.com";

    private static final String DEMO_CUSTOMER_PASSWORD =
            "Demo@123";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;

    private TextInputEditText edtEmail;
    private TextInputEditText edtPassword;

    private MaterialButton btnLogin;

    private TextView txtRegister;
    private TextView txtForgotPassword;
    private TextView txtContinueGuest;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Xử lý thao tác Repository trên luồng nền
    private final ExecutorService loginExecutor =
            Executors.newSingleThreadExecutor();

    // Ngăn nhấn nút đăng nhập nhiều lần
    private boolean isLoggingIn = false;

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ========================================
        // 4.1. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_login
        );

        // ========================================
        // 4.2. KHỞI TẠO SESSION
        // ========================================

        sessionManager =
                new SessionManager(this);

        // ========================================
        // 4.3. KHỞI TẠO CÁC THÀNH PHẦN
        // ========================================

        initViews();

        setupListeners();
    }

    // ========================================
    // 5. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        // ========================================
        // 5.1. TEXT INPUT LAYOUT
        // ========================================

        tilEmail = findViewById(
                R.id.tilEmail
        );

        tilPassword = findViewById(
                R.id.tilPassword
        );

        // ========================================
        // 5.2. EDIT TEXT
        // ========================================

        edtEmail = findViewById(
                R.id.edtEmail
        );

        edtPassword = findViewById(
                R.id.edtPassword
        );

        // ========================================
        // 5.3. NÚT ĐĂNG NHẬP
        // ========================================

        btnLogin = findViewById(
                R.id.btnLogin
        );

        // ========================================
        // 5.4. CÁC NÚT ĐIỀU HƯỚNG
        // ========================================

        txtRegister = findViewById(
                R.id.txtRegister
        );

        txtForgotPassword = findViewById(
                R.id.txtForgotPassword
        );

        txtContinueGuest = findViewById(
                R.id.txtContinueGuest
        );
    }

    // ========================================
    // 6. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 6.1. ĐĂNG NHẬP
        // ========================================

        btnLogin.setOnClickListener(
                v -> validateLogin()
        );

        // ========================================
        // 6.2. CHUYỂN SANG REGISTER
        // ========================================

        txtRegister.setOnClickListener(v -> {

            if (isLoggingIn) {
                return;
            }

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);
        });

        // ========================================
        // 6.3. CHUYỂN SANG FORGOT PASSWORD
        // ========================================

        txtForgotPassword.setOnClickListener(v -> {

            if (isLoggingIn) {
                return;
            }

            Intent intent = new Intent(
                    LoginActivity.this,
                    ForgotPasswordActivity.class
            );

            startActivity(intent);
        });

        // ========================================
        // 6.4. TIẾP TỤC VỚI TƯ CÁCH GUEST
        // ========================================

        txtContinueGuest.setOnClickListener(v -> {

            if (isLoggingIn) {
                return;
            }

            openGuestHome();
        });
    }

    // ========================================
    // 7. KIỂM TRA DỮ LIỆU ĐĂNG NHẬP
    // ========================================

    private void validateLogin() {

        // ========================================
        // 7.1. NGĂN ĐĂNG NHẬP LIÊN TIẾP
        // ========================================

        if (isLoggingIn) {
            return;
        }

        // ========================================
        // 7.2. LẤY EMAIL
        // ========================================

        String email =
                edtEmail.getText() == null
                        ? ""
                        : edtEmail.getText()
                        .toString()
                        .trim();

        // ========================================
        // 7.3. LẤY MẬT KHẨU
        // ========================================

        String password =
                edtPassword.getText() == null
                        ? ""
                        : edtPassword.getText()
                        .toString();

        // ========================================
        // 7.4. XÓA THÔNG BÁO LỖI CŨ
        // ========================================

        tilEmail.setError(null);
        tilPassword.setError(null);

        boolean isValid = true;

        // ========================================
        // 7.5. KIỂM TRA EMAIL
        // ========================================

        if (email.isEmpty()) {

            tilEmail.setError(
                    "Vui lòng nhập email"
            );

            isValid = false;

        } else if (!Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            tilEmail.setError(
                    "Email không hợp lệ"
            );

            isValid = false;
        }

        // ========================================
        // 7.6. KIỂM TRA MẬT KHẨU
        // ========================================

        if (password.isEmpty()) {

            tilPassword.setError(
                    "Vui lòng nhập mật khẩu"
            );

            isValid = false;
        }

        // ========================================
        // 7.7. DỪNG NẾU KHÔNG HỢP LỆ
        // ========================================

        if (!isValid) {
            return;
        }

        // ========================================
        // 7.8. XỬ LÝ ĐĂNG NHẬP DEMO
        // ========================================

        handleDemoLogin(
                email,
                password
        );
    }

    // ========================================
    // 8. KIỂM TRA TÀI KHOẢN DEMO
    // ========================================

    private void handleDemoLogin(
            String email,
            String password
    ) {

        // ========================================
        // 8.1. KIỂM TRA THÔNG TIN DEMO
        // ========================================

        boolean isDemoCustomer =
                DEMO_CUSTOMER_EMAIL.equalsIgnoreCase(
                        email
                )
                        && DEMO_CUSTOMER_PASSWORD.equals(
                        password
                );

        // ========================================
        // 8.2. THÔNG TIN KHÔNG KHỚP
        // ========================================

        if (!isDemoCustomer) {

            Toast.makeText(
                    this,
                    "Chưa kết nối Backend. Hãy sử dụng "
                            + "tài khoản demo để kiểm thử.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // ========================================
        // 8.3. BẮT ĐẦU ĐĂNG NHẬP
        // ========================================

        performDemoLogin();
    }

    // ========================================
    // 9. THỰC HIỆN ĐĂNG NHẬP DEMO
    // ========================================

    private void performDemoLogin() {

        // ========================================
        // 9.1. KIỂM TRA TRẠNG THÁI
        // ========================================

        if (isLoggingIn || loginExecutor.isShutdown()) {
            return;
        }

        // ========================================
        // 9.2. KHÓA CÁC NÚT THAO TÁC
        // ========================================

        setLoginLoading(true);

        // ========================================
        // 9.3. LƯU SESSION DEMO BAN ĐẦU
        // ========================================

        // Đây là phiên tạm dùng để xác định
        // Customer đang đăng nhập.
        //
        // Nếu UserRepository đã lưu hồ sơ
        // chỉnh sửa, thông tin này sẽ được
        // đồng bộ lại ngay sau đó.

        boolean isSaved =
                sessionManager.saveSession(
                        DEMO_CUSTOMER_ID,
                        DEMO_CUSTOMER_NAME,
                        DEMO_CUSTOMER_EMAIL,
                        SessionManager.ROLE_CUSTOMER
                );

        // ========================================
        // 9.4. KIỂM TRA SESSION
        // ========================================

        if (!isSaved || !sessionManager.isCustomer()) {

            setLoginLoading(false);

            Toast.makeText(
                    this,
                    "Không thể tạo phiên đăng nhập.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 9.5. ĐỌC HỒ SƠ TRÊN LUỒNG NỀN
        // ========================================

        // Không chuyển ngay đến Customer Home.
        //
        // Trước tiên phải lấy thông tin hồ sơ
        // đã lưu từ UserRepository để tránh
        // hiển thị tên/email demo cũ.

        loginExecutor.execute(() -> {

            UserRepository.UserResult result;

            try {

                // ========================================
                // 9.6. LẤY HOẶC KHỞI TẠO HỒ SƠ
                // ========================================

                // Repository sẽ:
                //
                // 1. Đọc Customer ID từ Session.
                // 2. Tìm hồ sơ đã lưu theo ID.
                // 3. Tạo hồ sơ nếu chưa có.
                // 4. Đồng bộ tên/email vào Session.

                result =
                        UserRepository.getOrCreateCurrentCustomer(
                                getApplicationContext()
                        );

            } catch (RuntimeException exception) {

                // ========================================
                // 9.7. XỬ LÝ LỖI ĐỌC REPOSITORY
                // ========================================

                Log.e(
                        TAG,
                        "Lỗi khi đồng bộ hồ sơ đăng nhập.",
                        exception
                );

                runOnUiThread(() -> {

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    handleLoginFailure(
                            "Không thể tải hồ sơ Customer."
                    );
                });

                return;
            }

            // ========================================
            // 9.8. XỬ LÝ KẾT QUẢ TRÊN MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // ========================================
                // 9.9. KIỂM TRA REPOSITORY
                // ========================================

                if (!result.isSuccess()
                        || result.getUser() == null) {

                    handleLoginFailure(
                            result.getMessage()
                    );

                    return;
                }

                // ========================================
                // 9.10. KIỂM TRA USER
                // ========================================

                User user = result.getUser();

                // Chỉ chấp nhận đúng Customer demo.
                //
                // Không cho phép hồ sơ thuộc
                // User ID hoặc vai trò khác.

                if (!DEMO_CUSTOMER_ID.equals(
                        user.getUserId()
                ) || !user.isCustomer()) {

                    handleLoginFailure(
                            "Hồ sơ Customer không hợp lệ."
                    );

                    return;
                }

                // ========================================
                // 9.11. KIỂM TRA SESSION SAU ĐỒNG BỘ
                // ========================================

                if (!sessionManager.isCustomer()
                        || !DEMO_CUSTOMER_ID.equals(
                        sessionManager.getUserId()
                )) {

                    handleLoginFailure(
                            "Phiên đăng nhập không hợp lệ."
                    );

                    return;
                }

                // ========================================
                // 9.12. ĐĂNG NHẬP THÀNH CÔNG
                // ========================================

                Toast.makeText(
                        this,
                        "Đăng nhập Customer demo thành công",
                        Toast.LENGTH_SHORT
                ).show();

                // ========================================
                // 9.13. ĐIỀU HƯỚNG THEO VAI TRÒ
                // ========================================

                navigateAfterLogin();
            });
        });
    }

    // ========================================
    // 10. XỬ LÝ ĐĂNG NHẬP THẤT BẠI
    // ========================================

    private void handleLoginFailure(
            String message
    ) {

        // ========================================
        // 10.1. XÓA SESSION TẠM
        // ========================================

        // Nếu không thể tải hồ sơ,
        // không giữ phiên Customer chưa hoàn tất.

        sessionManager.logout();

        // ========================================
        // 10.2. MỞ KHÓA CÁC NÚT
        // ========================================

        setLoginLoading(false);

        // ========================================
        // 10.3. HIỂN THỊ THÔNG BÁO
        // ========================================

        if (message == null
                || message.trim().isEmpty()) {

            message = "Đăng nhập không thành công.";
        }

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ========================================
    // 11. CẬP NHẬT TRẠNG THÁI ĐĂNG NHẬP
    // ========================================

    private void setLoginLoading(
            boolean loading
    ) {

        // ========================================
        // 11.1. LƯU TRẠNG THÁI
        // ========================================

        isLoggingIn = loading;

        // ========================================
        // 11.2. NÚT ĐĂNG NHẬP
        // ========================================

        btnLogin.setEnabled(!loading);

        btnLogin.setText(
                loading
                        ? "Đang đăng nhập..."
                        : "Đăng nhập"
        );

        // ========================================
        // 11.3. CÁC NÚT ĐIỀU HƯỚNG
        // ========================================

        txtRegister.setEnabled(!loading);

        txtForgotPassword.setEnabled(!loading);

        txtContinueGuest.setEnabled(!loading);
    }

    // ========================================
    // 12. ĐIỀU HƯỚNG THEO SESSION
    // ========================================

    private void navigateAfterLogin() {

        // ========================================
        // 12.1. KIỂM TRA ĐĂNG NHẬP
        // ========================================

        if (!sessionManager.isLoggedIn()) {

            setLoginLoading(false);
            return;
        }

        // ========================================
        // 12.2. TÀI KHOẢN CUSTOMER
        // ========================================

        if (sessionManager.isCustomer()) {

            openCustomerHome();
            return;
        }

        // ========================================
        // 12.3. CÁC VAI TRÒ CHƯA HỖ TRỢ
        // ========================================

        setLoginLoading(false);

        Toast.makeText(
                this,
                "Vai trò tài khoản chưa được hỗ trợ.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ========================================
    // 13. ĐIỀU HƯỚNG ĐẾN CUSTOMER HOME
    // ========================================

    private void openCustomerHome() {

        // ========================================
        // 13.1. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                LoginActivity.this,
                CustomerHomeActivity.class
        );

        // ========================================
        // 13.2. LẤY TÊN TỪ SESSION ĐÃ ĐỒNG BỘ
        // ========================================

        // Lúc này, UserRepository đã tải
        // hồ sơ và đồng bộ Session.
        //
        // Nếu Customer đã chỉnh sửa họ tên
        // thì đây là tên mới nhất.

        String customerName =
                sessionManager.getUserName();

        // ========================================
        // 13.3. GIỮ TƯƠNG THÍCH CUSTOMER HOME
        // ========================================

        intent.putExtra(
                CustomerHomeActivity.EXTRA_CUSTOMER_NAME,
                customerName
        );

        // ========================================
        // 13.4. XÓA BACK STACK
        // ========================================

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        // ========================================
        // 13.5. MỞ CUSTOMER HOME
        // ========================================

        startActivity(intent);

        finish();
    }

    // ========================================
    // 14. ĐIỀU HƯỚNG ĐẾN GUEST HOME
    // ========================================

    private void openGuestHome() {

        // ========================================
        // 14.1. KHÔNG CHUYỂN KHI ĐANG ĐĂNG NHẬP
        // ========================================

        if (isLoggingIn) {
            return;
        }

        // ========================================
        // 14.2. XÓA SESSION CŨ
        // ========================================

        // Chỉ xóa dữ liệu phiên đăng nhập.
        //
        // Hồ sơ đã lưu trong UserRepository
        // vẫn được giữ nguyên trên thiết bị.

        sessionManager.logout();

        // ========================================
        // 14.3. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                LoginActivity.this,
                GuestHomeActivity.class
        );

        // ========================================
        // 14.4. GIỮ NGUYÊN CỜ ĐIỀU HƯỚNG
        // ========================================

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        // ========================================
        // 14.5. MỞ GUEST HOME
        // ========================================

        startActivity(intent);

        finish();
    }

    // ========================================
    // 15. GIẢI PHÓNG TÀI NGUYÊN
    // ========================================

    @Override
    protected void onDestroy() {

        // Không nhận thêm công việc mới.
        //
        // Lưu ý:
        // Session và Repository là dữ liệu
        // của ứng dụng, không phụ thuộc vào
        // vòng đời của riêng Activity.

        loginExecutor.shutdownNow();

        super.onDestroy();
    }
}
