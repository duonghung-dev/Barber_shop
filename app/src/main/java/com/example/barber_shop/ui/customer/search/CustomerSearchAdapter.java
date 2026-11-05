
package com.example.barber_shop.ui.customer.search;

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
import com.example.barber_shop.data.model.SearchResult;

import java.util.Objects;

public class CustomerSearchAdapter
        extends ListAdapter<SearchResult,
        CustomerSearchAdapter.SearchViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ SỰ KIỆN CLICK
    // ========================================

    public interface OnSearchResultClickListener {

        void onResultClick(SearchResult result);
    }

    // Listener nhận từ CustomerSearchActivity
    private final OnSearchResultClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU VỚI DIFFUTIL
    // ========================================

    private static final DiffUtil.ItemCallback<SearchResult>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SearchResult>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull SearchResult oldItem,
                        @NonNull SearchResult newItem
                ) {

                    // Một kết quả được xác định bởi:
                    // - Loại kết quả
                    // - ID của kết quả
                    //
                    // Ví dụ:
                    // BRANCH + CN001
                    // SERVICE + DV001
                    // BARBER + BR001

                    return oldItem.getType() == newItem.getType()
                            && Objects.equals(
                            oldItem.getId(),
                            newItem.getId()
                    );
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SearchResult oldItem,
                        @NonNull SearchResult newItem
                ) {

                    // So sánh các thuộc tính
                    // được hiển thị trên Card

                    return Objects.equals(
                            oldItem.getTitle(),
                            newItem.getTitle()
                    )
                            && Objects.equals(
                            oldItem.getSubtitle(),
                            newItem.getSubtitle()
                    )
                            && Objects.equals(
                            oldItem.getDescription(),
                            newItem.getDescription()
                    )
                            && Objects.equals(
                            oldItem.getTypeLabel(),
                            newItem.getTypeLabel()
                    )
                            && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public CustomerSearchAdapter(
            OnSearchResultClickListener listener
    ) {

        // Truyền DiffUtil cho ListAdapter
        super(DIFF_CALLBACK);

        // Lưu listener để xử lý sự kiện click
        this.listener = listener;
    }

    // ========================================
    // 4. TẠO VIEW HOLDER
    // ========================================

    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        // Sử dụng layout riêng của Customer
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.item_customer_search,
                        parent,
                        false
                );

        // Tạo ViewHolder cho Card
        return new SearchViewHolder(view);
    }

    // ========================================
    // 5. GÁN DỮ LIỆU CHO TỪNG CARD
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull SearchViewHolder holder,
            int position
    ) {

        // Lấy kết quả tại vị trí hiện tại
        SearchResult result = getItem(position);

        // ========================================
        // 5.1. HIỂN THỊ LOẠI KẾT QUẢ
        // ========================================

        // Ví dụ: Chi nhánh, Dịch vụ, Barber
        holder.txtType.setText(
                result.getTypeLabel()
        );

        // ========================================
        // 5.2. HIỂN THỊ TIÊU ĐỀ
        // ========================================

        // Ví dụ:
        // - Tên chi nhánh
        // - Tên dịch vụ
        // - Tên Barber
        holder.txtTitle.setText(
                result.getTitle()
        );

        // ========================================
        // 5.3. HIỂN THỊ PHỤ ĐỀ
        // ========================================

        holder.txtSubtitle.setText(
                result.getSubtitle()
        );

        // ========================================
        // 5.4. HIỂN THỊ MÔ TẢ
        // ========================================

        holder.txtDescription.setText(
                result.getDescription()
        );

        // ========================================
        // 5.5. HIỂN THỊ HÌNH ẢNH
        // ========================================

        holder.imgResult.setImageResource(
                result.getImageResId()
        );

        // ========================================
        // 5.6. XỬ LÝ SỰ KIỆN NHẤN CARD
        // ========================================

        holder.itemView.setOnClickListener(v -> {

            // Gửi kết quả được chọn về Activity
            if (listener != null) {

                listener.onResultClick(result);
            }
        });
    }

    // ========================================
    // 6. VIEW HOLDER
    // ========================================

    static class SearchViewHolder
            extends RecyclerView.ViewHolder {

        // Hình ảnh kết quả
        AppCompatImageView imgResult;

        // Loại kết quả
        TextView txtType;

        // Tiêu đề
        TextView txtTitle;

        // Phụ đề
        TextView txtSubtitle;

        // Mô tả
        TextView txtDescription;

        // ========================================
        // 7. ÁNH XẠ VIEW TRONG CARD
        // ========================================

        SearchViewHolder(
                @NonNull View itemView
        ) {
            super(itemView);

            // Hình ảnh
            imgResult = itemView.findViewById(
                    R.id.imgCustomerSearchResult
            );

            // Loại kết quả
            txtType = itemView.findViewById(
                    R.id.txtCustomerSearchResultType
            );

            // Tiêu đề
            txtTitle = itemView.findViewById(
                    R.id.txtCustomerSearchResultTitle
            );

            // Phụ đề
            txtSubtitle = itemView.findViewById(
                    R.id.txtCustomerSearchResultSubtitle
            );

            // Mô tả
            txtDescription = itemView.findViewById(
                    R.id.txtCustomerSearchResultDescription
            );
        }
    }
}
