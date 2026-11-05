
package com.example.barber_shop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.model.Notification;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class NotificationRepository {

    // ========================================
    // 1. CẤU HÌNH LƯU TRỮ
    // ========================================

    private static final String PREF_NAME =
            "barber_update_notifications";

    private static final String KEY_NOTIFICATIONS =
            "notifications_json";

    // Khóa đồng bộ đọc và ghi.
    //
    // Mọi thao tác thay đổi danh sách thông báo
    // phải được thực hiện bên trong LOCK.

    private static final Object LOCK = new Object();

    // ========================================
    // 2. CONSTRUCTOR
    // ========================================

    private NotificationRepository() {

        // Repository sử dụng phương thức static.
        // Không cần khởi tạo đối tượng.
    }

    // ========================================
    // 3. CLASS TRẢ KẾT QUẢ THAO TÁC
    // ========================================

    public static class NotificationResult {

        // Trạng thái thực hiện
        private final boolean success;

        // Thông báo kết quả
        private final String message;

        // Notification liên quan
        private final Notification notification;

        // Số lượng thông báo được thay đổi
        private final int affectedCount;

        // ========================================
        // 3.1. CONSTRUCTOR
        // ========================================

        private NotificationResult(
                boolean success,
                String message,
                Notification notification,
                int affectedCount
        ) {

            this.success = success;
            this.message = message;
            this.notification = notification;
            this.affectedCount = affectedCount;
        }

        // ========================================
        // 3.2. KIỂM TRA KẾT QUẢ
        // ========================================

        public boolean isSuccess() {

            return success;
        }

        // ========================================
        // 3.3. LẤY THÔNG BÁO KẾT QUẢ
        // ========================================

        public String getMessage() {

            return message;
        }

        // ========================================
        // 3.4. LẤY NOTIFICATION
        // ========================================

        public Notification getNotification() {

            return notification;
        }

        // ========================================
        // 3.5. LẤY SỐ LƯỢNG ĐÃ THAY ĐỔI
        // ========================================

        public int getAffectedCount() {

            return affectedCount;
        }

        // ========================================
        // 3.6. TẠO KẾT QUẢ THÀNH CÔNG
        // ========================================

        private static NotificationResult success(
                String message,
                Notification notification,
                int affectedCount
        ) {

            return new NotificationResult(
                    true,
                    message,
                    notification,
                    affectedCount
            );
        }

        // ========================================
        // 3.7. TẠO KẾT QUẢ THẤT BẠI
        // ========================================

        private static NotificationResult failure(
                String message
        ) {

            return new NotificationResult(
                    false,
                    message,
                    null,
                    0
            );
        }
    }

    // ========================================
    // 4. TẠO THÔNG BÁO - PHƯƠNG THỨC CŨ
    // ========================================

    // Giữ nguyên 6 tham số.
    //
    // BookingRepository hiện tại đang gọi
    // phương thức này khi:
    // - Customer đặt lịch thành công
    // - Customer hủy lịch thành công
    //
    // Vì vậy chúng ta không được xóa hoặc
    // thay đổi chữ ký phương thức.
    //
    // Khi không truyền eventKey, Repository
    // vẫn chống trùng theo:
    // Customer + Booking + Loại thông báo.

    public static NotificationResult createNotification(
            Context context,
            String customerId,
            String bookingCode,
            String type,
            String title,
            String message
    ) {

        return createNotificationInternal(
                context,
                customerId,
                bookingCode,
                type,
                title,
                message,
                null
        );
    }

    // ========================================
    // 5. TẠO THÔNG BÁO CÓ EVENT KEY - MỚI
    // ========================================

    // Sử dụng khi cần phân biệt từng sự kiện.
    //
    // Ví dụ:
    // - Đổi lịch lần thứ nhất
    // - Đổi lịch lần thứ hai
    // - Nhắc lịch cho thời gian hẹn mới
    //
    // eventKey phải ổn định khi xử lý lại
    // cùng một sự kiện, nhưng khác nhau
    // giữa các sự kiện độc lập.

    public static NotificationResult createNotification(
            Context context,
            String customerId,
            String bookingCode,
            String type,
            String title,
            String message,
            String eventKey
    ) {

        return createNotificationInternal(
                context,
                customerId,
                bookingCode,
                type,
                title,
                message,
                eventKey
        );
    }

    // ========================================
    // 6. XỬ LÝ TẠO THÔNG BÁO CHUNG
    // ========================================

    private static NotificationResult createNotificationInternal(
            Context context,
            String customerId,
            String bookingCode,
            String type,
            String title,
            String message,
            String eventKey
    ) {

        // ========================================
        // 6.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return NotificationResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 6.2. CHUẨN HÓA DỮ LIỆU
        // ========================================

        customerId = normalizeValue(customerId);
        bookingCode = normalizeValue(bookingCode);
        type = normalizeValue(type);
        title = normalizeValue(title);
        message = normalizeValue(message);

        // STT 21 MỞ RỘNG
        eventKey = normalizeValue(eventKey);

        // ========================================
        // 6.3. KIỂM TRA THÔNG TIN BẮT BUỘC
        // ========================================

        if (customerId == null
                || bookingCode == null
                || title == null
                || message == null) {

            return NotificationResult.failure(
                    "Thông tin thông báo chưa đầy đủ."
            );
        }

        // ========================================
        // 6.4. KIỂM TRA LOẠI THÔNG BÁO
        // ========================================

        if (!Notification.isValidType(type)) {

            return NotificationResult.failure(
                    "Loại thông báo không hợp lệ."
            );
        }

        // ========================================
        // 6.5. KIỂM TRA BOOKING TỒN TẠI
        // ========================================

        // Đọc Booking trước khi lấy LOCK của
        // NotificationRepository.
        //
        // Tránh giữ đồng thời khóa Notification
        // trong lúc chờ khóa Booking.

        final Booking booking;

        try {

            booking = BookingRepository.findByCode(
                    context,
                    bookingCode
            );

        } catch (RuntimeException e) {

            return NotificationResult.failure(
                    "Không thể kiểm tra lịch hẹn liên quan."
            );
        }

        if (booking == null) {

            return NotificationResult.failure(
                    "Không tìm thấy lịch hẹn liên quan."
            );
        }

        // ========================================
        // 6.6. KIỂM TRA CHỦ SỞ HỮU BOOKING
        // ========================================

        if (!customerId.equals(
                booking.getCustomerId()
        )) {

            return NotificationResult.failure(
                    "Customer không sở hữu lịch hẹn này."
            );
        }

        // ========================================
        // 6.7. ĐỒNG BỘ THAO TÁC ĐỌC VÀ GHI
        // ========================================

        synchronized (LOCK) {

            try {

                // ========================================
                // 6.8. ĐỌC DANH SÁCH THÔNG BÁO
                // ========================================

                SharedPreferences preferences =
                        getPreferences(context);

                List<Notification> allNotifications =
                        readAllNotifications(preferences);

                // ========================================
                // 6.9. KIỂM TRA THÔNG BÁO TRÙNG
                // ========================================

                Notification existingNotification =
                        findDuplicateNotification(
                                allNotifications,
                                customerId,
                                bookingCode,
                                type,
                                eventKey
                        );

                if (existingNotification != null) {

                    // Trả về thông báo đã có.
                    //
                    // Không thêm bản ghi trùng
                    // và không đổi trạng thái đã đọc.

                    return NotificationResult.success(
                            "Thông báo đã tồn tại.",
                            existingNotification,
                            0
                    );
                }

                // ========================================
                // 6.10. TẠO NOTIFICATION ID
                // ========================================

                String notificationId =
                        "NT-" + UUID.randomUUID();

                // ========================================
                // 6.11. TẠO NOTIFICATION MỚI
                // ========================================

                Notification newNotification =
                        new Notification(
                                notificationId,
                                customerId,
                                bookingCode,
                                type,
                                title,
                                message,
                                System.currentTimeMillis(),
                                false,
                                eventKey
                        );

                // ========================================
                // 6.12. THÊM VÀO DANH SÁCH
                // ========================================

                allNotifications.add(
                        newNotification
                );

                // ========================================
                // 6.13. LƯU XUỐNG SHAREDPREFERENCES
                // ========================================

                boolean saved =
                        saveAllNotifications(
                                preferences,
                                allNotifications
                        );

                if (!saved) {

                    return NotificationResult.failure(
                            "Không thể lưu thông báo."
                    );
                }

                // ========================================
                // 6.14. TRẢ KẾT QUẢ THÀNH CÔNG
                // ========================================

                return NotificationResult.success(
                        "Tạo thông báo thành công.",
                        newNotification,
                        1
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return NotificationResult.failure(
                        "Không thể tạo thông báo. "
                                + "Vui lòng thử lại."
                );
            }
        }
    }

    // ========================================
    // 7. KIỂM TRA THÔNG BÁO TRÙNG - MỚI
    // ========================================

    private static Notification findDuplicateNotification(
            List<Notification> notifications,
            String customerId,
            String bookingCode,
            String type,
            String eventKey
    ) {

        // ========================================
        // 7.1. PHÂN LOẠI SỰ KIỆN
        // ========================================

        boolean oneTimeType =
                isOneTimeNotificationType(type);

        // ========================================
        // 7.2. DUYỆT DANH SÁCH
        // ========================================

        for (Notification notification
                : notifications) {

            // ========================================
            // 7.3. KIỂM TRA CUSTOMER
            // ========================================

            if (!customerId.equals(
                    notification.getCustomerId()
            )) {

                continue;
            }

            // ========================================
            // 7.4. KIỂM TRA BOOKING
            // ========================================

            if (!bookingCode.equals(
                    notification.getBookingCode()
            )) {

                continue;
            }

            // ========================================
            // 7.5. KIỂM TRA LOẠI THÔNG BÁO
            // ========================================

            if (!type.equals(
                    notification.getType()
            )) {

                continue;
            }

            // ========================================
            // 7.6. SỰ KIỆN CHỈ XẢY RA MỘT LẦN
            // ========================================

            // Với CREATED, CONFIRMED,
            // CANCELLED, COMPLETED:
            //
            // Chỉ cho phép một thông báo
            // cho mỗi Customer + Booking + Type.
            //
            // Không phụ thuộc eventKey.

            if (oneTimeType) {

                return notification;
            }

            // ========================================
            // 7.7. SỰ KIỆN KHÔNG CÓ EVENT KEY
            // ========================================

            // Giữ hành vi cũ cho phương thức
            // createNotification() 6 tham số.
            //
            // Khi không cung cấp eventKey,
            // chỉ tạo một thông báo cho mỗi loại.
            //
            // Muốn tạo nhiều thông báo đổi lịch
            // hoặc nhắc lịch thì phải truyền
            // eventKey khác nhau.

            if (eventKey == null) {

                return notification;
            }

            // ========================================
            // 7.8. SO SÁNH EVENT KEY
            // ========================================

            String existingEventKey =
                    notification.getEventKey();

            // Cùng eventKey nghĩa là cùng
            // một sự kiện đã được xử lý trước đó.

            if (eventKey.equals(existingEventKey)) {

                return notification;
            }
        }

        // ========================================
        // 7.9. KHÔNG CÓ THÔNG BÁO TRÙNG
        // ========================================

        return null;
    }

    // ========================================
    // 8. KIỂM TRA LOẠI THÔNG BÁO MỘT LẦN
    // ========================================

    private static boolean isOneTimeNotificationType(
            String type
    ) {

        // Các loại này chỉ tạo tối đa một
        // thông báo cho mỗi Booking.
        //
        // Quy tắc hiện tại của FE:
        // - CREATED
        // - CONFIRMED
        // - CANCELLED
        // - COMPLETED

        return Notification.TYPE_BOOKING_CREATED.equals(type)
                || Notification.TYPE_BOOKING_CONFIRMED.equals(type)
                || Notification.TYPE_BOOKING_CANCELLED.equals(type)
                || Notification.TYPE_BOOKING_COMPLETED.equals(type);
    }

    // ========================================
    // 9. LẤY TẤT CẢ THÔNG BÁO CỦA CUSTOMER
    // ========================================

    public static List<Notification>
    getNotificationsForCustomer(
            Context context,
            String customerId
    ) {

        List<Notification> result =
                new ArrayList<>();

        // ========================================
        // 9.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {

            return result;
        }

        customerId = normalizeValue(customerId);

        if (customerId == null) {

            return result;
        }

        // ========================================
        // 9.2. ĐỌC DỮ LIỆU
        // ========================================

        synchronized (LOCK) {

            try {

                List<Notification> allNotifications =
                        readAllNotifications(
                                getPreferences(context)
                        );

                // ========================================
                // 9.3. LỌC THEO CUSTOMER
                // ========================================

                for (Notification notification
                        : allNotifications) {

                    if (customerId.equals(
                            notification.getCustomerId()
                    )) {

                        result.add(notification);
                    }
                }

                // ========================================
                // 9.4. SẮP XẾP MỚI NHẤT TRƯỚC
                // ========================================

                Collections.sort(
                        result,
                        (notification1, notification2) ->
                                Long.compare(
                                        notification2.getCreatedAtMillis(),
                                        notification1.getCreatedAtMillis()
                                )
                );

                return result;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc danh sách thông báo.",
                        e
                );
            }
        }
    }

    // ========================================
    // 10. LẤY THÔNG BÁO CHƯA ĐỌC
    // ========================================

    public static List<Notification>
    getUnreadNotificationsForCustomer(
            Context context,
            String customerId
    ) {

        // ========================================
        // 10.1. LẤY DANH SÁCH CUSTOMER
        // ========================================

        List<Notification> allNotifications =
                getNotificationsForCustomer(
                        context,
                        customerId
                );

        List<Notification> unreadNotifications =
                new ArrayList<>();

        // ========================================
        // 10.2. LỌC TRẠNG THÁI CHƯA ĐỌC
        // ========================================

        for (Notification notification
                : allNotifications) {

            if (!notification.isRead()) {

                unreadNotifications.add(
                        notification
                );
            }
        }

        return unreadNotifications;
    }

    // ========================================
    // 11. ĐẾM THÔNG BÁO CHƯA ĐỌC
    // ========================================

    public static int getUnreadCount(
            Context context,
            String customerId
    ) {

        List<Notification> unreadNotifications =
                getUnreadNotificationsForCustomer(
                        context,
                        customerId
                );

        return unreadNotifications.size();
    }

    // ========================================
    // 12. ĐÁNH DẤU MỘT THÔNG BÁO ĐÃ ĐỌC
    // ========================================

    public static NotificationResult markAsRead(
            Context context,
            String customerId,
            String notificationId
    ) {

        // ========================================
        // 12.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return NotificationResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 12.2. CHUẨN HÓA THAM SỐ
        // ========================================

        customerId = normalizeValue(customerId);
        notificationId = normalizeValue(notificationId);

        if (customerId == null
                || notificationId == null) {

            return NotificationResult.failure(
                    "Thông tin thông báo không hợp lệ."
            );
        }

        // ========================================
        // 12.3. ĐỒNG BỘ THAO TÁC
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                List<Notification> allNotifications =
                        readAllNotifications(preferences);

                // ========================================
                // 12.4. TÌM THÔNG BÁO
                // ========================================

                for (int i = 0;
                     i < allNotifications.size();
                     i++) {

                    Notification notification =
                            allNotifications.get(i);

                    if (!notificationId.equals(
                            notification.getNotificationId()
                    )) {

                        continue;
                    }

                    // ========================================
                    // 12.5. KIỂM TRA CHỦ SỞ HỮU
                    // ========================================

                    if (!customerId.equals(
                            notification.getCustomerId()
                    )) {

                        return NotificationResult.failure(
                                "Bạn không có quyền cập nhật thông báo này."
                        );
                    }

                    // ========================================
                    // 12.6. KIỂM TRA TRẠNG THÁI
                    // ========================================

                    if (notification.isRead()) {

                        return NotificationResult.success(
                                "Thông báo đã được đọc trước đó.",
                                notification,
                                0
                        );
                    }

                    // ========================================
                    // 12.7. TẠO PHIÊN BẢN ĐÃ ĐỌC
                    // ========================================

                    // Notification.markAsRead()
                    // giữ nguyên eventKey,
                    // notificationId và dữ liệu khác.

                    Notification updatedNotification =
                            notification.markAsRead();

                    // ========================================
                    // 12.8. THAY THẾ TRONG DANH SÁCH
                    // ========================================

                    allNotifications.set(
                            i,
                            updatedNotification
                    );

                    // ========================================
                    // 12.9. LƯU TRẠNG THÁI MỚI
                    // ========================================

                    boolean saved =
                            saveAllNotifications(
                                    preferences,
                                    allNotifications
                            );

                    if (!saved) {

                        return NotificationResult.failure(
                                "Không thể cập nhật trạng thái thông báo."
                        );
                    }

                    // ========================================
                    // 12.10. TRẢ KẾT QUẢ
                    // ========================================

                    return NotificationResult.success(
                            "Đã đánh dấu thông báo là đã đọc.",
                            updatedNotification,
                            1
                    );
                }

                // ========================================
                // 12.11. KHÔNG TÌM THẤY
                // ========================================

                return NotificationResult.failure(
                        "Không tìm thấy thông báo."
                );

            } catch (JSONException
                     | IllegalArgumentException e) {

                return NotificationResult.failure(
                        "Không thể cập nhật thông báo."
                );
            }
        }
    }

    // ========================================
    // 13. ĐÁNH DẤU TẤT CẢ THÔNG BÁO ĐÃ ĐỌC
    // ========================================

    public static NotificationResult markAllAsRead(
            Context context,
            String customerId
    ) {

        // ========================================
        // 13.1. KIỂM TRA CONTEXT
        // ========================================

        if (context == null) {

            return NotificationResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        // ========================================
        // 13.2. KIỂM TRA CUSTOMER
        // ========================================

        customerId = normalizeValue(customerId);

        if (customerId == null) {

            return NotificationResult.failure(
                    "Thông tin Customer không hợp lệ."
            );
        }

        // ========================================
        // 13.3. ĐỒNG BỘ THAO TÁC
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                List<Notification> allNotifications =
                        readAllNotifications(preferences);

                int updatedCount = 0;

                // ========================================
                // 13.4. DUYỆT DANH SÁCH
                // ========================================

                for (int i = 0;
                     i < allNotifications.size();
                     i++) {

                    Notification notification =
                            allNotifications.get(i);

                    // ========================================
                    // 13.5. BỎ QUA CUSTOMER KHÁC
                    // ========================================

                    if (!customerId.equals(
                            notification.getCustomerId()
                    )) {

                        continue;
                    }

                    // ========================================
                    // 13.6. BỎ QUA THÔNG BÁO ĐÃ ĐỌC
                    // ========================================

                    if (notification.isRead()) {

                        continue;
                    }

                    // ========================================
                    // 13.7. TẠO PHIÊN BẢN ĐÃ ĐỌC
                    // ========================================

                    Notification updatedNotification =
                            notification.markAsRead();

                    allNotifications.set(
                            i,
                            updatedNotification
                    );

                    updatedCount++;
                }

                // ========================================
                // 13.8. KHÔNG CÓ GÌ CẦN CẬP NHẬT
                // ========================================

                if (updatedCount == 0) {

                    return NotificationResult.success(
                            "Không có thông báo chưa đọc.",
                            null,
                            0
                    );
                }

                // ========================================
                // 13.9. LƯU DANH SÁCH
                // ========================================

                boolean saved =
                        saveAllNotifications(
                                preferences,
                                allNotifications
                        );

                if (!saved) {

                    return NotificationResult.failure(
                            "Không thể cập nhật danh sách thông báo."
                    );
                }

                // ========================================
                // 13.10. TRẢ KẾT QUẢ
                // ========================================

                return NotificationResult.success(
                        "Đã đánh dấu tất cả thông báo là đã đọc.",
                        null,
                        updatedCount
                );

            } catch (JSONException
                     | IllegalArgumentException e) {

                return NotificationResult.failure(
                        "Không thể cập nhật thông báo."
                );
            }
        }
    }

    // ========================================
    // 14. LẤY SHAREDPREFERENCES
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
    // 15. ĐỌC TOÀN BỘ NOTIFICATION TỪ JSON
    // ========================================

    private static List<Notification>
    readAllNotifications(
            SharedPreferences preferences
    ) throws JSONException {

        List<Notification> notifications =
                new ArrayList<>();

        // ========================================
        // 15.1. LẤY CHUỖI JSON
        // ========================================

        String jsonText = preferences.getString(
                KEY_NOTIFICATIONS,
                "[]"
        );

        if (jsonText == null) {

            throw new JSONException(
                    "Dữ liệu thông báo không hợp lệ."
            );
        }

        // ========================================
        // 15.2. CHUYỂN SANG JSON ARRAY
        // ========================================

        JSONArray jsonArray =
                new JSONArray(jsonText);

        // ========================================
        // 15.3. DUYỆT TỪNG PHẦN TỬ
        // ========================================

        for (int i = 0;
             i < jsonArray.length();
             i++) {

            JSONObject item =
                    jsonArray.getJSONObject(i);

            // ========================================
            // 15.4. ĐỌC EVENT KEY - MỚI
            // ========================================

            // Dữ liệu cũ không có "eventKey".
            //
            // Nếu không có trường này hoặc
            // trường được lưu dưới dạng null,
            // chúng ta sử dụng giá trị null.
            //
            // Không xóa hay tạo lại dữ liệu cũ.

            String eventKey = null;

            if (item.has("eventKey")
                    && !item.isNull("eventKey")) {

                eventKey = normalizeValue(
                        item.getString("eventKey")
                );
            }

            // ========================================
            // 15.5. CHUYỂN JSON THÀNH NOTIFICATION
            // ========================================

            Notification notification =
                    new Notification(
                            item.getString("notificationId"),
                            item.getString("customerId"),
                            item.getString("bookingCode"),
                            item.getString("type"),
                            item.getString("title"),
                            item.getString("message"),
                            item.getLong("createdAtMillis"),
                            item.getBoolean("isRead"),
                            eventKey
                    );

            notifications.add(notification);
        }

        return notifications;
    }

    // ========================================
    // 16. LƯU TOÀN BỘ NOTIFICATION XUỐNG JSON
    // ========================================

    private static boolean saveAllNotifications(
            SharedPreferences preferences,
            List<Notification> notifications
    ) throws JSONException {

        // ========================================
        // 16.1. TẠO JSON ARRAY
        // ========================================

        JSONArray jsonArray =
                new JSONArray();

        // ========================================
        // 16.2. CHUYỂN TỪ MODEL SANG JSON
        // ========================================

        for (Notification notification
                : notifications) {

            JSONObject item =
                    new JSONObject();

            // ========================================
            // 16.3. THÔNG TIN ĐỊNH DANH
            // ========================================

            item.put(
                    "notificationId",
                    notification.getNotificationId()
            );

            item.put(
                    "customerId",
                    notification.getCustomerId()
            );

            item.put(
                    "bookingCode",
                    notification.getBookingCode()
            );

            // ========================================
            // 16.4. LOẠI THÔNG BÁO
            // ========================================

            item.put(
                    "type",
                    notification.getType()
            );

            // ========================================
            // 16.5. TIÊU ĐỀ VÀ NỘI DUNG
            // ========================================

            item.put(
                    "title",
                    notification.getTitle()
            );

            item.put(
                    "message",
                    notification.getMessage()
            );

            // ========================================
            // 16.6. THỜI GIAN TẠO
            // ========================================

            item.put(
                    "createdAtMillis",
                    notification.getCreatedAtMillis()
            );

            // ========================================
            // 16.7. TRẠNG THÁI ĐÃ ĐỌC
            // ========================================

            item.put(
                    "isRead",
                    notification.isRead()
            );

            // ========================================
            // 16.8. EVENT KEY - STT 21 MỞ RỘNG
            // ========================================

            // Chỉ ghi eventKey khi có giá trị.
            //
            // Với thông báo cũ không có eventKey,
            // giữ định dạng tương thích.

            if (notification.hasEventKey()) {

                item.put(
                        "eventKey",
                        notification.getEventKey()
                );
            }

            // ========================================
            // 16.9. THÊM VÀO JSON ARRAY
            // ========================================

            jsonArray.put(item);
        }

        // ========================================
        // 16.10. GHI VÀO SHAREDPREFERENCES
        // ========================================

        // commit() cho biết thao tác ghi
        // đã thành công hay chưa.
        //
        // Các phương thức ghi cần được gọi
        // trên luồng nền, không phải Main Thread.

        return preferences.edit()
                .putString(
                        KEY_NOTIFICATIONS,
                        jsonArray.toString()
                )
                .commit();
    }

    // ========================================
    // 17. CHUẨN HÓA CHUỖI
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

