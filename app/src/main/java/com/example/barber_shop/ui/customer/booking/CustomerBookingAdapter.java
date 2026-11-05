
package com.example.barber_shop.ui.customer.booking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Booking;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerBookingAdapter
        extends RecyclerView.Adapter<
        CustomerBookingAdapter.BookingViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ SỰ KIỆN
    // ========================================

    public interface OnBookingClickListener {

        void onBookingClick(Booking booking);
    }

    // ========================================
    // 2. KHAI BÁO DỮ LIỆU
    // ========================================

    private final List<Booking> bookings =
            new ArrayList<>();

    private final OnBookingClickListener listener;

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerBookingAdapter(
            OnBookingClickListener listener
    ) {

        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEWHOLDER
    // ========================================

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_customer_booking,
                parent,
                false
        );

        return new BookingViewHolder(view);
    }

    // ========================================
    // 5. HIỂN THỊ DỮ LIỆU BOOKING
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull BookingViewHolder holder,
            int position
    ) {

        Booking booking = bookings.get(position);

        // ========================================
        // 5.1. MÃ BOOKING
        // ========================================

        holder.txtBookingCode.setText(
                booking.getBookingCode()
        );

        // ========================================
        // 5.2. DỊCH VỤ
        // ========================================

        holder.txtBookingService.setText(
                booking.getServiceName()
        );

        // ========================================
        // 5.3. CHI NHÁNH
        // ========================================

        holder.txtBookingBranch.setText(
                "Chi nhánh: " + booking.getBranchName()
        );

        // ========================================
        // 5.4. BARBER
        // ========================================

        holder.txtBookingBarber.setText(
                "Barber: " + booking.getBarberName()
        );

        // ========================================
        // 5.5. NGÀY VÀ GIỜ
        // ========================================

        String dateText = formatBookingDate(
                booking.getBookingDate()
        );

        holder.txtBookingDateTime.setText(
                "Ngày giờ: "
                        + dateText
                        + " · "
                        + booking.getBookingTime()
        );

        // ========================================
        // 5.6. GIÁ TIỀN
        // ========================================

        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        holder.txtBookingPrice.setText(
                formatter.format(
                        booking.getTotalPrice()
                ) + " đ"
        );

        // ========================================
        // 5.7. TRẠNG THÁI
        // ========================================

        String statusText =
                booking.getStatusDisplayName();

        // Lịch đã qua giờ nhưng chưa được hệ thống
        // cập nhật trạng thái thực tế.
        if (booking.isActive()
                && !isBookingInFuture(booking)) {

            statusText += " · Đã qua giờ";
        }

        holder.txtBookingStatus.setText(statusText);

        // Màu trạng thái theo Design System
        holder.txtBookingStatus.setTextColor(
                getStatusColor(booking.getStatus())
        );

        // ========================================
        // 5.8. XỬ LÝ KHI NHẤN VÀO CARD
        // ========================================

        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onBookingClick(booking);
            }
        });
    }

    // ========================================
    // 6. LẤY SỐ LƯỢNG BOOKING
    // ========================================

    @Override
    public int getItemCount() {

        return bookings.size();
    }

    // ========================================
    // 7. CẬP NHẬT DANH SÁCH BOOKING
    // ========================================

    public void submitList(List<Booking> newBookings) {

        bookings.clear();

        if (newBookings != null) {
            bookings.addAll(newBookings);
        }

        // Làm mới danh sách trên RecyclerView
        notifyDataSetChanged();
    }

    // ========================================
    // 8. ĐỊNH DẠNG NGÀY
    // ========================================

    private String formatBookingDate(String dateText) {

        if (dateText == null) {
            return "--";
        }

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date date = parser.parse(dateText);

            if (date == null) {
                return dateText;
            }

            SimpleDateFormat formatter =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            new Locale("vi", "VN")
                    );

            return formatter.format(date);

        } catch (ParseException e) {

            return dateText;
        }
    }

    // ========================================
    // 9. KIỂM TRA LỊCH HẸN CÒN Ở TƯƠNG LAI
    // ========================================

    private boolean isBookingInFuture(Booking booking) {

        SimpleDateFormat parser =
                new SimpleDateFormat(
                        "yyyy-MM-dd HH:mm",
                        Locale.US
                );

        parser.setLenient(false);

        try {

            Date date = parser.parse(
                    booking.getBookingDate()
                            + " "
                            + booking.getBookingTime()
            );

            return date != null
                    && date.getTime()
                    > System.currentTimeMillis();

        } catch (ParseException e) {

            return false;
        }
    }

    // ========================================
    // 10. MÀU TRẠNG THÁI
    // ========================================

    private int getStatusColor(String status) {

        if (Booking.STATUS_PENDING.equals(status)) {
            return Color.parseColor("#B26A00");
        }

        if (Booking.STATUS_CONFIRMED.equals(status)) {
            return Color.parseColor("#2563EB");
        }

        if (Booking.STATUS_COMPLETED.equals(status)) {
            return Color.parseColor("#16A34A");
        }

        if (Booking.STATUS_CANCELLED.equals(status)) {
            return Color.parseColor("#DC2626");
        }

        return Color.parseColor("#756F69");
    }

    // ========================================
    // 11. VIEWHOLDER
    // ========================================

    public static class BookingViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtBookingCode;
        private final TextView txtBookingStatus;
        private final TextView txtBookingService;
        private final TextView txtBookingBranch;
        private final TextView txtBookingBarber;
        private final TextView txtBookingDateTime;
        private final TextView txtBookingPrice;

        public BookingViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            txtBookingCode = itemView.findViewById(
                    R.id.txtItemBookingCode
            );

            txtBookingStatus = itemView.findViewById(
                    R.id.txtItemBookingStatus
            );

            txtBookingService = itemView.findViewById(
                    R.id.txtItemBookingService
            );

            txtBookingBranch = itemView.findViewById(
                    R.id.txtItemBookingBranch
            );

            txtBookingBarber = itemView.findViewById(
                    R.id.txtItemBookingBarber
            );

            txtBookingDateTime = itemView.findViewById(
                    R.id.txtItemBookingDateTime
            );

            txtBookingPrice = itemView.findViewById(
                    R.id.txtItemBookingPrice
            );
        }
    }
}
