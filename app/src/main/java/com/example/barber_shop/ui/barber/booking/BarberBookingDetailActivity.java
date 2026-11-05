
package com.example.barber_shop.ui.barber.booking;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BookingRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarberBookingDetailActivity
        extends AppCompatActivity {

    public static final String EXTRA_BOOKING_CODE =
            "extra_booking_code";

    private static final Locale VIETNAM_LOCALE =
            new Locale("vi", "VN");

    private View btnBarberDetailBack;
    private View btnBarberDetailRefresh;

    private NestedScrollView scrollBarberBookingDetail;

    private TextView txtBarberDetailBookingCode;
    private Chip chipBarberDetailStatus;

    private TextView txtBarberDetailCustomerId;

    private TextView txtBarberDetailServiceName;
    private TextView txtBarberDetailBarberName;
    private TextView txtBarberDetailBranchName;

    private TextView txtBarberDetailBookingDate;
    private TextView txtBarberDetailBookingTime;
    private TextView txtBarberDetailDuration;

    private TextView txtBarberDetailTotalPrice;
    private TextView txtBarberDetailStatusMessage;

    private MaterialCardView cardBarberDetailActions;

    private View btnBarberDetailConfirmBooking;
    private View btnBarberDetailCompleteBooking;
    private View btnBarberDetailProposeReschedule;

    private LinearLayout layoutBarberDetailLoading;
    private LinearLayout layoutBarberDetailError;

    private TextView txtBarberDetailError;
    private View btnBarberDetailRetry;

    private SessionManager sessionManager;
    private ExecutorService executorService;

    private String bookingCode;
    private Booking currentBooking;

    private int loadRequestVersion = 0;

    private boolean isRedirectingToLogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!hasValidBarberSession()) {
            openLogin();
            return;
        }

        Intent intent = getIntent();

        if (intent != null) {

            bookingCode = normalizeValue(
                    intent.getStringExtra(
                            EXTRA_BOOKING_CODE
                    )
            );
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_barber_booking_detail
        );

        executorService =
                Executors.newSingleThreadExecutor();

        setupWindowInsets();

        initViews();

        setupListeners();
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (isFinishing() || isRedirectingToLogin) {
            return;
        }

        if (!ensureBarberSession()) {
            return;
        }

        loadBookingDetail();
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
                R.id.barberBookingDetailRoot
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

        btnBarberDetailBack = findViewById(
                R.id.btnBarberDetailBack
        );

        btnBarberDetailRefresh = findViewById(
                R.id.btnBarberDetailRefresh
        );

        scrollBarberBookingDetail = findViewById(
                R.id.scrollBarberBookingDetail
        );

        txtBarberDetailBookingCode = findViewById(
                R.id.txtBarberDetailBookingCode
        );

        chipBarberDetailStatus = findViewById(
                R.id.chipBarberDetailStatus
        );

        txtBarberDetailCustomerId = findViewById(
                R.id.txtBarberDetailCustomerId
        );

        txtBarberDetailServiceName = findViewById(
                R.id.txtBarberDetailServiceName
        );

        txtBarberDetailBarberName = findViewById(
                R.id.txtBarberDetailBarberName
        );

        txtBarberDetailBranchName = findViewById(
                R.id.txtBarberDetailBranchName
        );

        txtBarberDetailBookingDate = findViewById(
                R.id.txtBarberDetailBookingDate
        );

        txtBarberDetailBookingTime = findViewById(
                R.id.txtBarberDetailBookingTime
        );

        txtBarberDetailDuration = findViewById(
                R.id.txtBarberDetailDuration
        );

        txtBarberDetailTotalPrice = findViewById(
                R.id.txtBarberDetailTotalPrice
        );

        txtBarberDetailStatusMessage = findViewById(
                R.id.txtBarberDetailStatusMessage
        );

        cardBarberDetailActions = findViewById(
                R.id.cardBarberDetailActions
        );

        btnBarberDetailConfirmBooking = findViewById(
                R.id.btnBarberDetailConfirmBooking
        );

        btnBarberDetailCompleteBooking = findViewById(
                R.id.btnBarberDetailCompleteBooking
        );

        btnBarberDetailProposeReschedule = findViewById(
                R.id.btnBarberDetailProposeReschedule
        );

        layoutBarberDetailLoading = findViewById(
                R.id.layoutBarberDetailLoading
        );

        layoutBarberDetailError = findViewById(
                R.id.layoutBarberDetailError
        );

        txtBarberDetailError = findViewById(
                R.id.txtBarberDetailError
        );

        btnBarberDetailRetry = findViewById(
                R.id.btnBarberDetailRetry
        );
    }

    private void setupListeners() {

        btnBarberDetailBack.setOnClickListener(
                v -> finish()
        );

        btnBarberDetailRefresh.setOnClickListener(
                v -> loadBookingDetail()
        );

        btnBarberDetailRetry.setOnClickListener(
                v -> loadBookingDetail()
        );

        btnBarberDetailConfirmBooking.setOnClickListener(
                v -> handleConfirmBooking()
        );

        btnBarberDetailCompleteBooking.setOnClickListener(
                v -> handleCompleteBooking()
        );

        btnBarberDetailProposeReschedule.setOnClickListener(
                v -> handleProposeReschedule()
        );
    }

    private void loadBookingDetail() {

        if (!ensureBarberSession()) {
            return;
        }

        if (bookingCode == null) {

            showError(
                    "Không nhận được mã lịch hẹn. "
                            + "Vui lòng quay lại danh sách "
                            + "và chọn một lịch hẹn."
            );

            return;
        }

        String barberId = normalizeValue(
                sessionManager.getUserId()
        );

        if (barberId == null) {

            showError(
                    "Không xác định được Barber hiện tại."
            );

            return;
        }

        if (executorService == null
                || executorService.isShutdown()) {
            return;
        }

        String requestedBarberId = barberId;
        String requestedBookingCode = bookingCode;

        int currentRequestVersion =
                ++loadRequestVersion;

        currentBooking = null;

        showLoading();

        executorService.execute(() -> {

            Booking resultBooking = null;
            String resultError = null;

            try {

                if (BarberRepository.findById(
                        requestedBarberId
                ) == null) {

                    throw new IllegalStateException(
                            "Tài khoản hiện tại chưa được "
                                    + "liên kết với hồ sơ Barber."
                    );
                }

                resultBooking =
                        BookingRepository.findByCode(
                                getApplicationContext(),
                                requestedBookingCode
                        );

                if (resultBooking == null) {

                    throw new IllegalStateException(
                            "Không tìm thấy lịch hẹn "
                                    + "với mã đã cung cấp."
                    );
                }

                if (!requestedBarberId.equals(
                        resultBooking.getBarberId()
                )) {

                    throw new IllegalStateException(
                            "Bạn không có quyền xem "
                                    + "lịch hẹn này."
                    );
                }

            } catch (RuntimeException e) {

                resultError = e.getMessage();

                if (resultError == null
                        || resultError.trim().isEmpty()) {

                    resultError =
                            "Không thể tải thông tin lịch hẹn. "
                                    + "Vui lòng thử lại.";
                }
            }

            Booking finalBooking = resultBooking;
            String finalError = resultError;

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

                String currentBarberId = normalizeValue(
                        sessionManager.getUserId()
                );

                if (!requestedBarberId.equals(
                        currentBarberId
                )) {

                    loadBookingDetail();
                    return;
                }

                if (finalError != null) {

                    showError(finalError);
                    return;
                }

                if (finalBooking == null) {

                    showError(
                            "Không nhận được thông tin lịch hẹn."
                    );

                    return;
                }

                currentBooking = finalBooking;

                displayBookingDetails(
                        finalBooking
                );
            });
        });
    }

    private void displayBookingDetails(
            Booking booking
    ) {

        txtBarberDetailBookingCode.setText(
                safeText(
                        booking.getBookingCode()
                )
        );

        txtBarberDetailCustomerId.setText(
                safeText(
                        booking.getCustomerId()
                )
        );

        txtBarberDetailServiceName.setText(
                safeText(
                        booking.getServiceName()
                )
        );

        txtBarberDetailBarberName.setText(
                safeText(
                        booking.getBarberName()
                )
        );

        txtBarberDetailBranchName.setText(
                safeText(
                        booking.getBranchName()
                )
        );

        txtBarberDetailBookingDate.setText(
                formatBookingDate(
                        booking.getBookingDate()
                )
        );

        txtBarberDetailBookingTime.setText(
                safeText(
                        booking.getBookingTime()
                )
        );

        txtBarberDetailDuration.setText(
                String.format(
                        VIETNAM_LOCALE,
                        "%d phút",
                        booking.getDurationMinutes()
                )
        );

        NumberFormat numberFormat =
                NumberFormat.getNumberInstance(
                        VIETNAM_LOCALE
                );

        txtBarberDetailTotalPrice.setText(
                numberFormat.format(
                        booking.getTotalPrice()
                ) + " đ"
        );

        updateBookingStatus(
                booking
        );

        updateActionButtons(
                booking
        );

        showContent();
    }

    private void updateBookingStatus(
            Booking booking
    ) {

        String status = booking.getStatus();

        int colorResource;

        if (Booking.STATUS_PENDING.equals(status)) {

            colorResource = R.color.status_pending;

        } else if (
                Booking.STATUS_CONFIRMED.equals(status)
        ) {

            colorResource = R.color.status_confirmed;

        } else if (
                Booking.STATUS_COMPLETED.equals(status)
        ) {

            colorResource = R.color.status_completed;

        } else if (
                Booking.STATUS_CANCELLED.equals(status)
        ) {

            colorResource = R.color.status_cancelled;

        } else {

            colorResource = R.color.text_secondary;
        }

        int statusColor = ContextCompat.getColor(
                this,
                colorResource
        );

        int backgroundColor =
                ColorUtils.blendARGB(
                        Color.WHITE,
                        statusColor,
                        0.12f
                );

        chipBarberDetailStatus.setText(
                booking.getStatusDisplayName()
        );

        chipBarberDetailStatus.setTextColor(
                statusColor
        );

        chipBarberDetailStatus.setChipBackgroundColor(
                ColorStateList.valueOf(
                        backgroundColor
                )
        );
    }

    private void updateActionButtons(
            Booking booking
    ) {

        btnBarberDetailConfirmBooking.setVisibility(
                View.GONE
        );

        btnBarberDetailCompleteBooking.setVisibility(
                View.GONE
        );

        btnBarberDetailProposeReschedule.setVisibility(
                View.GONE
        );

        cardBarberDetailActions.setVisibility(
                View.GONE
        );

        String status = booking.getStatus();

        Date bookingStartTime =
                parseBookingDateTime(
                        booking.getBookingDate(),
                        booking.getBookingTime()
                );

        boolean hasValidTime =
                bookingStartTime != null;

        boolean isFutureBooking =
                hasValidTime
                        && bookingStartTime.getTime()
                        > System.currentTimeMillis();

        boolean hasStarted =
                hasValidTime
                        && bookingStartTime.getTime()
                        <= System.currentTimeMillis();

        if (Booking.STATUS_PENDING.equals(status)) {

            if (isFutureBooking) {

                btnBarberDetailConfirmBooking.setVisibility(
                        View.VISIBLE
                );

                btnBarberDetailProposeReschedule.setVisibility(
                        View.VISIBLE
                );

                cardBarberDetailActions.setVisibility(
                        View.VISIBLE
                );

                txtBarberDetailStatusMessage.setText(
                        "Lịch hẹn đang chờ xác nhận. "
                                + "Bạn có thể xác nhận lịch hẹn "
                                + "hoặc đề xuất thay đổi thời gian. "
                                + "Các nghiệp vụ này sẽ được "
                                + "kết nối ở bước tiếp theo."
                );

            } else {

                txtBarberDetailStatusMessage.setText(
                        "Lịch hẹn đang ở trạng thái chờ xác nhận, "
                                + "nhưng thời gian bắt đầu đã đến "
                                + "hoặc dữ liệu thời gian không hợp lệ. "
                                + "Không thể xác nhận lịch hẹn "
                                + "theo quy tắc hiện tại."
                );
            }

            return;
        }

        if (Booking.STATUS_CONFIRMED.equals(status)) {

            if (hasStarted) {

                btnBarberDetailCompleteBooking.setVisibility(
                        View.VISIBLE
                );

                cardBarberDetailActions.setVisibility(
                        View.VISIBLE
                );

                txtBarberDetailStatusMessage.setText(
                        "Lịch hẹn đã được xác nhận và "
                                + "đã đến thời gian bắt đầu. "
                                + "Nghiệp vụ đánh dấu hoàn thành "
                                + "sẽ được kết nối ở bước tiếp theo."
                );

            } else if (isFutureBooking) {

                btnBarberDetailProposeReschedule.setVisibility(
                        View.VISIBLE
                );

                cardBarberDetailActions.setVisibility(
                        View.VISIBLE
                );

                txtBarberDetailStatusMessage.setText(
                        "Lịch hẹn đã được xác nhận. "
                                + "Bạn có thể xem thông tin "
                                + "và đề xuất đổi lịch nếu cần. "
                                + "Chỉ đánh dấu hoàn thành "
                                + "khi lịch hẹn đã bắt đầu."
                );

            } else {

                txtBarberDetailStatusMessage.setText(
                        "Lịch hẹn đã được xác nhận, "
                                + "nhưng dữ liệu thời gian không hợp lệ. "
                                + "Vui lòng kiểm tra lại lịch hẹn."
                );
            }

            return;
        }

        if (Booking.STATUS_COMPLETED.equals(status)) {

            txtBarberDetailStatusMessage.setText(
                    "Dịch vụ đã được đánh dấu hoàn thành. "
                            + "Lịch hẹn này không còn "
                            + "thao tác cập nhật trạng thái."
            );

            return;
        }

        if (Booking.STATUS_CANCELLED.equals(status)) {

            txtBarberDetailStatusMessage.setText(
                    "Lịch hẹn đã bị hủy. "
                            + "Bạn không thể xác nhận, "
                            + "hoàn thành hoặc đề xuất "
                            + "đổi lịch cho Booking này."
            );

            return;
        }

        txtBarberDetailStatusMessage.setText(
                "Trạng thái lịch hẹn không xác định. "
                        + "Không thể thực hiện thao tác."
        );
    }

    private void handleConfirmBooking() {

        if (!ensureBarberSession()) {
            return;
        }

        if (!isCurrentBookingOwnedByBarber()) {
            return;
        }

        if (!Booking.STATUS_PENDING.equals(
                currentBooking.getStatus()
        )) {

            loadBookingDetail();
            return;
        }

        showPendingFeatureDialog(
                "Xác nhận lịch hẹn",
                "Nghiệp vụ xác nhận Booking "
                        + "chưa được kết nối với "
                        + "BookingRepository. "
                        + "Trạng thái lịch hẹn chưa thay đổi."
        );
    }

    private void handleCompleteBooking() {

        if (!ensureBarberSession()) {
            return;
        }

        if (!isCurrentBookingOwnedByBarber()) {
            return;
        }

        if (!Booking.STATUS_CONFIRMED.equals(
                currentBooking.getStatus()
        )) {

            loadBookingDetail();
            return;
        }

        showPendingFeatureDialog(
                "Hoàn thành lịch hẹn",
                "Nghiệp vụ hoàn thành Booking "
                        + "chưa được kết nối với "
                        + "BookingRepository. "
                        + "Trạng thái lịch hẹn chưa thay đổi."
        );
    }

    private void handleProposeReschedule() {

        if (!ensureBarberSession()) {
            return;
        }

        if (!isCurrentBookingOwnedByBarber()) {
            return;
        }

        String status = currentBooking.getStatus();

        boolean canPropose =
                Booking.STATUS_PENDING.equals(status)
                        || Booking.STATUS_CONFIRMED.equals(
                        status
                );

        Date bookingStartTime =
                parseBookingDateTime(
                        currentBooking.getBookingDate(),
                        currentBooking.getBookingTime()
                );

        if (!canPropose
                || bookingStartTime == null
                || bookingStartTime.getTime()
                <= System.currentTimeMillis()) {

            loadBookingDetail();
            return;
        }

        showPendingFeatureDialog(
                "Đề xuất đổi lịch",
                "Chức năng đề xuất đổi lịch "
                        + "sẽ được triển khai tại "
                        + "màn hình Barber Reschedule Request. "
                        + "Lịch hẹn hiện tại chưa thay đổi."
        );
    }

    private void showPendingFeatureDialog(
            String title,
            String message
    ) {

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        "Đã hiểu",
                        (dialog, which) ->
                                dialog.dismiss()
                )
                .show();
    }

    private boolean isCurrentBookingOwnedByBarber() {

        if (currentBooking == null) {

            loadBookingDetail();
            return false;
        }

        String barberId = normalizeValue(
                sessionManager.getUserId()
        );

        if (barberId == null
                || !barberId.equals(
                currentBooking.getBarberId()
        )) {

            showError(
                    "Bạn không có quyền "
                            + "thao tác trên lịch hẹn này."
            );

            return false;
        }

        return true;
    }

    private Date parseBookingDateTime(
            String date,
            String time
    ) {

        if (date == null || time == null) {
            return null;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        formatter.setLenient(false);

        try {

            return formatter.parse(
                    date + " " + time
            );

        } catch (ParseException e) {

            return null;
        }
    }

    private String formatBookingDate(
            String date
    ) {

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

    private String safeText(
            String value
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            return "Chưa cập nhật";
        }

        return value.trim();
    }

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

    private void showLoading() {

        scrollBarberBookingDetail.setVisibility(
                View.GONE
        );

        cardBarberDetailActions.setVisibility(
                View.GONE
        );

        layoutBarberDetailError.setVisibility(
                View.GONE
        );

        layoutBarberDetailLoading.setVisibility(
                View.VISIBLE
        );
    }

    private void showContent() {

        layoutBarberDetailLoading.setVisibility(
                View.GONE
        );

        layoutBarberDetailError.setVisibility(
                View.GONE
        );

        scrollBarberBookingDetail.setVisibility(
                View.VISIBLE
        );
    }

    private void showError(
            String message
    ) {

        currentBooking = null;

        scrollBarberBookingDetail.setVisibility(
                View.GONE
        );

        cardBarberDetailActions.setVisibility(
                View.GONE
        );

        layoutBarberDetailLoading.setVisibility(
                View.GONE
        );

        layoutBarberDetailError.setVisibility(
                View.VISIBLE
        );

        if (message == null
                || message.trim().isEmpty()) {

            message =
                    "Không thể tải thông tin lịch hẹn. "
                            + "Vui lòng thử lại.";
        }

        txtBarberDetailError.setText(
                message
        );
    }

    private boolean hasValidBarberSession() {

        if (sessionManager == null
                || !sessionManager.isBarber()) {

            return false;
        }

        String barberId = normalizeValue(
                sessionManager.getUserId()
        );

        return barberId != null;
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
                BarberBookingDetailActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}
