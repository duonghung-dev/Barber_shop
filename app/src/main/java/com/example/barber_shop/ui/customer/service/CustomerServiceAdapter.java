
package com.example.barber_shop.ui.customer.service;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.SalonService;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class CustomerServiceAdapter extends
        ListAdapter<SalonService, CustomerServiceAdapter.ServiceViewHolder> {

    // ========================================
    // 1. XỬ LÝ SỰ KIỆN CHỌN DỊCH VỤ
    // ========================================

    public interface OnServiceClickListener {
        void onServiceClick(SalonService service);
    }

    private final OnServiceClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU KHI CẬP NHẬT DANH SÁCH
    // ========================================

    private static final DiffUtil.ItemCallback<SalonService> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SalonService>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull SalonService oldItem,
                        @NonNull SalonService newItem
                ) {
                    return Objects.equals(
                            oldItem.getId(),
                            newItem.getId()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SalonService oldItem,
                        @NonNull SalonService newItem
                ) {
                    return Objects.equals(
                            oldItem.getName(),
                            newItem.getName()
                    ) && Objects.equals(
                            oldItem.getCategory(),
                            newItem.getCategory()
                    ) && Objects.equals(
                            oldItem.getDescription(),
                            newItem.getDescription()
                    ) && oldItem.getDurationMinutes()
                            == newItem.getDurationMinutes()
                            && oldItem.getPriceVnd()
                            == newItem.getPriceVnd()
                            && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. KHỞI TẠO ADAPTER
    // ========================================

    public CustomerServiceAdapter(
            OnServiceClickListener listener
    ) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEW HOLDER
    // ========================================

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_customer_service,
                parent,
                false
        );

        return new ServiceViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU DỊCH VỤ
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull ServiceViewHolder holder,
            int position
    ) {

        SalonService service = getItem(position);

        // Hiển thị tên dịch vụ
        holder.txtName.setText(
                service.getName()
        );

        // Hiển thị danh mục
        holder.txtCategory.setText(
                service.getCategory()
        );

        // Hiển thị mô tả
        holder.txtDescription.setText(
                service.getDescription()
        );

        // Hiển thị thời lượng
        holder.txtDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        service.getDurationMinutes()
                )
        );

        // Định dạng giá tiền Việt Nam
        NumberFormat formatter =
                NumberFormat.getNumberInstance(
                        new Locale("vi", "VN")
                );

        String priceText = formatter.format(
                service.getPriceVnd()
        ) + " đ";

        holder.txtPrice.setText(priceText);

        // Hiển thị hình minh họa
        holder.imgService.setImageResource(
                service.getImageResId()
        );

        // Xử lý khi Customer nhấn vào Card
        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onServiceClick(service);
            }
        });
    }

    // ========================================
    // 6. ÁNH XẠ CÁC VIEW CỦA CARD
    // ========================================

    static class ServiceViewHolder
            extends RecyclerView.ViewHolder {

        final AppCompatImageView imgService;

        final TextView txtName;
        final TextView txtCategory;
        final TextView txtDescription;
        final TextView txtDuration;
        final TextView txtPrice;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);

            imgService = itemView.findViewById(
                    R.id.imgCustomerItemService
            );

            txtName = itemView.findViewById(
                    R.id.txtCustomerItemServiceName
            );

            txtCategory = itemView.findViewById(
                    R.id.txtCustomerItemServiceCategory
            );

            txtDescription = itemView.findViewById(
                    R.id.txtCustomerItemServiceDescription
            );

            txtDuration = itemView.findViewById(
                    R.id.txtCustomerItemServiceDuration
            );

            txtPrice = itemView.findViewById(
                    R.id.txtCustomerItemServicePrice
            );
        }
    }
}

