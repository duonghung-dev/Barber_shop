
package com.example.barber_shop.ui.customer.booking;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerRescheduleBookingActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    public static final String EXTRA_BOOKING_CODE =
            "extra_booking_code";

    // ========================================
    // 2. KEY LƯU LỰA CHỌN KHI XOAY MÀN HÌNH
    // ========================================

    private static final String STATE_NEW_DATE =
            "state_reschedule_new_date";

    private static final String STATE_NEW_TIME =
            "state_reschedule_new_time";

    // ========================================
    // 3. KHAI BÁO CÁC VIEW
    // ========================================

    // Toolbar
    private MaterialToolbar toolbarCustomerRescheduleBooking;

    // Nội dung
    private ScrollView scrollCustomerRescheduleBooking;

    // Thông tin Booking
    private TextView txtRescheduleBookingCode;
    private TextView txtRescheduleBookingStatus;
    private TextView txtRescheduleBranchName;
    private TextView txtRescheduleServiceName;
    private TextView txtRescheduleBarberName;
    private TextView txtRescheduleDuration;

    // Thời gian đã đặt
    private TextView txtRescheduleCurrentDate;
    private TextView txtRescheduleCurrentTime;

    // Chọn ngày mới
    private TextView txtRescheduleSelectedDate;
    private MaterialButton btnRescheduleSelectDate;

    // Chọn giờ mới
    private TextView txtRescheduleTimeHint;
    private ChipGroup chipGroupRescheduleTimeSlots;
    private TextView txtRescheduleSelectedTime;

    // Thông báo
    private TextView txtRescheduleNotice;
    private TextView txtRescheduleConfirmationStatus;

    // Loading
    private ProgressBar progressCustomerRescheduleBooking;

    // Lỗi
    private LinearLayout layoutCustomerRescheduleError;
    private TextView txtCustomerRescheduleError;

    // Xác nhận
    private MaterialButton btnConfirmReschedule;

    // ========================================
    // 4. KHAI BÁO DỮ LIỆU
    // ========================================

    private SessionManager sessionManager;

    // Mã Booking được truyền từ màn hình chi tiết
    private String currentBookingCode;

    // Booking hiện tại
    private Booking currentBooking;

    // Ngày mới: yyyy-MM-dd
    private String selectedNewDate;

    // Giờ mới: HH:mm
    private String selectedNewTime;

    // Kiểm tra trạng thái lưu
    private boolean isSubmitting = false;

    // Đánh dấu khi đang tạo lại các Chip
    private boolean isBuildingTimeSlots = false;

    // Mỗi Chip ID tương ứng một giá trị giờ
    private final Map<Integer, String> timeSlotMap =
            new HashMap<>();

    // Luồng xử lý Repository
    private final ExecutorService bookingExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt các lần tải Booking
    private int loadRequestVersion = 0;

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 5.1. Khởi tạo Session
        sessionManager = new SessionManager(this);

        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // 5.2. Edge-to-Edge
        EdgeToEdge.enable(this);

        // 5.3. Giao diện
        setContentView(
                R.layout.activity_customer_reschedule_booking
        );

        // 5.4. Thiết lập View
        setupWindowInsets();
        initViews();
        setupListeners();
        setupBackNavigation();

        // 5.5. Nhận mã Booking
        currentBookingCode = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BOOKING_CODE
                )
        );

        // 5.6. Khôi phục lựa chọn ngày giờ
        if (savedInstanceState != null) {

            selectedNewDate = normalizeValue(
                    savedInstanceState.getString(
                            STATE_NEW_DATE
                    )
            );

            selectedNewTime = normalizeValue(
                    savedInstanceState.getString(
                            STATE_NEW_TIME
                    )
            );
        }

        // 5.7. Kiểm tra mã Booking
        if (currentBookingCode == null) {

            showLoadError(
                    "Không tìm thấy mã lịch hẹn."
            );
        }

        // Booking sẽ được tải trong onResume()
    }

    // ========================================
    // 6. TẢI LẠI BOOKING KHI QUAY VỀ
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing()) {
            return;
        }

        // 6.1. Kiểm tra Session
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // 6.2. Không tải lại khi đang lưu
        if (isSubmitting) {
            return;
        }

        // 6.3. Đọc lại Booking
        if (currentBookingCode != null) {
            loadBookingDetail();
        }
    }

    // ========================================
    // 7. LƯU LỰA CHỌN KHI XOAY MÀN HÌNH
    // ========================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(
                STATE_NEW_DATE,
                selectedNewDate
        );

        outState.putString(
                STATE_NEW_TIME,
                selectedNewTime
        );
    }

    // ========================================
    // 8. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerRescheduleBookingRoot
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
    // 9. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        // Toolbar
        toolbarCustomerRescheduleBooking = findViewById(
                R.id.toolbarCustomerRescheduleBooking
        );

        // ScrollView
        scrollCustomerRescheduleBooking = findViewById(
                R.id.scrollCustomerRescheduleBooking
        );

        // Thông tin Booking
        txtRescheduleBookingCode = findViewById(
                R.id.txtRescheduleBookingCode
        );

        txtRescheduleBookingStatus = findViewById(
                R.id.txtRescheduleBookingStatus
        );

        txtRescheduleBranchName = findViewById(
                R.id.txtRescheduleBranchName
        );

        txtRescheduleServiceName = findViewById(
                R.id.txtRescheduleServiceName
        );

        txtRescheduleBarberName = findViewById(
                R.id.txtRescheduleBarberName
        );

        txtRescheduleDuration = findViewById(
                R.id.txtRescheduleDuration
        );

        // Ngày giờ hiện tại
        txtRescheduleCurrentDate = findViewById(
                R.id.txtRescheduleCurrentDate
        );

        txtRescheduleCurrentTime = findViewById(
                R.id.txtRescheduleCurrentTime
        );

        // Chọn ngày mới
        txtRescheduleSelectedDate = findViewById(
                R.id.txtRescheduleSelectedDate
        );

        btnRescheduleSelectDate = findViewById(
                R.id.btnRescheduleSelectDate
        );

        // Chọn giờ mới
        txtRescheduleTimeHint = findViewById(
                R.id.txtRescheduleTimeHint
        );

        chipGroupRescheduleTimeSlots = findViewById(
                R.id.chipGroupRescheduleTimeSlots
        );

        txtRescheduleSelectedTime = findViewById(
                R.id.txtRescheduleSelectedTime
        );

        // Thông báo
        txtRescheduleNotice = findViewById(
                R.id.txtRescheduleNotice
        );

        txtRescheduleConfirmationStatus = findViewById(
                R.id.txtRescheduleConfirmationStatus
        );

        // Loading
        progressCustomerRescheduleBooking = findViewById(
                R.id.progressCustomerRescheduleBooking
        );

        // Trạng thái lỗi
        layoutCustomerRescheduleError = findViewById(
                R.id.layoutCustomerRescheduleError
        );

        txtCustomerRescheduleError = findViewById(
                R.id.txtCustomerRescheduleError
        );

        // Nút xác nhận
        btnConfirmReschedule = findViewById(
                R.id.btnConfirmReschedule
        );
    }

    // ========================================
    // 10. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // 10.1. Nút quay lại trên Toolbar
        toolbarCustomerRescheduleBooking
                .setNavigationOnClickListener(
                        v -> handleBackNavigation()
                );

        // 10.2. Mở lịch chọn ngày
        btnRescheduleSelectDate.setOnClickListener(
                v -> openDatePicker()
        );

        // 10.3. Chọn khung giờ mới
        chipGroupRescheduleTimeSlots
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (isBuildingTimeSlots) {
                                return;
                            }

                            selectedNewTime = null;

                            if (!checkedIds.isEmpty()) {

                                int selectedChipId =
                                        checkedIds.get(0);

                                selectedNewTime =
                                        timeSlotMap.get(
                                                selectedChipId
                                        );
                            }

                            updateSelectedTimeText();
                            updateConfirmationState();
                        }
                );

        // 10.4. Xác nhận đổi lịch
        btnConfirmReschedule.setOnClickListener(
                v -> confirmReschedule()
        );
    }

    // ========================================
    // 11. XỬ LÝ NÚT BACK
    // ========================================

    private void setupBackNavigation() {

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {
                        handleBackNavigation();
                    }
                }
        );
    }

    private void handleBackNavigation() {

        // Không quay lại khi Repository đang lưu
        if (isSubmitting) {

            Toast.makeText(
                    this,
                    "Đang cập nhật lịch hẹn.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Quay về màn hình Chi tiết lịch hẹn
        finish();
    }

    // ========================================
    // 12. ĐỌC BOOKING TỪ REPOSITORY
    // ========================================

    private void loadBookingDetail() {

        // 12.1. Kiểm tra Customer ID
        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            showLoadError(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // 12.2. Hiển thị Loading
        showLoading();

        final int requestVersion =
                ++loadRequestVersion;

        final Context appContext =
                getApplicationContext();

        final String bookingCode =
                currentBookingCode;

        // 12.3. Đọc trên luồng riêng
        bookingExecutor.execute(() -> {

            Booking loadedBooking = null;
            String errorMessage = null;

            try {

                loadedBooking =
                        BookingRepository.findByCode(
                                appContext,
                                bookingCode
                        );

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc thông tin lịch hẹn.";
            }

            final Booking resultBooking =
                    loadedBooking;

            final String resultError =
                    errorMessage;

            // 12.4. Cập nhật giao diện
            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // Kiểm tra Session một lần nữa
                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeValue(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                // Lỗi đọc dữ liệu
                if (resultError != null) {

                    showLoadError(resultError);
                    return;
                }

                // Không tìm thấy Booking
                if (resultBooking == null) {

                    showLoadError(
                            "Không tìm thấy lịch hẹn."
                    );

                    return;
                }

                // 12.5. Kiểm tra quyền sở hữu
                if (!customerId.equals(
                        resultBooking.getCustomerId()
                )) {

                    showLoadError(
                            "Bạn không có quyền đổi lịch hẹn này."
                    );

                    return;
                }

                // 12.6. Kiểm tra trạng thái
                if (!resultBooking.isActive()) {

                    showLoadError(
                            "Chỉ có thể đổi lịch đang chờ xác nhận "
                                    + "hoặc đã xác nhận."
                    );

                    return;
                }

                // 12.7. Không đổi lịch đã qua giờ
                if (!isFutureDateTime(
                        resultBooking.getBookingDate(),
                        resultBooking.getBookingTime()
                )) {

                    showLoadError(
                            "Lịch hẹn đã qua giờ bắt đầu, "
                                    + "không thể đổi lịch."
                    );

                    return;
                }

                // 12.8. Lưu Booking đang xử lý
                currentBooking = resultBooking;

                // Hiển thị thông tin
                displayBookingInfo();
            });
        });
    }

    // ========================================
    // 13. HIỂN THỊ LOADING
    // ========================================

    private void showLoading() {

        scrollCustomerRescheduleBooking.setVisibility(
                View.GONE
        );

        layoutCustomerRescheduleError.setVisibility(
                View.GONE
        );

        progressCustomerRescheduleBooking.setVisibility(
                View.VISIBLE
        );

        btnConfirmReschedule.setEnabled(false);
        btnRescheduleSelectDate.setEnabled(false);
    }

    // ========================================
    // 14. HIỂN THỊ THÔNG TIN BOOKING
    // ========================================

    private void displayBookingInfo() {

        if (currentBooking == null) {
            return;
        }

        // 14.1. Mã Booking
        txtRescheduleBookingCode.setText(
                currentBooking.getBookingCode()
        );

        // 14.2. Trạng thái
        txtRescheduleBookingStatus.setText(
                currentBooking.getStatusDisplayName()
        );

        // 14.3. Chi nhánh
        txtRescheduleBranchName.setText(
                currentBooking.getBranchName()
        );

        // 14.4. Dịch vụ
        txtRescheduleServiceName.setText(
                currentBooking.getServiceName()
        );

        // 14.5. Barber
        txtRescheduleBarberName.setText(
                currentBooking.getBarberName()
        );

        // 14.6. Thời lượng
        txtRescheduleDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentBooking.getDurationMinutes()
                )
        );

        // 14.7. Ngày đang đặt
        txtRescheduleCurrentDate.setText(
                formatDisplayDate(
                        currentBooking.getBookingDate()
                )
        );

        // 14.8. Giờ đang đặt
        txtRescheduleCurrentTime.setText(
                currentBooking.getBookingTime()
        );

        // 14.9. Hiển thị ngày được chọn
        updateSelectedDateText();

        // 14.10. Hiển thị các khung giờ
        renderTimeSlots();

        // 14.11. Hoàn tất Loading
        progressCustomerRescheduleBooking.setVisibility(
                View.GONE
        );

        layoutCustomerRescheduleError.setVisibility(
                View.GONE
        );

        scrollCustomerRescheduleBooking.setVisibility(
                View.VISIBLE
        );

        btnRescheduleSelectDate.setEnabled(true);

        updateConfirmationState();
    }

    // ========================================
    // 15. CHỌN NGÀY HẸN MỚI
    // ========================================

    private void openDatePicker() {

        if (currentBooking == null || isSubmitting) {
            return;
        }

        // 15.1. Ngày mặc định là hôm nay
        Calendar initialDate = Calendar.getInstance();

        // Nếu đã chọn ngày, sử dụng ngày đó
        Date previouslySelected =
                parseStrictDate(selectedNewDate);

        if (previouslySelected != null
                && !isDateBeforeToday(previouslySelected)) {

            initialDate.setTime(previouslySelected);
        }

        // 15.2. Tạo Date Picker
        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            // Chuyển ngày sang yyyy-MM-dd
                            String newDate = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    year,
                                    month + 1,
                                    dayOfMonth
                            );

                            // Kiểm tra ngày hợp lệ
                            if (isDateBeforeToday(
                                    parseStrictDate(newDate)
                            )) {

                                Toast.makeText(
                                        this,
                                        "Không thể chọn ngày đã qua.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            // 15.3. Nếu đổi ngày, xóa giờ cũ
                            if (!newDate.equals(selectedNewDate)) {
                                selectedNewTime = null;
                            }

                            selectedNewDate = newDate;

                            // Cập nhật ngày
                            updateSelectedDateText();

                            // Tạo lại các Chip giờ
                            renderTimeSlots();

                            // Cập nhật trạng thái xác nhận
                            updateConfirmationState();
                        },
                        initialDate.get(Calendar.YEAR),
                        initialDate.get(Calendar.MONTH),
                        initialDate.get(Calendar.DAY_OF_MONTH)
                );

        // 15.4. Chỉ cho phép chọn từ hôm nay
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        datePickerDialog.getDatePicker().setMinDate(
                today.getTimeInMillis()
        );

        datePickerDialog.show();
    }

    // ========================================
    // 16. TẠO DANH SÁCH KHUNG GIỜ
    // ========================================

    private void renderTimeSlots() {

        // Đánh dấu bắt đầu tạo lại Chip
        isBuildingTimeSlots = true;

        // Giữ lại giờ đã chọn (nếu còn hợp lệ)
        String previousTime = selectedNewTime;

        selectedNewTime = null;

        // Xóa các Chip cũ
        chipGroupRescheduleTimeSlots.removeAllViews();
        timeSlotMap.clear();

        // 16.1. Chưa có Booking hoặc ngày mới
        if (currentBooking == null
                || selectedNewDate == null) {

            txtRescheduleTimeHint.setText(
                    "Vui lòng chọn ngày mới "
                            + "để xem các khung giờ."
            );

            isBuildingTimeSlots = false;

            updateSelectedTimeText();
            return;
        }

        // 16.2. Kiểm tra ngày hợp lệ
        Date date = parseStrictDate(selectedNewDate);

        if (date == null || isDateBeforeToday(date)) {

            txtRescheduleTimeHint.setText(
                    "Ngày đã chọn không hợp lệ. "
                            + "Vui lòng chọn lại."
            );

            isBuildingTimeSlots = false;

            updateSelectedTimeText();
            return;
        }

        // 16.3. Kiểm tra thời lượng dịch vụ
        int durationMinutes =
                currentBooking.getDurationMinutes();

        if (durationMinutes <= 0) {

            txtRescheduleTimeHint.setText(
                    "Thời lượng dịch vụ không hợp lệ."
            );

            isBuildingTimeSlots = false;

            updateSelectedTimeText();
            return;
        }

        // ========================================
        // 16.4. KHUNG GIỜ BUỔI SÁNG
        // ========================================

        int morningCount = addTimeSlots(
                8 * 60,
                12 * 60,
                durationMinutes,
                previousTime
        );

        // ========================================
        // 16.5. KHUNG GIỜ BUỔI CHIỀU
        // ========================================

        int afternoonCount = addTimeSlots(
                13 * 60,
                18 * 60,
                durationMinutes,
                previousTime
        );

        // ========================================
        // 16.6. THÔNG BÁO KẾT QUẢ
        // ========================================

        int totalSlots = morningCount + afternoonCount;

        if (totalSlots == 0) {

            txtRescheduleTimeHint.setText(
                    "Không có khung giờ phù hợp "
                            + "với ngày và thời lượng dịch vụ."
            );

        } else {

            txtRescheduleTimeHint.setText(
                    "Có " + totalSlots
                            + " khung giờ phù hợp với giờ hoạt động. "
                            + "Khả năng trùng lịch Barber "
                            + "sẽ được kiểm tra khi xác nhận."
            );
        }

        isBuildingTimeSlots = false;

        updateSelectedTimeText();
    }

    // ========================================
    // 17. THÊM CHIP KHUNG GIỜ VÀO CHIPGROUP
    // ========================================

    private int addTimeSlots(
            int startMinutes,
            int endMinutes,
            int durationMinutes,
            String previousTime
    ) {

        int addedCount = 0;

        // Mỗi khung giờ cách nhau 30 phút
        for (int minutes = startMinutes;
             minutes + durationMinutes <= endMinutes;
             minutes += 30) {

            // 17.1. Chuyển số phút thành HH:mm
            int hour = minutes / 60;
            int minute = minutes % 60;

            String timeText = String.format(
                    Locale.US,
                    "%02d:%02d",
                    hour,
                    minute
            );

            // 17.2. Loại bỏ thời gian đã qua
            if (!isFutureDateTime(
                    selectedNewDate,
                    timeText
            )) {
                continue;
            }

            // 17.3. Tạo Chip
            Chip chip = createTimeSlotChip(timeText);

            // 17.4. Lưu ID và giá trị giờ
            timeSlotMap.put(
                    chip.getId(),
                    timeText
            );

            // 17.5. Thêm vào ChipGroup
            chipGroupRescheduleTimeSlots.addView(chip);

            // 17.6. Khôi phục lựa chọn cũ nếu hợp lệ
            if (timeText.equals(previousTime)) {

                chip.setChecked(true);

                selectedNewTime = timeText;
            }

            addedCount++;
        }

        return addedCount;
    }

    // ========================================
    // 18. TẠO CHIP KHUNG GIỜ
    // ========================================

    private Chip createTimeSlotChip(String timeText) {

        Chip chip = new Chip(this);

        // ID duy nhất cho từng Chip
        chip.setId(View.generateViewId());

        // Nội dung giờ
        chip.setText(timeText);

        // Cho phép chọn Chip
        chip.setCheckable(true);
        chip.setClickable(true);

        // Không sử dụng biểu tượng đóng
        chip.setCloseIconVisible(false);

        // Không hiển thị dấu tích khi chọn
        chip.setCheckedIconVisible(false);

        // ========================================
        // 18.1. MÀU NỀN THEO TRẠNG THÁI
        // ========================================

        int selectedBackground = ContextCompat.getColor(
                this,
                R.color.primary_light
        );

        int normalBackground = ContextCompat.getColor(
                this,
                R.color.surface
        );

        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        int[] backgroundColors = new int[]{
                selectedBackground,
                normalBackground
        };

        chip.setChipBackgroundColor(
                new ColorStateList(
                        states,
                        backgroundColors
                )
        );

        // ========================================
        // 18.2. MÀU CHỮ THEO TRẠNG THÁI
        // ========================================

        int selectedTextColor = ContextCompat.getColor(
                this,
                R.color.primary_dark
        );

        int normalTextColor = ContextCompat.getColor(
                this,
                R.color.text_primary
        );

        chip.setTextColor(
                new ColorStateList(
                        states,
                        new int[]{
                                selectedTextColor,
                                normalTextColor
                        }
                )
        );

        // ========================================
        // 18.3. VIỀN CHIP
        // ========================================

        int borderColor = ContextCompat.getColor(
                this,
                R.color.border_default
        );

        chip.setChipStrokeColor(
                ColorStateList.valueOf(borderColor)
        );

        chip.setChipStrokeWidth(
                getResources().getDisplayMetrics().density
        );

        return chip;
    }

    // ========================================
    // 19. HIỂN THỊ NGÀY MỚI ĐÃ CHỌN
    // ========================================

    private void updateSelectedDateText() {

        if (selectedNewDate == null) {

            txtRescheduleSelectedDate.setText(
                    "Chưa chọn ngày mới"
            );

            return;
        }

        txtRescheduleSelectedDate.setText(
                formatDisplayDate(selectedNewDate)
        );
    }

    // ========================================
    // 20. HIỂN THỊ GIỜ MỚI ĐÃ CHỌN
    // ========================================

    private void updateSelectedTimeText() {

        if (selectedNewTime == null) {

            txtRescheduleSelectedTime.setText(
                    "Giờ mới: Chưa chọn"
            );

            return;
        }

        txtRescheduleSelectedTime.setText(
                "Giờ mới: " + selectedNewTime
        );
    }

    // ========================================
    // 21. KIỂM TRA ĐIỀU KIỆN ĐỔI LỊCH
    // ========================================

    private String getRescheduleValidationError() {

        // 21.1. Chưa tải Booking
        if (currentBooking == null) {

            return "Chưa có thông tin lịch hẹn.";
        }

        // 21.2. Trạng thái Booking
        if (!currentBooking.isActive()) {

            return "Lịch hẹn không còn được phép đổi.";
        }

        // 21.3. Lịch hiện tại đã qua giờ
        if (!isFutureDateTime(
                currentBooking.getBookingDate(),
                currentBooking.getBookingTime()
        )) {

            return "Lịch hẹn hiện tại đã qua giờ.";
        }

        // 21.4. Chưa chọn ngày mới
        if (selectedNewDate == null) {

            return "Vui lòng chọn ngày hẹn mới.";
        }

        // 21.5. Ngày mới không hợp lệ
        Date newDate = parseStrictDate(selectedNewDate);

        if (newDate == null || isDateBeforeToday(newDate)) {

            return "Ngày hẹn mới không hợp lệ.";
        }

        // 21.6. Chưa chọn giờ
        if (selectedNewTime == null) {

            return "Vui lòng chọn giờ hẹn mới.";
        }

        // 21.7. Giờ mới phải thuộc Chip hợp lệ
        if (!timeSlotMap.containsValue(selectedNewTime)) {

            return "Khung giờ mới không hợp lệ.";
        }

        // 21.8. Không chọn thời gian đã qua
        if (!isFutureDateTime(
                selectedNewDate,
                selectedNewTime
        )) {

            return "Thời gian mới phải ở tương lai.";
        }

        // 21.9. Phải khác thời gian hiện tại
        if (selectedNewDate.equals(
                currentBooking.getBookingDate()
        ) && selectedNewTime.equals(
                currentBooking.getBookingTime()
        )) {

            return "Vui lòng chọn thời gian khác lịch hiện tại.";
        }

        // Các điều kiện cục bộ đã hợp lệ.
        // Repository sẽ kiểm tra trùng lịch khi lưu.
        return null;
    }

    // ========================================
    // 22. CẬP NHẬT NÚT XÁC NHẬN
    // ========================================

    private void updateConfirmationState() {

        // 22.1. Đang lưu
        if (isSubmitting) {

            btnConfirmReschedule.setEnabled(false);

            btnConfirmReschedule.setText(
                    "Đang cập nhật..."
            );

            btnRescheduleSelectDate.setEnabled(false);

            txtRescheduleConfirmationStatus.setText(
                    "Hệ thống đang xử lý thay đổi."
            );

            return;
        }

        btnRescheduleSelectDate.setEnabled(
                currentBooking != null
        );

        btnConfirmReschedule.setText(
                "Xác nhận đổi lịch"
        );

        // 22.2. Kiểm tra dữ liệu
        String validationError =
                getRescheduleValidationError();

        if (validationError != null) {

            btnConfirmReschedule.setEnabled(false);

            txtRescheduleConfirmationStatus.setText(
                    validationError
            );

            return;
        }

        // 22.3. Cho phép xác nhận
        btnConfirmReschedule.setEnabled(true);

        txtRescheduleConfirmationStatus.setText(
                "Thời gian đã chọn hợp lệ để gửi yêu cầu. "
                        + "Khung giờ sẽ được kiểm tra trùng khi lưu."
        );
    }

    // ========================================
    // 23. XÁC NHẬN ĐỔI LỊCH
    // ========================================

    private void confirmReschedule() {

        // 23.1. Ngăn nhấn nhiều lần
        if (isSubmitting) {
            return;
        }

        // 23.2. Kiểm tra Session
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // 23.3. Kiểm tra lựa chọn mới
        String validationError =
                getRescheduleValidationError();

        if (validationError != null) {

            Toast.makeText(
                    this,
                    validationError,
                    Toast.LENGTH_LONG
            ).show();

            updateConfirmationState();
            return;
        }

        // 23.4. Xác nhận với Customer
        String message =
                "Mã lịch hẹn: "
                        + currentBooking.getBookingCode()
                        + "\n\n"
                        + "Thời gian cũ:\n"
                        + formatDisplayDate(
                        currentBooking.getBookingDate()
                )
                        + " · "
                        + currentBooking.getBookingTime()
                        + "\n\n"
                        + "Thời gian mới:\n"
                        + formatDisplayDate(selectedNewDate)
                        + " · "
                        + selectedNewTime
                        + "\n\n"
                        + "Bạn có chắc chắn muốn đổi lịch hẹn?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Xác nhận đổi lịch")
                .setMessage(message)
                .setNegativeButton(
                        "Quay lại",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Xác nhận",
                        (dialog, which) -> {

                            // Kiểm tra lại ngay trước khi lưu
                            String latestError =
                                    getRescheduleValidationError();

                            if (latestError != null) {

                                Toast.makeText(
                                        this,
                                        latestError,
                                        Toast.LENGTH_LONG
                                ).show();

                                updateConfirmationState();
                                return;
                            }

                            performReschedule();
                        }
                )
                .show();
    }

    // ========================================
    // 24. GỌI REPOSITORY ĐỂ ĐỔI LỊCH
    // ========================================

    private void performReschedule() {

        if (isSubmitting || currentBooking == null) {
            return;
        }

        // 24.1. Lấy Customer ID
        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            showLoadError(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // 24.2. Lưu dữ liệu sẽ gửi
        final String bookingCode =
                currentBooking.getBookingCode();

        final String newDate =
                selectedNewDate;

        final String newTime =
                selectedNewTime;

        final Context appContext =
                getApplicationContext();

        // 24.3. Chuyển sang trạng thái đang lưu
        isSubmitting = true;
        updateConfirmationState();

        // 24.4. Gọi Repository trên luồng riêng
        bookingExecutor.execute(() -> {

            BookingRepository.BookingResult result;

            try {

                result =
                        BookingRepository.rescheduleBooking(
                                appContext,
                                customerId,
                                bookingCode,
                                newDate,
                                newTime
                        );

            } catch (RuntimeException e) {

                result = null;
            }

            final BookingRepository.BookingResult finalResult =
                    result;

            // 24.5. Trả kết quả về Main Thread
            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isSubmitting = false;

                // Session đã thay đổi
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
                // 24.6. ĐỔI LỊCH THÀNH CÔNG
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()) {

                    showRescheduleSuccessDialog(
                            newDate,
                            newTime
                    );

                    return;
                }

                // ========================================
                // 24.7. ĐỔI LỊCH THẤT BẠI
                // ========================================

                String errorMessage =
                        finalResult != null
                                ? normalizeValue(
                                finalResult.getMessage()
                        )
                                : null;

                if (errorMessage == null) {

                    errorMessage =
                            "Không thể đổi lịch hẹn. "
                                    + "Vui lòng thử lại.";
                }

                updateConfirmationState();

                txtRescheduleConfirmationStatus.setText(
                        errorMessage
                );

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Đổi lịch chưa thành công")
                        .setMessage(errorMessage)
                        .setPositiveButton(
                                "Đã hiểu",
                                (dialog, which) -> dialog.dismiss()
                        )
                        .show();
            });
        });
    }

    // ========================================
    // 25. HIỂN THỊ ĐỔI LỊCH THÀNH CÔNG
    // ========================================

    private void showRescheduleSuccessDialog(
            String newDate,
            String newTime
    ) {

        // Ngăn đổi lại ngay trong khi Dialog hiển thị
        isSubmitting = true;
        updateConfirmationState();

        String message =
                "Lịch hẹn đã được cập nhật trên thiết bị."
                        + "\n\n"
                        + "Ngày mới: "
                        + formatDisplayDate(newDate)
                        + "\n"
                        + "Giờ mới: "
                        + newTime
                        + "\n\n"
                        + "Chưa đồng bộ với Backend.";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Đổi lịch thành công")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        "Xem lịch hẹn",
                        (dialog, which) -> {

                            // Quay về màn hình Booking Detail.
                            // Detail sẽ tải lại Booking trong onResume().
                            isSubmitting = false;
                            finish();
                        }
                )
                .show();
    }

    // ========================================
    // 26. ĐỊNH DẠNG NGÀY HIỂN THỊ
    // ========================================

    private String formatDisplayDate(String dateText) {

        Date date = parseStrictDate(dateText);

        if (date == null) {
            return dateText == null ? "--" : dateText;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "EEEE, dd/MM/yyyy",
                        new Locale("vi", "VN")
                );

        return formatter.format(date);
    }

    // ========================================
    // 27. PHÂN TÍCH NGÀY YYYY-MM-DD
    // ========================================

    private Date parseStrictDate(String dateText) {

        if (dateText == null
                || !dateText.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {

            return null;
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            return parser.parse(dateText);

        } catch (ParseException e) {

            return null;
        }
    }

    // ========================================
    // 28. KIỂM TRA NGÀY ĐÃ QUA
    // ========================================

    private boolean isDateBeforeToday(Date date) {

        if (date == null) {
            return true;
        }

        Calendar selected = Calendar.getInstance();
        selected.setTime(date);

        Calendar today = Calendar.getInstance();

        // Chỉ so sánh ngày, không so sánh giờ
        selected.set(Calendar.HOUR_OF_DAY, 0);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return selected.before(today);
    }

    // ========================================
    // 29. KIỂM TRA NGÀY GIỜ Ở TƯƠNG LAI
    // ========================================

    private boolean isFutureDateTime(
            String dateText,
            String timeText
    ) {

        if (dateText == null || timeText == null) {
            return false;
        }

        if (!timeText.matches(
                "([01][0-9]|2[0-3]):[0-5][0-9]"
        )) {

            return false;
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date date = parser.parse(
                    dateText + " " + timeText
            );

            return date != null
                    && date.getTime()
                    > System.currentTimeMillis();

        } catch (ParseException e) {

            return false;
        }
    }

    // ========================================
    // 30. CHUẨN HÓA CHUỖI
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
    // 31. HIỂN THỊ LỖI
    // ========================================

    private void showLoadError(String message) {

        currentBooking = null;

        // Ẩn Loading
        progressCustomerRescheduleBooking.setVisibility(
                View.GONE
        );

        // Ẩn nội dung
        scrollCustomerRescheduleBooking.setVisibility(
                View.GONE
        );

        // Hiển thị lỗi
        layoutCustomerRescheduleError.setVisibility(
                View.VISIBLE
        );

        txtCustomerRescheduleError.setText(message);

        // Khóa các nút
        btnRescheduleSelectDate.setEnabled(false);
        btnConfirmReschedule.setEnabled(false);

        txtRescheduleConfirmationStatus.setText(
                "Không thể tiếp tục đổi lịch."
        );
    }

    // ========================================
    // 32. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerRescheduleBookingActivity.this,
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
    // 33. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        bookingExecutor.shutdown();

        super.onDestroy();
    }
}
