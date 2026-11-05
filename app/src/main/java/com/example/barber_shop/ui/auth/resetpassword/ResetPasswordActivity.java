
package com.example.barber_shop.ui.auth.resetpassword;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barber_shop.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;

    private TextInputEditText edtNewPassword;
    private TextInputEditText edtConfirmPassword;

    private MaterialButton btnResetPassword;
    private TextView txtBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilNewPassword =
                findViewById(R.id.tilNewPassword);

        tilConfirmPassword =
                findViewById(R.id.tilConfirmPassword);

        edtNewPassword =
                findViewById(R.id.edtNewPassword);

        edtConfirmPassword =
                findViewById(R.id.edtConfirmPassword);

        btnResetPassword =
                findViewById(R.id.btnResetPassword);

        txtBack = findViewById(R.id.txtBack);
    }

    private void setupListeners() {
        btnResetPassword.setOnClickListener(
                v -> validatePassword()
        );

        txtBack.setOnClickListener(v -> finish());
    }

    private void validatePassword() {

        String newPassword =
                edtNewPassword.getText() == null
                        ? ""
                        : edtNewPassword.getText().toString();

        String confirmPassword =
                edtConfirmPassword.getText() == null
                        ? ""
                        : edtConfirmPassword.getText().toString();

        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (newPassword.isEmpty()) {
            tilNewPassword.setError(
                    "Vui lòng nhập mật khẩu mới"
            );
            isValid = false;

        } else if (newPassword.length() < 8) {
            tilNewPassword.setError(
                    "Mật khẩu phải có ít nhất 8 ký tự"
            );
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError(
                    "Vui lòng xác nhận mật khẩu"
            );
            isValid = false;

        } else if (!confirmPassword.equals(newPassword)) {
            tilConfirmPassword.setError(
                    "Mật khẩu xác nhận không khớp"
            );
            isValid = false;
        }

        if (!isValid) {
            return;
        }


        Toast.makeText(
                this,
                "Dữ liệu hợp lệ. Chưa kết nối Backend để đổi mật khẩu.",
                Toast.LENGTH_LONG
        ).show();
    }
}

