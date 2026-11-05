
package com.example.barber_shop.ui.guest.branch;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Branch;

import java.util.Locale;
import java.util.Objects;

public class BranchAdapter
        extends ListAdapter<Branch, BranchAdapter.BranchViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ SỰ KIỆN CLICK
    // ========================================

    public interface OnBranchClickListener {
        void onBranchClick(Branch branch);
    }

    private final OnBranchClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU CHI NHÁNH
    // ========================================

    private static final DiffUtil.ItemCallback<Branch> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Branch>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Branch oldItem,
                        @NonNull Branch newItem
                ) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Branch oldItem,
                        @NonNull Branch newItem
                ) {
                    return Objects.equals(
                            oldItem.getName(),
                            newItem.getName()
                    ) && Objects.equals(
                            oldItem.getAddress(),
                            newItem.getAddress()
                    ) && Objects.equals(
                            oldItem.getWorkingHours(),
                            newItem.getWorkingHours()
                    ) && Double.compare(
                            oldItem.getRating(),
                            newItem.getRating()
                    ) == 0 && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public BranchAdapter(OnBranchClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEW HOLDER
    // ========================================

    @NonNull
    @Override
    public BranchViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_guest_branch,
                        parent,
                        false
                );

        return new BranchViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU LÊN CARD
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull BranchViewHolder holder,
            int position
    ) {

        Branch branch = getItem(position);

        // Tên chi nhánh
        holder.txtBranchName.setText(
                branch.getName()
        );

        // Địa chỉ chi nhánh
        holder.txtBranchAddress.setText(
                branch.getAddress()
        );

        // Giờ hoạt động
        holder.txtBranchHours.setText(
                branch.getWorkingHours()
        );

        // Đánh giá
        holder.txtBranchRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f",
                        branch.getRating()
                )
        );

        // Hình ảnh chi nhánh
        holder.imgBranch.setImageResource(
                branch.getImageResId()
        );

        // Xử lý khi nhấn vào Card chi nhánh
        holder.itemView.setOnClickListener(v -> {

            if (listener != null) {
                listener.onBranchClick(branch);
            }

        });
    }

    // ========================================
    // 6. VIEW HOLDER
    // ========================================

    static class BranchViewHolder
            extends RecyclerView.ViewHolder {

        ImageView imgBranch;

        TextView txtBranchName;
        TextView txtBranchAddress;
        TextView txtBranchHours;
        TextView txtBranchRating;

        public BranchViewHolder(@NonNull View itemView) {
            super(itemView);

            imgBranch = itemView.findViewById(
                    R.id.imgBranch
            );

            txtBranchName = itemView.findViewById(
                    R.id.txtBranchName
            );

            txtBranchAddress = itemView.findViewById(
                    R.id.txtBranchAddress
            );

            txtBranchHours = itemView.findViewById(
                    R.id.txtBranchHours
            );

            txtBranchRating = itemView.findViewById(
                    R.id.txtBranchRating
            );
        }
    }
}
