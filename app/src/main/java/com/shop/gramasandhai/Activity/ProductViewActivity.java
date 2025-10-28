package com.shop.gramasandhai.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shop.gramasandhai.Adapter.VariantAdapter;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductViewActivity extends AppCompatActivity implements VariantAdapter.OnVariantChangeListener {

    // Views
    private TextView tvProductName, tvProductId, tvProductPrice, tvProductDescription, tvStatusBadge;
    private TextView tvVariantsCount, tvActiveVariants, tvTotalStock, tvVariantsCountHeader;
    private TextView tvRating, tvErrorTitle, tvErrorMessage;
    private SwitchCompat switchProductStatus;
    private RecyclerView rvVariants;
    private RatingBar ratingBar;
    private ImageView ivProductImage;
    private ExtendedFloatingActionButton fabAddVariant;
    private MaterialButton btnEditPrice;

    private ProgressBar progressBar;

    // Layouts
    private LinearLayout contentLayout, errorLayout;
    private MaterialButton btnRetry;

    private JSONObject productData;
    private List<JSONObject> variantsList;
    private VariantAdapter variantAdapter;

    private String productId;
    private String productName;
    private String productDescription;
    private boolean productStatusChanged = false;
    private boolean hasChanges = false;

    // Add this flag to prevent circular updates
    private boolean isUpdatingProductStatus = false;

    private OkHttpClient client;

    private static final String TAG = "ProductViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_view);

        // Only get product ID from intent
        productId = getIntent().getStringExtra("prod_id");
        if (productId == null) {
            Toast.makeText(this, "Product ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();
        loadProductDataFromAPI();
    }

    private void initializeViews() {
        // Main views
        tvProductName = findViewById(R.id.tvProductName);
        tvProductId = findViewById(R.id.tvProductId);
        tvProductPrice = findViewById(R.id.tvProductPrice);
        tvProductDescription = findViewById(R.id.tvProductDescription);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        switchProductStatus = findViewById(R.id.switchProductStatus);
        rvVariants = findViewById(R.id.rvVariants);
        ivProductImage = findViewById(R.id.ivProductImage);
        ratingBar = findViewById(R.id.ratingBar);
        tvRating = findViewById(R.id.tvRating);
        progressBar = findViewById(R.id.progressBar);
        fabAddVariant = findViewById(R.id.fabAddVariant);
        btnEditPrice = findViewById(R.id.btnEditPrice);

        // Stats views
        tvVariantsCount = findViewById(R.id.tvVariantsCount);
        tvActiveVariants = findViewById(R.id.tvActiveVariants);
        tvTotalStock = findViewById(R.id.tvTotalStock);
        tvVariantsCountHeader = findViewById(R.id.tvVariantsCountHeader);

        // Layouts
        contentLayout = findViewById(R.id.contentLayout);
        errorLayout = findViewById(R.id.errorLayout);

        // Error layout views
        tvErrorTitle = findViewById(R.id.tvErrorTitle);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        btnRetry = findViewById(R.id.btnRetry);

        variantsList = new ArrayList<>();
        client = new OkHttpClient();
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        variantAdapter = new VariantAdapter(variantsList, this);
        rvVariants.setLayoutManager(new LinearLayoutManager(this));
        rvVariants.setAdapter(variantAdapter);
    }

    private void setupListeners() {
        // Product status switch
        switchProductStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isUpdatingProductStatus) {
                return; // Prevent circular updates
            }

            productStatusChanged = true;
            hasChanges = true;

            // Update status badge
            updateStatusBadge(isChecked);

            // Call API to update product status
            updateProductStatusInAPI(productId, isChecked);

            // Update all variants status
            updateAllVariantsStatus(isChecked);
        });

        // Retry button
        btnRetry.setOnClickListener(v -> loadProductDataFromAPI());

        // Add Variant FAB
        fabAddVariant.setOnClickListener(v -> showAddVariantDialog());

        // Edit Price Button
        btnEditPrice.setOnClickListener(v -> showEditPriceDialog());
    }

    private void showEditPriceDialog() {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_price, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog window properties
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);

            // Add animation
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Initialize views
        TextView tvCurrentPrice = dialogView.findViewById(R.id.tvCurrentPrice);
        TextInputEditText etNewPrice = dialogView.findViewById(R.id.etNewPrice);
        TextInputLayout textInputLayoutPrice = dialogView.findViewById(R.id.textInputLayoutPrice);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        MaterialButton btnSuggestion1 = dialogView.findViewById(R.id.btnSuggestion1);
        MaterialButton btnSuggestion2 = dialogView.findViewById(R.id.btnSuggestion2);
        MaterialButton btnSuggestion3 = dialogView.findViewById(R.id.btnSuggestion3);
        LinearLayout layoutWarning = dialogView.findViewById(R.id.layoutWarning);
        TextView tvWarning = dialogView.findViewById(R.id.tvWarning);

        // Set current price
        String currentPrice = tvProductPrice.getText().toString().replace("₹", "").trim();
        tvCurrentPrice.setText("₹" + currentPrice);
        etNewPrice.setText(currentPrice);
        etNewPrice.setSelection(etNewPrice.getText().length());

        // Price validation and warning
        etNewPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newPriceStr = s.toString().trim();
                if (!newPriceStr.isEmpty()) {
                    try {
                        double currentPriceVal = Double.parseDouble(currentPrice);
                        double newPriceVal = Double.parseDouble(newPriceStr);

                        if (newPriceVal < currentPriceVal * 0.5) {
                            // Price reduced by more than 50%
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price reduced significantly (over 50%)");
                            btnUpdate.setBackgroundColor(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color));
                        } else if (newPriceVal > currentPriceVal * 2) {
                            // Price increased by more than 100%
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price increased significantly (over 100%)");
                            btnUpdate.setBackgroundColor(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color));
                        } else if (newPriceVal < 1) {
                            // Price too low
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price seems too low");
                            btnUpdate.setBackgroundColor(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color));
                        } else {
                            layoutWarning.setVisibility(View.GONE);
                            btnUpdate.setBackgroundColor(ContextCompat.getColor(ProductViewActivity.this, R.color.colorPrimary));
                        }
                    } catch (NumberFormatException e) {
                        layoutWarning.setVisibility(View.GONE);
                    }
                } else {
                    layoutWarning.setVisibility(View.GONE);
                }
            }
        });

        // Suggestion buttons
        btnSuggestion1.setOnClickListener(v -> {
            try {
                double current = Double.parseDouble(currentPrice);
                double newPrice = current + 10;
                etNewPrice.setText(String.valueOf((int) newPrice));
                etNewPrice.setSelection(etNewPrice.getText().length());
            } catch (NumberFormatException e) {
                etNewPrice.setText("10");
            }
        });

        btnSuggestion2.setOnClickListener(v -> {
            try {
                double current = Double.parseDouble(currentPrice);
                double newPrice = current + 20;
                etNewPrice.setText(String.valueOf((int) newPrice));
                etNewPrice.setSelection(etNewPrice.getText().length());
            } catch (NumberFormatException e) {
                etNewPrice.setText("20");
            }
        });

        btnSuggestion3.setOnClickListener(v -> {
            try {
                double current = Double.parseDouble(currentPrice);
                double newPrice = current + 50;
                etNewPrice.setText(String.valueOf((int) newPrice));
                etNewPrice.setSelection(etNewPrice.getText().length());
            } catch (NumberFormatException e) {
                etNewPrice.setText("50");
            }
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Update button
        btnUpdate.setOnClickListener(v -> {
            String newPrice = etNewPrice.getText().toString().trim();
            if (!newPrice.isEmpty()) {
                try {
                    double priceValue = Double.parseDouble(newPrice);
                    if (priceValue <= 0) {
                        textInputLayoutPrice.setError("Price must be greater than 0");
                        return;
                    }

                    if (priceValue > 1000000) {
                        textInputLayoutPrice.setError("Price seems too high");
                        return;
                    }

                    textInputLayoutPrice.setError(null);
                    updateProductPrice(newPrice);
                    dialog.dismiss();

                } catch (NumberFormatException e) {
                    textInputLayoutPrice.setError("Please enter a valid price");
                }
            } else {
                textInputLayoutPrice.setError("Please enter a price");
            }
        });

        // Clear error when user starts typing
        etNewPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayoutPrice.setError(null);
            }
        });
    }

    private void updateProductPrice(String newPrice) {
        showLoadingState();

        // Create request body
        RequestBody requestBody = new FormBody.Builder()
                .add("prod_price", newPrice)
                .build();

        // Build the request
        String url = Attributes.Main_Url + "store/productUpdate/" + productId;
        Log.d(TAG, "Updating product price - URL: " + url + ", Price: " + newPrice);

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    hideLoadingState();
                    Log.e(TAG, "Product price update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this,
                            "Failed to update price: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Price update response: " + responseBodyString);

                runOnUiThread(() -> {
                    hideLoadingState();
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful()) {
                            // Check for success in different possible response formats
                            boolean isSuccess = false;
                            String message = "Price updated successfully";

                            if (responseObject.has("status")) {
                                String status = responseObject.optString("status", "");
                                isSuccess = "success".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
                            } else if (responseObject.has("success")) {
                                isSuccess = responseObject.getBoolean("success");
                            } else {
                                // If no status field, consider 200 response as success
                                isSuccess = response.isSuccessful();
                            }

                            if (responseObject.has("message")) {
                                message = responseObject.getString("message");
                            }

                            if (isSuccess) {
                                // Show success message
                                Toast.makeText(ProductViewActivity.this, message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Product price updated successfully");

                                // REFRESH THE PRODUCT DATA TO SHOW UPDATED PRICE
                                loadProductDataFromAPI();

                            } else {
                                String errorMessage = responseObject.optString("error", "Failed to update price");
                                Log.e(TAG, "Price update failed: " + errorMessage);
                                Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMessage = "Server error: " + response.code();
                            Log.e(TAG, "Price update failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing price update response: " + e.getMessage());
                        // Even if parsing fails, refresh data if response was successful
                        if (response.isSuccessful()) {
                            Toast.makeText(ProductViewActivity.this, "Price updated successfully", Toast.LENGTH_SHORT).show();
                            // REFRESH THE PRODUCT DATA TO SHOW UPDATED PRICE
                            loadProductDataFromAPI();
                        } else {
                            Toast.makeText(ProductViewActivity.this, "Failed to update price", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void updateStatusBadge(boolean isActive) {
        if (isActive) {
            tvStatusBadge.setText("Active");
            tvStatusBadge.setBackgroundResource(R.drawable.badge_active);
        } else {
            tvStatusBadge.setText("Inactive");
            tvStatusBadge.setBackgroundResource(R.drawable.badge_inactive);
        }
    }

    private void loadProductDataFromAPI() {
        showLoadingState();

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/product/" + productId)
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideLoadingState();
                    showErrorState("Network Error", "Please check your internet connection and try again.");
                    Log.e("API_ERROR", "Request failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("PRODUCT_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    hideLoadingState();
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && responseObject.getBoolean("status")) {
                            // Parse the successful response
                            JSONObject data = responseObject.getJSONObject("data");
                            JSONObject product = data.getJSONObject("product");
                            JSONArray variants = data.getJSONArray("variants");

                            // Process the product data
                            processProductData(product, variants);
                            showContentState();

                        } else {
                            String errorMessage = responseObject.optString("message", "Failed to load product data");
                            showErrorState("Load Failed", errorMessage);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        showErrorState("Parse Error", "Error parsing product data");
                    }
                });
            }
        });
    }

    private void showLoadingState() {
        progressBar.setVisibility(View.VISIBLE);
        fabAddVariant.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.GONE);
    }

    private void showContentState() {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
        fabAddVariant.setVisibility(View.VISIBLE);
        errorLayout.setVisibility(View.GONE);
    }

    private void showErrorState(String title, String message) {
        progressBar.setVisibility(View.GONE);
        contentLayout.setVisibility(View.GONE);
        errorLayout.setVisibility(View.VISIBLE);

        tvErrorTitle.setText(title);
        tvErrorMessage.setText(message);
    }

    private void hideLoadingState() {
        progressBar.setVisibility(View.GONE);
    }

    private void processProductData(JSONObject product, JSONArray variants) {
        try {
            // Store product data
            productData = product;

            // Display product details
            displayProductDetails(product);

            // Load variants data
            loadVariantsData(variants);

            // Update stats
            updateProductStats();

        } catch (Exception e) {
            Log.e("PROCESS_ERROR", "Error processing product data: " + e.getMessage());
            Toast.makeText(this, "Error displaying product data", Toast.LENGTH_SHORT).show();
        }
    }

    // Add this method to ProductViewActivity
    private void showAddVariantDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Variant");
        builder.setMessage("Do you want to add a new variant to this product?");
        builder.setPositiveButton("Add Variant", (dialog, which) -> {
            Intent intent = new Intent(ProductViewActivity.this, AddVariantActivity.class);
            intent.putExtra("product_id", productId);
            startActivityForResult(intent, 1001);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // Handle the result when returning from AddVariantActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Refresh the product data to show the new variant
            loadProductDataFromAPI();
            Toast.makeText(this, "Variant added successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    // Add this method to handle variant deletion
    @Override
    public void onVariantDeleteClicked(int position, String variantId) {
        showDeleteConfirmationDialog(position, variantId);
    }

    private void showDeleteConfirmationDialog(int position, String variantId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Variant")
                .setMessage("Are you sure you want to delete this variant? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteVariantFromAPI(position, variantId);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteVariantFromAPI(int position, String variantId) {
        // Show a progress dialog for better UX
        AlertDialog progressDialog = new AlertDialog.Builder(this)
                .setMessage("Deleting variant...")
                .setCancelable(false)
                .create();
        progressDialog.show();

        String url = Attributes.Main_Url + "store/variant/delete/" + variantId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Log.e("API_ERROR", "Variant deletion failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Failed to delete variant: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("DELETE_RESPONSE", "Response: " + responseBodyString);

                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful()) {
                            // Check for success in different possible response formats
                            boolean isSuccess = false;
                            if (responseObject.has("status")) {
                                isSuccess = responseObject.getBoolean("status");
                            } else if (responseObject.has("success")) {
                                isSuccess = responseObject.getBoolean("success");
                            } else {
                                // If no status field, consider 200 response as success
                                isSuccess = response.isSuccessful();
                            }

                            if (isSuccess) {
                                Toast.makeText(ProductViewActivity.this, "Variant deleted successfully", Toast.LENGTH_SHORT).show();

                                // Refresh the entire product data
                                loadProductDataFromAPI();

                            } else {
                                String errorMessage = responseObject.optString("message", "Failed to delete variant");
                                Log.e("API_ERROR", "Variant deletion failed: " + errorMessage);
                                Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMessage = "Server error: " + response.code();
                            Log.e("API_ERROR", "Variant deletion failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Delete response parsing failed: " + e.getMessage());
                        Toast.makeText(ProductViewActivity.this, "Variant deleted successfully", Toast.LENGTH_SHORT).show();

                        // Even if parsing fails, refresh the data
                        loadProductDataFromAPI();
                    }
                });
            }
        });
    }

    private void displayProductDetails(JSONObject product) {
        try {
            // Display product name
            if (product.has("prod_name")) {
                productName = product.getString("prod_name");
                tvProductName.setText(productName);

                // Update toolbar title
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(productName);
                }
            }

            // Display product price
            if (product.has("prod_price")) {
                String price = product.getString("prod_price");
                tvProductPrice.setText("₹" + price);
            }

            // Display product ID
            if (product.has("id")) {
                productId = product.getString("id");
                tvProductId.setText("ID: " + productId);
            }

            // Display product description
            if (product.has("description")) {
                productDescription = product.getString("description");
                if (productDescription != null && !productDescription.isEmpty() && !productDescription.equals("null")) {
                    tvProductDescription.setText(productDescription);
                    tvProductDescription.setVisibility(TextView.VISIBLE);
                } else {
                    tvProductDescription.setVisibility(TextView.GONE);
                }
            } else {
                tvProductDescription.setVisibility(TextView.GONE);
            }

            // Set product status
            if (product.has("status")) {
                boolean isActive = "1".equals(product.getString("status"));
                // Remove listener temporarily to avoid triggering
                switchProductStatus.setOnCheckedChangeListener(null);
                switchProductStatus.setChecked(isActive);
                setupListeners(); // Re-setup listeners
                updateStatusBadge(isActive);
            }

            // Load product image
            if (product.has("main_image")) {
                String imageUrl = Attributes.Root_Url + "uploads/images/" + product.getString("main_image");
                if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_product_placeholder)
                            .error(R.drawable.ic_product_placeholder)
                            .into(ivProductImage);
                }
            }

            // Set ratings
            if (product.has("ratings")) {
                float rating = (float) product.getDouble("ratings");
                ratingBar.setRating(rating);

                String ratingsText = String.format("%.1f", rating);
                if (product.has("no_of_ratings")) {
                    ratingsText += " (" + product.getString("no_of_ratings") + " reviews)";
                }
                tvRating.setText(ratingsText);
            }

        } catch (JSONException e) {
            Log.e("DISPLAY_ERROR", "Error displaying product details: " + e.getMessage());
        }
    }

    private void updateProductStats() {
        try {
            int totalVariants = variantsList.size();
            int activeVariants = 0;
            int totalStock = 0;
            Log.d("DEBUG_VARIANTS", "Variants list size: " + totalVariants);
            Log.d("DEBUG_VARIANTS", "Variants data: " + variantsList.toString());

            for (JSONObject variant : variantsList) {
                try {
                    // Safely get status - handle both string "1"/"0" and boolean true/false
                    String status = variant.optString("status", "0");
                    if ("1".equals(status) || "true".equalsIgnoreCase(status)) {
                        activeVariants++;
                    }

                    // Safely get stock - handle string values like "50 box"
                    if (variant.has("stock")) {
                        String stockValue = variant.optString("stock", "0");
                        // Extract numeric part from stock string (e.g., "50 box" -> 50)
                        try {
                            String numericStock = stockValue.replaceAll("[^0-9]", "").trim();
                            if (!numericStock.isEmpty()) {
                                totalStock += Integer.parseInt(numericStock);
                            }
                        } catch (NumberFormatException e) {
                            Log.e("STOCK_PARSE_ERROR", "Failed to parse stock: " + stockValue);
                        }
                    }
                } catch (Exception e) {
                    Log.e("VARIANT_STATS_ERROR", "Error processing variant: " + e.getMessage());
                }
            }

            // Update UI on main thread
            int finalActiveVariants = activeVariants;
            int finalTotalStock = totalStock;
            runOnUiThread(() -> {
                tvVariantsCount.setText(String.valueOf(totalVariants));
                tvActiveVariants.setText(String.valueOf(finalActiveVariants));
                tvTotalStock.setText(String.valueOf(finalTotalStock));

                String variantCount = String.valueOf(totalVariants) + " variant" + (totalVariants != 1 ? "s" : "");
                Log.d("DEBUG_UI", "Setting variant count header: " + variantCount);
                tvVariantsCountHeader.setText(variantCount);
            });

        } catch (Exception e) {
            Log.e("STATS_ERROR", "Error updating product stats: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadVariantsData(JSONArray variantsArray) {
        try {
            variantsList.clear();

            for (int i = 0; i < variantsArray.length(); i++) {
                JSONObject variant = variantsArray.getJSONObject(i);

                // Verify this variant belongs to the current product
                if (variant.has("prod_id") && variant.getString("prod_id").equals(productId)) {
                    variantsList.add(variant);
                }
            }

            Log.d("DEBUG_LOAD", "Loaded " + variantsList.size() + " variants");
            variantAdapter.notifyDataSetChanged();
            updateProductStats(); // Make sure this is called

        } catch (JSONException e) {
            Log.e("VARIANT_ERROR", "Error loading variants: " + e.getMessage());
            Toast.makeText(this, "Error loading variants", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAllVariantsStatus(boolean isChecked) {
        isUpdatingProductStatus = true;

        for (JSONObject variant : variantsList) {
            try {
                String variantId = variant.getString("id");
                String productId = variant.getString("prod_id");
                String statusValue = isChecked ? "1" : "0";
                variant.put("status", statusValue);

                // Call API for each variant
                updateVariantStatusInAPI(productId, variantId, statusValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        variantAdapter.notifyDataSetChanged();
        updateProductStats();

        // Reset the flag after a short delay to ensure all updates are processed
        new android.os.Handler().postDelayed(() -> {
            isUpdatingProductStatus = false;
        }, 500);
    }

    private void saveAllChanges() {
        // Implement bulk save functionality here
        Toast.makeText(this, "All changes saved successfully!", Toast.LENGTH_SHORT).show();
        hasChanges = false;
    }

    // Implement VariantAdapter.OnVariantChangeListener methods
    @Override
    public void onVariantStatusChanged(int position, boolean isActive, String statusValue) {
        try {
            JSONObject variant = variantsList.get(position);
            String variantId = variant.getString("id");
            String productId = variant.getString("prod_id");

            variant.put("status", statusValue);
            updateVariantStatusInAPI(productId, variantId, statusValue);

            hasChanges = true;

            // Update product status based on variant status changes
            updateProductStatusBasedOnVariants();

            updateProductStats();

        } catch (Exception e) {
            Log.e("VARIANT_STATUS_ERROR", "Error updating variant status: " + e.getMessage());
            Toast.makeText(this, "Failed to update variant status", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVariantUpdateClicked(int position, JSONObject updatedVariant) {
        try {
            String variantId = updatedVariant.getString("id");
            String price = String.valueOf(updatedVariant.getInt("price"));
            String discType = updatedVariant.getString("disc_type");
            String discPrice = String.valueOf(updatedVariant.getInt("disc_price"));

            updateVariantInAPI(variantId, price, discType, discPrice);
            hasChanges = true;

        } catch (Exception e) {
            Log.e("VARIANT_UPDATE_ERROR", "Error updating variant: " + e.getMessage());
            Toast.makeText(this, "Failed to update variant", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVariantPriceChanged(int position, String newPrice) {
        try {
            JSONObject variant = variantsList.get(position);
            String variantId = variant.getString("id");
            variant.put("price", Integer.parseInt(newPrice));
            variantPriceUpdate(variantId, newPrice);
            hasChanges = true;

        } catch (Exception e) {
            Log.e("PRICE_UPDATE_ERROR", "Error updating variant price: " + e.getMessage());
            Toast.makeText(this, "Failed to update variant price", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVariantDiscPriceChanged(int position, String newDiscPrice) {
        try {
            JSONObject variant = variantsList.get(position);
            String variantId = variant.getString("id");
            variant.put("disc_price", Integer.parseInt(newDiscPrice));
            variantDiscountUpdate(variantId, newDiscPrice);
            hasChanges = true;

        } catch (Exception e) {
            Log.e("DISC_PRICE_ERROR", "Error updating variant discounted price: " + e.getMessage());
            Toast.makeText(this, "Failed to update variant discounted price", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVariantDiscountTypeChanged(int position, String discountType, String discountValue) {
        try {
            JSONObject variant = variantsList.get(position);
            String variantId = variant.getString("id");
            variant.put("disc_type", discountType);
            variant.put("disc_value", discountValue);
            variantDiscountTypeUpdate(variantId, discountType, discountValue);
            hasChanges = true;

        } catch (Exception e) {
            Log.e("DISC_TYPE_ERROR", "Error updating discount type: " + e.getMessage());
            Toast.makeText(this, "Failed to update discount type", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW METHOD: Update product status based on variant statuses
    private void updateProductStatusBasedOnVariants() {
        if (isUpdatingProductStatus) {
            return; // Don't update if we're in the middle of a product status update
        }

        boolean allVariantsInactive = true;

        for (JSONObject variant : variantsList) {
            try {
                String status = variant.optString("status", "0");
                if ("1".equals(status)) {
                    allVariantsInactive = false;
                    break;
                }
            } catch (Exception e) {
                Log.e("STATUS_CHECK", "Error checking variant status: " + e.getMessage());
            }
        }

        // If all variants are inactive, turn off product status
        // If at least one variant is active, turn on product status
        boolean shouldProductBeActive = !allVariantsInactive;

        // Only update if there's a change needed
        if (switchProductStatus.isChecked() != shouldProductBeActive) {
            isUpdatingProductStatus = true;
            switchProductStatus.setChecked(shouldProductBeActive);
            updateStatusBadge(shouldProductBeActive);

            // Also update the product status in API
            updateProductStatusInAPI(productId, shouldProductBeActive);

            new android.os.Handler().postDelayed(() -> {
                isUpdatingProductStatus = false;
            }, 500);
        }
    }

    // Helper methods for discount type and status conversion
    private String getDiscountTypeValue(String displayValue) {
        switch (displayValue) {
            case "No Discount": return "0";
            case "Flat": return "1";
            case "Percentage": return "2";
            default: return "0";
        }
    }

    private String getStatusValue(String displayValue) {
        switch (displayValue) {
            case "Available": return "1";
            case "Unavailable": return "0";
            case "Out of Stock": return "2";
            case "Discontinued": return "3";
            default: return "1";
        }
    }

    // API Methods
    private void updateVariantInAPI(String variantId, String price, String discType, String discPrice) {
        RequestBody requestBody = new FormBody.Builder()
                .add("price", price)
                .add("disc_type", discType)
                .add("disc_price", discPrice)
                .build();

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/productUpdate/" + variantId)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Variant update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Failed to update variant", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(ProductViewActivity.this, "Variant updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e("API_ERROR", "Variant update failed: " + response.code());
                        Toast.makeText(ProductViewActivity.this, "Failed to update variant", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void variantPriceUpdate(String variantId, String price) {
        RequestBody requestBody = new FormBody.Builder()
                .add("price", price)
                .build();

        Log.d("PRICE_UPDATE", "Updating price for variant: " + variantId + " to: " + price);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/productUpdate/" + variantId)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Price update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Failed to update price: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("PRICE_UPDATE_RESPONSE", "Response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            Log.d("API_SUCCESS", "Price updated successfully");
                            Toast.makeText(ProductViewActivity.this, "Price updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = responseObject.optString("error", "Failed to update price");
                            Log.e("API_ERROR", "Price update failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Price update parsing failed: " + e.getMessage());
                        Toast.makeText(ProductViewActivity.this, "Failed to update price", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void variantDiscountUpdate(String variantId, String discPrice) {
        RequestBody requestBody = new FormBody.Builder()
                .add("disc_price", discPrice)
                .build();

        Log.d("DISCOUNT_UPDATE", "Updating discount price for variant: " + variantId + " to: " + discPrice);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/productUpdate/" + variantId)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Discount price update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Failed to update discount price: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("DISCOUNT_UPDATE_RESPONSE", "Response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            Log.d("API_SUCCESS", "Discount price updated successfully");
                            Toast.makeText(ProductViewActivity.this, "Discount price updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = responseObject.optString("error", "Failed to update discount price");
                            Log.e("API_ERROR", "Discount price update failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Discount price update parsing failed: " + e.getMessage());
                        Toast.makeText(ProductViewActivity.this, "Failed to update discount price", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void variantDiscountTypeUpdate(String variantId, String discountType, String discountValue) {
        RequestBody requestBody = new FormBody.Builder()
                .add("disc_type", discountType)
                .add("disc_value", discountValue)
                .build();

        Log.d("DISCOUNT_TYPE_UPDATE", "Updating discount type for variant: " + variantId +
                " Type: " + discountType + " Value: " + discountValue);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/productUpdate/" + variantId)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Discount type update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Failed to update discount type: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("DISCOUNT_TYPE_UPDATE_RESPONSE", "Response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            Log.d("API_SUCCESS", "Discount type updated successfully");
                            Toast.makeText(ProductViewActivity.this, "Discount type updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            String errorMessage = responseObject.optString("error", "Failed to update discount type");
                            Log.e("API_ERROR", "Discount type update failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Discount type update parsing failed: " + e.getMessage());
                        Toast.makeText(ProductViewActivity.this, "Failed to update discount type", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // API Methods for status updates
    private void updateVariantStatusInAPI(String productId, String variantId, String status) {
        String url = Attributes.Main_Url + "shop/" + productId + "/product_variant/hide/" + variantId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Failed to update variant status: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Log.d("API_SUCCESS", "Variant status updated successfully");
                    } else {
                        Log.e("API_ERROR", "Failed to update variant status: " + response.code());
                        Toast.makeText(ProductViewActivity.this, "Failed to update variant status", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateProductStatusInAPI(String productId, boolean isChecked) {
        String url = Attributes.Main_Url + "shop/" + shopId() + "/product/hide/" + productId;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    Log.e("API_ERROR", "Failed to update product status: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this, "Network error", Toast.LENGTH_SHORT).show();

                    // Revert the switch state if API call fails
                    switchProductStatus.setChecked(!isChecked);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        try {
                            String responseBody = response.body().string();
                            Log.d("API_SUCCESS", "Product status updated successfully: " + responseBody);

                            String statusMessage = isChecked ? "Product activated successfully" : "Product deactivated successfully";
                            Toast.makeText(ProductViewActivity.this, statusMessage, Toast.LENGTH_SHORT).show();

                        } catch (IOException e) {
                            Log.e("API_ERROR", "Error reading response: " + e.getMessage());
                        }
                    } else {
                        Log.e("API_ERROR", "Failed to update product status: " + response.code());
                        Toast.makeText(ProductViewActivity.this, "Failed to update product status", Toast.LENGTH_SHORT).show();

                        // Revert the switch state if API call fails
                        switchProductStatus.setChecked(!isChecked);
                    }
                });
            }
        });
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }

    // Method to get all updated variants
    public List<JSONObject> getUpdatedVariants() {
        return variantsList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client != null) {
            client.dispatcher().cancelAll();
        }
    }
}