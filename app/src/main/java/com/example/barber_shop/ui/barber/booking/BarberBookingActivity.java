
package com.example.barber_shop.ui.barber.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
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
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarberBookingActivity
        extends AppCompatActivity {

    private static final String STATE_SELECTED_FILTER =
            "state_selected_filter";

    private static final String STATE_SELECTED_DATE =
            "state_selected_date";

    private static final String STATE_SEARCH_QUERY =
            "state_search_query";

    private static final Locale VIETNAM_LOCALE =
            new Locale("vi", "VN");

    private static final String COUNT_NOT_LOADED = "—";

    private View btnBarberBookingBack;
    private View btnBarberBookingRefresh;

    private TextView txtBarberBookingTotalCount;
    private TextView txtBarberBookingPendingCount;
    private TextView txtBarberBookingResultCount;

    private TextInputEditText edtBarberBookingSearch;

    private MaterialButton btnBarberBookingSelectDate;
    private View btnBarberBookingClearDate;

    private ChipGroup chipGroupBarberBookingStatus;

    private RecyclerView recyclerBarberBookings;
    private BarberTodayBookingAdapter bookingAdapter;

    private LinearLayout layoutBarberBookingEmpty;
    private TextView txtBarberBookingEmptyTitle;
    private TextView txtBarberBookingEmptyMessage;
    private MaterialButton btnBarberBookingClearFilters;

    private LinearLayout layoutBarberBookingLoading;

    private LinearLayout layoutBarberBookingError;
    private TextView txtBarberBookingError;
    private View btnBarberBookingRetry;

    private SessionManager sessionManager;

    private ExecutorService executorService;

    private final List<Booking> allBookings =
            new ArrayList<>();

    private int selectedFilterId =
            R.id.chipBarberBookingAll;

    private String selectedDate = null;

    private String searchQuery = "";

    private int loadRequestVersion = 0;

    private boolean hasLoadedData = false;
    private boolean isRedirectingToLogin = false;

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
                            R.id.chipBarberBookingAll
                    );

            if (isSupportedFilter(restoredFilter)) {
                selectedFilterId = restoredFilter;
            }

            selectedDate =
                    savedInstanceState.getString(
                            STATE_SELECTED_DATE
                    );

            searchQuery =
                    savedInstanceState.getString(
                            STATE_SEARCH_QUERY,
                            ""
                    );
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_barber_booking
        );

        executorService =
                Executors.newSingleThreadExecutor();

        setupWindowInsets();

        initViews();

        setupRecyclerView();

        setupStatusFilters();

        setupSearch();

        setupDateFilter();

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

        loadBookings();
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
                R.id.barberBookingRoot
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

        btnBarberBookingBack = findViewById(
                R.id.btnBarberBookingBack
        );

        btnBarberBookingRefresh = findViewById(
                R.id.btnBarberBookingRefresh
        );

        txtBarberBookingTotalCount = findViewById(
                R.id.txtBarberBookingTotalCount
        );

        txtBarberBookingPendingCount = findViewById(
                R.id.txtBarberBookingPendingCount
        );

        txtBarberBookingResultCount = findViewById(
                R.id.txtBarberBookingResultCount
        );

        edtBarberBookingSearch = findViewById(
                R.id.edtBarberBookingSearch
        );

        btnBarberBookingSelectDate = findViewById(
                R.id.btnBarberBookingSelectDate
        );

        btnBarberBookingClearDate = findViewById(
                R.id.btnBarberBookingClearDate
        );

        chipGroupBarberBookingStatus = findViewById(
                R.id.chipGroupBarberBookingStatus
        );

        recyclerBarberBookings = findViewById(
                R.id.recyclerBarberBookings
        );

        layoutBarberBookingEmpty = findViewById(
                R.id.layoutBarberBookingEmpty
        );

        txtBarberBookingEmptyTitle = findViewById(
                R.id.txtBarberBookingEmptyTitle
        );

        txtBarberBookingEmptyMessage = findViewById(
                R.id.txtBarberBookingEmptyMessage
        );

        btnBarberBookingClearFilters = findViewById(
                R.id.btnBarberBookingClearFilters
        );

        layoutBarberBookingLoading = findViewById(
                R.id.layoutBarberBookingLoading
        );

        layoutBarberBookingError = findViewById(
                R.id.layoutBarberBookingError
        );

        txtBarberBookingError = findViewById(
                R.id.txtBarberBookingError
        );

        btnBarberBookingRetry = findViewById(
                R.id.btnBarberBookingRetry
        );
    }

    private void setupRecyclerView() {

        bookingAdapter =
                new BarberTodayBookingAdapter(
                        this::showBookingDetails
                );

        recyclerBarberBookings.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerBarberBookings.setHasFixedSize(
                true
        );

        recyclerBarberBookings.setAdapter(
                bookingAdapter
        );

        recyclerBarberBookings.setVisibility(
                View.GONE
        );
    }

    private void setupStatusFilters() {

        chipGroupBarberBookingStatus.check(
                selectedFilterId
        );

        chipGroupBarberBookingStatus
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

                            applyFilters();
                        }
                );
    }

    private boolean isSupportedFilter(int filterId) {

        return filterId == R.id.chipBarberBookingAll
                || filterId == R.id.chipBarberBookingPending
                || filterId == R.id.chipBarberBookingConfirmed
                || filterId == R.id.chipBarberBookingCompleted
                || filterId == R.id.chipBarberBookingCancelled;
    }

    private String getSelectedStatus() {

        if (selectedFilterId
                == R.id.chipBarberBookingPending) {

            return Booking.STATUS_PENDING;
        }

        if (selectedFilterId
                == R.id.chipBarberBookingConfirmed) {

            return Booking.STATUS_CONFIRMED;
        }

        if (selectedFilterId
                == R.id.chipBarberBookingCompleted) {

            return Booking.STATUS_COMPLETED;
        }

        if (selectedFilterId
                == R.id.chipBarberBookingCancelled) {

            return Booking.STATUS_CANCELLED;
        }

        return null;
    }

    private void setupSearch() {

        edtBarberBookingSearch.setText(
                searchQuery
        );

        edtBarberBookingSearch.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {

                        searchQuery =
                                s.toString().trim();

                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    private void setupDateFilter() {

        updateDateFilterButton();

        btnBarberBookingSelectDate.setOnClickListener(
                v -> showDatePicker()
        );

        btnBarberBookingClearDate.setOnClickListener(
                v -> clearDateFilter()
        );
    }

    private void showDatePicker() {

        if (!ensureBarberSession()) {
            return;
        }

        Calendar calendar =
                Calendar.getInstance();

        if (selectedDate != null) {

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.US
                    );

            formatter.setLenient(false);

            try {

                Date date = formatter.parse(
                        selectedDate
                );

                if (date != null) {

                    calendar.setTime(date);
                }

            } catch (ParseException ignored) {
            }
        }

        int year = calendar.get(
                Calendar.YEAR
        );

        int month = calendar.get(
                Calendar.MONTH
        );

        int day = calendar.get(
                Calendar.DAY_OF_MONTH
        );

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, selectedYear,
                         selectedMonth, selectedDay) -> {

                            selectedDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    selectedYear,
                                    selectedMonth + 1,
                                    selectedDay
                            );

                            updateDateFilterButton();

                            applyFilters();
                        },
                        year,
                        month,
                        day
                );

        datePickerDialog.show();
    }

    private void updateDateFilterButton() {

        if (selectedDate == null
                || selectedDate.trim().isEmpty()) {

            btnBarberBookingSelectDate.setText(
                    "📅  Tất cả ngày"
            );

            btnBarberBookingClearDate.setVisibility(
                    View.GONE
            );

            return;
        }

        btnBarberBookingSelectDate.setText(
                "📅  " + formatBookingDate(selectedDate)
        );

        btnBarberBookingClearDate.setVisibility(
                View.VISIBLE
        );
    }

    private void clearDateFilter() {

        selectedDate = null;

        updateDateFilterButton();

        applyFilters();
    }

    private void loadBookings() {

        if (!ensureBarberSession()) {
            return;
        }

        String barberId =
                sessionManager.getUserId();

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

        String requestedBarberId =
                barberId.trim();

        int currentRequestVersion =
                ++loadRequestVersion;

        hasLoadedData = false;

        allBookings.clear();

        bookingAdapter.setBookings(
                new ArrayList<>()
        );

        txtBarberBookingTotalCount.setText(
                COUNT_NOT_LOADED
        );

        txtBarberBookingPendingCount.setText(
                COUNT_NOT_LOADED
        );

        txtBarberBookingResultCount.setText(
                "Đang tải danh sách..."
        );

        showLoading();

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
                        BookingRepository.getBookingsForBarber(
                                getApplicationContext(),
                                requestedBarberId
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
                            "Không thể đọc danh sách lịch hẹn. "
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
                    loadBookings();
                    return;
                }

                if (finalError != null) {

                    hasLoadedData = false;

                    txtBarberBookingTotalCount.setText(
                            COUNT_NOT_LOADED
                    );

                    txtBarberBookingPendingCount.setText(
                            COUNT_NOT_LOADED
                    );

                    txtBarberBookingResultCount.setText(
                            "Không thể tải lịch hẹn"
                    );

                    showError(finalError);

                    return;
                }

                allBookings.clear();

                if (finalBookings != null) {

                    allBookings.addAll(
                            finalBookings
                    );
                }

                hasLoadedData = true;

                updateStatistics();

                applyFilters();
            });
        });
    }

    private void updateStatistics() {

        int totalCount =
                allBookings.size();

        int pendingCount = 0;

        for (Booking booking : allBookings) {

            if (booking != null
                    && Booking.STATUS_PENDING.equals(
                    booking.getStatus()
            )) {

                pendingCount++;
            }
        }

        txtBarberBookingTotalCount.setText(
                String.valueOf(totalCount)
        );

        txtBarberBookingPendingCount.setText(
                String.valueOf(pendingCount)
        );
    }

    private void applyFilters() {

        if (!hasLoadedData) {
            return;
        }

        List<Booking> filteredBookings =
                new ArrayList<>();

        String selectedStatus =
                getSelectedStatus();

        String normalizedQuery =
                searchQuery.toLowerCase(
                        VIETNAM_LOCALE
                );

        for (Booking booking : allBookings) {

            if (booking == null) {
                continue;
            }

            if (selectedDate != null
                    && !selectedDate.equals(
                    booking.getBookingDate()
            )) {
                continue;
            }

            if (selectedStatus != null
                    && !selectedStatus.equals(
                    booking.getStatus()
            )) {
                continue;
            }

            if (!normalizedQuery.isEmpty()) {

                String bookingCode =
                        safeText(
                                booking.getBookingCode()
                        ).toLowerCase(VIETNAM_LOCALE);

                String serviceName =
                        safeText(
                                booking.getServiceName()
                        ).toLowerCase(VIETNAM_LOCALE);

                boolean matchesSearch =
                        bookingCode.contains(
                                normalizedQuery
                        )
                                || serviceName.contains(
                                normalizedQuery
                        );

                if (!matchesSearch) {
                    continue;
                }
            }

            filteredBookings.add(
                    booking
            );
        }

        bookingAdapter.setBookings(
                filteredBookings
        );

        txtBarberBookingResultCount.setText(
                "Hiển thị "
                        + filteredBookings.size()
                        + "/"
                        + allBookings.size()
                        + " lịch hẹn"
        );

        if (filteredBookings.isEmpty()) {

            if (allBookings.isEmpty()) {

                showEmptyState(
                        "Chưa có lịch hẹn",
                        "Bạn chưa có lịch hẹn nào "
                                + "được phân công."
                );

            } else {

                showEmptyState(
                        "Không tìm thấy lịch hẹn",
                        "Không có lịch hẹn nào phù hợp "
                                + "với điều kiện tìm kiếm "
                                + "hoặc bộ lọc hiện tại."
                );
            }

            return;
        }

        showBookingList();
    }

    private void clearAllFilters() {

        searchQuery = "";

        edtBarberBookingSearch.setText("");

        selectedDate = null;

        updateDateFilterButton();

        selectedFilterId =
                R.id.chipBarberBookingAll;

        chipGroupBarberBookingStatus.check(
                selectedFilterId
        );

        applyFilters();
    }

    private boolean hasActiveFilters() {

        return !searchQuery.isEmpty()
                || selectedDate != null
                || selectedFilterId
                != R.id.chipBarberBookingAll;
    }

    private void showBookingList() {

        recyclerBarberBookings.setVisibility(
                View.VISIBLE
        );

        layoutBarberBookingEmpty.setVisibility(
                View.GONE
        );

        layoutBarberBookingLoading.setVisibility(
                View.GONE
        );

        layoutBarberBookingError.setVisibility(
                View.GONE
        );
    }

    private void showLoading() {

        recyclerBarberBookings.setVisibility(
                View.GONE
        );

        layoutBarberBookingEmpty.setVisibility(
                View.GONE
        );

        layoutBarberBookingError.setVisibility(
                View.GONE
        );

        layoutBarberBookingLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showEmptyState(
            String title,
            String message
    ) {

        recyclerBarberBookings.setVisibility(
                View.GONE
        );

        layoutBarberBookingLoading.setVisibility(
                View.GONE
        );

        layoutBarberBookingError.setVisibility(
                View.GONE
        );

        layoutBarberBookingEmpty.setVisibility(
                View.VISIBLE
        );

        txtBarberBookingEmptyTitle.setText(
                title
        );

        txtBarberBookingEmptyMessage.setText(
                message
        );

        btnBarberBookingClearFilters.setVisibility(
                hasActiveFilters()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void showError(String message) {

        recyclerBarberBookings.setVisibility(
                View.GONE
        );

        layoutBarberBookingLoading.setVisibility(
                View.GONE
        );

        layoutBarberBookingEmpty.setVisibility(
                View.GONE
        );

        layoutBarberBookingError.setVisibility(
                View.VISIBLE
        );

        if (message == null
                || message.trim().isEmpty()) {

            message =
                    "Không thể tải dữ liệu lịch hẹn. "
                            + "Vui lòng thử lại.";
        }

        txtBarberBookingError.setText(
                message
        );
    }

    private void setupListeners() {

        btnBarberBookingBack.setOnClickListener(
                v -> finish()
        );

        btnBarberBookingRefresh.setOnClickListener(
                v -> refreshBookings()
        );

        btnBarberBookingRetry.setOnClickListener(
                v -> refreshBookings()
        );

        btnBarberBookingClearFilters.setOnClickListener(
                v -> clearAllFilters()
        );
    }

    private void refreshBookings() {

        if (!ensureBarberSession()) {
            return;
        }

        loadBookings();
    }

    private void showBookingDetails(Booking booking) {

        if (!ensureBarberSession()) {
            return;
        }

        if (booking == null) {
            return;
        }

        String barberId =
                sessionManager.getUserId();

        if (barberId == null
                || !barberId.trim().equals(
                booking.getBarberId()
        )) {

            Toast.makeText(
                    this,
                    "Bạn không có quyền xem lịch hẹn này.",
                    Toast.LENGTH_SHORT
            ).show();

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
                        + "Ngày hẹn: "
                        + formatBookingDate(
                        booking.getBookingDate()
                )
                        + "\n"
                        + "Giờ hẹn: "
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
                .setTitle("Chi tiết lịch hẹn")
                .setMessage(bookingInformation)
                .setPositiveButton(
                        "Đóng",
                        (dialog, which) -> dialog.dismiss()
                )
                .show();
    }

    private String formatBookingDate(String date) {

        if (date == null
                || !date.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {
            return "Chưa cập nhật";
        }

        return date.substring(8, 10)
                + "/"
                + date.substring(5, 7)
                + "/"
                + date.substring(0, 4);
    }

    private String safeText(String value) {

        return value == null
                ? ""
                : value.trim();
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
                BarberBookingActivity.this,
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

        outState.putString(
                STATE_SELECTED_DATE,
                selectedDate
        );

        outState.putString(
                STATE_SEARCH_QUERY,
                searchQuery
        );

        super.onSaveInstanceState(
                outState
        );
    }
}
