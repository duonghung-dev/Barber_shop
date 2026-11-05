
package com.example.barber_shop.ui.customer.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.service.CustomerServiceListActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerMyBookingsActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC LOẠI BỘ LỌC
    // ========================================

    private static final int FILTER_ALL = 0;
    private static final int FILTER_UPCOMING = 1;
    private static final int FILTER_HISTORY = 2;

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerMyBookings;

    private ChipGroup chipGroupCustomerBookingFilters;

    private TextView txtCustomerBookingCount;

    private RecyclerView recyclerCustomerBookings;

    private LinearLayout layoutEmptyCustomerBookings;

    private TextView txtEmptyCustomerBookingsTitle;
    private TextView txtEmptyCustomerBookingsSubtitle;

    private ProgressBar progressCustomerBookings;

    private MaterialButton btnCustomerCreateNewBooking;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Adapter của RecyclerView
    private CustomerBookingAdapter bookingAdapter;

    // Toàn bộ Booking của Customer đang đăng nhập
    private final List<Booking> allBookings =
            new ArrayList<>();

    // Bộ lọc đang được chọn
    private int currentFilter = FILTER_ALL;

    // Xử lý đọc dữ liệu trên luồng riêng
    private final ExecutorService bookingExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt các lần tải dữ liệu
    private int loadRequestVersion = 0;

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
                R.layout.activity_customer_my_bookings
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();
        setupRecyclerView();
        setupListeners();

        // Mặc định chọn bộ lọc Tất cả
        chipGroupCustomerBookingFilters.check(
                R.id.chipCustomerBookingAll
        );
    }

    // ========================================
    // 5. TẢI LẠI DỮ LIỆU KHI QUAY VỀ
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

        // Mỗi lần quay lại sẽ đọc dữ liệu mới nhất
        loadBookings();
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerMyBookingsRoot
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
        toolbarCustomerMyBookings = findViewById(
                R.id.toolbarCustomerMyBookings
        );

        // Bộ lọc
        chipGroupCustomerBookingFilters = findViewById(
                R.id.chipGroupCustomerBookingFilters
        );

        // Số lượng kết quả
        txtCustomerBookingCount = findViewById(
                R.id.txtCustomerBookingCount
        );

        // RecyclerView
        recyclerCustomerBookings = findViewById(
                R.id.recyclerCustomerBookings
        );

        // Trạng thái không có lịch
        layoutEmptyCustomerBookings = findViewById(
                R.id.layoutEmptyCustomerBookings
        );

        txtEmptyCustomerBookingsTitle = findViewById(
                R.id.txtEmptyCustomerBookingsTitle
        );

        txtEmptyCustomerBookingsSubtitle = findViewById(
                R.id.txtEmptyCustomerBookingsSubtitle
        );

        // Loading
        progressCustomerBookings = findViewById(
                R.id.progressCustomerBookings
        );

        // Đặt lịch mới
        btnCustomerCreateNewBooking = findViewById(
                R.id.btnCustomerCreateNewBooking
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter
        bookingAdapter = new CustomerBookingAdapter(
                this::showBookingDetails
        );

        // Hiển thị theo chiều dọc
        recyclerCustomerBookings.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter
        recyclerCustomerBookings.setAdapter(
                bookingAdapter
        );
    }

    // ========================================
    // 9. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 9.1. QUAY VỀ MÀN HÌNH TRƯỚC
        // ========================================

        toolbarCustomerMyBookings
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 9.2. THAY ĐỔI BỘ LỌC
        // ========================================

        chipGroupCustomerBookingFilters
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (checkedIds.isEmpty()) {
                                return;
                            }

                            int checkedId = checkedIds.get(0);

                            if (checkedId
                                    == R.id.chipCustomerBookingUpcoming) {

                                currentFilter = FILTER_UPCOMING;

                            } else if (checkedId
                                    == R.id.chipCustomerBookingHistory) {

                                currentFilter = FILTER_HISTORY;

                            } else {

                                currentFilter = FILTER_ALL;
                            }

                            // Lọc lại dữ liệu đang có
                            applyBookingFilter();
                        }
                );

        // ========================================
        // 9.3. ĐẶT LỊCH MỚI
        // ========================================

        btnCustomerCreateNewBooking
                .setOnClickListener(
                        v -> openNewBooking()
                );
    }

    // ========================================
    // 10. ĐỌC BOOKING TỪ REPOSITORY
    // ========================================

    private void loadBookings() {

        // ========================================
        // 10.1. LẤY CUSTOMER ID
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            showLoadingError(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // ========================================
        // 10.2. HIỂN THỊ TRẠNG THÁI ĐANG TẢI
        // ========================================

        progressCustomerBookings.setVisibility(
                View.VISIBLE
        );

        recyclerCustomerBookings.setVisibility(
                View.GONE
        );

        layoutEmptyCustomerBookings.setVisibility(
                View.GONE
        );

        txtCustomerBookingCount.setText(
                "Đang tải lịch hẹn..."
        );

        // Tạo mã cho lần tải hiện tại
        final int requestVersion =
                ++loadRequestVersion;

        // ========================================
        // 10.3. ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        bookingExecutor.execute(() -> {

            List<Booking> loadedBookings = null;

            String errorMessage = null;

            try {

                loadedBookings =
                        BookingRepository.getBookingsForCustomer(
                                getApplicationContext(),
                                customerId
                        );

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc danh sách lịch hẹn.";
            }

            final List<Booking> resultBookings =
                    loadedBookings;

            final String resultError =
                    errorMessage;

            // ========================================
            // 10.4. CẬP NHẬT GIAO DIỆN
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả từ lần tải cũ
                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // Kiểm tra phiên đăng nhập lần nữa
                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeValue(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                progressCustomerBookings.setVisibility(
                        View.GONE
                );

                // ========================================
                // 10.5. XỬ LÝ LỖI
                // ========================================

                if (resultError != null) {

                    showLoadingError(resultError);
                    return;
                }

                // ========================================
                // 10.6. CẬP NHẬT DANH SÁCH GỐC
                // ========================================

                allBookings.clear();

                if (resultBookings != null) {
                    allBookings.addAll(resultBookings);
                }

                // Áp dụng bộ lọc hiện tại
                applyBookingFilter();
            });
        });
    }

    // ========================================
    // 11. LỌC DANH SÁCH BOOKING
    // ========================================

    private void applyBookingFilter() {

        List<Booking> filteredBookings =
                new ArrayList<>();

        // ========================================
        // 11.1. DUYỆT TOÀN BỘ BOOKING
        // ========================================

        for (Booking booking : allBookings) {

            if (booking == null) {
                continue;
            }

            boolean includeBooking;

            switch (currentFilter) {

                // ========================================
                // SẮP TỚI
                // ========================================

                case FILTER_UPCOMING:

                    includeBooking =
                            isUpcomingBooking(booking);

                    break;

                // ========================================
                // LỊCH SỬ
                // ========================================

                case FILTER_HISTORY:

                    includeBooking =
                            !isUpcomingBooking(booking);

                    break;

                // ========================================
                // TẤT CẢ
                // ========================================

                case FILTER_ALL:
                default:

                    includeBooking = true;
                    break;
            }

            if (includeBooking) {
                filteredBookings.add(booking);
            }
        }

        // ========================================
        // 11.2. CẬP NHẬT ADAPTER
        // ========================================

        bookingAdapter.submitList(filteredBookings);

        // ========================================
        // 11.3. CẬP NHẬT SỐ LƯỢNG
        // ========================================

        txtCustomerBookingCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d lịch hẹn",
                        filteredBookings.size()
                )
        );

        // ========================================
        // 11.4. HIỂN THỊ DANH SÁCH HOẶC EMPTY
        // ========================================

        if (filteredBookings.isEmpty()) {

            recyclerCustomerBookings.setVisibility(
                    View.GONE
            );

            showEmptyState();

        } else {

            layoutEmptyCustomerBookings.setVisibility(
                    View.GONE
            );

            recyclerCustomerBookings.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ========================================
    // 12. KIỂM TRA BOOKING SẮP TỚI
    // ========================================

    private boolean isUpcomingBooking(
            Booking booking
    ) {

        // Chỉ lịch PENDING hoặc CONFIRMED
        // mới có thể thuộc danh sách sắp tới.

        if (!booking.isActive()) {
            return false;
        }

        // Kiểm tra thời gian
        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date bookingDateTime = parser.parse(
                    booking.getBookingDate()
                            + " "
                            + booking.getBookingTime()
            );

            return bookingDateTime != null
                    && bookingDateTime.getTime()
                    > System.currentTimeMillis();

        } catch (ParseException e) {

            return false;
        }
    }

    // ========================================
    // 13. HIỂN THỊ TRẠNG THÁI TRỐNG
    // ========================================

    private void showEmptyState() {

        layoutEmptyCustomerBookings.setVisibility(
                View.VISIBLE
        );

        if (currentFilter == FILTER_UPCOMING) {

            txtEmptyCustomerBookingsTitle.setText(
                    "Không có lịch hẹn sắp tới"
            );

            txtEmptyCustomerBookingsSubtitle.setText(
                    "Bạn chưa có lịch hẹn nào "
                            + "đang chờ trong thời gian tới."
            );

        } else if (currentFilter == FILTER_HISTORY) {

            txtEmptyCustomerBookingsTitle.setText(
                    "Chưa có lịch sử đặt lịch"
            );

            txtEmptyCustomerBookingsSubtitle.setText(
                    "Các lịch đã hoàn thành, đã hủy "
                            + "hoặc đã qua giờ sẽ xuất hiện tại đây."
            );

        } else {

            txtEmptyCustomerBookingsTitle.setText(
                    "Chưa có lịch hẹn"
            );

            txtEmptyCustomerBookingsSubtitle.setText(
                    "Bạn có thể bắt đầu đặt lịch "
                            + "để trải nghiệm dịch vụ."
            );
        }
    }

    // ========================================
    // 14. HIỂN THỊ LỖI ĐỌC DỮ LIỆU
    // ========================================

    private void showLoadingError(String message) {

        progressCustomerBookings.setVisibility(
                View.GONE
        );

        recyclerCustomerBookings.setVisibility(
                View.GONE
        );

        layoutEmptyCustomerBookings.setVisibility(
                View.VISIBLE
        );

        txtCustomerBookingCount.setText(
                "Không thể tải lịch hẹn"
        );

        txtEmptyCustomerBookingsTitle.setText(
                "Đã xảy ra lỗi"
        );

        txtEmptyCustomerBookingsSubtitle.setText(
                message
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ========================================
    // 15. MỞ MÀN HÌNH CHI TIẾT LỊCH HẸN
    // ========================================

    private void showBookingDetails(Booking booking) {

        // ========================================
        // 15.1. KIỂM TRA BOOKING ĐƯỢC CHỌN
        // ========================================

        if (booking == null) {
            return;
        }

        // ========================================
        // 15.2. KIỂM TRA QUYỀN CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        // Chỉ cho xem Booking của Customer hiện tại
        if (customerId == null
                || !customerId.equals(
                booking.getCustomerId()
        )) {

            Toast.makeText(
                    this,
                    "Bạn không có quyền xem lịch hẹn này.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 15.3. LẤY MÃ BOOKING
        // ========================================

        String bookingCode = normalizeValue(
                booking.getBookingCode()
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
        // 15.4. ĐIỀU HƯỚNG ĐẾN STT 15
        // ========================================

        Intent intent = new Intent(
                CustomerMyBookingsActivity.this,
                CustomerBookingDetailActivity.class
        );

        // Chỉ truyền mã Booking.
        // Màn hình chi tiết sẽ tự đọc lại dữ liệu
        // từ BookingRepository và kiểm tra quyền
        // sở hữu của Customer thêm một lần nữa.
        intent.putExtra(
                CustomerBookingDetailActivity.EXTRA_BOOKING_CODE,
                bookingCode
        );

        // Mở màn hình Chi tiết lịch hẹn.
        // Không gọi finish() để Customer có thể
        // quay lại danh sách bằng nút Back.
        startActivity(intent);
    }

    // ========================================
    // 16. MỞ QUY TRÌNH ĐẶT LỊCH MỚI
    // ========================================

    private void openNewBooking() {

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Mở danh sách dịch vụ Customer
        Intent intent = new Intent(
                CustomerMyBookingsActivity.this,
                CustomerServiceListActivity.class
        );

        // Bắt đầu quy trình Booking
        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BOOKING_MODE,
                true
        );

        startActivity(intent);
    }

    // ========================================
    // 17. CHUẨN HÓA CHUỖI
    // ========================================

    private String normalizeValue(String value) {

        if (value == null) {
            return null;
        }

        String result = value.trim();

        return result.isEmpty()
                ? null
                : result;
    }

    // ========================================
    // 18. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerMyBookingsActivity.this,
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
    // 19. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        bookingExecutor.shutdown();

        super.onDestroy();
    }
}

