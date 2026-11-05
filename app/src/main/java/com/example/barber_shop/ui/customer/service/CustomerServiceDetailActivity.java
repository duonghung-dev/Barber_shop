
package com.example.barber_shop.ui.customer.service;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.barber.CustomerBarberListActivity;
import com.example.barber_shop.ui.customer.branch.CustomerBranchListActivity;
import com.example.barber_shop.ui.customer.booking.CustomerSelectDateTimeActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CustomerServiceDetailActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // ID dịch vụ được chọn
    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    // ID chi nhánh đã chọn
    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    // ID Barber đã chọn - MỚI
    public static final String EXTRA_BARBER_ID =
            "extra_barber_id";

    // Trạng thái Booking
    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerServiceDetail;

    private AppCompatImageView imgCustomerDetailService;

    private TextView txtCustomerDetailServiceName;
    private TextView txtCustomerDetailServiceCategory;
    private TextView txtCustomerDetailServiceDescription;
    private TextView txtCustomerDetailServiceDuration;
    private TextView txtCustomerDetailServicePrice;
    private TextView txtCustomerDetailServiceBranch;

    private View cardCustomerServiceBranch;

    private MaterialButton btnCustomerBookService;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Dịch vụ hiện tại
    private SalonService currentService;

    // Chi nhánh hiện tại
    private Branch currentBranch;

    // Barber đã chọn (nếu có)
    private Barber currentBarber;

    // ID chi nhánh
    private String currentBranchId;

    // ID Barber
    private String currentBarberId;

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

        // Chỉ Customer đã đăng nhập được truy cập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Hiển thị giao diện Customer Service Detail
        setContentView(
                R.layout.activity_customer_service_detail
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();

        // Tải dữ liệu dịch vụ, chi nhánh và Barber
        if (!loadServiceDetail()) {
            return;
        }

        // Thiết lập sự kiện
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

        // Kiểm tra phiên đăng nhập Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerServiceDetailRoot
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
    // 7. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // Toolbar
        toolbarCustomerServiceDetail = findViewById(
                R.id.toolbarCustomerServiceDetail
        );

        // Hình ảnh dịch vụ
        imgCustomerDetailService = findViewById(
                R.id.imgCustomerDetailService
        );

        // Tên dịch vụ
        txtCustomerDetailServiceName = findViewById(
                R.id.txtCustomerDetailServiceName
        );

        // Danh mục dịch vụ
        txtCustomerDetailServiceCategory = findViewById(
                R.id.txtCustomerDetailServiceCategory
        );

        // Mô tả dịch vụ
        txtCustomerDetailServiceDescription = findViewById(
                R.id.txtCustomerDetailServiceDescription
        );

        // Thời lượng
        txtCustomerDetailServiceDuration = findViewById(
                R.id.txtCustomerDetailServiceDuration
        );

        // Giá tiền
        txtCustomerDetailServicePrice = findViewById(
                R.id.txtCustomerDetailServicePrice
        );

        // Tên chi nhánh
        txtCustomerDetailServiceBranch = findViewById(
                R.id.txtCustomerDetailServiceBranch
        );

        // Card thông tin chi nhánh
        cardCustomerServiceBranch = findViewById(
                R.id.cardCustomerServiceBranch
        );

        // Nút Đặt lịch ngay
        btnCustomerBookService = findViewById(
                R.id.btnCustomerBookService
        );
    }

    // ========================================
    // 8. NHẬN VÀ KIỂM TRA DỮ LIỆU DỊCH VỤ
    // ========================================

    private boolean loadServiceDetail() {

        // ========================================
        // 8.1. NHẬN ID DỊCH VỤ
        // ========================================

        String serviceId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_SERVICE_ID
                )
        );

        // Kiểm tra ID dịch vụ hợp lệ
        if (serviceId == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin dịch vụ."
            );

            return false;
        }

        // Tìm dịch vụ trong Repository
        currentService = ServiceRepository.findById(
                serviceId
        );

        // Không tìm thấy dịch vụ
        if (currentService == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin dịch vụ."
            );

            return false;
        }

        // ========================================
        // 8.2. NHẬN ID CHI NHÁNH
        // ========================================

        currentBranchId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BRANCH_ID
                )
        );

        // ========================================
        // 8.3. NHẬN ID BARBER - MỚI
        // ========================================

        currentBarberId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BARBER_ID
                )
        );

        // ========================================
        // 8.4. NHẬN TRẠNG THÁI BOOKING
        // ========================================

        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // ========================================
        // 8.5. KIỂM TRA BARBER NẾU ĐÃ CHỌN
        // ========================================

        if (!loadBarberInfo()) {
            return false;
        }

        // ========================================
        // 8.6. KIỂM TRA CHI NHÁNH NẾU ĐÃ CHỌN
        // ========================================

        if (currentBranchId != null) {

            if (!loadBranchInfo()) {
                return false;
            }

        } else {

            // Customer mở dịch vụ từ Home/Search
            // nhưng chưa chọn chi nhánh.

            currentBranch = null;

            // Ẩn Card chi nhánh
            cardCustomerServiceBranch.setVisibility(
                    View.GONE
            );
        }

        // ========================================
        // 8.7. HIỂN THỊ THÔNG TIN DỊCH VỤ
        // ========================================

        displayServiceInfo();

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
    // 10. KIỂM TRA BARBER ĐÃ CHỌN
    // ========================================

    private boolean loadBarberInfo() {

        // ========================================
        // TRƯỜNG HỢP CHƯA CHỌN BARBER
        // ========================================

        if (currentBarberId == null) {

            currentBarber = null;
            return true;
        }

        // ========================================
        // TRƯỜNG HỢP ĐÃ CHỌN BARBER
        // ========================================

        // Tìm Barber theo ID
        currentBarber = BarberRepository.findById(
                currentBarberId
        );

        // Kiểm tra Barber tồn tại
        if (currentBarber == null) {

            showErrorAndFinish(
                    "Không tìm thấy Barber đã chọn."
            );

            return false;
        }

        // Lấy chi nhánh làm việc của Barber
        String barberBranchId = normalizeId(
                currentBarber.getBranchId()
        );

        // Kiểm tra Barber có chi nhánh không
        if (barberBranchId == null) {

            showErrorAndFinish(
                    "Barber chưa có chi nhánh làm việc."
            );

            return false;
        }

        // ========================================
        // 10.1. ĐÃ CHỌN CHI NHÁNH TRƯỚC
        // ========================================

        if (currentBranchId != null) {

            // Chi nhánh được truyền phải khớp
            // với nơi Barber đang làm việc.

            if (!currentBranchId.equals(barberBranchId)) {

                showErrorAndFinish(
                        "Barber không thuộc chi nhánh đã chọn."
                );

                return false;
            }

        } else {

            // ========================================
            // 10.2. ĐÃ CHỌN BARBER NHƯNG CHƯA CÓ BRANCH
            // ========================================

            // Tự lấy chi nhánh từ Barber
            currentBranchId = barberBranchId;
        }

        return true;
    }

    // ========================================
    // 11. KIỂM TRA THÔNG TIN CHI NHÁNH
    // ========================================

    private boolean loadBranchInfo() {

        // Tìm chi nhánh theo ID
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
        // 11.1. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        List<SalonService> branchServices =
                ServiceRepository.getServicesForBranch(
                        currentBranchId
                );

        boolean availableAtBranch = false;

        // Kiểm tra danh sách hợp lệ
        if (branchServices != null) {

            for (SalonService service : branchServices) {

                if (service != null
                        && currentService.getId().equals(
                        service.getId()
                )) {

                    availableAtBranch = true;
                    break;
                }
            }
        }

        // Dịch vụ không có tại chi nhánh
        if (!availableAtBranch) {

            showErrorAndFinish(
                    "Dịch vụ không có tại chi nhánh này."
            );

            return false;
        }

        // ========================================
        // 11.2. HIỂN THỊ CHI NHÁNH
        // ========================================

        txtCustomerDetailServiceBranch.setText(
                currentBranch.getName()
        );

        cardCustomerServiceBranch.setVisibility(
                View.VISIBLE
        );

        // Lưu ý:
        // Mới xác nhận dịch vụ có tại chi nhánh,
        // chưa kiểm tra Barber có chuyên môn
        // thực hiện dịch vụ này hay không.

        return true;
    }

    // ========================================
    // 12. HIỂN THỊ THÔNG TIN DỊCH VỤ
    // ========================================

    private void displayServiceInfo() {

        // ========================================
        // 12.1. TÊN DỊCH VỤ
        // ========================================

        txtCustomerDetailServiceName.setText(
                currentService.getName()
        );

        // ========================================
        // 12.2. DANH MỤC DỊCH VỤ
        // ========================================

        txtCustomerDetailServiceCategory.setText(
                currentService.getCategory()
        );

        // ========================================
        // 12.3. MÔ TẢ DỊCH VỤ
        // ========================================

        txtCustomerDetailServiceDescription.setText(
                currentService.getDescription()
        );

        // ========================================
        // 12.4. THỜI LƯỢNG
        // ========================================

        txtCustomerDetailServiceDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentService.getDurationMinutes()
                )
        );

        // ========================================
        // 12.5. GIÁ DỊCH VỤ
        // ========================================

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String priceText = formatter.format(
                currentService.getPriceVnd()
        ) + " đ";

        txtCustomerDetailServicePrice.setText(
                priceText
        );

        // ========================================
        // 12.6. HÌNH ẢNH DỊCH VỤ
        // ========================================

        imgCustomerDetailService.setImageResource(
                currentService.getImageResId()
        );
    }

    // ========================================
    // 13. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarCustomerServiceDetail
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // Nhấn nút Đặt lịch ngay
        btnCustomerBookService
                .setOnClickListener(
                        v -> startBooking()
                );
    }

    // ========================================
    // 14. BẮT ĐẦU QUY TRÌNH ĐẶT LỊCH
    // ========================================

    private void startBooking() {

        // ========================================
        // 14.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 14.2. KIỂM TRA DỊCH VỤ
        // ========================================

        if (currentService == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy dịch vụ đã chọn.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Đánh dấu bắt đầu Booking
        bookingMode = true;

        // ========================================
        // 14.3. CHƯA CHỌN CHI NHÁNH
        // ========================================

        if (currentBranchId == null) {

            openBranchSelection();
            return;
        }

        // ========================================
        // 14.4. ĐÃ CHỌN CHI NHÁNH,
        // NHƯNG CHƯA CHỌN BARBER
        // ========================================

        if (currentBarberId == null) {

            openBarberSelection();
            return;
        }

        // ========================================
        // 14.5. ĐÃ CHỌN ĐỦ THÔNG TIN
        // ========================================

        openSelectDateTime();
    }

    // ========================================
    // 15. ĐIỀU HƯỚNG ĐẾN CHỌN CHI NHÁNH
    // ========================================

    private void openBranchSelection() {

        // Kiểm tra Session
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        Intent intent = new Intent(
                CustomerServiceDetailActivity.this,
                CustomerBranchListActivity.class
        );

        // ========================================
        // 15.1. GIỮ DỊCH VỤ ĐÃ CHỌN
        // ========================================

        // Sử dụng key thống nhất "extra_service_id".
        // CustomerBranchListActivity cần được
        // cập nhật để nhận và chuyển tiếp dữ liệu.

        intent.putExtra(
                EXTRA_SERVICE_ID,
                currentService.getId()
        );

        // ========================================
        // 15.2. ĐÁNH DẤU BOOKING MODE
        // ========================================

        intent.putExtra(
                EXTRA_BOOKING_MODE,
                true
        );

        // Mở danh sách chi nhánh
        startActivity(intent);
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN CHỌN BARBER
    // ========================================

    private void openBarberSelection() {

        // Kiểm tra chi nhánh
        if (currentBranch == null) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn chi nhánh trước.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Intent intent = new Intent(
                CustomerServiceDetailActivity.this,
                CustomerBarberListActivity.class
        );

        // ========================================
        // 16.1. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 16.2. TRUYỀN ID DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_SERVICE_ID,
                currentService.getId()
        );

        // ========================================
        // 16.3. GIỮ BOOKING MODE
        // ========================================

        intent.putExtra(
                CustomerBarberListActivity.EXTRA_BOOKING_MODE,
                true
        );

        // Mở danh sách Barber
        startActivity(intent);
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN CHỌN NGÀY VÀ GIỜ
    // ========================================

    private void openSelectDateTime() {

        // ========================================
        // 17.1. KIỂM TRA ĐỦ 3 THÔNG TIN
        // ========================================

        if (currentBranch == null
                || currentService == null
                || currentBarber == null
                || currentBranchId == null
                || currentBarberId == null) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn đầy đủ chi nhánh, "
                            + "dịch vụ và Barber.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 17.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerServiceDetailActivity.this,
                CustomerSelectDateTimeActivity.class
        );

        // ========================================
        // 17.3. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 17.4. TRUYỀN ID DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_SERVICE_ID,
                currentService.getId()
        );

        // ========================================
        // 17.5. TRUYỀN ID BARBER
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_BARBER_ID,
                currentBarberId
        );

        // ========================================
        // 17.6. MỞ MÀN HÌNH CHỌN NGÀY VÀ GIỜ
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 18. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerServiceDetailActivity.this,
                LoginActivity.class
        );

        // Xóa các Activity cũ khỏi Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        // Đóng màn hình hiện tại
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

