package com.shop.gramasandhai.Activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.shop.gramasandhai.Adapter.OrdersAdapter;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.Model.Order;
import com.shop.gramasandhai.Model.Variant;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OrdersManageActivity extends AppCompatActivity {

    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<Order> ordersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private TabLayout tabLayout;

    private String currentFilter = "ALL"; // ALL, PENDING, PROCESSING, COMPLETED

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders_manage);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupTabs();
        setupListeners();

        FetchOrders();
    }

    private void initializeViews() {
        ordersRecyclerView = findViewById(R.id.ordersRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyStateText = findViewById(R.id.emptyStateText);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        ordersAdapter = new OrdersAdapter(filteredOrdersList,
                this::onOrderClicked,
                this::onStatusUpdateClicked,
                this::onCancelClicked); // Add cancel listener
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    // Add the cancel click handler method:
    private void onCancelClicked(Order order) {
        showCancelConfirmation(order);
    }

    // Add the cancel confirmation dialog:
    private void showCancelConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel order #" + order.getOrderId() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    // Call setStatus with CNL and order ID
                    setStatus("CNL", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void setStatus(String status, int id) {
        progressBar.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("order_status", status)
                .build();

        Log.d("STATUS_UPDATE", "Updating order " + id + " to status: " + status);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/orderUpdate/" + id)
                .post(requestBody)
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrdersManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e("API_ERROR", "Status update failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("STATUS_UPDATE_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            // Success - refresh orders list
                            Toast.makeText(OrdersManageActivity.this,
                                    "Order status updated successfully", Toast.LENGTH_SHORT).show();

                            // Auto-switch to appropriate tab based on new status
                            autoSwitchTabAfterStatusUpdate(status);
                            FetchOrders(); // Refresh the list
                        } else {
                            String errorMessage = responseObject.optString("message", "Failed to update order status");
                            Toast.makeText(OrdersManageActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        Toast.makeText(OrdersManageActivity.this,
                                "Error updating order status", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void autoSwitchTabAfterStatusUpdate(String newStatus) {
        switch (newStatus) {
            case "PRS": // Processing
                // Switch to Processing tab
                if (tabLayout.getTabCount() > 2) {
                    TabLayout.Tab processingTab = tabLayout.getTabAt(2);
                    if (processingTab != null) {
                        processingTab.select();
                    }
                }
                break;
            case "COM": // Completed
                // Switch to Completed tab
                if (tabLayout.getTabCount() > 3) {
                    TabLayout.Tab completedTab = tabLayout.getTabAt(3);
                    if (completedTab != null) {
                        completedTab.select();
                    }
                }
                break;
            case "CNL": // Cancelled
                // Stay on current tab or switch to All tab
                if (tabLayout.getTabCount() > 0) {
                    TabLayout.Tab allTab = tabLayout.getTabAt(0);
                    if (allTab != null) {
                        allTab.select();
                    }
                }
                break;
        }
    }

    private void setupTabs() {
        // Tabs are defined in XML, just setup listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: currentFilter = "ALL"; break;
                    case 1: currentFilter = "PENDING"; break;
                    case 2: currentFilter = "PROCESSING"; break;
                    case 3: currentFilter = "COMPLETED"; break;
                }
                filterOrders();
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.orders_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.action_refresh) {
            FetchOrders();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onOrderClicked(Order order) {
        showOrderDetails(order);
    }

    private void onStatusUpdateClicked(Order order) {
        // Direct status update based on current status
        String currentStatus = order.getOrderStatus();

        switch (currentStatus) {
            case "PENDING":
                // Show confirmation for processing
                showProcessConfirmation(order);
                break;
            case "PROCESSING":
                // Show confirmation for completing
                showCompleteConfirmation(order);
                break;
            case "COMPLETED":
            case "CANCELLED":
                // For completed/cancelled orders, just show details
                showOrderDetails(order);
                break;
        }
    }

    private void showProcessConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Process Order")
                .setMessage("Are you sure you want to process order #" + order.getOrderId() + "?")
                .setPositiveButton("Yes, Process", (dialog, which) -> {
                    // Call setStatus with PRS and order ID
                    setStatus("PRS", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCompleteConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Complete Order")
                .setMessage("Are you sure you want to mark order #" + order.getOrderId() + " as completed?")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    // Call setStatus with COM and order ID
                    setStatus("COM", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrderDetails(Order order) {
        // Build detailed message with variants information
        StringBuilder message = new StringBuilder();
        message.append("Customer: ").append(order.getCustomerName()).append("\n")
                .append("Amount: ₹").append(order.getAmount()).append("\n")
                .append("Status: ").append(order.getOrderStatus()).append("\n")
                .append("Date: ").append(order.getOrderedDate()).append("\n")
                .append("Address: ").append(order.getShippingAddress()).append(", ").append(order.getCity()).append("\n")
                .append("Payment: ").append(order.getPaymentMethod()).append("\n")
                .append("Items: ").append(order.getItemsCount()).append(" products\n\n");

        // Add variants details
        if (order.getVariants() != null && !order.getVariants().isEmpty()) {
            message.append("Order Items:\n");
            for (Variant variant : order.getVariants()) {
                message.append("• ").append(variant.getProdName())
                        .append(" (Qty: ").append(variant.getProdQty())
                        .append(", ₹").append(variant.getProdPrice()).append(")\n");
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Order Details - #" + order.getOrderId())
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void filterOrders() {
        filteredOrdersList.clear();

        if (currentFilter.equals("ALL")) {
            filteredOrdersList.addAll(ordersList);
        } else {
            for (Order order : ordersList) {
                if (order.getOrderStatus().equalsIgnoreCase(currentFilter)) {
                    filteredOrdersList.add(order);
                }
            }
        }

        ordersAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredOrdersList.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            ordersRecyclerView.setVisibility(View.GONE);

            // Show appropriate empty state message based on current filter
            switch (currentFilter) {
                case "PENDING":
                    emptyStateText.setText("No pending orders");
                    break;
                case "PROCESSING":
                    emptyStateText.setText("No processing orders");
                    break;
                case "COMPLETED":
                    emptyStateText.setText("No completed orders");
                    break;
                default:
                    emptyStateText.setText("No orders found");
            }
        } else {
            emptyStateText.setVisibility(View.GONE);
            ordersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void FetchOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        Log.d("OrdersFetch", "Fetching orders for shop: " + shopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + shopId() + "/orders")
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrdersManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateEmptyState();
                });
                Log.e("API_ERROR", "Request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d("Orders_RESPONSE", responseBodyString);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseOrdersData(responseObject.getJSONArray("orders"));
                        } else {
                            Toast.makeText(OrdersManageActivity.this,
                                    "Failed to load orders", Toast.LENGTH_LONG).show();
                            updateEmptyState();
                        }
                    } catch (JSONException e) {
                        Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                        Toast.makeText(OrdersManageActivity.this,
                                "Error parsing data", Toast.LENGTH_LONG).show();
                        updateEmptyState();
                    }
                });
            }
        });
    }

    private void parseOrdersData(JSONArray ordersArray) throws JSONException {
        ordersList.clear();

        for (int i = 0; i < ordersArray.length(); i++) {
            JSONObject orderObj = ordersArray.getJSONObject(i);
            Order order = new Order();

            // Parse order data
            order.setId(orderObj.getString("id"));
            order.setOrderId(orderObj.getString("order_id"));
            order.setAmount(orderObj.getString("amount"));
            order.setOrderStatus(mapStatus(orderObj.getString("order_status")));
            order.setPaymentMethod(orderObj.getString("payment_method"));
            order.setOrderedDate(formatDate(orderObj.getString("ordered_date")));
            order.setCustomerName(orderObj.getJSONObject("customer").getString("name"));
            order.setShippingAddress(orderObj.getString("shipping_address"));
            order.setCity(orderObj.getString("city"));

            // Parse variants
            JSONArray variants = orderObj.getJSONArray("variants");
            List<String> productNames = new ArrayList<>();
            List<Variant> variantList = new ArrayList<>();

            for (int j = 0; j < variants.length(); j++) {
                JSONObject variant = variants.getJSONObject(j);

                // Create Variant object
                Variant variantObj = new Variant();
                variantObj.setId(variant.getString("id"));
                variantObj.setOrdTbId(variant.getString("ord_tb_id"));
                variantObj.setOrderId(variant.getString("order_id"));
                variantObj.setProdId(variant.getString("prod_id"));
                variantObj.setProdName(variant.getString("prod_name"));
                variantObj.setProdQty(variant.getString("prod_qty"));
                variantObj.setProdPrice(variant.getString("prod_price"));
                variantObj.setWeight(variant.getString("weight"));
                variantObj.setImageName(variant.optString("imagename", ""));

                variantList.add(variantObj);
                productNames.add(variant.getString("prod_name"));
            }

            order.setProducts(productNames);
            order.setVariants(variantList); // Set variants list
            order.setItemsCount(variants.length());

            ordersList.add(order);
        }

        filterOrders();
    }

    private String mapStatus(String status) {
        switch (status) {
            case "1": return "PENDING";
            case "PRS": return "PROCESSING";
            case "COM": return "COMPLETED";
            case "CNL": return "CANCELLED";
            default: return "PENDING";
        }
    }

    private String formatDate(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return dateString;
        }
    }

    private String shopId() {
        // Implement your shop ID retrieval logic
        // You can get this from SharedPreferences or your app's session management
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }
}