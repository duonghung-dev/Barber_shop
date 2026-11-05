
package com.example.barber_shop.ui.customer.booking;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.model.Review;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.data.repository.ReviewRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.review.CustomerReviewActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerBookingDetailActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    public static final String EXTRA_BOOKING_CODE =
            "extra_booking_code";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    // Toolbar
    private MaterialToolbar toolbarCustomerBookingDetail;

    // Vùng hiển thị nội dung
    private ScrollView scrollCustomerBookingDetail;

    // Mã Booking và trạng thái
    private TextView txtDetailBookingCode;
    private TextView txtDetailBookingStatus;

    // Thông tin dịch vụ
    private TextView txtDetailBranchName;
    private TextView txtDetailServiceName;
    private TextView txtDetailBarberName;

    // Ngày giờ và thời lượng
    private TextView txtDetailBookingDate;
    private TextView txtDetailBookingTime;
    private TextView txtDetailDuration;

    // Tổng tiền
    private TextView txtDetailTotalPrice;

    // Thông báo
    private TextView txtDetailNotice;

    // Loading và Error
    private ProgressBar progressCustomerBookingDetail;

    private LinearLayout layoutCustomerBookingDetailError;

    private TextView txtCustomerBookingDetailError;

    // ========================================
    // 3. KHAI BÁO CÁC NÚT CHỨC NĂNG
    // ========================================

    // Sao chép mã Booking
    private MaterialButton btnDetailCopyCode;

    // STT 16 - Đổi lịch hẹn
    private MaterialButton btnDetailRescheduleBooking;

    // STT 17 - Hủy lịch hẹn
    private MaterialButton btnDetailCancelBooking;

    // STT 20 - Đánh giá dịch vụ (MỚI)
    private MaterialButton btnDetailReviewBooking;

    // Quay lại danh sách
    private MaterialButton btnDetailBackToList;

    // ========================================
    // 4. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Mã Booking nhận từ Intent
    private String currentBookingCode;

    // Booking hiện tại
    private Booking currentBooking;

    // STT 20 - Đánh giá đã lưu (MỚI)
    private Review currentReview;

    // Executor xử lý dữ liệu cục bộ
    private final ExecutorService bookingExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt kết quả các lần tải
    private int loadRequestVersion = 0;

    // Tránh gửi nhiều yêu cầu hủy cùng lúc
    private boolean isCancelling = false;

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo Session
        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer truy cập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Hiển thị giao diện
        setContentView(
                R.layout.activity_customer_booking_detail
        );

        // Thiết lập các thành phần
        setupWindowInsets();
        initViews();
        setupListeners();

        // Nhận mã Booking từ Intent
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
        }

        // Dữ liệu được tải trong onResume().
    }

    // ========================================
    // 6. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing()) {
            return;
        }

        // Kiểm tra phiên Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Không tải lại khi đang hủy lịch
        if (isCancelling) {
            return;
        }

        // Tải lại Booking và Review.
        //
        // Khi Customer quay lại từ màn hình
        // Đổi lịch hoặc Đánh giá, onResume()
        // sẽ được gọi để cập nhật dữ liệu.

        if (currentBookingCode != null) {
            loadBookingDetail();
        }
    }

    // ========================================
    // 7. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBookingDetailRoot
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
    // 8. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // ========================================
        // 8.1. TOOLBAR
        // ========================================

        toolbarCustomerBookingDetail = findViewById(
                R.id.toolbarCustomerBookingDetail
        );

        // ========================================
        // 8.2. VÙNG NỘI DUNG
        // ========================================

        scrollCustomerBookingDetail = findViewById(
                R.id.scrollCustomerBookingDetail
        );

        // ========================================
        // 8.3. MÃ BOOKING VÀ TRẠNG THÁI
        // ========================================

        txtDetailBookingCode = findViewById(
                R.id.txtDetailBookingCode
        );

        txtDetailBookingStatus = findViewById(
                R.id.txtDetailBookingStatus
        );

        // ========================================
        // 8.4. THÔNG TIN DỊCH VỤ
        // ========================================

        txtDetailBranchName = findViewById(
                R.id.txtDetailBranchName
        );

        txtDetailServiceName = findViewById(
                R.id.txtDetailServiceName
        );

        txtDetailBarberName = findViewById(
                R.id.txtDetailBarberName
        );

        // ========================================
        // 8.5. THỜI GIAN BOOKING
        // ========================================

        txtDetailBookingDate = findViewById(
                R.id.txtDetailBookingDate
        );

        txtDetailBookingTime = findViewById(
                R.id.txtDetailBookingTime
        );

        txtDetailDuration = findViewById(
                R.id.txtDetailDuration
        );

        // ========================================
        // 8.6. TỔNG TIỀN
        // ========================================

        txtDetailTotalPrice = findViewById(
                R.id.txtDetailTotalPrice
        );

        // ========================================
        // 8.7. THÔNG BÁO
        // ========================================

        txtDetailNotice = findViewById(
                R.id.txtDetailNotice
        );

        // ========================================
        // 8.8. LOADING
        // ========================================

        progressCustomerBookingDetail = findViewById(
                R.id.progressCustomerBookingDetail
        );

        // ========================================
        // 8.9. KHU VỰC LỖI
        // ========================================

        layoutCustomerBookingDetailError = findViewById(
                R.id.layoutCustomerBookingDetailError
        );

        txtCustomerBookingDetailError = findViewById(
                R.id.txtCustomerBookingDetailError
        );

        // ========================================
        // 8.10. CÁC NÚT CHỨC NĂNG
        // ========================================

        // Sao chép mã Booking
        btnDetailCopyCode = findViewById(
                R.id.btnDetailCopyCode
        );

        // STT 16 - Đổi lịch
        btnDetailRescheduleBooking = findViewById(
                R.id.btnDetailRescheduleBooking
        );

        // STT 17 - Hủy lịch
        btnDetailCancelBooking = findViewById(
                R.id.btnDetailCancelBooking
        );

        // STT 20 - Đánh giá dịch vụ (MỚI)
        btnDetailReviewBooking = findViewById(
                R.id.btnDetailReviewBooking
        );

        // Quay lại danh sách
        btnDetailBackToList = findViewById(
                R.id.btnDetailBackToList
        );
    }

    // ========================================
    // 9. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 9.1. NÚT BACK TRÊN TOOLBAR
        // ========================================

        toolbarCustomerBookingDetail
                .setNavigationOnClickListener(
                        v -> {
                            if (!isCancelling) {
                                finish();
                            }
                        }
                );

        // ========================================
        // 9.2. SAO CHÉP MÃ BOOKING
        // ========================================

        btnDetailCopyCode.setOnClickListener(
                v -> copyBookingCode()
        );

        // ========================================
        // 9.3. ĐỔI LỊCH - STT 16
        // ========================================

        btnDetailRescheduleBooking.setOnClickListener(
                v -> openRescheduleBooking()
        );

        // ========================================
        // 9.4. HỦY LỊCH - STT 17
        // ========================================

        btnDetailCancelBooking.setOnClickListener(
                v -> confirmCancelBooking()
        );

        // ========================================
        // 9.5. ĐÁNH GIÁ - STT 20 (MỚI)
        // ========================================

        btnDetailReviewBooking.setOnClickListener(
                v -> openReviewBooking()
        );

        // ========================================
        // 9.6. QUAY LẠI DANH SÁCH
        // ========================================

        btnDetailBackToList.setOnClickListener(
                v -> {
                    if (!isCancelling) {
                        finish();
                    }
                }
        );
    }

    // ========================================
    // 10. TẢI BOOKING VÀ REVIEW
    // ========================================

    private void loadBookingDetail() {

        // ========================================
        // 10.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // ========================================
        // 10.2. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            showLoadError(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // ========================================
        // 10.3. LẤY BOOKING CODE
        // ========================================

        final String bookingCode = normalizeValue(
                currentBookingCode
        );

        if (bookingCode == null) {

            showLoadError(
                    "Mã lịch hẹn không hợp lệ."
            );

            return;
        }

        // ========================================
        // 10.4. HIỂN THỊ LOADING
        // ========================================

        showLoading();

        // Đánh dấu phiên tải dữ liệu
        final int requestVersion =
                ++loadRequestVersion;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 10.5. ĐỌC DỮ LIỆU TRÊN LUỒNG RIÊNG
        // ========================================

        bookingExecutor.execute(() -> {

            Booking loadedBooking = null;
            Review loadedReview = null;

            String errorMessage = null;

            try {

                // ========================================
                // 10.6. TÌM BOOKING
                // ========================================

                loadedBooking =
                        BookingRepository.findByCode(
                                appContext,
                                bookingCode
                        );

                // ========================================
                // 10.7. ĐỌC REVIEW - STT 20 (MỚI)
                // ========================================

                // Chỉ đọc Review khi:
                // - Booking tồn tại
                // - Booking thuộc Customer
                // - Booking đã COMPLETED

                if (loadedBooking != null
                        && customerId.equals(
                        loadedBooking.getCustomerId()
                )
                        && Booking.STATUS_COMPLETED.equals(
                        loadedBooking.getStatus()
                )) {

                    loadedReview =
                            ReviewRepository.getReviewForBooking(
                                    appContext,
                                    customerId,
                                    bookingCode
                            );
                }

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc thông tin lịch hẹn.";
            }

            final Booking resultBooking = loadedBooking;
            final Review resultReview = loadedReview;
            final String resultError = errorMessage;

            // ========================================
            // 10.8. CẬP NHẬT TRÊN MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                // Activity đã đóng
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả cũ
                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // ========================================
                // 10.9. KIỂM TRA LẠI SESSION
                // ========================================

                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeValue(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                // ========================================
                // 10.10. XỬ LÝ LỖI
                // ========================================

                if (resultError != null) {

                    showLoadError(resultError);
                    return;
                }

                // ========================================
                // 10.11. KIỂM TRA BOOKING TỒN TẠI
                // ========================================

                if (resultBooking == null) {

                    showLoadError(
                            "Không tìm thấy lịch hẹn đã chọn."
                    );

                    return;
                }

                // ========================================
                // 10.12. KIỂM TRA CHỦ SỞ HỮU
                // ========================================

                if (!customerId.equals(
                        resultBooking.getCustomerId()
                )) {

                    showLoadError(
                            "Bạn không có quyền xem lịch hẹn này."
                    );

                    return;
                }

                // ========================================
                // 10.13. CẬP NHẬT DỮ LIỆU
                // ========================================

                currentBooking = resultBooking;

                // STT 20 - Review đã lưu
                currentReview = resultReview;

                // ========================================
                // 10.14. HIỂN THỊ GIAO DIỆN
                // ========================================

                displayBookingInfo();
            });
        });
    }

    // ========================================
    // 11. HIỂN THỊ TRẠNG THÁI LOADING
    // ========================================

    private void showLoading() {

        // Xóa dữ liệu cũ
        currentBooking = null;
        currentReview = null;

        // Ẩn nội dung
        scrollCustomerBookingDetail.setVisibility(
                View.GONE
        );

        // Ẩn khu vực lỗi
        layoutCustomerBookingDetailError.setVisibility(
                View.GONE
        );

        // Hiển thị ProgressBar
        progressCustomerBookingDetail.setVisibility(
                View.VISIBLE
        );

        // Khóa sao chép
        btnDetailCopyCode.setEnabled(false);

        // Ẩn nút Đổi lịch
        btnDetailRescheduleBooking.setEnabled(false);
        btnDetailRescheduleBooking.setVisibility(
                View.GONE
        );

        // Ẩn nút Hủy lịch
        btnDetailCancelBooking.setEnabled(false);
        btnDetailCancelBooking.setVisibility(
                View.GONE
        );

        // STT 20 - Ẩn nút Đánh giá (MỚI)
        btnDetailReviewBooking.setEnabled(false);
        btnDetailReviewBooking.setVisibility(
                View.GONE
        );

        // Khóa nút quay lại nếu đang hủy
        btnDetailBackToList.setEnabled(
                !isCancelling
        );
    }

    // ========================================
    // 12. HIỂN THỊ THÔNG TIN BOOKING
    // ========================================

    private void displayBookingInfo() {

        if (currentBooking == null) {
            return;
        }

        // ========================================
        // 12.1. MÃ BOOKING
        // ========================================

        txtDetailBookingCode.setText(
                currentBooking.getBookingCode()
        );

        // ========================================
        // 12.2. TRẠNG THÁI BOOKING
        // ========================================

        txtDetailBookingStatus.setText(
                currentBooking.getStatusDisplayName()
        );

        txtDetailBookingStatus.setTextColor(
                getBookingStatusColor(
                        currentBooking.getStatus()
                )
        );

        // ========================================
        // 12.3. CHI NHÁNH
        // ========================================

        txtDetailBranchName.setText(
                currentBooking.getBranchName()
        );

        // ========================================
        // 12.4. DỊCH VỤ
        // ========================================

        txtDetailServiceName.setText(
                currentBooking.getServiceName()
        );

        // ========================================
        // 12.5. BARBER
        // ========================================

        txtDetailBarberName.setText(
                currentBooking.getBarberName()
        );

        // ========================================
        // 12.6. NGÀY HẸN
        // ========================================

        txtDetailBookingDate.setText(
                formatBookingDate(
                        currentBooking.getBookingDate()
                )
        );

        // ========================================
        // 12.7. GIỜ HẸN
        // ========================================

        txtDetailBookingTime.setText(
                currentBooking.getBookingTime()
        );

        // ========================================
        // 12.8. THỜI LƯỢNG DỊCH VỤ
        // ========================================

        txtDetailDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentBooking.getDurationMinutes()
                )
        );

        // ========================================
        // 12.9. TỔNG TIỀN
        // ========================================

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String totalPriceText = formatter.format(
                currentBooking.getTotalPrice()
        ) + " đ";

        txtDetailTotalPrice.setText(
                totalPriceText
        );

        // ========================================
        // 12.10. THÔNG BÁO
        // ========================================

        String notice =
                "Thông tin lịch hẹn được lưu cục bộ "
                        + "trên thiết bị để phục vụ kiểm thử.\n"
                        + "Chưa đồng bộ với Backend.";

        if (currentBooking.isActive()
                && !isBookingInFuture(currentBooking)) {

            notice += "\n\n"
                    + "Lịch hẹn đã qua giờ bắt đầu, "
                    + "nhưng trạng thái chưa được cập nhật.";
        }

        txtDetailNotice.setText(notice);

        // ========================================
        // 12.11. HIỂN THỊ NỘI DUNG
        // ========================================

        progressCustomerBookingDetail.setVisibility(
                View.GONE
        );

        layoutCustomerBookingDetailError.setVisibility(
                View.GONE
        );

        scrollCustomerBookingDetail.setVisibility(
                View.VISIBLE
        );

        // Cho phép sao chép mã
        btnDetailCopyCode.setEnabled(true);

        // Cho phép quay lại
        btnDetailBackToList.setEnabled(true);

        // ========================================
        // 12.12. CẬP NHẬT CÁC NÚT CHỨC NĂNG
        // ========================================

        updateRescheduleButtonState();

        updateCancelButtonState();

        // STT 20 - Cập nhật nút đánh giá (MỚI)
        updateReviewButtonState();
    }

    // ========================================
    // 13. ĐỊNH DẠNG NGÀY HẸN
    // ========================================

    private String formatBookingDate(
            String bookingDate
    ) {

        if (bookingDate == null) {
            return "--";
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date date = parser.parse(
                    bookingDate
            );

            if (date == null) {
                return bookingDate;
            }

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "EEEE, dd/MM/yyyy",
                            new Locale("vi", "VN")
                    );

            return formatter.format(date);

        } catch (ParseException e) {

            return bookingDate;
        }
    }

    // ========================================
    // 14. KIỂM TRA BOOKING TRONG TƯƠNG LAI
    // ========================================

    private boolean isBookingInFuture(
            Booking booking
    ) {

        if (booking == null) {
            return false;
        }

        String dateTimeText =
                booking.getBookingDate()
                        + " "
                        + booking.getBookingTime();

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date bookingDateTime =
                    parser.parse(
                            dateTimeText
                    );

            return bookingDateTime != null
                    && bookingDateTime.getTime()
                    > System.currentTimeMillis();

        } catch (ParseException e) {

            return false;
        }
    }

    // ========================================
    // 15. LẤY MÀU TRẠNG THÁI BOOKING
    // ========================================

    private int getBookingStatusColor(
            String status
    ) {

        // PENDING - Chờ xác nhận
        if (Booking.STATUS_PENDING.equals(status)) {

            return Color.parseColor("#B26A00");
        }

        // CONFIRMED - Đã xác nhận
        if (Booking.STATUS_CONFIRMED.equals(status)) {

            return Color.parseColor("#2563EB");
        }

        // COMPLETED - Đã hoàn thành
        if (Booking.STATUS_COMPLETED.equals(status)) {

            return Color.parseColor("#16A34A");
        }

        // CANCELLED - Đã hủy
        if (Booking.STATUS_CANCELLED.equals(status)) {

            return Color.parseColor("#DC2626");
        }

        return Color.parseColor("#756F69");
    }

    // ========================================
    // 16. SAO CHÉP MÃ BOOKING
    // ========================================

    private void copyBookingCode() {

        // ========================================
        // 16.1. KIỂM TRA BOOKING
        // ========================================

        if (currentBooking == null) {

            Toast.makeText(
                    this,
                    "Không có mã lịch hẹn để sao chép.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.2. LẤY MÃ BOOKING
        // ========================================

        String bookingCode = normalizeValue(
                currentBooking.getBookingCode()
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Mã lịch hẹn không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.3. LẤY CLIPBOARD MANAGER
        // ========================================

        ClipboardManager clipboardManager =
                (ClipboardManager) getSystemService(
                        Context.CLIPBOARD_SERVICE
                );

        if (clipboardManager == null) {

            Toast.makeText(
                    this,
                    "Không thể sử dụng bộ nhớ tạm.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.4. SAO CHÉP
        // ========================================

        ClipData clipData = ClipData.newPlainText(
                "Mã lịch hẹn",
                bookingCode
        );

        clipboardManager.setPrimaryClip(
                clipData
        );

        Toast.makeText(
                this,
                "Đã sao chép mã lịch hẹn.",
                Toast.LENGTH_SHORT
        ).show();
    }

    // ========================================
    // 17. HIỂN THỊ LỖI ĐỌC BOOKING
    // ========================================

    private void showLoadError(
            String message
    ) {

        // Xóa dữ liệu cũ
        currentBooking = null;
        currentReview = null;

        // Ẩn Loading
        progressCustomerBookingDetail.setVisibility(
                View.GONE
        );

        // Ẩn nội dung
        scrollCustomerBookingDetail.setVisibility(
                View.GONE
        );

        // Hiển thị khu vực lỗi
        layoutCustomerBookingDetailError.setVisibility(
                View.VISIBLE
        );

        txtCustomerBookingDetailError.setText(
                message
        );

        // Khóa sao chép
        btnDetailCopyCode.setEnabled(false);

        // Ẩn Đổi lịch
        btnDetailRescheduleBooking.setEnabled(false);
        btnDetailRescheduleBooking.setVisibility(
                View.GONE
        );

        // Ẩn Hủy lịch
        btnDetailCancelBooking.setEnabled(false);
        btnDetailCancelBooking.setVisibility(
                View.GONE
        );

        // STT 20 - Ẩn Đánh giá (MỚI)
        btnDetailReviewBooking.setEnabled(false);
        btnDetailReviewBooking.setVisibility(
                View.GONE
        );

        // Cho phép quay lại danh sách
        btnDetailBackToList.setEnabled(true);
    }

    // ========================================
    // 18. KIỂM TRA BOOKING CÓ THỂ THAY ĐỔI
    // ========================================

    private boolean canModifyBooking() {

        // Booking phải tồn tại
        if (currentBooking == null) {
            return false;
        }

        // Không đang hủy
        if (isCancelling) {
            return false;
        }

        // Chỉ PENDING hoặc CONFIRMED
        if (!currentBooking.isActive()) {
            return false;
        }

        // Chưa đến giờ bắt đầu
        if (!isBookingInFuture(currentBooking)) {
            return false;
        }

        // Kiểm tra Session
        if (!sessionManager.isCustomer()) {
            return false;
        }

        // Kiểm tra đúng Customer
        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        return customerId != null
                && customerId.equals(
                currentBooking.getCustomerId()
        );
    }

    // ========================================
    // 19. KIỂM TRA ĐIỀU KIỆN ĐỔI LỊCH
    // ========================================

    private boolean canRescheduleBooking() {

        return canModifyBooking();
    }

    // ========================================
    // 20. CẬP NHẬT NÚT ĐỔI LỊCH - STT 16
    // ========================================

    private void updateRescheduleButtonState() {

        boolean canReschedule =
                canRescheduleBooking();

        btnDetailRescheduleBooking.setVisibility(
                canReschedule
                        ? View.VISIBLE
                        : View.GONE
        );

        btnDetailRescheduleBooking.setEnabled(
                canReschedule
        );
    }

    // ========================================
    // 21. MỞ MÀN HÌNH ĐỔI LỊCH - STT 16
    // ========================================

    private void openRescheduleBooking() {

        // ========================================
        // 21.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 21.2. KIỂM TRA ĐIỀU KIỆN
        // ========================================

        if (!canRescheduleBooking()) {

            updateRescheduleButtonState();

            Toast.makeText(
                    this,
                    "Lịch hẹn này hiện không thể đổi thời gian.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 21.3. LẤY MÃ BOOKING
        // ========================================

        String bookingCode = normalizeValue(
                currentBooking.getBookingCode()
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Mã lịch hẹn không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 21.4. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBookingDetailActivity.this,
                CustomerRescheduleBookingActivity.class
        );

        intent.putExtra(
                CustomerRescheduleBookingActivity.EXTRA_BOOKING_CODE,
                bookingCode
        );

        startActivity(intent);
    }

    // ========================================
    // 22. KIỂM TRA ĐIỀU KIỆN HỦY LỊCH
    // ========================================

    private boolean canCancelBooking() {

        // Quy tắc giống đổi lịch:
        // - Booking thuộc Customer
        // - PENDING hoặc CONFIRMED
        // - Chưa đến giờ bắt đầu
        // - Không đang hủy

        return canModifyBooking();
    }

    // ========================================
    // 23. CẬP NHẬT NÚT HỦY LỊCH - STT 17
    // ========================================

    private void updateCancelButtonState() {

        boolean canCancel =
                canCancelBooking();

        btnDetailCancelBooking.setVisibility(
                canCancel
                        ? View.VISIBLE
                        : View.GONE
        );

        btnDetailCancelBooking.setEnabled(
                canCancel
        );
    }

    // ========================================
    // 24. HIỂN THỊ DIALOG XÁC NHẬN HỦY
    // ========================================

    private void confirmCancelBooking() {

        // ========================================
        // 24.1. TRÁNH THAO TÁC LẶP
        // ========================================

        if (isCancelling) {
            return;
        }

        // ========================================
        // 24.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 24.3. KIỂM TRA ĐIỀU KIỆN HỦY
        // ========================================

        if (!canCancelBooking()) {

            updateCancelButtonState();

            Toast.makeText(
                    this,
                    "Lịch hẹn này hiện không thể hủy.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 24.4. LẤY MÃ BOOKING
        // ========================================

        String bookingCode = normalizeValue(
                currentBooking.getBookingCode()
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Mã lịch hẹn không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 24.5. TẠO NỘI DUNG XÁC NHẬN
        // ========================================

        String message =
                "Mã lịch hẹn: "
                        + bookingCode
                        + "\n\n"
                        + "Dịch vụ: "
                        + currentBooking.getServiceName()
                        + "\n"
                        + "Barber: "
                        + currentBooking.getBarberName()
                        + "\n"
                        + "Ngày hẹn: "
                        + formatBookingDate(
                        currentBooking.getBookingDate()
                )
                        + "\n"
                        + "Giờ hẹn: "
                        + currentBooking.getBookingTime()
                        + "\n\n"
                        + "Bạn có chắc chắn muốn hủy lịch hẹn này?";

        // ========================================
        // 24.6. HIỂN THỊ DIALOG
        // ========================================

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận hủy lịch hẹn")
                .setMessage(message)
                .setNegativeButton(
                        "Giữ lịch",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Xác nhận hủy",
                        (dialog, which) -> {

                            // Kiểm tra lại điều kiện
                            // trước khi thực hiện hủy.

                            if (!canCancelBooking()) {

                                updateCancelButtonState();

                                Toast.makeText(
                                        this,
                                        "Lịch hẹn không còn đủ điều kiện hủy.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            performCancelBooking();
                        }
                )
                .show();
    }

    // ========================================
    // 25. THỰC HIỆN HỦY LỊCH - STT 17
    // ========================================

    private void performCancelBooking() {

        // ========================================
        // 25.1. KIỂM TRA THAO TÁC
        // ========================================

        if (isCancelling || !canCancelBooking()) {
            return;
        }

        // ========================================
        // 25.2. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        // ========================================
        // 25.3. LẤY BOOKING CODE
        // ========================================

        final String bookingCode = normalizeValue(
                currentBooking.getBookingCode()
        );

        if (customerId == null || bookingCode == null) {

            Toast.makeText(
                    this,
                    "Thông tin lịch hẹn không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 25.4. KHÓA THAO TÁC HỦY LẶP
        // ========================================

        isCancelling = true;

        showLoading();

        // ========================================
        // 25.5. GỌI REPOSITORY
        // ========================================

        bookingExecutor.execute(() -> {

            BookingRepository.BookingResult result;

            try {

                result = BookingRepository.cancelBooking(
                        appContext,
                        customerId,
                        bookingCode
                );

            } catch (RuntimeException e) {

                result = null;
            }

            final BookingRepository.BookingResult
                    finalResult = result;

            // ========================================
            // 25.6. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isCancelling = false;

                // ========================================
                // 25.7. KIỂM TRA LẠI SESSION
                // ========================================

                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeValue(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                // ========================================
                // 25.8. HỦY THÀNH CÔNG
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()) {

                    // Tải lại Booking để cập nhật trạng thái
                    loadBookingDetail();

                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Hủy lịch thành công")
                            .setMessage(
                                    "Lịch hẹn "
                                            + bookingCode
                                            + " đã được hủy.\n\n"
                                            + "Trạng thái lịch hẹn "
                                            + "đã được cập nhật."
                            )
                            .setPositiveButton(
                                    "Đã hiểu",
                                    (dialog, which) ->
                                            dialog.dismiss()
                            )
                            .show();

                    return;
                }

                // ========================================
                // 25.9. HỦY THẤT BẠI
                // ========================================

                String errorMessage =
                        finalResult != null
                                ? normalizeValue(
                                finalResult.getMessage()
                        )
                                : null;

                if (errorMessage == null) {

                    errorMessage =
                            "Không thể hủy lịch hẹn. "
                                    + "Vui lòng thử lại.";
                }

                // Tải lại trạng thái mới nhất
                loadBookingDetail();

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Hủy lịch chưa thành công")
                        .setMessage(errorMessage)
                        .setPositiveButton(
                                "Đã hiểu",
                                (dialog, which) ->
                                        dialog.dismiss()
                        )
                        .show();
            });
        });
    }

    // ========================================
    // 26. CẬP NHẬT NÚT ĐÁNH GIÁ - STT 20 (MỚI)
    // ========================================

    private void updateReviewButtonState() {

        // ========================================
        // 26.1. KIỂM TRA ĐIỀU KIỆN
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        boolean canReview =
                currentBooking != null
                        && !isCancelling
                        && sessionManager.isCustomer()
                        && customerId != null
                        && customerId.equals(
                        currentBooking.getCustomerId()
                )
                        && Booking.STATUS_COMPLETED.equals(
                        currentBooking.getStatus()
                );

        // ========================================
        // 26.2. HIỂN THỊ NÚT
        // ========================================

        btnDetailReviewBooking.setVisibility(
                canReview
                        ? View.VISIBLE
                        : View.GONE
        );

        btnDetailReviewBooking.setEnabled(
                canReview
        );

        // ========================================
        // 26.3. CẬP NHẬT NỘI DUNG NÚT
        // ========================================

        if (canReview) {

            // Chưa có Review: cho phép đánh giá.
            //
            // Đã có Review: cho phép xem lại.

            btnDetailReviewBooking.setText(
                    currentReview == null
                            ? "Đánh giá dịch vụ"
                            : "Xem đánh giá"
            );
        }
    }

    // ========================================
    // 27. MỞ MÀN HÌNH ĐÁNH GIÁ - STT 20 (MỚI)
    // ========================================

    private void openReviewBooking() {

        // ========================================
        // 27.1. KIỂM TRA BOOKING
        // ========================================

        if (isCancelling || currentBooking == null) {
            return;
        }

        // ========================================
        // 27.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 27.3. KIỂM TRA ĐIỀU KIỆN ĐÁNH GIÁ
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        boolean canReview =
                customerId != null
                        && customerId.equals(
                        currentBooking.getCustomerId()
                )
                        && Booking.STATUS_COMPLETED.equals(
                        currentBooking.getStatus()
                );

        if (!canReview) {

            updateReviewButtonState();

            Toast.makeText(
                    this,
                    "Lịch hẹn chưa đủ điều kiện đánh giá.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 27.4. LẤY BOOKING CODE
        // ========================================

        String bookingCode = normalizeValue(
                currentBooking.getBookingCode()
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Mã lịch hẹn không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 27.5. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBookingDetailActivity.this,
                CustomerReviewActivity.class
        );

        // ========================================
        // 27.6. TRUYỀN BOOKING CODE
        // ========================================

        intent.putExtra(
                CustomerReviewActivity.EXTRA_BOOKING_CODE,
                bookingCode
        );

        // ========================================
        // 27.7. MỞ MÀN HÌNH ĐÁNH GIÁ
        // ========================================

        startActivity(intent);

        // Sau khi quay lại:
        // onResume() sẽ đọc lại Review.
        //
        // Nếu Customer đã gửi đánh giá,
        // nút được đổi thành "Xem đánh giá".
    }

    // ========================================
    // 28. CHUẨN HÓA CHUỖI
    // ========================================

    private String normalizeValue(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }

    // ========================================
    // 29. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBookingDetailActivity.this,
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
    // 30. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        // Không tiếp nhận thêm tác vụ mới
        bookingExecutor.shutdown();

        super.onDestroy();
    }
}



