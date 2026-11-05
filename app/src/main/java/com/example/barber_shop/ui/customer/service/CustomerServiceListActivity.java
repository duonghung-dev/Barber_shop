
package com.example.barber_shop.ui.customer.service;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
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
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerServiceListActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // ID chi nhánh đã chọn
    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    // ID Barber đã chọn - MỚI
    public static final String EXTRA_BARBER_ID =
            "extra_barber_id";

    // Trạng thái Booking
    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // Từ khóa tìm kiếm ban đầu
    public static final String EXTRA_INITIAL_SEARCH_KEYWORD =
            "extra_initial_search_keyword";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerServices;

    private TextInputEditText edtCustomerServiceSearch;

    private RecyclerView recyclerCustomerServices;

    private TextView txtCustomerServicesSubtitle;
    private TextView txtCustomerServiceCount;
    private TextView txtEmptyCustomerServices;

    private ChipGroup chipGroupCustomerServices;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Adapter riêng của Customer
    private CustomerServiceAdapter serviceAdapter;

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Danh sách dịch vụ gốc
    private final List<SalonService> allServices =
            new ArrayList<>();

    // Danh mục đang được chọn
    private String selectedCategory = "";

    // ID chi nhánh đã chọn
    private String currentBranchId;

    // ID Barber đã chọn - MỚI
    private String currentBarberId;

    // Trạng thái Booking
    private boolean bookingMode;

    // ========================================
    // 4. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Khởi tạo SessionManager
        sessionManager = new SessionManager(this);

        // Chỉ cho phép Customer đã đăng nhập
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Kích hoạt Edge-to-Edge
        EdgeToEdge.enable(this);

        // Sử dụng giao diện riêng của Customer
        setContentView(
                R.layout.activity_customer_service_list
        );

        setupWindowInsets();
        initViews();
        setupRecyclerView();

        // Nhận dữ liệu và tải danh sách dịch vụ
        if (!loadServices()) {
            return;
        }

        // Thiết lập sự kiện
        setupListeners();

        // Áp dụng từ khóa tìm kiếm ban đầu
        applyInitialSearch();
    }

    // ========================================
    // 5. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {
        super.onResume();

        // Không xử lý nếu Activity đang đóng
        if (isFinishing()) {
            return;
        }

        // Kiểm tra phiên đăng nhập Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerServiceListRoot
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
        toolbarCustomerServices = findViewById(
                R.id.toolbarCustomerServices
        );

        // Ô tìm kiếm dịch vụ
        edtCustomerServiceSearch = findViewById(
                R.id.edtCustomerServiceSearch
        );

        // RecyclerView
        recyclerCustomerServices = findViewById(
                R.id.recyclerCustomerServices
        );

        // Mô tả danh sách dịch vụ
        txtCustomerServicesSubtitle = findViewById(
                R.id.txtCustomerServicesSubtitle
        );

        // Số lượng dịch vụ
        txtCustomerServiceCount = findViewById(
                R.id.txtCustomerServiceCount
        );

        // Trạng thái không có kết quả
        txtEmptyCustomerServices = findViewById(
                R.id.txtEmptyCustomerServices
        );

        // Bộ lọc danh mục
        chipGroupCustomerServices = findViewById(
                R.id.chipGroupCustomerServices
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter riêng của Customer
        serviceAdapter = new CustomerServiceAdapter(
                this::openServiceDetail
        );

        // Hiển thị danh sách theo chiều dọc
        recyclerCustomerServices.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter vào RecyclerView
        recyclerCustomerServices.setAdapter(
                serviceAdapter
        );
    }

    // ========================================
    // 9. NHẬN DỮ LIỆU VÀ TẢI DANH SÁCH DỊCH VỤ
    // ========================================

    private boolean loadServices() {

        // Nhận ID chi nhánh
        currentBranchId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BRANCH_ID
                )
        );

        // Nhận ID Barber - MỚI
        currentBarberId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_BARBER_ID
                )
        );

        // Nhận trạng thái Booking
        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // ========================================
        // 9.1. KIỂM TRA BARBER ĐÃ CHỌN
        // ========================================

        // Nếu Customer chọn Barber trước,
        // cần kiểm tra Barber và chi nhánh làm việc.

        if (currentBarberId != null) {

            // Tìm Barber trong Repository
            Barber selectedBarber =
                    BarberRepository.findById(
                            currentBarberId
                    );

            // Kiểm tra Barber tồn tại
            if (selectedBarber == null) {

                showErrorAndFinish(
                        "Không tìm thấy Barber đã chọn."
                );

                return false;
            }

            // Chi nhánh làm việc của Barber
            String barberBranchId = normalizeId(
                    selectedBarber.getBranchId()
            );

            // Kiểm tra dữ liệu chi nhánh
            if (barberBranchId == null) {

                showErrorAndFinish(
                        "Barber chưa có chi nhánh làm việc."
                );

                return false;
            }

            // Nếu đã chọn chi nhánh trước đó,
            // chi nhánh phải trùng với chi nhánh
            // mà Barber đang làm việc.

            if (currentBranchId != null
                    && !currentBranchId.equals(
                    barberBranchId
            )) {

                showErrorAndFinish(
                        "Barber không thuộc chi nhánh đã chọn."
                );

                return false;
            }

            // Nếu chưa có chi nhánh nhưng đã có Barber,
            // tự lấy chi nhánh làm việc của Barber.

            if (currentBranchId == null) {

                currentBranchId = barberBranchId;
            }
        }

        // ========================================
        // 9.2. XÓA DANH SÁCH CŨ
        // ========================================

        allServices.clear();

        // ========================================
        // 9.3. TRƯỜNG HỢP CHƯA CHỌN CHI NHÁNH
        // ========================================

        if (currentBranchId == null) {

            // Mở từ Customer Home hoặc Search,
            // chưa chọn chi nhánh hay Barber.

            // Hiển thị tất cả dịch vụ
            allServices.addAll(
                    ServiceRepository.getSampleServices()
            );

            // Nếu đang đặt lịch
            if (bookingMode) {

                txtCustomerServicesSubtitle.setText(
                        "Chọn dịch vụ để bắt đầu đặt lịch."
                );
            }

        } else {

            // ========================================
            // 9.4. TRƯỜNG HỢP ĐÃ CÓ CHI NHÁNH
            // ========================================

            // Tìm chi nhánh theo ID
            Branch branch = BranchRepository.findById(
                    currentBranchId
            );

            // Kiểm tra chi nhánh tồn tại
            if (branch == null) {

                showErrorAndFinish(
                        "Không tìm thấy chi nhánh."
                );

                return false;
            }

            // ========================================
            // 9.5. CẬP NHẬT TIÊU ĐỀ
            // ========================================

            if (currentBarberId != null) {

                // Customer đã chọn Barber trước
                Barber barber = BarberRepository.findById(
                        currentBarberId
                );

                if (barber != null) {

                    txtCustomerServicesSubtitle.setText(
                            "Chọn dịch vụ để đặt lịch với "
                                    + barber.getName()
                                    + " tại "
                                    + branch.getName()
                    );
                }

            } else if (bookingMode) {

                // Đang đặt lịch tại chi nhánh
                txtCustomerServicesSubtitle.setText(
                        "Chọn dịch vụ để đặt lịch tại "
                                + branch.getName()
                );

            } else {

                // Chỉ đang xem danh sách dịch vụ
                txtCustomerServicesSubtitle.setText(
                        "Dịch vụ tại " + branch.getName()
                );
            }

            // ========================================
            // 9.6. LỌC DỊCH VỤ THEO CHI NHÁNH
            // ========================================

            allServices.addAll(
                    ServiceRepository.getServicesForBranch(
                            currentBranchId
                    )
            );
        }

        // ========================================
        // 9.7. HIỂN THỊ DANH SÁCH BAN ĐẦU
        // ========================================

        updateServiceList(
                new ArrayList<>(allServices)
        );

        return true;
    }

    // ========================================
    // 10. CHUẨN HÓA ID
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
    // 11. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 11.1. QUAY VỀ MÀN HÌNH TRƯỚC
        // ========================================

        toolbarCustomerServices
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 11.2. TÌM KIẾM THEO THỜI GIAN THỰC
        // ========================================

        edtCustomerServiceSearch.addTextChangedListener(
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

                        filterServices();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        // ========================================
        // 11.3. LỌC THEO DANH MỤC
        // ========================================

        chipGroupCustomerServices
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            if (checkedIds.isEmpty()) {
                                return;
                            }

                            int checkedId =
                                    checkedIds.get(0);

                            // Danh mục Cắt tóc
                            if (checkedId
                                    == R.id.chipCustomerHaircut) {

                                selectedCategory = "Cắt tóc";

                                // Danh mục Tạo kiểu
                            } else if (checkedId
                                    == R.id.chipCustomerStyling) {

                                selectedCategory = "Tạo kiểu";

                                // Danh mục Chăm sóc
                            } else if (checkedId
                                    == R.id.chipCustomerCare) {

                                selectedCategory = "Chăm sóc";

                                // Tất cả dịch vụ
                            } else {

                                selectedCategory = "";
                            }

                            // Lọc lại danh sách
                            filterServices();
                        }
                );
    }

    // ========================================
    // 12. NHẬN TỪ KHÓA TÌM KIẾM BAN ĐẦU
    // ========================================

    private void applyInitialSearch() {

        String initialKeyword =
                getIntent().getStringExtra(
                        EXTRA_INITIAL_SEARCH_KEYWORD
                );

        if (initialKeyword != null
                && !initialKeyword.trim().isEmpty()) {

            // Điền từ khóa vào ô tìm kiếm
            edtCustomerServiceSearch.setText(
                    initialKeyword
            );

            // Đặt con trỏ cuối chuỗi
            edtCustomerServiceSearch.setSelection(
                    initialKeyword.length()
            );

            // TextWatcher tự động lọc kết quả
        }
    }

    // ========================================
    // 13. TÌM KIẾM VÀ LỌC DỊCH VỤ
    // ========================================

    private void filterServices() {

        // Lấy từ khóa tìm kiếm
        Editable input =
                edtCustomerServiceSearch.getText();

        String keyword = input == null
                ? ""
                : input.toString().trim();

        // Chuẩn hóa từ khóa
        String normalizedKeyword = normalizeText(
                keyword
        );

        // Danh sách sau khi lọc
        List<SalonService> filtered =
                new ArrayList<>();

        // Duyệt danh sách dịch vụ
        for (SalonService service : allServices) {

            // ========================================
            // 13.1. KIỂM TRA DANH MỤC
            // ========================================

            boolean matchesCategory =
                    selectedCategory.isEmpty()
                            || selectedCategory.equals(
                            service.getCategory()
                    );

            // ========================================
            // 13.2. KIỂM TRA TỪ KHÓA
            // ========================================

            boolean matchesKeyword =
                    normalizeText(service.getName())
                            .contains(normalizedKeyword)
                            || normalizeText(
                            service.getDescription()
                    ).contains(normalizedKeyword);

            // ========================================
            // 13.3. KẾT HỢP HAI ĐIỀU KIỆN
            // ========================================

            if (matchesCategory && matchesKeyword) {

                filtered.add(service);
            }
        }

        // Cập nhật RecyclerView
        updateServiceList(filtered);
    }

    // ========================================
    // 14. CHUẨN HÓA TIẾNG VIỆT
    // ========================================

    private String normalizeText(String text) {

        if (text == null) {
            return "";
        }

        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    // ========================================
    // 15. CẬP NHẬT DANH SÁCH
    // ========================================

    private void updateServiceList(
            List<SalonService> services
    ) {

        // Cập nhật danh sách cho Adapter
        serviceAdapter.submitList(services);

        // Hiển thị số lượng dịch vụ
        txtCustomerServiceCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d dịch vụ",
                        services.size()
                )
        );

        // Hiển thị trạng thái không có kết quả
        txtEmptyCustomerServices.setVisibility(
                services.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN CUSTOMER SERVICE DETAIL
    // ========================================

    private void openServiceDetail(SalonService service) {

        // Kiểm tra dịch vụ được chọn
        if (service == null) {
            return;
        }

        // Kiểm tra Session trước khi điều hướng
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // ========================================
        // 16.1. TẠO INTENT
        // ========================================

        Intent intent = new Intent(
                CustomerServiceListActivity.this,
                CustomerServiceDetailActivity.class
        );

        // ========================================
        // 16.2. TRUYỀN ID DỊCH VỤ
        // ========================================

        intent.putExtra(
                CustomerServiceDetailActivity.EXTRA_SERVICE_ID,
                service.getId()
        );

        // ========================================
        // 16.3. GIỮ ID CHI NHÁNH
        // ========================================

        if (currentBranchId != null) {

            intent.putExtra(
                    CustomerServiceDetailActivity.EXTRA_BRANCH_ID,
                    currentBranchId
            );
        }

        // ========================================
        // 16.4. GIỮ ID BARBER - MỚI
        // ========================================

        if (currentBarberId != null) {

            // Sử dụng key thống nhất "extra_barber_id".
            //
            // CustomerServiceDetailActivity sẽ được
            // cập nhật để đọc key này và chuyển tiếp
            // sang màn hình chọn ngày giờ.

            intent.putExtra(
                    EXTRA_BARBER_ID,
                    currentBarberId
            );
        }

        // ========================================
        // 16.5. GIỮ TRẠNG THÁI BOOKING
        // ========================================

        intent.putExtra(
                CustomerServiceDetailActivity.EXTRA_BOOKING_MODE,
                bookingMode
        );

        // ========================================
        // 16.6. MỞ SERVICE DETAIL
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerServiceListActivity.this,
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
    // 18. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
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

