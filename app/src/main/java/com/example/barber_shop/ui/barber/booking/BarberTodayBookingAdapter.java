
package com.example.barber_shop.ui.barber.booking;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Booking;
import com.google.android.material.chip.Chip;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BarberTodayBookingAdapter
        extends ListAdapter<
        Booking,
        BarberTodayBookingAdapter.BookingViewHolder
        > {

    private static final Locale VIETNAM_LOCALE =
            new Locale("vi", "VN");

    public interface OnBookingClickListener {
        void onBookingClick(Booking booking);
    }

    private final OnBookingClickListener listener;

    private static final DiffUtil.ItemCallback<Booking>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Booking>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Booking oldItem,
                        @NonNull Booking newItem
                ) {
                    return Objects.equals(
                            oldItem.getBookingCode(),
                            newItem.getBookingCode()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Booking oldItem,
                        @NonNull Booking newItem
                ) {
                    return Objects.equals(
                            oldItem.getBookingCode(),
                            newItem.getBookingCode()
                    )
                            && Objects.equals(
                            oldItem.getCustomerId(),
                            newItem.getCustomerId()
                    )
                            && Objects.equals(
                            oldItem.getBranchName(),
                            newItem.getBranchName()
                    )
                            && Objects.equals(
                            oldItem.getServiceName(),
                            newItem.getServiceName()
                    )
                            && Objects.equals(
                            oldItem.getBookingDate(),
                            newItem.getBookingDate()
                    )
                            && Objects.equals(
                            oldItem.getBookingTime(),
                            newItem.getBookingTime()
                    )
                            && oldItem.getDurationMinutes()
                            == newItem.getDurationMinutes()
                            && oldItem.getTotalPrice()
                            == newItem.getTotalPrice()
                            && Objects.equals(
                            oldItem.getStatus(),
                            newItem.getStatus()
                    );
                }
            };

    public BarberTodayBookingAdapter(
            @NonNull OnBookingClickListener listener
    ) {
        super(DIFF_CALLBACK);

        this.listener = listener;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_barber_today_booking,
                parent,
                false
        );

        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull BookingViewHolder holder,
            int position
    ) {

        Booking booking = getItem(position);

        holder.bind(booking);

        holder.itemView.setOnClickListener(v -> {

            int currentPosition =
                    holder.getBindingAdapterPosition();

            if (currentPosition
                    == RecyclerView.NO_POSITION) {
                return;
            }

            Booking selectedBooking =
                    getItem(currentPosition);

            listener.onBookingClick(
                    selectedBooking
            );
        });
    }

    public void setBookings(
            List<Booking> bookings
    ) {

        List<Booking> newList = new ArrayList<>();

        if (bookings != null) {

            for (Booking booking : bookings) {

                if (booking != null) {
                    newList.add(booking);
                }
            }
        }

        submitList(newList);
    }

    public Booking getBookingAt(int position) {

        if (position < 0
                || position >= getItemCount()) {
            return null;
        }

        return getItem(position);
    }

    static class BookingViewHolder
            extends RecyclerView.ViewHolder {

        private final TextView txtBookingCode;
        private final TextView txtBookingTime;
        private final TextView txtBookingDate;
        private final TextView txtServiceName;
        private final TextView txtCustomerId;
        private final TextView txtBranchName;
        private final TextView txtDuration;
        private final TextView txtPrice;

        private final Chip chipBookingStatus;

        BookingViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            txtBookingCode = itemView.findViewById(
                    R.id.txtBarberTodayItemBookingCode
            );

            txtBookingTime = itemView.findViewById(
                    R.id.txtBarberTodayItemTime
            );

            txtBookingDate = itemView.findViewById(
                    R.id.txtBarberTodayItemDate
            );

            txtServiceName = itemView.findViewById(
                    R.id.txtBarberTodayItemService
            );

            txtCustomerId = itemView.findViewById(
                    R.id.txtBarberTodayItemCustomerId
            );

            txtBranchName = itemView.findViewById(
                    R.id.txtBarberTodayItemBranch
            );

            txtDuration = itemView.findViewById(
                    R.id.txtBarberTodayItemDuration
            );

            txtPrice = itemView.findViewById(
                    R.id.txtBarberTodayItemPrice
            );

            chipBookingStatus = itemView.findViewById(
                    R.id.chipBarberTodayItemStatus
            );
        }

        void bind(Booking booking) {

            txtBookingCode.setText(
                    "Mã lịch: "
                            + safeText(
                            booking.getBookingCode()
                    )
            );

            txtBookingTime.setText(
                    safeText(
                            booking.getBookingTime()
                    )
            );

            txtBookingDate.setText(
                    formatBookingDate(
                            booking.getBookingDate()
                    )
            );

            txtServiceName.setText(
                    safeText(
                            booking.getServiceName()
                    )
            );

            txtCustomerId.setText(
                    "Mã khách hàng: "
                            + safeText(
                            booking.getCustomerId()
                    )
            );

            txtBranchName.setText(
                    "Chi nhánh: "
                            + safeText(
                            booking.getBranchName()
                    )
            );

            txtDuration.setText(
                    String.format(
                            VIETNAM_LOCALE,
                            "%d phút",
                            booking.getDurationMinutes()
                    )
            );

            NumberFormat numberFormat =
                    NumberFormat.getNumberInstance(
                            VIETNAM_LOCALE
                    );

            txtPrice.setText(
                    numberFormat.format(
                            booking.getTotalPrice()
                    ) + " đ"
            );

            bindStatus(
                    booking.getStatus()
            );
        }

        private void bindStatus(String status) {

            int statusColorRes;
            String statusText;

            if (Booking.STATUS_PENDING.equals(status)) {

                statusColorRes = R.color.status_pending;
                statusText = "Chờ xác nhận";

            } else if (
                    Booking.STATUS_CONFIRMED.equals(status)
            ) {

                statusColorRes = R.color.status_confirmed;
                statusText = "Đã xác nhận";

            } else if (
                    Booking.STATUS_COMPLETED.equals(status)
            ) {

                statusColorRes = R.color.status_completed;
                statusText = "Hoàn thành";

            } else if (
                    Booking.STATUS_CANCELLED.equals(status)
            ) {

                statusColorRes = R.color.status_cancelled;
                statusText = "Đã hủy";

            } else {

                statusColorRes = R.color.text_secondary;
                statusText = "Không xác định";
            }

            int statusColor = ContextCompat.getColor(
                    itemView.getContext(),
                    statusColorRes
            );

            int chipBackgroundColor =
                    ColorUtils.blendARGB(
                            Color.WHITE,
                            statusColor,
                            0.12f
                    );

            chipBookingStatus.setText(
                    statusText
            );

            chipBookingStatus.setTextColor(
                    statusColor
            );

            chipBookingStatus.setChipBackgroundColor(
                    ColorStateList.valueOf(
                            chipBackgroundColor
                    )
            );

            chipBookingStatus.setClickable(false);
            chipBookingStatus.setCheckable(false);
        }

        private static String safeText(
                String value
        ) {

            if (value == null
                    || value.trim().isEmpty()) {
                return "Chưa cập nhật";
            }

            return value.trim();
        }

        private static String formatBookingDate(
                String bookingDate
        ) {

            if (bookingDate == null
                    || !bookingDate.matches(
                    "[0-9]{4}-[0-9]{2}-[0-9]{2}"
            )) {
                return "Chưa cập nhật";
            }

            return bookingDate.substring(8, 10)
                    + "/"
                    + bookingDate.substring(5, 7)
                    + "/"
                    + bookingDate.substring(0, 4);
        }
    }
}

