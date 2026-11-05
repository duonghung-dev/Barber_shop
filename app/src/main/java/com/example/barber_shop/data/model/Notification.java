
package com.example.barber_shop.data.model;

public class Notification {

    // ========================================
    // 1. KHAI BÁO CÁC LOẠI THÔNG BÁO
    // ========================================

    // Customer đặt lịch thành công
    public static final String TYPE_BOOKING_CREATED =
            "BOOKING_CREATED";

    // Booking được xác nhận
    public static final String TYPE_BOOKING_CONFIRMED =
            "BOOKING_CONFIRMED";

    // Booking đã bị hủy
    public static final String TYPE_BOOKING_CANCELLED =
            "BOOKING_CANCELLED";

    // Booking đã hoàn thành
    public static final String TYPE_BOOKING_COMPLETED =
            "BOOKING_COMPLETED";

    // Nhắc lịch hẹn sắp tới
    public static final String TYPE_BOOKING_REMINDER =
            "BOOKING_REMINDER";

    // ========================================
    // 1.1. THÔNG BÁO ĐỔI LỊCH - STT 21 MỞ RỘNG
    // ========================================

    // Customer đổi ngày hoặc giờ Booking thành công
    public static final String TYPE_BOOKING_RESCHEDULED =
            "BOOKING_RESCHEDULED";

    // ========================================
    // 2. KHAI BÁO THUỘC TÍNH
    // ========================================

    // ID duy nhất của thông báo
    private final String notificationId;

    // ID Customer nhận thông báo
    private final String customerId;

    // Mã Booking liên quan
    private final String bookingCode;

    // Loại thông báo
    private final String type;

    // Tiêu đề thông báo
    private final String title;

    // Nội dung thông báo
    private final String message;

    // Thời điểm tạo thông báo
    private final long createdAtMillis;

    // Trạng thái đã đọc
    private final boolean isRead;

    // ========================================
    // 2.1. EVENT KEY - STT 21 MỞ RỘNG
    // ========================================

    // Khóa nhận diện sự kiện tạo thông báo.
    //
    // Ví dụ:
    // BK000001:CREATED
    // BK000001:CANCELLED
    //
    // Với sự kiện có thể lặp lại như đổi lịch,
    // mỗi lần đổi lịch thành công cần có
    // eventKey riêng.
    //
    // eventKey có thể null để tương thích với
    // các thông báo được tạo trước khi mở rộng.

    private final String eventKey;

    // ========================================
    // 3. CONSTRUCTOR CŨ - GIỮ TƯƠNG THÍCH
    // ========================================

    // Constructor gồm 8 tham số.
    //
    // Giữ nguyên để NotificationRepository
    // và các class đang sử dụng không bị
    // ảnh hưởng khi nâng cấp Model.

    public Notification(
            String notificationId,
            String customerId,
            String bookingCode,
            String type,
            String title,
            String message,
            long createdAtMillis,
            boolean isRead
    ) {

        // Gọi Constructor mới.
        //
        // Các thông báo cũ chưa có eventKey
        // sẽ được gán null.

        this(
                notificationId,
                customerId,
                bookingCode,
                type,
                title,
                message,
                createdAtMillis,
                isRead,
                null
        );
    }

    // ========================================
    // 4. CONSTRUCTOR MỚI - STT 21 MỞ RỘNG
    // ========================================

    // Constructor gồm 9 tham số.
    //
    // Tham số eventKey được bổ sung ở cuối
    // để dễ phân biệt với Constructor cũ.

    public Notification(
            String notificationId,
            String customerId,
            String bookingCode,
            String type,
            String title,
            String message,
            long createdAtMillis,
            boolean isRead,
            String eventKey
    ) {

        // ========================================
        // 4.1. KIỂM TRA THÔNG TIN BẮT BUỘC
        // ========================================

        this.notificationId = requireValue(
                notificationId,
                "Notification ID"
        );

        this.customerId = requireValue(
                customerId,
                "Customer ID"
        );

        this.bookingCode = requireValue(
                bookingCode,
                "Booking Code"
        );

        // ========================================
        // 4.2. KIỂM TRA LOẠI THÔNG BÁO
        // ========================================

        String normalizedType = requireValue(
                type,
                "Notification Type"
        );

        if (!isValidType(normalizedType)) {

            throw new IllegalArgumentException(
                    "Loại thông báo không hợp lệ."
            );
        }

        this.type = normalizedType;

        // ========================================
        // 4.3. KIỂM TRA TIÊU ĐỀ VÀ NỘI DUNG
        // ========================================

        this.title = requireValue(
                title,
                "Notification Title"
        );

        this.message = requireValue(
                message,
                "Notification Message"
        );

        // ========================================
        // 4.4. KIỂM TRA THỜI GIAN TẠO
        // ========================================

        if (createdAtMillis <= 0) {

            throw new IllegalArgumentException(
                    "Thời điểm tạo thông báo không hợp lệ."
            );
        }

        this.createdAtMillis = createdAtMillis;

        // ========================================
        // 4.5. LƯU TRẠNG THÁI ĐÃ ĐỌC
        // ========================================

        this.isRead = isRead;

        // ========================================
        // 4.6. LƯU EVENT KEY - MỚI
        // ========================================

        // eventKey không bắt buộc.
        //
        // Nếu được cung cấp thì loại bỏ
        // khoảng trắng thừa ở hai đầu.

        this.eventKey = normalizeOptionalValue(
                eventKey
        );
    }

    // ========================================
    // 5. GETTER NOTIFICATION ID
    // ========================================

    public String getNotificationId() {

        return notificationId;
    }

    // ========================================
    // 6. GETTER CUSTOMER ID
    // ========================================

    public String getCustomerId() {

        return customerId;
    }

    // ========================================
    // 7. GETTER BOOKING CODE
    // ========================================

    public String getBookingCode() {

        return bookingCode;
    }

    // ========================================
    // 8. GETTER LOẠI THÔNG BÁO
    // ========================================

    public String getType() {

        return type;
    }

    // ========================================
    // 9. GETTER TIÊU ĐỀ
    // ========================================

    public String getTitle() {

        return title;
    }

    // ========================================
    // 10. GETTER NỘI DUNG
    // ========================================

    public String getMessage() {

        return message;
    }

    // ========================================
    // 11. GETTER THỜI GIAN TẠO
    // ========================================

    public long getCreatedAtMillis() {

        return createdAtMillis;
    }

    // ========================================
    // 12. KIỂM TRA TRẠNG THÁI ĐÃ ĐỌC
    // ========================================

    public boolean isRead() {

        return isRead;
    }

    // ========================================
    // 13. GETTER EVENT KEY - MỚI
    // ========================================

    public String getEventKey() {

        return eventKey;
    }

    // ========================================
    // 14. KIỂM TRA CÓ EVENT KEY
    // ========================================

    public boolean hasEventKey() {

        return eventKey != null;
    }

    // ========================================
    // 15. KIỂM TRA LOẠI THÔNG BÁO
    // ========================================

    public static boolean isValidType(String type) {

        if (type == null) {
            return false;
        }

        return TYPE_BOOKING_CREATED.equals(type)
                || TYPE_BOOKING_CONFIRMED.equals(type)
                || TYPE_BOOKING_CANCELLED.equals(type)
                || TYPE_BOOKING_COMPLETED.equals(type)
                || TYPE_BOOKING_REMINDER.equals(type)
                || TYPE_BOOKING_RESCHEDULED.equals(type);
    }

    // ========================================
    // 16. LẤY TÊN HIỂN THỊ CỦA LOẠI THÔNG BÁO
    // ========================================

    public String getTypeDisplayName() {

        switch (type) {

            case TYPE_BOOKING_CREATED:

                return "Đặt lịch thành công";

            case TYPE_BOOKING_CONFIRMED:

                return "Lịch hẹn đã xác nhận";

            case TYPE_BOOKING_CANCELLED:

                return "Lịch hẹn đã hủy";

            case TYPE_BOOKING_COMPLETED:

                return "Lịch hẹn hoàn thành";

            case TYPE_BOOKING_REMINDER:

                return "Nhắc lịch hẹn";

            // ========================================
            // STT 21 MỞ RỘNG - ĐỔI LỊCH
            // ========================================

            case TYPE_BOOKING_RESCHEDULED:

                return "Đổi lịch thành công";

            default:

                return "Thông báo";
        }
    }

    // ========================================
    // 17. TẠO THÔNG BÁO VỚI TRẠNG THÁI MỚI
    // ========================================

    public Notification withReadStatus(boolean read) {

        // ========================================
        // 17.1. GIỮ NGUYÊN DỮ LIỆU GỐC
        // ========================================

        // Vì các thuộc tính được khai báo final,
        // chúng ta không thay đổi trực tiếp
        // trạng thái của đối tượng hiện tại.
        //
        // Thay vào đó, tạo Notification mới.

        // ========================================
        // 17.2. GIỮ NGUYÊN EVENT KEY - MỚI
        // ========================================

        // eventKey phải được giữ nguyên khi
        // Customer đánh dấu đã đọc.
        //
        // Nếu không, thông tin nhận diện
        // sự kiện có thể bị mất.

        return new Notification(
                notificationId,
                customerId,
                bookingCode,
                type,
                title,
                message,
                createdAtMillis,
                read,
                eventKey
        );
    }

    // ========================================
    // 18. ĐÁNH DẤU THÔNG BÁO ĐÃ ĐỌC
    // ========================================

    public Notification markAsRead() {

        // ========================================
        // 18.1. NẾU ĐÃ ĐỌC
        // ========================================

        if (isRead) {

            return this;
        }

        // ========================================
        // 18.2. TẠO PHIÊN BẢN ĐÃ ĐỌC
        // ========================================

        return withReadStatus(true);
    }

    // ========================================
    // 19. CHUẨN HÓA THÔNG TIN BẮT BUỘC
    // ========================================

    private static String requireValue(
            String value,
            String fieldName
    ) {

        if (value == null
                || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return value.trim();
    }

    // ========================================
    // 20. CHUẨN HÓA THÔNG TIN KHÔNG BẮT BUỘC
    // ========================================

    private static String normalizeOptionalValue(
            String value
    ) {

        // ========================================
        // 20.1. KIỂM TRA NULL
        // ========================================

        if (value == null) {
            return null;
        }

        // ========================================
        // 20.2. LOẠI BỎ KHOẢNG TRẮNG
        // ========================================

        String normalized = value.trim();

        // ========================================
        // 20.3. CHUYỂN CHUỖI RỖNG SANG NULL
        // ========================================

        if (normalized.isEmpty()) {
            return null;
        }

        return normalized;
    }

    // ========================================
    // 21. HIỂN THỊ THÔNG TIN ĐỐI TƯỢNG
    // ========================================

    @Override
    public String toString() {

        return "Notification{" +
                "notificationId='" + notificationId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", bookingCode='" + bookingCode + '\'' +
                ", type='" + type + '\'' +
                ", title='" + title + '\'' +
                ", message='" + message + '\'' +
                ", createdAtMillis=" + createdAtMillis +
                ", isRead=" + isRead +
                ", eventKey='" + eventKey + '\'' +
                '}';
    }
}

