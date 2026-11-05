
package com.example.barber_shop.ui.customer.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.example.barber_shop.R;

import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.User;
import com.example.barber_shop.data.repository.UserRepository;

import com.example.barber_shop.ui.auth.login.LoginActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerEditProfileActivity
        extends AppCompatActivity {

    // ========================================
    // 1. KHAI BÁO HẰNG SỐ
    // ========================================

    private static final String TAG =
            "CustomerEditProfile";

    // Các Key dùng để giữ dữ liệu khi
    // Android tạo lại Activity.
    private static final String STATE_USER_ID =
            "state_user_id";

    private static final String STATE_FULL_NAME =
            "state_full_name";

    private static final String STATE_EMAIL =
            "state_email";

    private static final String STATE_PHONE =
            "state_phone";

    // ========================================
    // 2. KHAI BÁO CÁC VIEW
    // ========================================

    // Nút quay lại
    private ImageButton btnCustomerEditProfileBack;

    // Nội dung có thể cuộn
    private NestedScrollView scrollCustomerEditProfileContent;

    // Thông tin User hiện tại
    private TextView txtCustomerEditProfileAvatar;
    private TextView txtCustomerEditProfileCurrentName;
    private TextView txtCustomerEditProfileCurrentEmail;

    // Các TextInputLayout để hiển thị lỗi
    private TextInputLayout tilCustomerEditFullName;
    private TextInputLayout tilCustomerEditEmail;
    private TextInputLayout tilCustomerEditPhone;

    // Các ô nhập dữ liệu
    private TextInputEditText edtCustomerEditFullName;
    private TextInputEditText edtCustomerEditEmail;
    private TextInputEditText edtCustomerEditPhone;

    // Nút lưu và hủy
    private MaterialButton btnCustomerEditProfileSave;
    private MaterialButton btnCustomerEditProfileCancel;

    // Loading
    private ProgressBar progressCustomerEditProfile;

    // Trạng thái lỗi
    private LinearLayout layoutCustomerEditProfileError;
    private TextView txtCustomerEditProfileError;
    private MaterialButton btnCustomerEditProfileRetry;

    // ========================================
    // 3. KHAI BÁO DỮ LIỆU
    // ========================================

    // Quản lý phiên đăng nhập
    private SessionManager sessionManager;

    // Hồ sơ trước khi chỉnh sửa
    private User currentUser;

    // Xử lý đọc/ghi dữ liệu trên luồng nền
    private final ExecutorService profileExecutor =
            Executors.newSingleThreadExecutor();

    // Phân biệt các yêu cầu tải/lưu dữ liệu
    private int requestVersion = 0;

    // Ngăn lưu liên tiếp nhiều lần
    private boolean isSaving = false;

    // Tránh chuyển đến Login nhiều lần
    private boolean isRedirectingToLogin = false;

    // ========================================
    // 4. DỮ LIỆU BIỂU MẪU ĐƯỢC KHÔI PHỤC
    // ========================================

    // Giữ thông tin người dùng đã nhập
    // khi Activity được tạo lại,
    // ví dụ: xoay màn hình.

    private String restoredUserId;
    private String restoredFullName;
    private String restoredEmail;
    private String restoredPhone;

    private boolean hasRestoredDraft = false;

    // ========================================
    // 5. KHỞI TẠO ACTIVITY
    // ========================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ========================================
        // 5.1. KHỞI TẠO SESSION
        // ========================================

        sessionManager = new SessionManager(this);

        // ========================================
        // 5.2. KIỂM TRA PHÂN QUYỀN
        // ========================================

        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // ========================================
        // 5.3. EDGE-TO-EDGE
        // ========================================

        EdgeToEdge.enable(this);

        // ========================================
        // 5.4. HIỂN THỊ GIAO DIỆN
        // ========================================

        setContentView(
                R.layout.activity_customer_edit_profile
        );

        // ========================================
        // 5.5. KHỞI TẠO GIAO DIỆN
        // ========================================

        setupWindowInsets();

        initViews();

        setupListeners();

        setupBackPressed();

        // ========================================
        // 5.6. KHÔI PHỤC DỮ LIỆU CHƯA LƯU
        // ========================================

        restoreDraftState(savedInstanceState);

        // ========================================
        // 5.7. TẢI THÔNG TIN CUSTOMER
        // ========================================

        loadUserProfile();
    }

    // ========================================
    // 6. XỬ LÝ EDGE-TO-EDGE
    // ========================================

    private void setupWindowInsets() {

        View root = findViewById(
                R.id.customerEditProfileRoot
        );

        // Lưu Padding ban đầu
        final int initialLeft =
                root.getPaddingLeft();

        final int initialTop =
                root.getPaddingTop();

        final int initialRight =
                root.getPaddingRight();

        final int initialBottom =
                root.getPaddingBottom();

        // Chừa khoảng cách với các thanh
        // hệ thống của Android.
        ViewCompat.setOnApplyWindowInsetsListener(
                root,
                (view, windowInsets) -> {

                    Insets insets =
                            windowInsets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    view.setPadding(
                            initialLeft + insets.left,
                            initialTop + insets.top,
                            initialRight + insets.right,
                            initialBottom + insets.bottom
                    );

                    return windowInsets;
                }
        );

        ViewCompat.requestApplyInsets(root);
    }

    // ========================================
    // 7. ÁNH XẠ VIEW
    // ========================================

    private void initViews() {

        // ========================================
        // 7.1. THANH TIÊU ĐỀ
        // ========================================

        btnCustomerEditProfileBack = findViewById(
                R.id.btnCustomerEditProfileBack
        );

        // ========================================
        // 7.2. NỘI DUNG CHÍNH
        // ========================================

        scrollCustomerEditProfileContent = findViewById(
                R.id.scrollCustomerEditProfileContent
        );

        // ========================================
        // 7.3. THÔNG TIN TÀI KHOẢN
        // ========================================

        txtCustomerEditProfileAvatar = findViewById(
                R.id.txtCustomerEditProfileAvatar
        );

        txtCustomerEditProfileCurrentName = findViewById(
                R.id.txtCustomerEditProfileCurrentName
        );

        txtCustomerEditProfileCurrentEmail = findViewById(
                R.id.txtCustomerEditProfileCurrentEmail
        );

        // ========================================
        // 7.4. TEXT INPUT LAYOUT
        // ========================================

        tilCustomerEditFullName = findViewById(
                R.id.tilCustomerEditFullName
        );

        tilCustomerEditEmail = findViewById(
                R.id.tilCustomerEditEmail
        );

        tilCustomerEditPhone = findViewById(
                R.id.tilCustomerEditPhone
        );

        // ========================================
        // 7.5. EDIT TEXT
        // ========================================

        edtCustomerEditFullName = findViewById(
                R.id.edtCustomerEditFullName
        );

        edtCustomerEditEmail = findViewById(
                R.id.edtCustomerEditEmail
        );

        edtCustomerEditPhone = findViewById(
                R.id.edtCustomerEditPhone
        );

        // ========================================
        // 7.6. CÁC NÚT THAO TÁC
        // ========================================

        btnCustomerEditProfileSave = findViewById(
                R.id.btnCustomerEditProfileSave
        );

        btnCustomerEditProfileCancel = findViewById(
                R.id.btnCustomerEditProfileCancel
        );

        // ========================================
        // 7.7. LOADING
        // ========================================

        progressCustomerEditProfile = findViewById(
                R.id.progressCustomerEditProfile
        );

        // ========================================
        // 7.8. TRẠNG THÁI LỖI
        // ========================================

        layoutCustomerEditProfileError = findViewById(
                R.id.layoutCustomerEditProfileError
        );

        txtCustomerEditProfileError = findViewById(
                R.id.txtCustomerEditProfileError
        );

        btnCustomerEditProfileRetry = findViewById(
                R.id.btnCustomerEditProfileRetry
        );
    }

    // ========================================
    // 8. THIẾT LẬP SỰ KIỆN
    // ========================================

    private void setupListeners() {

        // ========================================
        // 8.1. NÚT QUAY LẠI
        // ========================================

        btnCustomerEditProfileBack.setOnClickListener(
                v -> attemptExit()
        );

        // ========================================
        // 8.2. NÚT LƯU THAY ĐỔI
        // ========================================

        btnCustomerEditProfileSave.setOnClickListener(
                v -> validateAndSaveProfile()
        );

        // ========================================
        // 8.3. NÚT HỦY
        // ========================================

        btnCustomerEditProfileCancel.setOnClickListener(
                v -> attemptExit()
        );

        // ========================================
        // 8.4. NÚT THỬ LẠI
        // ========================================

        btnCustomerEditProfileRetry.setOnClickListener(
                v -> loadUserProfile()
        );
    }

    // ========================================
    // 9. XỬ LÝ NÚT BACK CỦA ANDROID
    // ========================================

    private void setupBackPressed() {

        // Sử dụng OnBackPressedDispatcher
        // thay vì ghi đè onBackPressed().

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        attemptExit();
                    }
                }
        );
    }

    // ========================================
    // 10. KIỂM TRA SESSION KHI QUAY LẠI
    // ========================================

    @Override
    protected void onResume() {

        super.onResume();

        if (isFinishing()
                || isRedirectingToLogin) {

            return;
        }

        // Nếu Session không còn hợp lệ,
        // quay về màn hình Login.
        if (!sessionManager.isCustomer()) {

            openLogin();
            return;
        }

        // Nếu tài khoản đang đăng nhập đã
        // thay đổi trong lúc Activity tồn tại,
        // không tiếp tục hiển thị hồ sơ cũ.
        if (currentUser != null
                && !currentUser.getUserId().equals(
                sessionManager.getUserId()
        )) {

            finish();
        }

        // Không tự tải lại biểu mẫu tại đây.
        //
        // Việc tải lại trong onResume()
        // có thể làm mất dữ liệu Customer
        // đang nhập nhưng chưa lưu.
    }

    // ========================================
    // 11. TẢI HỒ SƠ CUSTOMER
    // ========================================

    private void loadUserProfile() {

        // ========================================
        // 11.1. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        if (profileExecutor.isShutdown()) {

            return;
        }

        // ========================================
        // 11.2. GHI NHẬN CUSTOMER HIỆN TẠI
        // ========================================

        final String requestedUserId =
                sessionManager.getUserId();

        // ========================================
        // 11.3. TẠO MÃ YÊU CẦU
        // ========================================

        final int currentRequest =
                ++requestVersion;

        // ========================================
        // 11.4. HIỂN THỊ LOADING
        // ========================================

        showLoading();

        // ========================================
        // 11.5. ĐỌC DỮ LIỆU TRÊN LUỒNG NỀN
        // ========================================

        profileExecutor.execute(() -> {

            UserRepository.UserResult result;

            try {

                result =
                        UserRepository.getOrCreateCurrentCustomer(
                                getApplicationContext()
                        );

            } catch (RuntimeException exception) {

                Log.e(
                        TAG,
                        "Lỗi khi tải hồ sơ Customer.",
                        exception
                );

                runOnUiThread(() -> {

                    if (!isActivityResultValid(currentRequest)) {

                        return;
                    }

                    showError(
                            "Đã xảy ra lỗi khi tải hồ sơ."
                    );
                });

                return;
            }

            // ========================================
            // 11.6. CẬP NHẬT GIAO DIỆN
            // ========================================

            runOnUiThread(() -> {

                if (!isActivityResultValid(currentRequest)) {

                    return;
                }

                // ========================================
                // 11.7. KIỂM TRA SESSION
                // ========================================

                if (!ensureCustomerSession()) {

                    return;
                }

                // Không hiển thị dữ liệu của
                // một Customer khác.
                if (!requestedUserId.equals(
                        sessionManager.getUserId()
                )) {

                    finish();
                    return;
                }

                // ========================================
                // 11.8. KIỂM TRA KẾT QUẢ
                // ========================================

                if (!result.isSuccess()
                        || result.getUser() == null) {

                    showError(result.getMessage());
                    return;
                }

                // ========================================
                // 11.9. LƯU USER HIỆN TẠI
                // ========================================

                currentUser = result.getUser();

                // ========================================
                // 11.10. HIỂN THỊ DỮ LIỆU
                // ========================================

                displayUserProfile(currentUser);

                showContent();
            });
        });
    }

    // ========================================
    // 12. HIỂN THỊ THÔNG TIN CUSTOMER
    // ========================================

    private void displayUserProfile(User user) {

        if (user == null) {

            showError(
                    "Không tìm thấy thông tin Customer."
            );

            return;
        }

        // ========================================
        // 12.1. HIỂN THỊ AVATAR
        // ========================================

        txtCustomerEditProfileAvatar.setText(
                getNameInitials(user.getFullName())
        );

        // ========================================
        // 12.2. HIỂN THỊ HỌ TÊN HIỆN TẠI
        // ========================================

        txtCustomerEditProfileCurrentName.setText(
                user.getFullName()
        );

        // ========================================
        // 12.3. HIỂN THỊ EMAIL HIỆN TẠI
        // ========================================

        txtCustomerEditProfileCurrentEmail.setText(
                user.getEmail()
        );

        // ========================================
        // 12.4. ĐỔ DỮ LIỆU VÀO BIỂU MẪU
        // ========================================

        // Mặc định sử dụng dữ liệu đã lưu
        // trong UserRepository.

        String fullName =
                user.getFullName();

        String email =
                user.getEmail();

        String phone =
                user.getPhoneNumber();

        // ========================================
        // 12.5. KHÔI PHỤC DỮ LIỆU CHƯA LƯU
        // ========================================

        // Nếu Activity được tạo lại và
        // User ID vẫn là tài khoản ban đầu,
        // ưu tiên giữ những gì Customer
        // đã nhập vào biểu mẫu.

        if (hasRestoredDraft
                && user.getUserId().equals(
                restoredUserId
        )) {

            fullName = restoredFullName;
            email = restoredEmail;
            phone = restoredPhone;
        }

        // Chỉ áp dụng dữ liệu khôi phục một lần
        hasRestoredDraft = false;

        // ========================================
        // 12.6. ĐIỀN HỌ VÀ TÊN
        // ========================================

        edtCustomerEditFullName.setText(
                fullName
        );

        // ========================================
        // 12.7. ĐIỀN EMAIL
        // ========================================

        edtCustomerEditEmail.setText(
                email
        );

        // ========================================
        // 12.8. ĐIỀN SỐ ĐIỆN THOẠI
        // ========================================

        edtCustomerEditPhone.setText(
                phone
        );

        // ========================================
        // 12.9. XÓA CÁC THÔNG BÁO LỖI CŨ
        // ========================================

        clearErrors();
    }

    // ========================================
    // 13. KIỂM TRA VÀ LƯU HỒ SƠ
    // ========================================

    private void validateAndSaveProfile() {

        // ========================================
        // 13.1. NGĂN LƯU NHIỀU LẦN
        // ========================================

        if (isSaving) {

            return;
        }

        // ========================================
        // 13.2. KIỂM TRA SESSION
        // ========================================

        if (!ensureCustomerSession()) {

            return;
        }

        // ========================================
        // 13.3. KIỂM TRA USER
        // ========================================

        if (currentUser == null) {

            Toast.makeText(
                    this,
                    "Vui lòng tải lại hồ sơ.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 13.4. LẤY DỮ LIỆU NHẬP
        // ========================================

        String fullName =
                getInput(edtCustomerEditFullName)
                        .trim();

        String email =
                getInput(edtCustomerEditEmail)
                        .trim();

        String phoneNumber =
                getInput(edtCustomerEditPhone)
                        .trim();

        // ========================================
        // 13.5. XÓA THÔNG BÁO LỖI
        // ========================================

        clearErrors();

        boolean isValid = true;

        View firstInvalidView = null;

        // ========================================
        // 13.6. KIỂM TRA HỌ VÀ TÊN
        // ========================================

        if (fullName.isEmpty()) {

            tilCustomerEditFullName.setError(
                    "Vui lòng nhập họ và tên"
            );

            firstInvalidView =
                    edtCustomerEditFullName;

            isValid = false;

        } else if (fullName.length() > 100) {

            tilCustomerEditFullName.setError(
                    "Họ và tên không được quá 100 ký tự"
            );

            firstInvalidView =
                    edtCustomerEditFullName;

            isValid = false;
        }

        // ========================================
        // 13.7. KIỂM TRA EMAIL
        // ========================================

        if (email.isEmpty()) {

            tilCustomerEditEmail.setError(
                    "Vui lòng nhập email"
            );

            if (firstInvalidView == null) {

                firstInvalidView =
                        edtCustomerEditEmail;
            }

            isValid = false;

        } else if (email.length() > 254
                || !Patterns.EMAIL_ADDRESS
                .matcher(email)
                .matches()) {

            tilCustomerEditEmail.setError(
                    "Email không hợp lệ"
            );

            if (firstInvalidView == null) {

                firstInvalidView =
                        edtCustomerEditEmail;
            }

            isValid = false;
        }

        // ========================================
        // 13.8. KIỂM TRA SỐ ĐIỆN THOẠI
        // ========================================

        // Số điện thoại không bắt buộc.
        //
        // Nếu có nhập, kiểm tra theo
        // quy tắc đang dùng tại Register.

        if (!phoneNumber.isEmpty()
                && !phoneNumber.matches(
                "^0[35789][0-9]{8}$"
        )) {

            tilCustomerEditPhone.setError(
                    "Số điện thoại không hợp lệ"
            );

            if (firstInvalidView == null) {

                firstInvalidView =
                        edtCustomerEditPhone;
            }

            isValid = false;
        }

        // ========================================
        // 13.9. DỪNG NẾU DỮ LIỆU KHÔNG HỢP LỆ
        // ========================================

        if (!isValid) {

            if (firstInvalidView != null) {

                firstInvalidView.requestFocus();
            }

            return;
        }

        // ========================================
        // 13.10. KIỂM TRA THÔNG TIN KHÔNG ĐỔI
        // ========================================

        if (isSameAsCurrentUser(
                fullName,
                email,
                phoneNumber
        )) {

            Toast.makeText(
                    this,
                    "Thông tin hồ sơ không thay đổi.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 13.11. GỌI REPOSITORY ĐỂ LƯU
        // ========================================

        saveProfile(
                fullName,
                email,
                phoneNumber
        );
    }

    // ========================================
    // 14. THỰC HIỆN LƯU HỒ SƠ
    // ========================================

    private void saveProfile(
            String fullName,
            String email,
            String phoneNumber
    ) {

        // ========================================
        // 14.1. KIỂM TRA EXECUTOR
        // ========================================

        if (profileExecutor.isShutdown()) {

            return;
        }

        // ========================================
        // 14.2. ĐÁNH DẤU ĐANG LƯU
        // ========================================

        isSaving = true;

        final int currentRequest =
                ++requestVersion;

        final String expectedUserId =
                currentUser.getUserId();

        // ========================================
        // 14.3. HIỂN THỊ LOADING
        // ========================================

        showLoading();

        // ========================================
        // 14.4. LƯU TRÊN LUỒNG NỀN
        // ========================================

        profileExecutor.execute(() -> {

            UserRepository.UserResult result;

            try {

                result =
                        UserRepository.updateCurrentCustomerProfile(
                                getApplicationContext(),
                                fullName,
                                email,
                                phoneNumber
                        );

            } catch (RuntimeException exception) {

                Log.e(
                        TAG,
                        "Lỗi khi lưu hồ sơ Customer.",
                        exception
                );

                runOnUiThread(() -> {

                    if (!isActivityResultValid(currentRequest)) {

                        return;
                    }

                    isSaving = false;

                    showContent();

                    Toast.makeText(
                            this,
                            "Đã xảy ra lỗi khi lưu hồ sơ.",
                            Toast.LENGTH_LONG
                    ).show();
                });

                return;
            }

            // ========================================
            // 14.5. XỬ LÝ KẾT QUẢ TRÊN MAIN THREAD
            // ========================================

            runOnUiThread(() -> {

                if (!isActivityResultValid(currentRequest)) {

                    return;
                }

                isSaving = false;

                // ========================================
                // 14.6. KIỂM TRA SESSION
                // ========================================

                if (!ensureCustomerSession()) {

                    return;
                }

                if (!expectedUserId.equals(
                        sessionManager.getUserId()
                )) {

                    finish();
                    return;
                }

                // ========================================
                // 14.7. XỬ LÝ THẤT BẠI
                // ========================================

                if (!result.isSuccess()
                        || result.getUser() == null) {

                    showContent();

                    String message =
                            result.getMessage();

                    // Trường hợp email bị trùng,
                    // hiển thị lỗi tại ô Email.
                    if (message != null
                            && message.toLowerCase(
                            Locale.ROOT
                    ).contains("email")) {

                        tilCustomerEditEmail.setError(
                                message
                        );
                    }

                    Toast.makeText(
                            this,
                            getErrorMessage(message),
                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                // ========================================
                // 14.8. LƯU THÀNH CÔNG
                // ========================================

                currentUser = result.getUser();

                Toast.makeText(
                        this,
                        result.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();

                // ========================================
                // 14.9. THÔNG BÁO KẾT QUẢ CHO ACTIVITY TRƯỚC
                // ========================================

                setResult(RESULT_OK);

                // ========================================
                // 14.10. QUAY VỀ CUSTOMER PROFILE
                // ========================================

                // CustomerProfileActivity sẽ
                // tự tải lại hồ sơ trong onResume().

                finish();
            });
        });
    }

    // ========================================
    // 15. KIỂM TRA THÔNG TIN KHÔNG THAY ĐỔI
    // ========================================

    private boolean isSameAsCurrentUser(
            String fullName,
            String email,
            String phoneNumber
    ) {

        if (currentUser == null) {

            return false;
        }

        return fullName.equals(
                currentUser.getFullName()
        )
                && email.equals(
                currentUser.getEmail()
        )
                && phoneNumber.equals(
                currentUser.getPhoneNumber()
        );
    }

    // ========================================
    // 16. XỬ LÝ THOÁT MÀN HÌNH
    // ========================================

    private void attemptExit() {

        // ========================================
        // 16.1. KIỂM TRA ĐANG LƯU
        // ========================================

        if (isSaving) {

            Toast.makeText(
                    this,
                    "Đang lưu thông tin, vui lòng thử lại sau.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // ========================================
        // 16.2. KIỂM TRA CÓ THAY ĐỔI CHƯA LƯU
        // ========================================

        if (hasUnsavedChanges()) {

            showDiscardChangesConfirmation();

            return;
        }

        // ========================================
        // 16.3. QUAY LẠI
        // ========================================

        finish();
    }

    // ========================================
    // 17. KIỂM TRA DỮ LIỆU CHƯA LƯU
    // ========================================

    private boolean hasUnsavedChanges() {

        if (currentUser == null) {

            return false;
        }

        String fullName =
                getInput(edtCustomerEditFullName).trim();

        String email =
                getInput(edtCustomerEditEmail).trim();

        String phone =
                getInput(edtCustomerEditPhone).trim();

        return !isSameAsCurrentUser(
                fullName,
                email,
                phone
        );
    }

    // ========================================
    // 18. HIỂN THỊ XÁC NHẬN HỦY CHỈNH SỬA
    // ========================================

    private void showDiscardChangesConfirmation() {

        new MaterialAlertDialogBuilder(this)

                .setTitle(
                        "Hủy chỉnh sửa?"
                )

                .setMessage(
                        "Bạn có thay đổi chưa lưu. "
                                + "Nếu thoát, những thay đổi này "
                                + "sẽ không được giữ lại."
                )

                .setNegativeButton(
                        "Tiếp tục chỉnh sửa",
                        (dialog, which) ->
                                dialog.dismiss()
                )

                .setPositiveButton(
                        "Thoát",
                        (dialog, which) ->
                                finish()
                )

                .show();
    }

    // ========================================
    // 19. HIỂN THỊ LOADING
    // ========================================

    private void showLoading() {

        progressCustomerEditProfile.setVisibility(
                View.VISIBLE
        );

        scrollCustomerEditProfileContent.setVisibility(
                View.GONE
        );

        layoutCustomerEditProfileError.setVisibility(
                View.GONE
        );
    }

    // ========================================
    // 20. HIỂN THỊ NỘI DUNG CHỈNH SỬA
    // ========================================

    private void showContent() {

        progressCustomerEditProfile.setVisibility(
                View.GONE
        );

        layoutCustomerEditProfileError.setVisibility(
                View.GONE
        );

        scrollCustomerEditProfileContent.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 21. HIỂN THỊ LỖI TẢI DỮ LIỆU
    // ========================================

    private void showError(String message) {

        progressCustomerEditProfile.setVisibility(
                View.GONE
        );

        scrollCustomerEditProfileContent.setVisibility(
                View.GONE
        );

        txtCustomerEditProfileError.setText(
                getErrorMessage(message)
        );

        layoutCustomerEditProfileError.setVisibility(
                View.VISIBLE
        );
    }

    // ========================================
    // 22. LẤY DỮ LIỆU TỪ EDIT TEXT
    // ========================================

    private String getInput(
            TextInputEditText input
    ) {

        if (input == null
                || input.getText() == null) {

            return "";
        }

        return input.getText().toString();
    }

    // ========================================
    // 23. XÓA CÁC THÔNG BÁO LỖI
    // ========================================

    private void clearErrors() {

        tilCustomerEditFullName.setError(null);

        tilCustomerEditEmail.setError(null);

        tilCustomerEditPhone.setError(null);
    }

    // ========================================
    // 24. LẤY CHỮ CÁI ĐẠI DIỆN
    // ========================================

    private String getNameInitials(String fullName) {

        if (fullName == null
                || fullName.trim().isEmpty()) {

            return "KH";
        }

        String[] parts =
                fullName.trim().split("\\s+");

        String firstInitial =
                parts[0].substring(0, 1);

        if (parts.length == 1) {

            return firstInitial.toUpperCase(
                    new Locale("vi", "VN")
            );
        }

        String lastPart =
                parts[parts.length - 1];

        String lastInitial =
                lastPart.substring(0, 1);

        return (firstInitial + lastInitial)
                .toUpperCase(
                        new Locale("vi", "VN")
                );
    }

    // ========================================
    // 25. KIỂM TRA SESSION CUSTOMER
    // ========================================

    private boolean ensureCustomerSession() {

        if (sessionManager.isCustomer()) {

            return true;
        }

        openLogin();

        return false;
    }

    // ========================================
    // 26. KIỂM TRA KẾT QUẢ BẤT ĐỒNG BỘ
    // ========================================

    private boolean isActivityResultValid(
            int expectedRequest
    ) {

        // Không cập nhật UI nếu Activity đã đóng,
        // bị hủy hoặc có yêu cầu mới hơn.

        return !isFinishing()
                && !isDestroyed()
                && !isRedirectingToLogin
                && expectedRequest == requestVersion;
    }

    // ========================================
    // 27. CHUẨN HÓA THÔNG BÁO LỖI
    // ========================================

    private String getErrorMessage(String message) {

        if (message == null
                || message.trim().isEmpty()) {

            return "Không thể xử lý yêu cầu.";
        }

        return message;
    }

    // ========================================
    // 28. CHUYỂN ĐẾN LOGIN
    // ========================================

    private void openLogin() {

        if (isRedirectingToLogin) {

            return;
        }

        isRedirectingToLogin = true;

        // Vô hiệu hóa kết quả tải dữ liệu cũ.
        requestVersion++;

        Intent intent = new Intent(
                CustomerEditProfileActivity.this,
                LoginActivity.class
        );

        // Xóa các màn hình Customer trước đó
        // khỏi Back Stack.
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }

    // ========================================
    // 29. KHÔI PHỤC BIỂU MẪU
    // ========================================

    private void restoreDraftState(
            Bundle savedInstanceState
    ) {

        if (savedInstanceState == null) {

            return;
        }

        // Chỉ khôi phục nếu trước đó có
        // một hồ sơ đang được chỉnh sửa.

        if (!savedInstanceState.containsKey(
                STATE_USER_ID
        )) {

            return;
        }

        restoredUserId =
                savedInstanceState.getString(
                        STATE_USER_ID
                );

        restoredFullName =
                savedInstanceState.getString(
                        STATE_FULL_NAME,
                        ""
                );

        restoredEmail =
                savedInstanceState.getString(
                        STATE_EMAIL,
                        ""
                );

        restoredPhone =
                savedInstanceState.getString(
                        STATE_PHONE,
                        ""
                );

        hasRestoredDraft = true;
    }

    // ========================================
    // 30. LƯU BIỂU MẪU KHI ACTIVITY TẠO LẠI
    // ========================================

    @Override
    protected void onSaveInstanceState(
            Bundle outState
    ) {

        // Chỉ lưu bản nháp nếu đã tải được User
        // và hiện không có thao tác ghi đang chạy.

        if (currentUser != null
                && !isSaving) {

            outState.putString(
                    STATE_USER_ID,
                    currentUser.getUserId()
            );

            outState.putString(
                    STATE_FULL_NAME,
                    getInput(edtCustomerEditFullName)
            );

            outState.putString(
                    STATE_EMAIL,
                    getInput(edtCustomerEditEmail)
            );

            outState.putString(
                    STATE_PHONE,
                    getInput(edtCustomerEditPhone)
            );
        }

        super.onSaveInstanceState(outState);
    }

    // ========================================
    // 31. GIẢI PHÓNG TÀI NGUYÊN
    // ========================================

    @Override
    protected void onDestroy() {

        // Hủy hiệu lực các kết quả cũ
        requestVersion++;

        // Ngừng nhận tác vụ mới.
        //
        // Nếu Activity bị hủy khi một thao tác
        // lưu đang diễn ra, kết quả lưu cần
        // được kiểm tra lại sau khi mở hồ sơ.
        profileExecutor.shutdownNow();

        super.onDestroy();
    }
}

