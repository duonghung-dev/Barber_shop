
package com.example.barber_shop.ui.customer.branch;

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
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerBranchListActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // ID dịch vụ đã chọn
    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    // Trạng thái Booking
    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBranches;

    private TextInputEditText edtCustomerBranchSearch;

    private RecyclerView recyclerCustomerBranches;

    private TextView txtEmptyCustomerBranches;
    private TextView txtCustomerBranchCount;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Adapter riêng của Customer
    private CustomerBranchAdapter branchAdapter;

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Danh sách chi nhánh gốc sau khi lọc
    // theo dịch vụ, nếu đang Booking
    private final List<Branch> allBranches =
            new ArrayList<>();

    // ID dịch vụ đã chọn
    private String currentServiceId;

    // Dịch vụ đang được đặt
    private SalonService currentService;

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
                R.layout.activity_customer_branch_list
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();
        setupRecyclerView();

        // Nhận dữ liệu và tải danh sách chi nhánh
        if (!loadBranches()) {
            return;
        }

        // Thiết lập các sự kiện
        setupListeners();
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
                R.id.customerBranchListRoot
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
    // 7. ÁNH XẠ CÁC VIEW
    // ========================================

    private void initViews() {

        // Toolbar
        toolbarCustomerBranches = findViewById(
                R.id.toolbarCustomerBranches
        );

        // Ô tìm kiếm chi nhánh
        edtCustomerBranchSearch = findViewById(
                R.id.edtCustomerBranchSearch
        );

        // RecyclerView
        recyclerCustomerBranches = findViewById(
                R.id.recyclerCustomerBranches
        );

        // Trạng thái không có kết quả
        txtEmptyCustomerBranches = findViewById(
                R.id.txtEmptyCustomerBranches
        );

        // Số lượng chi nhánh
        txtCustomerBranchCount = findViewById(
                R.id.txtCustomerBranchCount
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter riêng cho Customer
        branchAdapter = new CustomerBranchAdapter(
                this::openBranchDetail
        );

        // Hiển thị danh sách theo chiều dọc
        recyclerCustomerBranches.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter vào RecyclerView
        recyclerCustomerBranches.setAdapter(
                branchAdapter
        );
    }

    // ========================================
    // 9. NHẬN DỮ LIỆU VÀ TẢI DANH SÁCH CHI NHÁNH
    // ========================================

    private boolean loadBranches() {

        // ========================================
        // 9.1. NHẬN ID DỊCH VỤ
        // ========================================

        currentServiceId = normalizeId(
                getIntent().getStringExtra(
                        EXTRA_SERVICE_ID
                )
        );

        // ========================================
        // 9.2. NHẬN BOOKING MODE
        // ========================================

        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // ========================================
        // 9.3. KIỂM TRA DỊCH VỤ ĐÃ CHỌN
        // ========================================

        if (currentServiceId != null) {

            // Tra cứu dịch vụ
            currentService = ServiceRepository.findById(
                    currentServiceId
            );

            // Dịch vụ không tồn tại
            if (currentService == null) {

                showErrorAndFinish(
                        "Không tìm thấy dịch vụ đã chọn."
                );

                return false;
            }

        } else {

            currentService = null;
        }

        // ========================================
        // 9.4. XÓA DANH SÁCH CŨ
        // ========================================

        allBranches.clear();

        // Lấy tất cả chi nhánh từ Repository
        List<Branch> sampleBranches =
                BranchRepository.getSampleBranches();

        // ========================================
        // 9.5. TRƯỜNG HỢP CHƯA CHỌN DỊCH VỤ
        // ========================================

        if (currentService == null || !bookingMode) {

            // Customer mở từ Home/Search:
            // Hiển thị tất cả chi nhánh.
            //
            // Nếu có serviceId nhưng không trong
            // Booking Mode, vẫn hiển thị tất cả.

            allBranches.addAll(sampleBranches);

        } else {

            // ========================================
            // 9.6. ĐÃ CHỌN DỊCH VỤ VÀ ĐANG BOOKING
            // ========================================

            // Chỉ hiển thị những chi nhánh
            // đang cung cấp dịch vụ được chọn.

            for (Branch branch : sampleBranches) {

                if (branch == null) {
                    continue;
                }

                // Kiểm tra chi nhánh có dịch vụ không
                if (isServiceAvailableAtBranch(
                        branch.getId(),
                        currentServiceId
                )) {

                    allBranches.add(branch);
                }
            }
        }

        // ========================================
        // 9.7. CẬP NHẬT THÔNG TIN TOOLBAR
        // ========================================

        if (bookingMode && currentService != null) {

            // Customer đã chọn dịch vụ trước
            toolbarCustomerBranches.setSubtitle(
                    "Chọn chi nhánh cho "
                            + currentService.getName()
            );

        } else if (bookingMode) {

            // Booking chưa chọn dịch vụ
            toolbarCustomerBranches.setSubtitle(
                    "Chọn chi nhánh để tiếp tục đặt lịch"
            );

        } else {

            // Chỉ đang xem danh sách chi nhánh
            toolbarCustomerBranches.setSubtitle(null);
        }

        // ========================================
        // 9.8. HIỂN THỊ DANH SÁCH BAN ĐẦU
        // ========================================

        updateBranchList(
                new ArrayList<>(allBranches)
        );

        // Nếu dịch vụ đã chọn không có ở chi nhánh nào
        if (bookingMode
                && currentService != null
                && allBranches.isEmpty()) {

            txtEmptyCustomerBranches.setText(
                    "Hiện chưa có chi nhánh cung cấp dịch vụ "
                            + currentService.getName() + "."
            );

        } else {

            txtEmptyCustomerBranches.setText(
                    "Không tìm thấy chi nhánh phù hợp."
            );
        }

        return true;
    }

    // ========================================
    // 10. KIỂM TRA DỊCH VỤ CÓ TẠI CHI NHÁNH
    // ========================================

    private boolean isServiceAvailableAtBranch(
            String branchId,
            String serviceId
    ) {

        // Kiểm tra ID đầu vào
        if (branchId == null || serviceId == null) {
            return false;
        }

        // Lấy các dịch vụ thuộc chi nhánh
        List<SalonService> branchServices =
                ServiceRepository.getServicesForBranch(
                        branchId
                );

        // Không có dữ liệu dịch vụ
        if (branchServices == null) {
            return false;
        }

        // Duyệt danh sách dịch vụ
        for (SalonService service : branchServices) {

            if (service != null
                    && serviceId.equals(
                    service.getId()
            )) {

                return true;
            }
        }

        return false;
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
    // 12. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 12.1. QUAY VỀ MÀN HÌNH TRƯỚC
        // ========================================

        toolbarCustomerBranches
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 12.2. TÌM KIẾM THEO THỜI GIAN THỰC
        // ========================================

        edtCustomerBranchSearch.addTextChangedListener(
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

                        filterBranches(
                                s == null ? "" : s.toString()
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );
    }

    // ========================================
    // 13. TÌM KIẾM CHI NHÁNH
    // ========================================

    private void filterBranches(String keyword) {

        // Chuẩn hóa từ khóa tìm kiếm
        String query = normalizeText(
                keyword == null ? "" : keyword.trim()
        );

        // Danh sách sau khi tìm kiếm
        List<Branch> filteredBranches =
                new ArrayList<>();

        // Duyệt danh sách chi nhánh gốc
        for (Branch branch : allBranches) {

            // Chuẩn hóa tên chi nhánh
            String name = normalizeText(
                    branch.getName()
            );

            // Chuẩn hóa địa chỉ
            String address = normalizeText(
                    branch.getAddress()
            );

            // Tìm theo tên hoặc địa chỉ
            boolean matchesKeyword =
                    name.contains(query)
                            || address.contains(query);

            // Thêm chi nhánh phù hợp
            if (matchesKeyword) {

                filteredBranches.add(branch);
            }
        }

        // Cập nhật RecyclerView
        updateBranchList(filteredBranches);
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

    private void updateBranchList(
            List<Branch> branches
    ) {

        // Cập nhật dữ liệu cho Adapter
        branchAdapter.submitList(branches);

        // Hiển thị số lượng chi nhánh
        txtCustomerBranchCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d chi nhánh (dữ liệu minh họa)",
                        branches.size()
                )
        );

        // Hiển thị trạng thái không có kết quả
        txtEmptyCustomerBranches.setVisibility(
                branches.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN CUSTOMER BRANCH DETAIL
    // ========================================

    private void openBranchDetail(Branch branch) {

        // Kiểm tra dữ liệu chi nhánh
        if (branch == null) {
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
                CustomerBranchListActivity.this,
                CustomerBranchDetailActivity.class
        );

        // ========================================
        // 16.2. TRUYỀN ID CHI NHÁNH
        // ========================================

        intent.putExtra(
                CustomerBranchDetailActivity.EXTRA_BRANCH_ID,
                branch.getId()
        );

        // ========================================
        // 16.3. GIỮ ID DỊCH VỤ ĐÃ CHỌN
        // ========================================

        if (currentServiceId != null) {

            // CustomerBranchDetailActivity
            // sẽ được cập nhật để nhận ID này.

            intent.putExtra(
                    EXTRA_SERVICE_ID,
                    currentServiceId
            );
        }

        // ========================================
        // 16.4. GIỮ TRẠNG THÁI BOOKING
        // ========================================

        intent.putExtra(
                EXTRA_BOOKING_MODE,
                bookingMode
        );

        // ========================================
        // 16.5. MỞ BRANCH DETAIL
        // ========================================

        startActivity(intent);
    }

    // ========================================
    // 17. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBranchListActivity.this,
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


