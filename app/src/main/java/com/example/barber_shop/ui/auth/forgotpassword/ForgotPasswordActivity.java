package com.example.barber_shop.ui.auth.forgotpassword;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barber_shop.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import android.content.Intent;
import com.example.barber_shop.ui.auth.otp.VerifyOtpActivity;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText edtEmail;
    private MaterialButton btnSendOtp;
    private TextView txtBackLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initViews();
        setupListeners();
    }

    private void initViews() {

        tilEmail = findViewById(R.id.tilEmail);
        edtEmail = findViewById(R.id.edtEmail);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        txtBackLogin = findViewById(R.id.txtBackLogin);
    }

    private void setupListeners() {

        btnSendOtp.setOnClickListener(v -> validateEmail());

        txtBackLogin.setOnClickListener(v -> finish());
    }

    private void validateEmail() {

        String email = edtEmail.getText() == null
                ? ""
                : edtEmail.getText().toString().trim();

        tilEmail.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Email không hợp lệ");
            edtEmail.requestFocus();
            return;
        }

        Toast.makeText(
                this,
                "Chế độ Demo: Chưa gửi mã OTP thật.",
                Toast.LENGTH_LONG
        ).show();

        Intent intent = new Intent(
                ForgotPasswordActivity.this,
                VerifyOtpActivity.class
        );

        intent.putExtra(
                VerifyOtpActivity.EXTRA_EMAIL,
                email
        );

        startActivity(intent);
    }
}
