
package com.example.barber_shop.ui.guest.search;

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

public class SearchAdapter
        extends ListAdapter<SearchResult, SearchAdapter.SearchViewHolder> {

    // ========================================
    // 1. INTERFACE XỬ LÝ CLICK
    // ========================================

    public interface OnSearchResultClickListener {
        void onResultClick(SearchResult result);
    }

    private final OnSearchResultClickListener listener;

    // ========================================
    // 2. SO SÁNH DỮ LIỆU
    // ========================================

    private static final DiffUtil.ItemCallback<SearchResult>
            DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SearchResult>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull SearchResult oldItem,
                        @NonNull SearchResult newItem
                ) {
                    return oldItem.getType() == newItem.getType()
                            && oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SearchResult oldItem,
                        @NonNull SearchResult newItem
                ) {
                    return Objects.equals(
                            oldItem.getTitle(),
                            newItem.getTitle()
                    ) && Objects.equals(
                            oldItem.getSubtitle(),
                            newItem.getSubtitle()
                    ) && Objects.equals(
                            oldItem.getDescription(),
                            newItem.getDescription()
                    ) && oldItem.getImageResId()
                            == newItem.getImageResId();
                }
            };

    // ========================================
    // 3. CONSTRUCTOR
    // ========================================

    public SearchAdapter(
            OnSearchResultClickListener listener
    ) {
        super(DIFF_CALLBACK);
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

        View view = LayoutInflater.from(
                parent.getContext()
        ).inflate(
                R.layout.item_guest_search,
                parent,
                false
        );

        return new SearchViewHolder(view);
    }

    // ========================================
    // 5. HIỂN THỊ KẾT QUẢ
    // ========================================

    @Override
    public void onBindViewHolder(
            @NonNull SearchViewHolder holder,
            int position
    ) {

        SearchResult result = getItem(position);

        holder.txtType.setText(
                result.getTypeLabel()
        );

        holder.txtTitle.setText(
                result.getTitle()
        );

        holder.txtSubtitle.setText(
                result.getSubtitle()
        );

        holder.txtDescription.setText(
                result.getDescription()
        );

        holder.imgResult.setImageResource(
                result.getImageResId()
        );

        holder.itemView.setOnClickListener(v -> {

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

        AppCompatImageView imgResult;

        TextView txtType;
        TextView txtTitle;
        TextView txtSubtitle;
        TextView txtDescription;

        SearchViewHolder(@NonNull View itemView) {
            super(itemView);

            imgResult = itemView.findViewById(
                    R.id.imgSearchResult
            );

            txtType = itemView.findViewById(
                    R.id.txtSearchResultType
            );

            txtTitle = itemView.findViewById(
                    R.id.txtSearchResultTitle
            );

            txtSubtitle = itemView.findViewById(
                    R.id.txtSearchResultSubtitle
            );

            txtDescription = itemView.findViewById(
                    R.id.txtSearchResultDescription
            );
        }
    }
}
