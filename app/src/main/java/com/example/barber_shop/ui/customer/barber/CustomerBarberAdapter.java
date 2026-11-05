
package com.example.barber_shop.ui.customer.barber;

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
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BranchRepository;

import java.util.Locale;
import java.util.Objects;

public class CustomerBarberAdapter
        extends ListAdapter<Barber, CustomerBarberAdapter.BarberViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ SỰ KIỆN CLICK
    // ========================================

    public interface OnBarberClickListener {

        void onBarberClick(Barber barber);
    }

    // Listener nhận từ CustomerBarberListActivity
    private final OnBarberClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU VỚI DIFFUTIL
    // ========================================

    private static final DiffUtil.ItemCallback<Barber> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Barber>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Barber oldItem,
                        @NonNull Barber newItem
                ) {

                    // Kiểm tra hai Barber có cùng ID không
                    return Objects.equals(
                            oldItem.getId(),
                            newItem.getId()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Barber oldItem,
                        @NonNull Barber newItem
                ) {

                    // So sánh những thông tin hiển thị
                    return Objects.equals(
                            oldItem.getName(),
                            newItem.getName()
                    )
                            && Objects.equals(
                            oldItem.getBranchId(),
                            newItem.getBranchId()
                    )
                            && Objects.equals(
                            oldItem.getSpecialty(),
                            newItem.getSpecialty()
                    )
                            && oldItem.getExperienceYears()
                            == newItem.getExperienceYears()

                            && Double.compare(
                            oldItem.getRating(),
                            newItem.getRating()
                    ) == 0

                            && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerBarberAdapter(
            OnBarberClickListener listener
    ) {

        // Gửi DiffUtil cho ListAdapter
        super(DIFF_CALLBACK);

        // Lưu listener để xử lý khi nhấn Card
        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEW HOLDER
    // ========================================

    @NonNull
    @Override
    public BarberViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        // Sử dụng XML riêng của Customer
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_customer_barber,
                        parent,
                        false
                );

        return new BarberViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU CHO TỪNG CARD
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull BarberViewHolder holder,
            int position
    ) {

        // Lấy Barber tại vị trí hiện tại
        Barber barber = getItem(position);

        // ========================================
        // 5.1. HIỂN THỊ HỌ TÊN
        // ========================================

        holder.txtBarberName.setText(
                barber.getName()
        );

        // ========================================
        // 5.2. HIỂN THỊ CHUYÊN MÔN
        // ========================================

        holder.txtBarberSpecialty.setText(
                barber.getSpecialty()
        );

        // ========================================
        // 5.3. HIỂN THỊ CHI NHÁNH LÀM VIỆC
        // ========================================

        // Tra cứu chi nhánh thông qua branchId
        Branch branch = BranchRepository.findById(
                barber.getBranchId()
        );

        // Hiển thị tên chi nhánh nếu tìm thấy
        holder.txtBarberBranch.setText(
                branch != null
                        ? branch.getName()
                        : "Chưa có thông tin chi nhánh"
        );

        // ========================================
        // 5.4. HIỂN THỊ KINH NGHIỆM
        // ========================================

        holder.txtBarberExperience.setText(
                String.format(
                        Locale.getDefault(),
                        "%d năm kinh nghiệm",
                        barber.getExperienceYears()
                )
        );

        // ========================================
        // 5.5. HIỂN THỊ ĐÁNH GIÁ
        // ========================================

        holder.txtBarberRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f",
                        barber.getRating()
                )
        );

        // ========================================
        // 5.6. HIỂN THỊ ẢNH ĐẠI DIỆN
        // ========================================

        holder.imgBarber.setImageResource(
                barber.getImageResId()
        );

        // ========================================
        // 5.7. XỬ LÝ SỰ KIỆN NHẤN CARD
        // ========================================

        holder.itemView.setOnClickListener(v -> {

            // Chuyển Barber được chọn về Activity
            if (listener != null) {

                listener.onBarberClick(barber);
            }
        });
    }

    // ========================================
    // 6. VIEW HOLDER
    // ========================================

    static class BarberViewHolder
            extends RecyclerView.ViewHolder {

        // Ảnh đại diện Barber
        AppCompatImageView imgBarber;

        // Các thông tin của Barber
        TextView txtBarberName;
        TextView txtBarberSpecialty;
        TextView txtBarberBranch;
        TextView txtBarberExperience;
        TextView txtBarberRating;

        // ========================================
        // 7. ÁNH XẠ VIEW TRONG TỪNG CARD
        // ========================================

        BarberViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            // Ảnh đại diện
            imgBarber = itemView.findViewById(
                    R.id.imgCustomerItemBarber
            );

            // Họ tên
            txtBarberName = itemView.findViewById(
                    R.id.txtCustomerItemBarberName
            );

            // Chuyên môn
            txtBarberSpecialty = itemView.findViewById(
                    R.id.txtCustomerItemBarberSpecialty
            );

            // Chi nhánh làm việc
            txtBarberBranch = itemView.findViewById(
                    R.id.txtCustomerItemBarberBranch
            );

            // Số năm kinh nghiệm
            txtBarberExperience = itemView.findViewById(
                    R.id.txtCustomerItemBarberExperience
            );

            // Đánh giá
            txtBarberRating = itemView.findViewById(
                    R.id.txtCustomerItemBarberRating
            );
        }
    }
}
