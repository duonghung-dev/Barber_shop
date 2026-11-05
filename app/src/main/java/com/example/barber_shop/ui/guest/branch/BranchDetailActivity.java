
package com.example.barber_shop.ui.guest.branch;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.guest.service.ServiceListActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class BranchDetailActivity extends AppCompatActivity {

    // Key dùng để nhận ID chi nhánh từ Intent
    public static final String EXTRA_BRANCH_ID = "extra_branch_id";

    private MaterialToolbar toolbarBranchDetail;
    private AppCompatImageView imgDetailBranch;

    private TextView txtDetailBranchName;
    private TextView txtDetailBranchRating;
    private TextView txtDetailBranchAddress;
    private TextView txtDetailBranchHours;

    private MaterialButton btnViewBranchServices;
    private MaterialButton btnBookAtBranch;

    private Branch currentBranch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_branch_detail);

        setupWindowInsets();
        initViews();

        if (!loadBranchDetail()) {
            return;
        }

        setupListeners();
    }

    // ========================================
    // 1. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.branchDetailRoot);

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

        toolbarBranchDetail = findViewById(
                R.id.toolbarBranchDetail
        );

        imgDetailBranch = findViewById(
                R.id.imgDetailBranch
        );

        txtDetailBranchName = findViewById(
                R.id.txtDetailBranchName
        );

        txtDetailBranchRating = findViewById(
                R.id.txtDetailBranchRating
        );

        txtDetailBranchAddress = findViewById(
                R.id.txtDetailBranchAddress
        );

        txtDetailBranchHours = findViewById(
                R.id.txtDetailBranchHours
        );

        btnViewBranchServices = findViewById(
                R.id.btnViewBranchServices
        );

        btnBookAtBranch = findViewById(
                R.id.btnBookAtBranch
        );
    }

    // ========================================
    // 3. LẤY DỮ LIỆU CHI NHÁNH
    // ========================================

    private boolean loadBranchDetail() {

        // Nhận ID được gửi từ Branch List
        String branchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        // Tra cứu chi nhánh theo ID
        currentBranch = BranchRepository.findById(
                branchId
        );

        // Không tìm thấy chi nhánh
        if (currentBranch == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy thông tin chi nhánh.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return false;
        }

        // Hiển thị dữ liệu
        displayBranchInfo();

        return true;
    }

    // ========================================
    // 4. HIỂN THỊ THÔNG TIN
    // ========================================

    private void displayBranchInfo() {

        txtDetailBranchName.setText(
                currentBranch.getName()
        );

        txtDetailBranchAddress.setText(
                currentBranch.getAddress()
        );

        txtDetailBranchHours.setText(
                currentBranch.getWorkingHours()
        );

        txtDetailBranchRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f / 5.0",
                        currentBranch.getRating()
                )
        );

        imgDetailBranch.setImageResource(
                currentBranch.getImageResId()
        );
    }

    // ========================================
    // 5. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về Branch List
        toolbarBranchDetail.setNavigationOnClickListener(
                v -> finish()
        );

        // Xem dịch vụ tại chi nhánh
        btnViewBranchServices.setOnClickListener(
                v -> openServiceList()
        );

        // Guest nhấn đặt lịch
        btnBookAtBranch.setOnClickListener(
                v -> showLoginRequiredDialog()
        );
    }

    // ========================================
    // 6. CHUYỂN ĐẾN SERVICE LIST
    // ========================================

    private void openServiceList() {

        // Kiểm tra chi nhánh hiện tại
        if (currentBranch == null) {
            return;
        }

        // Tạo Intent mở Service List
        Intent intent = new Intent(
                BranchDetailActivity.this,
                ServiceListActivity.class
        );

        // Gửi ID chi nhánh sang Service List
        intent.putExtra(
                ServiceListActivity.EXTRA_BRANCH_ID,
                currentBranch.getId()
        );

        // Mở danh sách dịch vụ
        startActivity(intent);
    }

    // ========================================
    // 7. YÊU CẦU ĐĂNG NHẬP ĐỂ ĐẶT LỊCH
    // ========================================

    private void showLoginRequiredDialog() {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage(
                        "Bạn cần đăng nhập để đặt lịch tại chi nhánh này."
                )
                .setNegativeButton(
                        "Để sau",
                        (dialog, which) -> dialog.dismiss()
                )
                .setPositiveButton(
                        "Đăng nhập",
                        (dialog, which) -> openLogin()
                )
                .show();
    }

    // ========================================
    // 8. CHUYỂN ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                BranchDetailActivity.this,
                LoginActivity.class
        );

        // Tái sử dụng Login nếu còn trong Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        // Đóng màn hình Branch Detail
        finish();
    }
}

