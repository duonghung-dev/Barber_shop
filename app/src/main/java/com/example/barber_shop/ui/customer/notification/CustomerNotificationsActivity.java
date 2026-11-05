
package com.example.barber_shop.ui.customer.notification;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Notification;
import com.example.barber_shop.data.repository.NotificationRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.booking.CustomerBookingDetailActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerNotificationsActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO HẰNG SỐ BỘ LỌC
    // ========================================

    private static final int FILTER_ALL = 0;

    private static final int FILTER_UNREAD = 1;

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    // Toolbar
    private MaterialToolbar toolbarCustomerNotifications;

    // Số lượng thông báo chưa đọc
    private TextView txtCustomerNotificationsUnreadCount;

    // Nút đánh dấu tất cả đã đọc
    private MaterialButton btnCustomerNotificationsMarkAllRead;

    // Hai nút bộ lọc
    private MaterialButton btnCustomerNotificationsFilterAll;

    private MaterialButton btnCustomerNotificationsFilterUnread;

    // Danh sách thông báo
    private RecyclerView recyclerCustomerNotifications;

    // ========================================
    // 2.1. TRẠNG THÁI DANH SÁCH TRỐNG
    // ========================================

    private LinearLayout layoutCustomerNotificationsEmpty;

    private TextView txtCustomerNotificationsEmptyTitle;

    private TextView txtCustomerNotificationsEmptyMessage;

    // ========================================
    // 2.2. TRẠNG THÁI LOADING
    // ========================================

    private ProgressBar progressCustomerNotifications;

    // ========================================
    // 2.3. TRẠNG THÁI LỖI
    // ========================================

    private LinearLayout layoutCustomerNotificationsError;

    private TextView txtCustomerNotificationsError;

    private MaterialButton btnCustomerNotificationsRetry;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Phiên đăng nhập Customer
    private SessionManager sessionManager;

    // Adapter danh sách
    private CustomerNotificationAdapter notificationAdapter;

    // Danh sách thông báo đã tải
    private final List<Notification> allNotifications =
            new ArrayList<>();

    // Bộ lọc hiện tại
    private int currentFilter = FILTER_ALL;

    // Đã tải dữ liệu thành công?
    private boolean isDataReady = false;

    // Đang tải danh sách?
    private boolean isLoading = false;

    // Đang cập nhật trạng thái đã đọc?
    private boolean isUpdatingReadStatus = false;

    // Phân biệt các lần tải
    private int loadRequestVersion = 0;

    // ========================================
    // 3.1. EXECUTOR
    // ========================================

    // Các thao tác SharedPreferences được
    // thực hiện bên ngoài Main Thread.

    private final ExecutorService notificationExecutor =
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

        // Chỉ Customer được truy cập
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 4.2. KÍCH HOẠT EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 4.3. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_customer_notifications
        );

        // ========================================
        // 4.4. THIẾT LẬP THÀNH PHẦN
        // ========================================

        setupWindowInsets();

        initViews();

        setupRecyclerView();

        setupListeners();

        updateActionStates();

        // Dữ liệu được tải trong onResume().
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

        // ========================================
        // 5.1. KIỂM TRA CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 5.2. TẢI LẠI DANH SÁCH
        // ========================================

        // Khi quay lại từ màn hình Chi tiết
        // Booking, danh sách sẽ được đọc lại.

        if (!isUpdatingReadStatus) {

            loadNotifications();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerNotificationsRoot
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

        toolbarCustomerNotifications = findViewById(
                R.id.toolbarCustomerNotifications
        );

        // ========================================
        // 7.2. SỐ THÔNG BÁO CHƯA ĐỌC
        // ========================================

        txtCustomerNotificationsUnreadCount = findViewById(
                R.id.txtCustomerNotificationsUnreadCount
        );

        // ========================================
        // 7.3. NÚT ĐỌC TẤT CẢ
        // ========================================

        btnCustomerNotificationsMarkAllRead = findViewById(
                R.id.btnCustomerNotificationsMarkAllRead
        );

        // ========================================
        // 7.4. BỘ LỌC TẤT CẢ
        // ========================================

        btnCustomerNotificationsFilterAll = findViewById(
                R.id.btnCustomerNotificationsFilterAll
        );

        // ========================================
        // 7.5. BỘ LỌC CHƯA ĐỌC
        // ========================================

        btnCustomerNotificationsFilterUnread = findViewById(
                R.id.btnCustomerNotificationsFilterUnread
        );

        // ========================================
        // 7.6. RECYCLERVIEW
        // ========================================

        recyclerCustomerNotifications = findViewById(
                R.id.recyclerCustomerNotifications
        );

        // ========================================
        // 7.7. TRẠNG THÁI TRỐNG
        // ========================================

        layoutCustomerNotificationsEmpty = findViewById(
                R.id.layoutCustomerNotificationsEmpty
        );

        txtCustomerNotificationsEmptyTitle = findViewById(
                R.id.txtCustomerNotificationsEmptyTitle
        );

        txtCustomerNotificationsEmptyMessage = findViewById(
                R.id.txtCustomerNotificationsEmptyMessage
        );

        // ========================================
        // 7.8. PROGRESSBAR
        // ========================================

        progressCustomerNotifications = findViewById(
                R.id.progressCustomerNotifications
        );

        // ========================================
        // 7.9. TRẠNG THÁI LỖI
        // ========================================

        layoutCustomerNotificationsError = findViewById(
                R.id.layoutCustomerNotificationsError
        );

        txtCustomerNotificationsError = findViewById(
                R.id.txtCustomerNotificationsError
        );

        // ========================================
        // 7.10. NÚT THỬ LẠI
        // ========================================

        btnCustomerNotificationsRetry = findViewById(
                R.id.btnCustomerNotificationsRetry
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // ========================================
        // 8.1. LAYOUT MANAGER
        // ========================================

        recyclerCustomerNotifications.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // ========================================
        // 8.2. KHỞI TẠO ADAPTER
        // ========================================

        notificationAdapter =
                new CustomerNotificationAdapter(
                        notification -> {

                            // Chuyển sự kiện nhấn
                            // thông báo về Activity.

                            handleNotificationClick(
                                    notification
                            );
                        }
                );

        // ========================================
        // 8.3. GẮN ADAPTER
        // ========================================

        recyclerCustomerNotifications.setAdapter(
                notificationAdapter
        );
    }

    // ========================================
    // 9. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 9.1. QUAY LẠI
        // ========================================

        toolbarCustomerNotifications
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 9.2. LỌC TẤT CẢ
        // ========================================

        btnCustomerNotificationsFilterAll
                .setOnClickListener(v -> {

                    if (!isDataReady
                            || isLoading
                            || isUpdatingReadStatus) {

                        return;
                    }

                    currentFilter = FILTER_ALL;

                    displayNotifications();
                });

        // ========================================
        // 9.3. LỌC CHƯA ĐỌC
        // ========================================

        btnCustomerNotificationsFilterUnread
                .setOnClickListener(v -> {

                    if (!isDataReady
                            || isLoading
                            || isUpdatingReadStatus) {

                        return;
                    }

                    currentFilter = FILTER_UNREAD;

                    displayNotifications();
                });

        // ========================================
        // 9.4. ĐỌC TẤT CẢ
        // ========================================

        btnCustomerNotificationsMarkAllRead
                .setOnClickListener(
                        v -> markAllNotificationsAsRead()
                );

        // ========================================
        // 9.5. THỬ LẠI KHI LỖI
        // ========================================

        btnCustomerNotificationsRetry
                .setOnClickListener(
                        v -> loadNotifications()
                );
    }

    // ========================================
    // 10. TẢI DANH SÁCH THÔNG BÁO
    // ========================================

    private void loadNotifications() {

        // ========================================
        // 10.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 10.2. TRÁNH TẢI KHI ĐANG GHI
        // ========================================

        if (isUpdatingReadStatus) {
            return;
        }

        // ========================================
        // 10.3. LẤY CUSTOMER ID
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
        // 10.4. HIỂN THỊ LOADING
        // ========================================

        showLoading();

        final int requestVersion =
                ++loadRequestVersion;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 10.5. ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        notificationExecutor.execute(() -> {

            List<Notification> loadedNotifications = null;

            String errorMessage = null;

            try {

                // ========================================
                // 10.6. LẤY THEO CUSTOMER
                // ========================================

                loadedNotifications =
                        NotificationRepository
                                .getNotificationsForCustomer(
                                        appContext,
                                        customerId
                                );

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc danh sách thông báo.";
            }

            final List<Notification> resultNotifications =
                    loadedNotifications;

            final String resultError =
                    errorMessage;

            // ========================================
            // 10.7. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả của lần tải cũ
                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // ========================================
                // 10.8. KIỂM TRA LẠI CUSTOMER
                // ========================================

                if (!isSameCustomer(customerId)) {

                    openLogin();
                    return;
                }

                // ========================================
                // 10.9. XỬ LÝ LỖI
                // ========================================

                if (resultError != null
                        || resultNotifications == null) {

                    showLoadError(
                            resultError != null
                                    ? resultError
                                    : "Không thể tải thông báo."
                    );

                    return;
                }

                // ========================================
                // 10.10. CẬP NHẬT DANH SÁCH
                // ========================================

                allNotifications.clear();

                allNotifications.addAll(
                        resultNotifications
                );

                isLoading = false;

                isDataReady = true;

                // ========================================
                // 10.11. ẨN LOADING
                // ========================================

                progressCustomerNotifications.setVisibility(
                        View.GONE
                );

                layoutCustomerNotificationsError.setVisibility(
                        View.GONE
                );

                // ========================================
                // 10.12. HIỂN THỊ DỮ LIỆU
                // ========================================

                displayNotifications();
            });
        });
    }

    // ========================================
    // 11. HIỂN THỊ LOADING
    // ========================================

    private void showLoading() {

        isLoading = true;

        isDataReady = false;

        // Ẩn danh sách
        recyclerCustomerNotifications.setVisibility(
                View.GONE
        );

        // Ẩn trạng thái trống
        layoutCustomerNotificationsEmpty.setVisibility(
                View.GONE
        );

        // Ẩn lỗi
        layoutCustomerNotificationsError.setVisibility(
                View.GONE
        );

        // Hiển thị Loading
        progressCustomerNotifications.setVisibility(
                View.VISIBLE
        );

        updateActionStates();
    }

    // ========================================
    // 12. HIỂN THỊ DANH SÁCH THEO BỘ LỌC
    // ========================================

    private void displayNotifications() {

        // ========================================
        // 12.1. KIỂM TRA TRẠNG THÁI
        // ========================================

        if (!isDataReady) {
            return;
        }

        // ========================================
        // 12.2. TẠO DANH SÁCH HIỂN THỊ
        // ========================================

        List<Notification> displayedNotifications =
                new ArrayList<>();

        // ========================================
        // 12.3. LỌC THÔNG BÁO
        // ========================================

        for (Notification notification
                : allNotifications) {

            // Bộ lọc Tất cả
            if (currentFilter == FILTER_ALL) {

                displayedNotifications.add(
                        notification
                );

                continue;
            }

            // Bộ lọc Chưa đọc
            if (currentFilter == FILTER_UNREAD
                    && !notification.isRead()) {

                displayedNotifications.add(
                        notification
                );
            }
        }

        // ========================================
        // 12.4. CẬP NHẬT ADAPTER
        // ========================================

        notificationAdapter.submitList(
                displayedNotifications
        );

        // ========================================
        // 12.5. HIỂN THỊ DANH SÁCH / TRẠNG THÁI TRỐNG
        // ========================================

        boolean isEmpty =
                displayedNotifications.isEmpty();

        recyclerCustomerNotifications.setVisibility(
                isEmpty ? View.GONE : View.VISIBLE
        );

        layoutCustomerNotificationsEmpty.setVisibility(
                isEmpty ? View.VISIBLE : View.GONE
        );

        layoutCustomerNotificationsError.setVisibility(
                View.GONE
        );

        progressCustomerNotifications.setVisibility(
                View.GONE
        );

        // ========================================
        // 12.6. NỘI DUNG KHI KHÔNG CÓ DỮ LIỆU
        // ========================================

        if (isEmpty) {

            updateEmptyState();
        }

        // ========================================
        // 12.7. CẬP NHẬT SỐ CHƯA ĐỌC VÀ NÚT
        // ========================================

        updateUnreadCount();

        updateActionStates();
    }

    // ========================================
    // 13. HIỂN THỊ TRẠNG THÁI TRỐNG
    // ========================================

    private void updateEmptyState() {

        // ========================================
        // 13.1. KHÔNG CÓ THÔNG BÁO
        // ========================================

        if (currentFilter == FILTER_ALL) {

            txtCustomerNotificationsEmptyTitle.setText(
                    "Chưa có thông báo"
            );

            txtCustomerNotificationsEmptyMessage.setText(
                    "Các thông báo mới về lịch hẹn "
                            + "của bạn sẽ xuất hiện tại đây."
            );

            return;
        }

        // ========================================
        // 13.2. KHÔNG CÓ THÔNG BÁO CHƯA ĐỌC
        // ========================================

        txtCustomerNotificationsEmptyTitle.setText(
                "Bạn đã đọc tất cả thông báo"
        );

        txtCustomerNotificationsEmptyMessage.setText(
                "Hiện tại không có thông báo chưa đọc. "
                        + "Bạn có thể chọn Tất cả để xem lại."
        );
    }

    // ========================================
    // 14. ĐẾM THÔNG BÁO CHƯA ĐỌC
    // ========================================

    private int countUnreadNotifications() {

        int unreadCount = 0;

        for (Notification notification
                : allNotifications) {

            if (!notification.isRead()) {

                unreadCount++;
            }
        }

        return unreadCount;
    }

    // ========================================
    // 15. CẬP NHẬT SỐ THÔNG BÁO CHƯA ĐỌC
    // ========================================

    private void updateUnreadCount() {

        int unreadCount =
                countUnreadNotifications();

        txtCustomerNotificationsUnreadCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d thông báo chưa đọc",
                        unreadCount
                )
        );
    }

    // ========================================
    // 16. CẬP NHẬT TRẠNG THÁI CÁC NÚT
    // ========================================

    private void updateActionStates() {

        // ========================================
        // 16.1. KIỂM TRA CÓ THỂ TƯƠNG TÁC
        // ========================================

        boolean canInteract =
                isDataReady
                        && !isLoading
                        && !isUpdatingReadStatus;

        // ========================================
        // 16.2. BẬT / TẮT BỘ LỌC
        // ========================================

        btnCustomerNotificationsFilterAll.setEnabled(
                canInteract
        );

        btnCustomerNotificationsFilterUnread.setEnabled(
                canInteract
        );

        // ========================================
        // 16.3. HIỂN THỊ TRẠNG THÁI BỘ LỌC
        // ========================================

        updateFilterButtonAppearance(
                btnCustomerNotificationsFilterAll,
                currentFilter == FILTER_ALL
        );

        updateFilterButtonAppearance(
                btnCustomerNotificationsFilterUnread,
                currentFilter == FILTER_UNREAD
        );

        // ========================================
        // 16.4. NÚT ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC
        // ========================================

        boolean hasUnread =
                countUnreadNotifications() > 0;

        btnCustomerNotificationsMarkAllRead.setEnabled(
                canInteract && hasUnread
        );

        btnCustomerNotificationsMarkAllRead.setText(
                isUpdatingReadStatus
                        ? "Đang xử lý..."
                        : "Đọc tất cả"
        );

        // ========================================
        // 16.5. NÚT THỬ LẠI
        // ========================================

        btnCustomerNotificationsRetry.setEnabled(
                !isLoading && !isUpdatingReadStatus
        );
    }

    // ========================================
    // 17. CẬP NHẬT GIAO DIỆN NÚT LỌC
    // ========================================

    private void updateFilterButtonAppearance(
            MaterialButton button,
            boolean selected
    ) {

        // ========================================
        // 17.1. LẤY MÀU TỪ DESIGN SYSTEM
        // ========================================

        int primaryColor =
                ContextCompat.getColor(
                        this,
                        R.color.primary
                );

        int surfaceColor =
                ContextCompat.getColor(
                        this,
                        R.color.surface
                );

        int borderColor =
                ContextCompat.getColor(
                        this,
                        R.color.border_default
                );

        int whiteColor =
                ContextCompat.getColor(
                        this,
                        android.R.color.white
                );

        // ========================================
        // 17.2. MÀU NỀN NÚT
        // ========================================

        int backgroundColor =
                selected
                        ? primaryColor
                        : surfaceColor;

        button.setBackgroundTintList(
                ColorStateList.valueOf(
                        backgroundColor
                )
        );

        // ========================================
        // 17.3. MÀU CHỮ
        // ========================================

        button.setTextColor(
                selected
                        ? whiteColor
                        : primaryColor
        );

        // ========================================
        // 17.4. MÀU VIỀN
        // ========================================

        button.setStrokeColor(
                ColorStateList.valueOf(
                        borderColor
                )
        );

        button.setStrokeWidth(
                selected
                        ? 0
                        : dpToPx(1)
        );
    }

    // ========================================
    // 18. CHUYỂN DP SANG PIXEL
    // ========================================

    private int dpToPx(int dp) {

        float density = getResources()
                .getDisplayMetrics()
                .density;

        return Math.round(
                dp * density
        );
    }

    // ========================================
    // 19. XỬ LÝ KHI NHẤN MỘT THÔNG BÁO
    // ========================================

    private void handleNotificationClick(
            Notification notification
    ) {

        // ========================================
        // 19.1. KIỂM TRA ĐỐI TƯỢNG
        // ========================================

        if (notification == null) {
            return;
        }

        // ========================================
        // 19.2. TRÁNH NHẤN KHI ĐANG XỬ LÝ
        // ========================================

        if (!isDataReady
                || isLoading
                || isUpdatingReadStatus) {

            return;
        }

        // ========================================
        // 19.3. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 19.4. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {
            return;
        }

        // ========================================
        // 19.5. KIỂM TRA QUYỀN CUSTOMER
        // ========================================

        if (!customerId.equals(
                notification.getCustomerId()
        )) {

            Toast.makeText(
                    this,
                    "Bạn không có quyền xem thông báo này.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 19.6. LẤY BOOKING CODE
        // ========================================

        final String bookingCode = normalizeValue(
                notification.getBookingCode()
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Thông báo không có lịch hẹn liên quan.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 19.7. NẾU ĐÃ ĐỌC THÌ MỞ CHI TIẾT
        // ========================================

        if (notification.isRead()) {

            openBookingDetail(
                    bookingCode
            );

            return;
        }

        // ========================================
        // 19.8. LẤY NOTIFICATION ID
        // ========================================

        final String notificationId =
                notification.getNotificationId();

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 19.9. KHÓA THAO TÁC TRÙNG
        // ========================================

        isUpdatingReadStatus = true;

        updateActionStates();

        // ========================================
        // 19.10. ĐÁNH DẤU ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        notificationExecutor.execute(() -> {

            NotificationRepository.NotificationResult result;

            try {

                result = NotificationRepository.markAsRead(
                        appContext,
                        customerId,
                        notificationId
                );

            } catch (RuntimeException e) {

                result = null;
            }

            final NotificationRepository.NotificationResult
                    finalResult = result;

            // ========================================
            // 19.11. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isUpdatingReadStatus = false;

                // ========================================
                // 19.12. KIỂM TRA LẠI CUSTOMER
                // ========================================

                if (!isSameCustomer(customerId)) {

                    openLogin();
                    return;
                }

                // ========================================
                // 19.13. XỬ LÝ THẤT BẠI
                // ========================================

                if (finalResult == null
                        || !finalResult.isSuccess()) {

                    String message =
                            finalResult != null
                                    ? finalResult.getMessage()
                                    : "Không thể đánh dấu đã đọc.";

                    Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_LONG
                    ).show();

                    // Cập nhật lại trạng thái thực tế
                    loadNotifications();

                    return;
                }

                // ========================================
                // 19.14. CẬP NHẬT DANH SÁCH CỤC BỘ
                // ========================================

                for (int i = 0;
                     i < allNotifications.size();
                     i++) {

                    Notification item =
                            allNotifications.get(i);

                    if (notificationId.equals(
                            item.getNotificationId()
                    )) {

                        allNotifications.set(
                                i,
                                item.markAsRead()
                        );

                        break;
                    }
                }

                // ========================================
                // 19.15. CẬP NHẬT RECYCLERVIEW
                // ========================================

                displayNotifications();

                // ========================================
                // 19.16. MỞ CHI TIẾT BOOKING
                // ========================================

                openBookingDetail(
                        bookingCode
                );
            });
        });
    }

    // ========================================
    // 20. ĐÁNH DẤU TẤT CẢ ĐÃ ĐỌC
    // ========================================

    private void markAllNotificationsAsRead() {

        // ========================================
        // 20.1. KIỂM TRA TRẠNG THÁI
        // ========================================

        if (!isDataReady
                || isLoading
                || isUpdatingReadStatus) {

            return;
        }

        // ========================================
        // 20.2. KIỂM TRA THÔNG BÁO CHƯA ĐỌC
        // ========================================

        if (countUnreadNotifications() == 0) {

            Toast.makeText(
                    this,
                    "Không có thông báo chưa đọc.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 20.3. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 20.4. LẤY CUSTOMER ID
        // ========================================

        final String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {
            return;
        }

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 20.5. KHÓA THAO TÁC
        // ========================================

        isUpdatingReadStatus = true;

        updateActionStates();

        // ========================================
        // 20.6. GỌI REPOSITORY TRÊN LUỒNG RIÊNG
        // ========================================

        notificationExecutor.execute(() -> {

            NotificationRepository.NotificationResult result;

            try {

                result = NotificationRepository.markAllAsRead(
                        appContext,
                        customerId
                );

            } catch (RuntimeException e) {

                result = null;
            }

            final NotificationRepository.NotificationResult
                    finalResult = result;

            // ========================================
            // 20.7. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isUpdatingReadStatus = false;

                // ========================================
                // 20.8. KIỂM TRA LẠI SESSION
                // ========================================

                if (!isSameCustomer(customerId)) {

                    openLogin();
                    return;
                }

                // ========================================
                // 20.9. HIỂN THỊ KẾT QUẢ
                // ========================================

                String message;

                if (finalResult != null) {

                    message = finalResult.getMessage();

                } else {

                    message =
                            "Không thể cập nhật thông báo.";
                }

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();

                // ========================================
                // 20.10. ĐỌC LẠI DANH SÁCH
                // ========================================

                // Dù thành công hay thất bại,
                // đọc lại để lấy dữ liệu mới nhất.

                loadNotifications();
            });
        });
    }

    // ========================================
    // 21. MỞ CHI TIẾT BOOKING
    // ========================================

    private void openBookingDetail(
            String bookingCode
    ) {

        // ========================================
        // 21.1. KIỂM TRA BOOKING CODE
        // ========================================

        bookingCode = normalizeValue(
                bookingCode
        );

        if (bookingCode == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy mã lịch hẹn.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 21.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 21.3. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerNotificationsActivity.this,
                CustomerBookingDetailActivity.class
        );

        // ========================================
        // 21.4. TRUYỀN MÃ BOOKING
        // ========================================

        intent.putExtra(
                CustomerBookingDetailActivity.EXTRA_BOOKING_CODE,
                bookingCode
        );

        // ========================================
        // 21.5. MỞ ACTIVITY
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 22. HIỂN THỊ LỖI ĐỌC DỮ LIỆU
    // ========================================

    private void showLoadError(
            String message
    ) {

        // ========================================
        // 22.1. CẬP NHẬT TRẠNG THÁI
        // ========================================

        isLoading = false;

        isDataReady = false;

        allNotifications.clear();

        notificationAdapter.submitList(
                new ArrayList<>()
        );

        // ========================================
        // 22.2. ẨN DANH SÁCH
        // ========================================

        recyclerCustomerNotifications.setVisibility(
                View.GONE
        );

        // ========================================
        // 22.3. ẨN TRỐNG VÀ LOADING
        // ========================================

        layoutCustomerNotificationsEmpty.setVisibility(
                View.GONE
        );

        progressCustomerNotifications.setVisibility(
                View.GONE
        );

        // ========================================
        // 22.4. HIỂN THỊ LỖI
        // ========================================

        txtCustomerNotificationsError.setText(
                message
        );

        layoutCustomerNotificationsError.setVisibility(
                View.VISIBLE
        );

        // ========================================
        // 22.5. CẬP NHẬT NÚT
        // ========================================

        txtCustomerNotificationsUnreadCount.setText(
                "Chưa thể cập nhật"
        );

        updateActionStates();
    }

    // ========================================
    // 23. KIỂM TRA ĐÚNG CUSTOMER
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
    // 24. CHUẨN HÓA CHUỖI
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
    // 25. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerNotificationsActivity.this,
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
    // 26. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        notificationExecutor.shutdown();

        super.onDestroy();
    }
}
