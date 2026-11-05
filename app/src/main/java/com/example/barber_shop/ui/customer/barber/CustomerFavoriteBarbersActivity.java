
package com.example.barber_shop.ui.customer.barber;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.FavoriteBarberRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerFavoriteBarbersActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO CÁC VIEW
    // ========================================

    // Toolbar
    private MaterialToolbar toolbarCustomerFavoriteBarbers;

    // Tổng số Barber yêu thích
    private TextView txtCustomerFavoriteBarbersCount;

    // Danh sách Barber
    private RecyclerView recyclerCustomerFavoriteBarbers;

    // ========================================
    // 2. TRẠNG THÁI KHÔNG CÓ YÊU THÍCH
    // ========================================

    private LinearLayout layoutEmptyCustomerFavoriteBarbers;

    private TextView txtEmptyCustomerFavoriteTitle;
    private TextView txtEmptyCustomerFavoriteMessage;

    private MaterialButton btnCustomerFavoriteExploreBarbers;

    // ========================================
    // 3. TRẠNG THÁI LOADING VÀ ERROR
    // ========================================

    private ProgressBar progressCustomerFavoriteBarbers;

    private LinearLayout layoutErrorCustomerFavoriteBarbers;

    private TextView txtErrorCustomerFavoriteBarbers;

    private MaterialButton btnRetryCustomerFavoriteBarbers;

    // ========================================
    // 4. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Adapter của RecyclerView
    private CustomerFavoriteBarberAdapter favoriteBarberAdapter;

    // Danh sách Barber yêu thích đang hiển thị
    private final List<Barber> favoriteBarbers =
            new ArrayList<>();

    // Executor xử lý đọc, ghi dữ liệu
    private final ExecutorService favoriteExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt các yêu cầu tải dữ liệu
    private int loadRequestVersion = 0;

    // Đánh dấu đang tải danh sách
    private boolean isLoading = false;

    // Đánh dấu đang bỏ yêu thích
    private boolean isRemoving = false;

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ========================================
        // 5.1. KHỞI TẠO SESSION
        // ========================================

        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer truy cập
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 5.2. KÍCH HOẠT EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 5.3. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_customer_favorite_barbers
        );

        // ========================================
        // 5.4. THIẾT LẬP THÀNH PHẦN
        // ========================================

        setupWindowInsets();
        initViews();
        setupRecyclerView();
        setupListeners();
    }

    // ========================================
    // 6. TẢI LẠI DỮ LIỆU KHI QUAY VỀ
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        if (isFinishing()) {
            return;
        }

        // ========================================
        // 6.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 6.2. TRÁNH TẢI KHI ĐANG XÓA
        // ========================================

        if (isRemoving) {
            return;
        }

        // ========================================
        // 6.3. TẢI DANH SÁCH MỚI NHẤT
        // ========================================

        // Khi Customer quay về từ màn hình
        // Chi tiết Barber, danh sách sẽ được
        // cập nhật theo dữ liệu yêu thích mới.

        loadFavoriteBarbers();
    }

    // ========================================
    // 7. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerFavoriteBarbersRoot
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

        toolbarCustomerFavoriteBarbers = findViewById(
                R.id.toolbarCustomerFavoriteBarbers
        );

        // ========================================
        // 8.2. SỐ LƯỢNG BARBER YÊU THÍCH
        // ========================================

        txtCustomerFavoriteBarbersCount = findViewById(
                R.id.txtCustomerFavoriteBarbersCount
        );

        // ========================================
        // 8.3. RECYCLERVIEW
        // ========================================

        recyclerCustomerFavoriteBarbers = findViewById(
                R.id.recyclerCustomerFavoriteBarbers
        );

        // ========================================
        // 8.4. EMPTY STATE
        // ========================================

        layoutEmptyCustomerFavoriteBarbers = findViewById(
                R.id.layoutEmptyCustomerFavoriteBarbers
        );

        txtEmptyCustomerFavoriteTitle = findViewById(
                R.id.txtEmptyCustomerFavoriteTitle
        );

        txtEmptyCustomerFavoriteMessage = findViewById(
                R.id.txtEmptyCustomerFavoriteMessage
        );

        btnCustomerFavoriteExploreBarbers = findViewById(
                R.id.btnCustomerFavoriteExploreBarbers
        );

        // ========================================
        // 8.5. LOADING
        // ========================================

        progressCustomerFavoriteBarbers = findViewById(
                R.id.progressCustomerFavoriteBarbers
        );

        // ========================================
        // 8.6. ERROR STATE
        // ========================================

        layoutErrorCustomerFavoriteBarbers = findViewById(
                R.id.layoutErrorCustomerFavoriteBarbers
        );

        txtErrorCustomerFavoriteBarbers = findViewById(
                R.id.txtErrorCustomerFavoriteBarbers
        );

        btnRetryCustomerFavoriteBarbers = findViewById(
                R.id.btnRetryCustomerFavoriteBarbers
        );
    }

    // ========================================
    // 9. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // ========================================
        // 9.1. TẠO ADAPTER
        // ========================================

        // Adapter gửi hai sự kiện:
        // - onBarberClick()
        // - onRemoveFavorite()

        favoriteBarberAdapter =
                new CustomerFavoriteBarberAdapter(
                        new CustomerFavoriteBarberAdapter
                                .OnFavoriteBarberActionListener() {

                            @Override
                            public void onBarberClick(
                                    Barber barber
                            ) {

                                openBarberDetail(barber);
                            }

                            @Override
                            public void onRemoveFavorite(
                                    Barber barber
                            ) {

                                confirmRemoveFavorite(barber);
                            }
                        }
                );

        // ========================================
        // 9.2. THIẾT LẬP LAYOUT MANAGER
        // ========================================

        recyclerCustomerFavoriteBarbers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // ========================================
        // 9.3. GẮN ADAPTER
        // ========================================

        recyclerCustomerFavoriteBarbers.setAdapter(
                favoriteBarberAdapter
        );
    }

    // ========================================
    // 10. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 10.1. NÚT QUAY LẠI
        // ========================================

        toolbarCustomerFavoriteBarbers
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 10.2. KHÁM PHÁ BARBER
        // ========================================

        btnCustomerFavoriteExploreBarbers
                .setOnClickListener(
                        v -> openBarberList()
                );

        // ========================================
        // 10.3. THỬ TẢI LẠI
        // ========================================

        btnRetryCustomerFavoriteBarbers
                .setOnClickListener(
                        v -> {

                            if (!isLoading && !isRemoving) {

                                loadFavoriteBarbers();
                            }
                        }
                );
    }

    // ========================================
    // 11. ĐỌC DANH SÁCH BARBER YÊU THÍCH
    // ========================================

    private void loadFavoriteBarbers() {

        // ========================================
        // 11.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 11.2. KIỂM TRA THAO TÁC ĐANG XỬ LÝ
        // ========================================

        if (isRemoving) {
            return;
        }

        // ========================================
        // 11.3. LẤY CUSTOMER ID
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
        // 11.4. HIỂN THỊ LOADING
        // ========================================

        isLoading = true;

        showLoading();

        // Phân biệt các lần tải dữ liệu
        final int requestVersion =
                ++loadRequestVersion;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 11.5. ĐỌC DỮ LIỆU TRÊN LUỒNG RIÊNG
        // ========================================

        favoriteExecutor.execute(() -> {

            List<Barber> loadedBarbers =
                    new ArrayList<>();

            String errorMessage = null;

            try {

                // Lấy danh sách Barber ID
                // của Customer đang đăng nhập.

                List<String> favoriteIds =
                        FavoriteBarberRepository
                                .getFavoriteBarberIds(
                                        appContext,
                                        customerId
                                );

                // ========================================
                // 11.6. ĐỌC THÔNG TIN TỪNG BARBER
                // ========================================

                for (String barberId : favoriteIds) {

                    if (barberId == null) {
                        continue;
                    }

                    Barber barber =
                            BarberRepository.findById(
                                    barberId
                            );

                    // Nếu Barber không còn tồn tại
                    // trong dữ liệu hiện tại thì bỏ
                    // qua khi hiển thị, không tự ý
                    // xóa yêu thích đã lưu.

                    if (barber != null) {

                        loadedBarbers.add(barber);
                    }
                }

            } catch (RuntimeException e) {

                errorMessage =
                        "Không thể đọc danh sách Barber yêu thích.";
            }

            final List<Barber> resultBarbers =
                    loadedBarbers;

            final String resultError =
                    errorMessage;

            // ========================================
            // 11.7. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                // Activity đã đóng thì không cập nhật
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // Bỏ qua kết quả đọc cũ
                if (requestVersion != loadRequestVersion) {
                    return;
                }

                // ========================================
                // 11.8. KIỂM TRA LẠI SESSION
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
                // 11.9. XỬ LÝ LỖI
                // ========================================

                isLoading = false;

                if (resultError != null) {

                    showLoadingError(resultError);
                    return;
                }

                // ========================================
                // 11.10. CẬP NHẬT DANH SÁCH
                // ========================================

                favoriteBarbers.clear();

                favoriteBarbers.addAll(
                        resultBarbers
                );

                // Gửi bản sao danh sách sang Adapter
                favoriteBarberAdapter.submitList(
                        new ArrayList<>(favoriteBarbers)
                );

                // ========================================
                // 11.11. CẬP NHẬT GIAO DIỆN
                // ========================================

                updateFavoriteCount();

                showFavoriteContent();
            });
        });
    }

    // ========================================
    // 12. HIỂN THỊ TRẠNG THÁI ĐANG TẢI
    // ========================================

    private void showLoading() {

        // ========================================
        // 12.1. HIỂN THỊ PROGRESSBAR
        // ========================================

        progressCustomerFavoriteBarbers.setVisibility(
                View.VISIBLE
        );

        // ========================================
        // 12.2. ẨN DANH SÁCH
        // ========================================

        recyclerCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 12.3. ẨN EMPTY STATE
        // ========================================

        layoutEmptyCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 12.4. ẨN ERROR STATE
        // ========================================

        layoutErrorCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );
    }

    // ========================================
    // 13. CẬP NHẬT SỐ LƯỢNG YÊU THÍCH
    // ========================================

    private void updateFavoriteCount() {

        txtCustomerFavoriteBarbersCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d Barber",
                        favoriteBarbers.size()
                )
        );
    }

    // ========================================
    // 14. HIỂN THỊ NỘI DUNG
    // ========================================

    private void showFavoriteContent() {

        // ========================================
        // 14.1. DỪNG LOADING
        // ========================================

        progressCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // Ẩn thông báo lỗi
        layoutErrorCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 14.2. KIỂM TRA DANH SÁCH TRỐNG
        // ========================================

        if (favoriteBarbers.isEmpty()) {

            showEmptyState();
            return;
        }

        // ========================================
        // 14.3. HIỂN THỊ RECYCLERVIEW
        // ========================================

        layoutEmptyCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        recyclerCustomerFavoriteBarbers.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 15. HIỂN THỊ TRẠNG THÁI TRỐNG
    // ========================================

    private void showEmptyState() {

        // ========================================
        // 15.1. ẨN DANH SÁCH
        // ========================================

        recyclerCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 15.2. HIỂN THỊ EMPTY STATE
        // ========================================

        layoutEmptyCustomerFavoriteBarbers.setVisibility(
                View.VISIBLE
        );

        // ========================================
        // 15.3. NỘI DUNG THÔNG BÁO
        // ========================================

        txtEmptyCustomerFavoriteTitle.setText(
                "Chưa có Barber yêu thích"
        );

        txtEmptyCustomerFavoriteMessage.setText(
                "Hãy khám phá các Barber và nhấn "
                        + "biểu tượng trái tim để lưu "
                        + "vào danh sách yêu thích."
        );

        // ========================================
        // 15.4. HIỂN THỊ NÚT KHÁM PHÁ BARBER
        // ========================================

        btnCustomerFavoriteExploreBarbers.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 16. HIỂN THỊ LỖI TẢI DỮ LIỆU
    // ========================================

    private void showLoadingError(String message) {

        // ========================================
        // 16.1. KẾT THÚC LOADING
        // ========================================

        isLoading = false;

        progressCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 16.2. XÓA DANH SÁCH CŨ
        // ========================================

        favoriteBarbers.clear();

        favoriteBarberAdapter.submitList(
                new ArrayList<>()
        );

        updateFavoriteCount();

        // ========================================
        // 16.3. ẨN NỘI DUNG VÀ EMPTY STATE
        // ========================================

        recyclerCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        layoutEmptyCustomerFavoriteBarbers.setVisibility(
                View.GONE
        );

        // ========================================
        // 16.4. HIỂN THỊ ERROR STATE
        // ========================================

        layoutErrorCustomerFavoriteBarbers.setVisibility(
                View.VISIBLE
        );

        txtErrorCustomerFavoriteBarbers.setText(
                message
        );
    }

    // ========================================
    // 17. MỞ CHI TIẾT BARBER
    // ========================================

    private void openBarberDetail(Barber barber) {

        // ========================================
        // 17.1. KIỂM TRA THAO TÁC
        // ========================================

        if (isLoading || isRemoving) {
            return;
        }

        // ========================================
        // 17.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 17.3. KIỂM TRA BARBER
        // ========================================

        if (barber == null) {
            return;
        }

        String barberId = normalizeValue(
                barber.getId()
        );

        if (barberId == null) {

            Toast.makeText(
                    this,
                    "Thông tin Barber không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 17.4. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerFavoriteBarbersActivity.this,
                CustomerBarberDetailActivity.class
        );

        // Chỉ truyền Barber ID
        intent.putExtra(
                CustomerBarberDetailActivity.EXTRA_BARBER_ID,
                barberId
        );

        // ========================================
        // 17.5. MỞ MÀN HÌNH CHI TIẾT
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 18. XÁC NHẬN BỎ YÊU THÍCH
    // ========================================

    private void confirmRemoveFavorite(Barber barber) {

        // ========================================
        // 18.1. KIỂM TRA THAO TÁC
        // ========================================

        if (isLoading || isRemoving) {
            return;
        }

        // ========================================
        // 18.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 18.3. KIỂM TRA BARBER
        // ========================================

        if (barber == null) {
            return;
        }

        String barberId = normalizeValue(
                barber.getId()
        );

        if (barberId == null) {

            Toast.makeText(
                    this,
                    "Thông tin Barber không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 18.4. HIỂN THỊ DIALOG XÁC NHẬN
        // ========================================

        String barberName = normalizeValue(
                barber.getName()
        );

        if (barberName == null) {
            barberName = "Barber này";
        }

        String message =
                "Bạn có chắc chắn muốn bỏ "
                        + barberName
                        + " khỏi danh sách yêu thích?";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bỏ Barber yêu thích")
                .setMessage(message)
                .setNegativeButton(
                        "Giữ lại",
                        (dialog, which) ->
                                dialog.dismiss()
                )
                .setPositiveButton(
                        "Bỏ yêu thích",
                        (dialog, which) ->
                                performRemoveFavorite(barberId)
                )
                .show();
    }

    // ========================================
    // 19. THỰC HIỆN BỎ YÊU THÍCH
    // ========================================

    private void performRemoveFavorite(String barberId) {

        // ========================================
        // 19.1. KIỂM TRA THAO TÁC
        // ========================================

        if (isLoading || isRemoving) {
            return;
        }

        // ========================================
        // 19.2. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 19.3. LẤY THÔNG TIN
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        barberId = normalizeValue(barberId);

        if (customerId == null || barberId == null) {

            Toast.makeText(
                    this,
                    "Thông tin yêu thích không hợp lệ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        final String targetBarberId = barberId;

        final Context appContext =
                getApplicationContext();

        // ========================================
        // 19.4. KHÓA THAO TÁC LẶP
        // ========================================

        isRemoving = true;

        // Hiển thị Loading khi đang lưu
        showLoading();

        // ========================================
        // 19.5. GỌI REPOSITORY TRÊN LUỒNG RIÊNG
        // ========================================

        favoriteExecutor.execute(() -> {

            FavoriteBarberRepository.FavoriteResult result;

            try {

                result =
                        FavoriteBarberRepository.removeFavorite(
                                appContext,
                                customerId,
                                targetBarberId
                        );

            } catch (RuntimeException e) {

                result = null;
            }

            final FavoriteBarberRepository.FavoriteResult
                    finalResult = result;

            // ========================================
            // 19.6. CẬP NHẬT MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (isFinishing() || isDestroyed()) {
                    return;
                }

                // ========================================
                // 19.7. KẾT THÚC TRẠNG THÁI ĐANG XÓA
                // ========================================

                isRemoving = false;

                // ========================================
                // 19.8. KIỂM TRA LẠI SESSION
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
                // 19.9. KIỂM TRA KẾT QUẢ
                // ========================================

                if (finalResult != null
                        && finalResult.isSuccess()) {

                    Toast.makeText(
                            this,
                            finalResult.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();

                } else {

                    String errorMessage =
                            finalResult != null
                                    ? normalizeValue(
                                    finalResult.getMessage()
                            )
                                    : null;

                    if (errorMessage == null) {

                        errorMessage =
                                "Không thể bỏ yêu thích. "
                                        + "Vui lòng thử lại.";
                    }

                    Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                    ).show();
                }

                // ========================================
                // 19.10. TẢI LẠI DANH SÁCH
                // ========================================

                // Chỉ Repository thay đổi dữ liệu.
                //
                // Activity đọc lại dữ liệu mới nhất
                // sau khi xử lý xong, tránh xóa item
                // khỏi giao diện trước khi ghi thành công.

                loadFavoriteBarbers();
            });
        });
    }

    // ========================================
    // 20. MỞ DANH SÁCH BARBER
    // ========================================

    private void openBarberList() {

        // ========================================
        // 20.1. KIỂM TRA SESSION
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 20.2. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerFavoriteBarbersActivity.this,
                CustomerBarberListActivity.class
        );

        // Không truyền branchId hoặc bookingMode:
        // Customer đang khám phá tất cả Barber.

        // ========================================
        // 20.3. MỞ DANH SÁCH
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 21. CHUẨN HÓA CHUỖI
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
    // 22. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerFavoriteBarbersActivity.this,
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
    // 23. GIẢI PHÓNG EXECUTOR
    // ========================================

    @Override
    protected void onDestroy() {

        favoriteExecutor.shutdown();

        super.onDestroy();
    }
}
