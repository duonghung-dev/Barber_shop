
package com.example.barber_shop.ui.guest.barber;

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

public class BarberAdapter
        extends ListAdapter<Barber, BarberAdapter.BarberViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ CLICK
    // ========================================

    public interface OnBarberClickListener {
        void onBarberClick(Barber barber);
    }

    private final OnBarberClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU
    // ========================================

    private static final DiffUtil.ItemCallback<Barber> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Barber>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull Barber oldItem,
                        @NonNull Barber newItem
                ) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull Barber oldItem,
                        @NonNull Barber newItem
                ) {
                    return Objects.equals(
                            oldItem.getName(),
                            newItem.getName()
                    ) && Objects.equals(
                            oldItem.getBranchId(),
                            newItem.getBranchId()
                    ) && Objects.equals(
                            oldItem.getSpecialty(),
                            newItem.getSpecialty()
                    ) && oldItem.getExperienceYears()
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

    public BarberAdapter(OnBarberClickListener listener) {
        super(DIFF_CALLBACK);
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

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_guest_barber,
                        parent,
                        false
                );

        return new BarberViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull BarberViewHolder holder,
            int position
    ) {

        Barber barber = getItem(position);

        // Họ tên
        holder.txtBarberName.setText(
                barber.getName()
        );

        // Chuyên môn
        holder.txtBarberSpecialty.setText(
                barber.getSpecialty()
        );

        // Chi nhánh
        Branch branch = BranchRepository.findById(
                barber.getBranchId()
        );

        holder.txtBarberBranch.setText(
                branch != null
                        ? branch.getName()
                        : "Chưa có thông tin chi nhánh"
        );

        // Số năm kinh nghiệm
        holder.txtBarberExperience.setText(
                String.format(
                        Locale.getDefault(),
                        "%d năm kinh nghiệm",
                        barber.getExperienceYears()
                )
        );

        // Đánh giá
        holder.txtBarberRating.setText(
                String.format(
                        Locale.getDefault(),
                        "★ %.1f",
                        barber.getRating()
                )
        );

        // Ảnh đại diện
        holder.imgBarber.setImageResource(
                barber.getImageResId()
        );

        // Sự kiện nhấn Card
        holder.itemView.setOnClickListener(v -> {

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

        AppCompatImageView imgBarber;

        TextView txtBarberName;
        TextView txtBarberSpecialty;
        TextView txtBarberBranch;
        TextView txtBarberExperience;
        TextView txtBarberRating;

        BarberViewHolder(@NonNull View itemView) {
            super(itemView);

            imgBarber = itemView.findViewById(
                    R.id.imgBarber
            );

            txtBarberName = itemView.findViewById(
                    R.id.txtBarberName
            );

            txtBarberSpecialty = itemView.findViewById(
                    R.id.txtBarberSpecialty
            );

            txtBarberBranch = itemView.findViewById(
                    R.id.txtBarberBranch
            );

            txtBarberExperience = itemView.findViewById(
                    R.id.txtBarberExperience
            );

            txtBarberRating = itemView.findViewById(
                    R.id.txtBarberRating
            );
        }
    }
}
