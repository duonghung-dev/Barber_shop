
package com.example.barber_shop.ui.customer.barber;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
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
import com.example.barber_shop.data.repository.FavoriteBarberRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.branch.CustomerBranchDetailActivity;
import com.example.barber_shop.ui.customer.service.CustomerServiceListActivity;
import com.example.barber_shop.ui.customer.booking.CustomerSelectDateTimeActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerBarberDetailActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    public static final String EXTRA_BARBER_ID =
            "extra_barber_id";

    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBarberDetail;

    private AppCompatImageView imgCustomerDetailBarber;

    private TextView txtCustomerDetailBarberName;
    private TextView txtCustomerDetailBarberSpecialty;
    private TextView txtCustomerDetailBarberRating;
    private TextView txtCustomerDetailBarberExperience;

    private TextView txtCustomerDetailBarberBranchName;
    private TextView txtCustomerDetailBarberBranchAddress;

    private TextView txtCustomerBarberBookingHint;

    private MaterialButton btnCustomerViewBarberBranch;
    private MaterialButton btnCustomerBookWithBarber;

    // ========================================
    // 2.1. NÚT YÊU THÍCH - STT 19 (MỚI)
    // ========================================

    private TextView btnCustomerDetailBarberFavorite;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    private SessionManager sessionManager;

    // Barber hiện tại
    private Barber currentBarber;

    // Chi nhánh Barber đang làm việc
    private Branch currentBranch;

    // Dịch vụ đã chọn, nếu có
    private SalonService currentService;

    private String currentBranchId;
    private String currentServiceId;

    // Trạng thái quy trình Booking
    private boolean bookingMode;

    // ========================================
    // 3.1. DỮ LIỆU YÊU THÍCH - STT 19 (MỚI)
    // ========================================

    // Barber hiện tại có được yêu thích hay không
    private boolean isFavorite = false;

    // Đã đọc thành công trạng thái yêu thích?
    private boolean favoriteStateLoaded = false;

    // Đang thực hiện thêm/bỏ yêu thích?
    private boolean isUpdatingFavorite = false;

    // Executor thực hiện thao tác SharedPreferences
    private final ExecutorService favoriteExecutor =
            Executors.newSingleThreadExecutor();

    // Dùng để loại bỏ kết quả đọc dữ liệu cũ
    private int favoriteRequestVersion = 0;

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

        // Chỉ cho phép Customer đã đăng nhập
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 4.2. EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 4.3. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_customer_barber_detail
        );

        // ========================================
        // 4.4. KHỞI TẠO VIEW
        // ========================================

        setupWindowInsets();
        initViews();

        // ========================================
        // 4.5. ĐỌC THÔNG TIN BARBER
        // ========================================

        if (!loadBarberDetail()) {
            return;
        }

        // ========================================
        // 4.6. THIẾT LẬP SỰ KIỆN
        // ========================================

        setupListeners();

        // Trạng thái yêu thích được tải trong onResume().
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
        // 5.2. CẬP NHẬT YÊU THÍCH - STT 19
        // ========================================

        // Mỗi lần quay về từ màn hình khác,
        // đọc lại trạng thái yêu thích mới nhất.
        //
        // Không tải khi thao tác ghi đang chạy.

        if (currentBarber != null
                && !isUpdatingFavorite) {

            loadFavoriteStatus();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerBarberDetailRoot
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
        toolbarCustomerBarberDetail = findViewById(
                R.id.toolbarCustomerBarberDetail
        );

        // Ảnh đại diện
        imgCustomerDetailBarber = findViewById(
                R.id.imgCustomerDetailBarber
        );

        // Tên Barber
        txtCustomerDetailBarberName = findViewById(
                R.id.txtCustomerDetailBarberName
        );

        // Chuyên môn
        txtCustomerDetailBarberSpecialty = findViewById(
                R.id.txtCustomerDetailBarberSpecialty
        );

        // Đánh giá
        txtCustomerDetailBarberRating = findViewById(
                R.id.txtCustomerDetailBarberRating
        );

        // Kinh nghiệm
        txtCustomerDetailBarberExperience = findViewById(
                R.id.txtCustomerDetailBarberExperience
        );

        // Tên chi nhánh
        txtCustomerDetailBarberBranchName = findViewById(
                R.id.txtCustomerDetailBarberBranchName
        );

        // Địa chỉ chi nhánh
        txtCustomerDetailBarberBranchAddress = findViewById(
                R.id.txtCustomerDetailBarberBranchAddress
        );

        // Nút xem chi tiết chi nhánh
        btnCustomerViewBarberBranch = findViewById(
                R.id.btnCustomerViewBarberBranch
        );

        // Hướng dẫn đặt lịch
        txtCustomerBarberBookingHint = findViewById(
                R.id.txtCustomerBarberBookingHint
        );

        // Nút đặt lịch
        btnCustomerBookWithBarber = findViewById(
                R.id.btnCustomerBookWithBarber
        );

        // ========================================
        // 7.1. NÚT TRÁI TIM - STT 19 (MỚI)
        // ========================================

        btnCustomerDetailBarberFavorite = findViewById(
                R.id.btnCustomerDetailBarberFavorite
        );

        // Mặc định chưa cho thao tác
        // trước khi tải trạng thái yêu thích.
        updateFavoriteButton();
    }

    // ========================================
    // 8. LẤY DỮ LIỆU BARBER
    // ========================================

    private boolean loadBarberDetail() {

        // ========================================
        // 8.1. NHẬN BARBER ID
        // ========================================

        String barberId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BARBER_ID
                )
        );

        if (barberId == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin Barber."
            );

            return false;
        }

        // ========================================
        // 8.2. TÌM BARBER
        // ========================================

        currentBarber = BarberRepository.findById(
                barberId
        );

        if (currentBarber == null) {

            showErrorAndFinish(
                    "Không tìm thấy thông tin Barber."
            );

            return false;
        }

        // ========================================
        // 8.3. NHẬN DỮ LIỆU INTENT
        // ========================================

        String receivedBranchId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BRANCH_ID
                )
        );

        currentServiceId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_SERVICE_ID
                )
        );

        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // ========================================
        // 8.4. LẤY CHI NHÁNH CỦA BARBER
        // ========================================

        String barberBranchId = normalizeId(
                currentBarber.getBranchId()
        );

        // Nếu đã chọn chi nhánh trước đó,
        // Barber phải thuộc đúng chi nhánh ấy.
        if (receivedBranchId != null
                && !receivedBranchId.equals(barberBranchId)) {

            showErrorAndFinish(
                    "Barber không thuộc chi nhánh đã chọn."
            );

            return false;
        }

        currentBranchId = barberBranchId;

        if (currentBranchId != null) {

            currentBranch = BranchRepository.findById(
                    currentBranchId
            );

        } else {

            currentBranch = null;
        }

        // ========================================
        // 8.5. KIỂM TRA DỊCH VỤ ĐÃ CHỌN
        // ========================================

        if (!loadSelectedService()) {
            return false;
        }

        // ========================================
        // 8.6. HIỂN THỊ THÔNG TIN
        // ========================================

        displayBarberInfo();
        updateBookingHint();

        return true;
    }

    // ========================================
    // 9. CHUẨN HÓA ID
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
    // 10. KIỂM TRA DỊCH VỤ ĐÃ CHỌN
    // ========================================

    private boolean loadSelectedService() {

        // ========================================
        // 10.1. CHƯA CHỌN DỊCH VỤ
        // ========================================

        if (currentServiceId == null) {

            currentService = null;
            return true;
        }

        // ========================================
        // 10.2. TÌM DỊCH VỤ
        // ========================================

        currentService = ServiceRepository.findById(
                currentServiceId
        );

        if (currentService == null) {

            showErrorAndFinish(
                    "Không tìm thấy dịch vụ đã chọn."
            );

            return false;
        }

        // ========================================
        // 10.3. KIỂM TRA CHI NHÁNH
        // ========================================

        if (currentBranch == null) {

            showErrorAndFinish(
                    "Không thể xác minh chi nhánh của Barber."
            );

            return false;
        }

        // ========================================
        // 10.4. KIỂM TRA DỊCH VỤ TẠI CHI NHÁNH
        // ========================================

        List<SalonService> branchServices =
                ServiceRepository.getServicesForBranch(
                        currentBranch.getId()
                );

        boolean availableAtBranch = false;

        if (branchServices != null) {

            for (SalonService service : branchServices) {

                if (service != null
                        && currentServiceId.equals(
                        service.getId()
                )) {

                    availableAtBranch = true;
                    break;
                }
            }
        }

        if (!availableAtBranch) {

            showErrorAndFinish(
                    "Dịch vụ đã chọn không có tại chi nhánh này."
            );

            return false;
        }

        // Chỉ xác nhận dịch vụ có tại chi nhánh.
        // Việc xác nhận năng lực Barber theo
        // từng dịch vụ sẽ thực hiện khi tích hợp BE.

        return true;
    }

    // ========================================
    // 11. HIỂN THỊ THÔNG TIN BARBER
    // ========================================

    private void displayBarberInfo() {

        // ========================================
        // 11.1. HỌ TÊN BARBER
        // ========================================

        txtCustomerDetailBarberName.setText(
                currentBarber.getName()
        );

        // ========================================
        // 11.2. CHUYÊN MÔN
        // ========================================

        txtCustomerDetailBarberSpecialty.setText(
                currentBarber.getSpecialty()
        );

        // ========================================
        // 11.3. ĐÁNH GIÁ
        // ========================================

        txtCustomerDetailBarberRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f / 5.0",
                        currentBarber.getRating()
                )
        );

        // ========================================
        // 11.4. KINH NGHIỆM
        // ========================================

        txtCustomerDetailBarberExperience.setText(
                String.format(
                        Locale.getDefault(),
                        "%d năm kinh nghiệm",
                        currentBarber.getExperienceYears()
                )
        );

        // ========================================
        // 11.5. ẢNH ĐẠI DIỆN
        // ========================================

        imgCustomerDetailBarber.setImageResource(
                currentBarber.getImageResId()
        );

        // ========================================
        // 11.6. THÔNG TIN CHI NHÁNH
        // ========================================

        if (currentBranch != null) {

            txtCustomerDetailBarberBranchName.setText(
                    currentBranch.getName()
            );

            txtCustomerDetailBarberBranchAddress.setText(
                    currentBranch.getAddress()
            );

            txtCustomerDetailBarberBranchAddress.setVisibility(
                    View.VISIBLE
            );

            btnCustomerViewBarberBranch.setVisibility(
                    View.VISIBLE
            );

        } else {

            txtCustomerDetailBarberBranchName.setText(
                    "Chi nhánh đang cập nhật"
            );

            txtCustomerDetailBarberBranchAddress.setVisibility(
                    View.GONE
            );

            btnCustomerViewBarberBranch.setVisibility(
                    View.GONE
            );
        }
    }

    // ========================================
    // 12. CẬP NHẬT HƯỚNG DẪN BOOKING
    // ========================================

    private void updateBookingHint() {

        // ========================================
        // 12.1. ĐÃ CHỌN DỊCH VỤ
        // ========================================

        if (currentService != null) {

            txtCustomerBarberBookingHint.setText(
                    "Dịch vụ đã chọn: "
                            + currentService.getName()
            );

            // ========================================
            // 12.2. ĐANG BOOKING, CHƯA CHỌN DỊCH VỤ
            // ========================================

        } else if (bookingMode) {

            txtCustomerBarberBookingHint.setText(
                    "Tiếp tục chọn dịch vụ để đặt lịch."
            );

            // ========================================
            // 12.3. CHỈ ĐANG XEM BARBER
            // ========================================

        } else {

            txtCustomerBarberBookingHint.setText(
                    "Chọn dịch vụ để đặt lịch với Barber này."
            );
        }
    }

    // ========================================
    // 13. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 13.1. QUAY LẠI
        // ========================================

        toolbarCustomerBarberDetail.setNavigationOnClickListener(
                v -> finish()
        );

        // ========================================
        // 13.2. XEM CHI TIẾT CHI NHÁNH
        // ========================================

        btnCustomerViewBarberBranch.setOnClickListener(
                v -> openBranchDetail()
        );

        // ========================================
        // 13.3. ĐẶT LỊCH
        // ========================================

        btnCustomerBookWithBarber.setOnClickListener(
                v -> startBooking()
        );

        // ========================================
        // 13.4. THÊM/BỎ YÊU THÍCH - STT 19 (MỚI)
        // ========================================

        btnCustomerDetailBarberFavorite.setOnClickListener(
                v -> toggleFavorite()
        );
    }

    // ========================================
    // 14. ĐỌC TRẠNG THÁI YÊU THÍCH - STT 19
    // ========================================

    private void loadFavoriteStatus() {

        // ========================================
        // 14.1. KIỂM TRA ĐIỀU KIỆN
        // ========================================

        if (currentBarber == null || isUpdatingFavorite) {
            return;
        }

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        final String customerId = normalizeId(
                sessionManager.getUserId()
        );

        final String barberId = normalizeId(
                currentBarber.getId()
        );

        if (customerId == null || barberId == null) {

            favoriteStateLoaded = false;
            updateFavoriteButton();
            return;
        }

        // ========================================
        // 14.2. KHÓA NÚT KHI ĐANG ĐỌC
        // ========================================

        favoriteStateLoaded = false;
        updateFavoriteButton();

        final int requestVersion =
                ++favoriteRequestVersion;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 14.3. ĐỌC TRÊN LUỒNG RIÊNG
        // ========================================

        favoriteExecutor.execute(() -> {

            Boolean loadedFavoriteState = null;

            try {

                loadedFavoriteState =
                        FavoriteBarberRepository.isFavorite(
                                appContext,
                                customerId,
                                barberId
                        );

            } catch (RuntimeException e) {

                // Không ghi đè trạng thái hiện có
                // nếu dữ liệu yêu thích bị lỗi.
            }

            final Boolean result = loadedFavoriteState;

            // ========================================
            // 14.4. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả đã lỗi thời
                if (requestVersion != favoriteRequestVersion) {
                    return;
                }

                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeId(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                // ========================================
                // 14.5. XỬ LÝ KẾT QUẢ
                // ========================================

                if (result == null) {

                    favoriteStateLoaded = false;
                    updateFavoriteButton();

                    Toast.makeText(
                            this,
                            "Không thể tải trạng thái yêu thích.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                isFavorite = result;
                favoriteStateLoaded = true;

                updateFavoriteButton();
            });
        });
    }

    // ========================================
    // 15. CẬP NHẬT NÚT TRÁI TIM - STT 19
    // ========================================

    private void updateFavoriteButton() {

        if (btnCustomerDetailBarberFavorite == null) {
            return;
        }

        // ========================================
        // 15.1. XÁC ĐỊNH TRẠNG THÁI
        // ========================================

        if (isFavorite) {

            // Đã yêu thích: trái tim đặc màu đỏ
            btnCustomerDetailBarberFavorite.setText("♥");

            btnCustomerDetailBarberFavorite.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.status_cancelled
                    )
            );

            btnCustomerDetailBarberFavorite
                    .setContentDescription(
                            "Bỏ Barber khỏi yêu thích"
                    );

        } else {

            // Chưa yêu thích: trái tim rỗng màu nâu
            btnCustomerDetailBarberFavorite.setText("♡");

            btnCustomerDetailBarberFavorite.setTextColor(
                    ContextCompat.getColor(
                            this,
                            R.color.primary
                    )
            );

            btnCustomerDetailBarberFavorite
                    .setContentDescription(
                            "Thêm Barber vào yêu thích"
                    );
        }

        // ========================================
        // 15.2. BẬT/TẮT TƯƠNG TÁC
        // ========================================

        boolean canClick =
                currentBarber != null
                        && favoriteStateLoaded
                        && !isUpdatingFavorite
                        && sessionManager.isCustomer();

        btnCustomerDetailBarberFavorite.setEnabled(
                canClick
        );

        // Phân biệt trạng thái nút đang bị khóa
        btnCustomerDetailBarberFavorite.setAlpha(
                canClick ? 1.0f : 0.5f
        );
    }

    // ========================================
    // 16. THÊM/BỎ YÊU THÍCH - STT 19
    // ========================================

    private void toggleFavorite() {

        // ========================================
        // 16.1. KIỂM TRA THAO TÁC
        // ========================================

        if (currentBarber == null
                || !favoriteStateLoaded
                || isUpdatingFavorite) {

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
        // 16.3. LẤY CUSTOMER VÀ BARBER ID
        // ========================================

        final String customerId = normalizeId(
                sessionManager.getUserId()
        );

        final String barberId = normalizeId(
                currentBarber.getId()
        );

        if (customerId == null || barberId == null) {

            Toast.makeText(
                    this,
                    "Thông tin Customer hoặc Barber không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.4. KHÓA THAO TÁC LẶP
        // ========================================

        isUpdatingFavorite = true;

        // Làm vô hiệu các kết quả tải cũ
        ++favoriteRequestVersion;

        updateFavoriteButton();

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 16.5. GỌI REPOSITORY TRÊN LUỒNG RIÊNG
        // ========================================

        favoriteExecutor.execute(() -> {

            FavoriteBarberRepository.FavoriteResult result;

            try {

                result =
                        FavoriteBarberRepository.toggleFavorite(
                                appContext,
                                customerId,
                                barberId
                        );

            } catch (RuntimeException e) {

                result = null;
            }

            final FavoriteBarberRepository.FavoriteResult
                    finalResult = result;

            // ========================================
            // 16.6. CẬP NHẬT GIAO DIỆN
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                isUpdatingFavorite = false;

                // Kiểm tra lại Customer sau khi ghi
                if (!sessionManager.isCustomer()
                        || !customerId.equals(
                        normalizeId(
                                sessionManager.getUserId()
                        )
                )) {

                    openLogin();
                    return;
                }

                // ========================================
                // 16.7. GHI THÀNH CÔNG
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()) {

                    // Lấy trạng thái mới từ Repository
                    isFavorite = finalResult.isFavorite();

                    favoriteStateLoaded = true;

                    updateFavoriteButton();

                    Toast.makeText(
                            this,
                            finalResult.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                // ========================================
                // 16.8. GHI THẤT BẠI
                // ========================================

                String message = finalResult != null
                        ? finalResult.getMessage()
                        : null;

                if (message == null
                        || message.trim().isEmpty()) {

                    message =
                            "Không thể cập nhật yêu thích. "
                                    + "Vui lòng thử lại.";
                }

                Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                ).show();

                // Đọc lại trạng thái thật từ Repository,
                // không tự ý đổi hình trái tim.
                loadFavoriteStatus();
            });
        });
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN CHI TIẾT CHI NHÁNH
    // ========================================

    private void openBranchDetail() {

        if (currentBranch == null) {
            return;
        }

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        Intent intent = new Intent(
                CustomerBarberDetailActivity.this,
                CustomerBranchDetailActivity.class
        );

        intent.putExtra(
                CustomerBranchDetailActivity.EXTRA_BRANCH_ID,
                currentBranch.getId()
        );

        startActivity(intent);
    }

    // ========================================
    // 18. BẮT ĐẦU QUY TRÌNH ĐẶT LỊCH
    // ========================================

    private void startBooking() {

        // ========================================
        // 18.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 18.2. KIỂM TRA BARBER
        // ========================================

        if (currentBarber == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy Barber đã chọn.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Đánh dấu bắt đầu Booking
        bookingMode = true;

        // ========================================
        // 18.3. KIỂM TRA CHI NHÁNH
        // ========================================

        if (currentBranch == null) {

            Toast.makeText(
                    this,
                    "Chưa có thông tin chi nhánh "
                            + "để đặt lịch với Barber này.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        // ========================================
        // 18.4. CHƯA CHỌN DỊCH VỤ
        // ========================================

        if (currentService == null) {

            openServiceSelection();
            return;
        }

        // ========================================
        // 18.5. ĐÃ CHỌN ĐỦ THÔNG TIN
        // ========================================

        openSelectDateTime();
    }

    // ========================================
    // 19. ĐIỀU HƯỚNG ĐẾN CHỌN DỊCH VỤ
    // ========================================

    private void openServiceSelection() {

        Intent intent = new Intent(
                CustomerBarberDetailActivity.this,
                CustomerServiceListActivity.class
        );

        // ========================================
        // 19.1. GIỮ CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 19.2. GIỮ BARBER ĐÃ CHỌN
        // ========================================

        // Cùng key "extra_barber_id".
        // CustomerServiceListActivity sẽ tiếp nhận
        // và chuyển tiếp trong quy trình Booking.

        intent.putExtra(
                EXTRA_BARBER_ID,
                currentBarber.getId()
        );

        // ========================================
        // 19.3. ĐÁNH DẤU BOOKING MODE
        // ========================================

        intent.putExtra(
                CustomerServiceListActivity.EXTRA_BOOKING_MODE,
                true
        );

        startActivity(intent);
    }

    // ========================================
    // 20. ĐIỀU HƯỚNG ĐẾN CHỌN NGÀY VÀ GIỜ
    // ========================================

    private void openSelectDateTime() {

        // ========================================
        // 20.1. KIỂM TRA ĐỦ THÔNG TIN
        // ========================================

        if (currentBranchId == null
                || currentServiceId == null
                || currentBarber == null
                || currentService == null) {

            Toast.makeText(
                    this,
                    "Vui lòng chọn đầy đủ chi nhánh, "
                            + "dịch vụ và Barber.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 20.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerBarberDetailActivity.this,
                CustomerSelectDateTimeActivity.class
        );

        // ========================================
        // 20.3. TRUYỀN CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_BRANCH_ID,
                currentBranchId
        );

        // ========================================
        // 20.4. TRUYỀN DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_SERVICE_ID,
                currentServiceId
        );

        // ========================================
        // 20.5. TRUYỀN BARBER
        // ========================================

        intent.putExtra(
                CustomerSelectDateTimeActivity.EXTRA_BARBER_ID,
                currentBarber.getId()
        );

        startActivity(intent);
    }

    // ========================================
    // 21. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBarberDetailActivity.this,
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
    // 22. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
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
    // 23. GIẢI PHÓNG EXECUTOR - STT 19
    // ========================================

    @Override
    protected void onDestroy() {

        favoriteExecutor.shutdown();

        super.onDestroy();
    }
}


