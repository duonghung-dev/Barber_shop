
package com.example.barber_shop.ui.customer.barber;

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
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerBarberListActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // ID chi nhánh đã chọn
    public static final String EXTRA_BRANCH_ID =
            "extra_branch_id";

    // ID dịch vụ đã chọn
    public static final String EXTRA_SERVICE_ID =
            "extra_service_id";

    // Trạng thái bắt đầu Booking
    public static final String EXTRA_BOOKING_MODE =
            "extra_booking_mode";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerBarbers;

    private TextInputEditText edtCustomerBarberSearch;

    private RecyclerView recyclerCustomerBarbers;

    private TextView txtCustomerBarbersSubtitle;
    private TextView txtCustomerBarberCount;
    private TextView txtEmptyCustomerBarbers;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    private CustomerBarberAdapter barberAdapter;

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Danh sách Barber gốc
    private final List<Barber> allBarbers =
            new ArrayList<>();

    // Thông tin nhận từ màn hình trước
    private String currentBranchId;
    private String currentServiceId;

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

        // Sử dụng layout riêng của Customer
        setContentView(
                R.layout.activity_customer_barber_list
        );

        setupWindowInsets();
        initViews();
        setupRecyclerView();

        // Tải dữ liệu Barber
        if (!loadBarbers()) {
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
                R.id.customerBarberListRoot
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

        toolbarCustomerBarbers = findViewById(
                R.id.toolbarCustomerBarbers
        );

        edtCustomerBarberSearch = findViewById(
                R.id.edtCustomerBarberSearch
        );

        recyclerCustomerBarbers = findViewById(
                R.id.recyclerCustomerBarbers
        );

        txtCustomerBarbersSubtitle = findViewById(
                R.id.txtCustomerBarbersSubtitle
        );

        txtCustomerBarberCount = findViewById(
                R.id.txtCustomerBarberCount
        );

        txtEmptyCustomerBarbers = findViewById(
                R.id.txtEmptyCustomerBarbers
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter riêng cho Customer
        barberAdapter = new CustomerBarberAdapter(
                this::openBarberDetail
        );

        // Hiển thị danh sách theo chiều dọc
        recyclerCustomerBarbers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter vào RecyclerView
        recyclerCustomerBarbers.setAdapter(
                barberAdapter
        );
    }

    // ========================================
    // 9. NHẬN DỮ LIỆU VÀ TẢI DANH SÁCH BARBER
    // ========================================

    private boolean loadBarbers() {

        // Nhận ID chi nhánh nếu có
        currentBranchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        // Nhận ID dịch vụ nếu có
        currentServiceId = getIntent().getStringExtra(
                EXTRA_SERVICE_ID
        );

        // Nhận trạng thái Booking
        bookingMode = getIntent().getBooleanExtra(
                EXTRA_BOOKING_MODE,
                false
        );

        // Chuẩn hóa ID dịch vụ
        if (currentServiceId != null) {

            currentServiceId = currentServiceId.trim();

            if (currentServiceId.isEmpty()) {
                currentServiceId = null;
            }
        }

        // Xóa danh sách cũ
        allBarbers.clear();

        // ========================================
        // TRƯỜNG HỢP 1: CHƯA CHỌN CHI NHÁNH
        // ========================================

        if (currentBranchId == null
                || currentBranchId.trim().isEmpty()) {

            currentBranchId = null;

            // Hiển thị tất cả Barber
            allBarbers.addAll(
                    BarberRepository.getSampleBarbers()
            );

            // Nếu đang trong quy trình Booking
            if (bookingMode) {

                txtCustomerBarbersSubtitle.setText(
                        "Chọn Barber phù hợp để tiếp tục đặt lịch."
                );
            }

        } else {

            // ========================================
            // TRƯỜNG HỢP 2: ĐÃ CHỌN CHI NHÁNH
            // ========================================

            currentBranchId = currentBranchId.trim();

            // Tìm chi nhánh theo ID
            Branch branch = BranchRepository.findById(
                    currentBranchId
            );

            // Kiểm tra chi nhánh tồn tại
            if (branch == null) {

                Toast.makeText(
                        this,
                        "Không tìm thấy chi nhánh.",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
                return false;
            }

            // Cập nhật tiêu đề phù hợp
            if (bookingMode) {

                txtCustomerBarbersSubtitle.setText(
                        "Chọn Barber tại "
                                + branch.getName()
                                + " để tiếp tục đặt lịch."
                );

            } else {

                txtCustomerBarbersSubtitle.setText(
                        "Barber tại " + branch.getName()
                );
            }

            // Lấy Barber thuộc chi nhánh
            allBarbers.addAll(
                    BarberRepository.getBarbersForBranch(
                            currentBranchId
                    )
            );
        }

        // Hiển thị danh sách ban đầu
        updateBarberList(
                new ArrayList<>(allBarbers)
        );

        return true;
    }

    // ========================================
    // 10. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarCustomerBarbers
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // Tìm kiếm Barber theo thời gian thực
        edtCustomerBarberSearch.addTextChangedListener(
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

                        filterBarbers(
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
    // 11. TÌM KIẾM BARBER
    // ========================================

    private void filterBarbers(String keyword) {

        // Chuẩn hóa từ khóa tìm kiếm
        String query = normalizeText(
                keyword == null ? "" : keyword.trim()
        );

        List<Barber> filtered =
                new ArrayList<>();

        // Duyệt qua danh sách Barber
        for (Barber barber : allBarbers) {

            // Chuẩn hóa tên Barber
            String name = normalizeText(
                    barber.getName()
            );

            // Chuẩn hóa chuyên môn
            String specialty = normalizeText(
                    barber.getSpecialty()
            );

            // Tìm chi nhánh làm việc
            Branch branch = BranchRepository.findById(
                    barber.getBranchId()
            );

            // Chuẩn hóa tên chi nhánh
            String branchName = branch != null
                    ? normalizeText(branch.getName())
                    : "";

            // Tìm theo tên, chuyên môn hoặc chi nhánh
            boolean matchesKeyword =
                    name.contains(query)
                            || specialty.contains(query)
                            || branchName.contains(query);

            // Thêm Barber thỏa mãn điều kiện
            if (matchesKeyword) {
                filtered.add(barber);
            }
        }

        // Cập nhật RecyclerView
        updateBarberList(filtered);
    }

    // ========================================
    // 12. CHUẨN HÓA TIẾNG VIỆT
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
    // 13. CẬP NHẬT DANH SÁCH BARBER
    // ========================================

    private void updateBarberList(
            List<Barber> barbers
    ) {

        // Cập nhật dữ liệu cho Adapter
        barberAdapter.submitList(barbers);

        // Hiển thị số lượng kết quả
        txtCustomerBarberCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d Barber (dữ liệu minh họa)",
                        barbers.size()
                )
        );

        // Hiển thị trạng thái không có kết quả
        txtEmptyCustomerBarbers.setVisibility(
                barbers.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 14. ĐIỀU HƯỚNG ĐẾN CUSTOMER BARBER DETAIL
    // ========================================

    private void openBarberDetail(Barber barber) {

        // Kiểm tra dữ liệu Barber được chọn
        if (barber == null) {
            return;
        }

        // Kiểm tra Session trước khi điều hướng
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        // Tạo Intent mở Customer Barber Detail
        Intent intent = new Intent(
                CustomerBarberListActivity.this,
                CustomerBarberDetailActivity.class
        );

        // Truyền ID Barber được chọn
        intent.putExtra(
                CustomerBarberDetailActivity.EXTRA_BARBER_ID,
                barber.getId()
        );

        // Giữ ID chi nhánh nếu đã chọn trước đó
        if (currentBranchId != null) {

            intent.putExtra(
                    CustomerBarberDetailActivity.EXTRA_BRANCH_ID,
                    currentBranchId
            );
        }

        // Giữ ID dịch vụ nếu đã chọn trước đó
        if (currentServiceId != null) {

            intent.putExtra(
                    CustomerBarberDetailActivity.EXTRA_SERVICE_ID,
                    currentServiceId
            );
        }

        // Giữ trạng thái Booking
        intent.putExtra(
                CustomerBarberDetailActivity.EXTRA_BOOKING_MODE,
                bookingMode
        );

        // Mở Customer Barber Detail
        startActivity(intent);
    }

    // ========================================
    // 15. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerBarberListActivity.this,
                LoginActivity.class
        );

        // Xóa các Activity cũ khỏi Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        // Đóng màn hình hiện tại
        finish();
    }
}

