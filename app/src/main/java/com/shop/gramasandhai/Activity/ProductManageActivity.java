package com.shop.gramasandhai.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shop.gramasandhai.Adapter.CategoryAdapter;
import com.shop.gramasandhai.Adapter.ProductAdapter;
import com.shop.gramasandhai.Adapter.SubcategoryAdapter;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.Model.Category;
import com.shop.gramasandhai.Model.Product;
import com.shop.gramasandhai.Model.Subcategory;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductManageActivity extends AppCompatActivity {

    private RecyclerView rvItems;
    private TextView tvTotalItems, tvActiveItems, tvTotalLabel, tvBreadcrumb;
    private MaterialCardView cardBreadcrumb, cardSearchFilter;
    private TextInputEditText etSearch;
    private ImageButton btnBack;
    private FloatingActionButton fabAddProduct;
    private LinearLayout shimmerLayout, emptyLayout;
    private ShimmerFrameLayout shimmerTotal, shimmerActive, shimmerSearch;

    // ADDED: Content layout references for lazy loading
    private LinearLayout contentTotal, contentActive, contentSearch;

    // Adapters
    private CategoryAdapter categoryAdapter;
    private SubcategoryAdapter subcategoryAdapter;
    private ProductAdapter productAdapter;

    // Data
    private List<Category> categoryList = new ArrayList<>();
    private List<Subcategory> subcategoryList = new ArrayList<>();
    private List<Product> productList = new ArrayList<>();

    // Filtered data for search
    private List<Category> filteredCategoryList = new ArrayList<>();
    private List<Subcategory> filteredSubcategoryList = new ArrayList<>();
    private List<Product> filteredProductList = new ArrayList<>();

    // Current state
    private String currentLevel = "categories";
    private String selectedCategoryId = "";
    private String selectedCategoryName = "";
    private String selectedSubcategoryId = "";
    private String selectedSubcategoryName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_manage);

        initializeViews();
        setupToolbar();
        setupAdapters();
        setupClickListeners();

        // Load initial data with shimmer effect
        loadInitialData();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        rvItems = findViewById(R.id.rvItems);
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvActiveItems = findViewById(R.id.tvActiveItems);
        tvTotalLabel = findViewById(R.id.tvTotalLabel);
        tvBreadcrumb = findViewById(R.id.tvBreadcrumb);
        cardBreadcrumb = findViewById(R.id.cardBreadcrumb);
        cardSearchFilter = findViewById(R.id.cardSearchFilter);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        fabAddProduct = findViewById(R.id.fabAddProduct);

        // Shimmer views
        shimmerLayout = findViewById(R.id.shimmerLayout);
        emptyLayout = findViewById(R.id.emptyLayout);
        shimmerTotal = findViewById(R.id.shimmerTotal);
        shimmerActive = findViewById(R.id.shimmerActive);
        shimmerSearch = findViewById(R.id.shimmerSearch);

        // ADDED: Initialize content layout references
        contentTotal = findViewById(R.id.contentTotal);
        contentActive = findViewById(R.id.contentActive);
        contentSearch = findViewById(R.id.contentSearch);

        // Initialize with loading state
        showFullLoadingState(true);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setupAdapters() {
        categoryAdapter = new CategoryAdapter(this, new ArrayList<>());
        subcategoryAdapter = new SubcategoryAdapter(this, new ArrayList<>());
        productAdapter = new ProductAdapter(this, new ArrayList<>());

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(categoryAdapter);

        // Set click listeners
        categoryAdapter.setOnItemClickListener((category, position) -> {
            selectedCategoryId = category.getId();
            selectedCategoryName = category.getName();
            loadSubcategories(category.getId());
        });

        subcategoryAdapter.setOnItemClickListener((subcategory, position) -> {
            selectedSubcategoryId = subcategory.getId();
            selectedSubcategoryName = subcategory.getName();
            loadProducts(subcategory.getId());
        });

        productAdapter.setOnItemClickListener((product, position) -> {
            showProductDetails(product);
        });

        productAdapter.setOnMoreOptionsClickListener((product, position, view) -> {
            showProductOptionsMenu(product, view);
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> navigateBack());

        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(ProductManageActivity.this, AddProductActivity.class);
            startActivity(intent);
        });

        // Search functionality
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterItems(s.toString());
            }
        });
    }

    private void loadInitialData() {
        showFullLoadingState(true);
        loadCategories();
    }

    private void loadCategories() {
        currentLevel = "categories";
        updateBreadcrumb("Categories");
        Log.d("API_DEBUG", "Loading categories for Shop ID: " + shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/category")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showFullLoadingState(false);
                    Toast.makeText(ProductManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyState(true);
                });
                Log.e("API_ERROR", "Categories request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("API_RESPONSE", "Categories raw response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseCategoriesData(responseObject);

                            // Show categories immediately after parsing
                            showCategories();

                            // Load products data in background for global stats
                            loadProductsData();
                        } else {
                            String errorMsg = responseObject.optString("message", "Failed to load categories");
                            Toast.makeText(ProductManageActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            showFullLoadingState(false);
                            showEmptyState(true);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Categories parsing failed: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Error parsing categories: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Unexpected error: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Unexpected error occurred", Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    }
                });
            }
        });
    }

    private void loadSubcategories(String categoryId) {
        currentLevel = "subcategories";
        updateBreadcrumb(selectedCategoryName);
        showFullLoadingState(true);

        Log.d("API_DEBUG", "Loading subcategories for Category ID: " + categoryId + ", Shop ID: " + shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/subcategory")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showFullLoadingState(false);
                    Toast.makeText(ProductManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyState(true);
                });
                Log.e("API_ERROR", "Subcategories request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("API_RESPONSE", "Subcategories raw response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseSubcategoriesData(responseObject, categoryId);
                            showSubcategories();
                        } else {
                            String errorMsg = responseObject.optString("message", "Failed to load subcategories");
                            Toast.makeText(ProductManageActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            showFullLoadingState(false);
                            showEmptyState(true);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Subcategories parsing failed: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Error parsing subcategories: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Unexpected error: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Unexpected error occurred", Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    }
                });
            }
        });
    }

    private void loadProducts(String subcategoryId) {
        currentLevel = "products";
        updateBreadcrumb(selectedSubcategoryName);
        showFullLoadingState(true);

        Log.d("API_DEBUG", "Loading products for Subcategory ID: " + subcategoryId + ", Shop ID: " + shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/products")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    showFullLoadingState(false);
                    Toast.makeText(ProductManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    showEmptyState(true);
                });
                Log.e("API_ERROR", "Products request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("API_RESPONSE", "Products raw response: " + responseBodyString);

                runOnUiThread(() -> {
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseProductsData(responseObject, subcategoryId);
                            showProducts();
                        } else {
                            String errorMsg = responseObject.optString("message", "Failed to load products");
                            Toast.makeText(ProductManageActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            showFullLoadingState(false);
                            showEmptyState(true);
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Products parsing failed: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Error parsing products: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Unexpected error: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Unexpected error occurred", Toast.LENGTH_LONG).show();
                        showFullLoadingState(false);
                        showEmptyState(true);
                    }
                });
            }
        });
    }

    private void loadProductsData() {
        Log.d("API_DEBUG", "Loading products data for Shop ID: " + shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/products")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    // Even if products data fails, we still show the categories
                    showStatsLoading(false);
                    Log.e("API_ERROR", "Products request failed: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("API_RESPONSE", "Products raw response: " + responseBodyString);

                runOnUiThread(() -> {
                    showStatsLoading(false);
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseGlobalProductsData(responseObject);
                            updateAllStats();
                        } else {
                            Log.e("API_ERROR", "Failed to load products data: " + responseObject.optString("message", ""));
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Products parsing failed: " + e.getMessage());
                    } catch (Exception e) {
                        Log.e("PARSE_ERROR", "Unexpected error: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void parseCategoriesData(JSONObject responseObject) throws JSONException {
        categoryList.clear();

        if (responseObject.has("categories") && !responseObject.isNull("categories")) {
            JSONArray categoriesArray = responseObject.getJSONArray("categories");
            Log.d("PARSE_DEBUG", "Categories count: " + categoriesArray.length());

            for (int i = 0; i < categoriesArray.length(); i++) {
                JSONObject categoryObj = categoriesArray.getJSONObject(i);

                Category category = new Category();
                category.setId(categoryObj.optString("category_id", ""));
                category.setName(categoryObj.optString("category_name", ""));
                category.setSubtitle(categoryObj.optString("category_subtitle", ""));

                String imageUrl = categoryObj.optString("category_image", "");
                if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                    category.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
                } else {
                    category.setImageUrl("");
                }

                category.setStatus(categoryObj.optString("category_status", "0"));
                category.setPosition(categoryObj.optString("position", "0"));
                category.setSubcategoriesCount(categoryObj.optString("subcategory_count", "0"));

                categoryList.add(category);
            }
        }

        filteredCategoryList = new ArrayList<>(categoryList);
        Log.d("PARSE_DEBUG", "Final categories count: " + categoryList.size());
    }

    private void parseSubcategoriesData(JSONObject responseObject, String categoryId) throws JSONException {
        subcategoryList.clear();
        filteredSubcategoryList.clear();

        if (responseObject.has("subcategories") && !responseObject.isNull("subcategories")) {
            JSONArray subcategoriesArray = responseObject.getJSONArray("subcategories");
            Log.d("PARSE_DEBUG", "All subcategories count: " + subcategoriesArray.length());

            for (int i = 0; i < subcategoriesArray.length(); i++) {
                JSONObject subcategoryObj = subcategoriesArray.getJSONObject(i);

                Subcategory subcategory = new Subcategory();
                subcategory.setId(subcategoryObj.optString("subcategory_id", ""));
                subcategory.setName(subcategoryObj.optString("sub_category_name", ""));
                subcategory.setSubtitle(subcategoryObj.optString("sub_category_subtitle", ""));

                String imageUrl = subcategoryObj.optString("sub_category_image", "");
                if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                    subcategory.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
                } else {
                    subcategory.setImageUrl("");
                }

                subcategory.setStatus(subcategoryObj.optString("subcategory_status", "0"));
                subcategory.setCategoryId(subcategoryObj.optString("category_id", ""));
                subcategory.setCategoryName(subcategoryObj.optString("main_category", ""));
                subcategory.setProductCount(subcategoryObj.optString("products_count", "0"));

                subcategoryList.add(subcategory);

                // Add to filtered list if it belongs to the selected category
                if (subcategory.getCategoryId().equals(categoryId)) {
                    filteredSubcategoryList.add(subcategory);
                }
            }
        }

        Log.d("PARSE_DEBUG", "Filtered subcategories for category " + categoryId + ": " + filteredSubcategoryList.size());
    }

    private void parseProductsData(JSONObject responseObject, String subcategoryId) throws JSONException {
        productList.clear();
        filteredProductList.clear();

        if (responseObject.has("products") && !responseObject.isNull("products")) {
            JSONArray productsArray = responseObject.getJSONArray("products");
            Log.d("PARSE_DEBUG", "All products count: " + productsArray.length());

            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject productObj = productsArray.getJSONObject(i);

                Product product = new Product();
                product.setId(productObj.optString("id", ""));
                product.setName(productObj.optString("prod_name", ""));
                product.setDescription(productObj.optString("description", ""));

                String imageUrl = productObj.optString("main_image", "");
                if (imageUrl != null && !imageUrl.equals("null") && !imageUrl.isEmpty()) {
                    product.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
                } else {
                    product.setImageUrl("");
                }

                product.setStatus(productObj.optString("status", "0"));
                product.setCategoryId(productObj.optString("category_id", ""));
                product.setSubcategoryId(productObj.optString("subcategory_id", ""));
                product.setPrice(productObj.optString("prod_price", "0"));
                product.setStock(productObj.optString("stock", "0"));
                product.setDiscountValue(productObj.optString("disc_value", "0"));

                productList.add(product);

                // Add to filtered list if it belongs to the selected subcategory
                if (product.getSubcategoryId().equals(subcategoryId)) {
                    filteredProductList.add(product);
                }
            }
        }

        productAdapter.setApiResponseData(responseObject);
        Log.d("PARSE_DEBUG", "Filtered products for subcategory " + subcategoryId + ": " + filteredProductList.size());
    }

    private void parseGlobalProductsData(JSONObject responseObject) throws JSONException {
        // This method is only for global products data used in stats calculation
        // We don't need to filter here as it's for overall statistics
        if (responseObject.has("products") && !responseObject.isNull("products")) {
            JSONArray productsArray = responseObject.getJSONArray("products");
            Log.d("PARSE_DEBUG", "Global products count for stats: " + productsArray.length());
        }
    }

    private void showCategories() {
        currentLevel = "categories";
        updateBreadcrumb("Categories");
        rvItems.setAdapter(categoryAdapter);
        categoryAdapter.updateList(filteredCategoryList);

        // Calculate stats for current filtered list
        int totalCategories = filteredCategoryList.size();
        int activeCategories = getActiveCount(filteredCategoryList);
        updateStats(totalCategories, activeCategories);

        tvTotalLabel.setText("Total Categories");
        cardBreadcrumb.setVisibility(View.GONE);

        showEmptyState(filteredCategoryList.isEmpty());
        showFullLoadingState(false);
        etSearch.setText("");

        Log.d("STATS_DEBUG", "Categories - Total: " + totalCategories + ", Active: " + activeCategories);
    }

    private void showSubcategories() {
        currentLevel = "subcategories";
        rvItems.setAdapter(subcategoryAdapter);
        subcategoryAdapter.updateList(filteredSubcategoryList);

        // Calculate stats for current filtered subcategories
        int totalSubcategories = filteredSubcategoryList.size();
        int activeSubcategories = getActiveCount(filteredSubcategoryList);
        updateStats(totalSubcategories, activeSubcategories);

        tvTotalLabel.setText("Total Subcategories");
        cardBreadcrumb.setVisibility(View.VISIBLE);

        showEmptyState(filteredSubcategoryList.isEmpty());
        showFullLoadingState(false);
        etSearch.setText("");

        Log.d("STATS_DEBUG", "Subcategories - Total: " + totalSubcategories + ", Active: " + activeSubcategories);
    }

    private void showProducts() {
        currentLevel = "products";
        rvItems.setAdapter(productAdapter);
        productAdapter.updateList(filteredProductList);

        // Calculate stats for current filtered products
        int totalProducts = filteredProductList.size();
        int activeProducts = getActiveCount(filteredProductList);
        updateStats(totalProducts, activeProducts);

        tvTotalLabel.setText("Total Products");
        cardBreadcrumb.setVisibility(View.VISIBLE);

        showEmptyState(filteredProductList.isEmpty());
        showFullLoadingState(false);
        etSearch.setText("");

        Log.d("STATS_DEBUG", "Products - Total: " + totalProducts + ", Active: " + activeProducts);
    }

    private void navigateBack() {
        switch (currentLevel) {
            case "subcategories":
                showCategories();
                break;
            case "products":
                loadSubcategories(selectedCategoryId);
                break;
            default:
                finish();
                break;
        }
    }

    private void updateBreadcrumb(String currentItem) {
        String breadcrumb = "";
        switch (currentLevel) {
            case "categories":
                breadcrumb = "Categories";
                break;
            case "subcategories":
                breadcrumb = "Categories > " + currentItem;
                break;
            case "products":
                breadcrumb = "Categories > " + selectedCategoryName + " > " + currentItem;
                break;
        }
        tvBreadcrumb.setText(breadcrumb);
    }

    private void updateStats(int total, int active) {
        tvTotalItems.setText(String.valueOf(total));
        tvActiveItems.setText(String.valueOf(active));
        Log.d("STATS_UPDATE", "Updated stats - Total: " + total + ", Active: " + active);
    }

    private void updateAllStats() {
        // This method updates stats based on current level and filtered data
        switch (currentLevel) {
            case "categories":
                updateStats(filteredCategoryList.size(), getActiveCount(filteredCategoryList));
                break;
            case "subcategories":
                updateStats(filteredSubcategoryList.size(), getActiveCount(filteredSubcategoryList));
                break;
            case "products":
                updateStats(filteredProductList.size(), getActiveCount(filteredProductList));
                break;
        }
    }

    // Improved active count calculation
    private int getActiveCount(List<?> items) {
        int activeCount = 0;
        for (Object item : items) {
            if (item instanceof Category) {
                Category category = (Category) item;
                if ("1".equals(category.getStatus()) || "active".equalsIgnoreCase(category.getStatus())) {
                    activeCount++;
                }
            } else if (item instanceof Subcategory) {
                Subcategory subcategory = (Subcategory) item;
                if ("1".equals(subcategory.getStatus()) || "active".equalsIgnoreCase(subcategory.getStatus())) {
                    activeCount++;
                }
            } else if (item instanceof Product) {
                Product product = (Product) item;
                if ("1".equals(product.getStatus()) || "active".equalsIgnoreCase(product.getStatus())) {
                    activeCount++;
                }
            }
        }
        Log.d("ACTIVE_COUNT", "Active items: " + activeCount + " out of " + items.size());
        return activeCount;
    }

    private void filterItems(String query) {
        switch (currentLevel) {
            case "categories":
                filterCategories(query);
                break;
            case "subcategories":
                filterSubcategories(query);
                break;
            case "products":
                filterProducts(query);
                break;
        }
    }

    private void filterCategories(String query) {
        filteredCategoryList.clear();

        if (query.isEmpty()) {
            filteredCategoryList.addAll(categoryList);
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Category category : categoryList) {
                if (category.getName().toLowerCase().contains(searchQuery) ||
                        (category.getSubtitle() != null && category.getSubtitle().toLowerCase().contains(searchQuery))) {
                    filteredCategoryList.add(category);
                }
            }
        }

        categoryAdapter.updateList(filteredCategoryList);
        updateStats(filteredCategoryList.size(), getActiveCount(filteredCategoryList));
        showEmptyState(filteredCategoryList.isEmpty());
    }

    private void filterSubcategories(String query) {
        List<Subcategory> tempFilteredList = new ArrayList<>();

        if (query.isEmpty()) {
            // Show all subcategories for the selected category
            for (Subcategory subcategory : subcategoryList) {
                if (subcategory.getCategoryId().equals(selectedCategoryId)) {
                    tempFilteredList.add(subcategory);
                }
            }
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Subcategory subcategory : subcategoryList) {
                if (subcategory.getCategoryId().equals(selectedCategoryId) &&
                        (subcategory.getName().toLowerCase().contains(searchQuery) ||
                                (subcategory.getSubtitle() != null && subcategory.getSubtitle().toLowerCase().contains(searchQuery)))) {
                    tempFilteredList.add(subcategory);
                }
            }
        }

        filteredSubcategoryList.clear();
        filteredSubcategoryList.addAll(tempFilteredList);
        subcategoryAdapter.updateList(filteredSubcategoryList);
        updateStats(filteredSubcategoryList.size(), getActiveCount(filteredSubcategoryList));
        showEmptyState(filteredSubcategoryList.isEmpty());
    }

    private void filterProducts(String query) {
        List<Product> tempFilteredList = new ArrayList<>();

        if (query.isEmpty()) {
            // Show all products for the selected subcategory
            for (Product product : productList) {
                if (product.getSubcategoryId().equals(selectedSubcategoryId)) {
                    tempFilteredList.add(product);
                }
            }
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (Product product : productList) {
                if (product.getSubcategoryId().equals(selectedSubcategoryId) &&
                        (product.getName().toLowerCase().contains(searchQuery) ||
                                (product.getDescription() != null && product.getDescription().toLowerCase().contains(searchQuery)))) {
                    tempFilteredList.add(product);
                }
            }
        }

        filteredProductList.clear();
        filteredProductList.addAll(tempFilteredList);
        productAdapter.updateList(filteredProductList);
        updateStats(filteredProductList.size(), getActiveCount(filteredProductList));
        showEmptyState(filteredProductList.isEmpty());
    }

    // ENHANCED: Complete lazy loading methods
    private void showFullLoadingState(boolean show) {
        if (show) {
            // Show all shimmer layouts
            shimmerTotal.setVisibility(View.VISIBLE);
            shimmerActive.setVisibility(View.VISIBLE);
            shimmerSearch.setVisibility(View.VISIBLE);
            shimmerLayout.setVisibility(View.VISIBLE);

            // Hide all content layouts
            contentTotal.setVisibility(View.GONE);
            contentActive.setVisibility(View.GONE);
            contentSearch.setVisibility(View.GONE);
            rvItems.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
            cardBreadcrumb.setVisibility(View.GONE);

            // Start all shimmer animations
            shimmerTotal.startShimmer();
            shimmerActive.startShimmer();
            shimmerSearch.startShimmer();
        } else {
            // Hide all shimmer layouts
            shimmerTotal.setVisibility(View.GONE);
            shimmerActive.setVisibility(View.GONE);
            shimmerSearch.setVisibility(View.GONE);
            shimmerLayout.setVisibility(View.GONE);

            // Show all content layouts
            contentTotal.setVisibility(View.VISIBLE);
            contentActive.setVisibility(View.VISIBLE);
            contentSearch.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.VISIBLE);

            // Stop all shimmer animations
            shimmerTotal.stopShimmer();
            shimmerActive.stopShimmer();
            shimmerSearch.stopShimmer();
        }
    }

    private void showStatsLoading(boolean show) {
        if (show) {
            shimmerTotal.setVisibility(View.VISIBLE);
            shimmerActive.setVisibility(View.VISIBLE);
            contentTotal.setVisibility(View.GONE);
            contentActive.setVisibility(View.GONE);
            shimmerTotal.startShimmer();
            shimmerActive.startShimmer();
        } else {
            shimmerTotal.setVisibility(View.GONE);
            shimmerActive.setVisibility(View.GONE);
            contentTotal.setVisibility(View.VISIBLE);
            contentActive.setVisibility(View.VISIBLE);
            shimmerTotal.stopShimmer();
            shimmerActive.stopShimmer();
        }
    }

    private void showSearchLoading(boolean show) {
        if (show) {
            shimmerSearch.setVisibility(View.VISIBLE);
            contentSearch.setVisibility(View.GONE);
            shimmerSearch.startShimmer();
        } else {
            shimmerSearch.setVisibility(View.GONE);
            contentSearch.setVisibility(View.VISIBLE);
            shimmerSearch.stopShimmer();
        }
    }

    private void showRecyclerViewLoading(boolean show) {
        if (show) {
            shimmerLayout.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.GONE);
        } else {
            shimmerLayout.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyLayout.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            shimmerLayout.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            rvItems.setVisibility(View.VISIBLE);
            shimmerLayout.setVisibility(View.GONE);
        }
    }

    private void showProductDetails(Product product) {
        Toast.makeText(this, "Product Details: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    private void showProductOptionsMenu(Product product, View view) {
        Toast.makeText(this, "Options for: " + product.getName(), Toast.LENGTH_SHORT).show();
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.product_management_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            loadInitialData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!currentLevel.equals("categories")) {
            navigateBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart shimmer animations if they are visible
        if (shimmerTotal.getVisibility() == View.VISIBLE) {
            shimmerTotal.startShimmer();
        }
        if (shimmerActive.getVisibility() == View.VISIBLE) {
            shimmerActive.startShimmer();
        }
        if (shimmerSearch.getVisibility() == View.VISIBLE) {
            shimmerSearch.startShimmer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop all shimmer animations to save battery
        shimmerTotal.stopShimmer();
        shimmerActive.stopShimmer();
        shimmerSearch.stopShimmer();
    }
}