
package com.example.barber_shop.ui.guest.service;

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
import com.example.barber_shop.data.model.SalonService;
import com.example.barber_shop.data.repository.BranchRepository;
import com.example.barber_shop.data.repository.ServiceRepository;
import com.example.barber_shop.ui.auth.login.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ServiceDetailActivity extends AppCompatActivity {

    // Key nhận dữ liệu từ Intent
    public static final String EXTRA_SERVICE_ID = "extra_service_id";
    public static final String EXTRA_BRANCH_ID = "extra_branch_id";

    // Các thành phần giao diện
    private MaterialToolbar toolbarServiceDetail;
    private AppCompatImageView imgDetailService;

    private TextView txtDetailServiceName;
    private TextView txtDetailServiceCategory;
    private TextView txtDetailServiceDescription;
    private TextView txtDetailServiceDuration;
    private TextView txtDetailServicePrice;
    private TextView txtDetailServiceBranch;

    private View cardServiceBranch;
    private MaterialButton btnBookService;

    // Dữ liệu hiện tại
    private SalonService currentService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_guest_service_detail);

        setupWindowInsets();
        initViews();

        if (!loadServiceDetail()) {
            return;
        }

        setupListeners();
    }

    // ========================================
    // 1. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(R.id.serviceDetailRoot);

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

        toolbarServiceDetail = findViewById(
                R.id.toolbarServiceDetail
        );

        imgDetailService = findViewById(
                R.id.imgDetailService
        );

        txtDetailServiceName = findViewById(
                R.id.txtDetailServiceName
        );

        txtDetailServiceCategory = findViewById(
                R.id.txtDetailServiceCategory
        );

        txtDetailServiceDescription = findViewById(
                R.id.txtDetailServiceDescription
        );

        txtDetailServiceDuration = findViewById(
                R.id.txtDetailServiceDuration
        );

        txtDetailServicePrice = findViewById(
                R.id.txtDetailServicePrice
        );

        txtDetailServiceBranch = findViewById(
                R.id.txtDetailServiceBranch
        );

        cardServiceBranch = findViewById(
                R.id.cardServiceBranch
        );

        btnBookService = findViewById(
                R.id.btnBookService
        );
    }

    // ========================================
    // 3. LẤY DỮ LIỆU DỊCH VỤ
    // ========================================

    private boolean loadServiceDetail() {

        // Nhận ID dịch vụ
        String serviceId = getIntent().getStringExtra(
                EXTRA_SERVICE_ID
        );

        // Tìm dịch vụ trong Repository
        currentService = ServiceRepository.findById(
                serviceId
        );

        if (currentService == null) {

            Toast.makeText(
                    this,
                    "Không tìm thấy thông tin dịch vụ.",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return false;
        }

        // Nhận ID chi nhánh nếu có
        String branchId = getIntent().getStringExtra(
                EXTRA_BRANCH_ID
        );

        if (branchId != null) {

            Branch branch = BranchRepository.findById(
                    branchId
            );

            if (branch == null) {
                showErrorAndFinish("Không tìm thấy chi nhánh.");
                return false;
            }

            // Kiểm tra dịch vụ có thuộc chi nhánh hay không
            List<SalonService> branchServices =
                    ServiceRepository.getServicesForBranch(branchId);

            boolean availableAtBranch = false;

            for (SalonService service : branchServices) {

                if (service.getId().equals(
                        currentService.getId()
                )) {
                    availableAtBranch = true;
                    break;
                }
            }

            if (!availableAtBranch) {
                showErrorAndFinish(
                        "Dịch vụ không có tại chi nhánh này."
                );
                return false;
            }

            // Hiển thị thông tin chi nhánh
            txtDetailServiceBranch.setText(
                    branch.getName()
            );

            cardServiceBranch.setVisibility(
                    View.VISIBLE
            );
        }

        // Gán dữ liệu dịch vụ lên giao diện
        displayServiceInfo();

        return true;
    }

    // ========================================
    // 4. HIỂN THỊ THÔNG TIN DỊCH VỤ
    // ========================================

    private void displayServiceInfo() {

        txtDetailServiceName.setText(
                currentService.getName()
        );

        txtDetailServiceCategory.setText(
                currentService.getCategory()
        );

        txtDetailServiceDescription.setText(
                currentService.getDescription()
        );

        txtDetailServiceDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        currentService.getDurationMinutes()
                )
        );

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        txtDetailServicePrice.setText(
                formatter.format(
                        currentService.getPriceVnd()
                ) + " đ"
        );

        imgDetailService.setImageResource(
                currentService.getImageResId()
        );
    }

    // ========================================
    // 5. XỬ LÝ SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // Quay về Service List hoặc Guest Home
        toolbarServiceDetail.setNavigationOnClickListener(
                v -> finish()
        );

        // Guest nhấn đặt lịch
        btnBookService.setOnClickListener(
                v -> showLoginRequiredDialog()
        );
    }

    // ========================================
    // 6. YÊU CẦU ĐĂNG NHẬP ĐỂ ĐẶT LỊCH
    // ========================================

    private void showLoginRequiredDialog() {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Yêu cầu đăng nhập")
                .setMessage(
                        "Bạn cần đăng nhập để đặt lịch sử dụng dịch vụ "
                                + currentService.getName() + "."
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
    // 7. ĐIỀU HƯỚNG ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        Intent intent = new Intent(
                ServiceDetailActivity.this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);

        finish();
    }

    // ========================================
    // 8. XỬ LÝ DỮ LIỆU KHÔNG HỢP LỆ
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
