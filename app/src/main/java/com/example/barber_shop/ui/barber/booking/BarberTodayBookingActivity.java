
package com.example.barber_shop.ui.barber.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarberTodayBookingActivity
        extends AppCompatActivity {

    private static final String STATE_SELECTED_FILTER =
            "state_selected_filter";

    private static final Locale VIETNAM_LOCALE =
            new Locale("vi", "VN");

    private static final String COUNT_NOT_LOADED = "—";

    private View btnBarberTodayBack;
    private View btnBarberTodayRefresh;

    private TextView txtBarberTodayBookingDate;
    private TextView txtBarberTodayTotalCount;
    private TextView txtBarberTodayResultCount;

    private ChipGroup chipGroupBarberTodayStatus;

    private RecyclerView recyclerBarberTodayBookings;
    private BarberTodayBookingAdapter bookingAdapter;

    private LinearLayout layoutBarberTodayEmpty;
    private TextView txtBarberTodayEmptyTitle;
    private TextView txtBarberTodayEmptyMessage;
    private View btnBarberTodayEmptyRefresh;

    private LinearLayout layoutBarberTodayLoading;

    private LinearLayout layoutBarberTodayError;
    private TextView txtBarberTodayError;
    private View btnBarberTodayRetry;

    private SessionManager sessionManager;

    private ExecutorService executorService;

    private final List<Booking> allTodayBookings =
            new ArrayList<>();

    private int selectedFilterId =
            R.id.chipBarberTodayAll;

    private int loadRequestVersion = 0;

    private boolean isRedirectingToLogin = false;
    private boolean hasLoadedData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!hasValidBarberSession()) {
            openLogin();
            return;
        }

        if (savedInstanceState != null) {

            int restoredFilter =
                    savedInstanceState.getInt(
                            STATE_SELECTED_FILTER,
                            R.id.chipBarberTodayAll
                    );

            if (isSupportedFilter(restoredFilter)) {
                selectedFilterId = restoredFilter;
            }
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_barber_today_booking
        );

        executorService =
                Executors.newSingleThreadExecutor();

        setupWindowInsets();

        initViews();

        setupRecyclerView();

        setupStatusFilters();

        setupListeners();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (isFinishing() || isRedirectingToLogin) {
            return;
        }

        if (!hasValidBarberSession()) {
            openLogin();
            return;
        }

        loadTodayBookings();
    }

    @Override
    protected void onDestroy() {

        loadRequestVersion++;

        if (executorService != null) {
            executorService.shutdownNow();
        }

        super.onDestroy();
    }

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.barberTodayBookingRoot
        );

        final int initialLeft =
                root.getPaddingLeft();

        final int initialTop =
                root.getPaddingTop();

        final int initialRight =
                root.getPaddingRight();

        final int initialBottom =
                root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets insets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            initialLeft + insets.left,
                            initialTop + insets.top,
                            initialRight + insets.right,
                            initialBottom + insets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    private void initViews() {

        btnBarberTodayBack = findViewById(
                R.id.btnBarberTodayBack
        );

        btnBarberTodayRefresh = findViewById(
                R.id.btnBarberTodayRefresh
        );

        txtBarberTodayBookingDate = findViewById(
                R.id.txtBarberTodayBookingDate
        );

        txtBarberTodayTotalCount = findViewById(
                R.id.txtBarberTodayTotalCount
        );

        chipGroupBarberTodayStatus = findViewById(
                R.id.chipGroupBarberTodayStatus
        );

        txtBarberTodayResultCount = findViewById(
                R.id.txtBarberTodayResultCount
        );

        recyclerBarberTodayBookings = findViewById(
                R.id.recyclerBarberTodayBookings
        );

        layoutBarberTodayEmpty = findViewById(
                R.id.layoutBarberTodayEmpty
        );

        txtBarberTodayEmptyTitle = findViewById(
                R.id.txtBarberTodayEmptyTitle
        );

        txtBarberTodayEmptyMessage = findViewById(
                R.id.txtBarberTodayEmptyMessage
        );

        btnBarberTodayEmptyRefresh = findViewById(
                R.id.btnBarberTodayEmptyRefresh
        );

        layoutBarberTodayLoading = findViewById(
                R.id.layoutBarberTodayLoading
        );

        layoutBarberTodayError = findViewById(
                R.id.layoutBarberTodayError
        );

        txtBarberTodayError = findViewById(
                R.id.txtBarberTodayError
        );

        btnBarberTodayRetry = findViewById(
                R.id.btnBarberTodayRetry
        );
    }

    private void setupRecyclerView() {

        bookingAdapter =
                new BarberTodayBookingAdapter(
                        this::showBookingDetails
                );

        recyclerBarberTodayBookings.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerBarberTodayBookings.setHasFixedSize(
                true
        );

        recyclerBarberTodayBookings.setAdapter(
                bookingAdapter
        );

        recyclerBarberTodayBookings.setVisibility(
                View.GONE
        );
    }

    private void setupStatusFilters() {

        chipGroupBarberTodayStatus.check(
                selectedFilterId
        );

        chipGroupBarberTodayStatus
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (checkedIds.isEmpty()) {
                                return;
                            }

                            int checkedId =
                                    checkedIds.get(0);

                            if (!isSupportedFilter(checkedId)) {
                                return;
                            }

                            selectedFilterId = checkedId;

                            if (hasLoadedData) {

                                applyStatusFilter();

                            } else {

                                txtBarberTodayResultCount.setText(
                                        getSelectedFilterName()
                                                + " • Đang cập nhật"
                                );
                            }
                        }
                );
    }

    private boolean isSupportedFilter(int filterId) {

        return filterId == R.id.chipBarberTodayAll
                || filterId == R.id.chipBarberTodayPending
                || filterId == R.id.chipBarberTodayConfirmed
                || filterId == R.id.chipBarberTodayCompleted
                || filterId == R.id.chipBarberTodayCancelled;
    }

    private String getSelectedFilterName() {

        if (selectedFilterId
                == R.id.chipBarberTodayPending) {
            return "Chờ xác nhận";
        }

        if (selectedFilterId
                == R.id.chipBarberTodayConfirmed) {
            return "Đã xác nhận";
        }

        if (selectedFilterId
                == R.id.chipBarberTodayCompleted) {
            return "Hoàn thành";
        }

        if (selectedFilterId
                == R.id.chipBarberTodayCancelled) {
            return "Đã hủy";
        }

        return "Tất cả";
    }

    private void setupTodayDate(Date date) {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "EEEE, dd/MM/yyyy",
                        VIETNAM_LOCALE
                );

        String todayText = dateFormat.format(date);

        if (!todayText.isEmpty()) {

            todayText =
                    todayText.substring(0, 1)
                            .toUpperCase(VIETNAM_LOCALE)
                            + todayText.substring(1);
        }

        txtBarberTodayBookingDate.setText(
                todayText
        );
    }

    private String getTodayDateKey(Date date) {

        SimpleDateFormat dateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        return dateFormat.format(date);
    }

    private void loadTodayBookings() {

        if (!ensureBarberSession()) {
            return;
        }

        String barberId = sessionManager.getUserId();

        if (barberId == null
                || barberId.trim().isEmpty()) {

            showError(
                    "Không xác định được Barber hiện tại."
            );

            return;
        }

        if (executorService == null
                || executorService.isShutdown()) {
            return;
        }

        Date currentDate = new Date();

        String todayDate = getTodayDateKey(
                currentDate
        );

        setupTodayDate(currentDate);

        int currentRequestVersion =
                ++loadRequestVersion;

        hasLoadedData = false;

        allTodayBookings.clear();

        bookingAdapter.setBookings(
                new ArrayList<>()
        );

        txtBarberTodayTotalCount.setText(
                COUNT_NOT_LOADED
        );

        txtBarberTodayResultCount.setText(
                getSelectedFilterName()
                        + " • Đang tải..."
        );

        showLoading();

        String requestedBarberId = barberId.trim();

        executorService.execute(() -> {

            List<Booking> resultBookings = null;

            String resultError = null;

            try {

                if (BarberRepository.findById(
                        requestedBarberId
                ) == null) {

                    throw new IllegalStateException(
                            "Tài khoản Barber chưa được liên kết "
                                    + "với hồ sơ Barber hợp lệ."
                    );
                }

                resultBookings =
                        BookingRepository
                                .getBookingsForBarberOnDate(
                                        getApplicationContext(),
                                        requestedBarberId,
                                        todayDate
                                );

                if (resultBookings == null) {

                    throw new IllegalStateException(
                            "Không nhận được danh sách lịch hẹn."
                    );
                }

            } catch (RuntimeException e) {

                resultError = e.getMessage();

                if (resultError == null
                        || resultError.trim().isEmpty()) {

                    resultError =
                            "Không thể đọc dữ liệu lịch hẹn. "
                                    + "Vui lòng thử lại.";
                }
            }

            List<Booking> finalBookings =
                    resultBookings;

            String finalError =
                    resultError;

            runOnUiThread(() -> {

                if (isFinishing()
                        || isDestroyed()
                        || isRedirectingToLogin
                        || currentRequestVersion
                        != loadRequestVersion) {
                    return;
                }

                if (!hasValidBarberSession()) {

                    openLogin();
                    return;
                }

                String currentBarberId =
                        sessionManager.getUserId();

                if (currentBarberId == null
                        || !requestedBarberId.equals(
                        currentBarberId.trim()
                )) {
                    return;
                }

                if (finalError != null) {

                    hasLoadedData = false;

                    txtBarberTodayTotalCount.setText(
                            COUNT_NOT_LOADED
                    );

                    txtBarberTodayResultCount.setText(
                            "Không thể tải lịch hẹn"
                    );

                    showError(finalError);

                    return;
                }

                allTodayBookings.clear();

                if (finalBookings != null) {

                    allTodayBookings.addAll(
                            finalBookings
                    );
                }

                hasLoadedData = true;

                txtBarberTodayTotalCount.setText(
                        String.valueOf(
                                allTodayBookings.size()
                        )
                );

                applyStatusFilter();
            });
        });
    }

    private void applyStatusFilter() {

        if (!hasLoadedData) {
            return;
        }

        List<Booking> filteredBookings =
                new ArrayList<>();

        String selectedStatus =
                getSelectedStatus();

        for (Booking booking : allTodayBookings) {

            if (booking == null) {
                continue;
            }

            if (selectedStatus == null
                    || selectedStatus.equals(
                    booking.getStatus()
            )) {

                filteredBookings.add(
                        booking
                );
            }
        }

        bookingAdapter.setBookings(
                filteredBookings
        );

        txtBarberTodayResultCount.setText(
                getSelectedFilterName()
                        + " • "
                        + filteredBookings.size()
                        + " lịch hẹn"
        );

        if (filteredBookings.isEmpty()) {

            if (allTodayBookings.isEmpty()) {

                showEmptyState(
                        "Hôm nay bạn chưa có lịch hẹn "
                                + "nào được phân công."
                );

            } else {

                showEmptyState(
                        "Không có lịch hẹn nào ở trạng thái "
                                + getSelectedFilterName()
                                .toLowerCase(VIETNAM_LOCALE)
                                + "."
                );
            }

            return;
        }

        showBookingList();
    }

    private String getSelectedStatus() {

        if (selectedFilterId
                == R.id.chipBarberTodayPending) {

            return Booking.STATUS_PENDING;
        }

        if (selectedFilterId
                == R.id.chipBarberTodayConfirmed) {

            return Booking.STATUS_CONFIRMED;
        }

        if (selectedFilterId
                == R.id.chipBarberTodayCompleted) {

            return Booking.STATUS_COMPLETED;
        }

        if (selectedFilterId
                == R.id.chipBarberTodayCancelled) {

            return Booking.STATUS_CANCELLED;
        }

        return null;
    }

    private void showBookingList() {

        layoutBarberTodayLoading.setVisibility(
                View.GONE
        );

        layoutBarberTodayEmpty.setVisibility(
                View.GONE
        );

        layoutBarberTodayError.setVisibility(
                View.GONE
        );

        recyclerBarberTodayBookings.setVisibility(
                View.VISIBLE
        );
    }

    private void showLoading() {

        recyclerBarberTodayBookings.setVisibility(
                View.GONE
        );

        layoutBarberTodayEmpty.setVisibility(
                View.GONE
        );

        layoutBarberTodayError.setVisibility(
                View.GONE
        );

        layoutBarberTodayLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showEmptyState(String message) {

        recyclerBarberTodayBookings.setVisibility(
                View.GONE
        );

        layoutBarberTodayLoading.setVisibility(
                View.GONE
        );

        layoutBarberTodayError.setVisibility(
                View.GONE
        );

        layoutBarberTodayEmpty.setVisibility(
                View.VISIBLE
        );

        txtBarberTodayEmptyTitle.setText(
                "Không có lịch hẹn phù hợp"
        );

        if (message == null
                || message.trim().isEmpty()) {

            message =
                    "Không có lịch hẹn nào phù hợp "
                            + "với bộ lọc hiện tại.";
        }

        txtBarberTodayEmptyMessage.setText(
                message
        );
    }

    private void showError(String message) {

        recyclerBarberTodayBookings.setVisibility(
                View.GONE
        );

        layoutBarberTodayLoading.setVisibility(
                View.GONE
        );

        layoutBarberTodayEmpty.setVisibility(
                View.GONE
        );

        layoutBarberTodayError.setVisibility(
                View.VISIBLE
        );

        if (message == null
                || message.trim().isEmpty()) {

            message =
                    "Không thể tải dữ liệu lịch hẹn.";
        }

        txtBarberTodayError.setText(
                message
        );
    }

    private void setupListeners() {

        btnBarberTodayBack.setOnClickListener(
                v -> finish()
        );

        btnBarberTodayRefresh.setOnClickListener(
                v -> refreshTodayBookings()
        );

        btnBarberTodayEmptyRefresh.setOnClickListener(
                v -> refreshTodayBookings()
        );

        btnBarberTodayRetry.setOnClickListener(
                v -> refreshTodayBookings()
        );
    }

    private void refreshTodayBookings() {

        if (!ensureBarberSession()) {
            return;
        }

        loadTodayBookings();
    }

    private void showBookingDetails(Booking booking) {

        if (!ensureBarberSession()) {
            return;
        }

        if (booking == null) {
            return;
        }

        String currentBarberId =
                sessionManager.getUserId();

        if (currentBarberId == null
                || !currentBarberId.trim().equals(
                booking.getBarberId()
        )) {

            showError(
                    "Bạn không có quyền xem lịch hẹn này."
            );

            return;
        }

        NumberFormat numberFormat =
                NumberFormat.getNumberInstance(
                        VIETNAM_LOCALE
                );

        String bookingInformation =
                "Mã lịch hẹn: "
                        + booking.getBookingCode()
                        + "\n\n"
                        + "Mã khách hàng: "
                        + booking.getCustomerId()
                        + "\n"
                        + "Dịch vụ: "
                        + booking.getServiceName()
                        + "\n"
                        + "Chi nhánh: "
                        + booking.getBranchName()
                        + "\n\n"
                        + "Ngày: "
                        + formatBookingDate(
                        booking.getBookingDate()
                )
                        + "\n"
                        + "Giờ: "
                        + booking.getBookingTime()
                        + "\n"
                        + "Thời lượng: "
                        + booking.getDurationMinutes()
                        + " phút"
                        + "\n\n"
                        + "Tổng tiền: "
                        + numberFormat.format(
                        booking.getTotalPrice()
                )
                        + " đ"
                        + "\n"
                        + "Trạng thái: "
                        + booking.getStatusDisplayName();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Thông tin lịch hẹn")
                .setMessage(bookingInformation)
                .setPositiveButton(
                        "Đóng",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    private String formatBookingDate(
            String bookingDate
    ) {

        if (bookingDate == null
                || !bookingDate.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {
            return "Chưa cập nhật";
        }

        return bookingDate.substring(8, 10)
                + "/"
                + bookingDate.substring(5, 7)
                + "/"
                + bookingDate.substring(0, 4);
    }

    private boolean hasValidBarberSession() {

        if (sessionManager == null
                || !sessionManager.isBarber()) {

            return false;
        }

        String barberId =
                sessionManager.getUserId();

        return barberId != null
                && !barberId.trim().isEmpty();
    }

    private boolean ensureBarberSession() {

        if (hasValidBarberSession()) {
            return true;
        }

        openLogin();

        return false;
    }

    private void openLogin() {

        if (isRedirectingToLogin) {
            return;
        }

        isRedirectingToLogin = true;

        Intent intent = new Intent(
                BarberTodayBookingActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {

        outState.putInt(
                STATE_SELECTED_FILTER,
                selectedFilterId
        );

        super.onSaveInstanceState(
                outState
        );
    }
}
