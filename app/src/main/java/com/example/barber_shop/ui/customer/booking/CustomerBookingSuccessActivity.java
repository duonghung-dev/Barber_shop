
package com.example.barber_shop.ui.customer.booking;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerBookingSuccessActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // Chỉ cần truyền mã Booking.
    // Các thông tin còn lại đọc từ Repository.
    public static final String EXTRA_BOOKING_CODE =
            "extra_booking_code";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBookingSuccess;

    private TextView txtSuccessBookingCode;
    private TextView txtSuccessBookingStatus;

    private TextView txtSuccessBranchName;
    private TextView txtSuccessServiceName;
    private TextView txtSuccessBarberName;

    private TextView txtSuccessBookingDate;
    private TextView txtSuccessBookingTime;
    private TextView txtSuccessDuration;

    private TextView txtSuccessTotalPrice;
    private TextView txtSuccessNotice;

    private MaterialButton btnSuccessCopyCode;
    private MaterialButton btnSuccessBackHome;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    private SessionManager sessionManager;

    // Booking được đọc từ Repository
    private Booking currentBooking;

    // Mã Booking nhận từ STT 12
    private String currentBookingCode;

    // Dùng luồng riêng để đọc dữ liệu cục bộ
    private final ExecutorService bookingExecutor =
            Executors.newSingleThreadExecutor();

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Kiểm tra Customer đã đăng nhập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Sử dụng XML của STT 13
        setContentView(
                R.layout.activity_customer_booking_success
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();
        setupListeners();

        // Nhận mã Booking
        currentBookingCode = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BOOKING_CODE
                )
        );

        // Kiểm tra mã Booking
        if (currentBookingCode == null) {

            showLoadError(
                    "Không tìm thấy mã lịch hẹn."
            );

            return;
        }

        // Đọc Booking đã lưu
        loadBookingDetail();
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

        if (!sessionManager.isCustomer()) {
            openLogin();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBookingSuccessRoot
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

        // Toolbar
        toolbarCustomerBookingSuccess = findViewById(
                R.id.toolbarCustomerBookingSuccess
        );

        // Mã Booking và trạng thái
        txtSuccessBookingCode = findViewById(
                R.id.txtSuccessBookingCode
        );

        txtSuccessBookingStatus = findViewById(
                R.id.txtSuccessBookingStatus
        );

        // Chi nhánh
        txtSuccessBranchName = findViewById(
                R.id.txtSuccessBranchName
        );

        // Dịch vụ
        txtSuccessServiceName = findViewById(
                R.id.txtSuccessServiceName
        );

        // Barber
        txtSuccessBarberName = findViewById(
                R.id.txtSuccessBarberName
        );

        // Ngày và giờ
        txtSuccessBookingDate = findViewById(
                R.id.txtSuccessBookingDate
        );

        txtSuccessBookingTime = findViewById(
                R.id.txtSuccessBookingTime
        );

        // Thời lượng
        txtSuccessDuration = findViewById(
                R.id.txtSuccessDuration
        );

        // Tổng tiền
        txtSuccessTotalPrice = findViewById(
                R.id.txtSuccessTotalPrice
        );

        // Thông báo
        txtSuccessNotice = findViewById(
                R.id.txtSuccessNotice
        );

        // Nút sao chép mã
        btnSuccessCopyCode = findViewById(
                R.id.btnSuccessCopyCode
        );

        // Nút về trang chủ
        btnSuccessBackHome = findViewById(
                R.id.btnSuccessBackHome
        );
    }

    // ========================================
    // 8. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Toolbar quay về trang chủ,
        // không quay lại màn hình xác nhận.
        toolbarCustomerBookingSuccess
                .setNavigationOnClickListener(
                        v -> openCustomerHome()
                );

        // Sao chép mã Booking
        btnSuccessCopyCode.setOnClickListener(
                v -> copyBookingCode()
        );

        // Nút về trang chủ
        btnSuccessBackHome.setOnClickListener(
                v -> openCustomerHome()
        );

        // Xử lý nút Back của hệ thống Android
        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        openCustomerHome();
                    }
                }
        );
    }

    // ========================================
    // 9. ĐỌC THÔNG TIN BOOKING
    // ========================================

    private void loadBookingDetail() {

        // Tạm khóa sao chép khi chưa tải xong
        btnSuccessCopyCode.setEnabled(false);

        txtSuccessNotice.setText(
                "Đang kiểm tra lịch hẹn đã lưu..."
        );

        // Dùng Application Context để đọc dữ liệu
        final Context appContext =
                getApplicationContext();

        final String bookingCode =
                currentBookingCode;

        // ========================================
        // 9.1. ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        bookingExecutor.execute(() -> {

            Booking loadedBooking = null;

            String errorMessage = null;

            try {

                loadedBooking = BookingRepository.findByCode(
                        appContext,
                        bookingCode
                );

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc dữ liệu lịch hẹn.";
            }

            final Booking resultBooking =
                    loadedBooking;

            final String resultError =
                    errorMessage;

            // ========================================
            // 9.2. CẬP NHẬT GIAO DIỆN
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Kiểm tra phiên đăng nhập
                if (!sessionManager.isCustomer()) {
                    openLogin();
                    return;
                }

                // Xử lý lỗi đọc dữ liệu
                if (resultError != null) {
                    showLoadError(resultError);
                    return;
                }

                // Không tìm thấy Booking
                if (resultBooking == null) {

                    showLoadError(
                            "Không tìm thấy lịch hẹn đã lưu."
                    );

                    return;
                }

                // ========================================
                // 9.3. KIỂM TRA QUYỀN CUSTOMER
                // ========================================

                String customerId = normalizeValue(
                        sessionManager.getUserId()
                );

                if (customerId == null
                        || !customerId.equals(
                        resultBooking.getCustomerId()
                )) {

                    showLoadError(
                            "Bạn không có quyền xem lịch hẹn này."
                    );

                    return;
                }

                // ========================================
                // 9.4. HIỂN THỊ BOOKING
                // ========================================

                currentBooking = resultBooking;

                displayBookingInfo();
            });
        });
    }

    // ========================================
    // 10. HIỂN THỊ THÔNG TIN BOOKING
    // ========================================

    private void displayBookingInfo() {

        if (currentBooking == null) {
            return;
        }

        // ========================================
        // 10.1. MÃ LỊCH HẸN
        // ========================================

        txtSuccessBookingCode.setText(
                currentBooking.getBookingCode()
        );

        // ========================================
        // 10.2. TRẠNG THÁI
        // ========================================

        txtSuccessBookingStatus.setText(
                currentBooking.getStatusDisplayName()
        );

        // ========================================
        // 10.3. CHI NHÁNH
        // ========================================

        txtSuccessBranchName.setText(
                currentBooking.getBranchName()
        );

        // ========================================
        // 10.4. DỊCH VỤ
        // ========================================

        txtSuccessServiceName.setText(
                currentBooking.getServiceName()
        );

        // ========================================
        // 10.5. BARBER
        // ========================================

        txtSuccessBarberName.setText(
                currentBooking.getBarberName()
        );

        // ========================================
        // 10.6. NGÀY HẸN
        // ========================================

        txtSuccessBookingDate.setText(
                formatBookingDate(
                        currentBooking.getBookingDate()
                )
        );

        // ========================================
        // 10.7. GIỜ HẸN
        // ========================================

        txtSuccessBookingTime.setText(
                currentBooking.getBookingTime()
        );

        // ========================================
        // 10.8. THỜI LƯỢNG
        // ========================================

        txtSuccessDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentBooking.getDurationMinutes()
                )
        );

        // ========================================
        // 10.9. TỔNG TIỀN
        // ========================================

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String priceText = formatter.format(
                currentBooking.getTotalPrice()
        ) + " đ";

        txtSuccessTotalPrice.setText(
                priceText
        );

        // ========================================
        // 10.10. THÔNG BÁO TRẠNG THÁI
        // ========================================

        txtSuccessNotice.setText(
                "Lịch hẹn đã được lưu trên thiết bị "
                        + "để phục vụ kiểm thử.\n"
                        + "Trạng thái hiện tại: "
                        + currentBooking.getStatusDisplayName()
                        + ".\n"
                        + "Chưa đồng bộ với hệ thống Backend."
        );

        // Cho phép sao chép mã
        btnSuccessCopyCode.setEnabled(true);
    }

    // ========================================
    // 11. ĐỊNH DẠNG NGÀY HẸN
    // ========================================

    private String formatBookingDate(String dateText) {

        if (dateText == null) {
            return "--";
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date date = parser.parse(dateText);

            if (date == null) {
                return dateText;
            }

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "EEEE, dd/MM/yyyy",
                            new Locale("vi", "VN")
                    );

            return formatter.format(date);

        } catch (ParseException e) {

            // Nếu dữ liệu cũ có định dạng khác,
            // hiển thị nguyên giá trị ban đầu.
            return dateText;
        }
    }

    // ========================================
    // 12. SAO CHÉP MÃ LỊCH HẸN
    // ========================================

    private void copyBookingCode() {

        if (currentBooking == null) {

            Toast.makeText(
                    this,
                    "Chưa có mã lịch hẹn để sao chép.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Lấy ClipboardManager
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE
                );

        if (clipboard == null) {

            Toast.makeText(
                    this,
                    "Không thể truy cập bộ nhớ tạm.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Tạo dữ liệu sao chép
        ClipData clip = ClipData.newPlainText(
                "Mã lịch hẹn",
                currentBooking.getBookingCode()
        );

        // Sao chép vào Clipboard
        clipboard.setPrimaryClip(clip);

        Toast.makeText(
                this,
                "Đã sao chép mã lịch hẹn.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ========================================
    // 13. ĐIỀU HƯỚNG VỀ TRANG CHỦ
    // ========================================

    private void openCustomerHome() {

        // Mở lại Activity khởi chạy ứng dụng.
        // Màn hình khởi chạy sẽ điều hướng Customer
        // theo Session hiện tại.
        //
        // Cách này không phụ thuộc vào tên cụ thể
        // của CustomerHomeActivity.

        Intent launchIntent =
                getPackageManager().getLaunchIntentForPackage(
                        getPackageName()
                );

        if (launchIntent != null) {

            // Xóa luồng Booking cũ khỏi Back Stack
            launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(launchIntent);
            finish();

        } else {

            // Trường hợp ứng dụng không tìm thấy
            // Activity khởi chạy.
            finishAffinity();
        }
    }

    // ========================================
    // 14. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBookingSuccessActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    // ========================================
    // 15. CHUẨN HÓA CHUỖI
    // ========================================

    private String normalizeValue(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    // ========================================
    // 16. HIỂN THỊ LỖI ĐỌC BOOKING
    // ========================================

    private void showLoadError(String message) {

        txtSuccessBookingCode.setText("--");

        txtSuccessBookingStatus.setText(
                "Không thể xác minh lịch hẹn"
        );

        txtSuccessNotice.setText(message);

        btnSuccessCopyCode.setEnabled(false);

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ========================================
    // 17. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        bookingExecutor.shutdown();

        super.onDestroy();
    }
}
