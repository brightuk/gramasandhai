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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
    private ProgressBar progressBar; // Changed from LottieAnimationView to ProgressBar
    private TextView tvTotalItems, tvActiveItems, tvTotalLabel, tvBreadcrumb;
    private MaterialCardView cardBreadcrumb, cardSearchFilter;
    private TextInputEditText etSearch;
    private ImageButton btnBack;
    private FloatingActionButton fabAddProduct;

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
    private String currentLevel = "categories"; // categories, subcategories, products
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

        // Load categories
        loadCategories();
    }

    private void initializeViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rvItems = findViewById(R.id.rvItems);
        progressBar = findViewById(R.id.progressBar); // Updated to ProgressBar
        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvActiveItems = findViewById(R.id.tvActiveItems);
        tvTotalLabel = findViewById(R.id.tvTotalLabel);
        tvBreadcrumb = findViewById(R.id.tvBreadcrumb);
        cardBreadcrumb = findViewById(R.id.cardBreadcrumb);
        cardSearchFilter = findViewById(R.id.cardSearchFilter);
        etSearch = findViewById(R.id.etSearch);
        btnBack = findViewById(R.id.btnBack);
        fabAddProduct = findViewById(R.id.fabAddProduct);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setupAdapters() {
        categoryAdapter = new CategoryAdapter(this, categoryList);
        subcategoryAdapter = new SubcategoryAdapter(this, subcategoryList);
        productAdapter = new ProductAdapter(this, productList);

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

        // Search functionality with TextWatcher
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

    private void loadCategories() {
        showLoading(true);
        currentLevel = "categories";
        updateBreadcrumb("Categories");
        Log.d("tsssssddd", shopId());

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
                    showLoading(false);
                    Toast.makeText(ProductManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("Products_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    showLoading(false);
                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseCategoriesData(responseObject);
                            showCategories();
                        } else {
                            Toast.makeText(ProductManageActivity.this,
                                    "Failed to load data", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        Toast.makeText(ProductManageActivity.this,
                                "Error parsing data", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void parseCategoriesData(JSONObject responseObject) throws JSONException {
        categoryList.clear();
        subcategoryList.clear();
        productList.clear();

        // Parse categories
        JSONArray categoriesArray = responseObject.getJSONArray("categories");
        for (int i = 0; i < categoriesArray.length(); i++) {
            JSONObject categoryObj = categoriesArray.getJSONObject(i);

            Category category = new Category();
            category.setId(categoryObj.getString("category_id"));
            category.setName(categoryObj.getString("category_name"));
            category.setSubtitle(categoryObj.getString("category_subtitle"));

            // Build full image URL
            String imageUrl = categoryObj.getString("category_image");
            category.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
            category.setStatus(categoryObj.getString("category_status"));
            category.setPosition(categoryObj.getString("position"));
            category.setSubcategoriesCount(categoryObj.getString("subcategory_count"));

            categoryList.add(category);
        }

        // Parse subcategories
        JSONArray subcategoriesArray = responseObject.getJSONArray("subcategories");
        for (int i = 0; i < subcategoriesArray.length(); i++) {
            JSONObject subcategoryObj = subcategoriesArray.getJSONObject(i);

            Subcategory subcategory = new Subcategory();
            subcategory.setId(subcategoryObj.getString("subcategory_id"));
            subcategory.setName(subcategoryObj.getString("sub_category_name"));
            subcategory.setSubtitle(subcategoryObj.getString("sub_category_subtitle"));

            // Build full image URL
            String imageUrl = subcategoryObj.getString("sub_category_image");
            subcategory.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
            subcategory.setStatus(subcategoryObj.getString("subcategory_status"));
            subcategory.setCategoryId(subcategoryObj.getString("category_id"));
            subcategory.setCategoryName(subcategoryObj.getString("main_category"));
            subcategory.setProductCount(subcategoryObj.getString("products_count"));

            subcategoryList.add(subcategory);
        }

        // Parse products
        if (responseObject.has("products") && !responseObject.isNull("products")) {
            JSONArray productsArray = responseObject.getJSONArray("products");
            for (int i = 0; i < productsArray.length(); i++) {
                JSONObject productObj = productsArray.getJSONObject(i);

                Product product = new Product();
                product.setId(productObj.optString("id", ""));
                product.setName(productObj.optString("prod_name", ""));
                product.setDescription(productObj.optString("description", ""));

                // Build full image URL
                String imageUrl = productObj.optString("main_image", "");
                product.setImageUrl(Attributes.Root_Url + "uploads/images/" + imageUrl);
                product.setCategoryId(productObj.optString("category_id", ""));
                product.setSubcategoryId(productObj.optString("subcategory_id", ""));

                productList.add(product);
            }
        }

        // Pass the full API response to product adapter for variants data
        productAdapter.setApiResponseData(responseObject);

        // Initialize filtered lists with all data
        filteredCategoryList = new ArrayList<>(categoryList);
        filteredSubcategoryList = new ArrayList<>(subcategoryList);
        filteredProductList = new ArrayList<>(productList);
    }

    private void showCategories() {
        currentLevel = "categories";
        updateBreadcrumb("Categories");
        rvItems.setAdapter(categoryAdapter);
        categoryAdapter.updateList(filteredCategoryList);
        updateStats(filteredCategoryList.size(), getActiveCount(filteredCategoryList));
        tvTotalLabel.setText("Categories");
        cardBreadcrumb.setVisibility(View.GONE);

        // Clear search when showing categories
        etSearch.setText("");
    }

    private void loadSubcategories(String categoryId) {
        currentLevel = "subcategories";
        updateBreadcrumb(selectedCategoryName);

        // Filter subcategories for the selected category
        filteredSubcategoryList.clear();
        for (Subcategory subcategory : subcategoryList) {
            if (subcategory.getCategoryId().equals(categoryId)) {
                filteredSubcategoryList.add(subcategory);
            }
        }

        showSubcategories();
    }

    private void showSubcategories() {
        currentLevel = "subcategories";
        rvItems.setAdapter(subcategoryAdapter);
        subcategoryAdapter.updateList(filteredSubcategoryList);
        updateStats(filteredSubcategoryList.size(), getActiveCount(filteredSubcategoryList));
        tvTotalLabel.setText("Subcategories");
        cardBreadcrumb.setVisibility(View.VISIBLE);

        // Clear search when showing subcategories
        etSearch.setText("");
    }

    private void loadProducts(String subcategoryId) {
        currentLevel = "products";
        updateBreadcrumb(selectedSubcategoryName);

        // Filter products for the selected subcategory
        filteredProductList.clear();
        for (Product product : productList) {
            if (product.getSubcategoryId().equals(subcategoryId)) {
                filteredProductList.add(product);
            }
        }

        showProducts();
    }

    private void showProducts() {
        currentLevel = "products";
        rvItems.setAdapter(productAdapter);
        productAdapter.updateList(filteredProductList);
        updateStats(filteredProductList.size(), getActiveCount(filteredProductList));
        tvTotalLabel.setText("Products");
        cardBreadcrumb.setVisibility(View.VISIBLE);

        // Clear search when showing products
        etSearch.setText("");
    }

    private void navigateBack() {
        switch (currentLevel) {
            case "subcategories":
                showCategories();
                break;
            case "products":
                loadSubcategories(selectedCategoryId);
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
    }

    private int getActiveCount(List<?> items) {
        int activeCount = 0;
        for (Object item : items) {
            if (item instanceof Category) {
                if ("1".equals(((Category) item).getStatus())) activeCount++;
            } else if (item instanceof Subcategory) {
                if ("1".equals(((Subcategory) item).getStatus())) activeCount++;
            } else if (item instanceof Product) {
                if ("1".equals(((Product) item).getStatus())) activeCount++;
            }
        }
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
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        rvItems.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showAddDialog() {
        switch (currentLevel) {
            case "categories":
                Toast.makeText(this, "Add Category", Toast.LENGTH_SHORT).show();
                break;
            case "subcategories":
                Toast.makeText(this, "Add Subcategory to " + selectedCategoryName, Toast.LENGTH_SHORT).show();
                break;
            case "products":
                Toast.makeText(this, "Add Product to " + selectedSubcategoryName, Toast.LENGTH_SHORT).show();
                break;
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
            loadCategories();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}