package com.shop.gramasandhai.Adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.shop.gramasandhai.Model.Orders;
import com.shop.gramasandhai.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Orders> orderList;
    private OrderClickListener listener;

    public interface OrderClickListener {
        void onOrderClick(Orders order);
        void onTrackOrder(Orders order);
    }

    public OrderAdapter(List<Orders> orderList, OrderClickListener listener) {
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Orders order = orderList.get(position);
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public void updateData(List<Orders> newOrders) {
        // Clear existing data and add all new data
        if (orderList != null) {
            orderList.clear();
            if (newOrders != null) {
                orderList.addAll(newOrders);
            }
        } else {
            orderList = new ArrayList<>(newOrders);
        }
        notifyDataSetChanged(); // This is crucial - it refreshes the RecyclerView
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrderId, tvOrderDate, tvAmount, tvPaymentMethod, tvItems, tvDeliveryAddress;
        private Chip chipStatus;
        private MaterialButton btnViewDetails, btnTrack;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvItems = itemView.findViewById(R.id.tvItems); // Make sure this exists in your XML
            tvDeliveryAddress = itemView.findViewById(R.id.tvDeliveryAddress);
            chipStatus = itemView.findViewById(R.id.chipStatus);
//            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
//            btnTrack = itemView.findViewById(R.id.btnTrack);

            // Debug: Check if views are found
            Log.d("OrderAdapter", "tvOrderId: " + (tvOrderId != null));
            Log.d("OrderAdapter", "chipStatus: " + (chipStatus != null));
        }

        public void bind(Orders order, OrderClickListener listener) {
            Log.d("OrderAdapter", "Binding order: " + order.getOrderId());

            // Set order data with null checks
            if (tvOrderId != null) {
                tvOrderId.setText("Order #" + (order.getOrderId() != null ? order.getOrderId() : "N/A"));
            }
            if (tvAmount != null) {
                tvAmount.setText("₹" + (order.getAmount() != null ? order.getAmount() : "0"));
            }
            if (tvPaymentMethod != null) {
                tvPaymentMethod.setText(getPaymentMethodEmoji(order.getPaymentMethod()) + " " +
                        (order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A"));
            }
            if (tvDeliveryAddress != null) {
                String address = order.getShippingAddress() != null ? order.getShippingAddress() : "N/A";
                String city = order.getCity() != null ? order.getCity() : "";
                tvDeliveryAddress.setText("🏠 " + address + (city.isEmpty() ? "" : ", " + city));
            }

            // For items count - you might need to add this field to your Orders model
            if (tvItems != null) {
                tvItems.setText("📦 1 item"); // Default value - adjust based on your data
            }

            // Format date
            if (tvOrderDate != null) {
                String formattedDate = formatDate(order.getCreatedAt());
                tvOrderDate.setText(formattedDate);
            }

            // Set status
            setOrderStatus(order.getOrderStatus());

            // Set click listeners
//            if (btnViewDetails != null) {
//                btnViewDetails.setOnClickListener(v -> {
//                    Log.d("OrderAdapter", "View details clicked for: " + order.getOrderId());
//                    listener.onOrderClick(order);
//                });
//            }
//            if (btnTrack != null) {
//                btnTrack.setOnClickListener(v -> {
//                    Log.d("OrderAdapter", "Track clicked for: " + order.getOrderId());
//                    listener.onTrackOrder(order);
//                });
//            }

            itemView.setOnClickListener(v -> {
                Log.d("OrderAdapter", "Item clicked: " + order.getOrderId());
                listener.onOrderClick(order);
            });
        }

        private void setOrderStatus(String status) {
            if (chipStatus == null) {
                Log.e("OrderAdapter", "chipStatus is null!");
                return;
            }

            String statusText;
            int colorResource;

            if (status == null) {
                statusText = "Unknown";
                colorResource = R.color.status_pending;
            } else {
                switch (status) {
                    case "COM":
                        statusText = "Completed";
                        colorResource = R.color.status_completed;
                        break;
                    case "PRS":
                        statusText = "Processing";
                        colorResource = R.color.status_processing;
                        break;
                    case "CNL":
                        statusText = "Cancelled";
                        colorResource = R.color.status_cancelled;
                        break;
                    case "1":
                        statusText = "Pending";
                        colorResource = R.color.status_pending;
                        break;
                    default:
                        statusText = "Unknown";
                        colorResource = R.color.status_pending;
                }
            }

            chipStatus.setText(statusText);
            chipStatus.setChipBackgroundColorResource(colorResource);
            Log.d("OrderAdapter", "Set status: " + statusText);
        }

        private String getPaymentMethodEmoji(String paymentMethod) {
            if (paymentMethod == null) return "💰";

            switch (paymentMethod) {
                case "COD": return "💵";
                case "SPU": return "💳";
                case "ONLINE": return "🌐";
                default: return "💰";
            }
        }

        private String formatDate(String dateString) {
            if (dateString == null || dateString.isEmpty()) {
                return "Unknown Date";
            }

            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                return outputFormat.format(date);
            } catch (ParseException e) {
                return dateString;
            }
        }
    }
}