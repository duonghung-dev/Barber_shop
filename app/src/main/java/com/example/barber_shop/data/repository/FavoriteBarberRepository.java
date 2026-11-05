
package com.example.barber_shop.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.barber_shop.data.model.Barber;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FavoriteBarberRepository {

    // ========================================
    // 1. CẤU HÌNH SHAREDPREFERENCES
    // ========================================

    private static final String PREF_NAME =
            "barber_update_favorite_barbers";

    private static final String KEY_FAVORITES =
            "favorite_barbers_by_customer_json";

    // Đồng bộ các thao tác đọc và ghi dữ liệu
    private static final Object LOCK = new Object();

    // ========================================
    // 2. KHÔNG CHO KHỞI TẠO REPOSITORY
    // ========================================

    private FavoriteBarberRepository() {

        // Các phương thức được sử dụng dưới dạng static.
        // Không cần tạo đối tượng Repository.
    }

    // ========================================
    // 3. CLASS TRẢ KẾT QUẢ THAO TÁC
    // ========================================

    public static class FavoriteResult {

        // Thao tác thành công hay thất bại
        private final boolean success;

        // Barber có đang được yêu thích sau thao tác?
        private final boolean favorite;

        // Nội dung thông báo
        private final String message;

        private FavoriteResult(
                boolean success,
                boolean favorite,
                String message
        ) {

            this.success = success;
            this.favorite = favorite;
            this.message = message;
        }

        // ========================================
        // 3.1. KIỂM TRA THÀNH CÔNG
        // ========================================

        public boolean isSuccess() {
            return success;
        }

        // ========================================
        // 3.2. LẤY TRẠNG THÁI YÊU THÍCH
        // ========================================

        public boolean isFavorite() {
            return favorite;
        }

        // ========================================
        // 3.3. LẤY THÔNG BÁO
        // ========================================

        public String getMessage() {
            return message;
        }

        // ========================================
        // 3.4. TẠO KẾT QUẢ THÀNH CÔNG
        // ========================================

        private static FavoriteResult success(
                boolean favorite,
                String message
        ) {

            return new FavoriteResult(
                    true,
                    favorite,
                    message
            );
        }

        // ========================================
        // 3.5. TẠO KẾT QUẢ THẤT BẠI
        // ========================================

        private static FavoriteResult failure(
                boolean favorite,
                String message
        ) {

            return new FavoriteResult(
                    false,
                    favorite,
                    message
            );
        }
    }

    // ========================================
    // 4. LẤY DANH SÁCH BARBER YÊU THÍCH
    // ========================================

    public static List<String> getFavoriteBarberIds(
            Context context,
            String customerId
    ) {

        List<String> result = new ArrayList<>();

        // ========================================
        // 4.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {
            return result;
        }

        customerId = normalizeValue(customerId);

        if (customerId == null) {
            return result;
        }

        // ========================================
        // 4.2. ĐỌC DỮ LIỆU AN TOÀN
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                JSONObject allFavorites =
                        readFavoritesObject(preferences);

                // Lấy danh sách của đúng Customer
                return readCustomerFavoriteIds(
                        allFavorites,
                        customerId
                );

            } catch (JSONException e) {

                // Không âm thầm trả danh sách trống
                // khi dữ liệu JSON đang bị lỗi.
                throw new IllegalStateException(
                        "Không thể đọc danh sách Barber yêu thích.",
                        e
                );
            }
        }
    }

    // ========================================
    // 5. KIỂM TRA BARBER CÓ ĐƯỢC YÊU THÍCH
    // ========================================

    public static boolean isFavorite(
            Context context,
            String customerId,
            String barberId
    ) {

        // ========================================
        // 5.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {
            return false;
        }

        customerId = normalizeValue(customerId);
        barberId = normalizeValue(barberId);

        if (customerId == null || barberId == null) {
            return false;
        }

        // ========================================
        // 5.2. LẤY DANH SÁCH YÊU THÍCH
        // ========================================

        List<String> favoriteIds =
                getFavoriteBarberIds(
                        context,
                        customerId
                );

        // ========================================
        // 5.3. KIỂM TRA BARBER ID
        // ========================================

        return favoriteIds.contains(barberId);
    }

    // ========================================
    // 6. THÊM BARBER VÀO YÊU THÍCH
    // ========================================

    public static FavoriteResult addFavorite(
            Context context,
            String customerId,
            String barberId
    ) {

        // ========================================
        // 6.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {

            return FavoriteResult.failure(
                    false,
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        barberId = normalizeValue(barberId);

        if (customerId == null || barberId == null) {

            return FavoriteResult.failure(
                    false,
                    "Thông tin Customer hoặc Barber không hợp lệ."
            );
        }

        // ========================================
        // 6.2. KIỂM TRA VÀ LƯU DỮ LIỆU
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                JSONObject allFavorites =
                        readFavoritesObject(preferences);

                List<String> favoriteIds =
                        readCustomerFavoriteIds(
                                allFavorites,
                                customerId
                        );

                // ========================================
                // 6.3. CHỐNG LƯU TRÙNG
                // ========================================

                if (favoriteIds.contains(barberId)) {

                    return FavoriteResult.success(
                            true,
                            "Barber đã có trong danh sách yêu thích."
                    );
                }

                // ========================================
                // 6.4. KIỂM TRA BARBER TỒN TẠI
                // ========================================

                Barber barber =
                        BarberRepository.findById(
                                barberId
                        );

                if (barber == null) {

                    return FavoriteResult.failure(
                            false,
                            "Không tìm thấy Barber đã chọn."
                    );
                }

                // ========================================
                // 6.5. THÊM BARBER ID
                // ========================================

                favoriteIds.add(barberId);

                // ========================================
                // 6.6. LƯU DANH SÁCH MỚI
                // ========================================

                boolean saved = saveCustomerFavorites(
                        preferences,
                        allFavorites,
                        customerId,
                        favoriteIds
                );

                if (!saved) {

                    return FavoriteResult.failure(
                            false,
                            "Không thể lưu Barber yêu thích."
                    );
                }

                // ========================================
                // 6.7. TRẢ KẾT QUẢ
                // ========================================

                return FavoriteResult.success(
                        true,
                        "Đã thêm Barber vào danh sách yêu thích."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return FavoriteResult.failure(
                        false,
                        "Không thể xử lý yêu cầu thêm yêu thích."
                );
            }
        }
    }

    // ========================================
    // 7. BỎ BARBER KHỎI DANH SÁCH YÊU THÍCH
    // ========================================

    public static FavoriteResult removeFavorite(
            Context context,
            String customerId,
            String barberId
    ) {

        // ========================================
        // 7.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {

            return FavoriteResult.failure(
                    false,
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        barberId = normalizeValue(barberId);

        if (customerId == null || barberId == null) {

            return FavoriteResult.failure(
                    false,
                    "Thông tin Customer hoặc Barber không hợp lệ."
            );
        }

        // ========================================
        // 7.2. ĐỌC DỮ LIỆU
        // ========================================

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                JSONObject allFavorites =
                        readFavoritesObject(preferences);

                List<String> favoriteIds =
                        readCustomerFavoriteIds(
                                allFavorites,
                                customerId
                        );

                // ========================================
                // 7.3. KIỂM TRA BARBER TRONG DANH SÁCH
                // ========================================

                if (!favoriteIds.contains(barberId)) {

                    return FavoriteResult.success(
                            false,
                            "Barber không có trong danh sách yêu thích."
                    );
                }

                // ========================================
                // 7.4. XÓA BARBER ID
                // ========================================

                favoriteIds.remove(barberId);

                // ========================================
                // 7.5. LƯU THAY ĐỔI
                // ========================================

                boolean saved = saveCustomerFavorites(
                        preferences,
                        allFavorites,
                        customerId,
                        favoriteIds
                );

                if (!saved) {

                    return FavoriteResult.failure(
                            true,
                            "Không thể xóa Barber khỏi yêu thích."
                    );
                }

                // ========================================
                // 7.6. TRẢ KẾT QUẢ
                // ========================================

                return FavoriteResult.success(
                        false,
                        "Đã bỏ Barber khỏi danh sách yêu thích."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return FavoriteResult.failure(
                        true,
                        "Không thể xử lý yêu cầu bỏ yêu thích."
                );
            }
        }
    }

    // ========================================
    // 8. CHUYỂN ĐỔI TRẠNG THÁI YÊU THÍCH
    // ========================================

    public static FavoriteResult toggleFavorite(
            Context context,
            String customerId,
            String barberId
    ) {

        // ========================================
        // 8.1. KIỂM TRA THAM SỐ
        // ========================================

        if (context == null) {

            return FavoriteResult.failure(
                    false,
                    "Không thể truy cập dữ liệu ứng dụng."
            );
        }

        customerId = normalizeValue(customerId);
        barberId = normalizeValue(barberId);

        if (customerId == null || barberId == null) {

            return FavoriteResult.failure(
                    false,
                    "Thông tin Customer hoặc Barber không hợp lệ."
            );
        }

        // ========================================
        // 8.2. ĐỌC VÀ CẬP NHẬT TRONG CÙNG MỘT LOCK
        // ========================================

        // Quan trọng:
        // Việc kiểm tra trạng thái và thay đổi
        // danh sách phải được thực hiện đồng bộ,
        // tránh hai thao tác cùng ghi đè dữ liệu.

        synchronized (LOCK) {

            try {

                SharedPreferences preferences =
                        getPreferences(context);

                JSONObject allFavorites =
                        readFavoritesObject(preferences);

                List<String> favoriteIds =
                        readCustomerFavoriteIds(
                                allFavorites,
                                customerId
                        );

                // ========================================
                // 8.3. BARBER ĐANG ĐƯỢC YÊU THÍCH
                // ========================================

                if (favoriteIds.contains(barberId)) {

                    // Nhấn trái tim lần nữa:
                    // Bỏ Barber khỏi yêu thích.
                    favoriteIds.remove(barberId);

                    boolean saved = saveCustomerFavorites(
                            preferences,
                            allFavorites,
                            customerId,
                            favoriteIds
                    );

                    if (!saved) {

                        return FavoriteResult.failure(
                                true,
                                "Không thể bỏ yêu thích."
                        );
                    }

                    return FavoriteResult.success(
                            false,
                            "Đã bỏ Barber khỏi danh sách yêu thích."
                    );
                }

                // ========================================
                // 8.4. BARBER CHƯA ĐƯỢC YÊU THÍCH
                // ========================================

                // Kiểm tra Barber còn tồn tại
                Barber barber =
                        BarberRepository.findById(
                                barberId
                        );

                if (barber == null) {

                    return FavoriteResult.failure(
                            false,
                            "Không tìm thấy Barber đã chọn."
                    );
                }

                // Thêm Barber ID vào danh sách
                favoriteIds.add(barberId);

                // ========================================
                // 8.5. LƯU DANH SÁCH
                // ========================================

                boolean saved = saveCustomerFavorites(
                        preferences,
                        allFavorites,
                        customerId,
                        favoriteIds
                );

                if (!saved) {

                    return FavoriteResult.failure(
                            false,
                            "Không thể thêm Barber yêu thích."
                    );
                }

                // ========================================
                // 8.6. TRẢ TRẠNG THÁI MỚI
                // ========================================

                return FavoriteResult.success(
                        true,
                        "Đã thêm Barber vào danh sách yêu thích."
                );

            } catch (JSONException
                     | IllegalArgumentException
                     | IllegalStateException e) {

                return FavoriteResult.failure(
                        false,
                        "Không thể cập nhật trạng thái yêu thích."
                );
            }
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
    // 10. ĐỌC TOÀN BỘ JSON YÊU THÍCH
    // ========================================

    private static JSONObject readFavoritesObject(
            SharedPreferences preferences
    ) throws JSONException {

        // ========================================
        // 10.1. LẤY JSON ĐÃ LƯU
        // ========================================

        String jsonText = preferences.getString(
                KEY_FAVORITES,
                "{}"
        );

        // ========================================
        // 10.2. KIỂM TRA DỮ LIỆU
        // ========================================

        if (jsonText == null) {

            throw new JSONException(
                    "Dữ liệu yêu thích không hợp lệ."
            );
        }

        // ========================================
        // 10.3. CHUYỂN THÀNH JSONOBJECT
        // ========================================

        // Ví dụ dữ liệu lưu:
        //
        // {
        //     "KH001": ["BB001", "BB003"],
        //     "KH002": ["BB002"]
        // }
        //
        // Mỗi key tương ứng với một Customer ID.
        // Mỗi array chứa các Barber ID yêu thích.

        return new JSONObject(jsonText);
    }

    // ========================================
    // 11. ĐỌC DANH SÁCH CỦA MỘT CUSTOMER
    // ========================================

    private static List<String> readCustomerFavoriteIds(
            JSONObject allFavorites,
            String customerId
    ) throws JSONException {

        // ========================================
        // 11.1. TẠO DANH SÁCH KHÔNG TRÙNG
        // ========================================

        // LinkedHashSet:
        // - Không cho phép ID trùng nhau
        // - Giữ nguyên thứ tự thêm vào

        Set<String> favoriteIds =
                new LinkedHashSet<>();

        // ========================================
        // 11.2. CUSTOMER CHƯA CÓ DANH SÁCH
        // ========================================

        if (!allFavorites.has(customerId)) {

            return new ArrayList<>();
        }

        // ========================================
        // 11.3. LẤY JSON ARRAY
        // ========================================

        JSONArray jsonArray =
                allFavorites.getJSONArray(customerId);

        // ========================================
        // 11.4. ĐỌC TỪNG BARBER ID
        // ========================================

        for (int i = 0; i < jsonArray.length(); i++) {

            Object rawValue = jsonArray.get(i);

            if (!(rawValue instanceof String)) {

                throw new JSONException(
                        "Barber ID phải là chuỗi."
                );
            }

            String barberId = normalizeValue(
                    (String) rawValue
            );

            if (barberId == null) {

                throw new JSONException(
                        "Barber ID không hợp lệ."
                );
            }

            favoriteIds.add(barberId);
        }

        // ========================================
        // 11.5. TRẢ VỀ LIST
        // ========================================

        return new ArrayList<>(favoriteIds);
    }

    // ========================================
    // 12. LƯU DANH SÁCH CỦA MỘT CUSTOMER
    // ========================================

    private static boolean saveCustomerFavorites(
            SharedPreferences preferences,
            JSONObject allFavorites,
            String customerId,
            List<String> favoriteIds
    ) throws JSONException {

        // ========================================
        // 12.1. TẠO JSON ARRAY MỚI
        // ========================================

        JSONArray jsonArray = new JSONArray();

        for (String barberId : favoriteIds) {

            jsonArray.put(barberId);
        }

        // ========================================
        // 12.2. CẬP NHẬT CUSTOMER
        // ========================================

        if (favoriteIds.isEmpty()) {

            // Nếu Customer không còn Barber yêu thích,
            // bỏ key của Customer khỏi JSON.
            //
            // Không ảnh hưởng dữ liệu Customer khác.

            allFavorites.remove(customerId);

        } else {

            allFavorites.put(
                    customerId,
                    jsonArray
            );
        }

        // ========================================
        // 12.3. LƯU XUỐNG SHAREDPREFERENCES
        // ========================================

        // commit() trả về kết quả boolean.
        // Chỉ thông báo thành công sau khi ghi xong.
        //
        // Việc ghi phải thực hiện ngoài Main Thread
        // tại các Activity sử dụng Repository.

        return preferences.edit()
                .putString(
                        KEY_FAVORITES,
                        allFavorites.toString()
                )
                .commit();
    }

    // ========================================
    // 13. CHUẨN HÓA CHUỖI
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
