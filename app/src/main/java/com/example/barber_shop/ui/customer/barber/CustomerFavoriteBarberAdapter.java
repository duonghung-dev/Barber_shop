
package com.example.barber_shop.ui.customer.barber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.barber_shop.R;
import com.example.barber_shop.data.model.Barber;
import com.example.barber_shop.data.model.Branch;
import com.example.barber_shop.data.repository.BranchRepository;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public class CustomerFavoriteBarberAdapter
        extends RecyclerView.Adapter<
        CustomerFavoriteBarberAdapter.FavoriteBarberViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ SỰ KIỆN
    // ========================================

    public interface OnFavoriteBarberActionListener {

        // Customer chọn xem chi tiết Barber
        void onBarberClick(Barber barber);

        // Customer nhấn trái tim để bỏ yêu thích
        void onRemoveFavorite(Barber barber);
    }

    // ========================================
    // 2. KHAI BÁO DỮ LIỆU
    // ========================================

    // Danh sách Barber được hiển thị
    private final List<Barber> favoriteBarbers =
            new ArrayList<>();

    // Interface nhận sự kiện từ Adapter
    private final OnFavoriteBarberActionListener listener;

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerFavoriteBarberAdapter(
            @NonNull OnFavoriteBarberActionListener listener
    ) {

        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEWHOLDER
    // ========================================

    @NonNull
    @Override
    public FavoriteBarberViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        // ========================================
        // 4.1. INFLATE ITEM XML
        // ========================================

        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_customer_favorite_barber,
                        parent,
                        false
                );

        // ========================================
        // 4.2. TRẢ VỀ VIEWHOLDER
        // ========================================

        return new FavoriteBarberViewHolder(
                itemView
        );
    }

    // ========================================
    // 5. HIỂN THỊ DỮ LIỆU BARBER
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull FavoriteBarberViewHolder holder,
            int position
    ) {

        // ========================================
        // 5.1. LẤY BARBER TẠI VỊ TRÍ HIỆN TẠI
        // ========================================

        Barber barber = favoriteBarbers.get(
                position
        );

        // ========================================
        // 5.2. HIỂN THỊ TÊN BARBER
        // ========================================

        String barberName = normalizeValue(
                barber.getName()
        );

        if (barberName == null) {

            barberName = "Barber";
        }

        holder.txtBarberName.setText(
                barberName
        );

        // ========================================
        // 5.3. HIỂN THỊ ẢNH BARBER
        // ========================================

        int imageResId = barber.getImageResId();

        if (imageResId != 0) {

            holder.imgBarber.setImageResource(
                    imageResId
            );

            // Ảnh thật hiển thị không bị áp màu
            holder.imgBarber.clearColorFilter();

            holder.imgBarber.setPadding(
                    0, 0, 0, 0
            );

            holder.imgBarber.setScaleType(
                    ImageView.ScaleType.CENTER_CROP
            );

        } else {

            // Sử dụng biểu tượng mặc định nếu
            // Barber chưa có ảnh đại diện.

            holder.imgBarber.setImageResource(
                    android.R.drawable.ic_menu_myplaces
            );

            holder.imgBarber.setPadding(
                    dpToPx(holder.itemView, 18),
                    dpToPx(holder.itemView, 18),
                    dpToPx(holder.itemView, 18),
                    dpToPx(holder.itemView, 18)
            );

            holder.imgBarber.setScaleType(
                    ImageView.ScaleType.CENTER_INSIDE
            );
        }

        // ========================================
        // 5.4. HIỂN THỊ CHI NHÁNH
        // ========================================

        String branchName =
                getBarberBranchName(barber);

        holder.txtBarberBranch.setText(
                branchName
        );

        // ========================================
        // 5.5. GẮN SỰ KIỆN XEM CHI TIẾT
        // ========================================

        // Customer nhấn vào toàn bộ Card
        holder.cardFavoriteBarber.setOnClickListener(
                v -> {

                    Barber selectedBarber =
                            getBarberAtPosition(holder);

                    if (selectedBarber != null) {

                        listener.onBarberClick(
                                selectedBarber
                        );
                    }
                }
        );

        // Customer nhấn nút Xem chi tiết
        holder.btnViewDetail.setOnClickListener(
                v -> {

                    Barber selectedBarber =
                            getBarberAtPosition(holder);

                    if (selectedBarber != null) {

                        listener.onBarberClick(
                                selectedBarber
                        );
                    }
                }
        );

        // ========================================
        // 5.6. GẮN SỰ KIỆN BỎ YÊU THÍCH
        // ========================================

        // Chỉ thông báo Barber cần xóa về Activity.
        // Không tự xóa dữ liệu khỏi Adapter
        // khi Repository chưa trả kết quả thành công.

        holder.btnRemoveFavorite.setOnClickListener(
                v -> {

                    Barber selectedBarber =
                            getBarberAtPosition(holder);

                    if (selectedBarber != null) {

                        listener.onRemoveFavorite(
                                selectedBarber
                        );
                    }
                }
        );
    }

    // ========================================
    // 6. LẤY BARBER TỪ VIEWHOLDER AN TOÀN
    // ========================================

    private Barber getBarberAtPosition(
            FavoriteBarberViewHolder holder
    ) {

        // ========================================
        // 6.1. LẤY VỊ TRÍ HIỆN TẠI
        // ========================================

        // Không lưu cố định position từ onBind
        // vì danh sách có thể thay đổi sau khi xóa.

        int position =
                holder.getBindingAdapterPosition();

        // ========================================
        // 6.2. KIỂM TRA VỊ TRÍ
        // ========================================

        if (position == RecyclerView.NO_POSITION
                || position < 0
                || position >= favoriteBarbers.size()) {

            return null;
        }

        // ========================================
        // 6.3. TRẢ VỀ BARBER
        // ========================================

        return favoriteBarbers.get(position);
    }

    // ========================================
    // 7. LẤY TÊN CHI NHÁNH CỦA BARBER
    // ========================================

    private String getBarberBranchName(
            Barber barber
    ) {

        if (barber == null) {

            return "Chưa cập nhật chi nhánh";
        }

        // ========================================
        // 7.1. LẤY BRANCH ID
        // ========================================

        String branchId = normalizeValue(
                barber.getBranchId()
        );

        if (branchId == null) {

            return "Chưa cập nhật chi nhánh";
        }

        // ========================================
        // 7.2. TÌM CHI NHÁNH
        // ========================================

        Branch branch = BranchRepository.findById(
                branchId
        );

        if (branch == null) {

            return "Chưa cập nhật chi nhánh";
        }

        // ========================================
        // 7.3. LẤY TÊN CHI NHÁNH
        // ========================================

        String branchName = normalizeValue(
                branch.getName()
        );

        if (branchName == null) {

            return "Chưa cập nhật chi nhánh";
        }

        return branchName;
    }

    // ========================================
    // 8. CẬP NHẬT DANH SÁCH BARBER
    // ========================================

    public void submitList(
            List<Barber> newBarbers
    ) {

        // ========================================
        // 8.1. TẠO DANH SÁCH MỚI
        // ========================================

        List<Barber> updatedList =
                new ArrayList<>();

        // ========================================
        // 8.2. LOẠI BỎ BARBER NULL VÀ ID TRÙNG
        // ========================================

        // Sử dụng LinkedHashMap để giữ thứ tự
        // nhưng không cho một Barber xuất hiện
        // nhiều lần trong RecyclerView.

        LinkedHashMap<String, Barber> uniqueBarbers =
                new LinkedHashMap<>();

        if (newBarbers != null) {

            for (Barber barber : newBarbers) {

                if (barber == null) {
                    continue;
                }

                String barberId = normalizeValue(
                        barber.getId()
                );

                if (barberId == null) {
                    continue;
                }

                if (!uniqueBarbers.containsKey(barberId)) {

                    uniqueBarbers.put(
                            barberId,
                            barber
                    );
                }
            }
        }

        updatedList.addAll(
                uniqueBarbers.values()
        );

        // ========================================
        // 8.3. TÍNH TOÁN THAY ĐỔI DANH SÁCH
        // ========================================

        // DiffUtil giúp RecyclerView cập nhật
        // đúng các item được thêm, xóa hoặc sửa.
        //
        // Không cần làm mới toàn bộ danh sách.

        DiffUtil.DiffResult diffResult =
                DiffUtil.calculateDiff(
                        new DiffUtil.Callback() {

                            @Override
                            public int getOldListSize() {

                                return favoriteBarbers.size();
                            }

                            @Override
                            public int getNewListSize() {

                                return updatedList.size();
                            }

                            @Override
                            public boolean areItemsTheSame(
                                    int oldItemPosition,
                                    int newItemPosition
                            ) {

                                Barber oldBarber =
                                        favoriteBarbers.get(
                                                oldItemPosition
                                        );

                                Barber newBarber =
                                        updatedList.get(
                                                newItemPosition
                                        );

                                return Objects.equals(
                                        oldBarber.getId(),
                                        newBarber.getId()
                                );
                            }

                            @Override
                            public boolean areContentsTheSame(
                                    int oldItemPosition,
                                    int newItemPosition
                            ) {

                                Barber oldBarber =
                                        favoriteBarbers.get(
                                                oldItemPosition
                                        );

                                Barber newBarber =
                                        updatedList.get(
                                                newItemPosition
                                        );

                                // So sánh các thông tin
                                // được hiển thị trên item.

                                return Objects.equals(
                                        oldBarber.getName(),
                                        newBarber.getName()
                                ) && Objects.equals(
                                        oldBarber.getBranchId(),
                                        newBarber.getBranchId()
                                ) && oldBarber.getImageResId()
                                        == newBarber.getImageResId();
                            }
                        }
                );

        // ========================================
        // 8.4. CẬP NHẬT DỮ LIỆU
        // ========================================

        favoriteBarbers.clear();

        favoriteBarbers.addAll(
                updatedList
        );

        // ========================================
        // 8.5. THÔNG BÁO RECYCLERVIEW
        // ========================================

        diffResult.dispatchUpdatesTo(this);
    }

    // ========================================
    // 9. LẤY SỐ LƯỢNG BARBER
    // ========================================

    @Override
    public int getItemCount() {

        return favoriteBarbers.size();
    }

    // ========================================
    // 10. CHUYỂN DP SANG PIXEL
    // ========================================

    private int dpToPx(
            View view,
            int dp
    ) {

        float density = view.getResources()
                .getDisplayMetrics().density;

        return Math.round(
                dp * density
        );
    }

    // ========================================
    // 11. CHUẨN HÓA CHUỖI
    // ========================================

    private String normalizeValue(
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

    // ========================================
    // 12. VIEWHOLDER
    // ========================================

    public static class FavoriteBarberViewHolder
            extends RecyclerView.ViewHolder {

        // Card tổng
        private final MaterialCardView cardFavoriteBarber;

        // Ảnh đại diện
        private final ImageView imgBarber;

        // Thông tin Barber
        private final TextView txtBarberName;
        private final TextView txtBarberBranch;

        // Các nút
        private final MaterialButton btnViewDetail;
        private final TextView btnRemoveFavorite;

        // ========================================
        // 12.1. CONSTRUCTOR VIEWHOLDER
        // ========================================

        public FavoriteBarberViewHolder(
                @NonNull View itemView
        ) {

            super(itemView);

            // ========================================
            // 12.2. ÁNH XẠ CARD
            // ========================================

            cardFavoriteBarber = itemView.findViewById(
                    R.id.cardCustomerFavoriteBarber
            );

            // ========================================
            // 12.3. ÁNH XẠ ẢNH
            // ========================================

            imgBarber = itemView.findViewById(
                    R.id.imgCustomerFavoriteBarber
            );

            // ========================================
            // 12.4. ÁNH XẠ TÊN
            // ========================================

            txtBarberName = itemView.findViewById(
                    R.id.txtCustomerFavoriteBarberName
            );

            // ========================================
            // 12.5. ÁNH XẠ CHI NHÁNH
            // ========================================

            txtBarberBranch = itemView.findViewById(
                    R.id.txtCustomerFavoriteBarberBranch
            );

            // ========================================
            // 12.6. ÁNH XẠ NÚT XEM CHI TIẾT
            // ========================================

            btnViewDetail = itemView.findViewById(
                    R.id.btnCustomerFavoriteViewDetail
            );

            // ========================================
            // 12.7. ÁNH XẠ NÚT BỎ YÊU THÍCH
            // ========================================

            btnRemoveFavorite = itemView.findViewById(
                    R.id.btnCustomerFavoriteRemove
            );
        }
    }
}
