package com.shop.gramasandhai.Adapter;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.shop.gramasandhai.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class VariantAdapter extends RecyclerView.Adapter<VariantAdapter.VariantViewHolder> {

    private static final String TAG = "VariantAdapter";
    private List<JSONObject> variantsList;
    private OnVariantChangeListener listener;

    // Constants for discount types
    private static final String DISCOUNT_TYPE_NONE = "0";
    private static final String DISCOUNT_TYPE_FLAT = "1";
    private static final String DISCOUNT_TYPE_PERCENTAGE = "2";

    public interface OnVariantChangeListener {
        void onVariantStatusChanged(int position, boolean isActive, String statusValue);
        void onVariantUpdateClicked(int position, JSONObject updatedVariant);
        void onVariantPriceChanged(int position, String newPrice);
        void onVariantDiscPriceChanged(int position, String newDiscPrice);
        void onVariantDiscountTypeChanged(int position, String discountType, String discountValue);
        void onVariantDeleteClicked(int position, String variantId); // Add delete listener
    }

    public VariantAdapter(List<JSONObject> variantsList, OnVariantChangeListener listener) {
        this.variantsList = variantsList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VariantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variant, parent, false);
        return new VariantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VariantViewHolder holder, int position) {
        try {
            JSONObject variant = variantsList.get(position);
            holder.bindData(variant, position);
        } catch (Exception e) {
            Log.e(TAG, "Error in onBindViewHolder at position " + position + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return variantsList != null ? variantsList.size() : 0;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateVariantsList(List<JSONObject> newVariantsList) {
        this.variantsList = newVariantsList;
        notifyDataSetChanged();
    }

    public void updateVariant(int position, JSONObject updatedVariant) {
        if (position >= 0 && position < variantsList.size()) {
            variantsList.set(position, updatedVariant);
            notifyItemChanged(position);
        }
    }

    // Add method to remove variant from list
    public void removeVariant(int position) {
        if (position >= 0 && position < variantsList.size()) {
            variantsList.remove(position);
            notifyItemRemoved(position);
            // Notify about range change for positions after removed item
            notifyItemRangeChanged(position, variantsList.size() - position);
        }
    }

    class VariantViewHolder extends RecyclerView.ViewHolder {
        TextView tvVariantMeasure, tvSkuCode, tvStock, tvDiscountBadge, tvDiscountLabel, tvActualPrice;
        EditText etVariantPrice, etDiscountValue;
        SwitchCompat switchVariantStatus;
        MaterialButtonToggleGroup toggleDiscountType;
        MaterialButton btnNoDiscount, btnFlat, btnPercentage, btnUpdateVariant, btnDeleteVariant; // Add delete button
        LinearLayout layoutDiscountValue, layoutActualPrice;

        private boolean isUpdating = false;
        private String currentDiscountType = DISCOUNT_TYPE_NONE;
        private int currentPosition = -1;
        private JSONObject originalVariant;
        private boolean hasChanges = false;

        // Text watchers to avoid multiple listeners
        private final TextWatcher priceTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdating && currentPosition != RecyclerView.NO_POSITION) {
                    updateActualPrice();
                    updateDiscountBadge();
                    checkForChanges();
                }
            }
        };

        private final TextWatcher discountTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUpdating && currentPosition != RecyclerView.NO_POSITION) {
                    updateActualPrice();
                    updateDiscountBadge();
                    checkForChanges();
                }
            }
        };

        public VariantViewHolder(@NonNull View itemView) {
            super(itemView);
            initializeViews(itemView);
            setupListeners();
        }

        private void initializeViews(View itemView) {
            tvVariantMeasure = itemView.findViewById(R.id.tvVariantMeasure);
            etVariantPrice = itemView.findViewById(R.id.etVariantPrice);
            tvSkuCode = itemView.findViewById(R.id.tvSkuCode);
            tvStock = itemView.findViewById(R.id.tvStock);
            switchVariantStatus = itemView.findViewById(R.id.switchVariantStatus);
            tvDiscountBadge = itemView.findViewById(R.id.tvDiscountBadge);
            toggleDiscountType = itemView.findViewById(R.id.toggleDiscountType);
            btnNoDiscount = itemView.findViewById(R.id.btnNoDiscount);
            btnFlat = itemView.findViewById(R.id.btnFlat);
            btnPercentage = itemView.findViewById(R.id.btnPercentage);
            layoutDiscountValue = itemView.findViewById(R.id.layoutDiscountValue);
            layoutActualPrice = itemView.findViewById(R.id.layoutActualPrice);
            tvDiscountLabel = itemView.findViewById(R.id.tvDiscountLabel);
            tvActualPrice = itemView.findViewById(R.id.tvActualPrice);
            etDiscountValue = itemView.findViewById(R.id.etDiscountValue);
            btnUpdateVariant = itemView.findViewById(R.id.btnUpdateVariant);
            btnDeleteVariant = itemView.findViewById(R.id.btnDeleteVariant); // Initialize delete button
        }

        @SuppressLint("SetTextI18n")
        public void bindData(JSONObject variant, int position) {
            try {
                isUpdating = true;
                this.currentPosition = position;

                // Store original data for change detection
                if (variant != null) {
                    this.originalVariant = new JSONObject(variant.toString());
                } else {
                    this.originalVariant = new JSONObject();
                }

                this.hasChanges = false;

                // Set basic text data with null checks
                String measure = variant.optString("measure", "");
                String skuCode = variant.optString("sku_code", "");
                String stock = variant.optString("stock", "");

                tvVariantMeasure.setText(measure);
                tvSkuCode.setText((!TextUtils.isEmpty(skuCode) ? skuCode : "N/A"));
                tvStock.setText((!TextUtils.isEmpty(stock) ? stock : "0"));

                // Set prices safely with validation
                int price = variant.optInt("price", 0);
                int discPrice = variant.optInt("disc_price", 0);

                etVariantPrice.removeTextChangedListener(priceTextWatcher);
                etVariantPrice.setText(String.valueOf(Math.max(0, price)));
                etVariantPrice.addTextChangedListener(priceTextWatcher);

                // Set discount type and show appropriate layout
                currentDiscountType = variant.optString("disc_type", DISCOUNT_TYPE_NONE);

                // Remove listener temporarily to avoid triggering change detection
                toggleDiscountType.removeOnButtonCheckedListener(discountTypeListener);

                // Set the correct button checked state based on discount type
                if (DISCOUNT_TYPE_FLAT.equals(currentDiscountType)) {
                    toggleDiscountType.check(R.id.btnFlat);
                    showFlatLayout();
                } else if (DISCOUNT_TYPE_PERCENTAGE.equals(currentDiscountType)) {
                    toggleDiscountType.check(R.id.btnPercentage);
                    showPercentageLayout();
                } else {
                    toggleDiscountType.check(R.id.btnNoDiscount);
                    hideDiscountLayout();
                }

                toggleDiscountType.addOnButtonCheckedListener(discountTypeListener);

                // Set discount value
                etDiscountValue.removeTextChangedListener(discountTextWatcher);
                etDiscountValue.setText(String.valueOf(Math.max(0, discPrice)));
                etDiscountValue.addTextChangedListener(discountTextWatcher);

                // Update actual price and discount badge
                updateActualPrice();
                updateDiscountBadge();

                // Set switch status safely
                boolean isActive = "1".equals(variant.optString("status", "0"));
                switchVariantStatus.setOnCheckedChangeListener(null);
                switchVariantStatus.setChecked(isActive);
                setupSwitchListener();

                // Hide update button initially
                btnUpdateVariant.setVisibility(View.GONE);

                isUpdating = false;

            } catch (Exception e) {
                Log.e(TAG, "Error binding data at position " + position + ": " + e.getMessage());
                isUpdating = false;
            }
        }

        private final MaterialButtonToggleGroup.OnButtonCheckedListener discountTypeListener =
                new MaterialButtonToggleGroup.OnButtonCheckedListener() {
                    @Override
                    public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                        if (!isUpdating && currentPosition != RecyclerView.NO_POSITION) {
                            if (isChecked) {
                                if (checkedId == R.id.btnNoDiscount) {
                                    currentDiscountType = DISCOUNT_TYPE_NONE;
                                    hideDiscountLayout();
                                } else if (checkedId == R.id.btnPercentage) {
                                    currentDiscountType = DISCOUNT_TYPE_PERCENTAGE;
                                    showPercentageLayout();
                                } else if (checkedId == R.id.btnFlat) {
                                    currentDiscountType = DISCOUNT_TYPE_FLAT;
                                    showFlatLayout();
                                }
                            }

                            updateActualPrice();
                            updateDiscountBadge();
                            checkForChanges();
                        }
                    }
                };

        private void setupSwitchListener() {
            switchVariantStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isUpdating && listener != null && currentPosition != RecyclerView.NO_POSITION) {
                    String statusValue = isChecked ? "1" : "0";
                    Log.d(TAG, "Switch toggled at position " + currentPosition + ", status: " + statusValue);

                    try {
                        JSONObject variant = variantsList.get(currentPosition);
                        variant.put("status", statusValue);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error updating local status: " + e.getMessage());
                    }

                    listener.onVariantStatusChanged(currentPosition, isChecked, statusValue);
                }
            });
        }

        private void setupListeners() {
            btnUpdateVariant.setOnClickListener(v -> {
                if (listener != null && hasChanges && currentPosition != RecyclerView.NO_POSITION) {
                    try {
                        JSONObject updatedVariant = variantsList.get(currentPosition);

                        // Validate and get current values
                        String priceStr = etVariantPrice.getText().toString().trim();
                        String discountValueStr = etDiscountValue.getText().toString().trim();

                        if (TextUtils.isEmpty(priceStr)) {
                            Log.w(TAG, "Price is empty");
                            return;
                        }

                        // Update the variant with current values
                        updatedVariant.put("price", Integer.parseInt(priceStr));
                        updatedVariant.put("disc_type", currentDiscountType);

                        // For no discount, set discount price to 0
                        if (DISCOUNT_TYPE_NONE.equals(currentDiscountType)) {
                            updatedVariant.put("disc_price", 0);
                            etDiscountValue.setText("0");
                        } else if (!TextUtils.isEmpty(discountValueStr)) {
                            int discountValue = Integer.parseInt(discountValueStr);
                            updatedVariant.put("disc_price", discountValue);
                        } else {
                            updatedVariant.put("disc_price", 0);
                            etDiscountValue.setText("0");
                        }

                        // Update original variant for change detection
                        this.originalVariant = new JSONObject(updatedVariant.toString());

                        // Call update listener
                        listener.onVariantUpdateClicked(currentPosition, updatedVariant);

                        // Hide update button after click
                        btnUpdateVariant.setVisibility(View.GONE);
                        hasChanges = false;

                        // Refresh the UI with updated values
                        updateActualPrice();
                        updateDiscountBadge();

                    } catch (Exception e) {
                        Log.e(TAG, "Error preparing update: " + e.getMessage());
                    }
                }
            });

            // Setup delete button listener
            btnDeleteVariant.setOnClickListener(v -> {
                if (listener != null && currentPosition != RecyclerView.NO_POSITION) {
                    try {
                        JSONObject variant = variantsList.get(currentPosition);
                        String variantId = variant.getString("id");
                        listener.onVariantDeleteClicked(currentPosition, variantId);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error getting variant ID for deletion: " + e.getMessage());
                    }
                }
            });
        }

        private void checkForChanges() {
            if (isUpdating || originalVariant == null || currentPosition == RecyclerView.NO_POSITION) {
                return;
            }

            try {
                String currentPrice = etVariantPrice.getText().toString().trim();
                String currentDiscountValue = etDiscountValue.getText().toString().trim();

                String originalPrice = String.valueOf(originalVariant.optInt("price", 0));
                String originalDiscountType = originalVariant.optString("disc_type", DISCOUNT_TYPE_NONE);
                String originalDiscountValue = String.valueOf(originalVariant.optInt("disc_price", 0));

                boolean priceChanged = !currentPrice.equals(originalPrice);
                boolean discountTypeChanged = !currentDiscountType.equals(originalDiscountType);
                boolean discountValueChanged = !currentDiscountValue.equals(originalDiscountValue);

                hasChanges = priceChanged || discountTypeChanged || discountValueChanged;

                // Show/hide update button based on changes
                btnUpdateVariant.setVisibility(hasChanges ? View.VISIBLE : View.GONE);

            } catch (Exception e) {
                Log.e(TAG, "Error checking for changes: " + e.getMessage());
                btnUpdateVariant.setVisibility(View.GONE);
            }
        }

        private void showPercentageLayout() {
            layoutDiscountValue.setVisibility(View.VISIBLE);
            updateDiscountLabel();
        }

        private void showFlatLayout() {
            layoutDiscountValue.setVisibility(View.VISIBLE);
            updateDiscountLabel();
        }

        private void hideDiscountLayout() {
            layoutDiscountValue.setVisibility(View.GONE);
        }

        private void updateDiscountLabel() {
            if (DISCOUNT_TYPE_FLAT.equals(currentDiscountType)) {
                tvDiscountLabel.setText("Discount Amount");
            } else if (DISCOUNT_TYPE_PERCENTAGE.equals(currentDiscountType)) {
                tvDiscountLabel.setText("Discount Percentage");
            }
        }

        @SuppressLint("SetTextI18n")
        private void updateActualPrice() {
            try {
                String priceStr = etVariantPrice.getText().toString().trim();
                String discountValueStr = etDiscountValue.getText().toString().trim();

                if (TextUtils.isEmpty(priceStr)) {
                    layoutActualPrice.setVisibility(View.GONE);
                    return;
                }

                double price = Double.parseDouble(priceStr);

                if (DISCOUNT_TYPE_NONE.equals(currentDiscountType)) {
                    layoutActualPrice.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(discountValueStr)) {
                    layoutActualPrice.setVisibility(View.GONE);
                    return;
                }

                double discountValue = Double.parseDouble(discountValueStr);

                if (price <= 0 || discountValue <= 0) {
                    layoutActualPrice.setVisibility(View.GONE);
                    return;
                }

                double actualPrice = price;

                if (DISCOUNT_TYPE_FLAT.equals(currentDiscountType)) {
                    actualPrice = Math.max(0, price - discountValue);
                } else if (DISCOUNT_TYPE_PERCENTAGE.equals(currentDiscountType)) {
                    double discountAmount = (price * discountValue) / 100;
                    actualPrice = Math.max(0, price - discountAmount);
                }

                tvActualPrice.setText("₹" + formatDecimal(actualPrice));
                layoutActualPrice.setVisibility(View.VISIBLE);

            } catch (NumberFormatException e) {
                layoutActualPrice.setVisibility(View.GONE);
            } catch (Exception e) {
                Log.e(TAG, "Error updating actual price: " + e.getMessage());
                layoutActualPrice.setVisibility(View.GONE);
            }
        }

        @SuppressLint("SetTextI18n")
        private void updateDiscountBadge() {
            try {
                String priceStr = etVariantPrice.getText().toString().trim();
                String discountValueStr = etDiscountValue.getText().toString().trim();

                // Handle no discount case
                if (DISCOUNT_TYPE_NONE.equals(currentDiscountType)) {
                    tvDiscountBadge.setText("No Discount");
                    tvDiscountBadge.setVisibility(View.VISIBLE);
                    return;
                }

                // Validate inputs
                if (TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(discountValueStr)) {
                    tvDiscountBadge.setText("No Discount");
                    tvDiscountBadge.setVisibility(View.VISIBLE);
                    return;
                }

                double price = Double.parseDouble(priceStr);
                double discountValue = Double.parseDouble(discountValueStr);

                if (price <= 0 || discountValue <= 0) {
                    tvDiscountBadge.setText("No Discount");
                    tvDiscountBadge.setVisibility(View.VISIBLE);
                    return;
                }

                // Update badge based on CURRENT discount type and CURRENT discount value
                tvDiscountBadge.setVisibility(View.VISIBLE);
                if (DISCOUNT_TYPE_FLAT.equals(currentDiscountType)) {
                    tvDiscountBadge.setText("₹" + formatDecimal(discountValue) + " OFF");
                } else if (DISCOUNT_TYPE_PERCENTAGE.equals(currentDiscountType)) {
                    tvDiscountBadge.setText(formatDecimal(discountValue) + "% OFF");
                }

            } catch (NumberFormatException e) {
                tvDiscountBadge.setText("No Discount");
                tvDiscountBadge.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e(TAG, "Error updating discount badge: " + e.getMessage());
                tvDiscountBadge.setText("No Discount");
                tvDiscountBadge.setVisibility(View.VISIBLE);
            }
        }

        private String formatDecimal(double value) {
            if (value == (int) value) {
                return String.valueOf((int) value);
            } else {
                return String.format("%.2f", value).replaceAll("0*$", "").replaceAll("\\.$", "");
            }
        }

        public void clearFocus() {
            if (etVariantPrice != null) {
                etVariantPrice.clearFocus();
            }
            if (etDiscountValue != null) {
                etDiscountValue.clearFocus();
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull VariantViewHolder holder) {
        super.onViewRecycled(holder);
        holder.clearFocus();
    }
}