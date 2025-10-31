package com.shop.gramasandhai.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shop.gramasandhai.Adapter.VariantAdapter;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductViewActivity extends AppCompatActivity implements VariantAdapter.OnVariantChangeListener {

    // Views
    private TextView tvProductName, tvProductId, tvProductPrice, tvProductDescription, tvStatusBadge, tvDiscountedPriceMain, tvProductDiscount;
    private TextView tvVariantsCount, tvActiveVariants, tvTotalStock, tvVariantsCountHeader;
    private TextView tvRating, tvErrorTitle, tvErrorMessage;
    private SwitchCompat switchProductStatus;
    private RecyclerView rvVariants;
    private RatingBar ratingBar;
    private ImageView ivProductImage;
    private ExtendedFloatingActionButton fabAddVariant;
    private MaterialButton btnEditPrice;
    private LinearLayout layoutProductDiscount;

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

    // Image handling variables
    private Uri selectedImageUri = null;
    private File selectedImageFile = null;
    private static final int PICK_IMAGE_REQUEST = 1002;
    private static final int PERMISSION_REQUEST_CODE = 1003;

    // Dialog views cache
    private AlertDialog currentEditDialog;
    private ImageView dialogProductImage;

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
        tvProductDiscount = findViewById(R.id.tvProductDiscount);
        tvDiscountedPriceMain = findViewById(R.id.tvDiscountedPriceMain);
        layoutProductDiscount = findViewById(R.id.layoutProductDiscount);

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
        btnEditPrice.setOnClickListener(v -> showEditProductDialog());
    }

    private void showEditProductDialog() {
        // Create custom dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_price, null);
        builder.setView(dialogView);

        currentEditDialog = builder.create();
        currentEditDialog.show();

        // Set dialog window properties
        Window window = currentEditDialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setWindowAnimations(R.style.DialogAnimation);
        }

        // Initialize views
        dialogProductImage = dialogView.findViewById(R.id.ivProductImage);
        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputLayout textInputLayoutName = dialogView.findViewById(R.id.textInputLayoutName);
        TextView tvCurrentPrice = dialogView.findViewById(R.id.tvCurrentPrice);
        TextInputEditText etNewPrice = dialogView.findViewById(R.id.etNewPrice);
        TextInputLayout textInputLayoutPrice = dialogView.findViewById(R.id.textInputLayoutPrice);

        // NEW: Toggle buttons for discount type
        MaterialButton btnNoDiscount = dialogView.findViewById(R.id.btnNoDiscount);
        MaterialButton btnFlatDiscount = dialogView.findViewById(R.id.btnFlatDiscount);
        MaterialButton btnPercentageDiscount = dialogView.findViewById(R.id.btnPercentageDiscount);

        TextInputEditText etDiscountValue = dialogView.findViewById(R.id.etDiscountValue);
        TextInputLayout textInputLayoutDiscountValue = dialogView.findViewById(R.id.textInputLayoutDiscountValue);
        LinearLayout layoutDiscountedPrice = dialogView.findViewById(R.id.layoutDiscountedPrice);
        TextView tvDiscountedPrice = dialogView.findViewById(R.id.tvDiscountedPrice);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);
        LinearLayout layoutWarning = dialogView.findViewById(R.id.layoutWarning);
        TextView tvWarning = dialogView.findViewById(R.id.tvWarning);
        FloatingActionButton fabEditImage = dialogView.findViewById(R.id.fabEditImage);

        // Variables to track selected discount type
        final String[] selectedDiscountType = {"0"}; // Default: No Discount

        // Set current data
        try {
            if (productData != null) {
                // Set product name
                if (productData.has("prod_name")) {
                    String productName = productData.getString("prod_name");
                    etProductName.setText(productName);
                    etProductName.setSelection(etProductName.getText().length());
                }

                // Set current price
                if (productData.has("prod_price")) {
                    String currentPrice = productData.getString("prod_price");
                    tvCurrentPrice.setText("₹" + currentPrice);
                    etNewPrice.setText(currentPrice);
                    etNewPrice.setSelection(etNewPrice.getText().length());
                }

                // Set discount type and value
                if (productData.has("disc_type")) {
                    String discType = productData.getString("disc_type");
                    selectedDiscountType[0] = discType;

                    // Set initial toggle button state
                    setDiscountTypeToggleState(btnNoDiscount, btnFlatDiscount, btnPercentageDiscount, discType);

                    if (productData.has("disc_value")) {
                        String discValue = productData.getString("disc_value");
                        etDiscountValue.setText(discValue);

                        // Enable/disable discount value based on discount type
                        if ("0".equals(discType)) {
                            etDiscountValue.setEnabled(false);
                            etDiscountValue.setText("");
                            textInputLayoutDiscountValue.setHint("No Discount");
                        } else {
                            etDiscountValue.setEnabled(true);
                            if ("1".equals(discType)) {
                                textInputLayoutDiscountValue.setHint("Flat Discount Amount (₹)");
                            } else if ("2".equals(discType)) {
                                textInputLayoutDiscountValue.setHint("Percentage Discount (%)");
                            }
                        }
                    }
                }

                // Load product image
                if (productData.has("main_image")) {
                    String imageUrl = Attributes.Root_Url + "uploads/images/" + productData.getString("main_image");
                    if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null")) {
                        Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_product_placeholder)
                                .error(R.drawable.ic_product_placeholder)
                                .into(dialogProductImage);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error setting dialog data: " + e.getMessage());
        }

        // Toggle button click listeners
        btnNoDiscount.setOnClickListener(v -> {
            selectedDiscountType[0] = "0";
            setDiscountTypeToggleState(btnNoDiscount, btnFlatDiscount, btnPercentageDiscount, "0");
            etDiscountValue.setEnabled(false);
            etDiscountValue.setText("");
            textInputLayoutDiscountValue.setHint("No Discount");
            textInputLayoutDiscountValue.setError(null);
            layoutDiscountedPrice.setVisibility(View.GONE);
        });

        btnFlatDiscount.setOnClickListener(v -> {
            selectedDiscountType[0] = "1";
            setDiscountTypeToggleState(btnNoDiscount, btnFlatDiscount, btnPercentageDiscount, "1");
            etDiscountValue.setEnabled(true);
            textInputLayoutDiscountValue.setHint("Flat Discount Amount (₹)");
            calculateAndDisplayDiscountedPrice(etNewPrice, selectedDiscountType[0], etDiscountValue, tvDiscountedPrice, layoutDiscountedPrice);
        });

        btnPercentageDiscount.setOnClickListener(v -> {
            selectedDiscountType[0] = "2";
            setDiscountTypeToggleState(btnNoDiscount, btnFlatDiscount, btnPercentageDiscount, "2");
            etDiscountValue.setEnabled(true);
            textInputLayoutDiscountValue.setHint("Percentage Discount (%)");
            calculateAndDisplayDiscountedPrice(etNewPrice, selectedDiscountType[0], etDiscountValue, tvDiscountedPrice, layoutDiscountedPrice);
        });

        // Calculate and display discounted price when values change
        TextWatcher discountCalculator = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!"0".equals(selectedDiscountType[0])) {
                    calculateAndDisplayDiscountedPrice(etNewPrice, selectedDiscountType[0], etDiscountValue, tvDiscountedPrice, layoutDiscountedPrice);
                }
            }
        };

        etNewPrice.addTextChangedListener(discountCalculator);
        etDiscountValue.addTextChangedListener(discountCalculator);

        // Price validation and warning
        // Price validation and warning
        etNewPrice.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String newPriceStr = s.toString().trim();
                String currentPriceStr = tvCurrentPrice.getText().toString().replace("₹", "").trim();

                if (!newPriceStr.isEmpty() && !currentPriceStr.isEmpty()) {
                    try {
                        double currentPriceVal = Double.parseDouble(currentPriceStr);
                        double newPriceVal = Double.parseDouble(newPriceStr);

                        if (newPriceVal < currentPriceVal * 0.5) {
                            // Price reduced by more than 50%
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price reduced significantly (over 50%)");
                            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color)));
                        } else if (newPriceVal > currentPriceVal * 2) {
                            // Price increased by more than 100%
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price increased significantly (over 100%)");
                            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color)));
                        } else if (newPriceVal < 1) {
                            // Price too low
                            layoutWarning.setVisibility(View.VISIBLE);
                            tvWarning.setText("Price seems too low");
                            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.warning_color)));
                        } else {
                            layoutWarning.setVisibility(View.GONE);
                            btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.colorPrimary)));
                        }
                    } catch (NumberFormatException e) {
                        layoutWarning.setVisibility(View.GONE);
                        btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.colorPrimary)));
                    }
                } else {
                    layoutWarning.setVisibility(View.GONE);
                    btnUpdate.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(ProductViewActivity.this, R.color.colorPrimary)));
                }
            }
        });

        // Image selection
        fabEditImage.setOnClickListener(v -> {
            selectImageFromGallery();
        });

        // Cancel button
        btnCancel.setOnClickListener(v -> {
            // Reset selected image when canceling
            selectedImageUri = null;
            selectedImageFile = null;
            currentEditDialog.dismiss();
        });

        // Update button
        btnUpdate.setOnClickListener(v -> {
            String productName = etProductName.getText().toString().trim();
            String newPrice = etNewPrice.getText().toString().trim();
            String discountValue = etDiscountValue.getText().toString().trim();

            boolean hasError = false;

            // Validate product name
            if (productName.isEmpty()) {
                textInputLayoutName.setError("Product name is required");
                hasError = true;
            } else if (productName.length() < 2) {
                textInputLayoutName.setError("Product name must be at least 2 characters");
                hasError = true;
            } else {
                textInputLayoutName.setError(null);
            }

            // Validate price
            if (!newPrice.isEmpty()) {
                try {
                    double priceValue = Double.parseDouble(newPrice);
                    if (priceValue <= 0) {
                        textInputLayoutPrice.setError("Price must be greater than 0");
                        hasError = true;
                    } else if (priceValue > 1000000) {
                        textInputLayoutPrice.setError("Price seems too high");
                        hasError = true;
                    } else {
                        textInputLayoutPrice.setError(null);
                    }
                } catch (NumberFormatException e) {
                    textInputLayoutPrice.setError("Please enter a valid price");
                    hasError = true;
                }
            } else {
                textInputLayoutPrice.setError("Please enter a price");
                hasError = true;
            }

            // Validate discount
            // Validate discount
            if (!"0".equals(selectedDiscountType[0])) {
                if (discountValue.isEmpty()) {
                    textInputLayoutDiscountValue.setError("Please enter discount value");
                    hasError = true;
                } else {
                    try {
                        double discountVal = Double.parseDouble(discountValue);
                        double priceVal = Double.parseDouble(newPrice);

                        if ("2".equals(selectedDiscountType[0])) {
                            if (discountVal < 0 || discountVal > 100) {
                                textInputLayoutDiscountValue.setError("Percentage must be between 0-100");
                                hasError = true;
                            } else if ((priceVal * discountVal / 100) >= priceVal) {
                                textInputLayoutDiscountValue.setError("Discount amount cannot be greater than or equal to price");
                                hasError = true;
                            } else {
                                textInputLayoutDiscountValue.setError(null);
                            }
                        } else if ("1".equals(selectedDiscountType[0])) {
                            if (discountVal < 0) {
                                textInputLayoutDiscountValue.setError("Discount must be positive");
                                hasError = true;
                            } else if (discountVal >= priceVal) {
                                textInputLayoutDiscountValue.setError("Discount cannot be greater than or equal to price");
                                hasError = true;
                            } else {
                                textInputLayoutDiscountValue.setError(null);
                            }
                        }
                    } catch (NumberFormatException e) {
                        textInputLayoutDiscountValue.setError("Please enter a valid discount");
                        hasError = true;
                    }
                }
            }
            if (!hasError) {
                updateProductDetails(productName, newPrice, selectedDiscountType[0], discountValue, selectedImageFile);
                currentEditDialog.dismiss();
            }
        });

        // Clear errors when user starts typing
        etProductName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayoutName.setError(null);
            }
        });

        etNewPrice.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayoutPrice.setError(null);
            }
        });

        etDiscountValue.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                textInputLayoutDiscountValue.setError(null);
            }
        });

        // Initial calculation
        if (!"0".equals(selectedDiscountType[0])) {
            calculateAndDisplayDiscountedPrice(etNewPrice, selectedDiscountType[0], etDiscountValue, tvDiscountedPrice, layoutDiscountedPrice);
        }
    }

    // Helper method to set toggle button states
    // Helper method to set toggle button states
    private void setDiscountTypeToggleState(MaterialButton btnNoDiscount,
                                            MaterialButton btnFlatDiscount,
                                            MaterialButton btnPercentageDiscount,
                                            String discountType) {

        // Reset all buttons to default state
        btnNoDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        btnNoDiscount.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
        btnNoDiscount.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));

        btnFlatDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        btnFlatDiscount.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
        btnFlatDiscount.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));

        btnPercentageDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.white)));
        btnPercentageDiscount.setTextColor(ContextCompat.getColor(this, R.color.primary_color));
        btnPercentageDiscount.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));

        // Set active state for selected button
        switch (discountType) {
            case "0": // No Discount
                btnNoDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));
                btnNoDiscount.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
            case "1": // Flat
                btnFlatDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));
                btnFlatDiscount.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
            case "2": // Percentage
                btnPercentageDiscount.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary_color)));
                btnPercentageDiscount.setTextColor(ContextCompat.getColor(this, android.R.color.white));
                break;
        }
    }

    // Updated method to calculate discounted price
    // Updated method to calculate discounted price
    private void calculateAndDisplayDiscountedPrice(TextInputEditText etNewPrice,
                                                    String discountType,
                                                    TextInputEditText etDiscountValue,
                                                    TextView tvDiscountedPrice,
                                                    LinearLayout layoutDiscountedPrice) {

        String priceStr = etNewPrice.getText().toString().trim();
        String discountValueStr = etDiscountValue.getText().toString().trim();

        if (!priceStr.isEmpty() && !discountValueStr.isEmpty() && !"0".equals(discountType)) {
            try {
                double price = Double.parseDouble(priceStr);
                double discountValue = Double.parseDouble(discountValueStr);
                double discountedPrice = price;

                if ("2".equals(discountType)) { // Percentage
                    if (discountValue > 0 && discountValue <= 100) {
                        discountedPrice = price - (price * discountValue / 100);
                    }
                } else if ("1".equals(discountType)) { // Flat
                    if (discountValue > 0 && discountValue < price) {
                        discountedPrice = price - discountValue;
                    }
                }

                // Only show if discounted price is valid and less than original
                if (discountedPrice > 0 && discountedPrice < price) {
                    tvDiscountedPrice.setText("₹" + String.format("%.2f", discountedPrice));
                    layoutDiscountedPrice.setVisibility(View.VISIBLE);
                } else {
                    layoutDiscountedPrice.setVisibility(View.GONE);
                }

            } catch (NumberFormatException e) {
                layoutDiscountedPrice.setVisibility(View.GONE);
            }
        } else {
            layoutDiscountedPrice.setVisibility(View.GONE);
        }
    }

    private void selectImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - No permission needed for gallery access
            openImagePicker();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-12 - Request READ_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                openImagePicker();
            }
        } else {
            // Android 5 and below - No runtime permissions needed
            openImagePicker();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/jpg"});
        startActivityForResult(Intent.createChooser(intent, "Select Product Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied to read storage", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Refresh the product data to show the new variant
            loadProductDataFromAPI();
            Toast.makeText(this, "Variant added successfully!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();

            if (selectedImageUri != null) {
                try {
                    // Convert URI to File
                    selectedImageFile = getFileFromUri(selectedImageUri);

                    // Update the dialog image immediately if dialog is open
                    if (currentEditDialog != null && currentEditDialog.isShowing() && dialogProductImage != null) {
                        Picasso.get()
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_product_placeholder)
                                .error(R.drawable.ic_product_placeholder)
                                .into(dialogProductImage);

                        Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error processing selected image: " + e.getMessage());
                    Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private File getFileFromUri(Uri uri) throws IOException {
        // Create a temporary file
        File file = new File(getCacheDir(), "temp_product_image.jpg");

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[4 * 1024]; // 4KB buffer
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        }

        return file;
    }

    private void updateProductDetails(String productName, String newPrice, String discountType, String discountValue, File imageFile) {
        showLoadingState();

        // Create multipart form data
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("prod_name", productName)
                .addFormDataPart("prod_price", newPrice)
                .addFormDataPart("disc_type", discountType)
                .addFormDataPart("disc_value", discountValue.isEmpty() ? "0" : discountValue);

        // Add image file if selected
        if (imageFile != null && imageFile.exists()) {
            multipartBuilder.addFormDataPart("main_image",
                    "product_image.jpg",
                    RequestBody.create(MediaType.parse("image/*"), imageFile));
        }

        RequestBody requestBody = multipartBuilder.build();

        // Build the request
        String url = Attributes.Main_Url + "store/productUpdate/" + productId;
        Log.d(TAG, "Updating product - URL: " + url +
                ", Name: " + productName +
                ", Price: " + newPrice +
                ", Discount Type: " + discountType +
                ", Discount Value: " + discountValue +
                ", Has Image: " + (imageFile != null));

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
                    Log.e(TAG, "Product update failed: " + e.getMessage());
                    Toast.makeText(ProductViewActivity.this,
                            "Failed to update product: Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Product update response: " + responseBodyString);

                runOnUiThread(() -> {
                    hideLoadingState();
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful()) {
                            boolean isSuccess = false;
                            String message = "Product updated successfully";

                            if (responseObject.has("status")) {
                                String status = responseObject.optString("status", "");
                                isSuccess = "success".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
                            } else if (responseObject.has("success")) {
                                isSuccess = responseObject.getBoolean("success");
                            } else {
                                isSuccess = response.isSuccessful();
                            }

                            if (responseObject.has("message")) {
                                message = responseObject.getString("message");
                            }

                            if (isSuccess) {
                                Toast.makeText(ProductViewActivity.this, message, Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Product updated successfully");

                                // Reset selected image after successful upload
                                selectedImageUri = null;
                                selectedImageFile = null;

                                // Refresh the product data to show updated details
                                loadProductDataFromAPI();

                            } else {
                                String errorMessage = responseObject.optString("error", "Failed to update product");
                                Log.e(TAG, "Product update failed: " + errorMessage);
                                Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            String errorMessage = "Server error: " + response.code();
                            Log.e(TAG, "Product update failed: " + errorMessage);
                            Toast.makeText(ProductViewActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing product update response: " + e.getMessage());
                        if (response.isSuccessful()) {
                            Toast.makeText(ProductViewActivity.this, "Product updated successfully", Toast.LENGTH_SHORT).show();

                            // Reset selected image
                            selectedImageUri = null;
                            selectedImageFile = null;

                            loadProductDataFromAPI();
                        } else {
                            Toast.makeText(ProductViewActivity.this, "Failed to update product", Toast.LENGTH_SHORT).show();
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
                            boolean isSuccess = false;
                            if (responseObject.has("status")) {
                                isSuccess = responseObject.getBoolean("status");
                            } else if (responseObject.has("success")) {
                                isSuccess = responseObject.getBoolean("success");
                            } else {
                                isSuccess = response.isSuccessful();
                            }

                            if (isSuccess) {
                                Toast.makeText(ProductViewActivity.this, "Variant deleted successfully", Toast.LENGTH_SHORT).show();
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

                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(productName);
                }
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
                    tvProductDescription.setVisibility(View.GONE);
                }
            } else {
                tvProductDescription.setVisibility(View.GONE);
            }

            // Set product status
            if (product.has("status")) {
                boolean isActive = "1".equals(product.getString("status"));
                switchProductStatus.setOnCheckedChangeListener(null);
                switchProductStatus.setChecked(isActive);
                setupListeners();
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

            // FIXED: Display discount information and calculate final price
            double originalPrice = 0;
            double finalPrice = 0;
            boolean hasValidDiscount = false;
            String discountText = "";
            double discountedPrice = 0;

            // Get original price
            if (product.has("prod_price")) {
                try {
                    originalPrice = Double.parseDouble(product.getString("prod_price"));
                    finalPrice = originalPrice; // Default to original price
                } catch (NumberFormatException e) {
                    Log.e("PRICE_ERROR", "Error parsing price: " + e.getMessage());
                }
            }

            // Calculate discount if available
            if (product.has("disc_type") && product.has("disc_value")) {
                String discType = product.getString("disc_type");
                String discValue = product.getString("disc_value");

                Log.d("DISCOUNT_DEBUG", "Discount Type: " + discType + ", Discount Value: " + discValue);

                // Check if discount is valid
                if (!"0".equals(discType) && !discValue.isEmpty() && !"0".equals(discValue)) {
                    try {
                        double discountAmount = Double.parseDouble(discValue);

                        if ("1".equals(discType)) { // Flat discount
                            if (discountAmount > 0 && discountAmount < originalPrice) {
                                discountedPrice = originalPrice - discountAmount;
                                discountText = "₹" + String.format("%.2f", discountAmount) + " OFF";
                                hasValidDiscount = true;
                            }
                        } else if ("2".equals(discType)) { // Percentage discount
                            if (discountAmount > 0 && discountAmount <= 100) {
                                double discountAmt = (originalPrice * discountAmount) / 100;
                                if (discountAmt < originalPrice) {
                                    discountedPrice = originalPrice - discountAmt;
                                    discountText = String.format("%.0f", discountAmount) + "% OFF";
                                    hasValidDiscount = true;
                                }
                            }
                        }

                        if (hasValidDiscount && discountedPrice > 0) {
                            finalPrice = discountedPrice;
                        }

                    } catch (NumberFormatException e) {
                        Log.e("DISCOUNT_ERROR", "Error parsing discount value: " + e.getMessage());
                    }
                }
            }

            // Display prices
            if (hasValidDiscount) {
                // Show discounted price as current price
                tvProductPrice.setText("₹" + String.format("%.2f", finalPrice));

                // Show original price with strike-through
                tvDiscountedPriceMain.setText("₹" + String.format("%.2f", originalPrice));
                tvDiscountedPriceMain.setPaintFlags(tvDiscountedPriceMain.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                // Show discount text
                tvProductDiscount.setText(discountText);
                layoutProductDiscount.setVisibility(View.VISIBLE);

                Log.d("DISCOUNT_DEBUG", "Discount applied - Original: " + originalPrice +
                        ", Final: " + finalPrice + ", Discount: " + discountText);
            } else {
                // No discount - show only original price
                tvProductPrice.setText("₹" + String.format("%.2f", originalPrice));
                layoutProductDiscount.setVisibility(View.GONE);
                Log.d("DISCOUNT_DEBUG", "No discount applied - Price: " + originalPrice);
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
                    String status = variant.optString("status", "0");
                    if ("1".equals(status) || "true".equalsIgnoreCase(status)) {
                        activeVariants++;
                    }

                    if (variant.has("stock")) {
                        String stockValue = variant.optString("stock", "0");
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
                if (variant.has("prod_id") && variant.getString("prod_id").equals(productId)) {
                    variantsList.add(variant);
                }
            }

            Log.d("DEBUG_LOAD", "Loaded " + variantsList.size() + " variants");
            variantAdapter.notifyDataSetChanged();
            updateProductStats();

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
                updateVariantStatusInAPI(productId, variantId, statusValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        variantAdapter.notifyDataSetChanged();
        updateProductStats();

        new android.os.Handler().postDelayed(() -> {
            isUpdatingProductStatus = false;
        }, 500);
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

    private void updateProductStatusBasedOnVariants() {
        if (isUpdatingProductStatus) {
            return;
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

        boolean shouldProductBeActive = !allVariantsInactive;

        if (switchProductStatus.isChecked() != shouldProductBeActive) {
            isUpdatingProductStatus = true;
            switchProductStatus.setChecked(shouldProductBeActive);
            updateStatusBadge(shouldProductBeActive);
            updateProductStatusInAPI(productId, shouldProductBeActive);

            new android.os.Handler().postDelayed(() -> {
                isUpdatingProductStatus = false;
            }, 500);
        }
    }

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
                        Log.d("API_SUCCESSdd", String.valueOf(response));
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