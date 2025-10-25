package com.shop.gramasandhai.Activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate, tvShopName, orderCount, totalRevenue, pendingOrderCount, completedCount;
    private Toolbar toolbar;
    private ShimmerFrameLayout shimmerFrameLayout;
    private ScrollView mainScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        setupToolbar();
        initializeViews();
        setupClickListeners();
        updateCurrentDate();

        // Show shimmer and load data
        showShimmerLoading();
        dashboardCount(shopId());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            Log.d("TOOLBAR_DEBUG", "Toolbar found, setting as support action bar");
            setSupportActionBar(toolbar);


            // Optional: Enable the title if you want
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(true);
                Log.d("TOOLBAR_DEBUG", "Support action bar set successfully");
            } else {
                Log.e("TOOLBAR_DEBUG", "Support action bar is null");
            }
        } else {
            Log.e("TOOLBAR_DEBUG", "Toolbar is NULL! Check your layout XML");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d("MENU_DEBUG", "=== onCreateOptionsMenu STARTED ===");

        // Inflate the menu
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Debug: Check what items were inflated
        Log.d("MENU_DEBUG", "Menu inflated. Number of items: " + menu.size());
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Log.d("MENU_DEBUG", "Menu item " + i + ": ID=" + item.getItemId() + ", Title=" + item.getTitle());
        }

        Log.d("MENU_DEBUG", "=== onCreateOptionsMenu COMPLETED ===");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("MENU_DEBUG", "=== onOptionsItemSelected CALLED ===");
        Log.d("MENU_DEBUG", "Clicked item ID: " + item.getItemId());
        Log.d("MENU_DEBUG", "Expected logout ID: " + R.id.nav_logout);
        Log.d("MENU_DEBUG", "Item title: " + item.getTitle());

        int id = item.getItemId();

        // Add this debug line to see the actual resource names
        try {
            String resourceName = getResources().getResourceName(id);
            Log.d("MENU_DEBUG", "Resource name: " + resourceName);
        } catch (Exception e) {
            Log.d("MENU_DEBUG", "Resource name not found for ID: " + id);
        }

        if (id == R.id.nav_logout) {
            Log.d("MENU_DEBUG", "*** LOGOUT MENU ITEM CLICKED SUCCESSFULLY! ***");
            showLogoutConfirmation();
            return true; // Return true to indicate we handled the click
        }

        Log.d("MENU_DEBUG", "Menu item not handled, calling super");
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d("MENU_DEBUG", "onPrepareOptionsMenu called");
        return super.onPrepareOptionsMenu(menu);
    }

    private void initializeViews() {

        shimmerFrameLayout = findViewById(R.id.shimmer_view_container); // Add this to your main layout
        shimmerFrameLayout = findViewById(R.id.shimmer_view_container);
        mainScrollView = findViewById(R.id.main_scroll_view);

//        tvDate = findViewById(R.id.tvDate);
//        tvShopName = findViewById(R.id.tvShopName);
        orderCount = findViewById(R.id.orderCount);
        totalRevenue = findViewById(R.id.totalRevenue);
        pendingOrderCount = findViewById(R.id.pendingOrderCount);
        completedCount = findViewById(R.id.completedCount);

        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String name = prefs.getString("name", "");
        toolbar.setTitle(name);
//        tvShopName.setText(name);
    }





    private void showShimmerLoading() {
        mainScrollView.setVisibility(View.GONE);
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.startShimmer();
    }

    private void hideShimmerLoading() {
        shimmerFrameLayout.stopShimmer();
        shimmerFrameLayout.setVisibility(View.GONE);
        mainScrollView.setVisibility(View.VISIBLE);
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        String shopId = prefs.getString("shopId", "");
        return shopId;
    }

    private void showLogoutConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Logout");
        builder.setMessage("Are you sure you want to logout?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performLogout();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Customize button colors
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.red));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.primary_color));
    }

    private void performLogout() {
        // Clear user session
        clearUserSession();

        // Show logout success message
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Redirect to login activity
        redirectToLogin();
    }

    /**
     * Clear user session data from SharedPreferences
     */
    private void clearUserSession() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Clear all user data
        editor.clear();
        editor.apply();

        Log.d("LOGOUT", "User session cleared");
    }

    /**
     * Redirect to login activity
     */
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    public void dashboardCount(String shopId) {
        OkHttpClient client = new OkHttpClient();

        Log.d("DASHBOARD_API", "Shop ID: " + shopId);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId + "/dashboard")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideShimmerLoading();
                    Toast.makeText(MainActivity.this, "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("DASHBOARD_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    hideShimmerLoading(); // Hide shimmer when data is loaded

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            // Success logic
                            String pending = responseObject.optString("pending", "");
                            String process = responseObject.optString("process", "");
                            String totalorder = responseObject.optString("totalorder", "");
                            String completed = responseObject.optString("completed", "");
                            String revenue = responseObject.optString("revenue", "");

                            // Update UI on main thread
                            orderCount.setText(totalorder);
                            totalRevenue.setText(revenue);
                            completedCount.setText(completed);
                            pendingOrderCount.setText(pending);

                        } else {
                            if ("Invalid credentials".equals(responseObject.optString("error"))) {
                                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
                            }
                        }

                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        Toast.makeText(MainActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.startShimmer();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.stopShimmer();
        }
    }

    private void setupClickListeners() {
        // Quick Actions Click Listeners
        setupCardClickListener(R.id.card_products, ProductManageActivity.class);
        setupCardClickListener(R.id.card_orders, OrdersManageActivity.class); // Replace with your actual Orders activity
//        setupCardClickListener(R.id.card_inventory, InventoryActivity.class); // Replace with your actual Inventory activity

        // Add more cards as needed
        // setupCardClickListener(R.id.card_customers, CustomersActivity.class);
        // setupCardClickListener(R.id.card_analytics, AnalyticsActivity.class);
        // setupCardClickListener(R.id.card_settings, SettingsActivity.class);
    }

    private void setupCardClickListener(int cardId, Class<?> targetActivity) {
        MaterialCardView card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(view -> {
                try {
                    Intent intent = new Intent(MainActivity.this, targetActivity);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("NAVIGATION", "Error starting activity: " + e.getMessage());
                    Toast.makeText(MainActivity.this, "Feature not available yet", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void updateCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy • hh:mm a", Locale.getDefault());
        String currentDate = sdf.format(new Date());
//        tvDate.setText(currentDate);
    }
}