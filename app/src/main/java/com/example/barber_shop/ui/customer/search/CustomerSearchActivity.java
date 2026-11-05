
package com.example.barber_shop.ui.customer.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.SearchResult;
import com.example.barber_shop.data.repository.SearchRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.customer.barber.CustomerBarberDetailActivity;
import com.example.barber_shop.ui.customer.branch.CustomerBranchDetailActivity;
import com.example.barber_shop.ui.customer.service.CustomerServiceDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomerSearchActivity
        extends AppCompatActivity {

    // ========================================
    // 1. CÁC KEY NHẬN DỮ LIỆU TỪ INTENT
    // ========================================

    // Từ khóa tìm kiếm ban đầu (nếu có)
    public static final String EXTRA_INITIAL_SEARCH_KEYWORD =
            "extra_initial_search_keyword";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    private MaterialToolbar toolbarCustomerSearch;

    private TextInputEditText edtCustomerGlobalSearch;

    private ChipGroup chipGroupCustomerSearch;

    private RecyclerView recyclerCustomerSearchResults;

    private TextView txtCustomerSearchResultCount;

    private TextView txtEmptyCustomerSearch;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Adapter riêng dành cho Customer
    private CustomerSearchAdapter searchAdapter;

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Danh sách kết quả tìm kiếm gốc
    private final List<SearchResult> allResults =
            new ArrayList<>();

    // null nghĩa là không lọc theo loại
    private SearchResult.Type selectedType = null;

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

        // Sử dụng XML riêng dành cho Customer
        setContentView(
                R.layout.activity_customer_search
        );

        // Thiết lập giao diện
        setupWindowInsets();
        initViews();
        setupRecyclerView();

        // Nạp dữ liệu tìm kiếm
        loadSearchData();

        // Thiết lập sự kiện
        setupListeners();

        // Áp dụng từ khóa ban đầu nếu có
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

        // Kiểm tra Session Customer
        if (!sessionManager.isCustomer()) {
            openLogin();
        }
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerSearchRoot
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
        toolbarCustomerSearch = findViewById(
                R.id.toolbarCustomerSearch
        );

        // Ô nhập từ khóa tìm kiếm
        edtCustomerGlobalSearch = findViewById(
                R.id.edtCustomerGlobalSearch
        );

        // Nhóm bộ lọc
        chipGroupCustomerSearch = findViewById(
                R.id.chipGroupCustomerSearch
        );

        // RecyclerView
        recyclerCustomerSearchResults = findViewById(
                R.id.recyclerCustomerSearchResults
        );

        // Số lượng kết quả
        txtCustomerSearchResultCount = findViewById(
                R.id.txtCustomerSearchResultCount
        );

        // Thông báo khi không tìm thấy kết quả
        txtEmptyCustomerSearch = findViewById(
                R.id.txtEmptyCustomerSearch
        );
    }

    // ========================================
    // 8. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter riêng cho Customer
        searchAdapter = new CustomerSearchAdapter(
                this::openSearchResult
        );

        // Hiển thị kết quả theo chiều dọc
        recyclerCustomerSearchResults.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter vào RecyclerView
        recyclerCustomerSearchResults.setAdapter(
                searchAdapter
        );
    }

    // ========================================
    // 9. NẠP DỮ LIỆU TÌM KIẾM
    // ========================================

    private void loadSearchData() {

        // Xóa dữ liệu cũ
        allResults.clear();

        // Lấy dữ liệu dùng chung với Guest
        allResults.addAll(
                SearchRepository.getAllResults()
        );

        // Mặc định chọn bộ lọc "Tất cả"
        selectedType = null;

        chipGroupCustomerSearch.check(
                R.id.chipCustomerSearchAll
        );

        // Hiển thị toàn bộ kết quả
        updateSearchResults(
                new ArrayList<>(allResults)
        );
    }

    // ========================================
    // 10. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarCustomerSearch
                .setNavigationOnClickListener(
                        v -> finish()
                );

        // ========================================
        // 10.1. TÌM KIẾM THEO THỜI GIAN THỰC
        // ========================================

        edtCustomerGlobalSearch.addTextChangedListener(
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

                        // Tự động tìm khi từ khóa thay đổi
                        filterResults();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        // ========================================
        // 10.2. LỌC THEO LOẠI KẾT QUẢ
        // ========================================

        chipGroupCustomerSearch
                .setOnCheckedStateChangeListener(
                        (group, checkedIds) -> {

                            // Không có Chip nào được chọn
                            if (checkedIds.isEmpty()) {
                                return;
                            }

                            int checkedId = checkedIds.get(0);

                            // Lọc theo chi nhánh
                            if (checkedId
                                    == R.id.chipCustomerSearchBranches) {

                                selectedType =
                                        SearchResult.Type.BRANCH;

                                // Lọc theo dịch vụ
                            } else if (checkedId
                                    == R.id.chipCustomerSearchServices) {

                                selectedType =
                                        SearchResult.Type.SERVICE;

                                // Lọc theo Barber
                            } else if (checkedId
                                    == R.id.chipCustomerSearchBarbers) {

                                selectedType =
                                        SearchResult.Type.BARBER;

                                // Hiển thị tất cả
                            } else {

                                selectedType = null;
                            }

                            // Cập nhật kết quả sau khi lọc
                            filterResults();
                        }
                );
    }

    // ========================================
    // 11. NHẬN TỪ KHÓA TÌM KIẾM BAN ĐẦU
    // ========================================

    private void applyInitialSearch() {

        // Nhận từ khóa từ màn hình trước
        String initialKeyword =
                getIntent().getStringExtra(
                        EXTRA_INITIAL_SEARCH_KEYWORD
                );

        // Kiểm tra từ khóa hợp lệ
        if (initialKeyword == null
                || initialKeyword.trim().isEmpty()) {
            return;
        }

        // Điền từ khóa vào ô tìm kiếm
        edtCustomerGlobalSearch.setText(
                initialKeyword
        );

        // Đưa con trỏ về cuối chuỗi
        edtCustomerGlobalSearch.setSelection(
                initialKeyword.length()
        );

        // TextWatcher sẽ tự động gọi filterResults()
    }

    // ========================================
    // 12. KẾT HỢP TÌM KIẾM VÀ BỘ LỌC
    // ========================================

    private void filterResults() {

        // Lấy nội dung từ ô tìm kiếm
        Editable input = edtCustomerGlobalSearch.getText();

        // Chuẩn hóa từ khóa
        String keyword = input == null
                ? ""
                : normalizeText(
                input.toString().trim()
        );

        // Danh sách kết quả sau khi lọc
        List<SearchResult> filtered =
                new ArrayList<>();

        // Duyệt qua toàn bộ dữ liệu
        for (SearchResult result : allResults) {

            // ========================================
            // ĐIỀU KIỆN 1: LỌC THEO LOẠI
            // ========================================

            boolean matchesType =
                    selectedType == null
                            || result.getType() == selectedType;

            // ========================================
            // ĐIỀU KIỆN 2: LỌC THEO TỪ KHÓA
            // ========================================

            String searchableText =
                    result.getTitle() + " "
                            + result.getSubtitle() + " "
                            + result.getDescription();

            boolean matchesKeyword =
                    normalizeText(searchableText)
                            .contains(keyword);

            // ========================================
            // KẾT HỢP HAI ĐIỀU KIỆN
            // ========================================

            if (matchesType && matchesKeyword) {

                filtered.add(result);
            }
        }

        // Cập nhật kết quả lên RecyclerView
        updateSearchResults(filtered);
    }

    // ========================================
    // 13. CHUẨN HÓA TIẾNG VIỆT
    // ========================================

    private String normalizeText(String text) {

        // Kiểm tra chuỗi null
        if (text == null) {
            return "";
        }

        // Tách dấu tiếng Việt
        String normalized = Normalizer.normalize(
                text,
                Normalizer.Form.NFD
        );

        // Loại bỏ dấu và chuyển về chữ thường
        return normalized
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    // ========================================
    // 14. CẬP NHẬT KẾT QUẢ TÌM KIẾM
    // ========================================

    private void updateSearchResults(
            List<SearchResult> results
    ) {

        // Cập nhật danh sách cho Adapter
        searchAdapter.submitList(results);

        // Hiển thị số lượng kết quả
        txtCustomerSearchResultCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d kết quả",
                        results.size()
                )
        );

        // Hiển thị trạng thái rỗng nếu không có dữ liệu
        txtEmptyCustomerSearch.setVisibility(
                results.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 15. ĐIỀU HƯỚNG THEO LOẠI KẾT QUẢ
    // ========================================

    private void openSearchResult(SearchResult result) {

        // Kiểm tra kết quả hợp lệ
        if (result == null) {
            return;
        }

        // Kiểm tra Session trước khi điều hướng
        if (!sessionManager.isCustomer()) {
            openLogin();
            return;
        }

        Intent intent;

        // Xác định loại kết quả
        switch (result.getType()) {

            // ========================================
            // 15.1. KẾT QUẢ LÀ CHI NHÁNH
            // ========================================

            case BRANCH:

                intent = new Intent(
                        CustomerSearchActivity.this,
                        CustomerBranchDetailActivity.class
                );

                // Truyền ID chi nhánh
                intent.putExtra(
                        CustomerBranchDetailActivity.EXTRA_BRANCH_ID,
                        result.getId()
                );

                break;

            // ========================================
            // 15.2. KẾT QUẢ LÀ DỊCH VỤ
            // ========================================

            case SERVICE:

                intent = new Intent(
                        CustomerSearchActivity.this,
                        CustomerServiceDetailActivity.class
                );

                // Truyền ID dịch vụ
                intent.putExtra(
                        CustomerServiceDetailActivity.EXTRA_SERVICE_ID,
                        result.getId()
                );

                break;

            // ========================================
            // 15.3. KẾT QUẢ LÀ BARBER
            // ========================================

            case BARBER:

                intent = new Intent(
                        CustomerSearchActivity.this,
                        CustomerBarberDetailActivity.class
                );

                // Truyền ID Barber
                intent.putExtra(
                        CustomerBarberDetailActivity.EXTRA_BARBER_ID,
                        result.getId()
                );

                break;

            // Loại kết quả không được hỗ trợ
            default:
                return;
        }

        // Mở đúng màn hình Customer Detail
        startActivity(intent);
    }

    // ========================================
    // 16. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                CustomerSearchActivity.this,
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
