
package com.example.barber_shop.ui.guest.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.barber_shop.R;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.guest.barber.BarberDetailActivity;
import com.example.barber_shop.ui.guest.barber.BarberListActivity;
import com.example.barber_shop.ui.guest.branch.BranchListActivity;
import com.example.barber_shop.ui.guest.search.SearchActivity;
import com.example.barber_shop.ui.guest.service.ServiceDetailActivity;
import com.example.barber_shop.ui.guest.service.ServiceListActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class GuestHomeActivity extends AppCompatActivity {

    private BottomNavigationView guestBottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_home);

        setupWindowInsets();
        initViews();
        setupListeners();
        setupFeaturedServices();
        setupBottomNavigation();
    }

    // ========================================
    // 1. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.guestRoot);

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets insets = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            insets.left,
                            insets.top,
                            insets.right,
                            insets.bottom
                    );

                    return windowInsets;
                }
        );
    }

    // ========================================
    // 2. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        guestBottomNav = findViewById(
                R.id.guestBottomNav
        );
    }

    // ========================================
    // 3. XỬ LÝ SỰ KIỆN TRANG CHỦ
    // ========================================

    private void setupListeners() {

        // Nút đăng nhập
        findViewById(R.id.btnOpenLogin)
                .setOnClickListener(v -> openLogin());

        // Thanh tìm kiếm tổng hợp
        findViewById(R.id.cardSearch)
                .setOnClickListener(v -> openSearch());

        // Nút khám phá dịch vụ
        findViewById(R.id.btnExploreServices)
                .setOnClickListener(v -> openServiceList());

        // Xem tất cả dịch vụ
        findViewById(R.id.txtSeeAllServices)
                .setOnClickListener(v -> openServiceList());

        // Xem danh sách chi nhánh
        findViewById(R.id.cardBranches)
                .setOnClickListener(v -> openBranchList());

        // Xem tất cả Barber
        findViewById(R.id.txtSeeAllBarbers)
                .setOnClickListener(v -> openBarberList());

        // Xem Barber thứ nhất - B001
        findViewById(R.id.cardBarberOne)
                .setOnClickListener(v ->
                        openBarberDetail("B001")
                );

        // Xem Barber thứ hai - B002
        findViewById(R.id.cardBarberTwo)
                .setOnClickListener(v ->
                        openBarberDetail("B002")
                );
    }

    // ========================================
    // 4. HIỂN THỊ DỊCH VỤ NỔI BẬT
    // ========================================

    private void setupFeaturedServices() {

        // Dịch vụ cắt tóc nam - DV001
        bindServiceCard(
                R.id.itemServiceHaircut,
                "✂",
                "Cắt tóc",
                "DV001"
        );

        // Dịch vụ tạo kiểu Pomade - DV003
        bindServiceCard(
                R.id.itemServiceShave,
                "✦",
                "Tạo kiểu",
                "DV003"
        );

        // Dịch vụ gội đầu thư giãn - DV005
        bindServiceCard(
                R.id.itemServiceWash,
                "♨",
                "Gội đầu",
                "DV005"
        );
    }

    // ========================================
    // 5. GÁN DỮ LIỆU CHO CARD DỊCH VỤ
    // ========================================

    private void bindServiceCard(
            int cardId,
            String icon,
            String name,
            String serviceId
    ) {

        View card = findViewById(cardId);

        TextView txtIcon = card.findViewById(
                R.id.txtServiceIcon
        );

        TextView txtName = card.findViewById(
                R.id.txtServiceName
        );

        // Hiển thị biểu tượng và tên dịch vụ
        txtIcon.setText(icon);
        txtName.setText(name);

        // Nhấn Card để mở Service Detail tương ứng
        card.setOnClickListener(
                v -> openServiceDetail(serviceId)
        );
    }

    // ========================================
    // 6. XỬ LÝ BOTTOM NAVIGATION
    // ========================================

    private void setupBottomNavigation() {

        // Mặc định chọn tab Trang chủ
        guestBottomNav.setSelectedItemId(
                R.id.navGuestHome
        );

        guestBottomNav.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            // Tab Trang chủ
            if (itemId == R.id.navGuestHome) {
                return true;
            }

            // Tab Chi nhánh
            if (itemId == R.id.navGuestBranches) {
                openBranchList();
                return false;
            }

            // Tab Dịch vụ
            if (itemId == R.id.navGuestServices) {
                openServiceList();
                return false;
            }

            // Tab Tài khoản
            if (itemId == R.id.navGuestAccount) {
                openLogin();
                return false;
            }

            return false;
        });
    }

    // ========================================
    // 7. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                GuestHomeActivity.this,
                LoginActivity.class
        );

        // Tái sử dụng Login nếu còn trong Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        // Đóng Guest Home
        finish();
    }

    // ========================================
    // 8. ĐIỀU HƯỚNG ĐẾN BRANCH LIST
    // ========================================

    private void openBranchList() {

        Intent intent = new Intent(
                GuestHomeActivity.this,
                BranchListActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 9. ĐIỀU HƯỚNG ĐẾN SERVICE LIST
    // ========================================

    private void openServiceList() {

        Intent intent = new Intent(
                GuestHomeActivity.this,
                ServiceListActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 10. ĐIỀU HƯỚNG ĐẾN SERVICE DETAIL
    // ========================================

    private void openServiceDetail(String serviceId) {

        Intent intent = new Intent(
                GuestHomeActivity.this,
                ServiceDetailActivity.class
        );

        // Truyền ID dịch vụ
        intent.putExtra(
                ServiceDetailActivity.EXTRA_SERVICE_ID,
                serviceId
        );

        startActivity(intent);
    }

    // ========================================
    // 11. ĐIỀU HƯỚNG ĐẾN BARBER LIST
    // ========================================

    private void openBarberList() {

        Intent intent = new Intent(
                GuestHomeActivity.this,
                BarberListActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 12. ĐIỀU HƯỚNG ĐẾN BARBER DETAIL
    // ========================================

    private void openBarberDetail(String barberId) {

        // Tạo Intent mở Barber Detail
        Intent intent = new Intent(
                GuestHomeActivity.this,
                BarberDetailActivity.class
        );

        // Truyền ID Barber được chọn
        intent.putExtra(
                BarberDetailActivity.EXTRA_BARBER_ID,
                barberId
        );

        // Mở màn hình chi tiết Barber
        startActivity(intent);
    }

    // ========================================
    // 13. ĐIỀU HƯỚNG ĐẾN SEARCH & FILTER
    // ========================================

    private void openSearch() {

        // Tạo Intent mở màn hình tìm kiếm tổng hợp
        Intent intent = new Intent(
                GuestHomeActivity.this,
                SearchActivity.class
        );

        // Mở Search & Filter
        startActivity(intent);
    }

    // ========================================
    // 14. THÔNG BÁO CHỨC NĂNG CHƯA TRIỂN KHAI
    // ========================================

    private void showComingSoon(String feature) {

        Toast.makeText(
                this,
                feature + " sẽ được triển khai ở bước tiếp theo.",
                Toast.LENGTH_SHORT
        ).show();
    }
}


