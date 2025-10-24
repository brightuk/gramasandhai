package com.shop.gramasandhai.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.material.textfield.TextInputEditText;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.Model.Category;
import com.shop.gramasandhai.Model.Subcategory;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.*;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Main form fields
    private TextInputEditText etProductName, etMeasurement, etPrice, etSKU, etHSNCode, etStock, etDiscountPrice;
    private TextInputEditText etManufacturer, etMadeIn, etProductDescription, etShippingPolicy, etFSSAINo, etTax;
    private RadioGroup radioProductType;
    private RadioButton radioPacked, radioLoose;
    private Spinner spinnerMeasurementUnit, spinnerStockUnit, spinnerDiscountType, spinnerStatus;
    private Spinner spinnerCategory, spinnerSubcategory;
    private Button btnSelectImage, btnAddVariant, btnSubmit;
    private LinearLayout containerVariants;
    private ImageView ivProductImagePreview;

    private Uri productImageUri;
    private String currentPhotoPath;
    private int variantCount = 0;
    private List<View> variantViews = new ArrayList<>();

    // Category data
    private List<Category> categories = new ArrayList<>();
    private List<Subcategory> subcategories = new ArrayList<>();
    private List<Subcategory> filteredSubcategories = new ArrayList<>();

    // Adapters
    private ArrayAdapter<Category> categoryAdapter;
    private ArrayAdapter<Subcategory> subcategoryAdapter;

    // Additional fields
    private CheckBox cbReturnable, cbCancelable, cbCodAllowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate started");

            // Set content view first
            setContentView(R.layout.activity_add_product);
            Log.d(TAG, "setContentView completed");

            // Initialize views
            Log.d(TAG, "Initializing views...");
            initializeViews();

            // Setup components
            Log.d(TAG, "Setting up spinners...");
            setupSpinners();

            Log.d(TAG, "Setting up listeners...");
            setupClickListeners();

            Log.d(TAG, "Loading categories...");
            loadCategories();

            Log.d(TAG, "onCreate completed successfully");

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading page: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeViews() {
        try {
            // Text Input Fields
            etProductName = findViewById(R.id.etProductName);
            etMeasurement = findViewById(R.id.etMeasurement);
            etPrice = findViewById(R.id.etPrice);
            etSKU = findViewById(R.id.etSKU);
            etHSNCode = findViewById(R.id.etHSNCode);
            etStock = findViewById(R.id.etStock);
            etDiscountPrice = findViewById(R.id.etDiscountPrice);

            // New fields
            etManufacturer = findViewById(R.id.etManufacturer);
            etMadeIn = findViewById(R.id.etMadeIn);
            etProductDescription = findViewById(R.id.etProductDescription);
            etShippingPolicy = findViewById(R.id.etShippingPolicy);
            etFSSAINo = findViewById(R.id.etFSSAINo);
            etTax = findViewById(R.id.etTax);

            // Radio Buttons
            radioProductType = findViewById(R.id.radioProductType);
            radioPacked = findViewById(R.id.radioPacked);
            radioLoose = findViewById(R.id.radioLoose);

            // Spinners
            spinnerMeasurementUnit = findViewById(R.id.spinnerMeasurementUnit);
            spinnerStockUnit = findViewById(R.id.spinnerStockUnit);
            spinnerDiscountType = findViewById(R.id.spinnerDiscountType);
            spinnerStatus = findViewById(R.id.spinnerStatus);
            spinnerCategory = findViewById(R.id.spinnerCategory);
            spinnerSubcategory = findViewById(R.id.spinnerSubcategory);

            // Buttons
            btnSelectImage = findViewById(R.id.btnSelectImage);
            btnAddVariant = findViewById(R.id.btnAddVariant);
            btnSubmit = findViewById(R.id.btnSubmit);

            // Containers
            containerVariants = findViewById(R.id.containerVariants);

            // Image Preview
            ivProductImagePreview = findViewById(R.id.ivProductImagePreview);

            // Checkboxes
            cbReturnable = findViewById(R.id.cbReturnable);
            cbCancelable = findViewById(R.id.cbCancelable);
            cbCodAllowed = findViewById(R.id.cbCodAllowed);

            Log.d(TAG, "All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            throw new RuntimeException("View initialization failed", e);
        }
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }

    private void loadCategories() {
        Log.d("ShopID", shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/addproduct")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddProductActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("Categories_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseCategories(responseObject);
                            parseSubcategories(responseObject);
                            updateCategorySpinners();

                            Toast.makeText(AddProductActivity.this,
                                    "Categories loaded successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AddProductActivity.this,
                                    "Failed to load categories", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        Toast.makeText(AddProductActivity.this,
                                "Error parsing categories data", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseCategories(JSONObject responseObject) throws JSONException {
        categories.clear();

        // Add default option
        Category defaultCategory = new Category();
        defaultCategory.setId("0");
        defaultCategory.setName("Select Category");
        categories.add(defaultCategory);

        JSONArray categoriesArray = responseObject.getJSONArray("categories");
        for (int i = 0; i < categoriesArray.length(); i++) {
            JSONObject categoryObj = categoriesArray.getJSONObject(i);
            Category category = new Category();
            category.setId(categoryObj.getString("category_id"));
            category.setName(categoryObj.getString("category_name"));
            category.setSubtitle(categoryObj.getString("category_subtitle"));
            category.setImageUrl(categoryObj.getString("category_image"));
            category.setStatus(categoryObj.getString("category_status"));
            category.setPosition(categoryObj.getString("position"));
            categories.add(category);
        }
    }

    private void parseSubcategories(JSONObject responseObject) throws JSONException {
        subcategories.clear();

        JSONArray subcategoriesArray = responseObject.getJSONArray("subcategories");
        for (int i = 0; i < subcategoriesArray.length(); i++) {
            JSONObject subcategoryObj = subcategoriesArray.getJSONObject(i);
            Subcategory subcategory = new Subcategory();
            subcategory.setId(subcategoryObj.getString("subcategory_id"));
            subcategory.setCategoryId(subcategoryObj.getString("category_id"));
            subcategory.setName(subcategoryObj.getString("sub_category_name"));
            subcategory.setSubtitle(subcategoryObj.getString("sub_category_subtitle"));
            subcategory.setImageUrl(subcategoryObj.getString("sub_category_image"));
            subcategory.setStatus(subcategoryObj.getString("subcategory_status"));
            subcategory.setCategoryName(subcategoryObj.getString("main_category"));
            subcategories.add(subcategory);
        }
    }

    private void setupSpinners() {
        try {
            // Measurement Unit Spinner
            String[] measurementUnits = {"Select Unit", "kg", "gm", "mg", "lb", "oz", "l", "ml", "cm", "m", "mm", "inch", "ft", "piece", "pack"};
            ArrayAdapter<String> measurementAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, measurementUnits);
            measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerMeasurementUnit.setAdapter(measurementAdapter);

            // Stock Unit Spinner
            String[] stockUnits = {"Select Unit", "piece", "pack", "box", "carton", "set", "pair", "dozen"};
            ArrayAdapter<String> stockUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stockUnits);
            stockUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStockUnit.setAdapter(stockUnitAdapter);

            // Discount Type Spinner
            String[] discountTypes = {"Select Discount Type", "No Discount", "Flat", "Percentage"};
            ArrayAdapter<String> discountTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, discountTypes);
            discountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerDiscountType.setAdapter(discountTypeAdapter);

            // Status Spinner
            String[] statusOptions = {"Select Status", "Available", "Unavailable", "Out of Stock", "Discontinued"};
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerStatus.setAdapter(statusAdapter);

            // Setup Category Spinners
            setupCategorySpinners();

            Log.d(TAG, "Spinners setup successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up spinners: " + e.getMessage(), e);
        }
    }

    private void setupCategorySpinners() {
        // Category Spinner Adapter
        categoryAdapter = new ArrayAdapter<Category>(this, android.R.layout.simple_spinner_item, categories) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                Category category = categories.get(position);
                textView.setText(category.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                Category category = categories.get(position);
                textView.setText(category.getName());
                return textView;
            }
        };
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // Initialize filtered subcategories with default option
        Subcategory defaultSubcategory = new Subcategory();
        defaultSubcategory.setId("0");
        defaultSubcategory.setName("Select Subcategory");
        filteredSubcategories.add(defaultSubcategory);

        // Subcategory Spinner Adapter
        subcategoryAdapter = new ArrayAdapter<Subcategory>(this, android.R.layout.simple_spinner_item, filteredSubcategories) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                Subcategory subcategory = filteredSubcategories.get(position);
                textView.setText(subcategory.getName());
                return textView;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                Subcategory subcategory = filteredSubcategories.get(position);
                textView.setText(subcategory.getName());
                return textView;
            }
        };
        subcategoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubcategory.setAdapter(subcategoryAdapter);

        // Category selection listener
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    Category selectedCategory = categories.get(position);
                    filterSubcategories(selectedCategory.getId());
                } else {
                    filteredSubcategories.clear();
                    Subcategory defaultSub = new Subcategory();
                    defaultSub.setId("0");
                    defaultSub.setName("Select Subcategory");
                    filteredSubcategories.add(defaultSub);
                    subcategoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void filterSubcategories(String categoryId) {
        filteredSubcategories.clear();

        Subcategory defaultSubcategory = new Subcategory();
        defaultSubcategory.setId("0");
        defaultSubcategory.setName("Select Subcategory");
        filteredSubcategories.add(defaultSubcategory);

        for (Subcategory subcategory : subcategories) {
            if (subcategory.getCategoryId().equals(categoryId)) {
                filteredSubcategories.add(subcategory);
            }
        }

        subcategoryAdapter.notifyDataSetChanged();
        spinnerSubcategory.setSelection(0);
    }

    private void updateCategorySpinners() {
        categoryAdapter.notifyDataSetChanged();

        filteredSubcategories.clear();
        Subcategory defaultSub = new Subcategory();
        defaultSub.setId("0");
        defaultSub.setName("Select Subcategory");
        filteredSubcategories.add(defaultSub);
        subcategoryAdapter.notifyDataSetChanged();
    }

    private void setupClickListeners() {
        btnSubmit.setOnClickListener(v -> {
            try {
                submitProductForm();
            } catch (Exception e) {
                Log.e(TAG, "Error in submit: " + e.getMessage(), e);
                Toast.makeText(this, "Error submitting form", Toast.LENGTH_SHORT).show();
            }
        });

        btnSelectImage.setOnClickListener(v -> showImageSelectionDialog());
        btnAddVariant.setOnClickListener(v -> addVariantForm());
    }

    private void showImageSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Product Image");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Cancel"}, (dialog, which) -> {
            switch (which) {
                case 0: openCamera(); break;
                case 1: openGallery(); break;
                case 2: dialog.dismiss(); break;
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
        ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
    }

    private void requestStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                for (int i = 0; i < permissions.length; i++) {
                    if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        dispatchTakePictureIntent();
                        return;
                    } else if ((permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                            permissions[i].equals(Manifest.permission.READ_MEDIA_IMAGES)) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        dispatchGalleryIntent();
                        return;
                    }
                }
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
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
                productImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, productImageUri);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PICK_IMAGE_REQUEST:
                    if (data != null && data.getData() != null) {
                        productImageUri = data.getData();
                        displaySelectedImage();
                    }
                    break;
                case CAMERA_REQUEST:
                    if (productImageUri != null) {
                        displaySelectedImage();
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(productImageUri);
                        sendBroadcast(mediaScanIntent);
                    }
                    break;
            }
        }
    }

    private void displaySelectedImage() {
        if (productImageUri != null) {
            ivProductImagePreview.setImageURI(productImageUri);
            ivProductImagePreview.setVisibility(View.VISIBLE);
            btnSelectImage.setText("Change Image");
            Toast.makeText(this, "Image selected successfully", Toast.LENGTH_SHORT).show();
        }
    }

    private void addVariantForm() {
        try {
            variantCount++;
            LayoutInflater inflater = LayoutInflater.from(this);
            View variantView = inflater.inflate(R.layout.item_variant_form, containerVariants, false);

            TextView tvVariantTitle = variantView.findViewById(R.id.tvVariantTitle);
            tvVariantTitle.setText("Variant " + variantCount);

            Button btnRemoveVariant = variantView.findViewById(R.id.btnRemoveVariant);
            btnRemoveVariant.setOnClickListener(v -> removeVariant(variantView));

            setupVariantSpinners(variantView);

            containerVariants.addView(variantView);
            variantViews.add(variantView);

            Toast.makeText(this, "Variant " + variantCount + " added", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error adding variant: " + e.getMessage(), e);
            Toast.makeText(this, "Error adding variant", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupVariantSpinners(View variantView) {
        try {
            Spinner spinnerVariantMeasurementUnit = variantView.findViewById(R.id.spinnerVariantMeasurementUnit);
            Spinner spinnerVariantStockUnit = variantView.findViewById(R.id.spinnerVariantStockUnit);
            Spinner spinnerVariantDiscountType = variantView.findViewById(R.id.spinnerVariantDiscountType);
            Spinner spinnerVariantStatus = variantView.findViewById(R.id.spinnerVariantStatus);

            String[] measurementUnits = {"Select Unit", "kg", "gm", "mg", "lb", "oz", "l", "ml", "cm", "m", "mm", "inch", "ft", "piece", "pack"};
            ArrayAdapter<String> measurementAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, measurementUnits);
            measurementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVariantMeasurementUnit.setAdapter(measurementAdapter);

            String[] stockUnits = {"Select Unit", "piece", "pack", "box", "carton", "set", "pair", "dozen"};
            ArrayAdapter<String> stockUnitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stockUnits);
            stockUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVariantStockUnit.setAdapter(stockUnitAdapter);

            String[] discountTypes = {"Select Discount Type", "No Discount", "Flat", "Percentage"};
            ArrayAdapter<String> discountTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, discountTypes);
            discountTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVariantDiscountType.setAdapter(discountTypeAdapter);

            String[] statusOptions = {"Select Status", "Available", "Unavailable", "Out of Stock", "Discontinued"};
            ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
            statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVariantStatus.setAdapter(statusAdapter);

        } catch (Exception e) {
            Log.e(TAG, "Error setting up variant spinners: " + e.getMessage(), e);
        }
    }

    private void removeVariant(View variantView) {
        try {
            containerVariants.removeView(variantView);
            variantViews.remove(variantView);
            variantCount--;
            updateVariantTitles();
            Toast.makeText(this, "Variant removed", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error removing variant: " + e.getMessage(), e);
        }
    }

    private void updateVariantTitles() {
        for (int i = 0; i < variantViews.size(); i++) {
            View variantView = variantViews.get(i);
            TextView tvVariantTitle = variantView.findViewById(R.id.tvVariantTitle);
            tvVariantTitle.setText("Variant " + (i + 1));
        }
    }

    private void submitProductForm() {
        if (!validateMainForm()) {
            return;
        }

        if (!validateVariants()) {
            return;
        }

        submitProductToServer();
    }

    private boolean validateMainForm() {
        String productName = etProductName.getText().toString().trim();
        String measurement = etMeasurement.getText().toString().trim();
        String price = etPrice.getText().toString().trim();
        String sku = etSKU.getText().toString().trim();
        String hsnCode = etHSNCode.getText().toString().trim();
        String stock = etStock.getText().toString().trim();
        String manufacturer = etManufacturer.getText().toString().trim();
        String madeIn = etMadeIn.getText().toString().trim();
        String productDescription = etProductDescription.getText().toString().trim();
        String shippingPolicy = etShippingPolicy.getText().toString().trim();
        String fssaiNo = etFSSAINo.getText().toString().trim();
        String tax = etTax.getText().toString().trim();

        if (productName.isEmpty()) {
            etProductName.setError("Product name is required");
            return false;
        }

        if (radioProductType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select product type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select category", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerSubcategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select subcategory", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (measurement.isEmpty()) {
            etMeasurement.setError("Measurement is required");
            return false;
        }

        if (spinnerMeasurementUnit.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select measurement unit", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (price.isEmpty()) {
            etPrice.setError("Price is required");
            return false;
        }

        if (sku.isEmpty()) {
            etSKU.setError("SKU is required");
            return false;
        }

        if (hsnCode.isEmpty()) {
            etHSNCode.setError("HSN Code is required");
            return false;
        }

        if (stock.isEmpty()) {
            etStock.setError("Stock is required");
            return false;
        }

        if (spinnerStockUnit.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select stock unit", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerDiscountType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select discount type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select status", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (manufacturer.isEmpty()) {
            etManufacturer.setError("Manufacturer is required");
            return false;
        }

        if (madeIn.isEmpty()) {
            etMadeIn.setError("Made in country is required");
            return false;
        }

        if (productDescription.isEmpty()) {
            etProductDescription.setError("Product description is required");
            return false;
        }

        if (shippingPolicy.isEmpty()) {
            etShippingPolicy.setError("Shipping policy is required");
            return false;
        }

        if (fssaiNo.isEmpty()) {
            etFSSAINo.setError("FSSAI number is required");
            return false;
        }

        if (tax.isEmpty()) {
            etTax.setError("Tax is required");
            return false;
        }

        if (productImageUri == null) {
            Toast.makeText(this, "Please select product image", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateVariants() {
        for (View variantView : variantViews) {
            TextInputEditText etVariantMeasurement = variantView.findViewById(R.id.etVariantMeasurement);
            TextInputEditText etVariantPrice = variantView.findViewById(R.id.etVariantPrice);
            TextInputEditText etVariantSKU = variantView.findViewById(R.id.etVariantSKU);
            TextInputEditText etVariantHSNCode = variantView.findViewById(R.id.etVariantHSNCode);
            TextInputEditText etVariantStock = variantView.findViewById(R.id.etVariantStock);

            Spinner spinnerVariantMeasurementUnit = variantView.findViewById(R.id.spinnerVariantMeasurementUnit);
            Spinner spinnerVariantStockUnit = variantView.findViewById(R.id.spinnerVariantStockUnit);
            Spinner spinnerVariantDiscountType = variantView.findViewById(R.id.spinnerVariantDiscountType);
            Spinner spinnerVariantStatus = variantView.findViewById(R.id.spinnerVariantStatus);

            if (etVariantMeasurement.getText().toString().trim().isEmpty()) {
                etVariantMeasurement.setError("Measurement is required");
                return false;
            }

            if (spinnerVariantMeasurementUnit.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select measurement unit for variant", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (etVariantPrice.getText().toString().trim().isEmpty()) {
                etVariantPrice.setError("Price is required");
                return false;
            }

            if (etVariantSKU.getText().toString().trim().isEmpty()) {
                etVariantSKU.setError("SKU is required");
                return false;
            }

            if (etVariantHSNCode.getText().toString().trim().isEmpty()) {
                etVariantHSNCode.setError("HSN Code is required");
                return false;
            }

            if (etVariantStock.getText().toString().trim().isEmpty()) {
                etVariantStock.setError("Stock is required");
                return false;
            }

            if (spinnerVariantStockUnit.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select stock unit for variant", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (spinnerVariantDiscountType.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select discount type for variant", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (spinnerVariantStatus.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select status for variant", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void submitProductToServer() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Adding product...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        try {
            // Get main product data
            String productName = etProductName.getText().toString().trim();
            String productType = radioPacked.isChecked() ? "Packed" : "Loose";
            String measurement = etMeasurement.getText().toString().trim();
            String measurementUnit = spinnerMeasurementUnit.getSelectedItem().toString();
            String price = etPrice.getText().toString().trim();
            String sku = etSKU.getText().toString().trim();
            String hsnCode = etHSNCode.getText().toString().trim();
            String stock = etStock.getText().toString().trim();
            String stockUnit = spinnerStockUnit.getSelectedItem().toString();
            String discountType = spinnerDiscountType.getSelectedItem().toString();
            String discountPrice = etDiscountPrice.getText().toString().trim();
            String status = spinnerStatus.getSelectedItem().toString();

            // Get additional fields
            String manufacturer = etManufacturer.getText().toString().trim();
            String madeIn = etMadeIn.getText().toString().trim();
            String productDescription = etProductDescription.getText().toString().trim();
            String shippingPolicy = etShippingPolicy.getText().toString().trim();
            String fssaiNo = etFSSAINo.getText().toString().trim();
            String tax = etTax.getText().toString().trim();

            // Get category data
            Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
            Subcategory selectedSubcategory = (Subcategory) spinnerSubcategory.getSelectedItem();

            String categoryId = selectedCategory.getId();
            String subcategoryId = selectedSubcategory.getId();

            // Get checkbox values
            int returnable = cbReturnable.isChecked() ? 1 : 0;
            int cancelable = cbCancelable.isChecked() ? 1 : 0;
            int codAllowed = cbCodAllowed.isChecked() ? 1 : 0;

            // Create multipart request
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("product_name", productName)
                    .addFormDataPart("qtytype", measurementUnit)
                    .addFormDataPart("tax", tax)
                    .addFormDataPart("fssai_no", fssaiNo)
                    .addFormDataPart("category", categoryId)
                    .addFormDataPart("sub_category", subcategoryId)
                    .addFormDataPart("productType", productType)
                    .addFormDataPart("manufacturer", manufacturer)
                    .addFormDataPart("made_in", madeIn)
                    .addFormDataPart("returnable", String.valueOf(returnable))
                    .addFormDataPart("cancelable", String.valueOf(cancelable))
                    .addFormDataPart("cod_allowed", String.valueOf(codAllowed))
                    .addFormDataPart("total_quantity", stock)
                    .addFormDataPart("product_description", productDescription)
                    .addFormDataPart("shippingPolicy", shippingPolicy)
                    .addFormDataPart("price", price)
                    .addFormDataPart("sku", sku)
                    .addFormDataPart("hsn_code", hsnCode)
                    .addFormDataPart("discount_type", discountType)
                    .addFormDataPart("discount_price", discountPrice)
                    .addFormDataPart("status", status);

            // Add main image
            if (productImageUri != null) {
                File imageFile = new File(getRealPathFromURI(productImageUri));
                if (imageFile.exists()) {
                    multipartBuilder.addFormDataPart("main_image", imageFile.getName(),
                            RequestBody.create(MediaType.parse("image/*"), imageFile));
                }
            }

            // Add variants as arrays
            if (!variantViews.isEmpty()) {
                addVariantsAsArrays(multipartBuilder);
            }

            RequestBody requestBody = multipartBuilder.build();

            Request request = new Request.Builder()
                    .url(Attributes.Main_Url + "shop/" + shopId() + "/addproduct")
                    .post(requestBody)
                    .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                    .build();

            OkHttpClient client = new OkHttpClient();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(AddProductActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("SUBMIT_ERROR", "Request failed: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("SUBMIT_RESPONSE", responseBody);

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            if (response.isSuccessful() && "success".equals(jsonResponse.optString("status"))) {
                                Toast.makeText(AddProductActivity.this, "Product added successfully!", Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                String errorMessage = jsonResponse.optString("message", "Failed to add product");
                                Toast.makeText(AddProductActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Error parsing response: " + e.getMessage());
                            Toast.makeText(AddProductActivity.this, "Error parsing server response", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Error submitting product: " + e.getMessage(), e);
            Toast.makeText(this, "Error submitting product: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addVariantsAsArrays(MultipartBody.Builder multipartBuilder) {
        try {
            List<String> measurements = new ArrayList<>();
            List<String> measureUnits = new ArrayList<>();
            List<String> prices = new ArrayList<>();
            List<String> discountTypes = new ArrayList<>();
            List<String> discountPrices = new ArrayList<>();
            List<String> stocks = new ArrayList<>();
            List<String> stockUnits = new ArrayList<>();
            List<String> statuses = new ArrayList<>();
            List<String> skuCodes = new ArrayList<>();
            List<String> hsnCodes = new ArrayList<>();
            List<String> variantExists = new ArrayList<>(); // Empty for new variants

            for (View variantView : variantViews) {
                TextInputEditText etVariantMeasurement = variantView.findViewById(R.id.etVariantMeasurement);
                TextInputEditText etVariantPrice = variantView.findViewById(R.id.etVariantPrice);
                TextInputEditText etVariantSKU = variantView.findViewById(R.id.etVariantSKU);
                TextInputEditText etVariantHSNCode = variantView.findViewById(R.id.etVariantHSNCode);
                TextInputEditText etVariantStock = variantView.findViewById(R.id.etVariantStock);
                TextInputEditText etVariantDiscountPrice = variantView.findViewById(R.id.etVariantDiscountPrice);

                Spinner spinnerVariantMeasurementUnit = variantView.findViewById(R.id.spinnerVariantMeasurementUnit);
                Spinner spinnerVariantStockUnit = variantView.findViewById(R.id.spinnerVariantStockUnit);
                Spinner spinnerVariantDiscountType = variantView.findViewById(R.id.spinnerVariantDiscountType);
                Spinner spinnerVariantStatus = variantView.findViewById(R.id.spinnerVariantStatus);

                // Get values from form
                String measurement = etVariantMeasurement.getText().toString().trim();
                String measureUnit = spinnerVariantMeasurementUnit.getSelectedItem().toString();
                String price = etVariantPrice.getText().toString().trim();
                String skuCode = etVariantSKU.getText().toString().trim();
                String hsnCode = etVariantHSNCode.getText().toString().trim();
                String stock = etVariantStock.getText().toString().trim();
                String stockUnit = spinnerVariantStockUnit.getSelectedItem().toString();
                String discountType = getDiscountTypeValue(spinnerVariantDiscountType.getSelectedItem().toString());
                String discountPrice = etVariantDiscountPrice.getText().toString().trim();
                String status = getStatusValue(spinnerVariantStatus.getSelectedItem().toString());

                // Add to arrays
                measurements.add(measurement);
                measureUnits.add(measureUnit);
                prices.add(price);
                discountTypes.add(discountType);
                discountPrices.add(discountPrice);
                stocks.add(stock);
                stockUnits.add(stockUnit);
                statuses.add(status);
                skuCodes.add(skuCode);
                hsnCodes.add(hsnCode);
                variantExists.add(""); // Empty for new variants
            }

            // Add arrays to multipart builder
            for (int i = 0; i < measurements.size(); i++) {
                multipartBuilder.addFormDataPart("measurement[]", measurements.get(i));
                multipartBuilder.addFormDataPart("measureUnit[]", measureUnits.get(i));
                multipartBuilder.addFormDataPart("prices[]", prices.get(i));
                multipartBuilder.addFormDataPart("discount_type[]", discountTypes.get(i));
                multipartBuilder.addFormDataPart("discount_price[]", discountPrices.get(i));
                multipartBuilder.addFormDataPart("stock[]", stocks.get(i));
                multipartBuilder.addFormDataPart("stock_unit[]", stockUnits.get(i));
                multipartBuilder.addFormDataPart("status[]", statuses.get(i));
                multipartBuilder.addFormDataPart("sku_code[]", skuCodes.get(i));
                multipartBuilder.addFormDataPart("hsn_code[]", hsnCodes.get(i));
                multipartBuilder.addFormDataPart("variantexist[]", variantExists.get(i));
            }

        } catch (Exception e) {
            Log.e(TAG, "Error adding variants as arrays: " + e.getMessage(), e);
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

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return contentUri.getPath();
    }
}