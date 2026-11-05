
package com.example.barber_shop.ui.customer.branch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.service.CustomerServiceListActivity;
import com.example.barber_shop.ui.customer.barber.CustomerBarberListActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class CustomerBranchDetailActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // ID chi nhánh được chọn
    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    // ID dịch vụ đã chọn - MỚI
    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    // Trạng thái Booking - MỚI
    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBranchDetail;

    private AppCompatImageView imgCustomerDetailBranch;

    private TextView txtCustomerDetailBranchName;
    private TextView txtCustomerDetailBranchRating;
    private TextView txtCustomerDetailBranchAddress;
    private TextView txtCustomerDetailBranchHours;

    private MaterialButton btnCustomerViewBranchServices;
    private MaterialButton btnCustomerBookAtBranch;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Chi nhánh hiện tại
    private Branch currentBranch;

    // Dịch vụ đã chọn (nếu có)
    private SalonService currentService;

    // ID chi nhánh
    private String currentBranchId;

    // ID dịch vụ
    private String currentServiceId;

    // Trạng thái Booking
    private boolean bookingMode;

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer đã đăng nhập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Sử dụng XML riêng của Customer
        setContentView(
                R.layout.activity_customer_branch_detail
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();

        // Lấy và kiểm tra dữ liệu chi nhánh
        if (!loadBranchDetail()) {
            return;
        }

        // Thiết lập các sự kiện
        setupListeners();
    }

    // ========================================
    // 5. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        // Không xử lý nếu Activity đang đóng
        if (isFinishing()) {
            return;
        }

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBranchDetailRoot
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

        // ========================================
        // 7.1. TOOLBAR
        // ========================================

        toolbarCustomerBranchDetail = findViewById(
                R.id.toolbarCustomerBranchDetail
        );

        // ========================================
        // 7.2. HÌNH ẢNH CHI NHÁNH
        // ========================================

        imgCustomerDetailBranch = findViewById(
                R.id.imgCustomerDetailBranch
        );

        // ========================================
        // 7.3. TÊN CHI NHÁNH
        // ========================================

        txtCustomerDetailBranchName = findViewById(
                R.id.txtCustomerDetailBranchName
        );

        // ========================================
        // 7.4. ĐÁNH GIÁ CHI NHÁNH
        // ========================================

        txtCustomerDetailBranchRating = findViewById(
                R.id.txtCustomerDetailBranchRating
        );

        // ========================================
        // 7.5. ĐỊA CHỈ CHI NHÁNH
        // ========================================

        txtCustomerDetailBranchAddress = findViewById(
                R.id.txtCustomerDetailBranchAddress
        );

        // ========================================
        // 7.6. GIỜ HOẠT ĐỘNG
        // ========================================

        txtCustomerDetailBranchHours = findViewById(
                R.id.txtCustomerDetailBranchHours
        );

        // ========================================
        // 7.7. NÚT XEM DỊCH VỤ
        // ========================================

        btnCustomerViewBranchServices = findViewById(
                R.id.btnCustomerViewBranchServices
        );

        // ========================================
        // 7.8. NÚT ĐẶT LỊCH
        // ========================================

        btnCustomerBookAtBranch = findViewById(
                R.id.btnCustomerBookAtBranch
        );
    }

    // ========================================
    // 8. NHẬN VÀ KIỂM TRA DỮ LIỆU CHI NHÁNH
    // ========================================

    private boolean loadBranchDetail() {

        // ========================================
        // 8.1. NHẬN ID CHI NHÁNH
        // ========================================

        currentBranchId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BRANCH_ID
                )
        );

        // Kiểm tra ID chi nhánh
        if (currentBranchId == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin chi nhánh."
            );

            return false;
        }

        // ========================================
        // 8.2. TRA CỨU CHI NHÁNH
        // ========================================

        currentBranch = BranchRepository.findById(
                currentBranchId
        );

        // Kiểm tra chi nhánh tồn tại
        if (currentBranch == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin chi nhánh."
            );

            return false;
        }

        // ========================================
        // 8.3. NHẬN ID DỊCH VỤ - MỚI
        // ========================================

        currentServiceId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_SERVICE_ID
                )
        );

        // ========================================
        // 8.4. NHẬN TRẠNG THÁI BOOKING - MỚI
        // ========================================

        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // ========================================
        // 8.5. KIỂM TRA DỊCH VỤ ĐÃ CHỌN
        // ========================================

        if (!loadSelectedService()) {
            return false;
        }

        // ========================================
        // 8.6. HIỂN THỊ THÔNG TIN CHI NHÁNH
        // ========================================

        displayBranchInfo();

        // ========================================
        // 8.7. CẬP NHẬT NÚT BOOKING
        // ========================================

        updateBookingButton();

        return true;
    }

    // ========================================
    // 9. CHUẨN HÓA ID
    // ========================================

    private String normalizeId(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    // ========================================
    // 10. KIỂM TRA DỊCH VỤ ĐÃ CHỌN
    // ========================================

    private boolean loadSelectedService() {

        // ========================================
        // 10.1. CHƯA CHỌN DỊCH VỤ
        // ========================================

        if (currentServiceId == null) {

            currentService = null;
            return true;
        }

        // ========================================
        // 10.2. TRA CỨU DỊCH VỤ
        // ========================================

        currentService = ServiceRepository.findById(
                currentServiceId
        );

        // Dịch vụ không tồn tại
        if (currentService == null) {

            showErrorAndFinish(
                    "Không tìm thấy dịch vụ đã chọn."
            );

            return false;
        }

        // ========================================
        // 10.3. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        List<SalonService> branchServices =
                ServiceRepository.getServicesForBranch(
                        currentBranchId
                );

        // Kiểm tra dữ liệu danh sách
        if (branchServices == null) {

            showErrorAndFinish(
                    "Không thể kiểm tra dịch vụ tại chi nhánh."
            );

            return false;
        }

        boolean availableAtBranch = false;

        // Duyệt danh sách dịch vụ
        for (SalonService service : branchServices) {

            if (service != null
                    && currentServiceId.equals(
                    service.getId()
            )) {

                availableAtBranch = true;
                break;
            }
        }

        // Dịch vụ không có tại chi nhánh
        if (!availableAtBranch) {

            showErrorAndFinish(
                    "Dịch vụ đã chọn không có tại chi nhánh này."
            );

            return false;
        }

        return true;
    }

    // ========================================
    // 11. HIỂN THỊ THÔNG TIN CHI NHÁNH
    // ========================================

    private void displayBranchInfo() {

        // ========================================
        // 11.1. TÊN CHI NHÁNH
        // ========================================

        txtCustomerDetailBranchName.setText(
                currentBranch.getName()
        );

        // ========================================
        // 11.2. ĐỊA CHỈ
        // ========================================

        txtCustomerDetailBranchAddress.setText(
                currentBranch.getAddress()
        );

        // ========================================
        // 11.3. GIỜ HOẠT ĐỘNG
        // ========================================

        txtCustomerDetailBranchHours.setText(
                currentBranch.getWorkingHours()
        );

        // ========================================
        // 11.4. ĐÁNH GIÁ
        // ========================================

        txtCustomerDetailBranchRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f / 5.0",
                        currentBranch.getRating()
                )
        );

        // ========================================
        // 11.5. HÌNH ẢNH CHI NHÁNH
        // ========================================

        imgCustomerDetailBranch.setImageResource(
                currentBranch.getImageResId()
        );
    }

    // ========================================
    // 12. CẬP NHẬT NÚT ĐẶT LỊCH
    // ========================================

    private void updateBookingButton() {

        // ========================================
        // TRƯỜNG HỢP 1: ĐÃ CHỌN DỊCH VỤ
        // ========================================

        if (currentService != null) {

            // Customer đã chọn dịch vụ trước,
            // nên bước tiếp theo là chọn Barber.

            btnCustomerBookAtBranch.setText(
                    "Tiếp tục chọn Barber"
            );

            // ========================================
            // TRƯỜNG HỢP 2: CHƯA CHỌN DỊCH VỤ
            // ========================================

        } else {

            btnCustomerBookAtBranch.setText(
                    "Đặt lịch tại chi nhánh"
            );
        }
    }

    // ========================================
    // 13. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 13.1. QUAY VỀ MÀN HÌNH TRƯỚC
        // ========================================

        toolbarCustomerBranchDetail
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 13.2. XEM DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        btnCustomerViewBranchServices
                .setOnClickListener(
                        v -> openCustomerServices()
                );

        // ========================================
        // 13.3. BẮT ĐẦU ĐẶT LỊCH
        // ========================================

        btnCustomerBookAtBranch
                .setOnClickListener(
                        v -> startBooking()
                );
    }

    // ========================================
    // 14. XEM DỊCH VỤ TẠI CHI NHÁNH
    // ========================================

    private void openCustomerServices() {

        // Chỉ xem danh sách dịch vụ,
        // không bắt buộc thực hiện Booking.

        openCustomerServiceList(false);
    }

    // ========================================
    // 15. BẮT ĐẦU QUY TRÌNH ĐẶT LỊCH
    // ========================================

    private void startBooking() {

        // ========================================
        // 15.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 15.2. KIỂM TRA CHI NHÁNH
        // ========================================

        if (currentBranch == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy chi nhánh đã chọn.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Đánh dấu bắt đầu Booking
        bookingMode = true;

        // ========================================
        // 15.3. CHƯA CHỌN DỊCH VỤ
        // ========================================

        if (currentService == null) {

            // Bước tiếp theo là chọn dịch vụ
            openCustomerServiceList(true);
            return;
        }

        // ========================================
        // 15.4. ĐÃ CHỌN DỊCH VỤ
        // ========================================

        // Bước tiếp theo là chọn Barber
        openBarberSelection();
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN CUSTOMER SERVICE LIST
    // ========================================

    private void openCustomerServiceList(
            boolean isBooking
    ) {

        // ========================================
        // 16.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // ========================================
        // 16.2. KIỂM TRA CHI NHÁNH
        // ========================================

        if (currentBranch == null) {
            return;
        }

        // ========================================
        // 16.3. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBranchDetailActivity.this,
                CustomerServiceListActivity.class
        );

        // ========================================
        // 16.4. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 16.5. TRUYỀN TRẠNG THÁI BOOKING
        // ========================================

        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BOOKING_MODE,
                isBooking
        );

        // Lưu ý:
        // Nút "Xem dịch vụ" mở danh sách ở chế độ xem.
        // Nút "Đặt lịch" mở danh sách ở chế độ Booking
        // khi chưa chọn dịch vụ.

        startActivity(intent);
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN CUSTOMER BARBER LIST
    // ========================================

    private void openBarberSelection() {

        // ========================================
        // 17.1. KIỂM TRA DỮ LIỆU
        // ========================================

        if (currentBranch == null
                || currentService == null) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn đầy đủ chi nhánh và dịch vụ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 17.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBranchDetailActivity.this,
                CustomerBarberListActivity.class
        );

        // ========================================
        // 17.3. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 17.4. TRUYỀN ID DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_SERVICE_ID,
                currentServiceId
        );

        // ========================================
        // 17.5. GIỮ TRẠNG THÁI BOOKING
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_BOOKING_MODE,
                true
        );

        // ========================================
        // 17.6. MỞ DANH SÁCH BARBER
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 18. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBranchDetailActivity.this,
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
    // 19. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
    // ========================================

    private void showErrorAndFinish(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

        finish();
    }
}

