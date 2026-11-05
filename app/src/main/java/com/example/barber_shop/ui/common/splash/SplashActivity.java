
package com.example.barber_shop.ui.common.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.home.CustomerHomeActivity;

public class SplashActivity extends AppCompatActivity {

    // Thời gian hiển thị Splash: 1.2 giây
    private static final long SPLASH_DURATION_MS = 1200L;

    private SessionManager sessionManager;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private final Runnable navigationRunnable =
            this::navigateToNextScreen;

    // ========================================
    // 1. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Khởi tạo Android SplashScreen
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Hiển thị giao diện thương hiệu
        setContentView(R.layout.activity_splash);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Chuyển màn hình sau 1.2 giây
        handler.postDelayed(
                navigationRunnable,
                SPLASH_DURATION_MS
        );
    }

    // ========================================
    // 2. KIỂM TRA SESSION VÀ ĐIỀU HƯỚNG
    // ========================================

    private void navigateToNextScreen() {

        // Không điều hướng nếu Activity đã đóng
        if (isFinishing() || isDestroyed()) {
            return;
        }

        // Kiểm tra Customer đã đăng nhập
        if (sessionManager.isCustomer()) {

            openCustomerHome();
            return;
        }

        // Chưa đăng nhập hoặc vai trò chưa hỗ trợ
        openLogin();
    }

    // ========================================
    // 3. ĐIỀU HƯỚNG ĐẾN CUSTOMER HOME
    // ========================================

    private void openCustomerHome() {

        Intent intent = new Intent(
                SplashActivity.this,
                CustomerHomeActivity.class
        );

        // Lấy tên Customer từ Session
        String customerName = sessionManager.getUserName();

        // Truyền tên để tương thích với Customer Home
        intent.putExtra(
                CustomerHomeActivity.EXTRA_CUSTOMER_NAME,
                customerName
        );

        // Xóa các Activity cũ trong Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        // Đóng Splash
        finish();
    }

    // ========================================
    // 4. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                SplashActivity.this,
                LoginActivity.class
        );

        // Xóa các Activity cũ trong Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        // Đóng Splash
        finish();
    }

    // ========================================
    // 5. GIẢI PHÓNG HANDLER
    // ========================================

    @Override
    protected void onDestroy() {

        // Hủy tác vụ điều hướng nếu còn chờ
        handler.removeCallbacks(navigationRunnable);

        super.onDestroy();
    }
}


