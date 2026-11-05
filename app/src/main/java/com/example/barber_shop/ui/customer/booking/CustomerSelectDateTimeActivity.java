
package com.example.barber_shop.ui.customer.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CustomerSelectDateTimeActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    public static final String EXTRA_BARBER_ID =
            "extra_barber_id";

    // Dùng để truyền sang Booking Confirmation
    public static final String EXTRA_BOOKING_DATE =
            "extra_booking_date";

    public static final String EXTRA_BOOKING_TIME =
            "extra_booking_time";

    // ========================================
    // 2. CÁC KEY LƯU TRẠNG THÁI MÀN HÌNH
    // ========================================

    private static final String STATE_HAS_DATE =
            "state_has_date";

    private static final String STATE_SELECTED_DATE =
            "state_selected_date";

    private static final String STATE_SELECTED_TIME =
            "state_selected_time";

    // ========================================
    // 3. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerSelectDateTime;

    // Thông tin đặt lịch
    private TextView txtBookingBranchName;
    private TextView txtBookingServiceName;
    private TextView txtBookingBarberName;
    private TextView txtBookingServiceDuration;
    private TextView txtBookingTotalPrice;

    // Chọn ngày
    private MaterialCardView cardBookingDatePicker;
    private TextView txtBookingSelectedDate;

    // Chọn giờ
    private ChipGroup chipGroupBookingTimeSlots;
    private TextView txtBookingTimeStatus;

    // Tổng hợp thời gian
    private TextView txtBookingSelectedTime;

    // Nút tiếp tục
    private TextView txtBookingContinueHint;
    private MaterialButton btnBookingContinue;

    // ========================================
    // 4. KHAI BÁO DỮ LIỆU
    // ========================================

    private SessionManager sessionManager;

    private Branch currentBranch;
    private SalonService currentService;
    private Barber currentBarber;

    private String currentBranchId;
    private String currentServiceId;
    private String currentBarberId;

    // Ngày được chọn
    private Calendar selectedDate;

    // Giờ được chọn, định dạng HH:mm
    private String selectedTime;

    // Tránh xử lý thay đổi Chip trong lúc tạo lại
    private boolean isUpdatingTimeSlots = false;

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Sử dụng giao diện XML riêng của Customer
        setContentView(
                R.layout.activity_customer_select_date_time
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();

        // Lấy và kiểm tra dữ liệu đặt lịch
        if (!loadBookingData()) {
            return;
        }

        // Hiển thị thông tin
        displayBookingInfo();

        // Thiết lập sự kiện
        setupListeners();

        // Khôi phục ngày giờ khi xoay màn hình
        restoreSelection(savedInstanceState);

        // Cập nhật trạng thái nút
        updateSelectionState();
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

        // Kiểm tra quyền Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Làm mới khung giờ khi quay lại ứng dụng
        // để tránh giữ một giờ đã trôi qua
        if (selectedDate != null && currentService != null) {

            // Nếu ngày đã chọn trở thành ngày quá khứ
            if (selectedDate.before(getTodayStart())) {

                selectedDate = null;
                selectedTime = null;

                txtBookingSelectedDate.setText(
                        "Chưa chọn ngày"
                );

                chipGroupBookingTimeSlots.removeAllViews();

                txtBookingTimeStatus.setVisibility(View.VISIBLE);
                txtBookingTimeStatus.setText(
                        "Vui lòng chọn ngày để xem khung giờ."
                );

                updateSelectionState();

            } else {

                // Lưu giờ đang chọn trước khi làm mới
                String previousTime = selectedTime;

                renderTimeSlots();

                // Khôi phục nếu giờ vẫn còn hợp lệ
                restoreTimeSelection(previousTime);
            }
        }
    }

    // ========================================
    // 7. LƯU TRẠNG THÁI KHI XOAY MÀN HÌNH
    // ========================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        boolean hasDate = selectedDate != null;

        outState.putBoolean(
                STATE_HAS_DATE,
                hasDate
        );

        if (hasDate) {

            outState.putLong(
                    STATE_SELECTED_DATE,
                    selectedDate.getTimeInMillis()
            );
        }

        outState.putString(
                STATE_SELECTED_TIME,
                selectedTime
        );
    }

    // ========================================
    // 8. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerSelectDateTimeRoot
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
    // 9. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        toolbarCustomerSelectDateTime = findViewById(
                R.id.toolbarCustomerSelectDateTime
        );

        // Thông tin Booking
        txtBookingBranchName = findViewById(
                R.id.txtBookingBranchName
        );

        txtBookingServiceName = findViewById(
                R.id.txtBookingServiceName
        );

        txtBookingBarberName = findViewById(
                R.id.txtBookingBarberName
        );

        txtBookingServiceDuration = findViewById(
                R.id.txtBookingServiceDuration
        );

        txtBookingTotalPrice = findViewById(
                R.id.txtBookingTotalPrice
        );

        // Chọn ngày
        cardBookingDatePicker = findViewById(
                R.id.cardBookingDatePicker
        );

        txtBookingSelectedDate = findViewById(
                R.id.txtBookingSelectedDate
        );

        // Chọn giờ
        chipGroupBookingTimeSlots = findViewById(
                R.id.chipGroupBookingTimeSlots
        );

        txtBookingTimeStatus = findViewById(
                R.id.txtBookingTimeStatus
        );

        // Thời gian được chọn
        txtBookingSelectedTime = findViewById(
                R.id.txtBookingSelectedTime
        );

        // Tiếp tục
        txtBookingContinueHint = findViewById(
                R.id.txtBookingContinueHint
        );

        btnBookingContinue = findViewById(
                R.id.btnBookingContinue
        );
    }

    // ========================================
    // 10. NHẬN VÀ KIỂM TRA DỮ LIỆU BOOKING
    // ========================================

    private boolean loadBookingData() {

        // Lấy 3 ID từ màn hình trước
        currentBranchId = normalizeId(
                getIntent().getStringExtra(EXTRA_BRANCH_ID)
        );

        currentServiceId = normalizeId(
                getIntent().getStringExtra(EXTRA_SERVICE_ID)
        );

        currentBarberId = normalizeId(
                getIntent().getStringExtra(EXTRA_BARBER_ID)
        );

        // Bắt buộc có đủ 3 ID
        if (currentBranchId == null
                || currentServiceId == null
                || currentBarberId == null) {

            showErrorAndFinish(
                    "Vui lòng chọn đầy đủ chi nhánh, "
                            + "dịch vụ và Barber."
            );

            return false;
        }

        // Tìm dữ liệu trong Repository
        currentBranch = BranchRepository.findById(
                currentBranchId
        );

        currentService = ServiceRepository.findById(
                currentServiceId
        );

        currentBarber = BarberRepository.findById(
                currentBarberId
        );

        // Kiểm tra các đối tượng tồn tại
        if (currentBranch == null
                || currentService == null
                || currentBarber == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin đặt lịch."
            );

            return false;
        }

        // ========================================
        // 10.1. KIỂM TRA CHI NHÁNH CỦA BARBER
        // ========================================

        if (!currentBranchId.equals(
                currentBarber.getBranchId()
        )) {

            showErrorAndFinish(
                    "Barber không thuộc chi nhánh đã chọn."
            );

            return false;
        }

        // ========================================
        // 10.2. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        List<SalonService> availableServices =
                ServiceRepository.getServicesForBranch(
                        currentBranchId
                );

        boolean serviceFound = false;

        if (availableServices != null) {

            for (SalonService service : availableServices) {

                if (currentServiceId.equals(
                        service.getId()
                )) {

                    serviceFound = true;
                    break;
                }
            }
        }

        if (!serviceFound) {

            showErrorAndFinish(
                    "Dịch vụ đã chọn không có tại chi nhánh này."
            );

            return false;
        }

        // Lưu ý:
        // Chưa có dữ liệu xác nhận Barber thực hiện
        // được dịch vụ nào. Sẽ kiểm tra khi tích hợp BE.

        return true;
    }

    // ========================================
    // 11. CHUẨN HÓA ID
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
    // 12. HIỂN THỊ THÔNG TIN BOOKING
    // ========================================

    private void displayBookingInfo() {

        // Chi nhánh
        txtBookingBranchName.setText(
                currentBranch.getName()
        );

        // Dịch vụ
        txtBookingServiceName.setText(
                currentService.getName()
        );

        // Barber
        txtBookingBarberName.setText(
                currentBarber.getName()
        );

        // Thời lượng dịch vụ
        txtBookingServiceDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentService.getDurationMinutes()
                )
        );

        // Định dạng tiền Việt Nam
        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        txtBookingTotalPrice.setText(
                formatter.format(
                        currentService.getPriceVnd()
                ) + " đ"
        );
    }

    // ========================================
    // 13. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarCustomerSelectDateTime
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // Mở lịch chọn ngày
        cardBookingDatePicker
                .setOnClickListener(
                        v -> showDatePicker()
                );

        // Lắng nghe khi chọn Chip khung giờ
        chipGroupBookingTimeSlots
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (isUpdatingTimeSlots) {
                                return;
                            }

                            // Chưa chọn khung giờ
                            if (checkedIds.isEmpty()) {

                                selectedTime = null;

                            } else {

                                View selectedView =
                                        group.findViewById(
                                                checkedIds.get(0)
                                        );

                                if (selectedView instanceof Chip) {

                                    Object tag =
                                            selectedView.getTag();

                                    selectedTime = tag instanceof String
                                            ? (String) tag
                                            : null;
                                }
                            }

                            updateSelectionState();
                        }
                );

        // Nhấn nút Tiếp tục
        btnBookingContinue
                .setOnClickListener(
                        v -> continueBooking()
                );
    }

    // ========================================
    // 14. MỞ DATE PICKER
    // ========================================

    private void showDatePicker() {

        Calendar initialDate = selectedDate != null
                ? (Calendar) selectedDate.clone()
                : Calendar.getInstance();

        DatePickerDialog datePickerDialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, dayOfMonth) -> {

                            Calendar chosenDate =
                                    Calendar.getInstance();

                            chosenDate.set(
                                    year,
                                    month,
                                    dayOfMonth
                            );

                            setStartOfDay(chosenDate);

                            // Không chấp nhận ngày quá khứ
                            if (chosenDate.before(getTodayStart())) {

                                Toast.makeText(
                                        this,
                                        "Không thể chọn ngày trong quá khứ.",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            // Lưu ngày Customer chọn
                            selectedDate = chosenDate;

                            // Đổi ngày phải chọn lại giờ
                            selectedTime = null;

                            // Hiển thị ngày
                            txtBookingSelectedDate.setText(
                                    formatDisplayDate(selectedDate)
                            );

                            // Tạo danh sách giờ mới
                            renderTimeSlots();
                        },
                        initialDate.get(Calendar.YEAR),
                        initialDate.get(Calendar.MONTH),
                        initialDate.get(Calendar.DAY_OF_MONTH)
                );

        // Đặt ngày nhỏ nhất là hôm nay
        datePickerDialog.getDatePicker().setMinDate(
                getTodayStart().getTimeInMillis()
        );

        datePickerDialog.show();
    }

    // ========================================
    // 15. TẠO CÁC KHUNG GIỜ MINH HỌA
    // ========================================

    private void renderTimeSlots() {

        isUpdatingTimeSlots = true;

        // Xóa các Chip cũ
        chipGroupBookingTimeSlots.removeAllViews();

        // Reset giờ đã chọn
        selectedTime = null;

        // Chưa chọn ngày
        if (selectedDate == null) {

            isUpdatingTimeSlots = false;

            txtBookingTimeStatus.setVisibility(View.VISIBLE);

            txtBookingTimeStatus.setText(
                    "Vui lòng chọn ngày để xem khung giờ."
            );

            updateSelectionState();
            return;
        }

        int slotCount = 0;

        // ========================================
        // BUỔI SÁNG: 08:00 - 12:00
        // ========================================

        for (int hour = 8; hour < 12; hour++) {

            for (int minute = 0; minute < 60; minute += 30) {

                if (addTimeSlotIfValid(
                        hour,
                        minute,
                        12 * 60
                )) {
                    slotCount++;
                }
            }
        }

        // ========================================
        // BUỔI CHIỀU: 13:00 - 18:00
        // ========================================

        for (int hour = 13; hour < 18; hour++) {

            for (int minute = 0; minute < 60; minute += 30) {

                if (addTimeSlotIfValid(
                        hour,
                        minute,
                        18 * 60
                )) {
                    slotCount++;
                }
            }
        }

        isUpdatingTimeSlots = false;

        // Không có khung giờ hợp lệ
        if (slotCount == 0) {

            txtBookingTimeStatus.setVisibility(View.VISIBLE);

            txtBookingTimeStatus.setText(
                    "Không có khung giờ minh họa phù hợp "
                            + "cho ngày này."
            );

        } else {

            txtBookingTimeStatus.setVisibility(View.GONE);
        }

        updateSelectionState();
    }

    // ========================================
    // 16. KIỂM TRA VÀ THÊM MỘT KHUNG GIỜ
    // ========================================

    private boolean addTimeSlotIfValid(
            int hour,
            int minute,
            int periodEndMinutes
    ) {

        int startMinutes = hour * 60 + minute;

        int duration = currentService.getDurationMinutes();

        // Dịch vụ phải có thời lượng hợp lệ
        if (duration <= 0) {
            return false;
        }

        // Không cho thời gian phục vụ vượt giờ kết thúc
        // của buổi sáng hoặc buổi chiều
        if (startMinutes + duration > periodEndMinutes) {
            return false;
        }

        // Nếu chọn hôm nay, bỏ các giờ đã qua
        if (isToday(selectedDate)) {

            Calendar now = Calendar.getInstance();

            int currentMinutes =
                    now.get(Calendar.HOUR_OF_DAY) * 60
                            + now.get(Calendar.MINUTE);

            if (startMinutes <= currentMinutes) {
                return false;
            }
        }

        // Định dạng giờ: HH:mm
        String timeText = String.format(
                Locale.US,
                "%02d:%02d",
                hour,
                minute
        );

        // Tạo Chip và thêm vào ChipGroup
        addTimeChip(timeText);

        return true;
    }

    // ========================================
    // 17. TẠO CHIP KHUNG GIỜ
    // ========================================

    private void addTimeChip(String timeText) {

        Chip chip = new Chip(this);

        // ID duy nhất để ChipGroup quản lý lựa chọn
        chip.setId(View.generateViewId());

        // Hiển thị giờ
        chip.setText(timeText);

        // Lưu giờ trong Tag
        chip.setTag(timeText);

        // Cho phép chọn Chip
        chip.setCheckable(true);
        chip.setClickable(true);

        // Hiển thị dấu tích khi chọn
        chip.setCheckedIconVisible(true);

        // Thiết lập giao diện màu sắc
        int selectedBackground = ContextCompat.getColor(
                this,
                R.color.primary_light
        );

        int normalBackground = ContextCompat.getColor(
                this,
                R.color.surface
        );

        int selectedTextColor = ContextCompat.getColor(
                this,
                R.color.primary_dark
        );

        int normalTextColor = ContextCompat.getColor(
                this,
                R.color.text_primary
        );

        int selectedBorder = ContextCompat.getColor(
                this,
                R.color.primary
        );

        int normalBorder = ContextCompat.getColor(
                this,
                R.color.border_default
        );

        int[][] states = {
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };

        // Màu nền theo trạng thái
        chip.setChipBackgroundColor(
                new ColorStateList(
                        states,
                        new int[]{
                                selectedBackground,
                                normalBackground
                        }
                )
        );

        // Màu chữ theo trạng thái
        chip.setTextColor(
                new ColorStateList(
                        states,
                        new int[]{
                                selectedTextColor,
                                normalTextColor
                        }
                )
        );

        // Màu viền theo trạng thái
        chip.setChipStrokeColor(
                new ColorStateList(
                        states,
                        new int[]{
                                selectedBorder,
                                normalBorder
                        }
                )
        );

        // Độ dày viền
        float density = getResources()
                .getDisplayMetrics().density;

        chip.setChipStrokeWidth(density);

        // Kích thước chữ
        chip.setTextSize(14f);

        // Thêm Chip vào nhóm
        chipGroupBookingTimeSlots.addView(chip);
    }

    // ========================================
    // 18. CẬP NHẬT TRẠNG THÁI LỰA CHỌN
    // ========================================

    private void updateSelectionState() {

        boolean hasDate = selectedDate != null;
        boolean hasTime = selectedTime != null;

        // Nút Tiếp tục chỉ bật khi đủ ngày và giờ
        boolean canContinue = hasDate && hasTime;

        btnBookingContinue.setEnabled(canContinue);

        // ========================================
        // 18.1. HIỂN THỊ THỜI GIAN ĐÃ CHỌN
        // ========================================

        if (canContinue) {

            txtBookingSelectedTime.setText(
                    formatDisplayDate(selectedDate)
                            + " - " + selectedTime
            );

            txtBookingContinueHint.setText(
                    "Đã chọn đầy đủ ngày và giờ."
            );

        } else if (hasDate) {

            txtBookingSelectedTime.setText(
                    formatDisplayDate(selectedDate)
                            + " - Chưa chọn giờ"
            );

            txtBookingContinueHint.setText(
                    "Vui lòng chọn khung giờ để tiếp tục."
            );

        } else {

            txtBookingSelectedTime.setText(
                    "Chưa chọn ngày và giờ"
            );

            txtBookingContinueHint.setText(
                    "Vui lòng chọn ngày và giờ để tiếp tục."
            );
        }
    }

    // ========================================
    // 19. KHÔI PHỤC NGÀY GIỜ ĐÃ CHỌN
    // ========================================

    private void restoreSelection(Bundle savedInstanceState) {

        if (savedInstanceState == null) {
            return;
        }

        boolean hasDate = savedInstanceState.getBoolean(
                STATE_HAS_DATE,
                false
        );

        if (!hasDate) {
            return;
        }

        Calendar savedDate = Calendar.getInstance();

        savedDate.setTimeInMillis(
                savedInstanceState.getLong(
                        STATE_SELECTED_DATE
                )
        );

        setStartOfDay(savedDate);

        // Không khôi phục ngày đã qua
        if (savedDate.before(getTodayStart())) {
            return;
        }

        selectedDate = savedDate;

        txtBookingSelectedDate.setText(
                formatDisplayDate(selectedDate)
        );

        // Tạo lại các khung giờ
        renderTimeSlots();

        // Khôi phục giờ nếu vẫn hợp lệ
        restoreTimeSelection(
                savedInstanceState.getString(
                        STATE_SELECTED_TIME
                )
        );
    }

    // ========================================
    // 20. KHÔI PHỤC KHUNG GIỜ ĐÃ CHỌN
    // ========================================

    private void restoreTimeSelection(String timeToRestore) {

        if (timeToRestore == null) {
            updateSelectionState();
            return;
        }

        // Duyệt từng Chip hiện có
        for (int i = 0;
             i < chipGroupBookingTimeSlots.getChildCount();
             i++) {

            View child =
                    chipGroupBookingTimeSlots.getChildAt(i);

            if (!(child instanceof Chip)) {
                continue;
            }

            Chip chip = (Chip) child;

            if (timeToRestore.equals(chip.getTag())) {

                // Chỉ khôi phục nếu Chip còn tồn tại
                // trong danh sách giờ hợp lệ
                chip.setChecked(true);

                selectedTime = timeToRestore;

                updateSelectionState();
                return;
            }
        }

        // Giờ cũ không còn hợp lệ
        selectedTime = null;
        updateSelectionState();
    }

    // ========================================
    // 21. TIẾP TỤC QUY TRÌNH BOOKING
    // ========================================

    private void continueBooking() {

        // ========================================
        // 21.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // ========================================
        // 21.2. KIỂM TRA ĐỦ NGÀY VÀ GIỜ
        // ========================================

        if (selectedDate == null
                || selectedTime == null) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn đầy đủ ngày và giờ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 21.3. KIỂM TRA NGÀY ĐÃ CHỌN
        // ========================================

        if (selectedDate.before(getTodayStart())) {

            Toast.makeText(
                    this,
                    "Ngày đã chọn không còn hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            selectedDate = null;
            renderTimeSlots();

            txtBookingSelectedDate.setText(
                    "Chưa chọn ngày"
            );

            return;
        }

        // ========================================
        // 21.4. KIỂM TRA GIỜ ĐÃ CHỌN
        // ========================================

        if (!isSelectedTimeValid()) {

            Toast.makeText(
                    this,
                    "Khung giờ đã chọn không còn hợp lệ. "
                            + "Vui lòng chọn lại.",
                    Toast.LENGTH_LONG
            ).show();

            renderTimeSlots();
            return;
        }

        // ========================================
        // 21.5. CHUẨN BỊ DỮ LIỆU
        // ========================================

        // Chuyển ngày sang định dạng yyyy-MM-dd
        String bookingDate = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        ).format(
                selectedDate.getTime()
        );

        // Giờ có định dạng HH:mm
        String bookingTime = selectedTime;

        // ========================================
        // 21.6. MỞ CUSTOMER BOOKING CONFIRMATION
        // ========================================

        // Tạo Intent chuyển sang STT 12
        Intent intent = new Intent(
                CustomerSelectDateTimeActivity.this,
                CustomerBookingConfirmationActivity.class
        );

        // ========================================
        // 21.7. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerBookingConfirmationActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 21.8. TRUYỀN ID DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerBookingConfirmationActivity.EXTRA_SERVICE_ID,
                currentServiceId
        );

        // ========================================
        // 21.9. TRUYỀN ID BARBER
        // ========================================

        intent.putExtra(
                CustomerBookingConfirmationActivity.EXTRA_BARBER_ID,
                currentBarberId
        );

        // ========================================
        // 21.10. TRUYỀN NGÀY ĐẶT LỊCH
        // ========================================

        intent.putExtra(
                CustomerBookingConfirmationActivity.EXTRA_BOOKING_DATE,
                bookingDate
        );

        // ========================================
        // 21.11. TRUYỀN GIỜ ĐẶT LỊCH
        // ========================================

        intent.putExtra(
                CustomerBookingConfirmationActivity.EXTRA_BOOKING_TIME,
                bookingTime
        );

        // ========================================
        // 21.12. CHUYỂN SANG STT 12
        // ========================================

        // Không gọi finish() vì Customer
        // cần có thể quay lại chỉnh sửa ngày giờ.
        startActivity(intent);
    }

    // ========================================
    // 22. KIỂM TRA GIỜ ĐÃ CHỌN CÒN HỢP LỆ
    // ========================================

    private boolean isSelectedTimeValid() {

        if (selectedDate == null || selectedTime == null) {
            return false;
        }

        // Kiểm tra giờ theo định dạng HH:mm
        if (!selectedTime.matches(
                "^([01]\\d|2[0-3]):[0-5]\\d$"
        )) {
            return false;
        }

        String[] parts = selectedTime.split(":");

        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);

        int startMinutes = hour * 60 + minute;

        int duration = currentService.getDurationMinutes();

        // Giờ bắt đầu phải thuộc danh sách cách 30 phút
        if (minute != 0 && minute != 30) {
            return false;
        }

        if (duration <= 0) {
            return false;
        }

        // Kiểm tra nằm trọn trong một buổi
        boolean inMorning =
                startMinutes >= 8 * 60
                        && startMinutes + duration <= 12 * 60;

        boolean inAfternoon =
                startMinutes >= 13 * 60
                        && startMinutes + duration <= 18 * 60;

        if (!inMorning && !inAfternoon) {
            return false;
        }

        // Nếu hôm nay, giờ phải còn ở tương lai
        if (isToday(selectedDate)) {

            Calendar now = Calendar.getInstance();

            int currentMinutes =
                    now.get(Calendar.HOUR_OF_DAY) * 60
                            + now.get(Calendar.MINUTE);

            if (startMinutes <= currentMinutes) {
                return false;
            }
        }

        return true;
    }

    // ========================================
    // 23. ĐỊNH DẠNG NGÀY HIỂN THỊ
    // ========================================

    private String formatDisplayDate(Calendar date) {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "EEEE, dd/MM/yyyy",
                        new Locale("vi", "VN")
                );

        return formatter.format(
                date.getTime()
        );
    }

    // ========================================
    // 24. LẤY THỜI ĐIỂM BẮT ĐẦU HÔM NAY
    // ========================================

    private Calendar getTodayStart() {

        Calendar today = Calendar.getInstance();

        setStartOfDay(today);

        return today;
    }

    // ========================================
    // 25. ĐƯA CALENDAR VỀ 00:00:00
    // ========================================

    private void setStartOfDay(Calendar calendar) {

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    // ========================================
    // 26. KIỂM TRA NGÀY CÓ PHẢI HÔM NAY
    // ========================================

    private boolean isToday(Calendar date) {

        if (date == null) {
            return false;
        }

        Calendar today = Calendar.getInstance();

        return date.get(Calendar.YEAR)
                == today.get(Calendar.YEAR)
                && date.get(Calendar.DAY_OF_YEAR)
                == today.get(Calendar.DAY_OF_YEAR);
    }

    // ========================================
    // 27. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerSelectDateTimeActivity.this,
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
    // 28. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
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

