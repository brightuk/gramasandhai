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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

        // Check for drafts and offer to load
        checkForExistingDrafts();
    }

    private void checkForExistingDrafts() {
        SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
        String draftListKey = "drafts_for_product_" + productId;
        String existingDrafts = prefs.getString(draftListKey, "");

        if (!existingDrafts.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Found Saved Drafts")
                    .setMessage("You have saved drafts for this product. Would you like to load one?")
                    .setPositiveButton("View Drafts", (dialog, which) -> showDraftsList())
                    .setNegativeButton("Start New", null)
                    .show();
        }
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
        } else if (id == R.id.action_load_draft) {
            showDraftsList();
            return true;
        } else if (id == R.id.action_delete_drafts) {
            showBulkDeleteOptions();
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveAsDraft() {
        if (!validateDraftData()) {
            return;
        }

        try {
            JSONObject draftData = collectFormData();

            // Generate a unique draft ID
            String draftId = "variant_draft_" + System.currentTimeMillis();

            // Save to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save the draft data
            editor.putString(draftId, draftData.toString());

            // Also save a list of draft IDs for this product
            String draftListKey = "drafts_for_product_" + productId;
            String existingDrafts = prefs.getString(draftListKey, "");
            Set<String> draftSet = new HashSet<>();
            if (!existingDrafts.isEmpty()) {
                String[] draftsArray = existingDrafts.split(",");
                Collections.addAll(draftSet, draftsArray);
            }
            draftSet.add(draftId);

            String updatedDrafts = String.join(",", draftSet);
            editor.putString(draftListKey, updatedDrafts);

            // Save timestamp
            editor.putLong(draftId + "_timestamp", System.currentTimeMillis());

            if (editor.commit()) {
                showDraftSuccessDialog(draftId);
            } else {
                Toast.makeText(this, "Failed to save draft", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error saving draft", Toast.LENGTH_SHORT).show();
        }
    }

    private JSONObject collectFormData() throws JSONException {
        JSONObject draftData = new JSONObject();

        // Basic product info
        draftData.put("product_id", productId);
        draftData.put("created_at", System.currentTimeMillis());

        // Form fields
        draftData.put("measurement", etVariantMeasurement.getText().toString().trim());
        draftData.put("measurement_unit_position", spinnerVariantMeasurementUnit.getSelectedItemPosition());
        draftData.put("price", etVariantPrice.getText().toString().trim());
        draftData.put("sku", etVariantSKU.getText().toString().trim());
        draftData.put("hsn_code", etVariantHSNCode.getText().toString().trim());
        draftData.put("stock", etVariantStock.getText().toString().trim());
        draftData.put("stock_unit_position", spinnerVariantStockUnit.getSelectedItemPosition());
        draftData.put("discount_price", etVariantDiscountPrice.getText().toString().trim());
        draftData.put("discount_type_position", spinnerVariantDiscountType.getSelectedItemPosition());
        draftData.put("status_position", spinnerVariantStatus.getSelectedItemPosition());

        // Image handling - IMPROVED: Save both path and URI
        if (variantImageUri != null) {
            try {
                // Always save the URI string for reference
                draftData.put("image_uri", variantImageUri.toString());

                // Save the file path for camera images
                if (currentPhotoPath != null) {
                    draftData.put("image_path", currentPhotoPath);
                    Log.d(TAG, "Saved camera image path: " + currentPhotoPath);
                } else {
                    // For gallery images, try to get and save the path
                    String imagePath = getRealPathFromURI(variantImageUri);
                    if (imagePath != null) {
                        draftData.put("image_path", imagePath);
                    }
                }
                Log.d(TAG, "Saved image URI: " + variantImageUri.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error saving image data to draft: " + e.getMessage());
            }
        }

        return draftData;
    }

    private boolean validateDraftData() {
        // For drafts, we can be more lenient than final submission
        // But we should at least require basic information

        String measurement = etVariantMeasurement.getText().toString().trim();
        String price = etVariantPrice.getText().toString().trim();

        if (measurement.isEmpty() && price.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Empty Draft")
                    .setMessage("You haven't entered any variant information. Do you want to save an empty draft?")
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
                .setMessage("Your variant has been saved as draft. You can continue editing later from the drafts section.")
                .setPositiveButton("Continue Editing", null)
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setNeutralButton("View Drafts", (dialog, which) -> showDraftsList())
                .show();
    }

    // Method to load a saved draft - FIXED VERSION
// Method to load a saved draft - FIXED VERSION
    private void loadDraft(String draftId) {
        try {
            SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
            String draftJson = prefs.getString(draftId, "");

            if (draftJson == null || draftJson.isEmpty()) {
                Toast.makeText(this, "Draft not found", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject draftData = new JSONObject(draftJson);

            // Restore form fields
            etVariantMeasurement.setText(draftData.optString("measurement", ""));
            etVariantPrice.setText(draftData.optString("price", ""));
            etVariantSKU.setText(draftData.optString("sku", ""));
            etVariantHSNCode.setText(draftData.optString("hsn_code", ""));
            etVariantStock.setText(draftData.optString("stock", ""));
            etVariantDiscountPrice.setText(draftData.optString("discount_price", ""));

            // Restore spinner positions
            safeSetSpinnerSelection(spinnerVariantMeasurementUnit, draftData.optInt("measurement_unit_position", 0));
            safeSetSpinnerSelection(spinnerVariantStockUnit, draftData.optInt("stock_unit_position", 0));
            safeSetSpinnerSelection(spinnerVariantDiscountType, draftData.optInt("discount_type_position", 0));
            safeSetSpinnerSelection(spinnerVariantStatus, draftData.optInt("status_position", 0));

            // Handle image loading - FIXED VERSION
            boolean imageLoaded = false;

            if (draftData.has("image_path")) {
                String imagePath = draftData.getString("image_path");
                Log.d(TAG, "Loading image from path: " + imagePath);
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                    if (imageFile.exists()) {
                        try {
                            // Try multiple approaches to load the image
                            variantImageUri = Uri.fromFile(imageFile); // Try direct file URI first
                            if (variantImageUri != null) {
                                displaySelectedImage();
                                Toast.makeText(this, "Saved image loaded", Toast.LENGTH_SHORT).show();
                                imageLoaded = true;
                            } else {
                                Log.e(TAG, "Failed to create URI from file path");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error loading image from path: " + e.getMessage());
                        }
                    } else {
                        Log.e(TAG, "Image file not found at path: " + imagePath);
                    }
                }
            }

            // If path loading failed, try URI
            if (!imageLoaded && draftData.has("image_uri")) {
                // For gallery images, try to load from URI
                String imageUriString = draftData.getString("image_uri");
                Log.d(TAG, "Loading image from URI: " + imageUriString);
                try {
                    variantImageUri = Uri.parse(imageUriString);
                    if (variantImageUri != null) {
                        // Check if URI is still accessible
                        try {
                            getContentResolver().openInputStream(variantImageUri).close();
                            displaySelectedImage();
                            Toast.makeText(this, "Saved image loaded from gallery", Toast.LENGTH_SHORT).show();
                            imageLoaded = true;
                        } catch (Exception e) {
                            Log.e(TAG, "Gallery image URI no longer accessible: " + e.getMessage());
                            Toast.makeText(this, "Gallery image no longer available. Please reselect.", Toast.LENGTH_LONG).show();
                            variantImageUri = null;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing image URI: " + e.getMessage());
                }
            }

            // If both methods failed, show appropriate message
            if (!imageLoaded) {
                Log.d(TAG, "No image could be loaded from draft");
                // Don't show error toast here as it might be normal for drafts without images
            }

            Toast.makeText(this, "Draft loaded successfully", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Error loading draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error loading draft: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // Helper method to safely set spinner selection
    private void safeSetSpinnerSelection(Spinner spinner, int position) {
        try {
            if (spinner != null && position >= 0 && position < spinner.getCount()) {
                spinner.setSelection(position);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting spinner selection: " + e.getMessage());
        }
    }

    // Method to show list of drafts for this product
    private void showDraftsList() {
        SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
        String draftListKey = "drafts_for_product_" + productId;
        String existingDrafts = prefs.getString(draftListKey, "");

        if (existingDrafts.isEmpty()) {
            Toast.makeText(this, "No drafts found for this product", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] draftIds = existingDrafts.split(",");
        List<DraftItem> draftItems = new ArrayList<>();

        for (String draftId : draftIds) {
            String draftJson = prefs.getString(draftId, "");
            if (!draftJson.isEmpty()) {
                try {
                    JSONObject draftData = new JSONObject(draftJson);
                    String measurement = draftData.optString("measurement", "No measurement");
                    String price = draftData.optString("price", "No price");
                    long timestamp = draftData.optLong("created_at", System.currentTimeMillis());

                    String time = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(new Date(timestamp));

                    draftItems.add(new DraftItem(draftId, measurement + " - " + price + " (" + time + ")"));

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
    }

    // Helper class for draft items
    private static class DraftItem {
        String draftId;
        String displayText;

        DraftItem(String draftId, String displayText) {
            this.draftId = draftId;
            this.displayText = displayText;
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
                                    .setPositiveButton("Load", (d, w) -> loadDraft(draftId))
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
            SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Remove the draft data
            editor.remove(draftId);

            // Remove the timestamp
            editor.remove(draftId + "_timestamp");

            // Remove from product draft list
            String draftListKey = "drafts_for_product_" + productId;
            String existingDrafts = prefs.getString(draftListKey, "");
            if (!existingDrafts.isEmpty()) {
                Set<String> draftSet = new HashSet<>();
                Collections.addAll(draftSet, existingDrafts.split(","));
                draftSet.remove(draftId);

                String updatedDrafts = String.join(",", draftSet);
                if (updatedDrafts.isEmpty()) {
                    editor.remove(draftListKey);
                } else {
                    editor.putString(draftListKey, updatedDrafts);
                }
            }

            if (editor.commit()) {
                Toast.makeText(this, "Draft deleted successfully", Toast.LENGTH_SHORT).show();
                // Optionally refresh the drafts list
                showDraftsList();
            } else {
                Toast.makeText(this, "Failed to delete draft", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error deleting draft: " + e.getMessage(), e);
            Toast.makeText(this, "Error deleting draft", Toast.LENGTH_SHORT).show();
        }
    }

    private void showBulkDeleteOptions() {
        SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
        String draftListKey = "drafts_for_product_" + productId;
        String existingDrafts = prefs.getString(draftListKey, "");

        if (existingDrafts.isEmpty()) {
            Toast.makeText(this, "No drafts found for this product", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Drafts")
                .setItems(new CharSequence[]{
                        "Delete All Drafts for This Product",
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
                .setMessage("Are you sure you want to delete ALL drafts for this product? This action cannot be undone.")
                .setPositiveButton("Delete All", (dialog, which) -> deleteAllDrafts())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllDrafts() {
        try {
            SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            String draftListKey = "drafts_for_product_" + productId;
            String existingDrafts = prefs.getString(draftListKey, "");

            if (!existingDrafts.isEmpty()) {
                String[] draftIds = existingDrafts.split(",");
                int deletedCount = 0;

                for (String draftId : draftIds) {
                    editor.remove(draftId);
                    editor.remove(draftId + "_timestamp");
                    deletedCount++;
                }

                editor.remove(draftListKey);

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

    // Method to clear old drafts (optional cleanup)
    private int clearOldDrafts() {
        SharedPreferences prefs = getSharedPreferences("VariantDrafts", MODE_PRIVATE);
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

                    // Also remove from product draft lists
                    for (String key : allEntries.keySet()) {
                        if (key.startsWith("drafts_for_product_")) {
                            String drafts = prefs.getString(key, "");
                            if (drafts.contains(draftId)) {
                                String updatedDrafts = drafts.replace(draftId, "")
                                        .replace(",,", ",")
                                        .replaceAll("^,|,$", "");
                                if (updatedDrafts.isEmpty()) {
                                    editor.remove(key);
                                } else {
                                    editor.putString(key, updatedDrafts);
                                }
                            }
                        }
                    }
                }
            }
        }

        editor.apply();
        return deletedCount;
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
        currentPhotoPath = null;
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
            try {
                ivVariantImagePreview.setImageURI(variantImageUri);
                ivVariantImagePreview.setVisibility(View.VISIBLE);
                uploadPlaceholder.setVisibility(View.GONE);
                fabRemoveImage.setVisibility(View.VISIBLE);

                // Force image view to redraw
                ivVariantImagePreview.invalidate();

                Log.d(TAG, "Image displayed successfully: " + variantImageUri.toString());

            } catch (Exception e) {
                Log.e(TAG, "Error displaying selected image: " + e.getMessage());
                Toast.makeText(this, "Error displaying image", Toast.LENGTH_SHORT).show();
                // Reset image state on error
                variantImageUri = null;
                ivVariantImagePreview.setVisibility(View.GONE);
                uploadPlaceholder.setVisibility(View.VISIBLE);
                fabRemoveImage.setVisibility(View.GONE);
            }
        } else {
            ivVariantImagePreview.setVisibility(View.GONE);
            uploadPlaceholder.setVisibility(View.VISIBLE);
            fabRemoveImage.setVisibility(View.GONE);
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
                File imageFile = getImageFileFromUri(variantImageUri);
                if (imageFile != null && imageFile.exists()) {
                    multipartBuilder.addFormDataPart("variant_image[]",
                            imageFile.getName(),
                            RequestBody.create(MediaType.parse("image/*"), imageFile));
                } else {
                    Log.e(TAG, "Image file not found or inaccessible: " + variantImageUri);
                    Toast.makeText(this, "Image file not accessible. Please select image again.", Toast.LENGTH_LONG).show();
                    hideProgress();
                    return;
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
                    Log.d("VARIANT_RESPONSE", "Response Code: " + response.code() + ", Body: " + responseBody);

                    runOnUiThread(() -> {
                        hideProgress();
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);

                            // More flexible success checking
                            String status = jsonResponse.optString("status", "").toLowerCase();
                            boolean isSuccess = response.isSuccessful() &&
                                    (status.equals("success") ||
                                            status.equals("true") ||
                                            jsonResponse.optBoolean("success", false));

                            if (isSuccess) {


                                Intent intent = new Intent(AddVariantActivity.this, ProductViewActivity.class);
                                // Add flags to clear the back stack if needed
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                intent.putExtra("prod_id",productId);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                finish();
                            } else {
                                String errorMessage = jsonResponse.optString("message", "Failed to add variant");
                                Toast.makeText(AddVariantActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                Log.e("VARIANT_ERROR", "Server error: " + errorMessage);
                            }
                        } catch (JSONException e) {
                            Log.e("JSON_ERROR", "Error parsing response: " + e.getMessage() + ", Response: " + responseBody);
                            Toast.makeText(AddVariantActivity.this, "Error parsing server response", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e("VARIANT_ERROR", "Unexpected error: " + e.getMessage());
                            Toast.makeText(AddVariantActivity.this, "Unexpected error occurred", Toast.LENGTH_LONG).show();
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

    // Improved method to get image file from URI

    private File getImageFileFromUri(Uri uri) {
        if (uri == null) return null;

        try {
            String scheme = uri.getScheme();
            Log.d(TAG, "Getting file from URI with scheme: " + scheme);

            if (scheme != null && scheme.equals("file")) {
                // File URI
                String path = uri.getPath();
                Log.d(TAG, "File URI path: " + path);
                return new File(path);
            } else if (scheme != null && scheme.equals("content")) {
                // Content URI - try to get the file path
                String filePath = getRealPathFromURI(uri);
                if (filePath != null) {
                    Log.d(TAG, "Content URI resolved to path: " + filePath);
                    return new File(filePath);
                } else {
                    Log.d(TAG, "Content URI could not be resolved to path, creating temp file");
                    // For content URIs that we can't resolve, create a temporary file
                    return createTempFileFromUri(uri);
                }
            } else {
                Log.e(TAG, "Unknown URI scheme: " + scheme);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting image file from URI: " + e.getMessage(), e);
            return null;
        }
    }

    // Create temporary file from content URI
    private File createTempFileFromUri(Uri uri) {
        try {
            android.content.ContentResolver resolver = getContentResolver();
            java.io.InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream != null) {
                File tempFile = File.createTempFile("temp_variant_image", ".jpg", getCacheDir());
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
        if (contentUri == null) return null;

        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                String path = cursor.getString(column_index);
                Log.d(TAG, "Real path from URI: " + path);
                return path;
            } else {
                Log.d(TAG, "Cursor is null or empty for URI: " + contentUri);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting real path from URI: " + e.getMessage());
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
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