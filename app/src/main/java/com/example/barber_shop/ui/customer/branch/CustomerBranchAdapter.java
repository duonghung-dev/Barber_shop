
package com.example.barber_shop.ui.customer.branch;

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

public class CustomerBranchAdapter extends
        ListAdapter<Branch, CustomerBranchAdapter.BranchViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ CLICK
    // ========================================

    public interface OnBranchClickListener {
        void onBranchClick(Branch branch);
    }

    private final OnBranchClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU
    // ========================================

    private static final DiffUtil.ItemCallback<Branch>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Branch>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Branch oldItem,
                        @NonNull Branch newItem
                ) {
                    return oldItem.getId().equals(
                            newItem.getId()
                    );
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
                    ) == 0
                            && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerBranchAdapter(
            OnBranchClickListener listener
    ) {
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

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_customer_branch,
                parent,
                false
        );

        return new BranchViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU CHI NHÁNH
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull BranchViewHolder holder,
            int position
    ) {

        Branch branch = getItem(position);

        holder.imgBranch.setImageResource(
                branch.getImageResId()
        );

        holder.txtName.setText(
                branch.getName()
        );

        holder.txtAddress.setText(
                branch.getAddress()
        );

        holder.txtHours.setText(
                "Giờ hoạt động: " + branch.getWorkingHours()
        );

        holder.txtRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f / 5.0",
                        branch.getRating()
                )
        );

        // Xử lý khi nhấn một chi nhánh
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

        TextView txtName;
        TextView txtAddress;
        TextView txtHours;
        TextView txtRating;

        BranchViewHolder(@NonNull View itemView) {
            super(itemView);

            imgBranch = itemView.findViewById(
                    R.id.imgCustomerBranch
            );

            txtName = itemView.findViewById(
                    R.id.txtCustomerBranchName
            );

            txtAddress = itemView.findViewById(
                    R.id.txtCustomerBranchAddress
            );

            txtHours = itemView.findViewById(
                    R.id.txtCustomerBranchHours
            );

            txtRating = itemView.findViewById(
                    R.id.txtCustomerBranchRating
            );
        }
    }
}
