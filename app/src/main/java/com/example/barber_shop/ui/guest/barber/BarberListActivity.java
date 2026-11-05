
package com.example.barber_shop.ui.guest.barber;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarberListActivity extends AppCompatActivity {

    // Key nhận ID chi nhánh (nếu có)
    public static final String EXTRA_BRANCH_ID = "extra_branch_id";

    private MaterialToolbar toolbarBarbers;
    private TextInputEditText edtBarberSearch;
    private RecyclerView recyclerBarbers;
    private TextView txtBarbersSubtitle;
    private TextView txtBarberCount;
    private TextView txtEmptyBarbers;

    private BarberAdapter barberAdapter;

    private final List<Barber> allBarbers =
            new ArrayList<>();

    // ========================================
    // 1. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_barber_list);

        setupWindowInsets();
        initViews();
        setupRecyclerView();

        if (!loadBarbers()) {
            return;
        }

        setupListeners();
    }

    // ========================================
    // 2. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.barberListRoot);

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

        toolbarBarbers = findViewById(
                R.id.toolbarBarbers
        );

        edtBarberSearch = findViewById(
                R.id.edtBarberSearch
        );

        recyclerBarbers = findViewById(
                R.id.recyclerBarbers
        );

        txtBarbersSubtitle = findViewById(
                R.id.txtBarbersSubtitle
        );

        txtBarberCount = findViewById(
                R.id.txtBarberCount
        );

        txtEmptyBarbers = findViewById(
                R.id.txtEmptyBarbers
        );
    }

    // ========================================
    // 4. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        // Tạo Adapter
        barberAdapter = new BarberAdapter(
                barber -> openBarberDetail(barber)
        );

        // Danh sách theo chiều dọc
        recyclerBarbers.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Gắn Adapter
        recyclerBarbers.setAdapter(barberAdapter);
    }

    // ========================================
    // 5. LẤY DỮ LIỆU BARBER
    // ========================================

    private boolean loadBarbers() {

        String branchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        allBarbers.clear();

        if (branchId == null) {

            // Mở từ Guest Home:
            // Hiển thị tất cả Barber
            allBarbers.addAll(
                    BarberRepository.getSampleBarbers()
            );

        } else {

            // Mở theo một chi nhánh
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

            txtBarbersSubtitle.setText(
                    "Barber tại " + branch.getName()
            );

            // Lấy Barber thuộc chi nhánh
            allBarbers.addAll(
                    BarberRepository.getBarbersForBranch(
                            branchId
                    )
            );
        }

        updateBarberList(
                new ArrayList<>(allBarbers)
        );

        return true;
    }

    // ========================================
    // 6. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarBarbers.setNavigationOnClickListener(
                v -> finish()
        );

        // Tìm kiếm Barber
        edtBarberSearch.addTextChangedListener(
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
                        filterBarbers(s.toString());
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
    // 7. TÌM KIẾM BARBER
    // ========================================

    private void filterBarbers(String keyword) {

        String query = normalizeText(
                keyword.trim()
        );

        List<Barber> filtered = new ArrayList<>();

        for (Barber barber : allBarbers) {

            String name = normalizeText(
                    barber.getName()
            );

            String specialty = normalizeText(
                    barber.getSpecialty()
            );

            // Tra cứu chi nhánh
            Branch branch = BranchRepository.findById(
                    barber.getBranchId()
            );

            String branchName = branch != null
                    ? normalizeText(branch.getName())
                    : "";

            // Tìm theo tên, chuyên môn hoặc chi nhánh
            boolean matchesKeyword =
                    name.contains(query)
                            || specialty.contains(query)
                            || branchName.contains(query);

            if (matchesKeyword) {
                filtered.add(barber);
            }
        }

        updateBarberList(filtered);
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

    private void updateBarberList(
            List<Barber> barbers
    ) {

        barberAdapter.submitList(barbers);

        // Hiển thị số lượng kết quả
        txtBarberCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d Barber (dữ liệu minh họa)",
                        barbers.size()
                )
        );

        // Hiển thị trạng thái rỗng
        txtEmptyBarbers.setVisibility(
                barbers.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 10. ĐIỀU HƯỚNG ĐẾN BARBER DETAIL
    // ========================================

    private void openBarberDetail(Barber barber) {

        // Tạo Intent mở Barber Detail
        Intent intent = new Intent(
                BarberListActivity.this,
                BarberDetailActivity.class
        );

        // Truyền ID của Barber được chọn
        intent.putExtra(
                BarberDetailActivity.EXTRA_BARBER_ID,
                barber.getId()
        );

        // Mở màn hình chi tiết Barber
        startActivity(intent);
    }
}

