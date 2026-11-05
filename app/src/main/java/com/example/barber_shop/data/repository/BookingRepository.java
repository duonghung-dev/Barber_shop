
package com.example.barber_shop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Booking;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.model.Notification;
import com.example.barber_shop.data.model.SalonService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BookingRepository {

    private static final String TAG = "BookingRepository";

    private static final String PREF_NAME =
            "barber_update_bookings";

    private static final String KEY_BOOKINGS =
            "bookings_json";

    private static final String KEY_LAST_BOOKING_NUMBER =
            "last_booking_number";

    private static final Object LOCK = new Object();

    private static final int MORNING_START = 8 * 60;
    private static final int MORNING_END = 12 * 60;

    private static final int AFTERNOON_START = 13 * 60;
    private static final int AFTERNOON_END = 18 * 60;

    private BookingRepository() {
    }

    public static class BookingResult {

        private final boolean success;
        private final String message;
        private final Booking booking;

        private BookingResult(
                boolean success,
                String message,
                Booking booking
        ) {
            this.success = success;
            this.message = message;
            this.booking = booking;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Booking getBooking() {
            return booking;
        }

        private static BookingResult success(
                String message,
                Booking booking
        ) {
            return new BookingResult(
                    true,
                    message,
                    booking
            );
        }

        private static BookingResult failure(
                String message
        ) {
            return new BookingResult(
                    false,
                    message,
                    null
            );
        }
    }

    public static BookingResult createBooking(
            Context context,
            String customerId,
            String branchId,
            String serviceId,
            String barberId,
            String bookingDate,
            String bookingTime
    ) {

        BookingResult result = createBookingInternal(
                context,
                customerId,
                branchId,
                serviceId,
                barberId,
                bookingDate,
                bookingTime
        );

        if (result.isSuccess()
                && result.getBooking() != null) {

            Booking booking = result.getBooking();

            createNotificationBestEffort(
                    context,
                    booking,
                    Notification.TYPE_BOOKING_CREATED,
                    "Đặt lịch thành công",
                    "Bạn đã đặt lịch "
                            + booking.getServiceName()
                            + " tại "
                            + booking.getBranchName()
                            + " vào "
                            + booking.getBookingDate()
                            + " lúc "
                            + booking.getBookingTime()
                            + ". Mã lịch hẹn: "
                            + booking.getBookingCode()
                            + "."
            );
        }

        return result;
    }

    private static BookingResult createBookingInternal(
            Context context,
            String customerId,
            String branchId,
            String serviceId,
            String barberId,
            String bookingDate,
            String bookingTime
    ) {

        if (context == null) {
            return BookingResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        branchId = normalizeValue(branchId);
        serviceId = normalizeValue(serviceId);
        barberId = normalizeValue(barberId);
        bookingDate = normalizeValue(bookingDate);
        bookingTime = normalizeValue(bookingTime);

        if (customerId == null
                || branchId == null
                || serviceId == null
                || barberId == null
                || bookingDate == null
                || bookingTime == null) {

            return BookingResult.failure(
                    "Thông tin đặt lịch chưa đầy đủ."
            );
        }

        Branch branch = BranchRepository.findById(
                branchId
        );

        if (branch == null) {
            return BookingResult.failure(
                    "Không tìm thấy chi nhánh đã chọn."
            );
        }

        SalonService service = ServiceRepository.findById(
                serviceId
        );

        if (service == null) {
            return BookingResult.failure(
                    "Không tìm thấy dịch vụ đã chọn."
            );
        }

        Barber barber = BarberRepository.findById(
                barberId
        );

        if (barber == null) {
            return BookingResult.failure(
                    "Không tìm thấy Barber đã chọn."
            );
        }

        String barberBranchId = normalizeValue(
                barber.getBranchId()
        );

        if (!branchId.equals(barberBranchId)) {
            return BookingResult.failure(
                    "Barber không thuộc chi nhánh đã chọn."
            );
        }

        if (!isServiceAvailableAtBranch(
                branchId,
                serviceId
        )) {
            return BookingResult.failure(
                    "Dịch vụ không có tại chi nhánh này."
            );
        }

        int durationMinutes = service.getDurationMinutes();

        if (!isValidBookingDateTime(
                bookingDate,
                bookingTime,
                durationMinutes
        )) {
            return BookingResult.failure(
                    "Ngày hoặc giờ đặt lịch không hợp lệ."
            );
        }

        long totalPrice = Math.round(
                service.getPriceVnd()
        );

        if (totalPrice < 0) {
            return BookingResult.failure(
                    "Giá dịch vụ không hợp lệ."
            );
        }

        synchronized (LOCK) {

            SharedPreferences preferences =
                    getPreferences(context);

            try {

                List<Booking> allBookings =
                        readAllBookings(preferences);

                if (hasTimeConflict(
                        allBookings,
                        barberId,
                        bookingDate,
                        bookingTime,
                        durationMinutes,
                        null
                )) {
                    return BookingResult.failure(
                            "Barber đã có lịch hẹn trong "
                                    + "khung giờ này. "
                                    + "Vui lòng chọn giờ khác."
                    );
                }

                long bookingNumber = preferences.getLong(
                        KEY_LAST_BOOKING_NUMBER,
                        0L
                );

                String bookingCode;

                do {

                    bookingNumber++;

                    bookingCode = String.format(
                            Locale.US,
                            "BK%06d",
                            bookingNumber
                    );

                } while (containsBookingCode(
                        allBookings,
                        bookingCode
                ));

                Booking booking = new Booking(
                        bookingCode,
                        customerId,
                        branchId,
                        serviceId,
                        barberId,
                        branch.getName(),
                        service.getName(),
                        barber.getName(),
                        bookingDate,
                        bookingTime,
                        durationMinutes,
                        totalPrice,
                        Booking.STATUS_PENDING
                );

                allBookings.add(booking);

                boolean saved = saveAllBookings(
                        preferences,
                        allBookings,
                        bookingNumber
                );

                if (!saved) {
                    return BookingResult.failure(
                            "Không thể lưu lịch hẹn. "
                                    + "Vui lòng thử lại."
                    );
                }

                return BookingResult.success(
                        "Đặt lịch thành công.",
                        booking
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return BookingResult.failure(
                        "Không thể xử lý dữ liệu lịch hẹn "
                                + "trên thiết bị."
                );
            }
        }
    }

    public static List<Booking> getBookingsForCustomer(
            Context context,
            String customerId
    ) {

        List<Booking> result = new ArrayList<>();

        if (context == null) {
            return result;
        }

        customerId = normalizeValue(customerId);

        if (customerId == null) {
            return result;
        }

        synchronized (LOCK) {

            try {

                List<Booking> allBookings =
                        readAllBookings(getPreferences(context));

                for (Booking booking : allBookings) {

                    if (customerId.equals(
                            booking.getCustomerId()
                    )) {
                        result.add(booking);
                    }
                }

                Collections.sort(
                        result,
                        (booking1, booking2) -> {

                            String dateTime1 =
                                    booking1.getBookingDate()
                                            + " "
                                            + booking1.getBookingTime();

                            String dateTime2 =
                                    booking2.getBookingDate()
                                            + " "
                                            + booking2.getBookingTime();

                            return dateTime2.compareTo(dateTime1);
                        }
                );

                return result;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc danh sách lịch hẹn.",
                        e
                );
            }
        }
    }

    public static List<Booking> getBookingsForBarber(
            Context context,
            String barberId
    ) {

        List<Booking> result = new ArrayList<>();

        if (context == null) {
            return result;
        }

        barberId = normalizeValue(barberId);

        if (barberId == null) {
            return result;
        }

        synchronized (LOCK) {

            try {

                List<Booking> allBookings =
                        readAllBookings(getPreferences(context));

                for (Booking booking : allBookings) {

                    if (barberId.equals(
                            booking.getBarberId()
                    )) {
                        result.add(booking);
                    }
                }

                Collections.sort(
                        result,
                        (booking1, booking2) -> {

                            String dateTime1 =
                                    booking1.getBookingDate()
                                            + " "
                                            + booking1.getBookingTime();

                            String dateTime2 =
                                    booking2.getBookingDate()
                                            + " "
                                            + booking2.getBookingTime();

                            return dateTime1.compareTo(dateTime2);
                        }
                );

                return result;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc danh sách lịch hẹn của Barber.",
                        e
                );
            }
        }
    }

    public static List<Booking> getBookingsForBarberOnDate(
            Context context,
            String barberId,
            String bookingDate
    ) {

        List<Booking> result = new ArrayList<>();

        bookingDate = normalizeValue(bookingDate);

        if (context == null || bookingDate == null) {
            return result;
        }

        List<Booking> barberBookings =
                getBookingsForBarber(
                        context,
                        barberId
                );

        for (Booking booking : barberBookings) {

            if (bookingDate.equals(
                    booking.getBookingDate()
            )) {
                result.add(booking);
            }
        }

        return result;
    }

    public static Booking findByCode(
            Context context,
            String bookingCode
    ) {

        if (context == null) {
            return null;
        }

        bookingCode = normalizeValue(bookingCode);

        if (bookingCode == null) {
            return null;
        }

        synchronized (LOCK) {

            try {

                List<Booking> allBookings =
                        readAllBookings(getPreferences(context));

                for (Booking booking : allBookings) {

                    if (bookingCode.equals(
                            booking.getBookingCode()
                    )) {
                        return booking;
                    }
                }

                return null;

            } catch (JSONException
                     | IllegalArgumentException e) {

                throw new IllegalStateException(
                        "Không thể đọc thông tin lịch hẹn.",
                        e
                );
            }
        }
    }

    public static BookingResult cancelBooking(
            Context context,
            String customerId,
            String bookingCode
    ) {

        BookingResult result = cancelBookingInternal(
                context,
                customerId,
                bookingCode
        );

        if (result.isSuccess()
                && result.getBooking() != null) {

            Booking booking = result.getBooking();

            createNotificationBestEffort(
                    context,
                    booking,
                    Notification.TYPE_BOOKING_CANCELLED,
                    "Lịch hẹn đã bị hủy",
                    "Lịch hẹn "
                            + booking.getBookingCode()
                            + " cho dịch vụ "
                            + booking.getServiceName()
                            + " vào "
                            + booking.getBookingDate()
                            + " lúc "
                            + booking.getBookingTime()
                            + " đã được hủy thành công."
            );
        }

        return result;
    }

    private static BookingResult cancelBookingInternal(
            Context context,
            String customerId,
            String bookingCode
    ) {

        if (context == null) {
            return BookingResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        bookingCode = normalizeValue(bookingCode);

        if (customerId == null || bookingCode == null) {
            return BookingResult.failure(
                    "Thông tin lịch hẹn không hợp lệ."
            );
        }

        synchronized (LOCK) {

            SharedPreferences preferences =
                    getPreferences(context);

            try {

                List<Booking> allBookings =
                        readAllBookings(preferences);

                for (Booking booking : allBookings) {

                    if (!bookingCode.equals(
                            booking.getBookingCode()
                    )) {
                        continue;
                    }

                    if (!customerId.equals(
                            booking.getCustomerId()
                    )) {
                        return BookingResult.failure(
                                "Bạn không có quyền hủy lịch này."
                        );
                    }

                    if (!booking.isActive()) {
                        return BookingResult.failure(
                                "Lịch hẹn không thể hủy "
                                        + "ở trạng thái hiện tại."
                        );
                    }

                    if (!isValidBookingDateTime(
                            booking.getBookingDate(),
                            booking.getBookingTime(),
                            booking.getDurationMinutes()
                    )) {
                        return BookingResult.failure(
                                "Lịch hẹn đã đến hoặc qua giờ "
                                        + "bắt đầu, không thể hủy; "
                                        + "hoặc dữ liệu thời gian "
                                        + "không hợp lệ."
                        );
                    }

                    booking.setStatus(
                            Booking.STATUS_CANCELLED
                    );

                    boolean saved = saveAllBookings(
                            preferences,
                            allBookings,
                            preferences.getLong(
                                    KEY_LAST_BOOKING_NUMBER,
                                    0L
                            )
                    );

                    if (!saved) {
                        return BookingResult.failure(
                                "Không thể lưu trạng thái hủy."
                        );
                    }

                    return BookingResult.success(
                            "Đã hủy lịch hẹn.",
                            booking
                    );
                }

                return BookingResult.failure(
                        "Không tìm thấy lịch hẹn."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return BookingResult.failure(
                        "Không thể xử lý yêu cầu hủy lịch."
                );
            }
        }
    }

    public static BookingResult rescheduleBooking(
            Context context,
            String customerId,
            String bookingCode,
            String newDate,
            String newTime
    ) {

        BookingResult result = rescheduleBookingInternal(
                context,
                customerId,
                bookingCode,
                newDate,
                newTime
        );

        if (!result.isSuccess()
                || result.getBooking() == null) {

            return result;
        }

        Booking booking = result.getBooking();

        String eventKey =
                booking.getBookingCode()
                        + ":RESCHEDULE:"
                        + UUID.randomUUID();

        String title = "Đổi lịch thành công";

        String message =
                "Lịch hẹn "
                        + booking.getBookingCode()
                        + " đã được chuyển sang "
                        + booking.getBookingDate()
                        + " lúc "
                        + booking.getBookingTime()
                        + ". Dịch vụ: "
                        + booking.getServiceName()
                        + ". Barber: "
                        + booking.getBarberName()
                        + ".";

        createNotificationBestEffort(
                context,
                booking,
                Notification.TYPE_BOOKING_RESCHEDULED,
                title,
                message,
                eventKey
        );

        return result;
    }

    private static BookingResult rescheduleBookingInternal(
            Context context,
            String customerId,
            String bookingCode,
            String newDate,
            String newTime
    ) {

        if (context == null) {
            return BookingResult.failure(
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        bookingCode = normalizeValue(bookingCode);
        newDate = normalizeValue(newDate);
        newTime = normalizeValue(newTime);

        if (customerId == null
                || bookingCode == null
                || newDate == null
                || newTime == null) {

            return BookingResult.failure(
                    "Thông tin đổi lịch chưa đầy đủ."
            );
        }

        synchronized (LOCK) {

            SharedPreferences preferences =
                    getPreferences(context);

            try {

                List<Booking> allBookings =
                        readAllBookings(preferences);

                for (Booking booking : allBookings) {

                    if (!bookingCode.equals(
                            booking.getBookingCode()
                    )) {
                        continue;
                    }

                    if (!customerId.equals(
                            booking.getCustomerId()
                    )) {
                        return BookingResult.failure(
                                "Bạn không có quyền đổi lịch này."
                        );
                    }

                    if (!booking.isActive()) {
                        return BookingResult.failure(
                                "Lịch hẹn không thể thay đổi "
                                        + "ở trạng thái hiện tại."
                        );
                    }

                    if (!isValidBookingDateTime(
                            booking.getBookingDate(),
                            booking.getBookingTime(),
                            booking.getDurationMinutes()
                    )) {
                        return BookingResult.failure(
                                "Lịch hẹn đã qua giờ bắt đầu "
                                        + "hoặc thời gian cũ không hợp lệ."
                        );
                    }

                    if (!isValidBookingDateTime(
                            newDate,
                            newTime,
                            booking.getDurationMinutes()
                    )) {
                        return BookingResult.failure(
                                "Ngày hoặc giờ mới không hợp lệ."
                        );
                    }

                    if (newDate.equals(
                            booking.getBookingDate()
                    ) && newTime.equals(
                            booking.getBookingTime()
                    )) {
                        return BookingResult.failure(
                                "Vui lòng chọn ngày hoặc giờ khác "
                                        + "với lịch hẹn hiện tại."
                        );
                    }

                    if (hasTimeConflict(
                            allBookings,
                            booking.getBarberId(),
                            newDate,
                            newTime,
                            booking.getDurationMinutes(),
                            bookingCode
                    )) {
                        return BookingResult.failure(
                                "Barber đã có lịch hẹn "
                                        + "trong khung giờ mới."
                        );
                    }

                    booking.updateBookingDateTime(
                            newDate,
                            newTime
                    );

                    boolean saved = saveAllBookings(
                            preferences,
                            allBookings,
                            preferences.getLong(
                                    KEY_LAST_BOOKING_NUMBER,
                                    0L
                            )
                    );

                    if (!saved) {
                        return BookingResult.failure(
                                "Không thể lưu thời gian mới."
                        );
                    }

                    return BookingResult.success(
                            "Đổi lịch thành công.",
                            booking
                    );
                }

                return BookingResult.failure(
                        "Không tìm thấy lịch hẹn."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return BookingResult.failure(
                        "Không thể xử lý yêu cầu đổi lịch."
                );
            }
        }
    }

    private static boolean isServiceAvailableAtBranch(
            String branchId,
            String serviceId
    ) {

        List<SalonService> services =
                ServiceRepository.getServicesForBranch(
                        branchId
                );

        if (services == null) {
            return false;
        }

        for (SalonService service : services) {

            if (service != null
                    && serviceId.equals(
                    service.getId()
            )) {
                return true;
            }
        }

        return false;
    }

    private static boolean isValidBookingDateTime(
            String bookingDate,
            String bookingTime,
            int durationMinutes
    ) {

        if (bookingDate == null
                || bookingTime == null
                || durationMinutes <= 0) {

            return false;
        }

        if (!bookingDate.matches(
                "[0-9]{4}-[0-9]{2}-[0-9]{2}"
        )) {
            return false;
        }

        if (!bookingTime.matches(
                "([01][0-9]|2[0-3]):[0-5][0-9]"
        )) {
            return false;
        }

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        formatter.setLenient(false);

        Date selectedDateTime;

        try {

            selectedDateTime = formatter.parse(
                    bookingDate + " " + bookingTime
            );

        } catch (ParseException e) {
            return false;
        }

        if (selectedDateTime == null) {
            return false;
        }

        if (selectedDateTime.getTime()
                <= System.currentTimeMillis()) {
            return false;
        }

        int startMinutes = timeToMinutes(
                bookingTime
        );

        if (startMinutes < 0) {
            return false;
        }

        int minute = startMinutes % 60;

        if (minute != 0 && minute != 30) {
            return false;
        }

        long endMinutes =
                (long) startMinutes + durationMinutes;

        boolean validMorning =
                startMinutes >= MORNING_START
                        && endMinutes <= MORNING_END;

        boolean validAfternoon =
                startMinutes >= AFTERNOON_START
                        && endMinutes <= AFTERNOON_END;

        return validMorning || validAfternoon;
    }

    private static boolean hasTimeConflict(
            List<Booking> allBookings,
            String barberId,
            String bookingDate,
            String bookingTime,
            int durationMinutes,
            String excludedBookingCode
    ) {

        int newStart = timeToMinutes(
                bookingTime
        );

        int newEnd = newStart + durationMinutes;

        for (Booking existing : allBookings) {

            if (excludedBookingCode != null
                    && excludedBookingCode.equals(
                    existing.getBookingCode()
            )) {
                continue;
            }

            if (!barberId.equals(
                    existing.getBarberId()
            )) {
                continue;
            }

            if (!bookingDate.equals(
                    existing.getBookingDate()
            )) {
                continue;
            }

            if (!existing.isActive()) {
                continue;
            }

            int existingStart = timeToMinutes(
                    existing.getBookingTime()
            );

            int existingDuration =
                    existing.getDurationMinutes();

            if (existingStart < 0
                    || existingDuration <= 0) {

                throw new IllegalStateException(
                        "Dữ liệu thời gian lịch hẹn cũ không hợp lệ."
                );
            }

            int existingEnd =
                    existingStart + existingDuration;

            boolean overlapping =
                    newStart < existingEnd
                            && existingStart < newEnd;

            if (overlapping) {
                return true;
            }
        }

        return false;
    }

    private static int timeToMinutes(String time) {

        if (time == null
                || !time.matches(
                "([01][0-9]|2[0-3]):[0-5][0-9]"
        )) {
            return -1;
        }

        String[] parts = time.split(":");

        int hour = Integer.parseInt(
                parts[0]
        );

        int minute = Integer.parseInt(
                parts[1]
        );

        return hour * 60 + minute;
    }

    private static boolean containsBookingCode(
            List<Booking> bookings,
            String bookingCode
    ) {

        for (Booking booking : bookings) {

            if (bookingCode.equals(
                    booking.getBookingCode()
            )) {
                return true;
            }
        }

        return false;
    }

    private static SharedPreferences getPreferences(
            Context context
    ) {

        return context.getApplicationContext()
                .getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );
    }

    private static List<Booking> readAllBookings(
            SharedPreferences preferences
    ) throws JSONException {

        List<Booking> bookings = new ArrayList<>();

        String jsonText = preferences.getString(
                KEY_BOOKINGS,
                "[]"
        );

        if (jsonText == null) {
            throw new JSONException(
                    "Không có dữ liệu JSON hợp lệ."
            );
        }

        JSONArray jsonArray = new JSONArray(
                jsonText
        );

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject item = jsonArray.getJSONObject(
                    i
            );

            Booking booking = new Booking(
                    item.getString("bookingCode"),
                    item.getString("customerId"),
                    item.getString("branchId"),
                    item.getString("serviceId"),
                    item.getString("barberId"),
                    item.getString("branchName"),
                    item.getString("serviceName"),
                    item.getString("barberName"),
                    item.getString("bookingDate"),
                    item.getString("bookingTime"),
                    item.getInt("durationMinutes"),
                    item.getLong("totalPrice"),
                    item.getString("status")
            );

            bookings.add(booking);
        }

        return bookings;
    }

    private static boolean saveAllBookings(
            SharedPreferences preferences,
            List<Booking> bookings,
            long lastBookingNumber
    ) throws JSONException {

        JSONArray jsonArray = new JSONArray();

        for (Booking booking : bookings) {

            JSONObject item = new JSONObject();

            item.put(
                    "bookingCode",
                    booking.getBookingCode()
            );

            item.put(
                    "customerId",
                    booking.getCustomerId()
            );

            item.put(
                    "branchId",
                    booking.getBranchId()
            );

            item.put(
                    "serviceId",
                    booking.getServiceId()
            );

            item.put(
                    "barberId",
                    booking.getBarberId()
            );

            item.put(
                    "branchName",
                    booking.getBranchName()
            );

            item.put(
                    "serviceName",
                    booking.getServiceName()
            );

            item.put(
                    "barberName",
                    booking.getBarberName()
            );

            item.put(
                    "bookingDate",
                    booking.getBookingDate()
            );

            item.put(
                    "bookingTime",
                    booking.getBookingTime()
            );

            item.put(
                    "durationMinutes",
                    booking.getDurationMinutes()
            );

            item.put(
                    "totalPrice",
                    booking.getTotalPrice()
            );

            item.put(
                    "status",
                    booking.getStatus()
            );

            jsonArray.put(item);
        }

        return preferences.edit()
                .putString(
                        KEY_BOOKINGS,
                        jsonArray.toString()
                )
                .putLong(
                        KEY_LAST_BOOKING_NUMBER,
                        lastBookingNumber
                )
                .commit();
    }

    private static void createNotificationBestEffort(
            Context context,
            Booking booking,
            String type,
            String title,
            String message
    ) {

        createNotificationBestEffort(
                context,
                booking,
                type,
                title,
                message,
                null
        );
    }

    private static void createNotificationBestEffort(
            Context context,
            Booking booking,
            String type,
            String title,
            String message,
            String eventKey
    ) {

        if (context == null || booking == null) {
            return;
        }

        try {

            NotificationRepository.NotificationResult
                    notificationResult;

            if (eventKey == null) {

                notificationResult =
                        NotificationRepository
                                .createNotification(
                                        context,
                                        booking.getCustomerId(),
                                        booking.getBookingCode(),
                                        type,
                                        title,
                                        message
                                );

            } else {

                notificationResult =
                        NotificationRepository
                                .createNotification(
                                        context,
                                        booking.getCustomerId(),
                                        booking.getBookingCode(),
                                        type,
                                        title,
                                        message,
                                        eventKey
                                );
            }

            if (!notificationResult.isSuccess()) {

                Log.w(
                        TAG,
                        "Booking đã lưu nhưng thông báo chưa lưu: "
                                + notificationResult.getMessage()
                );
            }

        } catch (RuntimeException e) {

            Log.w(
                    TAG,
                    "Không thể lưu thông báo của Booking.",
                    e
            );
        }
    }

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
