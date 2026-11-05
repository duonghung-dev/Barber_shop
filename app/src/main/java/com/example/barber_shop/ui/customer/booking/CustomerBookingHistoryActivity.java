
package com.example.barber_shop.ui.customer.booking;

import android.content.Context;
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

public class CustomerBookingHistoryActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO CÁC BỘ LỌC
    // ========================================

    private static final int FILTER_ALL = 0;
    private static final int FILTER_COMPLETED = 1;
    private static final int FILTER_CANCELLED = 2;
    private static final int FILTER_OVERDUE = 3;

    // Lưu bộ lọc khi Activity được tạo lại
    private static final String STATE_CURRENT_FILTER =
            "state_current_history_filter";

    // ========================================
    // 2. KHAI BÁO VIEW
    // ========================================

    // Thanh tiêu đề
    private MaterialToolbar toolbarCustomerBookingHistory;

    // Thông tin thống kê
    private TextView txtCustomerHistoryTotal;
    private TextView txtCustomerHistoryCount;

    // Bộ lọc lịch sử
    private ChipGroup chipGroupCustomerHistoryFilters;

    // Danh sách Booking
    private RecyclerView recyclerCustomerBookingHistory;

    // Trạng thái trống
    private LinearLayout layoutEmptyCustomerBookingHistory;

    private TextView txtEmptyCustomerHistoryTitle;
    private TextView txtEmptyCustomerHistorySubtitle;

    // Nút khám phá dịch vụ
    private MaterialButton btnCustomerHistoryExploreServices;

    // Loading
    private ProgressBar progressCustomerBookingHistory;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Adapter hiển thị Booking
    private CustomerBookingAdapter bookingAdapter;

    // Danh sách chỉ chứa Booking thuộc lịch sử
    private final List<Booking> allHistoryBookings =
            new ArrayList<>();

    // Bộ lọc đang được lựa chọn
    private int currentFilter = FILTER_ALL;

    // Trạng thái tải dữ liệu
    private boolean isLoading = false;

    // Executor xử lý Repository
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

        // ========================================
        // 4.1. KHỞI TẠO SESSION
        // ========================================

        sessionManager = new SessionManager(this);

        // Chỉ cho Customer đã đăng nhập truy cập
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
                R.layout.activity_customer_booking_history
        );

        // ========================================
        // 4.4. KHÔI PHỤC BỘ LỌC
        // ========================================

        if (savedInstanceState != null) {

            int savedFilter = savedInstanceState.getInt(
                    STATE_CURRENT_FILTER,
                    FILTER_ALL
            );

            if (savedFilter >= FILTER_ALL
                    && savedFilter <= FILTER_OVERDUE) {

                currentFilter = savedFilter;
            }
        }

        // ========================================
        // 4.5. THIẾT LẬP GIAO DIỆN
        // ========================================

        setupWindowInsets();
        initViews();
        setupRecyclerView();
        setupListeners();

        // ========================================
        // 4.6. CHỌN BỘ LỌC BAN ĐẦU
        // ========================================

        // Đánh dấu đang chờ dữ liệu để tránh
        // hiển thị Empty trước khi tải Booking.
        isLoading = true;

        chipGroupCustomerHistoryFilters.check(
                getChipIdForCurrentFilter()
        );

        showLoading();
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

        // ========================================
        // 5.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 5.2. ĐỌC LẠI LỊCH SỬ
        // ========================================

        // Khi Customer quay về từ Booking Detail,
        // dữ liệu sẽ được tải lại.
        //
        // Điều này giúp màn hình phản ánh các thay đổi
        // mới nhất, ví dụ Booking vừa được hủy
        // hoặc thay đổi ngày giờ.

        loadBookingHistory();
    }

    // ========================================
    // 6. LƯU BỘ LỌC KHI TẠO LẠI ACTIVITY
    // ========================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(
                STATE_CURRENT_FILTER,
                currentFilter
        );
    }

    // ========================================
    // 7. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBookingHistoryRoot
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

        toolbarCustomerBookingHistory = findViewById(
                R.id.toolbarCustomerBookingHistory
        );

        // ========================================
        // 8.2. THỐNG KÊ LỊCH SỬ
        // ========================================

        txtCustomerHistoryTotal = findViewById(
                R.id.txtCustomerHistoryTotal
        );

        txtCustomerHistoryCount = findViewById(
                R.id.txtCustomerHistoryCount
        );

        // ========================================
        // 8.3. BỘ LỌC
        // ========================================

        chipGroupCustomerHistoryFilters = findViewById(
                R.id.chipGroupCustomerHistoryFilters
        );

        // ========================================
        // 8.4. RECYCLERVIEW
        // ========================================

        recyclerCustomerBookingHistory = findViewById(
                R.id.recyclerCustomerBookingHistory
        );

        // ========================================
        // 8.5. EMPTY STATE
        // ========================================

        layoutEmptyCustomerBookingHistory = findViewById(
                R.id.layoutEmptyCustomerBookingHistory
        );

        txtEmptyCustomerHistoryTitle = findViewById(
                R.id.txtEmptyCustomerHistoryTitle
        );

        txtEmptyCustomerHistorySubtitle = findViewById(
                R.id.txtEmptyCustomerHistorySubtitle
        );

        // ========================================
        // 8.6. NÚT KHÁM PHÁ DỊCH VỤ
        // ========================================

        btnCustomerHistoryExploreServices = findViewById(
                R.id.btnCustomerHistoryExploreServices
        );

        // ========================================
        // 8.7. LOADING
        // ========================================

        progressCustomerBookingHistory = findViewById(
                R.id.progressCustomerBookingHistory
        );
    }

    // ========================================
    // 9. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // ========================================
        // 9.1. TÁI SỬ DỤNG CUSTOMER BOOKING ADAPTER
        // ========================================

        // Adapter đã được xây dựng ở STT 14.
        // Mỗi khi Customer chọn một Booking,
        // showBookingDetails() sẽ được gọi.

        bookingAdapter = new CustomerBookingAdapter(
                this::showBookingDetails
        );

        // ========================================
        // 9.2. THIẾT LẬP LAYOUT MANAGER
        // ========================================

        recyclerCustomerBookingHistory.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // ========================================
        // 9.3. GẮN ADAPTER
        // ========================================

        recyclerCustomerBookingHistory.setAdapter(
                bookingAdapter
        );
    }

    // ========================================
    // 10. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 10.1. NÚT QUAY LẠI
        // ========================================

        toolbarCustomerBookingHistory
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 10.2. THAY ĐỔI BỘ LỌC
        // ========================================

        chipGroupCustomerHistoryFilters
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (checkedIds.isEmpty()) {
                                return;
                            }

                            int checkedId = checkedIds.get(0);

                            if (checkedId ==
                                    R.id.chipCustomerHistoryCompleted) {

                                currentFilter =
                                        FILTER_COMPLETED;

                            } else if (checkedId ==
                                    R.id.chipCustomerHistoryCancelled) {

                                currentFilter =
                                        FILTER_CANCELLED;

                            } else if (checkedId ==
                                    R.id.chipCustomerHistoryOverdue) {

                                currentFilter =
                                        FILTER_OVERDUE;

                            } else {

                                currentFilter = FILTER_ALL;
                            }

                            // Lọc dữ liệu đang có
                            if (!isLoading) {
                                applyHistoryFilter();
                            }
                        }
                );

        // ========================================
        // 10.3. KHÁM PHÁ DỊCH VỤ
        // ========================================

        btnCustomerHistoryExploreServices
                .setOnClickListener(
                        v -> openServices()
                );
    }

    // ========================================
    // 11. LẤY CHIP ID THEO BỘ LỌC
    // ========================================

    private int getChipIdForCurrentFilter() {

        switch (currentFilter) {

            case FILTER_COMPLETED:
                return R.id.chipCustomerHistoryCompleted;

            case FILTER_CANCELLED:
                return R.id.chipCustomerHistoryCancelled;

            case FILTER_OVERDUE:
                return R.id.chipCustomerHistoryOverdue;

            case FILTER_ALL:
            default:
                return R.id.chipCustomerHistoryAll;
        }
    }

    // ========================================
    // 12. ĐỌC LỊCH SỬ TỪ REPOSITORY
    // ========================================

    private void loadBookingHistory() {

        // ========================================
        // 12.1. KIỂM TRA CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 12.2. LẤY CUSTOMER ID
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
        // 12.3. HIỂN THỊ LOADING
        // ========================================

        isLoading = true;
        showLoading();

        // Phân biệt các lần tải
        final int requestVersion =
                ++loadRequestVersion;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 12.4. ĐỌC DỮ LIỆU TRÊN LUỒNG RIÊNG
        // ========================================

        bookingExecutor.execute(() -> {

            List<Booking> loadedBookings = null;
            String errorMessage = null;

            try {

                loadedBookings =
                        BookingRepository.getBookingsForCustomer(
                                appContext,
                                customerId
                        );

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể tải lịch sử đặt lịch.";
            }

            final List<Booking> resultBookings =
                    loadedBookings;

            final String resultError =
                    errorMessage;

            // ========================================
            // 12.5. TRẢ KẾT QUẢ VỀ MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                // Activity không còn tồn tại
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả từ yêu cầu cũ
                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // ========================================
                // 12.6. KIỂM TRA LẠI SESSION
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
                // 12.7. XỬ LÝ LỖI ĐỌC
                // ========================================

                if (resultError != null) {

                    showLoadingError(resultError);
                    return;
                }

                // ========================================
                // 12.8. CHUẨN BỊ DANH SÁCH LỊCH SỬ
                // ========================================

                allHistoryBookings.clear();

                if (resultBookings != null) {

                    for (Booking booking : resultBookings) {

                        if (booking == null) {
                            continue;
                        }

                        // Chỉ giữ Booking thuộc lịch sử
                        if (isHistoryBooking(booking)) {

                            allHistoryBookings.add(booking);
                        }
                    }
                }

                // ========================================
                // 12.9. HOÀN TẤT LOADING
                // ========================================

                isLoading = false;

                progressCustomerBookingHistory.setVisibility(
                        View.GONE
                );

                // Cập nhật thống kê và danh sách
                applyHistoryFilter();
            });
        });
    }

    // ========================================
    // 13. KIỂM TRA BOOKING THUỘC LỊCH SỬ
    // ========================================

    private boolean isHistoryBooking(Booking booking) {

        if (booking == null) {
            return false;
        }

        String status = booking.getStatus();

        // ========================================
        // 13.1. BOOKING HOÀN THÀNH
        // ========================================

        if (Booking.STATUS_COMPLETED.equals(status)) {
            return true;
        }

        // ========================================
        // 13.2. BOOKING ĐÃ HỦY
        // ========================================

        if (Booking.STATUS_CANCELLED.equals(status)) {
            return true;
        }

        // ========================================
        // 13.3. BOOKING QUÁ GIỜ
        // ========================================

        // Booking PENDING hoặc CONFIRMED
        // đã đến/qua giờ bắt đầu sẽ được
        // đưa vào lịch sử, nhưng KHÔNG tự
        // đổi trạng thái thành COMPLETED.

        return isOverdueBooking(booking);
    }

    // ========================================
    // 14. KIỂM TRA BOOKING QUÁ GIỜ
    // ========================================

    private boolean isOverdueBooking(Booking booking) {

        if (booking == null) {
            return false;
        }

        // Chỉ xét Booking còn PENDING/CONFIRMED
        if (!booking.isActive()) {
            return false;
        }

        // Chuyển ngày giờ sang Date để so sánh
        Date bookingDateTime =
                parseBookingDateTime(booking);

        // Không tự phân loại dữ liệu ngày giờ
        // bị lỗi thành Booking quá giờ.
        if (bookingDateTime == null) {
            return false;
        }

        // Thời gian đã đến hoặc qua giờ bắt đầu
        return bookingDateTime.getTime()
                <= System.currentTimeMillis();
    }

    // ========================================
    // 15. CHUYỂN NGÀY GIỜ BOOKING SANG DATE
    // ========================================

    private Date parseBookingDateTime(Booking booking) {

        if (booking == null) {
            return null;
        }

        String date = normalizeValue(
                booking.getBookingDate()
        );

        String time = normalizeValue(
                booking.getBookingTime()
        );

        // ========================================
        // 15.1. KIỂM TRA ĐỊNH DẠNG
        // ========================================

        if (date == null || time == null) {
            return null;
        }

        if (!date.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {
            return null;
        }

        if (!time.matches(
                "([01][0-9]|2[0-3]):[0-5][0-9]"
        )) {
            return null;
        }

        // ========================================
        // 15.2. PARSE NGÀY GIỜ
        // ========================================

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            return parser.parse(
                    date + " " + time
            );

        } catch (ParseException e) {

            return null;
        }
    }

    // ========================================
    // 16. LỌC DANH SÁCH LỊCH SỬ
    // ========================================

    private void applyHistoryFilter() {

        if (isLoading) {
            return;
        }

        List<Booking> filteredBookings =
                new ArrayList<>();

        // ========================================
        // 16.1. DUYỆT TẤT CẢ BOOKING LỊCH SỬ
        // ========================================

        for (Booking booking : allHistoryBookings) {

            if (booking == null) {
                continue;
            }

            boolean includeBooking = false;

            // ========================================
            // 16.2. PHÂN LOẠI THEO BỘ LỌC
            // ========================================

            switch (currentFilter) {

                // Lịch đã hoàn thành
                case FILTER_COMPLETED:

                    includeBooking =
                            Booking.STATUS_COMPLETED.equals(
                                    booking.getStatus()
                            );

                    break;

                // Lịch đã hủy
                case FILTER_CANCELLED:

                    includeBooking =
                            Booking.STATUS_CANCELLED.equals(
                                    booking.getStatus()
                            );

                    break;

                // Lịch quá giờ
                case FILTER_OVERDUE:

                    includeBooking =
                            isOverdueBooking(booking);

                    break;

                // Tất cả lịch sử
                case FILTER_ALL:
                default:

                    includeBooking = true;
                    break;
            }

            // ========================================
            // 16.3. THÊM BOOKING PHÙ HỢP
            // ========================================

            if (includeBooking) {

                filteredBookings.add(booking);
            }
        }

        // ========================================
        // 16.4. CẬP NHẬT ADAPTER
        // ========================================

        // submitList() là phương thức đang được
        // sử dụng ở CustomerMyBookingsActivity.

        bookingAdapter.submitList(filteredBookings);

        // ========================================
        // 16.5. HIỂN THỊ TỔNG SỐ LỊCH SỬ
        // ========================================

        txtCustomerHistoryTotal.setText(
                String.format(
                        Locale.getDefault(),
                        "%d lịch hẹn",
                        allHistoryBookings.size()
                )
        );

        // ========================================
        // 16.6. HIỂN THỊ SỐ KẾT QUẢ LỌC
        // ========================================

        txtCustomerHistoryCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d kết quả",
                        filteredBookings.size()
                )
        );

        // ========================================
        // 16.7. HIỂN THỊ DANH SÁCH HOẶC EMPTY
        // ========================================

        if (filteredBookings.isEmpty()) {

            recyclerCustomerBookingHistory.setVisibility(
                    View.GONE
            );

            showEmptyState();

        } else {

            layoutEmptyCustomerBookingHistory.setVisibility(
                    View.GONE
            );

            recyclerCustomerBookingHistory.setVisibility(
                    View.VISIBLE
            );
        }
    }

    // ========================================
    // 17. HIỂN THỊ TRẠNG THÁI ĐANG TẢI
    // ========================================

    private void showLoading() {

        progressCustomerBookingHistory.setVisibility(
                View.VISIBLE
        );

        recyclerCustomerBookingHistory.setVisibility(
                View.GONE
        );

        layoutEmptyCustomerBookingHistory.setVisibility(
                View.GONE
        );

        txtCustomerHistoryCount.setText(
                "Đang tải lịch sử..."
        );
    }

    // ========================================
    // 18. HIỂN THỊ TRẠNG THÁI TRỐNG
    // ========================================

    private void showEmptyState() {

        layoutEmptyCustomerBookingHistory.setVisibility(
                View.VISIBLE
        );

        // ========================================
        // 18.1. KHÔNG CÓ LỊCH SỬ NÀO
        // ========================================

        if (allHistoryBookings.isEmpty()) {

            txtEmptyCustomerHistoryTitle.setText(
                    "Chưa có lịch sử đặt lịch"
            );

            txtEmptyCustomerHistorySubtitle.setText(
                    "Các lịch hẹn đã hoàn thành, "
                            + "đã hủy hoặc quá giờ "
                            + "sẽ xuất hiện tại đây."
            );

            // Hiển thị nút khám phá dịch vụ
            btnCustomerHistoryExploreServices.setVisibility(
                    View.VISIBLE
            );

            return;
        }

        // ========================================
        // 18.2. CÓ LỊCH SỬ NHƯNG KHÔNG KHỚP BỘ LỌC
        // ========================================

        btnCustomerHistoryExploreServices.setVisibility(
                View.GONE
        );

        switch (currentFilter) {

            case FILTER_COMPLETED:

                txtEmptyCustomerHistoryTitle.setText(
                        "Chưa có lịch hẹn hoàn thành"
                );

                txtEmptyCustomerHistorySubtitle.setText(
                        "Bạn chưa có lịch hẹn nào "
                                + "ở trạng thái hoàn thành."
                );

                break;

            case FILTER_CANCELLED:

                txtEmptyCustomerHistoryTitle.setText(
                        "Chưa có lịch hẹn đã hủy"
                );

                txtEmptyCustomerHistorySubtitle.setText(
                        "Bạn chưa có lịch hẹn nào "
                                + "ở trạng thái đã hủy."
                );

                break;

            case FILTER_OVERDUE:

                txtEmptyCustomerHistoryTitle.setText(
                        "Không có lịch hẹn quá giờ"
                );

                txtEmptyCustomerHistorySubtitle.setText(
                        "Không có lịch PENDING hoặc "
                                + "CONFIRMED nào đã qua "
                                + "giờ bắt đầu."
                );

                break;

            case FILTER_ALL:
            default:

                txtEmptyCustomerHistoryTitle.setText(
                        "Không tìm thấy lịch sử"
                );

                txtEmptyCustomerHistorySubtitle.setText(
                        "Hiện chưa có lịch hẹn phù hợp."
                );

                break;
        }
    }

    // ========================================
    // 19. HIỂN THỊ LỖI TẢI LỊCH SỬ
    // ========================================

    private void showLoadingError(String message) {

        // ========================================
        // 19.1. DỪNG LOADING
        // ========================================

        isLoading = false;

        progressCustomerBookingHistory.setVisibility(
                View.GONE
        );

        // ========================================
        // 19.2. XÓA DANH SÁCH CŨ
        // ========================================

        allHistoryBookings.clear();

        bookingAdapter.submitList(
                new ArrayList<>()
        );

        recyclerCustomerBookingHistory.setVisibility(
                View.GONE
        );

        // ========================================
        // 19.3. HIỂN THỊ THÔNG BÁO LỖI
        // ========================================

        layoutEmptyCustomerBookingHistory.setVisibility(
                View.VISIBLE
        );

        txtCustomerHistoryTotal.setText(
                "Không thể tải dữ liệu"
        );

        txtCustomerHistoryCount.setText(
                "Không thể tải lịch sử"
        );

        txtEmptyCustomerHistoryTitle.setText(
                "Đã xảy ra lỗi"
        );

        txtEmptyCustomerHistorySubtitle.setText(
                message
        );

        // Không hiển thị nút đặt lịch khi có lỗi
        btnCustomerHistoryExploreServices.setVisibility(
                View.GONE
        );

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    // ========================================
    // 20. MỞ CHI TIẾT LỊCH HẸN
    // ========================================

    private void showBookingDetails(Booking booking) {

        // ========================================
        // 20.1. KIỂM TRA BOOKING
        // ========================================

        if (booking == null) {
            return;
        }

        // ========================================
        // 20.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 20.3. KIỂM TRA QUYỀN SỞ HỮU
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

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
        // 20.4. KIỂM TRA MÃ BOOKING
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
        // 20.5. MỞ BOOKING DETAIL
        // ========================================

        // Chỉ truyền mã Booking.
        // Màn hình Detail tự đọc thông tin mới nhất
        // và kiểm tra lại quyền sở hữu.

        Intent intent = new Intent(
                CustomerBookingHistoryActivity.this,
                CustomerBookingDetailActivity.class
        );

        intent.putExtra(
                CustomerBookingDetailActivity.EXTRA_BOOKING_CODE,
                bookingCode
        );

        startActivity(intent);
    }

    // ========================================
    // 21. MỞ MÀN HÌNH DỊCH VỤ
    // ========================================

    private void openServices() {

        // ========================================
        // 21.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 21.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBookingHistoryActivity.this,
                CustomerServiceListActivity.class
        );

        // Sử dụng cùng quy trình Đặt lịch mới
        // với CustomerMyBookingsActivity.
        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BOOKING_MODE,
                true
        );

        startActivity(intent);
    }

    // ========================================
    // 22. CHUẨN HÓA CHUỖI
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
    // 23. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBookingHistoryActivity.this,
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
    // 24. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        bookingExecutor.shutdown();

        super.onDestroy();
    }
}
