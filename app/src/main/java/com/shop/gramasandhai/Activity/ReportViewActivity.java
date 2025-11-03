package com.shop.gramasandhai.Activity;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.shop.gramasandhai.Adapter.OrderAdapter;
import com.shop.gramasandhai.Model.Orders;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ReportViewActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView recyclerViewOrders;
    private TextView tvCODRevenue, tvOnlineRevenue;
    private OrderAdapter orderAdapter;
    private List<Orders> orderList = new ArrayList<>();

    // UI Components
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private FloatingActionButton fabRefresh;

    // Shimmer and Content Views
    private ShimmerFrameLayout shimmerLayout;
    private NestedScrollView contentScrollView;

    // Summary Cards
    private TextView tvCompletedOrders, tvTotalRevenue, tvOrderCount;

    // Filters
    private MaterialButton btnDateFilter, btnStatusFilter;
    private Chip chipDateFilter, chipStatusFilter;
    private View layoutActiveFilters;

    // Date Range Components
    private MaterialCardView cardDateRange;
    private TextInputEditText etStartDate, etEndDate;
    private MaterialButton btnApplyDateRange, btnCancelDateRange;

    // Download Components
    private MaterialCardView cardDownload;
    private RadioGroup radioGroupDownloadType;
    private RadioButton radioToday, radioThisMonth, radioCustom;
    private TextInputEditText etDownloadStartDate, etDownloadEndDate;
    private View layoutCustomDate;
    private MaterialButton btnDownloadReport, btnCancelDownload;

    // Date pickers
    private Calendar calendarStart, calendarEnd, calendarDownloadStart, calendarDownloadEnd;
    private SimpleDateFormat dateFormatter;

    // Filter states
    private String currentStatusFilter = "COM";
    private String selectedStartDate = "";
    private String selectedEndDate = "";

    private static final String TAG = "ReportViewActivity";
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_view);

        Log.d(TAG, "onCreate: Activity created");
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupClickListeners();
        initializeDatePickers();

        // Show shimmer and load data with slight delay
        showShimmerLoading();
        handler.postDelayed(this::loadReport, 1000);
    }

    private void initializeViews() {
        Log.d(TAG, "initializeViews: Initializing views");

        toolbar = findViewById(R.id.toolbar);
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        fabRefresh = findViewById(R.id.fabRefresh);

        // Shimmer and Content Views
        shimmerLayout = findViewById(R.id.shimmerLayout);
        contentScrollView = findViewById(R.id.contentScrollView);

        // Revenue TextViews
        tvCODRevenue = findViewById(R.id.tvCODRevenue);
        tvOnlineRevenue = findViewById(R.id.tvOnlineRevenue);

        // Summary cards
        tvCompletedOrders = findViewById(R.id.tvCompletedOrders);
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue);
        tvOrderCount = findViewById(R.id.tvOrderCount);

        // Filters
        btnDateFilter = findViewById(R.id.btnDateFilter);
        btnStatusFilter = findViewById(R.id.btnStatusFilter);
        chipDateFilter = findViewById(R.id.chipDateFilter);
        chipStatusFilter = findViewById(R.id.chipStatusFilter);
        layoutActiveFilters = findViewById(R.id.layoutActiveFilters);

        // Date Range
        cardDateRange = findViewById(R.id.cardDateRange);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        btnApplyDateRange = findViewById(R.id.btnApplyDateRange);
        btnCancelDateRange = findViewById(R.id.btnCancelDateRange);

        // Download
        cardDownload = findViewById(R.id.cardDownload);
        radioGroupDownloadType = findViewById(R.id.radioGroupDownloadType);
        radioToday = findViewById(R.id.radioToday);
        radioThisMonth = findViewById(R.id.radioThisMonth);
        radioCustom = findViewById(R.id.radioCustom);
        etDownloadStartDate = findViewById(R.id.etDownloadStartDate);
        etDownloadEndDate = findViewById(R.id.etDownloadEndDate);
        layoutCustomDate = findViewById(R.id.layoutCustomDate);
        btnDownloadReport = findViewById(R.id.btnDownloadReport);
        btnCancelDownload = findViewById(R.id.btnCancelDownload);

        // Set default status filter to show only completed orders
        currentStatusFilter = "COM";
        btnStatusFilter.setText("Completed");
        chipStatusFilter.setText("Completed");
        chipStatusFilter.setVisibility(View.VISIBLE);
        updateActiveFiltersVisibility();

        Log.d(TAG, "initializeViews: Views initialized successfully");
    }

    private void showShimmerLoading() {
        shimmerLayout.setVisibility(View.VISIBLE);
        shimmerLayout.startShimmer();
        contentScrollView.setVisibility(View.GONE);
    }

    private void hideShimmerLoading() {
        shimmerLayout.stopShimmer();
        shimmerLayout.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.VISIBLE);
    }

    private void initializeDatePickers() {
        calendarStart = Calendar.getInstance();
        calendarEnd = Calendar.getInstance();
        calendarDownloadStart = Calendar.getInstance();
        calendarDownloadEnd = Calendar.getInstance();
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Set default dates for download (today)
        String today = dateFormatter.format(new Date());
        if (etDownloadStartDate != null) {
            etDownloadStartDate.setText(today);
        }
        if (etDownloadEndDate != null) {
            etDownloadEndDate.setText(today);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.report_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_download) {
            showDownloadCard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        Log.d(TAG, "Setting up RecyclerView");

        orderAdapter = new OrderAdapter(new ArrayList<>(), new OrderAdapter.OrderClickListener() {
            @Override
            public void onOrderClick(Orders order) {
                showOrderDetails(order);
            }

            @Override
            public void onTrackOrder(Orders order) {
                trackOrder(order);
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewOrders.setLayoutManager(layoutManager);
        recyclerViewOrders.setAdapter(orderAdapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    private void setupClickListeners() {
        fabRefresh.setOnClickListener(v -> {
            Log.d(TAG, "Refresh button clicked");
            showShimmerLoading();
            handler.postDelayed(this::loadReport, 500);
        });

        // Filter buttons
        btnStatusFilter.setOnClickListener(v -> showStatusFilterDialog());
        btnDateFilter.setOnClickListener(v -> showDateFilterDialog());

        // Date range buttons
        btnApplyDateRange.setOnClickListener(v -> applyDateRangeFilter());
        btnCancelDateRange.setOnClickListener(v -> hideDateRangeCard());

        // Download buttons
        btnDownloadReport.setOnClickListener(v -> downloadReport());
        btnCancelDownload.setOnClickListener(v -> hideDownloadCard());

        // Date picker listeners
        etStartDate.setOnClickListener(v -> showStartDatePicker());
        etEndDate.setOnClickListener(v -> showEndDatePicker());
        etDownloadStartDate.setOnClickListener(v -> showDownloadStartDatePicker());
        etDownloadEndDate.setOnClickListener(v -> showDownloadEndDatePicker());

        // Chip close listeners
        chipDateFilter.setOnCloseIconClickListener(v -> {
            selectedStartDate = "";
            selectedEndDate = "";
            chipDateFilter.setVisibility(View.GONE);
            updateActiveFiltersVisibility();
            applyFilters();
        });

        chipStatusFilter.setOnCloseIconClickListener(v -> {
            currentStatusFilter = "COM"; // Reset to completed only
            chipStatusFilter.setVisibility(View.VISIBLE);
            btnStatusFilter.setText("Completed");
            updateActiveFiltersVisibility();
            applyFilters();
        });

        // Radio group listener
        radioGroupDownloadType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioToday) {
                layoutCustomDate.setVisibility(View.GONE);
                String today = dateFormatter.format(new Date());
                etDownloadStartDate.setText(today);
                etDownloadEndDate.setText(today);
            } else if (checkedId == R.id.radioThisMonth) {
                layoutCustomDate.setVisibility(View.GONE);
                String firstDayOfMonth = getFirstDayOfMonth();
                String lastDayOfMonth = dateFormatter.format(new Date());
                etDownloadStartDate.setText(firstDayOfMonth);
                etDownloadEndDate.setText(lastDayOfMonth);
            } else if (checkedId == R.id.radioCustom) {
                layoutCustomDate.setVisibility(View.VISIBLE);
            }
        });
    }

    private String getFirstDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return dateFormatter.format(calendar.getTime());
    }

    private void applyFilters() {
        Log.d(TAG, "Applying filters - Status: " + currentStatusFilter +
                ", Date Range: " + selectedStartDate + " to " + selectedEndDate);
        Log.d(TAG, "Total orders before filter: " + orderList.size());

        List<Orders> tempFilteredList = new ArrayList<>();

        for (Orders order : orderList) {
            boolean statusMatch = order.getOrderStatus() != null &&
                    order.getOrderStatus().equalsIgnoreCase(currentStatusFilter);

            boolean dateMatch = selectedStartDate.isEmpty() || selectedEndDate.isEmpty() ||
                    isDateInRange(order.getCreatedAt(), selectedStartDate, selectedEndDate);

            if (statusMatch && dateMatch) {
                tempFilteredList.add(order);
            }
        }

        Log.d(TAG, "Filtered orders: " + tempFilteredList.size());

        runOnUiThread(() -> {
            orderAdapter.updateData(tempFilteredList);
            updateOrderCount(tempFilteredList.size());
            updateSummaryCards(tempFilteredList);
            checkEmptyState(tempFilteredList.size());
            Log.d(TAG, "Adapter notified with " + tempFilteredList.size() + " orders");
        });
    }

    private void updateOrderCount(int count) {
        runOnUiThread(() -> {
            if (tvOrderCount != null) {
                tvOrderCount.setText(count + " orders");
            }
        });
    }

    private void updateSummaryCards(List<Orders> orders) {
        runOnUiThread(() -> {
            try {
                int completedOrders = orders.size();
                double totalRevenue = 0.0;
                double codRevenue = 0.0;
                double onlineRevenue = 0.0;

                for (Orders order : orders) {
                    try {
                        double amount = Double.parseDouble(order.getAmount());
                        totalRevenue += amount;

                        if (order.getPaymentMethod() != null) {
                            if (order.getPaymentMethod().equalsIgnoreCase("COD")) {
                                codRevenue += amount;
                            } else {
                                onlineRevenue += amount;
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid amount: " + order.getAmount());
                    }
                }

                if (tvCompletedOrders != null) {
                    tvCompletedOrders.setText(String.valueOf(completedOrders));
                }
                if (tvTotalRevenue != null) {
                    tvTotalRevenue.setText("₹" + String.format(Locale.US, "%.2f", totalRevenue));
                }
                if (tvCODRevenue != null) {
                    tvCODRevenue.setText("₹" + String.format(Locale.US, "%.2f", codRevenue));
                }
                if (tvOnlineRevenue != null) {
                    tvOnlineRevenue.setText("₹" + String.format(Locale.US, "%.2f", onlineRevenue));
                }

                Log.d(TAG, "Summary updated - Completed: " + completedOrders +
                        ", Total Revenue: " + totalRevenue +
                        ", COD Revenue: " + codRevenue +
                        ", Online Revenue: " + onlineRevenue);
            } catch (Exception e) {
                Log.e(TAG, "Error in updateSummaryCards: " + e.getMessage(), e);
            }
        });
    }

    private void checkEmptyState(int count) {
        runOnUiThread(() -> {
            try {
                if (count == 0) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    recyclerViewOrders.setVisibility(View.GONE);
                    Log.d(TAG, "No orders to display - showing empty state");
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    recyclerViewOrders.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Displaying " + count + " orders");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in checkEmptyState: " + e.getMessage(), e);
            }
        });
    }

    private boolean isDateInRange(String orderDate, String startDate, String endDate) {
        try {
            if (startDate.isEmpty() || endDate.isEmpty() || orderDate == null || orderDate.isEmpty()) {
                return true;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date order = sdf.parse(orderDate.split(" ")[0]);
            Date start = sdf.parse(startDate);
            Date end = sdf.parse(endDate);

            return !order.before(start) && !order.after(end);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + e.getMessage());
            return false;
        }
    }

    private void downloadReport() {
        String startDate, endDate;

        if (radioToday.isChecked()) {
            String today = dateFormatter.format(new Date());
            startDate = today;
            endDate = today;
        } else if (radioThisMonth.isChecked()) {
            startDate = getFirstDayOfMonth();
            endDate = dateFormatter.format(new Date());
        } else {
            startDate = etDownloadStartDate.getText().toString().trim();
            endDate = etDownloadEndDate.getText().toString().trim();

            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        generatePrintReport(startDate, endDate);
        hideDownloadCard();
    }

    private void generatePrintReport(String startDate, String endDate) {
        showProgressBar(true);

        List<Orders> reportOrders = new ArrayList<>();
        for (Orders order : orderList) {
            if (isDateInRange(order.getCreatedAt(), startDate, endDate) &&
                    "COM".equals(order.getOrderStatus())) {
                reportOrders.add(order);
            }
        }

        int totalOrders = reportOrders.size();
        double totalRevenue = 0.0;
        double codRevenue = 0.0;
        double onlineRevenue = 0.0;

        for (Orders order : reportOrders) {
            try {
                double amount = Double.parseDouble(order.getAmount());
                totalRevenue += amount;

                if (order.getPaymentMethod() != null) {
                    if (order.getPaymentMethod().equalsIgnoreCase("COD")) {
                        codRevenue += amount;
                    } else {
                        onlineRevenue += amount;
                    }
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid amount: " + order.getAmount());
            }
        }

        String htmlContent = generateHTMLContent(startDate, endDate, reportOrders,
                totalOrders, totalRevenue, codRevenue, onlineRevenue);

        printHTMLContent(htmlContent, "Completed_Order_Report_" + startDate + "_to_" + endDate);

        showProgressBar(false);
    }

    private String generateHTMLContent(String startDate, String endDate, List<Orders> orders,
                                       int totalOrders, double totalRevenue,
                                       double codRevenue, double onlineRevenue) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }");
        html.append("h1 { color: #333; text-align: center; }");
        html.append("h2 { color: #666; border-bottom: 2px solid #333; padding-bottom: 5px; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        html.append("th { background-color: #f2f2f2; font-weight: bold; }");
        html.append(".summary { background-color: #f9f9f9; padding: 15px; border-radius: 5px; margin: 10px 0; }");
        html.append(".total { font-weight: bold; color: #2196F3; }");
        html.append(".revenue-breakdown { display: flex; justify-content: space-between; margin-top: 10px; }");
        html.append(".revenue-item { flex: 1; padding: 10px; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        String reportTitle;
        if (startDate.equals(endDate)) {
            reportTitle = "TODAY'S COMPLETED ORDERS REPORT - " + startDate;
        } else if (startDate.equals(getFirstDayOfMonth()) && endDate.equals(dateFormatter.format(new Date()))) {
            reportTitle = "THIS MONTH COMPLETED ORDERS REPORT";
        } else {
            reportTitle = "COMPLETED ORDERS REPORT";
        }

        html.append("<h1>").append(reportTitle).append("</h1>");
        html.append("<div class='summary'>");
        html.append("<p><strong>Date Range:</strong> ").append(startDate).append(" to ").append(endDate).append("</p>");
        html.append("</div>");

        html.append("<h2>SUMMARY</h2>");
        html.append("<div class='summary'>");
        html.append("<p><strong>Total Completed Orders:</strong> ").append(totalOrders).append("</p>");
        html.append("<p class='total'><strong>Total Revenue:</strong> ₹").append(String.format(Locale.US, "%.2f", totalRevenue)).append("</p>");

        html.append("<div class='revenue-breakdown'>");
        html.append("<div class='revenue-item'>");
        html.append("<p><strong>COD Revenue:</strong><br>₹").append(String.format(Locale.US, "%.2f", codRevenue)).append("</p>");
        html.append("</div>");
        html.append("<div class='revenue-item'>");
        html.append("<p><strong>Online Revenue:</strong><br>₹").append(String.format(Locale.US, "%.2f", onlineRevenue)).append("</p>");
        html.append("</div>");
        html.append("</div>");

        html.append("</div>");

        html.append("<h2>ORDER DETAILS</h2>");
        if (totalOrders > 0) {
            html.append("<table>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th>Order ID</th>");
            html.append("<th>Date</th>");
            html.append("<th>Customer</th>");
            html.append("<th>Amount</th>");
            html.append("<th>Payment Method</th>");
            html.append("</tr>");
            html.append("</thead>");
            html.append("<tbody>");

            for (Orders order : orders) {
                html.append("<tr>");
                html.append("<td>").append(order.getOrderId() != null ? order.getOrderId() : "N/A").append("</td>");
                html.append("<td>").append(order.getCreatedAt() != null ? order.getCreatedAt().split(" ")[0] : "N/A").append("</td>");
                html.append("<td>").append(order.getReceiverName() != null ? order.getReceiverName() : "N/A").append("</td>");
                html.append("<td>₹").append(order.getAmount() != null ? order.getAmount() : "0").append("</td>");
                html.append("<td>").append(order.getPaymentMethod() != null ? order.getPaymentMethod() : "N/A").append("</td>");
                html.append("</tr>");
            }

            html.append("</tbody>");
            html.append("</table>");
        } else {
            html.append("<p style='text-align: center; color: #666; padding: 20px;'>No completed orders found in the selected date range.</p>");
        }

        html.append("<div style='margin-top: 30px; text-align: center; color: #666;'>");
        html.append("<p>Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private void printHTMLContent(String htmlContent, String jobName) {
        WebView webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (printManager != null) {
                    PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);
                    PrintJob printJob = printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
                    Toast.makeText(ReportViewActivity.this, "Printing started...", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReportViewActivity.this, "Print service not available", Toast.LENGTH_SHORT).show();
                }
            }
        });

        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null);
    }

    private void showStatusFilterDialog() {
        String[] statuses = {"Completed"};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Filter by Status")
                .setItems(statuses, (dialog, which) -> {
                    currentStatusFilter = "COM";
                    chipStatusFilter.setText("Completed");
                    chipStatusFilter.setVisibility(View.VISIBLE);
                    btnStatusFilter.setText("Completed");
                    updateActiveFiltersVisibility();
                    applyFilters();
                })
                .show();
    }

    private void updateActiveFiltersVisibility() {
        boolean hasActiveFilters = chipDateFilter.getVisibility() == View.VISIBLE ||
                chipStatusFilter.getVisibility() == View.VISIBLE;
        layoutActiveFilters.setVisibility(hasActiveFilters ? View.VISIBLE : View.GONE);
    }

    private String shopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }

    private void loadReport() {
        showProgressBar(true);

        OkHttpClient client = new OkHttpClient();
        String url = "https://gramasandhai.in/services/index.php/order-list?shopId=" + shopId();
        Log.d(TAG, "Loading orders from: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .addHeader("X-Api", "SEC195C79FC4CCB09B48AA8")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    hideShimmerLoading();
                    showProgressBar(false);
                    Toast.makeText(ReportViewActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    checkEmptyState(0);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "API Response received, length: " + responseBodyString.length());
                Log.d("API Response received" , responseBodyString);

                runOnUiThread(() -> {
                    hideShimmerLoading();
                    showProgressBar(false);
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBodyString);
                        if (response.isSuccessful() && "success".equals(jsonResponse.optString("status"))) {
                            parseOrdersData(jsonResponse);
                            applyFilters();
                            Log.d(TAG, "Orders loaded and filtered: " + orderList.size());
                        } else {
                            String errorMsg = jsonResponse.optString("message", "Unknown error");
                            Toast.makeText(ReportViewActivity.this,
                                    "Failed to load orders: " + errorMsg, Toast.LENGTH_LONG).show();
                            checkEmptyState(0);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Error parsing response: " + e.getMessage());
                        Toast.makeText(ReportViewActivity.this,
                                "Error parsing orders data", Toast.LENGTH_LONG).show();
                        checkEmptyState(0);
                    }
                });
            }
        });
    }

    private void showProgressBar(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        });
    }

    private void parseOrdersData(JSONObject jsonResponse) throws JSONException {
        orderList.clear();

        if (jsonResponse.has("orders")) {
            JSONArray ordersArray = jsonResponse.getJSONArray("orders");
            Log.d(TAG, "Found " + ordersArray.length() + " orders in API response");

            for (int i = 0; i < ordersArray.length(); i++) {
                JSONObject orderObj = ordersArray.getJSONObject(i);

                Orders order = new Orders();
                order.setId(orderObj.optString("id", ""));
                order.setOrderId(orderObj.optString("order_id", ""));
                order.setShopId(orderObj.optString("shop_id", ""));
                order.setUserId(orderObj.optString("user_id", ""));
                order.setAmount(orderObj.optString("amount", "0"));
                order.setShippingAddress(orderObj.optString("shipping_address", ""));
                order.setCity(orderObj.optString("city", ""));
                order.setReceiverPhoneNo(orderObj.optString("receiver_phone_no", ""));
                order.setAddressId(orderObj.optString("address_id", ""));
                order.setOrderStatus(orderObj.optString("order_status", ""));
                order.setDeliveryStatus(orderObj.optString("delivery_status", ""));
                order.setPlatformFee(orderObj.optString("platformfee", "0"));
                order.setGstFee(orderObj.optString("gstfee", "0"));
                order.setDiscount(orderObj.optString("discount", "0"));
                order.setDeliveryFee(orderObj.optString("deliveryFee", "0"));
                order.setPaymentMethod(orderObj.optString("payment_method", ""));
                order.setTransactionId(orderObj.optString("transaction_id", ""));
                order.setInvoiceNo(orderObj.optString("invoice_no", ""));
                order.setInvoiceDate(orderObj.optString("invoice_date", ""));
                order.setOrderedDate(orderObj.optString("ordered_date", ""));
                order.setTimeSlot(orderObj.optString("time_slot", ""));
                order.setDeliveryInfo(orderObj.optString("delivery_info", ""));
                order.setOrderNotes(orderObj.optString("order_notes", ""));
                order.setDeliveryDate(orderObj.optString("delivery_date", ""));
                order.setStatus(orderObj.optString("status", ""));
                order.setCreatedAt(orderObj.optString("created_at", ""));
                order.setReceiverName(orderObj.optString("receiver_name", ""));

                orderList.add(order);
            }
        }

        Log.d(TAG, "Total orders parsed: " + orderList.size());
    }

    private void showOrderDetails(Orders order) {
        Toast.makeText(this, "View details for order: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
    }

    private void trackOrder(Orders order) {
        Toast.makeText(this, "Track order: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
    }

    private void showDateFilterDialog() {
        String[] dateRanges = {"Today", "Yesterday", "Last 7 Days", "Last 30 Days", "Custom Range", "Clear Filter"};
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Date Range")
                .setItems(dateRanges, (dialog, which) -> {
                    if (which == 4) {
                        showDateRangeCard();
                    } else if (which == 5) {
                        clearDateFilter();
                    } else {
                        applyPredefinedDateFilter(which);
                    }
                })
                .show();
    }

    private void applyPredefinedDateFilter(int filterIndex) {
        Calendar calendar = Calendar.getInstance();
        String startDate = "";
        String endDate = dateFormatter.format(calendar.getTime());

        switch (filterIndex) {
            case 0: // Today
                startDate = endDate;
                break;
            case 1: // Yesterday
                calendar.add(Calendar.DAY_OF_YEAR, -1);
                startDate = dateFormatter.format(calendar.getTime());
                endDate = startDate;
                break;
            case 2: // Last 7 Days
                calendar.add(Calendar.DAY_OF_YEAR, -6);
                startDate = dateFormatter.format(calendar.getTime());
                break;
            case 3: // Last 30 Days
                calendar.add(Calendar.DAY_OF_YEAR, -29);
                startDate = dateFormatter.format(calendar.getTime());
                break;
        }

        selectedStartDate = startDate;
        selectedEndDate = endDate;

        chipDateFilter.setText(getFilterDisplayText(filterIndex));
        chipDateFilter.setVisibility(View.VISIBLE);
        updateActiveFiltersVisibility();
        applyFilters();
    }

    private String getFilterDisplayText(int filterIndex) {
        switch (filterIndex) {
            case 0: return "Today";
            case 1: return "Yesterday";
            case 2: return "Last 7 Days";
            case 3: return "Last 30 Days";
            default: return "Custom";
        }
    }

    private void showDateRangeCard() {
        cardDateRange.setVisibility(View.VISIBLE);
        cardDownload.setVisibility(View.GONE);
    }

    private void hideDateRangeCard() {
        cardDateRange.setVisibility(View.GONE);
    }

    private void showDownloadCard() {
        cardDownload.setVisibility(View.VISIBLE);
        cardDateRange.setVisibility(View.GONE);
        radioToday.setChecked(true);

        String today = dateFormatter.format(new Date());
        etDownloadStartDate.setText(today);
        etDownloadEndDate.setText(today);
    }

    private void hideDownloadCard() {
        cardDownload.setVisibility(View.GONE);
    }

    private void clearDateFilter() {
        selectedStartDate = "";
        selectedEndDate = "";
        chipDateFilter.setVisibility(View.GONE);
        updateActiveFiltersVisibility();
        applyFilters();
    }

    private void applyDateRangeFilter() {
        String startDate = etStartDate.getText().toString().trim();
        String endDate = etEndDate.getText().toString().trim();

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedStartDate = startDate;
        selectedEndDate = endDate;

        chipDateFilter.setText("Custom: " + startDate + " to " + endDate);
        chipDateFilter.setVisibility(View.VISIBLE);
        updateActiveFiltersVisibility();
        applyFilters();
        hideDateRangeCard();
    }

    // Date Picker Methods
    private void showStartDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarStart.set(year, month, dayOfMonth);
            etStartDate.setText(dateFormatter.format(calendarStart.getTime()));
        }, calendarStart.get(Calendar.YEAR), calendarStart.get(Calendar.MONTH), calendarStart.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showEndDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarEnd.set(year, month, dayOfMonth);
            etEndDate.setText(dateFormatter.format(calendarEnd.getTime()));
        }, calendarEnd.get(Calendar.YEAR), calendarEnd.get(Calendar.MONTH), calendarEnd.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showDownloadStartDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarDownloadStart.set(year, month, dayOfMonth);
            etDownloadStartDate.setText(dateFormatter.format(calendarDownloadStart.getTime()));
        }, calendarDownloadStart.get(Calendar.YEAR), calendarDownloadStart.get(Calendar.MONTH), calendarDownloadStart.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void showDownloadEndDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendarDownloadEnd.set(year, month, dayOfMonth);
            etDownloadEndDate.setText(dateFormatter.format(calendarDownloadEnd.getTime()));
        }, calendarDownloadEnd.get(Calendar.YEAR), calendarDownloadEnd.get(Calendar.MONTH), calendarDownloadEnd.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }
}