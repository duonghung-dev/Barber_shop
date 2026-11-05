
package com.example.barber_shop.ui.auth.register;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barber_shop.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPhone;
    private TextInputLayout tilPassword, tilConfirmPassword;

    private TextInputEditText edtFullName, edtEmail, edtPhone;
    private TextInputEditText edtPassword, edtConfirmPassword;

    private MaterialButton btnRegister;
    private TextView txtLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        edtFullName = findViewById(R.id.edtFullName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> validateRegister());

        txtLogin.setOnClickListener(v -> finish());
    }

    private void validateRegister() {

        String fullName = getInput(edtFullName).trim();
        String email = getInput(edtEmail).trim();
        String phone = getInput(edtPhone).trim();
        String password = getInput(edtPassword);
        String confirmPassword = getInput(edtConfirmPassword);

        clearErrors();

        boolean isValid = true;

        // Kiểm tra họ tên
        if (fullName.isEmpty()) {
            tilFullName.setError("Vui lòng nhập họ và tên");
            isValid = false;
        }

        // Kiểm tra Email
        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        // Kiểm tra số điện thoại Việt Nam
        if (phone.isEmpty()) {
            tilPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else if (!phone.matches("^0[35789][0-9]{8}$")) {
            tilPhone.setError("Số điện thoại không hợp lệ");
            isValid = false;
        }

        // Kiểm tra mật khẩu
        if (password.isEmpty()) {
            tilPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 8) {
            tilPassword.setError("Mật khẩu phải có ít nhất 8 ký tự");
            isValid = false;
        }

        // Kiểm tra xác nhận mật khẩu
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(
                    "Vui lòng xác nhận mật khẩu");
            isValid = false;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(
                    "Mật khẩu xác nhận không khớp");
            isValid = false;
        }

        if (!isValid) {
            return;
        }

        Toast.makeText(
                this,
                "Thông tin hợp lệ. Chưa kết nối Backend.",
                Toast.LENGTH_SHORT
        ).show();
    }

    private String getInput(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString();
    }

    private void clearErrors() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }
}
