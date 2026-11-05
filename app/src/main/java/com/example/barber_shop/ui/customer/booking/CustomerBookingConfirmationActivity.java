

package com.example.barber_shop.ui.customer.booking;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerBookingConfirmationActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    public static final String EXTRA_BRANCH_ID =
            CustomerSelectDateTimeActivity.EXTRA_BRANCH_ID;

    public static final String EXTRA_SERVICE_ID =
            CustomerSelectDateTimeActivity.EXTRA_SERVICE_ID;

    public static final String EXTRA_BARBER_ID =
            CustomerSelectDateTimeActivity.EXTRA_BARBER_ID;

    public static final String EXTRA_BOOKING_DATE =
            CustomerSelectDateTimeActivity.EXTRA_BOOKING_DATE;

    public static final String EXTRA_BOOKING_TIME =
            CustomerSelectDateTimeActivity.EXTRA_BOOKING_TIME;

    // ========================================
    // 2. CÁC KEY LƯU TRẠNG THÁI
    // ========================================

    private static final String STATE_BOOKING_CREATED =
            "state_booking_created";

    private static final String STATE_BOOKING_CODE =
            "state_booking_code";

    // ========================================
    // 3. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBookingConfirmation;

    // Thông tin chi nhánh
    private TextView txtConfirmBranchName;
    private TextView txtConfirmBranchAddress;

    // Thông tin dịch vụ và Barber
    private TextView txtConfirmServiceName;
    private TextView txtConfirmServiceDuration;
    private TextView txtConfirmBarberName;

    // Thời gian đặt lịch
    private TextView txtConfirmBookingDate;
    private TextView txtConfirmBookingTime;

    // Thông tin khách hàng
    private TextView txtConfirmCustomerName;

    // Chi phí
    private TextView txtConfirmServicePrice;
    private TextView txtConfirmTotalPrice;

    // Trạng thái xác nhận
    private TextView txtConfirmBookingStatus;

    // Các nút chức năng
    private MaterialButton btnConfirmEditDateTime;
    private MaterialButton btnConfirmBooking;

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

    private String bookingDate;
    private String bookingTime;

    private Calendar selectedDateTime;

    // Ngăn Customer nhấn xác nhận nhiều lần
    private boolean isSubmitting = false;

    // Đánh dấu đã tạo Booking thành công
    private boolean bookingCreated = false;

    // Mã Booking nhận từ Repository
    private String createdBookingCode;

    // Xử lý thao tác lưu trên luồng riêng
    private final ExecutorService bookingExecutor =
            Executors.newSingleThreadExecutor();

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer truy cập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Sử dụng XML màn hình xác nhận
        setContentView(
                R.layout.activity_customer_booking_confirmation
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();

        // Nhận và kiểm tra thông tin Booking
        if (!loadBookingData()) {
            return;
        }

        // Khôi phục trạng thái khi xoay màn hình
        restoreBookingState(savedInstanceState);

        // Hiển thị thông tin Booking
        displayBookingInfo();

        // Thiết lập các sự kiện
        setupListeners();
        setupBackNavigation();

        // ========================================
        // 5.1. KHÔI PHỤC BOOKING ĐÃ TẠO
        // ========================================

        // Nếu Booking đã được lưu trước khi Activity
        // tạo lại, tiếp tục đến màn hình Success.

        if (bookingCreated
                && normalizeValue(createdBookingCode) != null) {

            openBookingSuccess(createdBookingCode);
            return;
        }

        // Cập nhật trạng thái nút xác nhận
        updateConfirmationState();
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

        // Kiểm tra lại thời gian hiện tại
        if (selectedDateTime != null) {
            updateConfirmationState();
        }
    }

    // ========================================
    // 7. LƯU TRẠNG THÁI KHI XOAY MÀN HÌNH
    // ========================================

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Booking đã được tạo hay chưa
        outState.putBoolean(
                STATE_BOOKING_CREATED,
                bookingCreated
        );

        // Mã Booking sau khi lưu thành công
        outState.putString(
                STATE_BOOKING_CODE,
                createdBookingCode
        );
    }

    // ========================================
    // 8. KHÔI PHỤC TRẠNG THÁI
    // ========================================

    private void restoreBookingState(
            Bundle savedInstanceState
    ) {

        if (savedInstanceState == null) {
            return;
        }

        bookingCreated = savedInstanceState.getBoolean(
                STATE_BOOKING_CREATED,
                false
        );

        createdBookingCode = savedInstanceState.getString(
                STATE_BOOKING_CODE
        );
    }

    // ========================================
    // 9. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBookingConfirmationRoot
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
    // 10. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // Toolbar
        toolbarCustomerBookingConfirmation = findViewById(
                R.id.toolbarCustomerBookingConfirmation
        );

        // Thông tin chi nhánh
        txtConfirmBranchName = findViewById(
                R.id.txtConfirmBranchName
        );

        txtConfirmBranchAddress = findViewById(
                R.id.txtConfirmBranchAddress
        );

        // Thông tin dịch vụ
        txtConfirmServiceName = findViewById(
                R.id.txtConfirmServiceName
        );

        txtConfirmServiceDuration = findViewById(
                R.id.txtConfirmServiceDuration
        );

        // Thông tin Barber
        txtConfirmBarberName = findViewById(
                R.id.txtConfirmBarberName
        );

        // Ngày và giờ
        txtConfirmBookingDate = findViewById(
                R.id.txtConfirmBookingDate
        );

        txtConfirmBookingTime = findViewById(
                R.id.txtConfirmBookingTime
        );

        // Thông tin Customer
        txtConfirmCustomerName = findViewById(
                R.id.txtConfirmCustomerName
        );

        // Giá và tổng tiền
        txtConfirmServicePrice = findViewById(
                R.id.txtConfirmServicePrice
        );

        txtConfirmTotalPrice = findViewById(
                R.id.txtConfirmTotalPrice
        );

        // Trạng thái xác nhận
        txtConfirmBookingStatus = findViewById(
                R.id.txtConfirmBookingStatus
        );

        // Nút chỉnh sửa ngày giờ
        btnConfirmEditDateTime = findViewById(
                R.id.btnConfirmEditDateTime
        );

        // Nút xác nhận đặt lịch
        btnConfirmBooking = findViewById(
                R.id.btnConfirmBooking
        );
    }

    // ========================================
    // 11. NHẬN VÀ KIỂM TRA DỮ LIỆU BOOKING
    // ========================================

    private boolean loadBookingData() {

        // ========================================
        // 11.1. NHẬN ID CHI NHÁNH
        // ========================================

        currentBranchId = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BRANCH_ID
                )
        );

        // ========================================
        // 11.2. NHẬN ID DỊCH VỤ
        // ========================================

        currentServiceId = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_SERVICE_ID
                )
        );

        // ========================================
        // 11.3. NHẬN ID BARBER
        // ========================================

        currentBarberId = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BARBER_ID
                )
        );

        // ========================================
        // 11.4. NHẬN NGÀY ĐẶT LỊCH
        // ========================================

        bookingDate = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BOOKING_DATE
                )
        );

        // ========================================
        // 11.5. NHẬN GIỜ ĐẶT LỊCH
        // ========================================

        bookingTime = normalizeValue(
                getIntent().getStringExtra(
                        EXTRA_BOOKING_TIME
                )
        );

        // ========================================
        // 11.6. KIỂM TRA DỮ LIỆU BẮT BUỘC
        // ========================================

        if (currentBranchId == null
                || currentServiceId == null
                || currentBarberId == null
                || bookingDate == null
                || bookingTime == null) {

            showErrorAndFinish(
                    "Thông tin đặt lịch chưa đầy đủ."
            );

            return false;
        }

        // ========================================
        // 11.7. TÌM CÁC ĐỐI TƯỢNG
        // ========================================

        currentBranch = BranchRepository.findById(
                currentBranchId
        );

        currentService = ServiceRepository.findById(
                currentServiceId
        );

        currentBarber = BarberRepository.findById(
                currentBarberId
        );

        if (currentBranch == null
                || currentService == null
                || currentBarber == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin đặt lịch."
            );

            return false;
        }

        // ========================================
        // 11.8. KIỂM TRA BARBER THUỘC CHI NHÁNH
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
        // 11.9. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        if (!isServiceAvailableAtBranch()) {

            showErrorAndFinish(
                    "Dịch vụ không có tại chi nhánh này."
            );

            return false;
        }

        // ========================================
        // 11.10. PHÂN TÍCH NGÀY VÀ GIỜ
        // ========================================

        selectedDateTime = parseBookingDateTime(
                bookingDate,
                bookingTime
        );

        if (selectedDateTime == null) {

            showErrorAndFinish(
                    "Ngày hoặc giờ đặt lịch không hợp lệ."
            );

            return false;
        }

        return true;
    }

    // ========================================
    // 12. CHUẨN HÓA CHUỖI
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
    // 13. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
    // ========================================

    private boolean isServiceAvailableAtBranch() {

        List<SalonService> branchServices =
                ServiceRepository.getServicesForBranch(
                        currentBranchId
                );

        if (branchServices == null) {
            return false;
        }

        for (SalonService service : branchServices) {

            if (service != null
                    && currentServiceId.equals(
                    service.getId()
            )) {

                return true;
            }
        }

        return false;
    }

    // ========================================
    // 14. PHÂN TÍCH NGÀY VÀ GIỜ
    // ========================================

    private Calendar parseBookingDateTime(
            String date,
            String time
    ) {

        if (date == null || time == null) {
            return null;
        }

        // Ngày phải có dạng yyyy-MM-dd
        if (!date.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {
            return null;
        }

        // Giờ phải có dạng HH:mm
        if (!time.matches(
                "([01][0-9]|2[0-3]):[0-5][0-9]"
        )) {
            return null;
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        // Không tự sửa ngày không hợp lệ
        parser.setLenient(false);

        try {

            Date parsedDate = parser.parse(
                    date + " " + time
            );

            if (parsedDate == null) {
                return null;
            }

            Calendar calendar = Calendar.getInstance();

            calendar.setTime(parsedDate);

            return calendar;

        } catch (ParseException e) {

            return null;
        }
    }

    // ========================================
    // 15. HIỂN THỊ THÔNG TIN BOOKING
    // ========================================

    private void displayBookingInfo() {

        // ========================================
        // 15.1. HIỂN THỊ CHI NHÁNH
        // ========================================

        txtConfirmBranchName.setText(
                currentBranch.getName()
        );

        txtConfirmBranchAddress.setText(
                currentBranch.getAddress()
        );

        // ========================================
        // 15.2. HIỂN THỊ DỊCH VỤ
        // ========================================

        txtConfirmServiceName.setText(
                currentService.getName()
        );

        txtConfirmServiceDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentService.getDurationMinutes()
                )
        );

        // ========================================
        // 15.3. HIỂN THỊ BARBER
        // ========================================

        txtConfirmBarberName.setText(
                currentBarber.getName()
        );

        // ========================================
        // 15.4. HIỂN THỊ NGÀY ĐẶT LỊCH
        // ========================================

        SimpleDateFormat displayDateFormatter =
                new SimpleDateFormat(
                        "EEEE, dd/MM/yyyy",
                        new Locale("vi", "VN")
                );

        txtConfirmBookingDate.setText(
                displayDateFormatter.format(
                        selectedDateTime.getTime()
                )
        );

        // ========================================
        // 15.5. HIỂN THỊ GIỜ ĐẶT LỊCH
        // ========================================

        txtConfirmBookingTime.setText(
                bookingTime
        );

        // ========================================
        // 15.6. HIỂN THỊ THÔNG TIN CUSTOMER
        // ========================================

        String customerName = normalizeValue(
                sessionManager.getUserName()
        );

        if (customerName == null) {
            customerName = "Khách hàng";
        }

        txtConfirmCustomerName.setText(
                customerName
        );

        // ========================================
        // 15.7. HIỂN THỊ CHI PHÍ
        // ========================================

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String priceText = formatter.format(
                currentService.getPriceVnd()
        ) + " đ";

        txtConfirmServicePrice.setText(
                priceText
        );

        // Hiện tại chỉ đặt một dịch vụ
        txtConfirmTotalPrice.setText(
                priceText
        );
    }

    // ========================================
    // 16. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 16.1. QUAY VỀ MÀN HÌNH TRƯỚC
        // ========================================

        toolbarCustomerBookingConfirmation
                .setNavigationOnClickListener(
                        v -> handleBackNavigation()
                );

        // ========================================
        // 16.2. CHỈNH SỬA NGÀY GIỜ
        // ========================================

        btnConfirmEditDateTime
                .setOnClickListener(
                        v -> editBookingDateTime()
                );

        // ========================================
        // 16.3. XÁC NHẬN ĐẶT LỊCH
        // ========================================

        btnConfirmBooking
                .setOnClickListener(
                        v -> confirmBooking()
                );
    }

    // ========================================
    // 17. XỬ LÝ BACK VÀ CHỈNH SỬA NGÀY GIỜ
    // ========================================

    private void setupBackNavigation() {

        // Xử lý nút Back vật lý / cử chỉ Back.
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

        // ========================================
        // 17.1. KHÔNG QUAY LẠI KHI ĐANG LƯU
        // ========================================

        if (isSubmitting) {

            Toast.makeText(
                    this,
                    "Đang lưu lịch hẹn, vui lòng không quay lại.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 17.2. ĐÃ LƯU THÀNH CÔNG
        // ========================================

        if (bookingCreated) {

            if (normalizeValue(createdBookingCode) != null) {
                openBookingSuccess(createdBookingCode);
            }

            return;
        }

        // ========================================
        // 17.3. CHƯA LƯU BOOKING
        // ========================================

        finish();
    }

    private void editBookingDateTime() {

        // Quay lại CustomerSelectDateTimeActivity
        // nếu chưa lưu Booking.

        handleBackNavigation();
    }

    // ========================================
    // 18. KIỂM TRA THỜI GIAN ĐẶT LỊCH
    // ========================================

    private String getTimeValidationError() {

        if (selectedDateTime == null
                || currentService == null) {

            return "Thông tin thời gian chưa hợp lệ.";
        }

        // ========================================
        // 18.1. KHÔNG CHỌN THỜI GIAN ĐÃ QUA
        // ========================================

        if (selectedDateTime.getTimeInMillis()
                <= System.currentTimeMillis()) {

            return "Thời gian đã chọn đã qua. "
                    + "Vui lòng chỉnh sửa ngày hoặc giờ.";
        }

        // ========================================
        // 18.2. KIỂM TRA THỜI LƯỢNG
        // ========================================

        int duration =
                currentService.getDurationMinutes();

        if (duration <= 0) {

            return "Thời lượng dịch vụ không hợp lệ.";
        }

        // ========================================
        // 18.3. KIỂM TRA KHUNG GIỜ
        // ========================================

        int hour = selectedDateTime.get(
                Calendar.HOUR_OF_DAY
        );

        int minute = selectedDateTime.get(
                Calendar.MINUTE
        );

        // Các khung giờ minh họa cách nhau 30 phút
        if (minute != 0 && minute != 30) {

            return "Khung giờ đã chọn không hợp lệ.";
        }

        int startMinutes = hour * 60 + minute;

        int endMinutes = startMinutes + duration;

        // Buổi sáng: 08:00 - 12:00
        boolean validMorning =
                startMinutes >= 8 * 60
                        && endMinutes <= 12 * 60;

        // Buổi chiều: 13:00 - 18:00
        boolean validAfternoon =
                startMinutes >= 13 * 60
                        && endMinutes <= 18 * 60;

        if (!validMorning && !validAfternoon) {

            return "Khung giờ không nằm trong "
                    + "thời gian hoạt động minh họa.";
        }

        return null;
    }

    // ========================================
    // 19. CẬP NHẬT TRẠNG THÁI XÁC NHẬN
    // ========================================

    private void updateConfirmationState() {

        // ========================================
        // 19.1. BOOKING ĐÃ ĐƯỢC TẠO
        // ========================================

        if (bookingCreated) {

            btnConfirmBooking.setEnabled(false);

            btnConfirmBooking.setText(
                    "Đã ghi nhận lịch hẹn"
            );

            btnConfirmEditDateTime.setEnabled(false);

            txtConfirmBookingStatus.setText(
                    "Đã lưu lịch hẹn trên thiết bị. "
                            + "Mã: " + createdBookingCode
            );

            return;
        }

        // ========================================
        // 19.2. ĐANG LƯU BOOKING
        // ========================================

        if (isSubmitting) {

            btnConfirmBooking.setEnabled(false);

            btnConfirmBooking.setText(
                    "Đang lưu lịch hẹn..."
            );

            btnConfirmEditDateTime.setEnabled(false);

            txtConfirmBookingStatus.setText(
                    "Hệ thống đang xử lý yêu cầu."
            );

            return;
        }

        // Chưa tạo Booking, cho phép chỉnh sửa
        btnConfirmEditDateTime.setEnabled(true);

        btnConfirmBooking.setText(
                "Xác nhận đặt lịch"
        );

        // ========================================
        // 19.3. KIỂM TRA CUSTOMER ID
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            btnConfirmBooking.setEnabled(false);

            txtConfirmBookingStatus.setText(
                    "Không tìm thấy tài khoản Customer."
            );

            return;
        }

        // ========================================
        // 19.4. KIỂM TRA THỜI GIAN
        // ========================================

        String validationError = getTimeValidationError();

        if (validationError != null) {

            btnConfirmBooking.setEnabled(false);

            txtConfirmBookingStatus.setText(
                    validationError
            );

            return;
        }

        // ========================================
        // 19.5. CHO PHÉP XÁC NHẬN
        // ========================================

        btnConfirmBooking.setEnabled(true);

        txtConfirmBookingStatus.setText(
                "Thông tin hợp lệ. "
                        + "Lịch hẹn sẽ được lưu trên thiết bị."
        );
    }

    // ========================================
    // 20. XÁC NHẬN ĐẶT LỊCH
    // ========================================

    private void confirmBooking() {

        // ========================================
        // 20.1. NGĂN NHẤN NHIỀU LẦN
        // ========================================

        if (isSubmitting || bookingCreated) {
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
        // 20.3. LẤY CUSTOMER ID
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy thông tin Customer.",
                    Toast.LENGTH_SHORT
            ).show();

            updateConfirmationState();
            return;
        }

        // ========================================
        // 20.4. KIỂM TRA LẠI THỜI GIAN
        // ========================================

        String validationError = getTimeValidationError();

        if (validationError != null) {

            Toast.makeText(
                    this,
                    validationError,
                    Toast.LENGTH_LONG
            ).show();

            updateConfirmationState();
            return;
        }

        // ========================================
        // 20.5. CHUYỂN SANG TRẠNG THÁI ĐANG LƯU
        // ========================================

        isSubmitting = true;

        updateConfirmationState();

        // Context của ứng dụng phục vụ lưu dữ liệu
        final android.content.Context appContext =
                getApplicationContext();

        // ========================================
        // 20.6. GỌI REPOSITORY TRÊN LUỒNG RIÊNG
        // ========================================

        bookingExecutor.execute(() -> {

            BookingRepository.BookingResult result;

            try {

                // Tạo Booking và lưu vào SharedPreferences
                result = BookingRepository.createBooking(
                        appContext,
                        customerId,
                        currentBranchId,
                        currentServiceId,
                        currentBarberId,
                        bookingDate,
                        bookingTime
                );

            } catch (RuntimeException e) {

                result = null;
            }

            // Kết quả trả về từ Repository
            final BookingRepository.BookingResult finalResult =
                    result;

            // ========================================
            // 20.7. CẬP NHẬT UI TRÊN MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isSubmitting = false;

                // ========================================
                // 20.8. LƯU BOOKING THÀNH CÔNG
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()
                        && finalResult.getBooking() != null) {

                    Booking createdBooking =
                            finalResult.getBooking();

                    // Lấy mã Booking được Repository tạo
                    createdBookingCode =
                            createdBooking.getBookingCode();

                    // Đánh dấu Booking đã được tạo
                    bookingCreated = true;

                    // Vô hiệu hóa nút xác nhận
                    updateConfirmationState();

                    // ========================================
                    // MỚI: CHUYỂN SANG STT 13
                    // ========================================

                    // Chỉ mở Success sau khi Repository
                    // trả về kết quả lưu thành công.

                    openBookingSuccess(createdBookingCode);

                    return;
                }

                // ========================================
                // 20.9. LƯU BOOKING THẤT BẠI
                // ========================================

                String errorMessage =
                        finalResult != null
                                ? finalResult.getMessage()
                                : "Không thể lưu lịch hẹn. "
                                  + "Vui lòng thử lại.";

                updateConfirmationState();

                // Hiển thị lý do thất bại
                txtConfirmBookingStatus.setText(
                        errorMessage
                );

                // Hiển thị Dialog lỗi
                new MaterialAlertDialogBuilder(
                        CustomerBookingConfirmationActivity.this
                )
                        .setTitle("Đặt lịch chưa thành công")
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
    // 21. ĐIỀU HƯỚNG ĐẾN BOOKING SUCCESS
    // ========================================

    private void openBookingSuccess(
            String bookingCode
    ) {

        // ========================================
        // 21.1. KIỂM TRA MÃ BOOKING
        // ========================================

        String validCode = normalizeValue(bookingCode);

        if (validCode == null) {

            txtConfirmBookingStatus.setText(
                    "Lịch hẹn đã được ghi nhận "
                            + "nhưng thiếu mã Booking."
            );

            return;
        }

        // ========================================
        // 21.2. TẠO INTENT MỞ STT 13
        // ========================================

        Intent intent = new Intent(
                CustomerBookingConfirmationActivity.this,
                CustomerBookingSuccessActivity.class
        );

        // ========================================
        // 21.3. TRUYỀN MÃ BOOKING
        // ========================================

        // Không cần truyền lại thông tin chi nhánh,
        // dịch vụ, Barber, ngày giờ hoặc giá tiền.
        //
        // CustomerBookingSuccessActivity sẽ sử dụng
        // mã Booking để đọc từ BookingRepository.

        intent.putExtra(
                CustomerBookingSuccessActivity.EXTRA_BOOKING_CODE,
                validCode
        );

        // ========================================
        // 21.4. XÓA LUỒNG BOOKING CŨ
        // ========================================

        // Tránh Customer nhấn Back để quay lại
        // màn hình xác nhận và đặt trùng lịch.

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        // ========================================
        // 21.5. MỞ BOOKING SUCCESS
        // ========================================

        startActivity(intent);

        // Đóng màn hình xác nhận
        finish();
    }

    // ========================================
    // 22. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBookingConfirmationActivity.this,
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
    // 23. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
    // ========================================

    private void showErrorAndFinish(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();

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


