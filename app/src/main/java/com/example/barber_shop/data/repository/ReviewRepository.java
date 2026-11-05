
package com.example.barber_shop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.model.Review;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ReviewRepository {

    // ========================================
    // 1. CẤU HÌNH LƯU TRỮ
    // ========================================

    private static final String PREF_NAME =
            "barber_update_reviews";

    private static final String KEY_REVIEWS =
            "reviews_json";

    // Đồng bộ các thao tác đọc và ghi
    private static final Object LOCK = new Object();

    // ========================================
    // 2. CONSTRUCTOR
    // ========================================

    private ReviewRepository() {

        // Repository sử dụng các phương thức static.
        // Không cần tạo đối tượng.
    }

    // ========================================
    // 3. CLASS TRẢ KẾT QUẢ ĐÁNH GIÁ
    // ========================================

    public static class ReviewResult {

        // Trạng thái thao tác
        private final boolean success;

        // Thông báo kết quả
        private final String message;

        // Đánh giá vừa được lưu
        private final Review review;

        private ReviewResult(
                boolean success,
                String message,
                Review review
        ) {

            this.success = success;
            this.message = message;
            this.review = review;
        }

        // ========================================
        // 3.1. KIỂM TRA THÀNH CÔNG
        // ========================================

        public boolean isSuccess() {

            return success;
        }

        // ========================================
        // 3.2. LẤY THÔNG BÁO
        // ========================================

        public String getMessage() {

            return message;
        }

        // ========================================
        // 3.3. LẤY ĐÁNH GIÁ
        // ========================================

        public Review getReview() {

            return review;
        }

        // ========================================
        // 3.4. TẠO KẾT QUẢ THÀNH CÔNG
        // ========================================

        private static ReviewResult success(
                String message,
                Review review
        ) {

            return new ReviewResult(
                    true,
                    message,
                    review
            );
        }

        // ========================================
        // 3.5. TẠO KẾT QUẢ THẤT BẠI
        // ========================================

        private static ReviewResult failure(
                String message
        ) {

            return new ReviewResult(
                    false,
                    message,
                    null
            );
        }
    }

    // ========================================
    // 4. GỬI ĐÁNH GIÁ
    // ========================================

    public static ReviewResult submitReview(
            Context context,
            String customerId,
            String bookingCode,
            int barberRating,
            int serviceRating,
            String comment
    ) {

        // ========================================
        // 4.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return ReviewResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 4.2. CHUẨN HÓA THAM SỐ
        // ========================================

        customerId = normalizeValue(customerId);
        bookingCode = normalizeValue(bookingCode);

        if (customerId == null || bookingCode == null) {

            return ReviewResult.failure(
                    "Thông tin Customer hoặc Booking không hợp lệ."
            );
        }

        // ========================================
        // 4.3. KIỂM TRA SỐ SAO
        // ========================================

        if (!Review.isValidRating(barberRating)) {

            return ReviewResult.failure(
                    "Vui lòng đánh giá Barber từ 1 đến 5 sao."
            );
        }

        if (!Review.isValidRating(serviceRating)) {

            return ReviewResult.failure(
                    "Vui lòng đánh giá dịch vụ từ 1 đến 5 sao."
            );
        }

        // ========================================
        // 4.4. KIỂM TRA NHẬN XÉT
        // ========================================

        String normalizedComment =
                comment == null ? "" : comment.trim();

        if (normalizedComment.length()
                > Review.MAX_COMMENT_LENGTH) {

            return ReviewResult.failure(
                    "Nhận xét không được vượt quá 500 ký tự."
            );
        }

        // ========================================
        // 4.5. ĐỌC VÀ GHI TRONG CÙNG LOCK
        // ========================================

        synchronized (LOCK) {

            try {

                // ========================================
                // 4.6. TÌM BOOKING
                // ========================================

                Booking booking =
                        BookingRepository.findByCode(
                                context,
                                bookingCode
                        );

                if (booking == null) {

                    return ReviewResult.failure(
                            "Không tìm thấy lịch hẹn cần đánh giá."
                    );
                }

                // ========================================
                // 4.7. KIỂM TRA QUYỀN CUSTOMER
                // ========================================

                if (!customerId.equals(
                        booking.getCustomerId()
                )) {

                    return ReviewResult.failure(
                            "Bạn không có quyền đánh giá lịch hẹn này."
                    );
                }

                // ========================================
                // 4.8. KIỂM TRA BOOKING COMPLETED
                // ========================================

                if (!Booking.STATUS_COMPLETED.equals(
                        booking.getStatus()
                )) {

                    return ReviewResult.failure(
                            "Chỉ có thể đánh giá lịch hẹn đã hoàn thành."
                    );
                }

                // ========================================
                // 4.9. ĐỌC DANH SÁCH ĐÁNH GIÁ
                // ========================================

                SharedPreferences preferences =
                        getPreferences(context);

                List<Review> allReviews =
                        readAllReviews(preferences);

                // ========================================
                // 4.10. KIỂM TRA ĐÁNH GIÁ TRÙNG
                // ========================================

                for (Review existingReview : allReviews) {

                    if (bookingCode.equals(
                            existingReview.getBookingCode()
                    )) {

                        return ReviewResult.failure(
                                "Lịch hẹn này đã được đánh giá."
                        );
                    }
                }

                // ========================================
                // 4.11. LẤY BARBER VÀ SERVICE ID
                // ========================================

                String barberId = normalizeValue(
                        booking.getBarberId()
                );

                String serviceId = normalizeValue(
                        booking.getServiceId()
                );

                if (barberId == null || serviceId == null) {

                    return ReviewResult.failure(
                            "Thông tin Barber hoặc dịch vụ chưa đầy đủ."
                    );
                }

                // ========================================
                // 4.12. TẠO REVIEW ID DUY NHẤT
                // ========================================

                String reviewId =
                        "RV-" + UUID.randomUUID();

                // ========================================
                // 4.13. TẠO ĐỐI TƯỢNG REVIEW
                // ========================================

                Review review = new Review(
                        reviewId,
                        bookingCode,
                        customerId,
                        barberId,
                        serviceId,
                        barberRating,
                        serviceRating,
                        normalizedComment,
                        System.currentTimeMillis()
                );

                // ========================================
                // 4.14. THÊM REVIEW VÀO DANH SÁCH
                // ========================================

                allReviews.add(review);

                // ========================================
                // 4.15. LƯU XUỐNG THIẾT BỊ
                // ========================================

                boolean saved = saveAllReviews(
                        preferences,
                        allReviews
                );

                if (!saved) {

                    return ReviewResult.failure(
                            "Không thể lưu đánh giá. Vui lòng thử lại."
                    );
                }

                // ========================================
                // 4.16. TRẢ KẾT QUẢ THÀNH CÔNG
                // ========================================

                return ReviewResult.success(
                        "Gửi đánh giá thành công.",
                        review
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return ReviewResult.failure(
                        "Không thể xử lý đánh giá. Vui lòng thử lại."
                );
            }
        }
    }

    // ========================================
    // 5. LẤY ĐÁNH GIÁ THEO BOOKING
    // ========================================

    public static Review getReviewForBooking(
            Context context,
            String customerId,
            String bookingCode
    ) {

        // ========================================
        // 5.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {
            return null;
        }

        customerId = normalizeValue(customerId);
        bookingCode = normalizeValue(bookingCode);

        if (customerId == null || bookingCode == null) {
            return null;
        }

        // ========================================
        // 5.2. ĐỌC DỮ LIỆU
        // ========================================

        synchronized (LOCK) {

            try {

                List<Review> allReviews =
                        readAllReviews(
                                getPreferences(context)
                        );

                // ========================================
                // 5.3. TÌM ĐÚNG REVIEW
                // ========================================

                for (Review review : allReviews) {

                    boolean sameCustomer =
                            customerId.equals(
                                    review.getCustomerId()
                            );

                    boolean sameBooking =
                            bookingCode.equals(
                                    review.getBookingCode()
                            );

                    if (sameCustomer && sameBooking) {

                        return review;
                    }
                }

                return null;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc thông tin đánh giá.",
                        e
                );
            }
        }
    }

    // ========================================
    // 6. KIỂM TRA BOOKING ĐÃ ĐƯỢC ĐÁNH GIÁ
    // ========================================

    public static boolean hasReviewedBooking(
            Context context,
            String customerId,
            String bookingCode
    ) {

        // ========================================
        // 6.1. TÌM ĐÁNH GIÁ
        // ========================================

        Review review = getReviewForBooking(
                context,
                customerId,
                bookingCode
        );

        // ========================================
        // 6.2. TRẢ KẾT QUẢ
        // ========================================

        return review != null;
    }

    // ========================================
    // 7. LẤY DANH SÁCH ĐÁNH GIÁ CỦA CUSTOMER
    // ========================================

    public static List<Review> getReviewsForCustomer(
            Context context,
            String customerId
    ) {

        List<Review> result = new ArrayList<>();

        // ========================================
        // 7.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {
            return result;
        }

        customerId = normalizeValue(customerId);

        if (customerId == null) {
            return result;
        }

        // ========================================
        // 7.2. ĐỌC DANH SÁCH ĐÁNH GIÁ
        // ========================================

        synchronized (LOCK) {

            try {

                List<Review> allReviews =
                        readAllReviews(
                                getPreferences(context)
                        );

                // ========================================
                // 7.3. LỌC THEO CUSTOMER ID
                // ========================================

                for (Review review : allReviews) {

                    if (customerId.equals(
                            review.getCustomerId()
                    )) {

                        result.add(review);
                    }
                }

                // ========================================
                // 7.4. SẮP XẾP MỚI NHẤT TRƯỚC
                // ========================================

                Collections.sort(
                        result,
                        (review1, review2) ->
                                Long.compare(
                                        review2.getCreatedAtMillis(),
                                        review1.getCreatedAtMillis()
                                )
                );

                return result;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc danh sách đánh giá.",
                        e
                );
            }
        }
    }

    // ========================================
    // 8. LẤY SHAREDPREFERENCES
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
    // 9. ĐỌC TOÀN BỘ REVIEW ĐÃ LƯU
    // ========================================

    private static List<Review> readAllReviews(
            SharedPreferences preferences
    ) throws JSONException {

        List<Review> reviews = new ArrayList<>();

        // ========================================
        // 9.1. ĐỌC CHUỖI JSON
        // ========================================

        String jsonText = preferences.getString(
                KEY_REVIEWS,
                "[]"
        );

        if (jsonText == null) {

            throw new JSONException(
                    "Dữ liệu đánh giá không hợp lệ."
            );
        }

        // ========================================
        // 9.2. CHUYỂN SANG JSON ARRAY
        // ========================================

        JSONArray jsonArray =
                new JSONArray(jsonText);

        // ========================================
        // 9.3. ĐỌC TỪNG ĐÁNH GIÁ
        // ========================================

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject item =
                    jsonArray.getJSONObject(i);

            Review review = new Review(
                    item.getString("reviewId"),
                    item.getString("bookingCode"),
                    item.getString("customerId"),
                    item.getString("barberId"),
                    item.getString("serviceId"),
                    item.getInt("barberRating"),
                    item.getInt("serviceRating"),
                    item.getString("comment"),
                    item.getLong("createdAtMillis")
            );

            reviews.add(review);
        }

        return reviews;
    }

    // ========================================
    // 10. LƯU TOÀN BỘ DANH SÁCH REVIEW
    // ========================================

    private static boolean saveAllReviews(
            SharedPreferences preferences,
            List<Review> reviews
    ) throws JSONException {

        // ========================================
        // 10.1. TẠO JSON ARRAY
        // ========================================

        JSONArray jsonArray = new JSONArray();

        // ========================================
        // 10.2. CHUYỂN REVIEW SANG JSON
        // ========================================

        for (Review review : reviews) {

            JSONObject item = new JSONObject();

            item.put(
                    "reviewId",
                    review.getReviewId()
            );

            item.put(
                    "bookingCode",
                    review.getBookingCode()
            );

            item.put(
                    "customerId",
                    review.getCustomerId()
            );

            item.put(
                    "barberId",
                    review.getBarberId()
            );

            item.put(
                    "serviceId",
                    review.getServiceId()
            );

            item.put(
                    "barberRating",
                    review.getBarberRating()
            );

            item.put(
                    "serviceRating",
                    review.getServiceRating()
            );

            item.put(
                    "comment",
                    review.getComment()
            );

            item.put(
                    "createdAtMillis",
                    review.getCreatedAtMillis()
            );

            jsonArray.put(item);
        }

        // ========================================
        // 10.3. GHI VÀO SHAREDPREFERENCES
        // ========================================

        // commit() trả về kết quả thành công
        // hoặc thất bại khi ghi dữ liệu.
        //
        // Activity phải gọi thao tác ghi
        // trên luồng riêng, không dùng Main Thread.

        return preferences.edit()
                .putString(
                        KEY_REVIEWS,
                        jsonArray.toString()
                )
                .commit();
    }

    // ========================================
    // 11. CHUẨN HÓA CHUỖI
    // ========================================

    private static String normalizeValue(
            String value
    ) {

        if (value == null) {

            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
