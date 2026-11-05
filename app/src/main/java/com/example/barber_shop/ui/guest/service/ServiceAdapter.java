
package com.example.barber_shop.ui.guest.service;

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

public class ServiceAdapter
        extends ListAdapter<SalonService, ServiceAdapter.ServiceViewHolder> {

    public interface OnServiceClickListener {
        void onServiceClick(SalonService service);
    }

    private final OnServiceClickListener listener;

    private static final DiffUtil.ItemCallback<SalonService> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<SalonService>() {

                @Override
                public boolean areItemsTheSame(
                        @NonNull SalonService oldItem,
                        @NonNull SalonService newItem
                ) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(
                        @NonNull SalonService oldItem,
                        @NonNull SalonService newItem
                ) {
                    return Objects.equals(
                            oldItem.getName(), newItem.getName()
                    ) && Objects.equals(
                            oldItem.getCategory(), newItem.getCategory()
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

    public ServiceAdapter(OnServiceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_guest_service, parent, false);

        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ServiceViewHolder holder,
            int position
    ) {
        SalonService service = getItem(position);

        holder.txtServiceName.setText(service.getName());

        holder.txtServiceCategory.setText(service.getCategory());

        holder.txtServiceDescription.setText(
                service.getDescription()
        );

        holder.txtServiceDuration.setText(
                String.format(
                        Locale.getDefault(),
                        "%d phút",
                        service.getDurationMinutes()
                )
        );

        NumberFormat formatter = NumberFormat.getNumberInstance(
                new Locale("vi", "VN")
        );

        holder.txtServicePrice.setText(
                formatter.format(service.getPriceVnd()) + " đ"
        );

        holder.imgService.setImageResource(
                service.getImageResId()
        );

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onServiceClick(service);
            }
        });
    }

    static class ServiceViewHolder
            extends RecyclerView.ViewHolder {

        AppCompatImageView imgService;

        TextView txtServiceName;
        TextView txtServiceCategory;
        TextView txtServiceDescription;
        TextView txtServiceDuration;
        TextView txtServicePrice;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);

            imgService = itemView.findViewById(
                    R.id.imgService
            );

            txtServiceName = itemView.findViewById(
                    R.id.txtServiceName
            );

            txtServiceCategory = itemView.findViewById(
                    R.id.txtServiceCategory
            );

            txtServiceDescription = itemView.findViewById(
                    R.id.txtServiceDescription
            );

            txtServiceDuration = itemView.findViewById(
                    R.id.txtServiceDuration
            );

            txtServicePrice = itemView.findViewById(
                    R.id.txtServicePrice
            );
        }
    }
}
