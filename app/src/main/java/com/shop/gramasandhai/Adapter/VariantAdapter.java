package com.shop.gramasandhai.Adapter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
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
        void onVariantDeleteClicked(int position, String variantId);
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
        TextView tvVariantMeasure, tvSkuCode, tvStock, tvDiscountBadge, tvDiscountLabel, tvActualPrice,etVariantPrice;
        TextView  etDiscountValue;
        SwitchCompat switchVariantStatus;
        MaterialButtonToggleGroup toggleDiscountType;
        MaterialButton btnNoDiscount, btnFlat, btnPercentage, btnUpdateVariant, btnDeleteVariant, btnEditVariant;
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
//            btnUpdateVariant = itemView.findViewById(R.id.btnUpdateVariant);
            btnDeleteVariant = itemView.findViewById(R.id.btnDeleteVariant);
            btnEditVariant = itemView.findViewById(R.id.btnEditVariant);
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

//                etVariantPrice.removeTextChangedListener(priceTextWatcher);
                etVariantPrice.setText(String.valueOf(Math.max(0, price)));
//                etVariantPrice.addTextChangedListener(priceTextWatcher);

                // Set discount type and show appropriate layout
                currentDiscountType = variant.optString("disc_type", DISCOUNT_TYPE_NONE);

                // Remove listener temporarily to avoid triggering change detection
//                toggleDiscountType.removeOnButtonCheckedListener(discountTypeListener);

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

//                toggleDiscountType.addOnButtonCheckedListener(discountTypeListener);

                // Set discount value
//                etDiscountValue.removeTextChangedListener(discountTextWatcher);
                etDiscountValue.setText(String.valueOf(Math.max(0, discPrice)));
//                etDiscountValue.addTextChangedListener(discountTextWatcher);

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

            // Setup edit button listener
            btnEditVariant.setOnClickListener(v -> {
                if (listener != null && currentPosition != RecyclerView.NO_POSITION) {
                    try {
                        JSONObject variant = variantsList.get(currentPosition);
                        showEditVariantDialog(currentPosition, variant);
                    } catch (Exception e) {
                        Log.e(TAG, "Error opening edit dialog: " + e.getMessage());
                    }
                }
            });
        }

        private void showEditVariantDialog(int position, JSONObject variant) {
            try {
                Context context = itemView.getContext();

                // Create custom dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialog);
                View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_variant_price, null);
                builder.setView(dialogView);

                AlertDialog dialog = builder.create();
                dialog.show();

                // Set dialog window properties
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                    window.setGravity(Gravity.CENTER);
                    window.setWindowAnimations(R.style.DialogAnimation);
                }

                // Initialize views
                TextView tvVariantInfo = dialogView.findViewById(R.id.tvVariantInfo);
                TextView tvCurrentPrice = dialogView.findViewById(R.id.tvCurrentPrice);
                TextInputEditText etNewPrice = dialogView.findViewById(R.id.etNewPrice);
                TextInputLayout textInputLayoutPrice = dialogView.findViewById(R.id.textInputLayoutPrice);
                TextInputLayout textInputLayoutDiscount = dialogView.findViewById(R.id.textInputLayoutDiscount);
                TextInputEditText etDiscountValue = dialogView.findViewById(R.id.etDiscountValue);
                MaterialButtonToggleGroup toggleDiscountType = dialogView.findViewById(R.id.toggleDiscountType);
                MaterialButton btnNoDiscount = dialogView.findViewById(R.id.btnNoDiscount);
                MaterialButton btnFlatDiscount = dialogView.findViewById(R.id.btnFlatDiscount);
                MaterialButton btnPercentageDiscount = dialogView.findViewById(R.id.btnPercentageDiscount);
                LinearLayout layoutFinalPrice = dialogView.findViewById(R.id.layoutFinalPrice);
                TextView tvFinalPrice = dialogView.findViewById(R.id.tvFinalPrice);
                MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
                MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);
//                MaterialButton btnSuggestion1 = dialogView.findViewById(R.id.btnSuggestion1);
//                MaterialButton btnSuggestion2 = dialogView.findViewById(R.id.btnSuggestion2);
//                MaterialButton btnSuggestion3 = dialogView.findViewById(R.id.btnSuggestion3);
                LinearLayout layoutWarning = dialogView.findViewById(R.id.layoutWarning);
                TextView tvWarning = dialogView.findViewById(R.id.tvWarning);

                // Get variant data
                String variantMeasure = variant.optString("measure", "Variant");
                int currentPrice = variant.optInt("price", 0);
                String discountType = variant.optString("disc_type", DISCOUNT_TYPE_NONE);
                int discountValue = variant.optInt("disc_price", 0);

                // Set variant info
                tvVariantInfo.setText(variantMeasure);
                tvCurrentPrice.setText("₹" + currentPrice);
                etNewPrice.setText(String.valueOf(currentPrice));
                etNewPrice.setSelection(etNewPrice.getText().length());

                // Set discount type
                switch (discountType) {
                    case DISCOUNT_TYPE_FLAT:
                        toggleDiscountType.check(R.id.btnFlatDiscount);
                        textInputLayoutDiscount.setVisibility(View.VISIBLE);
                        break;
                    case DISCOUNT_TYPE_PERCENTAGE:
                        toggleDiscountType.check(R.id.btnPercentageDiscount);
                        textInputLayoutDiscount.setVisibility(View.VISIBLE);
                        break;
                    default:
                        toggleDiscountType.check(R.id.btnNoDiscount);
                        textInputLayoutDiscount.setVisibility(View.GONE);
                        break;
                }

                // Set discount value
                etDiscountValue.setText(String.valueOf(discountValue));

                // Calculate and show initial final price
                updateFinalPrice(etNewPrice, etDiscountValue, toggleDiscountType, tvFinalPrice, layoutFinalPrice);

                // Discount type toggle listener
                toggleDiscountType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                    if (isChecked) {
                        if (checkedId == R.id.btnNoDiscount) {
                            textInputLayoutDiscount.setVisibility(View.GONE);
                            etDiscountValue.setText("0");
                        } else {
                            textInputLayoutDiscount.setVisibility(View.VISIBLE);
                            if (etDiscountValue.getText().toString().equals("0")) {
                                etDiscountValue.setText("10");
                            }
                        }
                        updateFinalPrice(etNewPrice, etDiscountValue, toggleDiscountType, tvFinalPrice, layoutFinalPrice);
                        validatePrice(etNewPrice, currentPrice, layoutWarning, tvWarning);
                    }
                });

                // Text watchers for real-time updates
                etNewPrice.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable s) {
                        updateFinalPrice(etNewPrice, etDiscountValue, toggleDiscountType, tvFinalPrice, layoutFinalPrice);
                        validatePrice(etNewPrice, currentPrice, layoutWarning, tvWarning);
                    }
                });

                etDiscountValue.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}
                    public void afterTextChanged(Editable s) {
                        updateFinalPrice(etNewPrice, etDiscountValue, toggleDiscountType, tvFinalPrice, layoutFinalPrice);
                        validatePrice(etNewPrice, currentPrice, layoutWarning, tvWarning);
                    }
                });

                // Suggestion buttons
//                btnSuggestion1.setOnClickListener(v -> adjustPrice(etNewPrice, 10));
//                btnSuggestion2.setOnClickListener(v -> adjustPrice(etNewPrice, 20));
//                btnSuggestion3.setOnClickListener(v -> adjustPrice(etNewPrice, 50));

                // Cancel button
                btnCancel.setOnClickListener(v -> dialog.dismiss());

                // Update button
                btnUpdate.setOnClickListener(v -> {
                    if (validateInputs(etNewPrice, etDiscountValue, toggleDiscountType, textInputLayoutPrice, textInputLayoutDiscount)) {
                        updateVariantData(position, variant, etNewPrice, etDiscountValue, toggleDiscountType);
                        dialog.dismiss();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error showing edit variant dialog: " + e.getMessage());
                Toast.makeText(itemView.getContext(), "Error opening editor", Toast.LENGTH_SHORT).show();
            }
        }

        // Helper method to adjust price
//        private void adjustPrice(TextInputEditText etNewPrice, int amount) {
//            try {
//                String currentPriceStr = etNewPrice.getText().toString().trim();
//                if (!currentPriceStr.isEmpty()) {
//                    double newPrice = Double.parseDouble(currentPriceStr) + amount;
//                    etNewPrice.setText(String.valueOf((int) newPrice));
//                    etNewPrice.setSelection(etNewPrice.getText().length());
//                }
//            } catch (NumberFormatException e) {
//                etNewPrice.setText(String.valueOf(amount));
//            }
//        }

        // Helper method to update final price
        private void updateFinalPrice(TextInputEditText etNewPrice, TextInputEditText etDiscountValue,
                                      MaterialButtonToggleGroup toggleDiscountType, TextView tvFinalPrice,
                                      LinearLayout layoutFinalPrice) {
            try {
                String priceStr = etNewPrice.getText().toString().trim();
                String discountStr = etDiscountValue.getText().toString().trim();

                if (priceStr.isEmpty()) {
                    layoutFinalPrice.setVisibility(View.GONE);
                    return;
                }

                double price = Double.parseDouble(priceStr);
                double finalPrice = price;

                if (!discountStr.isEmpty() && toggleDiscountType.getCheckedButtonId() != R.id.btnNoDiscount) {
                    double discount = Double.parseDouble(discountStr);

                    if (toggleDiscountType.getCheckedButtonId() == R.id.btnFlatDiscount) {
                        finalPrice = Math.max(0, price - discount);
                    } else if (toggleDiscountType.getCheckedButtonId() == R.id.btnPercentageDiscount) {
                        double discountAmount = (price * discount) / 100;
                        finalPrice = Math.max(0, price - discountAmount);
                    }
                }

                tvFinalPrice.setText("₹" + finalPrice);
                layoutFinalPrice.setVisibility(View.VISIBLE);

            } catch (NumberFormatException e) {
                layoutFinalPrice.setVisibility(View.GONE);
            }
        }

        // Helper method to validate inputs
        private boolean validateInputs(TextInputEditText etNewPrice, TextInputEditText etDiscountValue,
                                       MaterialButtonToggleGroup toggleDiscountType, TextInputLayout textInputLayoutPrice,
                                       TextInputLayout textInputLayoutDiscount) {
            String priceStr = etNewPrice.getText().toString().trim();
            String discountStr = etDiscountValue.getText().toString().trim();

            if (priceStr.isEmpty()) {
                textInputLayoutPrice.setError("Please enter a price");
                return false;
            }

            try {
                double price = Double.parseDouble(priceStr);
                if (price <= 0) {
                    textInputLayoutPrice.setError("Price must be greater than 0");
                    return false;
                }

                if (price > 100000) {
                    textInputLayoutPrice.setError("Price seems too high");
                    return false;
                }

                if (toggleDiscountType.getCheckedButtonId() != R.id.btnNoDiscount && !discountStr.isEmpty()) {
                    double discount = Double.parseDouble(discountStr);
                    if (discount < 0) {
                        textInputLayoutDiscount.setError("Discount cannot be negative");
                        return false;
                    }

                    if (toggleDiscountType.getCheckedButtonId() == R.id.btnFlatDiscount && discount >= price) {
                        textInputLayoutDiscount.setError("Discount cannot exceed price");
                        return false;
                    }

                    if (toggleDiscountType.getCheckedButtonId() == R.id.btnPercentageDiscount && discount > 100) {
                        textInputLayoutDiscount.setError("Discount cannot exceed 100%");
                        return false;
                    }
                }

                textInputLayoutPrice.setError(null);
                textInputLayoutDiscount.setError(null);
                return true;

            } catch (NumberFormatException e) {
                textInputLayoutPrice.setError("Please enter a valid price");
                return false;
            }
        }

        // Helper method to validate price and show warnings
        private void validatePrice(TextInputEditText etNewPrice, int currentPrice,
                                   LinearLayout layoutWarning, TextView tvWarning) {
            try {
                String priceStr = etNewPrice.getText().toString().trim();
                if (priceStr.isEmpty()) {
                    layoutWarning.setVisibility(View.GONE);
                    return;
                }

                double newPrice = Double.parseDouble(priceStr);
                double currentPriceVal = currentPrice;

                if (newPrice < currentPriceVal * 0.3) {
                    layoutWarning.setVisibility(View.VISIBLE);
                    tvWarning.setText("Price reduced significantly (over 70%)");
                } else if (newPrice > currentPriceVal * 3) {
                    layoutWarning.setVisibility(View.VISIBLE);
                    tvWarning.setText("Price increased significantly (over 200%)");
                } else if (newPrice < 1) {
                    layoutWarning.setVisibility(View.VISIBLE);
                    tvWarning.setText("Price seems too low");
                } else {
                    layoutWarning.setVisibility(View.GONE);
                }

            } catch (NumberFormatException e) {
                layoutWarning.setVisibility(View.GONE);
            }
        }

        // Helper method to update variant data
        private void updateVariantData(int pos, JSONObject variantData, TextInputEditText etNewPrice,
                                       TextInputEditText etDiscountValue, MaterialButtonToggleGroup toggleDiscountType) {
            try {
                String newPriceStr = etNewPrice.getText().toString().trim();
                String discountValueStr = etDiscountValue.getText().toString().trim();

                int newPrice = Integer.parseInt(newPriceStr);
                int newDiscountValue = 0;
                String newDiscountType = DISCOUNT_TYPE_NONE;

                int checkedId = toggleDiscountType.getCheckedButtonId();
                if (checkedId == R.id.btnFlatDiscount) {
                    newDiscountType = DISCOUNT_TYPE_FLAT;
                    newDiscountValue = Integer.parseInt(discountValueStr);
                } else if (checkedId == R.id.btnPercentageDiscount) {
                    newDiscountType = DISCOUNT_TYPE_PERCENTAGE;
                    newDiscountValue = Integer.parseInt(discountValueStr);
                }

                // Update the variant object
                variantData.put("price", newPrice);
                variantData.put("disc_type", newDiscountType);
                variantData.put("disc_price", newDiscountValue);

                // Notify listeners
                if (listener != null) {
                    // Update price
                    listener.onVariantPriceChanged(pos, newPriceStr);

                    // Update discount type and value
                    listener.onVariantDiscountTypeChanged(pos, newDiscountType, String.valueOf(newDiscountValue));

                    // Also trigger the update clicked listener to save all changes
                    listener.onVariantUpdateClicked(pos, variantData);
                }

                // Show success message
                Toast.makeText(itemView.getContext(), "Variant updated successfully", Toast.LENGTH_SHORT).show();

                // Update the current view
                bindData(variantData, pos);

            } catch (Exception e) {
                Log.e(TAG, "Error updating variant data: " + e.getMessage());
                Toast.makeText(itemView.getContext(), "Failed to update variant", Toast.LENGTH_SHORT).show();
            }
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