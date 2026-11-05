

package com.example.barber_shop.ui.guest.service;

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
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ServiceListActivity extends AppCompatActivity {

    public static final String EXTRA_BRANCH_ID = "extra_branch_id";

    private MaterialToolbar toolbarServices;
    private TextInputEditText edtServiceSearch;
    private RecyclerView recyclerServices;
    private TextView txtServicesSubtitle;
    private TextView txtServiceCount;
    private TextView txtEmptyServices;
    private ChipGroup chipGroupServices;

    private ServiceAdapter serviceAdapter;

    private final List<SalonService> allServices =
            new ArrayList<>();

    private String selectedCategory = "";

    // ========================================
    // 1. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_service_list);

        setupWindowInsets();
        initViews();
        setupRecyclerView();

        if (!loadServices()) {
            return;
        }

        setupListeners();
    }

    // ========================================
    // 2. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.serviceListRoot);

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
    // 3. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        toolbarServices = findViewById(
                R.id.toolbarServices
        );

        edtServiceSearch = findViewById(
                R.id.edtServiceSearch
        );

        recyclerServices = findViewById(
                R.id.recyclerServices
        );

        txtServicesSubtitle = findViewById(
                R.id.txtServicesSubtitle
        );

        txtServiceCount = findViewById(
                R.id.txtServiceCount
        );

        txtEmptyServices = findViewById(
                R.id.txtEmptyServices
        );

        chipGroupServices = findViewById(
                R.id.chipGroupServices
        );
    }

    // ========================================
    // 4. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Khởi tạo Adapter
        serviceAdapter = new ServiceAdapter(
                service -> openServiceDetail(service)
        );

        // Hiển thị danh sách theo chiều dọc
        recyclerServices.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter vào RecyclerView
        recyclerServices.setAdapter(serviceAdapter);
    }

    // ========================================
    // 5. LẤY DỮ LIỆU DỊCH VỤ
    // ========================================

    private boolean loadServices() {

        // Nhận ID chi nhánh nếu có
        String branchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        allServices.clear();

        if (branchId == null) {

            // Mở từ Guest Home:
            // Hiển thị tất cả dịch vụ
            allServices.addAll(
                    ServiceRepository.getSampleServices()
            );

        } else {

            // Mở từ Branch Detail:
            // Hiển thị dịch vụ thuộc chi nhánh
            Branch branch = BranchRepository.findById(
                    branchId
            );

            if (branch == null) {

                Toast.makeText(
                        this,
                        "Không tìm thấy chi nhánh.",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
                return false;
            }

            // Hiển thị tên chi nhánh
            txtServicesSubtitle.setText(
                    "Dịch vụ tại " + branch.getName()
            );

            // Lấy dịch vụ theo chi nhánh
            allServices.addAll(
                    ServiceRepository.getServicesForBranch(
                            branchId
                    )
            );
        }

        // Cập nhật danh sách ban đầu
        updateServiceList(
                new ArrayList<>(allServices)
        );

        return true;
    }

    // ========================================
    // 6. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay lại màn hình trước
        toolbarServices.setNavigationOnClickListener(
                v -> finish()
        );

        // Tìm kiếm khi người dùng nhập
        edtServiceSearch.addTextChangedListener(
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

        // Lọc theo nhóm dịch vụ
        chipGroupServices.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {

                    if (checkedIds.isEmpty()) {
                        return;
                    }

                    int checkedId = checkedIds.get(0);

                    if (checkedId == R.id.chipHaircut) {

                        selectedCategory = "Cắt tóc";

                    } else if (checkedId == R.id.chipStyling) {

                        selectedCategory = "Tạo kiểu";

                    } else if (checkedId == R.id.chipCare) {

                        selectedCategory = "Chăm sóc";

                    } else {

                        selectedCategory = "";
                    }

                    filterServices();
                }
        );
    }

    // ========================================
    // 7. TÌM KIẾM VÀ LỌC DỊCH VỤ
    // ========================================

    private void filterServices() {

        Editable input = edtServiceSearch.getText();

        String keyword = input == null
                ? ""
                : input.toString().trim();

        String normalizedKeyword = normalizeText(keyword);

        List<SalonService> filtered = new ArrayList<>();

        for (SalonService service : allServices) {

            // Kiểm tra danh mục
            boolean matchesCategory =
                    selectedCategory.isEmpty()
                            || selectedCategory.equals(
                            service.getCategory()
                    );

            // Kiểm tra từ khóa tìm kiếm
            boolean matchesKeyword =
                    normalizeText(service.getName())
                            .contains(normalizedKeyword)
                            || normalizeText(service.getDescription())
                            .contains(normalizedKeyword);

            // Thỏa mãn cả danh mục và từ khóa
            if (matchesCategory && matchesKeyword) {
                filtered.add(service);
            }
        }

        updateServiceList(filtered);
    }

    // ========================================
    // 8. CHUẨN HÓA TIẾNG VIỆT
    // ========================================

    private String normalizeText(String text) {

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
    // 9. CẬP NHẬT RECYCLERVIEW
    // ========================================

    private void updateServiceList(
            List<SalonService> services
    ) {

        // Cập nhật danh sách
        serviceAdapter.submitList(services);

        // Cập nhật số lượng dịch vụ
        txtServiceCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d dịch vụ",
                        services.size()
                )
        );

        // Hiển thị thông báo nếu không có kết quả
        txtEmptyServices.setVisibility(
                services.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 10. ĐIỀU HƯỚNG ĐẾN SERVICE DETAIL
    // ========================================

    private void openServiceDetail(SalonService service) {

        // Tạo Intent mở Service Detail
        Intent intent = new Intent(
                ServiceListActivity.this,
                ServiceDetailActivity.class
        );

        // Truyền ID dịch vụ được chọn
        intent.putExtra(
                ServiceDetailActivity.EXTRA_SERVICE_ID,
                service.getId()
        );

        // Lấy ID chi nhánh từ màn hình hiện tại
        String branchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        // Nếu được mở từ một chi nhánh,
        // tiếp tục truyền ID chi nhánh sang Service Detail
        if (branchId != null) {

            intent.putExtra(
                    ServiceDetailActivity.EXTRA_BRANCH_ID,
                    branchId
            );
        }

        // Mở màn hình Service Detail
        startActivity(intent);
    }
}
