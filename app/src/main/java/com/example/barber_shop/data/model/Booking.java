
package com.example.barber_shop.data.model;

public class Booking {

    // ========================================
    // 1. CÁC TRẠNG THÁI ĐẶT LỊCH
    // ========================================

    // Lịch hẹn đang chờ xác nhận
    public static final String STATUS_PENDING =
            "PENDING";

    // Lịch hẹn đã được xác nhận
    public static final String STATUS_CONFIRMED =
            "CONFIRMED";

    // Lịch hẹn đã hoàn thành
    public static final String STATUS_COMPLETED =
            "COMPLETED";

    // Lịch hẹn đã bị hủy
    public static final String STATUS_CANCELLED =
            "CANCELLED";

    // ========================================
    // 2. KHAI BÁO THUỘC TÍNH
    // ========================================

    // Mã lịch hẹn
    private String bookingCode;

    // ID của Customer đặt lịch
    private String customerId;

    // ID của chi nhánh
    private String branchId;

    // ID dịch vụ
    private String serviceId;

    // ID Barber thực hiện
    private String barberId;

    // Tên chi nhánh tại thời điểm đặt lịch
    private String branchName;

    // Tên dịch vụ tại thời điểm đặt lịch
    private String serviceName;

    // Tên Barber tại thời điểm đặt lịch
    private String barberName;

    // Ngày đặt lịch: yyyy-MM-dd
    private String bookingDate;

    // Giờ bắt đầu: HH:mm
    private String bookingTime;

    // Thời lượng dịch vụ (phút)
    private int durationMinutes;

    // Tổng giá tiền (VND)
    private long totalPrice;

    // Trạng thái lịch hẹn
    private String status;

    // Thời điểm tạo lịch hẹn (milliseconds)
    private long createdAtMillis;

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public Booking(
            String bookingCode,
            String customerId,
            String branchId,
            String serviceId,
            String barberId,
            String branchName,
            String serviceName,
            String barberName,
            String bookingDate,
            String bookingTime,
            int durationMinutes,
            long totalPrice,
            String status
    ) {

        this.bookingCode = bookingCode;
        this.customerId = customerId;
        this.branchId = branchId;
        this.serviceId = serviceId;
        this.barberId = barberId;

        this.branchName = branchName;
        this.serviceName = serviceName;
        this.barberName = barberName;

        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;

        this.durationMinutes = durationMinutes;
        this.totalPrice = totalPrice;

        // Kiểm tra trạng thái khi khởi tạo
        setStatus(status);

        // Lưu thời điểm tạo đối tượng
        this.createdAtMillis = System.currentTimeMillis();
    }

    // ========================================
    // 4. GETTER - THÔNG TIN ĐỊNH DANH
    // ========================================

    public String getBookingCode() {
        return bookingCode;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getBarberId() {
        return barberId;
    }

    // ========================================
    // 5. GETTER - THÔNG TIN HIỂN THỊ
    // ========================================

    public String getBranchName() {
        return branchName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getBarberName() {
        return barberName;
    }

    // ========================================
    // 6. GETTER - THỜI GIAN ĐẶT LỊCH
    // ========================================

    public String getBookingDate() {
        return bookingDate;
    }

    public String getBookingTime() {
        return bookingTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public long getCreatedAtMillis() {
        return createdAtMillis;
    }

    // ========================================
    // 7. GETTER - GIÁ TIỀN VÀ TRẠNG THÁI
    // ========================================

    public long getTotalPrice() {
        return totalPrice;
    }

    public String getStatus() {
        return status;
    }

    // ========================================
    // 8. CẬP NHẬT TRẠNG THÁI BOOKING
    // ========================================

    public void setStatus(String status) {

        if (!STATUS_PENDING.equals(status)
                && !STATUS_CONFIRMED.equals(status)
                && !STATUS_COMPLETED.equals(status)
                && !STATUS_CANCELLED.equals(status)) {

            throw new IllegalArgumentException(
                    "Trạng thái Booking không hợp lệ."
            );
        }

        this.status = status;
    }

    // ========================================
    // 9. LẤY TÊN TRẠNG THÁI ĐỂ HIỂN THỊ
    // ========================================

    public String getStatusDisplayName() {

        switch (status) {

            case STATUS_PENDING:
                return "Chờ xác nhận";

            case STATUS_CONFIRMED:
                return "Đã xác nhận";

            case STATUS_COMPLETED:
                return "Đã hoàn thành";

            case STATUS_CANCELLED:
                return "Đã hủy";

            default:
                return "Không xác định";
        }
    }

    // ========================================
    // 10. KIỂM TRA LỊCH HẸN CÒN HIỆU LỰC
    // ========================================

    public boolean isActive() {

        // Lịch đang chờ hoặc đã xác nhận
        // được xem là lịch còn hiệu lực.

        return STATUS_PENDING.equals(status)
                || STATUS_CONFIRMED.equals(status);
    }

    // ========================================
    // 11. KIỂM TRA LỊCH HẸN ĐÃ HỦY
    // ========================================

    public boolean isCancelled() {

        return STATUS_CANCELLED.equals(status);
    }

    // ========================================
    // 12. CẬP NHẬT NGÀY GIỜ KHI ĐỔI LỊCH
    // ========================================

    public void updateBookingDateTime(
            String newBookingDate,
            String newBookingTime
    ) {

        // Chỉ cho phép cập nhật đối tượng
        // khi lịch hẹn còn hiệu lực.

        if (!isActive()) {

            throw new IllegalStateException(
                    "Không thể đổi lịch hẹn "
                            + "đã hủy hoặc hoàn thành."
            );
        }

        // Kiểm tra dữ liệu đầu vào cơ bản
        if (newBookingDate == null
                || newBookingDate.trim().isEmpty()
                || newBookingTime == null
                || newBookingTime.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Ngày giờ đặt lịch không hợp lệ."
            );
        }

        // Repository sẽ chịu trách nhiệm
        // kiểm tra trùng lịch, thời gian hoạt động
        // và tính hợp lệ trước khi gọi phương thức này.

        this.bookingDate = newBookingDate.trim();
        this.bookingTime = newBookingTime.trim();
    }
}
