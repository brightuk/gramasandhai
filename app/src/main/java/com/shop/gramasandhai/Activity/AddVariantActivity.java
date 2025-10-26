package com.shop.gramasandhai.Activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AddVariantActivity extends AppCompatActivity {

    private static final String TAG = "AddVariantActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CAMERA = 100;
    private static final int PERMISSION_REQUEST_GALLERY = 101;

    // UI Components
    private TextInputEditText etVariantMeasurement, etVariantPrice, etVariantSKU, etVariantHSNCode,
            etVariantStock, etVariantDiscountPrice;
    private TextInputLayout measurementInputLayout, priceInputLayout, stockInputLayout,
            discountPriceInputLayout, skuInputLayout, hsnInputLayout;
    private Spinner spinnerVariantMeasurementUnit, spinnerVariantStockUnit,
            spinnerVariantDiscountType, spinnerVariantStatus;
    private MaterialButton btnSubmitVariant, btnCancel;
    private ImageView ivVariantImagePreview;
    private LinearProgressIndicator progressIndicator;
    private MaterialCardView cardImageUpload;
    private FloatingActionButton fabRemoveImage;
    private LinearLayout uploadPlaceholder;

    private Uri variantImageUri;
    private String currentPhotoPath;
    private String productId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_variant);

        // Get product ID from intent
        productId = getIntent().getStringExtra("product_id");
        if (productId == null) {
            Toast.makeText(this, "Product ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupSpinners();
        setupClickListeners();
    }

    private void initializeViews() {
        // Progress Indicator
        progressIndicator = findViewById(R.id.progressIndicator);

        // Image Upload Section
        cardImageUpload = findViewById(R.id.cardImageUpload);
        ivVariantImagePreview = findViewById(R.id.ivVariantImagePreview);
        fabRemoveImage = findViewById(R.id.fabRemoveImage);
        uploadPlaceholder = findViewById(R.id.uploadPlaceholder);

        // Text Input Fields
        etVariantMeasurement = findViewById(R.id.etVariantMeasurement);
        etVariantPrice = findViewById(R.id.etVariantPrice);
        etVariantSKU = findViewById(R.id.etVariantSKU);
        etVariantHSNCode = findViewById(R.id.etVariantHSNCode);
        etVariantStock = findViewById(R.id.etVariantStock);
        etVariantDiscountPrice = findViewById(R.id.etVariantDiscountPrice);

        // TextInputLayouts
        measurementInputLayout = findViewById(R.id.measurementInputLayout);
        priceInputLayout = findViewById(R.id.priceInputLayout);
        stockInputLayout = findViewById(R.id.stockInputLayout);
        discountPriceInputLayout = findViewById(R.id.discountPriceInputLayout);
        skuInputLayout = findViewById(R.id.skuInputLayout);
        hsnInputLayout = findViewById(R.id.hsnInputLayout);

        // Spinners
        spinnerVariantMeasurementUnit = findViewById(R.id.spinnerVariantMeasurementUnit);
        spinnerVariantStockUnit = findViewById(R.id.spinnerVariantStockUnit);
        spinnerVariantDiscountType = findViewById(R.id.spinnerVariantDiscountType);
        spinnerVariantStatus = findViewById(R.id.spinnerVariantStatus);

        // Buttons
        btnSubmitVariant = findViewById(R.id.btnSubmitVariant);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Add New Variant");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_variant, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save_draft) {
            saveAsDraft();
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAsDraft() {
        Toast.makeText(this, "Draft saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Add Variant Help")
                .setMessage("Fill in the variant details:\n\n• Measurement: Size/quantity of the variant\n• Price: Selling price\n• Stock: Available quantity\n• Discount: Optional discount amount\n• Status: Availability status\n• SKU/HSN: Identification codes")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void setupSpinners() {
        // Measurement Unit Spinner
        String[] measurementUnits = {"Select Unit", "kg", "gm", "mg", "lb", "oz", "l", "ml", "cm", "m", "mm", "inch", "ft", "piece", "pack"};
        ArrayAdapter<String> measurementAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, measurementUnits);
        measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVariantMeasurementUnit.setAdapter(measurementAdapter);

        // Stock Unit Spinner
        String[] stockUnits = {"Select Unit", "piece", "pack", "box", "carton", "set", "pair", "dozen"};
        ArrayAdapter<String> stockUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stockUnits);
        stockUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVariantStockUnit.setAdapter(stockUnitAdapter);

        // Discount Type Spinner
        String[] discountTypes = {"Select Discount Type", "No Discount", "Flat", "Percentage"};
        ArrayAdapter<String> discountTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, discountTypes);
        discountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVariantDiscountType.setAdapter(discountTypeAdapter);

        // Status Spinner
        String[] statusOptions = {"Select Status", "Available", "Unavailable", "Out of Stock", "Discontinued"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVariantStatus.setAdapter(statusAdapter);

        // Set default selections
        spinnerVariantStatus.setSelection(1); // Available
        spinnerVariantDiscountType.setSelection(1); // No Discount
    }

    private void setupClickListeners() {
        // Image upload
        cardImageUpload.setOnClickListener(v -> showImageSelectionDialog());

        // Remove image
        fabRemoveImage.setOnClickListener(v -> removeSelectedImage());

        // Form submission
        btnSubmitVariant.setOnClickListener(v -> submitVariantForm());

        // Cancel button
        btnCancel.setOnClickListener(v -> showCancelConfirmation());
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Cancel Variant Creation")
                .setMessage("Are you sure you want to cancel? All unsaved changes will be lost.")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> finish())
                .setNegativeButton("Continue Editing", null)
                .show();
    }

    private void removeSelectedImage() {
        variantImageUri = null;
        ivVariantImagePreview.setVisibility(View.GONE);
        uploadPlaceholder.setVisibility(View.VISIBLE);
        fabRemoveImage.setVisibility(View.GONE);
        Toast.makeText(this, "Image removed", Toast.LENGTH_SHORT).show();
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Variant Image");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Cancel"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    openCamera();
                    break;
                case 1:
                    openGallery();
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void openCamera() {
        if (checkCameraPermission()) {
            dispatchTakePictureIntent();
        } else {
            requestCameraPermission();
        }
    }

    private void openGallery() {
        if (checkStoragePermission()) {
            dispatchGalleryIntent();
        } else {
            requestStoragePermission();
        }
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestCameraPermission() {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CAMERA);
    }

    private void requestStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_GALLERY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case PERMISSION_REQUEST_CAMERA:
                    dispatchTakePictureIntent();
                    break;
                case PERMISSION_REQUEST_GALLERY:
                    dispatchGalleryIntent();
                    break;
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file: " + ex.getMessage());
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                variantImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, variantImageUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "VARIANT_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null && data.getData() != null) {
                        variantImageUri = data.getData();
                        displaySelectedImage();
                    }
                    break;
                case CAMERA_REQUEST:
                    if (variantImageUri != null) {
                        displaySelectedImage();
                        // Notify gallery about the new image
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(variantImageUri);
                        sendBroadcast(mediaScanIntent);
                    }
                    break;
            }
        }
    }

    private void displaySelectedImage() {
        if (variantImageUri != null) {
            ivVariantImagePreview.setImageURI(variantImageUri);
            ivVariantImagePreview.setVisibility(View.VISIBLE);
            uploadPlaceholder.setVisibility(View.GONE);
            fabRemoveImage.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateVariantForm() {
        boolean isValid = true;

        // Clear previous errors
        clearErrors();

        // Measurement validation
        String measurement = etVariantMeasurement.getText().toString().trim();
        if (measurement.isEmpty()) {
            showError(measurementInputLayout, "Measurement is required");
            isValid = false;
        } else if (!isValidDecimal(measurement)) {
            showError(measurementInputLayout, "Please enter a valid measurement");
            isValid = false;
        }

        // Measurement unit validation
        if (spinnerVariantMeasurementUnit.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select measurement unit", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Price validation
        String price = etVariantPrice.getText().toString().trim();
        if (price.isEmpty()) {
            showError(priceInputLayout, "Price is required");
            isValid = false;
        } else if (!isValidDecimal(price)) {
            showError(priceInputLayout, "Please enter a valid price");
            isValid = false;
        }

        // Stock validation
        String stock = etVariantStock.getText().toString().trim();
        if (stock.isEmpty()) {
            showError(stockInputLayout, "Stock quantity is required");
            isValid = false;
        } else if (!isValidInteger(stock)) {
            showError(stockInputLayout, "Please enter a valid stock quantity");
            isValid = false;
        }

        // Stock unit validation
        if (spinnerVariantStockUnit.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select stock unit", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Discount validation
        String discountPrice = etVariantDiscountPrice.getText().toString().trim();
        if (!discountPrice.isEmpty() && !isValidDecimal(discountPrice)) {
            showError(discountPriceInputLayout, "Please enter a valid discount amount");
            isValid = false;
        }

        // Discount type validation
        if (spinnerVariantDiscountType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select discount type", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Status validation
        if (spinnerVariantStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select status", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        return isValid;
    }

    private void clearErrors() {
        if (measurementInputLayout != null) measurementInputLayout.setError(null);
        if (priceInputLayout != null) priceInputLayout.setError(null);
        if (stockInputLayout != null) stockInputLayout.setError(null);
        if (discountPriceInputLayout != null) discountPriceInputLayout.setError(null);
        if (skuInputLayout != null) skuInputLayout.setError(null);
        if (hsnInputLayout != null) hsnInputLayout.setError(null);
    }

    private void showError(TextInputLayout inputLayout, String error) {
        if (inputLayout != null) {
            inputLayout.setError(error);
        }
    }

    private boolean isValidDecimal(String value) {
        try {
            double num = Double.parseDouble(value);
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidInteger(String value) {
        try {
            int num = Integer.parseInt(value);
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void submitVariantForm() {
        if (!validateVariantForm()) {
            return;
        }

        submitVariantToServer();
    }

    private void submitVariantToServer() {
        showProgress();

        try {
            // Get form data
            String measurement = etVariantMeasurement.getText().toString().trim();
            String measurementUnit = spinnerVariantMeasurementUnit.getSelectedItem().toString();
            String price = etVariantPrice.getText().toString().trim();
            String sku = etVariantSKU.getText().toString().trim();
            String hsnCode = etVariantHSNCode.getText().toString().trim();
            String stock = etVariantStock.getText().toString().trim();
            String stockUnit = spinnerVariantStockUnit.getSelectedItem().toString();
            String discountType = getDiscountTypeValue(spinnerVariantDiscountType.getSelectedItem().toString());
            String discountPrice = etVariantDiscountPrice.getText().toString().trim();
            if (discountPrice.isEmpty()) discountPrice = "0";
            String status = getStatusValue(spinnerVariantStatus.getSelectedItem().toString());

            // Create multipart request
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("productid", productId)
                    .addFormDataPart("measurement[]", measurement)
                    .addFormDataPart("measureUnit[]", measurementUnit)
                    .addFormDataPart("prices[]", price)
                    .addFormDataPart("discount_type[]", discountType)
                    .addFormDataPart("discount_price[]", discountPrice)
                    .addFormDataPart("stock[]", stock)
                    .addFormDataPart("stock_unit[]", stockUnit)
                    .addFormDataPart("status[]", status)
                    .addFormDataPart("sku_code[]", sku)
                    .addFormDataPart("hsn_code[]", hsnCode);

            // Add variant image if exists
            if (variantImageUri != null) {
                String imagePath = getRealPathFromURI(variantImageUri);
                if (imagePath != null) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        multipartBuilder.addFormDataPart("variant_image[]",
                                imageFile.getName(),
                                RequestBody.create(MediaType.parse("image/*"), imageFile));
                    }
                }
            }

            RequestBody requestBody = multipartBuilder.build();

            Request request = new Request.Builder()
                    .url(Attributes.Main_Url + "store/addProductVariant")
                    .post(requestBody)
                    .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(AddVariantActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("VARIANT_ERROR", "Request failed: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("VARIANT_RESPONSE", responseBody);

                    runOnUiThread(() -> {
                        hideProgress();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (response.isSuccessful() && "success".equals(jsonResponse.optString("status"))) {
                                showSuccess("Variant added successfully!");
                            } else {
                                String errorMessage = jsonResponse.optString("message", "Failed to add variant");
                                Toast.makeText(AddVariantActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Error parsing response: " + e.getMessage());
                            Toast.makeText(AddVariantActivity.this, "Error parsing server response", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            hideProgress();
            Log.e(TAG, "Error submitting variant: " + e.getMessage(), e);
            Toast.makeText(this, "Error submitting variant: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showProgress() {
        progressIndicator.setVisibility(View.VISIBLE);
        btnSubmitVariant.setEnabled(false);
        btnCancel.setEnabled(false);
    }

    private void hideProgress() {
        progressIndicator.setVisibility(View.GONE);
        btnSubmitVariant.setEnabled(true);
        btnCancel.setEnabled(true);
    }

    private void showSuccess(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    setResult(RESULT_OK);
                    finish();
                })
                .setCancelable(false)
                .show();
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

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentUri, proj, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return contentUri.getPath();
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up temporary camera file if exists
        if (currentPhotoPath != null) {
            File file = new File(currentPhotoPath);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}