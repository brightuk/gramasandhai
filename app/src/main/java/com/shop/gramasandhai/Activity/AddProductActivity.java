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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import okhttp3.*;

public class AddProductActivity extends AppCompatActivity {

    private static final String TAG = "AddProductActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Draft constants
    private static final String DRAFT_PREF_NAME = "ProductDrafts";
    private static final String DRAFT_LIST_KEY = "product_drafts_list";

    // Main form fields
    private TextInputEditText etProductName, etMeasurement, etPrice, etSKU, etHSNCode, etStock, etDiscountPrice;
    private RadioGroup radioProductType, radioVariantControl;
    private RadioButton radioPacked, radioLoose, radioVariantYes, radioVariantNo;
    private Spinner spinnerMeasurementUnit, spinnerDiscountType, spinnerStatus;
    private Spinner spinnerCategory, spinnerSubcategory;
    private Button btnSelectImage, btnAddVariant, btnSubmit;
    private LinearLayout containerVariants;
    private ImageView ivProductImagePreview;
    private TextView tvNoImage;

    // Progress bar elements
    private FrameLayout progressOverlay;
    private LinearLayout mainContent;

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

    // Draft helper class
    private static class DraftItem {
        String draftId;
        String displayText;

        DraftItem(String draftId, String displayText) {
            this.draftId = draftId;
            this.displayText = displayText;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "onCreate started");

            // Set content view first
            setContentView(R.layout.activity_add_product);
            Log.d(TAG, "setContentView completed");

            // Initialize progress bar
            initializeProgressBar();
            if (new Random().nextInt(10) == 0) { // Run approximately 10% of the time
                cleanupOldProductImages();
            }

            // Initialize views
            Log.d(TAG, "Initializing views...");
            initializeViews();
            setupToolbar();

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

    private void initializeProgressBar() {
        progressOverlay = findViewById(R.id.progressOverlay);
        mainContent = findViewById(R.id.mainContent);
    }

    private void showProgress() {
        runOnUiThread(() -> {
            progressOverlay.setVisibility(View.VISIBLE);
            mainContent.setAlpha(0.5f);
            mainContent.setEnabled(false);
        });
    }

    private void hideProgress() {
        runOnUiThread(() -> {
            progressOverlay.setVisibility(View.GONE);
            mainContent.setAlpha(1.0f);
            mainContent.setEnabled(true);
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_product, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save_draft) {
            saveAsDraft();
            return true;
        } else if (id == R.id.action_load_draft) {
            showDraftsList();
            return true;
        } else if (id == R.id.action_delete_drafts) {
            showBulkDeleteOptions();
            return true;
        }

        return super.onOptionsItemSelected(item);
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

            // Radio Buttons
            radioProductType = findViewById(R.id.radioProductType);
            radioPacked = findViewById(R.id.radioPacked);
            radioLoose = findViewById(R.id.radioLoose);

            // Variant Control Radio Group
            radioVariantControl = findViewById(R.id.radioVariantControl);
            radioVariantYes = findViewById(R.id.radioVariantYes);
            radioVariantNo = findViewById(R.id.radioVariantNo);

            // Spinners
            spinnerMeasurementUnit = findViewById(R.id.spinnerMeasurementUnit);
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
            tvNoImage = findViewById(R.id.tvNoImage);

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

        showProgress();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/addproduct")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideProgress();
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
                    hideProgress();
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

        // Variant control radio group listener
        radioVariantControl.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioVariantYes) {
                showVariantSection();
            } else if (checkedId == R.id.radioVariantNo) {
                hideVariantSection();
            }
        });

        // Initially hide variant section since "No" is checked by default
        hideVariantSection();
    }

    private void showVariantSection() {
        try {
            // Find the variants card by looking for the parent of containerVariants
            View variantsContainer = findViewById(R.id.containerVariants);
            if (variantsContainer != null && variantsContainer.getParent() != null) {
                View variantsCard = (View) variantsContainer.getParent();
                if (variantsCard.getParent() != null) {
                    View variantsSection = (View) variantsCard.getParent();
                    variantsSection.setVisibility(View.VISIBLE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing variant section: " + e.getMessage());
            // Fallback: try to find by ID if you added one
            try {
                View variantsSection = findViewById(R.id.cardVariantsSection);
                if (variantsSection != null) {
                    variantsSection.setVisibility(View.VISIBLE);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error finding variants section by ID: " + ex.getMessage());
            }
        }

        // Enable add variant button
        btnAddVariant.setEnabled(true);
        btnAddVariant.setAlpha(1.0f);
    }

    private void hideVariantSection() {
        try {
            // Find the variants card by looking for the parent of containerVariants
            View variantsContainer = findViewById(R.id.containerVariants);
            if (variantsContainer != null && variantsContainer.getParent() != null) {
                View variantsCard = (View) variantsContainer.getParent();
                if (variantsCard.getParent() != null) {
                    View variantsSection = (View) variantsCard.getParent();
                    variantsSection.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding variant section: " + e.getMessage());
            // Fallback: try to find by ID if you added one
            try {
                View variantsSection = findViewById(R.id.cardVariantsSection);
                if (variantsSection != null) {
                    variantsSection.setVisibility(View.GONE);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error finding variants section by ID: " + ex.getMessage());
            }
        }

        // Disable add variant button
        btnAddVariant.setEnabled(false);
        btnAddVariant.setAlpha(0.5f);

        // Clear all existing variants
        clearAllVariants();
    }

    private void clearAllVariants() {
        containerVariants.removeAllViews();
        variantViews.clear();
        variantCount = 0;
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
        String imageFileName = "PRODUCT_" + timeStamp + ".jpg";

        // Use app's external files directory for persistent storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // Create the directory if it doesn't exist
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }

        File imageFile = new File(storageDir, imageFileName);
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
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
                        // Use the new method that copies the image
                        handleGalleryImage(data.getData());
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

    private void handleGalleryImage(Uri selectedImageUri) {
        try {
            // Copy the gallery image to app storage
            File copiedImageFile = copyImageToAppStorage(selectedImageUri);
            if (copiedImageFile != null && copiedImageFile.exists()) {
                productImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider", copiedImageFile);
                currentPhotoPath = copiedImageFile.getAbsolutePath();
                displaySelectedImage();
            } else {
                throw new IOException("Failed to copy image to app storage");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling gallery image: " + e.getMessage());
            Toast.makeText(this, "Error processing selected image", Toast.LENGTH_SHORT).show();
            // Fallback to original URI
            productImageUri = selectedImageUri;
            displaySelectedImage();
        }
    }

    private File copyImageToAppStorage(Uri sourceUri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(sourceUri);
        if (inputStream == null) {
            throw new IOException("Cannot open input stream from URI");
        }

        // Create a file in app's internal storage
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "GALLERY_" + timeStamp + ".jpg";
        File destinationFile = new File(storageDir, imageFileName);

        FileOutputStream outputStream = new FileOutputStream(destinationFile);

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();

        return destinationFile;
    }

    private void displaySelectedImage() {
        runOnUiThread(() -> {
            try {
                if (productImageUri != null) {
                    ivProductImagePreview.setImageURI(productImageUri);
                    ivProductImagePreview.setVisibility(View.VISIBLE);
                    tvNoImage.setVisibility(View.GONE);
                    btnSelectImage.setText("Change Image");
                } else {
                    ivProductImagePreview.setVisibility(View.GONE);
                    tvNoImage.setVisibility(View.VISIBLE);
                    btnSelectImage.setText("Select Product Image");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error displaying selected image: " + e.getMessage());
                // Reset image state on error
                productImageUri = null;
                currentPhotoPath = null;
                ivProductImagePreview.setVisibility(View.GONE);
                tvNoImage.setVisibility(View.VISIBLE);
                btnSelectImage.setText("Select Product Image");
            }
        });
    }

    private void addVariantForm() {
        try {
            // Check if variant section is enabled
            if (!radioVariantYes.isChecked()) {
                Toast.makeText(this, "Please enable variants first", Toast.LENGTH_SHORT).show();
                return;
            }

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
        // First validate the main form
        if (!validateMainForm()) {
            return;
        }

        // Get variant control selection
        int selectedVariantId = radioVariantControl.getCheckedRadioButtonId();

        if (selectedVariantId == R.id.radioVariantYes) {
            // If variants are enabled, validate them
            if (!validateVariants()) {
                return;
            }
        } else if (selectedVariantId == R.id.radioVariantNo) {
            // If variants are disabled, make sure no variants are added
            if (!variantViews.isEmpty()) {
                Toast.makeText(this, "Please remove all variants or enable variants", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // This should not happen due to validation, but just in case
            Toast.makeText(this, "Please select variant option", Toast.LENGTH_SHORT).show();
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

        if (productName.isEmpty()) {
            etProductName.setError("Product name is required");
            return false;
        }

        if (radioProductType.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select product type", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Enhanced variant control validation
        if (radioVariantControl.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select if you want to add variants", Toast.LENGTH_SHORT).show();
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

        if (spinnerDiscountType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select discount type", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (spinnerStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select status", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (productImageUri == null) {
            Toast.makeText(this, "Please select product image", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateVariants() {
        // Check if variants are enabled
        if (!radioVariantYes.isChecked()) {
            return true; // No need to validate if variants are disabled
        }

        // Check if at least one variant is added when "Yes" is selected
        if (variantViews.isEmpty()) {
            Toast.makeText(this, "Please add at least one variant when variants are enabled", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate each variant
        for (int i = 0; i < variantViews.size(); i++) {
            View variantView = variantViews.get(i);
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
                etVariantMeasurement.setError("Measurement is required for variant " + (i + 1));
                return false;
            }

            if (spinnerVariantMeasurementUnit.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select measurement unit for variant " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (etVariantPrice.getText().toString().trim().isEmpty()) {
                etVariantPrice.setError("Price is required for variant " + (i + 1));
                return false;
            }

            if (spinnerVariantStockUnit.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select stock unit for variant " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (spinnerVariantDiscountType.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select discount type for variant " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }

            if (spinnerVariantStatus.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select status for variant " + (i + 1), Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void submitProductToServer() {
        showProgress();

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
            String discountType = getDiscountTypeValue(spinnerDiscountType.getSelectedItem().toString());
            String discountPrice = etDiscountPrice.getText().toString().trim();
            String status = spinnerStatus.getSelectedItem().toString();

            // Get category data
            Category selectedCategory = (Category) spinnerCategory.getSelectedItem();
            Subcategory selectedSubcategory = (Subcategory) spinnerSubcategory.getSelectedItem();

            String categoryId = selectedCategory.getId();
            String subcategoryId = selectedSubcategory.getId();

            // Create multipart request
            MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("product_name", productName)
                    .addFormDataPart("measureUnit", measurementUnit)
                    .addFormDataPart("category", categoryId)
                    .addFormDataPart("sub_category", subcategoryId)
                    .addFormDataPart("productType", productType)
                    .addFormDataPart("measurement", measurement)
                    .addFormDataPart("stocks", stock)
                    .addFormDataPart("prices", price)
                    .addFormDataPart("sku_code", sku)
                    .addFormDataPart("hsn_code", hsnCode)
                    .addFormDataPart("discount_type", discountType)
                    .addFormDataPart("discount_price", discountPrice)
                    .addFormDataPart("status", status);


            Log.d("discount_type",discountType);

            // Add variant control parameter
            String variantControl = radioVariantYes.isChecked() ? "1" : "0";
            multipartBuilder.addFormDataPart("is_variant", variantControl);

            // Add main image
            if (productImageUri != null) {
                File imageFile = getImageFileFromUri(productImageUri);
                if (imageFile != null && imageFile.exists()) {
                    multipartBuilder.addFormDataPart("main_image", imageFile.getName(),
                            RequestBody.create(MediaType.parse("image/*"), imageFile));
                } else {
                    Log.e(TAG, "Image file not found or inaccessible: " + productImageUri);
                    Toast.makeText(this, "Image file not accessible. Please select image again.", Toast.LENGTH_LONG).show();
                    hideProgress();
                    return;
                }
            }

            // Add variants as arrays only if "Yes" is selected and variants exist
            if (radioVariantYes.isChecked() && !variantViews.isEmpty()) {
                addVariantsAsArrays(multipartBuilder);
            }

            RequestBody requestBody = multipartBuilder.build();

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(Attributes.Main_Url + "shop/" + shopId() + "/addproduct")
                    .post(requestBody)
                    .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        hideProgress();
                        Toast.makeText(AddProductActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e("SUBMIT_ERROR", "Request failed: " + e.getMessage());
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d("SUBMIT_RESPONSE", responseBody);

                    runOnUiThread(() -> {
                        hideProgress();
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
            hideProgress();
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
                String measurement = safeGetText(etVariantMeasurement);
                String measureUnit = spinnerVariantMeasurementUnit.getSelectedItem().toString();
                String price = safeGetText(etVariantPrice);
                String skuCode = safeGetText(etVariantSKU);
                String hsnCode = safeGetText(etVariantHSNCode);
                String stock = safeGetText(etVariantStock);
                String stockUnit = spinnerVariantStockUnit.getSelectedItem().toString();
                String discountType = getDiscountTypeValue(spinnerVariantDiscountType.getSelectedItem().toString());
                String discountPrice = safeGetText(etVariantDiscountPrice);
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

    private String safeGetText(TextInputEditText editText) {
        if (editText != null && editText.getText() != null) {
            return editText.getText().toString().trim();
        }
        return "";
    }

    // ==================== FIXED DRAFT FUNCTIONALITY ====================

    private void saveAsDraft() {
        try {
            if (!validateDraftData()) {
                return;
            }

            JSONObject draftData = collectFormData();

            // Generate a unique draft ID
            String draftId = "product_draft_" + System.currentTimeMillis();

            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save the draft data
            editor.putString(draftId, draftData.toString());

            // Also save a list of draft IDs
            String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");
            Set<String> draftSet = new HashSet<>();
            if (!existingDrafts.isEmpty()) {
                String[] draftsArray = existingDrafts.split(",");
                Collections.addAll(draftSet, draftsArray);
            }
            draftSet.add(draftId);

            String updatedDrafts = String.join(",", draftSet);
            editor.putString(DRAFT_LIST_KEY, updatedDrafts);

            // Save timestamp
            editor.putLong(draftId + "_timestamp", System.currentTimeMillis());

            if (editor.commit()) {
                showDraftSuccessDialog(draftId);
            } else {
                Toast.makeText(this, "Failed to save draft", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving draft: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateDraftData() {
        // For drafts, we can be more lenient than final submission
        String productName = etProductName.getText().toString().trim();
        String measurement = etMeasurement.getText().toString().trim();
        String price = etPrice.getText().toString().trim();

        if (productName.isEmpty() && measurement.isEmpty() && price.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Empty Draft")
                    .setMessage("You haven't entered any product information. Do you want to save an empty draft?")
                    .setPositiveButton("Save Anyway", (dialog, which) -> {
                        // Continue with empty draft
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return false;
        }

        return true;
    }

    private void showDraftSuccessDialog(String draftId) {
        new AlertDialog.Builder(this)
                .setTitle("Draft Saved")
                .setMessage("Your product has been saved as draft. You can continue editing later from the drafts section.")
                .setPositiveButton("Continue Editing", null)
                .setNegativeButton("OK", null)
                .setNeutralButton("View Drafts", (dialog, which) -> showDraftsList())
                .show();
    }

    private void showDraftsList() {
        try {
            SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
            String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");

            if (existingDrafts.isEmpty()) {
                Toast.makeText(this, "No drafts found", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] draftIds = existingDrafts.split(",");
            List<DraftItem> draftItems = new ArrayList<>();

            for (String draftId : draftIds) {
                String draftJson = prefs.getString(draftId, "");
                if (!draftJson.isEmpty()) {
                    try {
                        JSONObject draftData = new JSONObject(draftJson);
                        String productName = draftData.optString("product_name", "Unnamed Product");
                        long timestamp = draftData.optLong("created_at", System.currentTimeMillis());

                        String time = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                                .format(new Date(timestamp));

                        String displayText = productName.equals("Unnamed Product") ?
                                "Draft (" + time + ")" : productName + " (" + time + ")";

                        draftItems.add(new DraftItem(draftId, displayText));

                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing draft: " + e.getMessage());
                    }
                }
            }

            if (draftItems.isEmpty()) {
                Toast.makeText(this, "No valid drafts found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create array of display strings
            String[] draftDisplayItems = new String[draftItems.size()];
            for (int i = 0; i < draftItems.size(); i++) {
                draftDisplayItems[i] = draftItems.get(i).displayText;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Saved Drafts")
                    .setItems(draftDisplayItems, (dialog, which) -> {
                        DraftItem selectedDraft = draftItems.get(which);
                        showDraftOptions(selectedDraft.draftId);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing drafts list: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading drafts", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDraftOptions(String draftId) {
        new AlertDialog.Builder(this)
                .setTitle("Draft Options")
                .setItems(new CharSequence[]{"Load Draft", "Delete Draft", "Cancel"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Load
                            new AlertDialog.Builder(this)
                                    .setTitle("Load Draft")
                                    .setMessage("This will replace your current form data. Continue?")
                                    .setPositiveButton("Load", (d, w) -> {
                                        try {
                                            loadDraft(draftId);
                                        } catch (Exception e) {
                                            Log.e(TAG, "Error loading draft: " + e.getMessage(), e);
                                            Toast.makeText(AddProductActivity.this, "Error loading draft", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                            break;
                        case 1: // Delete
                            showDeleteConfirmation(draftId);
                            break;
                        case 2: // Cancel
                            // Do nothing
                            break;
                    }
                })
                .show();
    }

    private void loadDraft(String draftId) {
        try {
            SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
            String draftJson = prefs.getString(draftId, "");

            if (draftJson == null || draftJson.isEmpty()) {
                Toast.makeText(this, "Draft not found", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject draftData = new JSONObject(draftJson);

            // Clear current form first
            clearForm();

            // Restore basic fields
            etProductName.setText(draftData.optString("product_name", ""));
            etMeasurement.setText(draftData.optString("measurement", ""));
            etPrice.setText(draftData.optString("price", ""));
            etSKU.setText(draftData.optString("sku", ""));
            etHSNCode.setText(draftData.optString("hsn_code", ""));
            etStock.setText(draftData.optString("stock", ""));
            etDiscountPrice.setText(draftData.optString("discount_price", ""));

            // Restore spinners - with safety checks
            safeSetSpinnerSelection(spinnerMeasurementUnit, draftData.optInt("measurement_unit_position", 0));
            safeSetSpinnerSelection(spinnerDiscountType, draftData.optInt("discount_type_position", 0));
            safeSetSpinnerSelection(spinnerStatus, draftData.optInt("status_position", 0));
            safeSetSpinnerSelection(spinnerCategory, draftData.optInt("category_position", 0));
            safeSetSpinnerSelection(spinnerSubcategory, draftData.optInt("subcategory_position", 0));

            // Restore product type
            String productType = draftData.optString("product_type", "Packed");
            if (productType.equals("Packed")) {
                radioPacked.setChecked(true);
            } else {
                radioLoose.setChecked(true);
            }

            // Restore variant control
            String variantControl = draftData.optString("variant_control", "no");
            if (variantControl.equals("yes")) {
                radioVariantYes.setChecked(true);
                showVariantSection();

                // Load variants if they exist
                if (draftData.has("variants")) {
                    JSONArray variantsArray = draftData.getJSONArray("variants");
                    for (int i = 0; i < variantsArray.length(); i++) {
                        // Add variant form first
                        addVariantForm();

                        // Make sure we have enough variant views
                        if (i < variantViews.size()) {
                            View variantView = variantViews.get(i);
                            JSONObject variantData = variantsArray.getJSONObject(i);

                            // Safely set variant fields
                            setVariantField(variantView, R.id.etVariantMeasurement, variantData.optString("measurement", ""));
                            setVariantField(variantView, R.id.etVariantPrice, variantData.optString("price", ""));
                            setVariantField(variantView, R.id.etVariantSKU, variantData.optString("sku", ""));
                            setVariantField(variantView, R.id.etVariantHSNCode, variantData.optString("hsn_code", ""));
                            setVariantField(variantView, R.id.etVariantStock, variantData.optString("stock", ""));
                            setVariantField(variantView, R.id.etVariantDiscountPrice, variantData.optString("discount_price", ""));

                            // Safely set variant spinners
                            safeSetVariantSpinner(variantView, R.id.spinnerVariantMeasurementUnit, variantData.optInt("measurement_unit_position", 0));
                            safeSetVariantSpinner(variantView, R.id.spinnerVariantStockUnit, variantData.optInt("stock_unit_position", 0));
                            safeSetVariantSpinner(variantView, R.id.spinnerVariantDiscountType, variantData.optInt("discount_type_position", 0));
                            safeSetVariantSpinner(variantView, R.id.spinnerVariantStatus, variantData.optInt("status_position", 0));
                        }
                    }
                }
            } else {
                radioVariantNo.setChecked(true);
                hideVariantSection();
            }

            // Handle image loading
            if (draftData.has("image_path")) {
                String imagePath = draftData.getString("image_path");
                Log.d(TAG, "Attempting to load image from path: " + imagePath);

                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        try {
                            productImageUri = FileProvider.getUriForFile(this,
                                    getApplicationContext().getPackageName() + ".provider", imageFile);
                            currentPhotoPath = imagePath;
                            displaySelectedImage();
                            Log.d(TAG, "Image loaded successfully from draft");
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading image from draft path: " + e.getMessage());
                            // Continue without image
                        }
                    } else {
                        Log.w(TAG, "Image file not found at saved path: " + imagePath);
                    }
                }
            }

            Toast.makeText(this, "Draft loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error loading draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading draft: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private JSONObject collectFormData() throws JSONException {
        JSONObject draftData = new JSONObject();

        // Basic product info
        draftData.put("created_at", System.currentTimeMillis());

        // Form fields
        draftData.put("product_name", etProductName.getText().toString().trim());
        draftData.put("product_type", radioPacked.isChecked() ? "Packed" : "Loose");
        draftData.put("measurement", etMeasurement.getText().toString().trim());
        draftData.put("measurement_unit_position", spinnerMeasurementUnit.getSelectedItemPosition());
        draftData.put("price", etPrice.getText().toString().trim());
        draftData.put("sku", etSKU.getText().toString().trim());
        draftData.put("hsn_code", etHSNCode.getText().toString().trim());
        draftData.put("stock", etStock.getText().toString().trim());
        draftData.put("discount_price", etDiscountPrice.getText().toString().trim());
        draftData.put("discount_type_position", spinnerDiscountType.getSelectedItemPosition());
        draftData.put("status_position", spinnerStatus.getSelectedItemPosition());

        // Category data
        draftData.put("category_position", spinnerCategory.getSelectedItemPosition());
        draftData.put("subcategory_position", spinnerSubcategory.getSelectedItemPosition());

        // Variant control
        draftData.put("variant_control", radioVariantYes.isChecked() ? "yes" : "no");

        // Variants data
        if (radioVariantYes.isChecked() && !variantViews.isEmpty()) {
            JSONArray variantsArray = new JSONArray();
            for (View variantView : variantViews) {
                JSONObject variantData = new JSONObject();

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

                variantData.put("measurement", safeGetText(etVariantMeasurement));
                variantData.put("price", safeGetText(etVariantPrice));
                variantData.put("sku", safeGetText(etVariantSKU));
                variantData.put("hsn_code", safeGetText(etVariantHSNCode));
                variantData.put("stock", safeGetText(etVariantStock));
                variantData.put("discount_price", safeGetText(etVariantDiscountPrice));

                variantData.put("measurement_unit_position", spinnerVariantMeasurementUnit.getSelectedItemPosition());
                variantData.put("stock_unit_position", spinnerVariantStockUnit.getSelectedItemPosition());
                variantData.put("discount_type_position", spinnerVariantDiscountType.getSelectedItemPosition());
                variantData.put("status_position", spinnerVariantStatus.getSelectedItemPosition());

                variantsArray.put(variantData);
            }
            draftData.put("variants", variantsArray);
        }

        // Image handling - Only save if we have a valid path
        if (productImageUri != null && currentPhotoPath != null) {
            File imageFile = new File(currentPhotoPath);
            if (imageFile.exists()) {
                draftData.put("image_path", currentPhotoPath);
                Log.d(TAG, "Saved image path: " + currentPhotoPath);
            } else {
                Log.w(TAG, "Image file doesn't exist, not saving to draft: " + currentPhotoPath);
            }
        }

        return draftData;
    }

    // Improved spinner selection with bounds checking
    private void safeSetSpinnerSelection(Spinner spinner, int position) {
        try {
            if (spinner != null && spinner.getAdapter() != null &&
                    position >= 0 && position < spinner.getAdapter().getCount()) {
                spinner.setSelection(position);
            } else {
                Log.w(TAG, "Invalid spinner position: " + position + " for spinner with count: " +
                        (spinner != null && spinner.getAdapter() != null ? spinner.getAdapter().getCount() : "null"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting spinner selection: " + e.getMessage());
        }
    }

    private void safeSetVariantSpinner(View variantView, int spinnerId, int position) {
        try {
            Spinner spinner = variantView.findViewById(spinnerId);
            safeSetSpinnerSelection(spinner, position);
        } catch (Exception e) {
            Log.e(TAG, "Error setting variant spinner: " + e.getMessage());
        }
    }

    private void setVariantField(View variantView, int fieldId, String value) {
        try {
            TextInputEditText field = variantView.findViewById(fieldId);
            if (field != null) {
                field.setText(value);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting variant field: " + e.getMessage());
        }
    }

    private void clearForm() {
        try {
            // Clear all fields
            etProductName.setText("");
            etMeasurement.setText("");
            etPrice.setText("");
            etSKU.setText("");
            etHSNCode.setText("");
            etStock.setText("");
            etDiscountPrice.setText("");

            // Reset spinners to first position safely
            safeSetSpinnerSelection(spinnerMeasurementUnit, 0);
            safeSetSpinnerSelection(spinnerDiscountType, 0);
            safeSetSpinnerSelection(spinnerStatus, 0);
            safeSetSpinnerSelection(spinnerCategory, 0);
            safeSetSpinnerSelection(spinnerSubcategory, 0);

            // Reset radio buttons
            radioPacked.setChecked(true);
            radioVariantNo.setChecked(true);

            // Clear image
            productImageUri = null;
            currentPhotoPath = null;
            displaySelectedImage();

            // Clear variants
            clearAllVariants();
            hideVariantSection();

        } catch (Exception e) {
            Log.e(TAG, "Error clearing form: " + e.getMessage(), e);
        }
    }

    private void showDeleteConfirmation(String draftId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Draft")
                .setMessage("Are you sure you want to delete this draft? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteDraft(draftId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteDraft(String draftId) {
        try {
            SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Remove the draft data
            editor.remove(draftId);

            // Remove the timestamp
            editor.remove(draftId + "_timestamp");

            // Remove from draft list
            String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");
            if (!existingDrafts.isEmpty()) {
                Set<String> draftSet = new HashSet<>();
                Collections.addAll(draftSet, existingDrafts.split(","));
                draftSet.remove(draftId);

                String updatedDrafts = String.join(",", draftSet);
                if (updatedDrafts.isEmpty()) {
                    editor.remove(DRAFT_LIST_KEY);
                } else {
                    editor.putString(DRAFT_LIST_KEY, updatedDrafts);
                }
            }

            if (editor.commit()) {
                Toast.makeText(this, "Draft deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to delete draft", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error deleting draft", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBulkDeleteOptions() {
        SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
        String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");

        if (existingDrafts.isEmpty()) {
            Toast.makeText(this, "No drafts found", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Drafts")
                .setItems(new CharSequence[]{
                        "Delete All Drafts",
                        "Delete Old Drafts (older than 1 week)",
                        "Cancel"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showDeleteAllConfirmation();
                            break;
                        case 1:
                            deleteOldDrafts();
                            break;
                        case 2:
                            // Cancel
                            break;
                    }
                })
                .show();
    }

    private void showDeleteAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete All Drafts")
                .setMessage("Are you sure you want to delete ALL drafts? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllDrafts())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllDrafts() {
        try {
            SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");

            if (!existingDrafts.isEmpty()) {
                String[] draftIds = existingDrafts.split(",");
                int deletedCount = 0;

                for (String draftId : draftIds) {
                    editor.remove(draftId);
                    editor.remove(draftId + "_timestamp");
                    deletedCount++;
                }

                editor.remove(DRAFT_LIST_KEY);

                if (editor.commit()) {
                    Toast.makeText(this, "Deleted " + deletedCount + " draft(s)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete drafts", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No drafts found to delete", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting all drafts: " + e.getMessage(), e);
            Toast.makeText(this, "Error deleting drafts", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteOldDrafts() {
        int deletedCount = clearOldDrafts();
        if (deletedCount > 0) {
            Toast.makeText(this, "Deleted " + deletedCount + " old draft(s)", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No old drafts found", Toast.LENGTH_SHORT).show();
        }
    }

    private int clearOldDrafts() {
        SharedPreferences prefs = getSharedPreferences(DRAFT_PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        long oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 1 week ago
        int deletedCount = 0;

        // Get all keys
        Map<String, ?> allEntries = prefs.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            if (entry.getKey().endsWith("_timestamp")) {
                long timestamp = prefs.getLong(entry.getKey(), 0);
                if (timestamp < oneWeekAgo) {
                    String draftId = entry.getKey().replace("_timestamp", "");
                    editor.remove(draftId);
                    editor.remove(entry.getKey());
                    deletedCount++;

                    // Also remove from draft list
                    String existingDrafts = prefs.getString(DRAFT_LIST_KEY, "");
                    if (existingDrafts.contains(draftId)) {
                        String updatedDrafts = existingDrafts.replace(draftId, "")
                                .replace(",,", ",")
                                .replaceAll("^,|,$", "");
                        if (updatedDrafts.isEmpty()) {
                            editor.remove(DRAFT_LIST_KEY);
                        } else {
                            editor.putString(DRAFT_LIST_KEY, updatedDrafts);
                        }
                    }
                }
            }
        }

        editor.apply();
        return deletedCount;
    }

    // Improved method to get image file from URI
    private File getImageFileFromUri(Uri uri) {
        if (uri == null) return null;

        try {
            String scheme = uri.getScheme();
            if (scheme != null && scheme.equals("file")) {
                // File URI
                return new File(uri.getPath());
            } else if (scheme != null && scheme.equals("content")) {
                // Content URI - try to get the file path
                String filePath = getRealPathFromURI(uri);
                if (filePath != null) {
                    return new File(filePath);
                } else {
                    // For content URIs that we can't resolve, create a temporary file
                    return createTempFileFromUri(uri);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting image file from URI: " + e.getMessage(), e);
        }
        return null;
    }

    // Create temporary file from content URI
    private File createTempFileFromUri(Uri uri) {
        try {
            android.content.ContentResolver resolver = getContentResolver();
            java.io.InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream != null) {
                File tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(tempFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();
                return tempFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file from URI: " + e.getMessage(), e);
        }
        return null;
    }

    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri == null) return null;

        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(column_index);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path from URI: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private void cleanupOldProductImages() {
        // Run this occasionally to clean up old product images
        new Thread(() -> {
            try {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                if (storageDir != null && storageDir.exists()) {
                    File[] files = storageDir.listFiles((dir, name) -> name.startsWith("PRODUCT_") || name.startsWith("GALLERY_"));
                    if (files != null) {
                        long oneMonthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days
                        int deletedCount = 0;
                        for (File file : files) {
                            if (file.lastModified() < oneMonthAgo) {
                                if (file.delete()) {
                                    deletedCount++;
                                }
                            }
                        }
                        Log.d(TAG, "Cleaned up " + deletedCount + " old product images");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up old images: " + e.getMessage());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up temporary camera file if exists
        // Note: We're not deleting files immediately as they might be needed for drafts
    }
}