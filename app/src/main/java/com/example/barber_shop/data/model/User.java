
package com.example.barber_shop.data.model;

public class User {

    // ========================================
    // 1. KHAI BÁO THUỘC TÍNH
    // ========================================

    // ID duy nhất của người dùng.
    //
    // Ví dụ: CUS_DEMO_001
    private final String userId;

    // Họ và tên người dùng
    private final String fullName;

    // Email của người dùng
    private final String email;

    // Số điện thoại.
    //
    // Có thể là chuỗi rỗng nếu Customer
    // chưa cập nhật số điện thoại.
    private final String phoneNumber;

    // Vai trò tài khoản.
    //
    // Giá trị hiện tại của hệ thống:
    // CUSTOMER, BARBER hoặc ADMIN.
    private final String role;

    // ========================================
    // 2. CONSTRUCTOR
    // ========================================

    public User(
            String userId,
            String fullName,
            String email,
            String phoneNumber,
            String role
    ) {

        // ========================================
        // 2.1. KIỂM TRA USER ID
        // ========================================

        // User ID bắt buộc có giá trị.
        // Không cho phép null hoặc chuỗi rỗng.

        this.userId = requireValue(
                userId,
                "User ID"
        );

        // ========================================
        // 2.2. KIỂM TRA HỌ VÀ TÊN
        // ========================================

        this.fullName = requireValue(
                fullName,
                "Họ và tên"
        );

        // ========================================
        // 2.3. KIỂM TRA EMAIL
        // ========================================

        // Kiểm tra email không được bỏ trống.
        //
        // Việc kiểm tra định dạng email sẽ
        // được thực hiện tại Activity
        // và Repository khi cập nhật hồ sơ.

        this.email = requireValue(
                email,
                "Email"
        );

        // ========================================
        // 2.4. CHUẨN HÓA SỐ ĐIỆN THOẠI
        // ========================================

        // Không bắt buộc phải có số điện thoại
        // ngay khi tạo hồ sơ.
        //
        // Nếu null thì chuyển thành chuỗi rỗng.

        this.phoneNumber =
                normalizeOptionalValue(
                        phoneNumber
                );

        // ========================================
        // 2.5. KIỂM TRA VAI TRÒ
        // ========================================

        String normalizedRole = requireValue(
                role,
                "Vai trò"
        );

        // Đồng bộ với các vai trò đang được
        // SessionManager sử dụng.
        //
        // Không cho phép tạo User có vai trò
        // nằm ngoài hệ thống hiện tại.

        if (!"CUSTOMER".equals(normalizedRole)
                && !"BARBER".equals(normalizedRole)
                && !"ADMIN".equals(normalizedRole)) {

            throw new IllegalArgumentException(
                    "Vai trò người dùng không hợp lệ."
            );
        }

        this.role = normalizedRole;
    }

    // ========================================
    // 3. GETTER USER ID
    // ========================================

    public String getUserId() {

        return userId;
    }

    // ========================================
    // 4. GETTER HỌ VÀ TÊN
    // ========================================

    public String getFullName() {

        return fullName;
    }

    // ========================================
    // 5. GETTER EMAIL
    // ========================================

    public String getEmail() {

        return email;
    }

    // ========================================
    // 6. GETTER SỐ ĐIỆN THOẠI
    // ========================================

    public String getPhoneNumber() {

        return phoneNumber;
    }

    // ========================================
    // 7. GETTER VAI TRÒ
    // ========================================

    public String getRole() {

        return role;
    }

    // ========================================
    // 8. KIỂM TRA ĐÃ CÓ SỐ ĐIỆN THOẠI CHƯA
    // ========================================

    public boolean hasPhoneNumber() {

        // Trả về true nếu số điện thoại
        // đã được cập nhật.

        return !phoneNumber.isEmpty();
    }

    // ========================================
    // 9. KIỂM TRA TÀI KHOẢN CUSTOMER
    // ========================================

    public boolean isCustomer() {

        return "CUSTOMER".equals(role);
    }

    // ========================================
    // 10. KIỂM TRA TÀI KHOẢN BARBER
    // ========================================

    public boolean isBarber() {

        return "BARBER".equals(role);
    }

    // ========================================
    // 11. KIỂM TRA TÀI KHOẢN ADMIN
    // ========================================

    public boolean isAdmin() {

        return "ADMIN".equals(role);
    }

    // ========================================
    // 12. TẠO USER VỚI THÔNG TIN ĐÃ CẬP NHẬT
    // ========================================

    public User withUpdatedProfile(
            String newFullName,
            String newEmail,
            String newPhoneNumber
    ) {

        // ========================================
        // 12.1. KHÔNG THAY ĐỔI THÔNG TIN ĐỊNH DANH
        // ========================================

        // Khi Customer chỉnh sửa hồ sơ,
        // chỉ cho phép thay đổi:
        //
        // - Họ và tên
        // - Email
        // - Số điện thoại
        //
        // Không thay đổi:
        //
        // - User ID
        // - Vai trò

        // ========================================
        // 12.2. TẠO ĐỐI TƯỢNG USER MỚI
        // ========================================

        // Các thuộc tính đang dùng final
        // nên không sửa trực tiếp đối tượng.
        //
        // Trả về một User mới chứa dữ liệu
        // hồ sơ đã được cập nhật.

        return new User(
                userId,
                newFullName,
                newEmail,
                newPhoneNumber,
                role
        );
    }

    // ========================================
    // 13. CHUẨN HÓA THÔNG TIN BẮT BUỘC
    // ========================================

    private static String requireValue(
            String value,
            String fieldName
    ) {

        // ========================================
        // 13.1. KIỂM TRA NULL
        // ========================================

        if (value == null) {

            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        // ========================================
        // 13.2. LOẠI BỎ KHOẢNG TRẮNG
        // ========================================

        String normalized = value.trim();

        // ========================================
        // 13.3. KIỂM TRA CHUỖI RỖNG
        // ========================================

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return normalized;
    }

    // ========================================
    // 14. CHUẨN HÓA THÔNG TIN KHÔNG BẮT BUỘC
    // ========================================

    private static String normalizeOptionalValue(
            String value
    ) {

        // ========================================
        // 14.1. NẾU GIÁ TRỊ NULL
        // ========================================

        if (value == null) {

            return "";
        }

        // ========================================
        // 14.2. LOẠI BỎ KHOẢNG TRẮNG
        // ========================================

        return value.trim();
    }

    // ========================================
    // 15. HIỂN THỊ THÔNG TIN ĐỐI TƯỢNG
    // ========================================

    @Override
    public String toString() {

        return "User{" +
                "userId='" + userId + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
