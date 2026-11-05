
package com.example.barber_shop.ui.customer.home;

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
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.ServiceRepository;

import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.branch.CustomerBranchListActivity;
import com.example.barber_shop.ui.customer.service.CustomerServiceListActivity;
import com.example.barber_shop.ui.customer.barber.CustomerBarberListActivity;
import com.example.barber_shop.ui.customer.barber.CustomerFavoriteBarbersActivity;
import com.example.barber_shop.ui.customer.booking.CustomerMyBookingsActivity;
import com.example.barber_shop.ui.customer.booking.CustomerBookingHistoryActivity;
import com.example.barber_shop.ui.customer.search.CustomerSearchActivity;

// STT 22 - Màn hình hồ sơ Customer
import com.example.barber_shop.ui.customer.profile.CustomerProfileActivity;

// STT 21 - Màn hình thông báo Customer
import com.example.barber_shop.ui.customer.notification.CustomerNotificationsActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomerHomeActivity extends AppCompatActivity {

    // ========================================
    // 1. HẰNG SỐ
    // ========================================

    // Giữ tương thích với Login/Splash.
    // Tên Customer được lấy trực tiếp từ Session.
    public static final String EXTRA_CUSTOMER_NAME =
            "extra_customer_name";

    // ========================================
    // 2. KHAI BÁO VIEW
    // ========================================

    private TextView txtCustomerName;

    private BottomNavigationView customerBottomNav;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    private SessionManager sessionManager;

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo Session
        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer đã đăng nhập
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // Bật Edge-to-Edge
        EdgeToEdge.enable(this);

        // Hiển thị giao diện
        setContentView(R.layout.activity_customer_home);

        // Khởi tạo các thành phần
        setupWindowInsets();
        initViews();
        setupCustomerGreeting();
        setupFeaturedServices();
        setupListeners();
        setupBottomNavigation();
    }

    // ========================================
    // 5. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing()) {
            return;
        }

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // Cập nhật tên Customer từ Session
        if (txtCustomerName != null) {

            setupCustomerGreeting();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerHomeRoot
        );

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
    // 7. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        txtCustomerName = findViewById(
                R.id.txtCustomerName
        );

        customerBottomNav = findViewById(
                R.id.customerBottomNav
        );
    }

    // ========================================
    // 8. HIỂN THỊ TÊN CUSTOMER
    // ========================================

    private void setupCustomerGreeting() {

        String customerName =
                sessionManager.getUserName();

        if (customerName == null
                || customerName.trim().isEmpty()) {

            customerName = "Khách hàng";
        }

        txtCustomerName.setText(
                customerName.trim()
        );
    }

    // ========================================
    // 9. HIỂN THỊ DỊCH VỤ NỔI BẬT
    // ========================================

    private void setupFeaturedServices() {

        // Cắt tóc nam
        bindFeaturedService(
                R.id.featuredServiceOne,
                "DV001"
        );

        // Tạo kiểu Pomade
        bindFeaturedService(
                R.id.featuredServiceTwo,
                "DV003"
        );

        // Gội đầu thư giãn
        bindFeaturedService(
                R.id.featuredServiceThree,
                "DV005"
        );
    }

    // ========================================
    // 10. GÁN DỮ LIỆU CHO CARD DỊCH VỤ
    // ========================================

    private void bindFeaturedService(
            int cardId,
            String serviceId
    ) {

        // Tìm Card
        View card = findViewById(cardId);

        // Tìm dịch vụ theo ID
        SalonService service =
                ServiceRepository.findById(
                        serviceId
                );

        // Ẩn Card nếu dịch vụ không tồn tại
        if (service == null) {

            card.setVisibility(View.GONE);
            return;
        }

        // Ánh xạ View trong Card
        TextView txtName = card.findViewById(
                R.id.txtCustomerServiceName
        );

        TextView txtDuration = card.findViewById(
                R.id.txtCustomerServiceDuration
        );

        TextView txtPrice = card.findViewById(
                R.id.txtCustomerServicePrice
        );

        // Hiển thị tên dịch vụ
        txtName.setText(
                service.getName()
        );

        // Hiển thị thời lượng
        txtDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        service.getDurationMinutes()
                )
        );

        // Định dạng giá tiền
        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String priceText = formatter.format(
                service.getPriceVnd()
        ) + " đ";

        txtPrice.setText(priceText);

        // Nhấn vào dịch vụ nổi bật
        card.setOnClickListener(
                v -> openFeaturedService(
                        service.getId()
                )
        );
    }

    // ========================================
    // 11. THIẾT LẬP SỰ KIỆN TRANG CHỦ
    // ========================================

    private void setupListeners() {

        // ========================================
        // 11.1. THÔNG BÁO - STT 21 (CẬP NHẬT)
        // ========================================

        // Trước đây:
        // v -> showComingSoon("Thông báo")
        //
        // Bây giờ:
        // Mở CustomerNotificationsActivity.

        findViewById(R.id.btnCustomerNotifications)
                .setOnClickListener(
                        v -> openNotifications()
                );

        // ========================================
        // 11.2. THANH TÌM KIẾM
        // ========================================

        findViewById(R.id.cardCustomerSearch)
                .setOnClickListener(
                        v -> openSearch()
                );

        // ========================================
        // 11.3. KHÁM PHÁ DỊCH VỤ
        // ========================================

        findViewById(R.id.btnCustomerExploreServices)
                .setOnClickListener(
                        v -> openServiceList()
                );

        // ========================================
        // 11.4. CHI NHÁNH
        // ========================================

        findViewById(R.id.cardCustomerBranches)
                .setOnClickListener(
                        v -> openBranchList()
                );

        // ========================================
        // 11.5. DỊCH VỤ
        // ========================================

        findViewById(R.id.cardCustomerServices)
                .setOnClickListener(
                        v -> openServiceList()
                );

        // ========================================
        // 11.6. BARBER
        // ========================================

        findViewById(R.id.cardCustomerBarbers)
                .setOnClickListener(
                        v -> openBarberList()
                );

        // ========================================
        // 11.7. QUẢN LÝ LỊCH HẸN - STT 18
        // ========================================

        // Giữ nguyên lựa chọn:
        // - Lịch hẹn của tôi
        // - Lịch sử đặt lịch

        findViewById(R.id.btnCustomerMyBookings)
                .setOnClickListener(
                        v -> showBookingOptions()
                );

        // ========================================
        // 11.8. XEM TẤT CẢ DỊCH VỤ
        // ========================================

        findViewById(R.id.txtCustomerSeeAllServices)
                .setOnClickListener(
                        v -> openServiceList()
                );

        // ========================================
        // 11.9. XEM TẤT CẢ BARBER
        // ========================================

        findViewById(R.id.btnCustomerSeeBarbers)
                .setOnClickListener(
                        v -> openBarberList()
                );
    }

    // ========================================
    // 12. THIẾT LẬP BOTTOM NAVIGATION
    // ========================================

    private void setupBottomNavigation() {

        // Mặc định chọn Trang chủ
        customerBottomNav.setSelectedItemId(
                R.id.navCustomerHome
        );

        customerBottomNav.setOnItemSelectedListener(
                item -> {

                    int itemId = item.getItemId();

                    // ========================================
                    // 12.1. TRANG CHỦ
                    // ========================================

                    if (itemId == R.id.navCustomerHome) {

                        return true;
                    }

                    // ========================================
                    // 12.2. KHÁM PHÁ
                    // ========================================

                    if (itemId == R.id.navCustomerExplore) {

                        openSearch();
                        return false;
                    }

                    // ========================================
                    // 12.3. LỊCH HẸN
                    // ========================================

                    if (itemId == R.id.navCustomerBookings) {

                        openMyBookings();

                        // Giữ tab Trang chủ khi quay lại
                        return false;
                    }

                    // ========================================
                    // 12.4. TÀI KHOẢN - STT 19
                    // ========================================

                    if (itemId == R.id.navCustomerProfile) {

                        // Hiển thị lựa chọn:
                        // - Barber yêu thích
                        // - Hồ sơ khách hàng

                        showCustomerAccountOptions();

                        return false;
                    }

                    return false;
                }
        );
    }

    // ========================================
    // 13. LỰA CHỌN QUẢN LÝ LỊCH HẸN - STT 18
    // ========================================

    private void showBookingOptions() {

        // Kiểm tra Session
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // Danh sách chức năng
        String[] options = {
                "Lịch hẹn của tôi",
                "Lịch sử đặt lịch"
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle("Quản lý lịch hẹn")
                .setItems(
                        options,
                        (dialog, which) -> {

                            // Lịch hẹn của tôi
                            if (which == 0) {

                                openMyBookings();
                                return;
                            }

                            // Lịch sử đặt lịch
                            if (which == 1) {

                                openBookingHistory();
                            }
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    // ========================================
    // 14. LỰA CHỌN TÀI KHOẢN - STT 19
    // ========================================

    private void showCustomerAccountOptions() {

        // ========================================
        // 14.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 14.2. DANH SÁCH CHỨC NĂNG
        // ========================================

        // Giữ nguyên Dialog Tài khoản đã có.
        // Customer vẫn có thể truy cập nhanh:
        // - Barber yêu thích
        // - Hồ sơ khách hàng (STT 22)

        String[] options = {
                "Barber yêu thích",
                "Hồ sơ khách hàng"
        };

        // ========================================
        // 14.3. HIỂN THỊ DIALOG
        // ========================================

        new MaterialAlertDialogBuilder(this)
                .setTitle("Tài khoản Customer")
                .setItems(
                        options,
                        (dialog, which) -> {

                            // ========================================
                            // LỰA CHỌN 1: BARBER YÊU THÍCH
                            // ========================================

                            if (which == 0) {

                                openFavoriteBarbers();
                                return;
                            }

                            // ========================================
                            // LỰA CHỌN 2: HỒ SƠ KHÁCH HÀNG
                            // ========================================

                            if (which == 1) {

                                openCustomerProfile();
                            }
                        }
                )
                .setNegativeButton(
                        "Đóng",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    // ========================================
    // 15. ĐIỀU HƯỚNG ĐẾN BRANCH LIST
    // ========================================

    private void openBranchList() {

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerBranchListActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN SERVICE LIST
    // ========================================

    private void openServiceList() {

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerServiceListActivity.class
        );

        // Không truyền branchId:
        // Hiển thị tất cả dịch vụ.

        startActivity(intent);
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN BARBER LIST
    // ========================================

    private void openBarberList() {

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerBarberListActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 18. ĐIỀU HƯỚNG ĐẾN SEARCH & FILTER
    // ========================================

    private void openSearch() {

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerSearchActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 19. ĐIỀU HƯỚNG ĐẾN LỊCH HẸN CỦA TÔI
    // ========================================

    private void openMyBookings() {

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerMyBookingsActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 20. ĐIỀU HƯỚNG ĐẾN LỊCH SỬ BOOKING
    // ========================================

    private void openBookingHistory() {

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerBookingHistoryActivity.class
        );

        startActivity(intent);
    }

    // ========================================
    // 21. ĐIỀU HƯỚNG ĐẾN BARBER YÊU THÍCH
    // ========================================

    private void openFavoriteBarbers() {

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerFavoriteBarbersActivity.class
        );

        // Không truyền Customer ID bằng Intent.
        // Activity đích tự lấy từ SessionManager.

        startActivity(intent);
    }

    // ========================================
    // 22. ĐIỀU HƯỚNG ĐẾN CUSTOMER PROFILE
    //     STT 22 - MỚI
    // ========================================

    private void openCustomerProfile() {

        // ========================================
        // 22.1. KIỂM TRA PHÂN QUYỀN
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 22.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerProfileActivity.class
        );

        // ========================================
        // 22.3. MỞ PROFILE
        // ========================================

        // Không truyền User ID qua Intent.
        //
        // CustomerProfileActivity sẽ tự lấy
        // Customer ID từ SessionManager,
        // sau đó đọc hồ sơ từ UserRepository.

        startActivity(intent);
    }

    // ========================================
    // 23. XỬ LÝ DỊCH VỤ NỔI BẬT
    // ========================================

    private void openFeaturedService(String serviceId) {

        // ========================================
        // 23.1. TÌM DỊCH VỤ
        // ========================================

        SalonService service =
                ServiceRepository.findById(
                        serviceId
                );

        if (service == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy dịch vụ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 23.2. MỞ DANH SÁCH DỊCH VỤ
        // ========================================

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerServiceListActivity.class
        );

        intent.putExtra(
                CustomerServiceListActivity.EXTRA_INITIAL_SEARCH_KEYWORD,
                service.getName()
        );

        startActivity(intent);
    }

    // ========================================
    // 24. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                LoginActivity.class
        );

        // Xóa các Activity cũ khỏi Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // ========================================
    // 25. ĐIỀU HƯỚNG ĐẾN THÔNG BÁO - STT 21
    // ========================================

    private void openNotifications() {

        // ========================================
        // 25.1. KIỂM TRA SESSION CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 25.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerHomeActivity.this,
                CustomerNotificationsActivity.class
        );

        // ========================================
        // 25.3. MỞ MÀN HÌNH THÔNG BÁO
        // ========================================

        // Không truyền Customer ID bằng Intent.
        // Activity tự lấy Customer ID
        // từ SessionManager.

        startActivity(intent);
    }

    // ========================================
    // 26. CHỨC NĂNG DỰ PHÒNG
    // ========================================

    private void showComingSoon(String feature) {

        Toast.makeText(
                this,
                feature + " sẽ được triển khai ở bước tiếp theo.",
                Toast.LENGTH_SHORT
        ).show();
    }
}
