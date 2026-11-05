
package com.example.barber_shop.ui.guest.barber;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BarberRepository;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.example.barber_shop.ui.guest.branch.BranchDetailActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class BarberDetailActivity extends AppCompatActivity {

    // Key dùng để nhận ID Barber từ Intent
    public static final String EXTRA_BARBER_ID = "extra_barber_id";

    private MaterialToolbar toolbarBarberDetail;
    private AppCompatImageView imgDetailBarber;

    private TextView txtDetailBarberName;
    private TextView txtDetailBarberSpecialty;
    private TextView txtDetailBarberRating;
    private TextView txtDetailBarberExperience;
    private TextView txtDetailBarberBranchName;
    private TextView txtDetailBarberBranchAddress;

    private MaterialButton btnViewBarberBranch;
    private MaterialButton btnBookWithBarber;

    private Barber currentBarber;
    private Branch currentBranch;

    // ========================================
    // 1. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_barber_detail);

        setupWindowInsets();
        initViews();

        if (!loadBarberDetail()) {
            return;
        }

        setupListeners();
    }

    // ========================================
    // 2. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.barberDetailRoot);

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

        toolbarBarberDetail = findViewById(
                R.id.toolbarBarberDetail
        );

        imgDetailBarber = findViewById(
                R.id.imgDetailBarber
        );

        txtDetailBarberName = findViewById(
                R.id.txtDetailBarberName
        );

        txtDetailBarberSpecialty = findViewById(
                R.id.txtDetailBarberSpecialty
        );

        txtDetailBarberRating = findViewById(
                R.id.txtDetailBarberRating
        );

        txtDetailBarberExperience = findViewById(
                R.id.txtDetailBarberExperience
        );

        txtDetailBarberBranchName = findViewById(
                R.id.txtDetailBarberBranchName
        );

        txtDetailBarberBranchAddress = findViewById(
                R.id.txtDetailBarberBranchAddress
        );

        btnViewBarberBranch = findViewById(
                R.id.btnViewBarberBranch
        );

        btnBookWithBarber = findViewById(
                R.id.btnBookWithBarber
        );
    }

    // ========================================
    // 4. LẤY DỮ LIỆU BARBER
    // ========================================

    private boolean loadBarberDetail() {

        // Nhận ID Barber từ Intent
        String barberId = getIntent().getStringExtra(
                EXTRA_BARBER_ID
        );

        // Tìm Barber theo ID
        currentBarber = BarberRepository.findById(
                barberId
        );

        // Không tìm thấy Barber
        if (currentBarber == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy thông tin Barber.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return false;
        }

        // Tìm chi nhánh mà Barber đang làm việc
        currentBranch = BranchRepository.findById(
                currentBarber.getBranchId()
        );

        // Hiển thị dữ liệu
        displayBarberInfo();

        return true;
    }

    // ========================================
    // 5. HIỂN THỊ THÔNG TIN BARBER
    // ========================================

    private void displayBarberInfo() {

        // Họ tên
        txtDetailBarberName.setText(
                currentBarber.getName()
        );

        // Chuyên môn
        txtDetailBarberSpecialty.setText(
                currentBarber.getSpecialty()
        );

        // Đánh giá
        txtDetailBarberRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f / 5.0",
                        currentBarber.getRating()
                )
        );

        // Kinh nghiệm
        txtDetailBarberExperience.setText(
                String.format(
                        Locale.getDefault(),
                        "%d năm kinh nghiệm",
                        currentBarber.getExperienceYears()
                )
        );

        // Ảnh đại diện
        imgDetailBarber.setImageResource(
                currentBarber.getImageResId()
        );

        // Thông tin chi nhánh
        if (currentBranch != null) {

            txtDetailBarberBranchName.setText(
                    currentBranch.getName()
            );

            txtDetailBarberBranchAddress.setText(
                    currentBranch.getAddress()
            );

        } else {

            txtDetailBarberBranchName.setText(
                    "Chi nhánh đang cập nhật"
            );

            txtDetailBarberBranchAddress.setVisibility(
                    View.GONE
            );

            btnViewBarberBranch.setVisibility(
                    View.GONE
            );
        }
    }

    // ========================================
    // 6. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về màn hình trước
        toolbarBarberDetail.setNavigationOnClickListener(
                v -> finish()
        );

        // Xem chi tiết chi nhánh làm việc
        btnViewBarberBranch.setOnClickListener(
                v -> openBranchDetail()
        );

        // Guest nhấn đặt lịch
        btnBookWithBarber.setOnClickListener(
                v -> showLoginRequiredDialog()
        );
    }

    // ========================================
    // 7. ĐIỀU HƯỚNG ĐẾN BRANCH DETAIL
    // ========================================

    private void openBranchDetail() {

        if (currentBranch == null) {
            return;
        }

        Intent intent = new Intent(
                BarberDetailActivity.this,
                BranchDetailActivity.class
        );

        // Truyền ID chi nhánh
        intent.putExtra(
                BranchDetailActivity.EXTRA_BRANCH_ID,
                currentBranch.getId()
        );

        startActivity(intent);
    }

    // ========================================
    // 8. YÊU CẦU ĐĂNG NHẬP ĐỂ ĐẶT LỊCH
    // ========================================

    private void showLoginRequiredDialog() {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage(
                        "Bạn cần đăng nhập để đặt lịch với "
                                + currentBarber.getName() + "."
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
    // 9. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                BarberDetailActivity.this,
                LoginActivity.class
        );

        // Tái sử dụng Login nếu còn trong Back Stack
        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        // Đóng Barber Detail
        finish();
    }
}
