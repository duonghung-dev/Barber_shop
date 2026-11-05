
package com.example.barber_shop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Patterns;

import com.example.barber_shop.core.session.SessionManager;
import com.example.barber_shop.data.model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    // ========================================
    // 1. CẤU HÌNH REPOSITORY
    // ========================================

    private static final String TAG =
            "UserRepository";

    // Tên SharedPreferences lưu hồ sơ User
    private static final String PREF_NAME =
            "barber_update_users";

    // Key chứa danh sách User dưới dạng JSON
    private static final String KEY_USERS =
            "users_json";

    // Khóa đồng bộ thao tác đọc và ghi
    private static final Object LOCK = new Object();

    // ========================================
    // 2. CONSTRUCTOR
    // ========================================

    private UserRepository() {

        // Sử dụng các phương thức static.
        // Không khởi tạo UserRepository.
    }

    // ========================================
    // 3. CLASS TRẢ KẾT QUẢ
    // ========================================

    public static class UserResult {

        // Trạng thái thành công hoặc thất bại
        private final boolean success;

        // Nội dung kết quả
        private final String message;

        // Thông tin User liên quan
        private final User user;

        // ========================================
        // 3.1. CONSTRUCTOR
        // ========================================

        private UserResult(
                boolean success,
                String message,
                User user
        ) {

            this.success = success;
            this.message = message;
            this.user = user;
        }

        // ========================================
        // 3.2. KIỂM TRA THÀNH CÔNG
        // ========================================

        public boolean isSuccess() {

            return success;
        }

        // ========================================
        // 3.3. LẤY NỘI DUNG KẾT QUẢ
        // ========================================

        public String getMessage() {

            return message;
        }

        // ========================================
        // 3.4. LẤY ĐỐI TƯỢNG USER
        // ========================================

        public User getUser() {

            return user;
        }

        // ========================================
        // 3.5. KẾT QUẢ THÀNH CÔNG
        // ========================================

        private static UserResult success(
                String message,
                User user
        ) {

            return new UserResult(
                    true,
                    message,
                    user
            );
        }

        // ========================================
        // 3.6. KẾT QUẢ THẤT BẠI
        // ========================================

        private static UserResult failure(
                String message
        ) {

            return new UserResult(
                    false,
                    message,
                    null
            );
        }
    }

    // ========================================
    // 4. LẤY HOẶC KHỞI TẠO HỒ SƠ CUSTOMER
    // ========================================

    public static UserResult getOrCreateCurrentCustomer(
            Context context
    ) {

        // ========================================
        // 4.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return UserResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 4.2. KIỂM TRA PHIÊN ĐĂNG NHẬP
        // ========================================

        SessionManager sessionManager =
                new SessionManager(context);

        // Chỉ sử dụng phương thức này
        // với tài khoản CUSTOMER.

        if (!sessionManager.isCustomer()) {

            return UserResult.failure(
                    "Vui lòng đăng nhập tài khoản Customer."
            );
        }

        // ========================================
        // 4.3. LẤY USER ID TỪ SESSION
        // ========================================

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            return UserResult.failure(
                    "Không tìm thấy mã tài khoản Customer."
            );
        }

        // ========================================
        // 4.4. ĐỌC HOẶC TẠO HỒ SƠ
        // ========================================

        UserResult result =
                getOrCreateCustomerInternal(
                        context,
                        sessionManager,
                        customerId
                );

        // ========================================
        // 4.5. ĐỒNG BỘ THÔNG TIN VÀO SESSION
        // ========================================

        // Nếu Customer đã chỉnh sửa hồ sơ
        // trong lần đăng nhập trước:
        //
        // - Ưu tiên dữ liệu đã lưu ở Repository.
        // - Không thay thế bằng tên/email demo.
        //
        // SessionManager được cập nhật lại
        // nếu thông tin đang khác với hồ sơ.

        if (result.isSuccess()
                && result.getUser() != null) {

            syncSessionWithUser(
                    context,
                    result.getUser()
            );
        }

        return result;
    }

    // ========================================
    // 5. XỬ LÝ LẤY HOẶC TẠO CUSTOMER
    // ========================================

    private static UserResult getOrCreateCustomerInternal(
            Context context,
            SessionManager sessionManager,
            String customerId
    ) {

        // ========================================
        // 5.1. ĐỒNG BỘ THAO TÁC
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                List<User> allUsers =
                        readAllUsers(preferences);

                // ========================================
                // 5.2. TÌM USER THEO ID
                // ========================================

                for (User user : allUsers) {

                    if (!customerId.equals(
                            user.getUserId()
                    )) {

                        continue;
                    }

                    // ========================================
                    // 5.3. KIỂM TRA VAI TRÒ
                    // ========================================

                    if (!user.isCustomer()) {

                        return UserResult.failure(
                                "Tài khoản không thuộc vai trò Customer."
                        );
                    }

                    // ========================================
                    // 5.4. TRẢ VỀ HỒ SƠ ĐÃ LƯU
                    // ========================================

                    // Không ghi đè User đã tồn tại.
                    //
                    // Điều này giúp giữ nguyên:
                    // - Họ tên đã chỉnh sửa
                    // - Email đã chỉnh sửa
                    // - Số điện thoại đã cập nhật

                    return UserResult.success(
                            "Lấy hồ sơ Customer thành công.",
                            user
                    );
                }

                // ========================================
                // 5.5. LẤY THÔNG TIN TỪ SESSION
                // ========================================

                // Chỉ thực hiện khi chưa có
                // hồ sơ tương ứng trong Repository.

                String fullName = normalizeValue(
                        sessionManager.getUserName()
                );

                String email = normalizeValue(
                        sessionManager.getUserEmail()
                );

                // ========================================
                // 5.6. KIỂM TRA THÔNG TIN KHỞI TẠO
                // ========================================

                // Với Customer demo hiện tại,
                // SessionManager đã có tên và email.
                //
                // Không tự tạo email giả.
                //
                // Nếu chưa có họ tên,
                // dùng nhãn hiển thị chung.

                if (fullName == null) {

                    fullName = "Khách hàng";
                }

                if (email == null
                        || !Patterns.EMAIL_ADDRESS
                        .matcher(email)
                        .matches()) {

                    return UserResult.failure(
                            "Email tài khoản chưa hợp lệ."
                    );
                }

                // ========================================
                // 5.7. KIỂM TRA EMAIL KHÔNG TRÙNG
                // ========================================

                for (User existingUser : allUsers) {

                    if (email.equalsIgnoreCase(
                            existingUser.getEmail()
                    )) {

                        return UserResult.failure(
                                "Email đã thuộc về tài khoản khác."
                        );
                    }
                }

                // ========================================
                // 5.8. TẠO USER MỚI
                // ========================================

                // Chưa có số điện thoại trong Session.
                //
                // Sử dụng chuỗi rỗng để màn hình
                // Profile hiển thị "Chưa cập nhật".

                User newUser = new User(
                        customerId,
                        fullName,
                        email,
                        "",
                        SessionManager.ROLE_CUSTOMER
                );

                // ========================================
                // 5.9. THÊM VÀO DANH SÁCH
                // ========================================

                allUsers.add(newUser);

                // ========================================
                // 5.10. LƯU DỮ LIỆU
                // ========================================

                boolean saved = saveAllUsers(
                        preferences,
                        allUsers
                );

                if (!saved) {

                    return UserResult.failure(
                            "Không thể khởi tạo hồ sơ Customer."
                    );
                }

                // ========================================
                // 5.11. TRẢ KẾT QUẢ
                // ========================================

                return UserResult.success(
                        "Khởi tạo hồ sơ Customer thành công.",
                        newUser
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                Log.e(
                        TAG,
                        "Lỗi khi đọc hoặc khởi tạo hồ sơ.",
                        e
                );

                return UserResult.failure(
                        "Không thể đọc hoặc khởi tạo hồ sơ."
                );
            }
        }
    }

    // ========================================
    // 6. CẬP NHẬT HỒ SƠ CUSTOMER
    // ========================================

    public static UserResult updateCurrentCustomerProfile(
            Context context,
            String newFullName,
            String newEmail,
            String newPhoneNumber
    ) {

        // ========================================
        // 6.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return UserResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 6.2. KIỂM TRA PHIÊN ĐĂNG NHẬP
        // ========================================

        SessionManager sessionManager =
                new SessionManager(context);

        if (!sessionManager.isCustomer()) {

            return UserResult.failure(
                    "Vui lòng đăng nhập tài khoản Customer."
            );
        }

        String customerId = normalizeValue(
                sessionManager.getUserId()
        );

        if (customerId == null) {

            return UserResult.failure(
                    "Không xác định được tài khoản Customer."
            );
        }

        // ========================================
        // 6.3. CHUẨN HÓA THÔNG TIN
        // ========================================

        newFullName = normalizeValue(newFullName);
        newEmail = normalizeValue(newEmail);
        newPhoneNumber = normalizeValue(newPhoneNumber);

        // ========================================
        // 6.4. KIỂM TRA HỌ VÀ TÊN
        // ========================================

        if (newFullName == null) {

            return UserResult.failure(
                    "Vui lòng nhập họ và tên."
            );
        }

        // ========================================
        // 6.5. KIỂM TRA EMAIL
        // ========================================

        if (newEmail == null) {

            return UserResult.failure(
                    "Vui lòng nhập email."
            );
        }

        if (!Patterns.EMAIL_ADDRESS
                .matcher(newEmail)
                .matches()) {

            return UserResult.failure(
                    "Địa chỉ email không hợp lệ."
            );
        }

        // ========================================
        // 6.6. KIỂM TRA SỐ ĐIỆN THOẠI
        // ========================================

        // Số điện thoại được phép để trống.
        //
        // Nếu có nhập thì phải đúng định dạng
        // số điện thoại Việt Nam đang được
        // RegisterActivity sử dụng.

        if (newPhoneNumber != null
                && !newPhoneNumber.matches(
                "^0[35789][0-9]{8}$"
        )) {

            return UserResult.failure(
                    "Số điện thoại không hợp lệ."
            );
        }

        // Chuyển null thành chuỗi rỗng để
        // tương thích với User.java.

        if (newPhoneNumber == null) {

            newPhoneNumber = "";
        }

        // ========================================
        // 6.7. ĐẢM BẢO HỒ SƠ ĐÃ ĐƯỢC KHỞI TẠO
        // ========================================

        UserResult initialResult =
                getOrCreateCurrentCustomer(context);

        if (!initialResult.isSuccess()) {

            return initialResult;
        }

        // ========================================
        // 6.8. CẬP NHẬT HỒ SƠ
        // ========================================

        UserResult result =
                updateCustomerProfileInternal(
                        context,
                        customerId,
                        newFullName,
                        newEmail,
                        newPhoneNumber
                );

        // ========================================
        // 6.9. ĐỒNG BỘ SESSION SAU KHI LƯU
        // ========================================

        // Repository là nguồn dữ liệu chính
        // của hồ sơ tại giai đoạn FE.
        //
        // Session được cập nhật lại sau khi
        // dữ liệu hồ sơ đã lưu thành công.

        if (result.isSuccess()
                && result.getUser() != null) {

            syncSessionWithUser(
                    context,
                    result.getUser()
            );
        }

        return result;
    }

    // ========================================
    // 7. XỬ LÝ CẬP NHẬT HỒ SƠ
    // ========================================

    private static UserResult updateCustomerProfileInternal(
            Context context,
            String customerId,
            String newFullName,
            String newEmail,
            String newPhoneNumber
    ) {

        // ========================================
        // 7.1. ĐỒNG BỘ THAO TÁC
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                List<User> allUsers =
                        readAllUsers(preferences);

                // ========================================
                // 7.2. TÌM CUSTOMER HIỆN TẠI
                // ========================================

                for (int i = 0;
                     i < allUsers.size();
                     i++) {

                    User currentUser =
                            allUsers.get(i);

                    if (!customerId.equals(
                            currentUser.getUserId()
                    )) {

                        continue;
                    }

                    // ========================================
                    // 7.3. KIỂM TRA VAI TRÒ
                    // ========================================

                    if (!currentUser.isCustomer()) {

                        return UserResult.failure(
                                "Bạn không có quyền chỉnh sửa hồ sơ này."
                        );
                    }

                    // ========================================
                    // 7.4. KIỂM TRA EMAIL TRÙNG
                    // ========================================

                    // Không cho phép Customer sử dụng
                    // email của User khác.
                    //
                    // Bỏ qua tài khoản đang chỉnh sửa.

                    for (User otherUser : allUsers) {

                        if (customerId.equals(
                                otherUser.getUserId()
                        )) {

                            continue;
                        }

                        if (newEmail.equalsIgnoreCase(
                                otherUser.getEmail()
                        )) {

                            return UserResult.failure(
                                    "Email đã được sử dụng bởi tài khoản khác."
                            );
                        }
                    }

                    // ========================================
                    // 7.5. KIỂM TRA DỮ LIỆU KHÔNG ĐỔI
                    // ========================================

                    boolean sameName =
                            newFullName.equals(
                                    currentUser.getFullName()
                            );

                    boolean sameEmail =
                            newEmail.equals(
                                    currentUser.getEmail()
                            );

                    boolean samePhone =
                            newPhoneNumber.equals(
                                    currentUser.getPhoneNumber()
                            );

                    if (sameName
                            && sameEmail
                            && samePhone) {

                        return UserResult.success(
                                "Thông tin hồ sơ không thay đổi.",
                                currentUser
                        );
                    }

                    // ========================================
                    // 7.6. TẠO USER ĐÃ CẬP NHẬT
                    // ========================================

                    // Dùng phương thức được định nghĩa
                    // trong User.java.
                    //
                    // Giữ nguyên:
                    // - userId
                    // - role

                    User updatedUser =
                            currentUser.withUpdatedProfile(
                                    newFullName,
                                    newEmail,
                                    newPhoneNumber
                            );

                    // ========================================
                    // 7.7. THAY THẾ TRONG DANH SÁCH
                    // ========================================

                    allUsers.set(
                            i,
                            updatedUser
                    );

                    // ========================================
                    // 7.8. LƯU DỮ LIỆU MỚI
                    // ========================================

                    boolean saved = saveAllUsers(
                            preferences,
                            allUsers
                    );

                    if (!saved) {

                        return UserResult.failure(
                                "Không thể lưu thông tin hồ sơ."
                        );
                    }

                    // ========================================
                    // 7.9. TRẢ KẾT QUẢ
                    // ========================================

                    return UserResult.success(
                            "Cập nhật hồ sơ thành công.",
                            updatedUser
                    );
                }

                // ========================================
                // 7.10. KHÔNG TÌM THẤY CUSTOMER
                // ========================================

                return UserResult.failure(
                        "Không tìm thấy hồ sơ Customer."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                Log.e(
                        TAG,
                        "Lỗi khi cập nhật hồ sơ Customer.",
                        e
                );

                return UserResult.failure(
                        "Không thể cập nhật thông tin hồ sơ."
                );
            }
        }
    }

    // ========================================
    // 8. ĐỒNG BỘ THÔNG TIN VỚI SESSION
    // ========================================

    private static void syncSessionWithUser(
            Context context,
            User user
    ) {

        // ========================================
        // 8.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null || user == null) {

            return;
        }

        SessionManager sessionManager =
                new SessionManager(context);

        // ========================================
        // 8.2. KIỂM TRA QUYỀN CUSTOMER
        // ========================================

        if (!sessionManager.isCustomer()
                || !user.isCustomer()) {

            return;
        }

        // ========================================
        // 8.3. KIỂM TRA CÙNG TÀI KHOẢN
        // ========================================

        if (!user.getUserId().equals(
                sessionManager.getUserId()
        )) {

            return;
        }

        // ========================================
        // 8.4. KIỂM TRA THÔNG TIN SESSION
        // ========================================

        boolean sameName =
                user.getFullName().equals(
                        sessionManager.getUserName()
                );

        boolean sameEmail =
                user.getEmail().equals(
                        sessionManager.getUserEmail()
                );

        // Không cần ghi lại Session nếu
        // tên và email đã trùng với Repository.

        if (sameName && sameEmail) {

            return;
        }

        // ========================================
        // 8.5. CẬP NHẬT SESSION
        // ========================================

        // Chỉ đồng bộ các trường đã tồn tại
        // trong SessionManager.
        //
        // Không thêm số điện thoại vào Session.
        //
        // Giữ nguyên User ID và vai trò.

        boolean saved =
                sessionManager.saveSession(
                        user.getUserId(),
                        user.getFullName(),
                        user.getEmail(),
                        user.getRole()
                );

        if (!saved) {

            Log.w(
                    TAG,
                    "Không thể đồng bộ hồ sơ vào Session."
            );
        }
    }

    // ========================================
    // 9. LẤY SHAREDPREFERENCES
    // ========================================

    private static SharedPreferences getPreferences(
            Context context
    ) {

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    // ========================================
    // 10. ĐỌC TOÀN BỘ USER TỪ JSON
    // ========================================

    private static List<User> readAllUsers(
            SharedPreferences preferences
    ) throws JSONException {

        List<User> users =
                new ArrayList<>();

        // ========================================
        // 10.1. ĐỌC CHUỖI JSON
        // ========================================

        String jsonText = preferences.getString(
                KEY_USERS,
                "[]"
        );

        if (jsonText == null) {

            throw new JSONException(
                    "Dữ liệu hồ sơ không hợp lệ."
            );
        }

        // ========================================
        // 10.2. CHUYỂN SANG JSON ARRAY
        // ========================================

        JSONArray jsonArray =
                new JSONArray(jsonText);

        // ========================================
        // 10.3. DUYỆT TỪNG USER
        // ========================================

        for (int i = 0;
             i < jsonArray.length();
             i++) {

            JSONObject item =
                    jsonArray.getJSONObject(i);

            // ========================================
            // 10.4. CHUYỂN JSON THÀNH USER
            // ========================================

            User user = new User(
                    item.getString("userId"),
                    item.getString("fullName"),
                    item.getString("email"),
                    item.optString("phoneNumber", ""),
                    item.getString("role")
            );

            users.add(user);
        }

        return users;
    }

    // ========================================
    // 11. LƯU TOÀN BỘ USER XUỐNG JSON
    // ========================================

    private static boolean saveAllUsers(
            SharedPreferences preferences,
            List<User> users
    ) throws JSONException {

        JSONArray jsonArray =
                new JSONArray();

        // ========================================
        // 11.1. DUYỆT DANH SÁCH USER
        // ========================================

        for (User user : users) {

            JSONObject item =
                    new JSONObject();

            // ========================================
            // 11.2. LƯU USER ID
            // ========================================

            item.put(
                    "userId",
                    user.getUserId()
            );

            // ========================================
            // 11.3. LƯU HỌ VÀ TÊN
            // ========================================

            item.put(
                    "fullName",
                    user.getFullName()
            );

            // ========================================
            // 11.4. LƯU EMAIL
            // ========================================

            item.put(
                    "email",
                    user.getEmail()
            );

            // ========================================
            // 11.5. LƯU SỐ ĐIỆN THOẠI
            // ========================================

            item.put(
                    "phoneNumber",
                    user.getPhoneNumber()
            );

            // ========================================
            // 11.6. LƯU VAI TRÒ
            // ========================================

            item.put(
                    "role",
                    user.getRole()
            );

            // ========================================
            // 11.7. THÊM VÀO JSON ARRAY
            // ========================================

            jsonArray.put(item);
        }

        // ========================================
        // 11.8. GHI XUỐNG THIẾT BỊ
        // ========================================

        // commit() trả về true hoặc false
        // để biết kết quả lưu.
        //
        // Các Activity nên gọi phương thức ghi
        // qua Executor để tránh chặn Main Thread.

        return preferences.edit()
                .putString(
                        KEY_USERS,
                        jsonArray.toString()
                )
                .commit();
    }

    // ========================================
    // 12. CHUẨN HÓA CHUỖI
    // ========================================

    private static String normalizeValue(
            String value
    ) {

        // ========================================
        // 12.1. KIỂM TRA NULL
        // ========================================

        if (value == null) {

            return null;
        }

        // ========================================
        // 12.2. LOẠI BỎ KHOẢNG TRẮNG
        // ========================================

        String normalized =
                value.trim();

        // ========================================
        // 12.3. CHUYỂN CHUỖI RỖNG SANG NULL
        // ========================================

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
