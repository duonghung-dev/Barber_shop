
package com.example.barber_shop.ui.guest.search;

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
import com.example.barber_shop.data.model.SearchResult;
import com.example.barber_shop.data.repository.SearchRepository;
import com.example.barber_shop.ui.guest.barber.BarberDetailActivity;
import com.example.barber_shop.ui.guest.branch.BranchDetailActivity;
import com.example.barber_shop.ui.guest.service.ServiceDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private MaterialToolbar toolbarSearch;
    private TextInputEditText edtGlobalSearch;
    private ChipGroup chipGroupSearch;
    private RecyclerView recyclerSearchResults;
    private TextView txtSearchResultCount;
    private TextView txtEmptySearch;

    private SearchAdapter searchAdapter;

    private final List<SearchResult> allResults =
            new ArrayList<>();

    // Null nghĩa là không lọc theo loại
    private SearchResult.Type selectedType = null;

    // ========================================
    // 1. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_search);

        setupWindowInsets();
        initViews();
        setupRecyclerView();
        loadSearchData();
        setupListeners();
    }

    // ========================================
    // 2. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.searchRoot);

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

        toolbarSearch = findViewById(
                R.id.toolbarSearch
        );

        edtGlobalSearch = findViewById(
                R.id.edtGlobalSearch
        );

        chipGroupSearch = findViewById(
                R.id.chipGroupSearch
        );

        recyclerSearchResults = findViewById(
                R.id.recyclerSearchResults
        );

        txtSearchResultCount = findViewById(
                R.id.txtSearchResultCount
        );

        txtEmptySearch = findViewById(
                R.id.txtEmptySearch
        );
    }

    // ========================================
    // 4. THIẾT LẬP RECYCLERVIEW
    // ========================================

    private void setupRecyclerView() {

        searchAdapter = new SearchAdapter(
                result -> openSearchResult(result)
        );

        recyclerSearchResults.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerSearchResults.setAdapter(
                searchAdapter
        );
    }

    // ========================================
    // 5. NẠP DỮ LIỆU
    // ========================================

    private void loadSearchData() {

        allResults.clear();

        allResults.addAll(
                SearchRepository.getAllResults()
        );

        // Mặc định hiển thị toàn bộ kết quả
        chipGroupSearch.check(R.id.chipSearchAll);

        updateSearchResults(
                new ArrayList<>(allResults)
        );
    }

    // ========================================
    // 6. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay lại Guest Home
        toolbarSearch.setNavigationOnClickListener(
                v -> finish()
        );

        // Tìm kiếm theo thời gian thực
        edtGlobalSearch.addTextChangedListener(
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
                        filterResults();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        // Lọc theo loại kết quả
        chipGroupSearch.setOnCheckedStateChangeListener(
                (group, checkedIds) -> {

                    if (checkedIds.isEmpty()) {
                        return;
                    }

                    int checkedId = checkedIds.get(0);

                    if (checkedId == R.id.chipSearchBranches) {

                        selectedType = SearchResult.Type.BRANCH;

                    } else if (
                            checkedId == R.id.chipSearchServices
                    ) {

                        selectedType = SearchResult.Type.SERVICE;

                    } else if (
                            checkedId == R.id.chipSearchBarbers
                    ) {

                        selectedType = SearchResult.Type.BARBER;

                    } else {

                        selectedType = null;
                    }

                    filterResults();
                }
        );
    }

    // ========================================
    // 7. KẾT HỢP SEARCH VÀ FILTER
    // ========================================

    private void filterResults() {

        Editable input = edtGlobalSearch.getText();

        String keyword = input == null
                ? ""
                : normalizeText(
                input.toString().trim()
        );

        List<SearchResult> filtered =
                new ArrayList<>();

        for (SearchResult result : allResults) {

            // Điều kiện 1: Bộ lọc loại
            boolean matchesType =
                    selectedType == null
                            || result.getType() == selectedType;

            // Điều kiện 2: Từ khóa tìm kiếm
            String searchableText =
                    result.getTitle() + " "
                            + result.getSubtitle() + " "
                            + result.getDescription();

            boolean matchesKeyword =
                    normalizeText(searchableText)
                            .contains(keyword);

            // Cả hai điều kiện phải đúng
            if (matchesType && matchesKeyword) {
                filtered.add(result);
            }
        }

        updateSearchResults(filtered);
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
    // 9. HIỂN THỊ KẾT QUẢ
    // ========================================

    private void updateSearchResults(
            List<SearchResult> results
    ) {

        searchAdapter.submitList(results);

        txtSearchResultCount.setText(
                String.format(
                        Locale.getDefault(),
                        "%d kết quả",
                        results.size()
                )
        );

        txtEmptySearch.setVisibility(
                results.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    // ========================================
    // 10. ĐIỀU HƯỚNG THEO LOẠI KẾT QUẢ
    // ========================================

    private void openSearchResult(SearchResult result) {

        Intent intent;

        switch (result.getType()) {

            case BRANCH:

                intent = new Intent(
                        this,
                        BranchDetailActivity.class
                );

                intent.putExtra(
                        BranchDetailActivity.EXTRA_BRANCH_ID,
                        result.getId()
                );

                break;

            case SERVICE:

                intent = new Intent(
                        this,
                        ServiceDetailActivity.class
                );

                intent.putExtra(
                        ServiceDetailActivity.EXTRA_SERVICE_ID,
                        result.getId()
                );

                break;

            case BARBER:

                intent = new Intent(
                        this,
                        BarberDetailActivity.class
                );

                intent.putExtra(
                        BarberDetailActivity.EXTRA_BARBER_ID,
                        result.getId()
                );

                break;

            default:
                return;
        }

        startActivity(intent);
    }
}
