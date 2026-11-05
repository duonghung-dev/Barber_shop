
package com.example.barber_shop.core.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    // ========================================
    // 1. KHAI BÁO TÊN SESSION VÀ KEY
    // ========================================

    private static final String PREF_NAME =
            "BarberShopSession";

    private static final String KEY_IS_LOGGED_IN =
            "is_logged_in";

    private static final String KEY_USER_ID =
            "user_id";

    private static final String KEY_USER_NAME =
            "user_name";

    private static final String KEY_USER_EMAIL =
            "user_email";

    private static final String KEY_USER_ROLE =
            "user_role";

    // ========================================
    // 2. KHAI BÁO CÁC VAI TRÒ
    // ========================================

    public static final String ROLE_CUSTOMER = "CUSTOMER";
    public static final String ROLE_BARBER = "BARBER";
    public static final String ROLE_ADMIN = "ADMIN";

    private final SharedPreferences preferences;

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public SessionManager(Context context) {

        preferences = context
                .getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // ========================================
    // 4. LƯU THÔNG TIN PHIÊN ĐĂNG NHẬP
    // ========================================

    public boolean saveSession(
            String userId,
            String userName,
            String userEmail,
            String userRole
    ) {

        // Chỉ cho phép lưu vai trò hợp lệ
        if (!ROLE_CUSTOMER.equals(userRole)
                && !ROLE_BARBER.equals(userRole)
                && !ROLE_ADMIN.equals(userRole)) {

            return false;
        }

        if (userId == null || userId.trim().isEmpty()
                || userEmail == null
                || userEmail.trim().isEmpty()) {

            return false;
        }

        SharedPreferences.Editor editor =
                preferences.edit();

        editor.putBoolean(
                KEY_IS_LOGGED_IN,
                true
        );

        editor.putString(
                KEY_USER_ID,
                userId
        );

        editor.putString(
                KEY_USER_NAME,
                userName == null ? "" : userName
        );

        editor.putString(
                KEY_USER_EMAIL,
                userEmail
        );

        editor.putString(
                KEY_USER_ROLE,
                userRole
        );

        editor.apply();

        return true;
    }

    // ========================================
    // 5. KIỂM TRA TRẠNG THÁI ĐĂNG NHẬP
    // ========================================

    public boolean isLoggedIn() {

        return preferences.getBoolean(
                KEY_IS_LOGGED_IN,
                false
        );
    }

    // ========================================
    // 6. LẤY THÔNG TIN NGƯỜI DÙNG
    // ========================================

    public String getUserId() {

        return preferences.getString(
                KEY_USER_ID,
                ""
        );
    }

    public String getUserName() {

        return preferences.getString(
                KEY_USER_NAME,
                ""
        );
    }

    public String getUserEmail() {

        return preferences.getString(
                KEY_USER_EMAIL,
                ""
        );
    }

    public String getUserRole() {

        return preferences.getString(
                KEY_USER_ROLE,
                ""
        );
    }

    // ========================================
    // 7. KIỂM TRA VAI TRÒ
    // ========================================

    public boolean isCustomer() {

        return isLoggedIn()
                && ROLE_CUSTOMER.equals(getUserRole());
    }

    public boolean isBarber() {

        return isLoggedIn()
                && ROLE_BARBER.equals(getUserRole());
    }

    public boolean isAdmin() {

        return isLoggedIn()
                && ROLE_ADMIN.equals(getUserRole());
    }

    // ========================================
    // 8. ĐĂNG XUẤT
    // ========================================

    public void logout() {

        preferences.edit()
                .clear()
                .apply();
    }
}
