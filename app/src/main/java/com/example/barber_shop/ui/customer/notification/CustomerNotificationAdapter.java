
package com.example.barber_shop.ui.customer.notification;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Notification;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CustomerNotificationAdapter
        extends RecyclerView.Adapter<
        CustomerNotificationAdapter.NotificationViewHolder> {

    // ========================================
    // 1. KHAI BÁO DỮ LIỆU
    // ========================================

    // Danh sách thông báo đang hiển thị
    private final List<Notification> notificationList =
            new ArrayList<>();

    // Sự kiện khi Customer nhấn thông báo
    private final OnNotificationClickListener listener;

    // ========================================
    // 2. INTERFACE XỬ LÝ SỰ KIỆN
    // ========================================

    public interface OnNotificationClickListener {

        void onNotificationClick(
                Notification notification
        );
    }

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerNotificationAdapter(
            OnNotificationClickListener listener
    ) {

        this.listener = listener;

        // Dữ liệu sẽ được cung cấp sau
        // thông qua phương thức submitList().
    }

    // ========================================
    // 4. TẠO VIEWHOLDER
    // ========================================

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        // ========================================
        // 4.1. LẤY CONTEXT
        // ========================================

        Context context = parent.getContext();

        // ========================================
        // 4.2. INFLATE ITEM XML
        // ========================================

        View view = LayoutInflater.from(context)
                .inflate(
                        R.layout.item_customer_notification,
                        parent,
                        false
                );

        // ========================================
        // 4.3. TRẢ VỀ VIEWHOLDER
        // ========================================

        return new NotificationViewHolder(view);
    }

    // ========================================
    // 5. HIỂN THỊ DỮ LIỆU VÀO ITEM
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull NotificationViewHolder holder,
            int position
    ) {

        // ========================================
        // 5.1. LẤY THÔNG BÁO
        // ========================================

        Notification notification =
                notificationList.get(position);

        // ========================================
        // 5.2. HIỂN THỊ LOẠI THÔNG BÁO
        // ========================================

        holder.txtCustomerNotificationType.setText(
                notification.getTypeDisplayName()
        );

        // ========================================
        // 5.3. HIỂN THỊ TIÊU ĐỀ
        // ========================================

        holder.txtCustomerNotificationTitle.setText(
                notification.getTitle()
        );

        // ========================================
        // 5.4. HIỂN THỊ NỘI DUNG
        // ========================================

        holder.txtCustomerNotificationMessage.setText(
                notification.getMessage()
        );

        // ========================================
        // 5.5. HIỂN THỊ THỜI GIAN
        // ========================================

        holder.txtCustomerNotificationTime.setText(
                formatNotificationTime(
                        notification.getCreatedAtMillis()
                )
        );

        // ========================================
        // 5.6. CẬP NHẬT BIỂU TƯỢNG
        // ========================================

        // STT 21 mở rộng:
        // Hỗ trợ thêm BOOKING_RESCHEDULED.

        updateNotificationIcon(
                holder,
                notification
        );

        // ========================================
        // 5.7. CẬP NHẬT TRẠNG THÁI ĐÃ ĐỌC
        // ========================================

        updateReadStatus(
                holder,
                notification
        );

        // ========================================
        // 5.8. XỬ LÝ NHẤN VÀO THÔNG BÁO
        // ========================================

        holder.cardCustomerNotification
                .setOnClickListener(v -> {

                    if (listener == null) {
                        return;
                    }

                    // ========================================
                    // 5.8.1. LẤY VỊ TRÍ HIỆN TẠI
                    // ========================================

                    // RecyclerView có thể thay đổi
                    // vị trí sau khi cập nhật danh sách.
                    //
                    // Vì vậy, không sử dụng trực tiếp
                    // position nhận từ onBindViewHolder().

                    int adapterPosition =
                            holder.getBindingAdapterPosition();

                    // ========================================
                    // 5.8.2. KIỂM TRA VỊ TRÍ
                    // ========================================

                    if (adapterPosition
                            == RecyclerView.NO_POSITION) {

                        return;
                    }

                    if (adapterPosition < 0
                            || adapterPosition
                            >= notificationList.size()) {

                        return;
                    }

                    // ========================================
                    // 5.8.3. LẤY THÔNG BÁO ĐƯỢC CHỌN
                    // ========================================

                    Notification selectedNotification =
                            notificationList.get(
                                    adapterPosition
                            );

                    // ========================================
                    // 5.8.4. CHUYỂN SỰ KIỆN VỀ ACTIVITY
                    // ========================================

                    // Activity sẽ:
                    // - Kiểm tra Customer
                    // - Đánh dấu đã đọc
                    // - Mở Chi tiết Booking

                    listener.onNotificationClick(
                            selectedNotification
                    );
                });
    }

    // ========================================
    // 6. LẤY SỐ LƯỢNG THÔNG BÁO
    // ========================================

    @Override
    public int getItemCount() {

        return notificationList.size();
    }

    // ========================================
    // 7. CẬP NHẬT DANH SÁCH THÔNG BÁO
    // ========================================

    public void submitList(
            List<Notification> newNotifications
    ) {

        // ========================================
        // 7.1. SAO CHÉP DANH SÁCH MỚI
        // ========================================

        List<Notification> newList =
                new ArrayList<>();

        if (newNotifications != null) {

            newList.addAll(
                    newNotifications
            );
        }

        // ========================================
        // 7.2. SAO CHÉP DANH SÁCH HIỆN TẠI
        // ========================================

        List<Notification> oldList =
                new ArrayList<>(
                        notificationList
                );

        // ========================================
        // 7.3. SO SÁNH DỮ LIỆU BẰNG DIFFUTIL
        // ========================================

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new DiffUtil.Callback() {

                            // ========================================
                            // 7.3.1. SỐ LƯỢNG ITEM CŨ
                            // ========================================

                            @Override
                            public int getOldListSize() {

                                return oldList.size();
                            }

                            // ========================================
                            // 7.3.2. SỐ LƯỢNG ITEM MỚI
                            // ========================================

                            @Override
                            public int getNewListSize() {

                                return newList.size();
                            }

                            // ========================================
                            // 7.3.3. KIỂM TRA CÙNG NOTIFICATION
                            // ========================================

                            @Override
                            public boolean areItemsTheSame(
                                    int oldItemPosition,
                                    int newItemPosition
                            ) {

                                Notification oldItem =
                                        oldList.get(
                                                oldItemPosition
                                        );

                                Notification newItem =
                                        newList.get(
                                                newItemPosition
                                        );

                                // Notification ID là
                                // khóa định danh duy nhất.
                                //
                                // Hai sự kiện đổi lịch có
                                // hai Notification ID khác nhau.

                                return oldItem.getNotificationId()
                                        .equals(
                                                newItem.getNotificationId()
                                        );
                            }

                            // ========================================
                            // 7.3.4. KIỂM TRA NỘI DUNG THAY ĐỔI
                            // ========================================

                            @Override
                            public boolean areContentsTheSame(
                                    int oldItemPosition,
                                    int newItemPosition
                            ) {

                                Notification oldItem =
                                        oldList.get(
                                                oldItemPosition
                                        );

                                Notification newItem =
                                        newList.get(
                                                newItemPosition
                                        );

                                // ========================================
                                // ID
                                // ========================================

                                boolean sameId =
                                        oldItem.getNotificationId()
                                                .equals(
                                                        newItem.getNotificationId()
                                                );

                                // ========================================
                                // CUSTOMER ID
                                // ========================================

                                boolean sameCustomer =
                                        oldItem.getCustomerId()
                                                .equals(
                                                        newItem.getCustomerId()
                                                );

                                // ========================================
                                // BOOKING CODE
                                // ========================================

                                boolean sameBooking =
                                        oldItem.getBookingCode()
                                                .equals(
                                                        newItem.getBookingCode()
                                                );

                                // ========================================
                                // LOẠI THÔNG BÁO
                                // ========================================

                                boolean sameType =
                                        oldItem.getType()
                                                .equals(
                                                        newItem.getType()
                                                );

                                // ========================================
                                // TIÊU ĐỀ
                                // ========================================

                                boolean sameTitle =
                                        oldItem.getTitle()
                                                .equals(
                                                        newItem.getTitle()
                                                );

                                // ========================================
                                // NỘI DUNG
                                // ========================================

                                boolean sameMessage =
                                        oldItem.getMessage()
                                                .equals(
                                                        newItem.getMessage()
                                                );

                                // ========================================
                                // THỜI GIAN TẠO
                                // ========================================

                                boolean sameTime =
                                        oldItem.getCreatedAtMillis()
                                                == newItem.getCreatedAtMillis();

                                // ========================================
                                // TRẠNG THÁI ĐÃ ĐỌC
                                // ========================================

                                boolean sameReadStatus =
                                        oldItem.isRead()
                                                == newItem.isRead();

                                // ========================================
                                // EVENT KEY - STT 21 MỞ RỘNG (MỚI)
                                // ========================================

                                // eventKey có thể null
                                // đối với thông báo cũ.
                                //
                                // Objects.equals() cho phép
                                // so sánh an toàn khi một hoặc
                                // cả hai giá trị là null.
                                //
                                // Ví dụ:
                                // null và null -> true
                                // null và "ABC" -> false
                                // "ABC" và "ABC" -> true

                                boolean sameEventKey =
                                        Objects.equals(
                                                oldItem.getEventKey(),
                                                newItem.getEventKey()
                                        );

                                // ========================================
                                // TRẢ KẾT QUẢ SO SÁNH
                                // ========================================

                                return sameId
                                        && sameCustomer
                                        && sameBooking
                                        && sameType
                                        && sameTitle
                                        && sameMessage
                                        && sameTime
                                        && sameReadStatus
                                        && sameEventKey;
                            }
                        }
                );

        // ========================================
        // 7.4. CẬP NHẬT DANH SÁCH
        // ========================================

        notificationList.clear();

        notificationList.addAll(
                newList
        );

        // ========================================
        // 7.5. THÔNG BÁO RECYCLERVIEW
        // ========================================

        // DiffUtil chỉ yêu cầu RecyclerView
        // cập nhật các item thực sự thay đổi.

        diffResult.dispatchUpdatesTo(this);
    }

    // ========================================
    // 8. CẬP NHẬT BIỂU TƯỢNG THÔNG BÁO
    // ========================================

    private void updateNotificationIcon(
            NotificationViewHolder holder,
            Notification notification
    ) {

        // ========================================
        // 8.1. LẤY LOẠI THÔNG BÁO
        // ========================================

        String type = notification.getType();

        String iconText;

        // ========================================
        // 8.2. CHỌN ICON THEO LOẠI THÔNG BÁO
        // ========================================

        switch (type) {

            // ========================================
            // 8.2.1. ĐẶT LỊCH THÀNH CÔNG
            // ========================================

            case Notification.TYPE_BOOKING_CREATED:

                iconText = "📅";
                break;

            // ========================================
            // 8.2.2. LỊCH HẸN ĐƯỢC XÁC NHẬN
            // ========================================

            case Notification.TYPE_BOOKING_CONFIRMED:

                iconText = "✅";
                break;

            // ========================================
            // 8.2.3. LỊCH HẸN BỊ HỦY
            // ========================================

            case Notification.TYPE_BOOKING_CANCELLED:

                iconText = "❌";
                break;

            // ========================================
            // 8.2.4. LỊCH HẸN HOÀN THÀNH
            // ========================================

            case Notification.TYPE_BOOKING_COMPLETED:

                iconText = "⭐";
                break;

            // ========================================
            // 8.2.5. NHẮC LỊCH HẸN
            // ========================================

            case Notification.TYPE_BOOKING_REMINDER:

                iconText = "⏰";
                break;

            // ========================================
            // 8.2.6. ĐỔI LỊCH THÀNH CÔNG - MỚI
            // ========================================

            // STT 21 mở rộng:
            //
            // Khi Customer đổi ngày hoặc giờ
            // thành công, hiển thị biểu tượng
            // mũi tên xoay để phân biệt.

            case Notification.TYPE_BOOKING_RESCHEDULED:

                iconText = "🔄";
                break;

            // ========================================
            // 8.2.7. LOẠI THÔNG BÁO KHÁC
            // ========================================

            default:

                iconText = "🔔";
                break;
        }

        // ========================================
        // 8.3. HIỂN THỊ BIỂU TƯỢNG
        // ========================================

        holder.txtCustomerNotificationIcon.setText(
                iconText
        );

        // ========================================
        // 8.4. MÔ TẢ PHỤC VỤ ACCESSIBILITY
        // ========================================

        holder.txtCustomerNotificationIcon
                .setContentDescription(
                        notification.getTypeDisplayName()
                );
    }

    // ========================================
    // 9. CẬP NHẬT TRẠNG THÁI ĐÃ ĐỌC
    // ========================================

    private void updateReadStatus(
            NotificationViewHolder holder,
            Notification notification
    ) {

        // ========================================
        // 9.1. LẤY CONTEXT
        // ========================================

        Context context =
                holder.itemView.getContext();

        // ========================================
        // 9.2. THÔNG BÁO ĐÃ ĐỌC
        // ========================================

        if (notification.isRead()) {

            // Ẩn dấu chấm chưa đọc
            holder.cardCustomerNotificationUnreadDot
                    .setVisibility(
                            View.GONE
                    );

            // Nền trắng
            holder.cardCustomerNotification
                    .setCardBackgroundColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.surface
                            )
                    );

            // Viền mặc định
            holder.cardCustomerNotification
                    .setStrokeColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.border_default
                            )
                    );

            // Tiêu đề bình thường
            holder.txtCustomerNotificationTitle
                    .setTypeface(
                            null,
                            Typeface.NORMAL
                    );

        } else {

            // ========================================
            // 9.3. THÔNG BÁO CHƯA ĐỌC
            // ========================================

            // Hiển thị dấu chấm
            holder.cardCustomerNotificationUnreadDot
                    .setVisibility(
                            View.VISIBLE
                    );

            // Dùng nền nâu nhạt
            holder.cardCustomerNotification
                    .setCardBackgroundColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.primary_light
                            )
                    );

            // Viền màu chủ đạo
            holder.cardCustomerNotification
                    .setStrokeColor(
                            ContextCompat.getColor(
                                    context,
                                    R.color.primary
                            )
                    );

            // In đậm tiêu đề
            holder.txtCustomerNotificationTitle
                    .setTypeface(
                            null,
                            Typeface.BOLD
                    );
        }
    }

    // ========================================
    // 10. ĐỊNH DẠNG THỜI GIAN THÔNG BÁO
    // ========================================

    private String formatNotificationTime(
            long createdAtMillis
    ) {

        // ========================================
        // 10.1. KIỂM TRA THỜI GIAN
        // ========================================

        if (createdAtMillis <= 0) {

            return "--";
        }

        // ========================================
        // 10.2. ĐỊNH DẠNG NGÀY GIỜ
        // ========================================

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "dd/MM/yyyy - HH:mm",
                        new Locale("vi", "VN")
                );

        // ========================================
        // 10.3. CHUYỂN MILLISECONDS SANG DATE
        // ========================================

        Date date = new Date(
                createdAtMillis
        );

        // ========================================
        // 10.4. TRẢ VỀ CHUỖI HIỂN THỊ
        // ========================================

        return formatter.format(date);
    }

    // ========================================
    // 11. VIEWHOLDER
    // ========================================

    public static class NotificationViewHolder
            extends RecyclerView.ViewHolder {

        // ========================================
        // 11.1. CARD CHÍNH
        // ========================================

        private final MaterialCardView
                cardCustomerNotification;

        // ========================================
        // 11.2. BIỂU TƯỢNG
        // ========================================

        private final TextView
                txtCustomerNotificationIcon;

        // ========================================
        // 11.3. LOẠI THÔNG BÁO
        // ========================================

        private final TextView
                txtCustomerNotificationType;

        // ========================================
        // 11.4. TIÊU ĐỀ
        // ========================================

        private final TextView
                txtCustomerNotificationTitle;

        // ========================================
        // 11.5. NỘI DUNG
        // ========================================

        private final TextView
                txtCustomerNotificationMessage;

        // ========================================
        // 11.6. THỜI GIAN
        // ========================================

        private final TextView
                txtCustomerNotificationTime;

        // ========================================
        // 11.7. DẤU CHẤM CHƯA ĐỌC
        // ========================================

        private final MaterialCardView
                cardCustomerNotificationUnreadDot;

        // ========================================
        // 12. CONSTRUCTOR VIEWHOLDER
        // ========================================

        public NotificationViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            // ========================================
            // 12.1. CARD THÔNG BÁO
            // ========================================

            cardCustomerNotification =
                    itemView.findViewById(
                            R.id.cardCustomerNotification
                    );

            // ========================================
            // 12.2. ICON
            // ========================================

            txtCustomerNotificationIcon =
                    itemView.findViewById(
                            R.id.txtCustomerNotificationIcon
                    );

            // ========================================
            // 12.3. LOẠI THÔNG BÁO
            // ========================================

            txtCustomerNotificationType =
                    itemView.findViewById(
                            R.id.txtCustomerNotificationType
                    );

            // ========================================
            // 12.4. TIÊU ĐỀ
            // ========================================

            txtCustomerNotificationTitle =
                    itemView.findViewById(
                            R.id.txtCustomerNotificationTitle
                    );

            // ========================================
            // 12.5. NỘI DUNG
            // ========================================

            txtCustomerNotificationMessage =
                    itemView.findViewById(
                            R.id.txtCustomerNotificationMessage
                    );

            // ========================================
            // 12.6. THỜI GIAN
            // ========================================

            txtCustomerNotificationTime =
                    itemView.findViewById(
                            R.id.txtCustomerNotificationTime
                    );

            // ========================================
            // 12.7. DẤU CHẤM CHƯA ĐỌC
            // ========================================

            cardCustomerNotificationUnreadDot =
                    itemView.findViewById(
                            R.id.cardCustomerNotificationUnreadDot
                    );
        }
    }
}

