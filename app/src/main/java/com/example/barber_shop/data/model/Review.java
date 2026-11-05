
package com.example.barber_shop.data.model;

public class Review {

    // ========================================
    // 1. CÁC HẰNG SỐ
    // ========================================

    // Giới hạn số sao đánh giá
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;

    // Giới hạn độ dài nội dung nhận xét
    public static final int MAX_COMMENT_LENGTH = 500;

    // ========================================
    // 2. KHAI BÁO THUỘC TÍNH
    // ========================================

    // ID duy nhất của đánh giá
    private final String reviewId;

    // Mã Booking được đánh giá
    private final String bookingCode;

    // ID Customer gửi đánh giá
    private final String customerId;

    // ID Barber được đánh giá
    private final String barberId;

    // ID dịch vụ được đánh giá
    private final String serviceId;

    // Số sao dành cho Barber
    private final int barberRating;

    // Số sao dành cho dịch vụ
    private final int serviceRating;

    // Nội dung nhận xét
    private final String comment;

    // Thời điểm đánh giá (Unix milliseconds)
    private final long createdAtMillis;

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public Review(
            String reviewId,
            String bookingCode,
            String customerId,
            String barberId,
            String serviceId,
            int barberRating,
            int serviceRating,
            String comment,
            long createdAtMillis
    ) {

        // ========================================
        // 3.1. KIỂM TRA CÁC ID BẮT BUỘC
        // ========================================

        this.reviewId = requireValue(
                reviewId,
                "Review ID"
        );

        this.bookingCode = requireValue(
                bookingCode,
                "Booking Code"
        );

        this.customerId = requireValue(
                customerId,
                "Customer ID"
        );

        this.barberId = requireValue(
                barberId,
                "Barber ID"
        );

        this.serviceId = requireValue(
                serviceId,
                "Service ID"
        );

        // ========================================
        // 3.2. KIỂM TRA SỐ SAO BARBER
        // ========================================

        if (!isValidRating(barberRating)) {

            throw new IllegalArgumentException(
                    "Số sao Barber phải từ 1 đến 5."
            );
        }

        this.barberRating = barberRating;

        // ========================================
        // 3.3. KIỂM TRA SỐ SAO DỊCH VỤ
        // ========================================

        if (!isValidRating(serviceRating)) {

            throw new IllegalArgumentException(
                    "Số sao dịch vụ phải từ 1 đến 5."
            );
        }

        this.serviceRating = serviceRating;

        // ========================================
        // 3.4. CHUẨN HÓA NHẬN XÉT
        // ========================================

        // Nhận xét không bắt buộc.
        // Nếu null thì chuyển thành chuỗi rỗng.

        String normalizedComment =
                comment == null ? "" : comment.trim();

        if (normalizedComment.length() > MAX_COMMENT_LENGTH) {

            throw new IllegalArgumentException(
                    "Nhận xét không được vượt quá 500 ký tự."
            );
        }

        this.comment = normalizedComment;

        // ========================================
        // 3.5. KIỂM TRA THỜI ĐIỂM ĐÁNH GIÁ
        // ========================================

        if (createdAtMillis <= 0) {

            throw new IllegalArgumentException(
                    "Thời điểm đánh giá không hợp lệ."
            );
        }

        this.createdAtMillis = createdAtMillis;
    }

    // ========================================
    // 4. GETTER REVIEW ID
    // ========================================

    public String getReviewId() {

        return reviewId;
    }

    // ========================================
    // 5. GETTER BOOKING CODE
    // ========================================

    public String getBookingCode() {

        return bookingCode;
    }

    // ========================================
    // 6. GETTER CUSTOMER ID
    // ========================================

    public String getCustomerId() {

        return customerId;
    }

    // ========================================
    // 7. GETTER BARBER ID
    // ========================================

    public String getBarberId() {

        return barberId;
    }

    // ========================================
    // 8. GETTER SERVICE ID
    // ========================================

    public String getServiceId() {

        return serviceId;
    }

    // ========================================
    // 9. GETTER SỐ SAO BARBER
    // ========================================

    public int getBarberRating() {

        return barberRating;
    }

    // ========================================
    // 10. GETTER SỐ SAO DỊCH VỤ
    // ========================================

    public int getServiceRating() {

        return serviceRating;
    }

    // ========================================
    // 11. GETTER NỘI DUNG NHẬN XÉT
    // ========================================

    public String getComment() {

        return comment;
    }

    // ========================================
    // 12. GETTER THỜI ĐIỂM ĐÁNH GIÁ
    // ========================================

    public long getCreatedAtMillis() {

        return createdAtMillis;
    }

    // ========================================
    // 13. KIỂM TRA SỐ SAO HỢP LỆ
    // ========================================

    public static boolean isValidRating(int rating) {

        return rating >= MIN_RATING
                && rating <= MAX_RATING;
    }

    // ========================================
    // 14. KIỂM TRA NHẬN XÉT CÓ NỘI DUNG
    // ========================================

    public boolean hasComment() {

        return !comment.isEmpty();
    }

    // ========================================
    // 15. CHUẨN HÓA THUỘC TÍNH BẮT BUỘC
    // ========================================

    private static String requireValue(
            String value,
            String fieldName
    ) {

        if (value == null || value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " không được để trống."
            );
        }

        return value.trim();
    }

    // ========================================
    // 16. HIỂN THỊ THÔNG TIN ĐỐI TƯỢNG
    // ========================================

    @Override
    public String toString() {

        return "Review{" +
                "reviewId='" + reviewId + '\'' +
                ", bookingCode='" + bookingCode + '\'' +
                ", customerId='" + customerId + '\'' +
                ", barberId='" + barberId + '\'' +
                ", serviceId='" + serviceId + '\'' +
                ", barberRating=" + barberRating +
                ", serviceRating=" + serviceRating +
                ", createdAtMillis=" + createdAtMillis +
                '}';
    }
}
