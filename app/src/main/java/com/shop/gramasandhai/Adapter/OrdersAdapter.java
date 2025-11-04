package com.shop.gramasandhai.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.shop.gramasandhai.Model.Order;
import com.shop.gramasandhai.Model.Variant;
import com.shop.gramasandhai.R;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.OrderViewHolder> {

    private List<Order> orders;
    private OnOrderClickListener orderClickListener;
    private OnStatusUpdateListener statusUpdateListener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public interface OnStatusUpdateListener {
        void onStatusUpdateClick(Order order);
    }

    public OrdersAdapter(List<Order> orders, OnOrderClickListener orderClickListener,
                         OnStatusUpdateListener statusUpdateListener) {
        this.orders = orders;
        this.orderClickListener = orderClickListener;
        this.statusUpdateListener = statusUpdateListener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        holder.bind(order, orderClickListener, statusUpdateListener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class OrderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvOrderId, tvOrderStatus, tvCustomerName, tvOrderDate;
        private TextView tvProductsPreview, tvItemsCount, tvOrderAmount;
        private Button btnViewDetails, btnUpdateStatus;
        private MaterialCardView cardView;
        private LinearLayout productsHeader, variantsContainer;
        private ImageView ivExpandArrow;
        private RecyclerView variantsRecyclerView;
        private VariantsAdapter variantsAdapter;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize views
            cardView = itemView.findViewById(R.id.cardView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvProductsPreview = itemView.findViewById(R.id.tvProductsPreview);
            tvItemsCount = itemView.findViewById(R.id.tvItemsCount);
            tvOrderAmount = itemView.findViewById(R.id.tvOrderAmount);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnUpdateStatus = itemView.findViewById(R.id.btnUpdateStatus);
            productsHeader = itemView.findViewById(R.id.productsHeader);
            variantsContainer = itemView.findViewById(R.id.variantsContainer);
            ivExpandArrow = itemView.findViewById(R.id.ivExpandArrow);
            variantsRecyclerView = itemView.findViewById(R.id.variantsRecyclerView);

            // Setup variants recyclerview
            variantsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            variantsAdapter = new VariantsAdapter();
            variantsRecyclerView.setAdapter(variantsAdapter);
        }

        public void bind(Order order, OnOrderClickListener orderClickListener,
                         OnStatusUpdateListener statusUpdateListener) {

            // Set order data
            tvOrderId.setText("Order #" + order.getOrderId());
            tvOrderStatus.setText(order.getOrderStatus());
            tvCustomerName.setText(order.getCustomerName());
            tvOrderDate.setText(order.getOrderedDate());

            // Set formatted amount with Indian currency format
            String formattedAmount = formatIndianCurrency(Double.valueOf(order.getAmount()));
            tvOrderAmount.setText(formattedAmount);

            tvItemsCount.setText(order.getItemsCount() + " items");

            // Set products preview
            if (order.getProducts() != null && !order.getProducts().isEmpty()) {
                String productsPreview = String.join(", ", order.getProducts());
                if (productsPreview.length() > 50) {
                    productsPreview = productsPreview.substring(0, 47) + "...";
                }
                tvProductsPreview.setText(productsPreview);
            }

            // Set variants data
            if (order.getVariants() != null && !order.getVariants().isEmpty()) {
                variantsAdapter.setVariants(order.getVariants());
            }

            // Set expand/collapse state
            setExpandState(order.isExpanded());

            // Set status background and button text
            setStatusUI(order.getOrderStatus());

            // Set click listeners
            btnUpdateStatus.setOnClickListener(v -> statusUpdateListener.onStatusUpdateClick(order));

            // Products header click listener for expand/collapse
            productsHeader.setOnClickListener(v -> {
                boolean isExpanded = order.isExpanded();
                order.setExpanded(!isExpanded);
                setExpandState(!isExpanded);
                notifyItemChanged(getAdapterPosition());
            });

            // Show/hide View Details button based on order status
            if ("COMPLETED".equals(order.getOrderStatus())) {
                btnViewDetails.setVisibility(View.GONE);
            } else if ("CANCELLED".equals(order.getOrderStatus())) {
                btnViewDetails.setVisibility(View.GONE);
            } else {
                btnViewDetails.setVisibility(View.VISIBLE);
                btnViewDetails.setOnClickListener(v -> orderClickListener.onOrderClick(order));
            }
        }

        private void setExpandState(boolean isExpanded) {
            if (isExpanded) {
                variantsContainer.setVisibility(View.VISIBLE);
                ivExpandArrow.setImageResource(R.drawable.ic_expand_less);
            } else {
                variantsContainer.setVisibility(View.GONE);
                ivExpandArrow.setImageResource(R.drawable.ic_expand_more);
            }
        }

        private void setStatusUI(String status) {
            int backgroundRes;
            String buttonText;

            switch (status) {
                case "PENDING":
                    backgroundRes = R.drawable.status_bg_pending;
                    buttonText = "Process";
                    break;
                case "PROCESSING":
                    backgroundRes = R.drawable.status_bg_processing;
                    buttonText = "Complete";
                    break;
                case "COMPLETED":
                    backgroundRes = R.drawable.status_bg_completed;
                    buttonText = "View";
                    break;
                case "CANCELLED":
                    backgroundRes = R.drawable.status_bg_cancelled;
                    buttonText = "View";
                    break;
                default:
                    backgroundRes = R.drawable.status_bg_pending;
                    buttonText = "Process";
            }

            tvOrderStatus.setBackgroundResource(backgroundRes);
            btnUpdateStatus.setText(buttonText);
        }

        // Helper method to format currency in Indian format
        private String formatIndianCurrency(double amount) {
            try {
                // Round to nearest integer
                long roundedAmount = Math.round(amount);

                // Use Indian locale for formatting with commas
                DecimalFormat indianFormat = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("en", "IN"));
                indianFormat.setGroupingUsed(true);
                indianFormat.setMaximumFractionDigits(0);

                return "₹" + indianFormat.format(roundedAmount);
            } catch (Exception e) {
                // Fallback to simple format
                return "₹" + Math.round(amount);
            }
        }
    }

    // Variants Adapter inner class
    private static class VariantsAdapter extends RecyclerView.Adapter<VariantsAdapter.VariantViewHolder> {

        private List<Variant> variants;

        public void setVariants(List<Variant> variants) {
            this.variants = variants;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VariantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.variant_item, parent, false);
            return new VariantViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VariantViewHolder holder, int position) {
            Variant variant = variants.get(position);
            holder.bind(variant, position);
        }

        @Override
        public int getItemCount() {
            return variants != null ? variants.size() : 0;
        }

        static class VariantViewHolder extends RecyclerView.ViewHolder {
            private TextView tvProductName, tvQuantity, tvWeight, tvProductPrice;
            private View itemBackground;

            public VariantViewHolder(@NonNull View itemView) {
                super(itemView);
                tvProductName = itemView.findViewById(R.id.tvProductName);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvWeight = itemView.findViewById(R.id.tvWeight);
                tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
                itemBackground = itemView; // The root view of variant_item layout
            }

            public void bind(Variant variant, int position) {
                tvProductName.setText(variant.getProdName());
                tvQuantity.setText("Qty: " + variant.getProdQty());
                tvWeight.setText(variant.getWeight() + "g");

                // Format product price with Indian currency
                String formattedPrice = formatIndianCurrency(Double.valueOf(variant.getProdPrice()));
                tvProductPrice.setText(formattedPrice);

                // Set alternating background colors
                if (position % 2 == 0) {
                    // Even positions - lighter color
                    itemBackground.setBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.variant_bg_even)
                    );
                } else {
                    // Odd positions - slightly darker color
                    itemBackground.setBackgroundColor(
                            ContextCompat.getColor(itemView.getContext(), R.color.variant_bg_odd)
                    );
                }
            }

            // Helper method for variant price formatting
            private String formatIndianCurrency(double amount) {
                try {
                    // Round to nearest integer
                    long roundedAmount = Math.round(amount);

                    // Use Indian locale for formatting with commas
                    DecimalFormat indianFormat = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("en", "IN"));
                    indianFormat.setGroupingUsed(true);
                    indianFormat.setMaximumFractionDigits(0);

                    return "₹" + indianFormat.format(roundedAmount);
                } catch (Exception e) {
                    return "₹" + Math.round(amount);
                }
            }
        }
    }
}