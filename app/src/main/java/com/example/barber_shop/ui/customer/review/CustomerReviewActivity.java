
package com.example.barber_shop.ui.customer.review;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RatingBar;
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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerReviewActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // Sử dụng cùng key với Booking Detail
    // để giữ thống nhất giữa các màn hình.
    public static final String EXTRA_BOOKING_CODE =
            "extra_booking_code";

    // ========================================
    // 2. KHAI BÁO VIEW
    // ========================================

    // Toolbar
    private MaterialToolbar toolbarCustomerReview;

    // ========================================
    // 2.1. THÔNG TIN BOOKING
    // ========================================

    private TextView txtCustomerReviewBookingCode;
    private TextView txtCustomerReviewServiceName;
    private TextView txtCustomerReviewBarberName;
    private TextView txtCustomerReviewBookingDateTime;

    // ========================================
    // 2.2. CHẤM ĐIỂM BARBER
    // ========================================

    private RatingBar ratingCustomerBarber;

    private TextView txtCustomerBarberRatingValue;

    // ========================================
    // 2.3. CHẤM ĐIỂM DỊCH VỤ
    // ========================================

    private RatingBar ratingCustomerService;

    private TextView txtCustomerServiceRatingValue;

    // ========================================
    // 2.4. NHẬN XÉT
    // ========================================

    private TextInputLayout layoutCustomerReviewComment;

    private TextInputEditText edtCustomerReviewComment;

    // ========================================
    // 2.5. GỬI ĐÁNH GIÁ
    // ========================================

    private MaterialButton btnCustomerSubmitReview;

    private ProgressBar progressCustomerSubmitReview;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Booking hiện tại
    private Booking currentBooking;

    // Mã Booking nhận từ Intent
    private String currentBookingCode;

    // Đánh giá đã lưu, nếu có
    private Review existingReview;

    // ========================================
    // 3.1. TRẠNG THÁI XỬ LÝ
    // ========================================

    // Đã tải và xác minh Booking thành công?
    private boolean isReady = false;

    // Đang tải thông tin Booking/Review?
    private boolean isLoading = false;

    // Đang gửi đánh giá?
    private boolean isSubmitting = false;

    // ========================================
    // 3.2. EXECUTOR
    // ========================================

    // Các thao tác đọc/ghi SharedPreferences
    // được thực hiện bên ngoài Main Thread.
    private final ExecutorService reviewExecutor =
            Executors.newSingleThreadExecutor();

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

        // Chỉ Customer đã đăng nhập được truy cập
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 4.2. KÍCH HOẠT EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 4.3. HIỂN THỊ XML
        // ========================================

        setContentView(
                R.layout.activity_customer_review
        );

        // ========================================
        // 4.4. KHỞI TẠO CÁC THÀNH PHẦN
        // ========================================

        setupWindowInsets();
        initViews();
        setupListeners();

        // ========================================
        // 4.5. NHẬN BOOKING CODE
        // ========================================

        currentBookingCode = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BOOKING_CODE
                )
        );

        if (currentBookingCode == null) {

            showErrorAndFinish(
                    "Không tìm thấy mã lịch hẹn cần đánh giá."
            );

            return;
        }

        // ========================================
        // 4.6. TẢI THÔNG TIN ĐÁNH GIÁ
        // ========================================

        loadReviewData();
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

        // Kiểm tra Customer còn đăng nhập
        if (!sessionManager.isCustomer()) {

            openLogin();
        }

        // Không tự tải lại form tại đây,
        // tránh làm mất nội dung Customer
        // đang nhập nhưng chưa gửi.
        //
        // Repository sẽ kiểm tra lại Booking
        // và đánh giá trùng khi thực sự gửi.
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerReviewRoot
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

        toolbarCustomerReview = findViewById(
                R.id.toolbarCustomerReview
        );

        // ========================================
        // 7.2. THÔNG TIN BOOKING
        // ========================================

        txtCustomerReviewBookingCode = findViewById(
                R.id.txtCustomerReviewBookingCode
        );

        txtCustomerReviewServiceName = findViewById(
                R.id.txtCustomerReviewServiceName
        );

        txtCustomerReviewBarberName = findViewById(
                R.id.txtCustomerReviewBarberName
        );

        txtCustomerReviewBookingDateTime = findViewById(
                R.id.txtCustomerReviewBookingDateTime
        );

        // ========================================
        // 7.3. ĐÁNH GIÁ BARBER
        // ========================================

        ratingCustomerBarber = findViewById(
                R.id.ratingCustomerBarber
        );

        txtCustomerBarberRatingValue = findViewById(
                R.id.txtCustomerBarberRatingValue
        );

        // ========================================
        // 7.4. ĐÁNH GIÁ DỊCH VỤ
        // ========================================

        ratingCustomerService = findViewById(
                R.id.ratingCustomerService
        );

        txtCustomerServiceRatingValue = findViewById(
                R.id.txtCustomerServiceRatingValue
        );

        // ========================================
        // 7.5. NHẬN XÉT
        // ========================================

        layoutCustomerReviewComment = findViewById(
                R.id.layoutCustomerReviewComment
        );

        edtCustomerReviewComment = findViewById(
                R.id.edtCustomerReviewComment
        );

        // ========================================
        // 7.6. NÚT GỬI ĐÁNH GIÁ
        // ========================================

        btnCustomerSubmitReview = findViewById(
                R.id.btnCustomerSubmitReview
        );

        // ========================================
        // 7.7. PROGRESSBAR
        // ========================================

        progressCustomerSubmitReview = findViewById(
                R.id.progressCustomerSubmitReview
        );

        // ========================================
        // 7.8. TRẠNG THÁI BAN ĐẦU
        // ========================================

        // Chưa đọc dữ liệu nên chưa được gửi
        setReviewInputEnabled(false);

        updateRatingLabels();
        updateSubmitButton();
    }

    // ========================================
    // 8. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 8.1. QUAY LẠI
        // ========================================

        toolbarCustomerReview.setNavigationOnClickListener(
                v -> finish()
        );

        // ========================================
        // 8.2. CHẤM SAO BARBER
        // ========================================

        ratingCustomerBarber.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    // Cập nhật nội dung hiển thị
                    updateRatingLabels();

                    // Kiểm tra điều kiện gửi
                    updateSubmitButton();
                }
        );

        // ========================================
        // 8.3. CHẤM SAO DỊCH VỤ
        // ========================================

        ratingCustomerService.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    updateRatingLabels();

                    updateSubmitButton();
                }
        );

        // ========================================
        // 8.4. NÚT GỬI ĐÁNH GIÁ
        // ========================================

        btnCustomerSubmitReview.setOnClickListener(
                v -> submitReview()
        );
    }

    // ========================================
    // 9. TẢI THÔNG TIN BOOKING VÀ REVIEW
    // ========================================

    private void loadReviewData() {

        // ========================================
        // 9.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 9.2. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // ========================================
        // 9.3. KIỂM TRA BOOKING CODE
        // ========================================

        if (currentBookingCode == null) {

            showErrorAndFinish(
                    "Mã lịch hẹn không hợp lệ."
            );

            return;
        }

        // ========================================
        // 9.4. HIỂN THỊ LOADING
        // ========================================

        isLoading = true;
        isReady = false;

        progressCustomerSubmitReview.setVisibility(
                View.VISIBLE
        );

        setReviewInputEnabled(false);
        updateSubmitButton();

        final Context appContext =
                getApplicationContext();

        final String bookingCode =
                currentBookingCode;

        // ========================================
        // 9.5. ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        reviewExecutor.execute(() -> {

            Booking loadedBooking = null;
            Review loadedReview = null;

            String errorMessage = null;

            try {

                // ========================================
                // 9.6. TÌM BOOKING THEO MÃ
                // ========================================

                loadedBooking =
                        BookingRepository.findByCode(
                                appContext,
                                bookingCode
                        );

                if (loadedBooking == null) {

                    errorMessage =
                            "Không tìm thấy lịch hẹn cần đánh giá.";

                    // ========================================
                    // 9.7. KIỂM TRA CUSTOMER SỞ HỮU
                    // ========================================

                } else if (!customerId.equals(
                        loadedBooking.getCustomerId()
                )) {

                    errorMessage =
                            "Bạn không có quyền đánh giá lịch hẹn này.";

                    // ========================================
                    // 9.8. KIỂM TRA COMPLETED
                    // ========================================

                } else if (!Booking.STATUS_COMPLETED.equals(
                        loadedBooking.getStatus()
                )) {

                    errorMessage =
                            "Chỉ có thể đánh giá lịch hẹn đã hoàn thành.";

                } else {

                    // ========================================
                    // 9.9. TÌM ĐÁNH GIÁ ĐÃ GỬI
                    // ========================================

                    loadedReview =
                            ReviewRepository.getReviewForBooking(
                                    appContext,
                                    customerId,
                                    bookingCode
                            );
                }

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể tải thông tin đánh giá.";
            }

            final Booking resultBooking = loadedBooking;
            final Review resultReview = loadedReview;
            final String resultError = errorMessage;

            // ========================================
            // 9.10. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // ========================================
                // 9.11. KIỂM TRA LẠI SESSION
                // ========================================

                if (!isSameCustomer(customerId)) {

                    openLogin();
                    return;
                }

                isLoading = false;

                progressCustomerSubmitReview.setVisibility(
                        View.GONE
                );

                // ========================================
                // 9.12. XỬ LÝ LỖI
                // ========================================

                if (resultError != null) {

                    showErrorAndFinish(resultError);
                    return;
                }

                // ========================================
                // 9.13. HIỂN THỊ THÔNG TIN BOOKING
                // ========================================

                currentBooking = resultBooking;

                displayBookingInfo();

                isReady = true;

                // ========================================
                // 9.14. KIỂM TRA ĐÃ ĐÁNH GIÁ
                // ========================================

                existingReview = resultReview;

                if (existingReview != null) {

                    // Đã đánh giá: chỉ xem
                    displayExistingReview();

                } else {

                    // Chưa đánh giá: cho nhập
                    enableNewReview();
                }

                updateSubmitButton();
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
        // 10.1. MÃ BOOKING
        // ========================================

        txtCustomerReviewBookingCode.setText(
                "Mã lịch hẹn: "
                        + currentBooking.getBookingCode()
        );

        // ========================================
        // 10.2. TÊN DỊCH VỤ
        // ========================================

        txtCustomerReviewServiceName.setText(
                currentBooking.getServiceName()
        );

        // ========================================
        // 10.3. TÊN BARBER
        // ========================================

        txtCustomerReviewBarberName.setText(
                "Barber: "
                        + currentBooking.getBarberName()
        );

        // ========================================
        // 10.4. NGÀY VÀ GIỜ
        // ========================================

        String bookingDate = normalizeValue(
                currentBooking.getBookingDate()
        );

        String bookingTime = normalizeValue(
                currentBooking.getBookingTime()
        );

        if (bookingDate == null) {
            bookingDate = "--";
        }

        if (bookingTime == null) {
            bookingTime = "--";
        }

        txtCustomerReviewBookingDateTime.setText(
                "Ngày hẹn: "
                        + bookingDate
                        + " - "
                        + bookingTime
        );
    }

    // ========================================
    // 11. BẬT FORM ĐÁNH GIÁ MỚI
    // ========================================

    private void enableNewReview() {

        // ========================================
        // 11.1. KIỂM TRA ĐIỀU KIỆN
        // ========================================

        if (!isReady || currentBooking == null) {
            return;
        }

        // ========================================
        // 11.2. CHƯA CÓ ĐÁNH GIÁ
        // ========================================

        existingReview = null;

        // ========================================
        // 11.3. CHO PHÉP NHẬP
        // ========================================

        setReviewInputEnabled(true);

        // ========================================
        // 11.4. CẬP NHẬT GIAO DIỆN
        // ========================================

        updateRatingLabels();
        updateSubmitButton();
    }

    // ========================================
    // 12. HIỂN THỊ ĐÁNH GIÁ ĐÃ GỬI
    // ========================================

    private void displayExistingReview() {

        if (existingReview == null) {
            return;
        }

        // ========================================
        // 12.1. CHẶN CHỈNH SỬA
        // ========================================

        setReviewInputEnabled(false);

        // ========================================
        // 12.2. HIỂN THỊ SỐ SAO BARBER
        // ========================================

        ratingCustomerBarber.setRating(
                existingReview.getBarberRating()
        );

        // ========================================
        // 12.3. HIỂN THỊ SỐ SAO DỊCH VỤ
        // ========================================

        ratingCustomerService.setRating(
                existingReview.getServiceRating()
        );

        // ========================================
        // 12.4. HIỂN THỊ NHẬN XÉT
        // ========================================

        edtCustomerReviewComment.setText(
                existingReview.getComment()
        );

        // ========================================
        // 12.5. CẬP NHẬT NHÃN
        // ========================================

        updateRatingLabels();

        updateSubmitButton();
    }

    // ========================================
    // 13. BẬT/TẮT CÁC Ô NHẬP
    // ========================================

    private void setReviewInputEnabled(
            boolean enabled
    ) {

        // ========================================
        // 13.1. CHẤM SAO BARBER
        // ========================================

        // isIndicator = true:
        // Chỉ hiển thị, không cho thay đổi.

        ratingCustomerBarber.setIsIndicator(
                !enabled
        );

        // ========================================
        // 13.2. CHẤM SAO DỊCH VỤ
        // ========================================

        ratingCustomerService.setIsIndicator(
                !enabled
        );

        // ========================================
        // 13.3. Ô NHẬN XÉT
        // ========================================

        layoutCustomerReviewComment.setEnabled(
                enabled
        );

        edtCustomerReviewComment.setEnabled(
                enabled
        );
    }

    // ========================================
    // 14. HIỂN THỊ GIÁ TRỊ SỐ SAO
    // ========================================

    private void updateRatingLabels() {

        // ========================================
        // 14.1. SỐ SAO BARBER
        // ========================================

        int barberStars = Math.round(
                ratingCustomerBarber.getRating()
        );

        if (barberStars == 0) {

            txtCustomerBarberRatingValue.setText(
                    "Chưa chọn số sao"
            );

        } else {

            txtCustomerBarberRatingValue.setText(
                    String.format(
                            Locale.getDefault(),
                            "%d/5 sao",
                            barberStars
                    )
            );
        }

        // ========================================
        // 14.2. SỐ SAO DỊCH VỤ
        // ========================================

        int serviceStars = Math.round(
                ratingCustomerService.getRating()
        );

        if (serviceStars == 0) {

            txtCustomerServiceRatingValue.setText(
                    "Chưa chọn số sao"
            );

        } else {

            txtCustomerServiceRatingValue.setText(
                    String.format(
                            Locale.getDefault(),
                            "%d/5 sao",
                            serviceStars
                    )
            );
        }
    }

    // ========================================
    // 15. CẬP NHẬT NÚT GỬI ĐÁNH GIÁ
    // ========================================

    private void updateSubmitButton() {

        // ========================================
        // 15.1. ĐANG GỬI ĐÁNH GIÁ
        // ========================================

        if (isSubmitting) {

            btnCustomerSubmitReview.setText(
                    "Đang gửi đánh giá..."
            );

            btnCustomerSubmitReview.setEnabled(false);

            return;
        }

        // ========================================
        // 15.2. ĐÃ ĐÁNH GIÁ
        // ========================================

        if (existingReview != null) {

            btnCustomerSubmitReview.setText(
                    "Đã gửi đánh giá"
            );

            btnCustomerSubmitReview.setEnabled(false);

            return;
        }

        // ========================================
        // 15.3. ĐÁNH GIÁ MỚI
        // ========================================

        btnCustomerSubmitReview.setText(
                "Gửi đánh giá"
        );

        // ========================================
        // 15.4. KIỂM TRA ĐỦ HAI MỨC SAO
        // ========================================

        boolean hasBarberRating =
                Review.isValidRating(
                        Math.round(
                                ratingCustomerBarber.getRating()
                        )
                );

        boolean hasServiceRating =
                Review.isValidRating(
                        Math.round(
                                ratingCustomerService.getRating()
                        )
                );

        // ========================================
        // 15.5. CHỈ BẬT NÚT KHI HỢP LỆ
        // ========================================

        boolean canSubmit =
                isReady
                        && !isLoading
                        && !isSubmitting
                        && currentBooking != null
                        && hasBarberRating
                        && hasServiceRating;

        btnCustomerSubmitReview.setEnabled(
                canSubmit
        );
    }

    // ========================================
    // 16. GỬI ĐÁNH GIÁ
    // ========================================

    private void submitReview() {

        // ========================================
        // 16.1. KIỂM TRA TRẠNG THÁI
        // ========================================

        if (!isReady
                || isLoading
                || isSubmitting
                || existingReview != null
                || currentBooking == null) {

            return;
        }

        // ========================================
        // 16.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 16.3. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy thông tin Customer.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.4. KIỂM TRA QUYỀN SỞ HỮU BOOKING
        // ========================================

        if (!customerId.equals(
                currentBooking.getCustomerId()
        )) {

            Toast.makeText(
                    this,
                    "Bạn không có quyền đánh giá lịch hẹn này.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.5. KIỂM TRA TRẠNG THÁI COMPLETED
        // ========================================

        if (!Booking.STATUS_COMPLETED.equals(
                currentBooking.getStatus()
        )) {

            Toast.makeText(
                    this,
                    "Chỉ có thể đánh giá lịch hẹn đã hoàn thành.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.6. LẤY SỐ SAO
        // ========================================

        final int barberRating = Math.round(
                ratingCustomerBarber.getRating()
        );

        final int serviceRating = Math.round(
                ratingCustomerService.getRating()
        );

        // ========================================
        // 16.7. KIỂM TRA SỐ SAO
        // ========================================

        if (!Review.isValidRating(barberRating)
                || !Review.isValidRating(serviceRating)) {

            Toast.makeText(
                    this,
                    "Vui lòng chấm điểm Barber và dịch vụ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.8. LẤY NỘI DUNG NHẬN XÉT
        // ========================================

        String comment = "";

        if (edtCustomerReviewComment.getText() != null) {

            comment = edtCustomerReviewComment
                    .getText()
                    .toString()
                    .trim();
        }

        if (comment.length()
                > Review.MAX_COMMENT_LENGTH) {

            layoutCustomerReviewComment.setError(
                    "Nhận xét không được vượt quá 500 ký tự."
            );

            return;
        }

        layoutCustomerReviewComment.setError(null);

        final String reviewComment = comment;

        final String bookingCode =
                currentBooking.getBookingCode();

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 16.9. KHÓA FORM KHI ĐANG GỬI
        // ========================================

        isSubmitting = true;

        setReviewInputEnabled(false);
        updateSubmitButton();

        progressCustomerSubmitReview.setVisibility(
                View.VISIBLE
        );

        // ========================================
        // 16.10. GỌI REPOSITORY TRÊN LUỒNG RIÊNG
        // ========================================

        reviewExecutor.execute(() -> {

            ReviewRepository.ReviewResult result;

            try {

                result = ReviewRepository.submitReview(
                        appContext,
                        customerId,
                        bookingCode,
                        barberRating,
                        serviceRating,
                        reviewComment
                );

            } catch (RuntimeException e) {

                result = null;
            }

            final ReviewRepository.ReviewResult
                    finalResult = result;

            // ========================================
            // 16.11. TRỞ VỀ MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Kiểm tra lại Customer
                if (!isSameCustomer(customerId)) {

                    openLogin();
                    return;
                }

                // ========================================
                // 16.12. KẾT THÚC LOADING
                // ========================================

                isSubmitting = false;

                progressCustomerSubmitReview.setVisibility(
                        View.GONE
                );

                // ========================================
                // 16.13. XỬ LÝ THÀNH CÔNG
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()
                        && finalResult.getReview() != null) {

                    existingReview =
                            finalResult.getReview();

                    // Hiển thị đánh giá đã gửi
                    // ở chế độ chỉ xem.
                    displayExistingReview();

                    Toast.makeText(
                            this,
                            finalResult.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // ========================================
                // 16.14. XỬ LÝ THẤT BẠI
                // ========================================

                String message = finalResult != null
                        ? finalResult.getMessage()
                        : null;

                if (message == null
                        || message.trim().isEmpty()) {

                    message =
                            "Không thể gửi đánh giá. "
                                    + "Vui lòng thử lại.";
                }

                // Cho phép nhập lại sau lỗi.
                setReviewInputEnabled(true);
                updateSubmitButton();

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                ).show();
            });
        });
    }

    // ========================================
    // 17. KIỂM TRA ĐÚNG CUSTOMER HIỆN TẠI
    // ========================================

    private boolean isSameCustomer(
            String expectedCustomerId
    ) {

        if (!sessionManager.isCustomer()) {
            return false;
        }

        String currentCustomerId = normalizeValue(
                sessionManager.getUserId()
        );

        return currentCustomerId != null
                && currentCustomerId.equals(
                expectedCustomerId
        );
    }

    // ========================================
    // 18. CHUẨN HÓA CHUỖI
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
    // 19. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerReviewActivity.this,
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
    // 20. XỬ LÝ LỖI VÀ ĐÓNG ACTIVITY
    // ========================================

    private void showErrorAndFinish(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();

        finish();
    }

    // ========================================
    // 21. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        reviewExecutor.shutdown();

        super.onDestroy();
    }
}
