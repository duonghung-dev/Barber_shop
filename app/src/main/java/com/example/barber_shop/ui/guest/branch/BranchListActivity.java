

package com.example.barber_shop.ui.guest.branch;

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
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BranchRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BranchListActivity extends AppCompatActivity {

    private MaterialToolbar toolbarBranches;
    private TextInputEditText edtBranchSearch;
    private RecyclerView recyclerBranches;
    private TextView txtEmptyBranches;

    private BranchAdapter branchAdapter;

    private final List<Branch> allBranches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_branch_list);

        setupWindowInsets();
        initViews();
        setupRecyclerView();
        loadSampleBranches();
        setupListeners();
    }

    // ========================================
    // 1. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.branchListRoot);

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
    // 2. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        toolbarBranches = findViewById(
                R.id.toolbarBranches
        );

        edtBranchSearch = findViewById(
                R.id.edtBranchSearch
        );

        recyclerBranches = findViewById(
                R.id.recyclerBranches
        );

        txtEmptyBranches = findViewById(
                R.id.txtEmptyBranches
        );
    }

    // ========================================
    // 3. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        branchAdapter = new BranchAdapter(
                branch -> openBranchDetail(branch)
        );

        recyclerBranches.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerBranches.setAdapter(branchAdapter);
    }

    // ========================================
    // 4. LẤY DỮ LIỆU TỪ REPOSITORY
    // ========================================

    private void loadSampleBranches() {

        allBranches.clear();

        allBranches.addAll(
                BranchRepository.getSampleBranches()
        );

        updateBranchList(
                new ArrayList<>(allBranches)
        );
    }

    // ========================================
    // 5. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về Guest Home
        toolbarBranches.setNavigationOnClickListener(
                v -> finish()
        );

        // Tìm kiếm theo thời gian thực
        edtBranchSearch.addTextChangedListener(
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
                        filterBranches(s.toString());
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
    // 6. TÌM KIẾM CHI NHÁNH
    // ========================================

    private void filterBranches(String keyword) {

        String query = keyword.trim()
                .toLowerCase(Locale.ROOT);

        List<Branch> filteredBranches = new ArrayList<>();

        for (Branch branch : allBranches) {

            String name = branch.getName()
                    .toLowerCase(Locale.ROOT);

            String address = branch.getAddress()
                    .toLowerCase(Locale.ROOT);

            if (name.contains(query)
                    || address.contains(query)) {

                filteredBranches.add(branch);
            }
        }

        updateBranchList(filteredBranches);
    }

    // ========================================
    // 7. CẬP NHẬT DANH SÁCH
    // ========================================

    private void updateBranchList(List<Branch> branches) {

        branchAdapter.submitList(branches);

        txtEmptyBranches.setVisibility(
                branches.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 8. ĐIỀU HƯỚNG ĐẾN BRANCH DETAIL
    // ========================================

    private void openBranchDetail(Branch branch) {

        Intent intent = new Intent(
                BranchListActivity.this,
                BranchDetailActivity.class
        );

        // Chỉ gửi ID chi nhánh
        intent.putExtra(
                BranchDetailActivity.EXTRA_BRANCH_ID,
                branch.getId()
        );

        startActivity(intent);
    }
}
