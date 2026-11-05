
package com.example.barber_shop.ui.auth.otp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.barber_shop.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;



public class VerifyOtpActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private static final long RESEND_DELAY_MS = 60000L;

    private TextView txtOtpEmail;
    private TextView txtResendCountdown;
    private TextView txtResendOtp;
    private TextView txtBackForgot;

    private TextInputLayout tilOtp;
    private TextInputEditText edtOtp;
    private MaterialButton btnVerifyOtp;

    private CountDownTimer countDownTimer;
    private long resendAvailableAt = 0L;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        initViews();

        email = getIntent().getStringExtra(EXTRA_EMAIL);

        if (email == null || email.trim().isEmpty()) {
            Toast.makeText(
                    this,
                    "Không tìm thấy email xác minh",
                    Toast.LENGTH_SHORT
            ).show();
            finish();
            return;
        }

        txtOtpEmail.setText(email);

        setupListeners();
        startResendCountdown();
    }

    private void initViews() {
        txtOtpEmail = findViewById(R.id.txtOtpEmail);
        txtResendCountdown = findViewById(R.id.txtResendCountdown);
        txtResendOtp = findViewById(R.id.txtResendOtp);
        txtBackForgot = findViewById(R.id.txtBackForgot);

        tilOtp = findViewById(R.id.tilOtp);
        edtOtp = findViewById(R.id.edtOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
    }

    private void setupListeners() {

        btnVerifyOtp.setOnClickListener(v -> validateOtp());

        txtResendOtp.setOnClickListener(v -> resendOtp());

        txtBackForgot.setOnClickListener(v -> finish());
    }

    private void validateOtp() {

        String otp = edtOtp.getText() == null
                ? ""
                : edtOtp.getText().toString().trim();

        tilOtp.setError(null);

        if (otp.isEmpty()) {
            tilOtp.setError("Vui lòng nhập mã OTP");
            edtOtp.requestFocus();
            return;
        }

        if (!otp.matches("[0-9]{6}")) {
            tilOtp.setError("Mã OTP phải gồm 6 chữ số");
            edtOtp.requestFocus();
            return;
        }

        // TODO: Gọi API xác minh OTP tại đây.
        // Chỉ chuyển sang ResetPasswordActivity khi
        // Backend trả về kết quả xác minh thành công.

        Toast.makeText(
                this,
                "OTP đúng định dạng. Chưa kết nối xác minh Backend.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void startResendCountdown() {

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        resendAvailableAt =
                android.os.SystemClock.elapsedRealtime()
                        + RESEND_DELAY_MS;

        txtResendOtp.setEnabled(false);
        txtResendOtp.setAlpha(0.5f);

        countDownTimer = new CountDownTimer(
                RESEND_DELAY_MS,
                1000L
        ) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds =
                        (millisUntilFinished + 999) / 1000;

                txtResendCountdown.setText(
                        "Bạn có thể gửi lại mã sau "
                                + seconds + " giây"
                );
            }

            @Override
            public void onFinish() {
                enableResend();
            }
        };

        countDownTimer.start();
    }

    private void enableResend() {
        txtResendCountdown.setText("Chưa nhận được mã OTP?");
        txtResendOtp.setEnabled(true);
        txtResendOtp.setAlpha(1.0f);
    }

    private void resendOtp() {

        long remaining =
                resendAvailableAt
                        - android.os.SystemClock.elapsedRealtime();

        if (remaining > 0) {
            return;
        }

        // TODO: Gọi API gửi lại OTP.
        // Khi có Backend, chỉ khởi động lại bộ đếm
        // nếu yêu cầu gửi lại được chấp nhận.

        Toast.makeText(
                this,
                "Chưa kết nối Backend để gửi lại OTP.",
                Toast.LENGTH_SHORT
        ).show();
    }



    @Override
    protected void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        super.onDestroy();
    }
}
