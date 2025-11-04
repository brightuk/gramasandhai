package com.shop.gramasandhai.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.shop.gramasandhai.Adapter.OrdersAdapter;
import com.shop.gramasandhai.Attributes.Attributes;
import com.shop.gramasandhai.Model.Order;
import com.shop.gramasandhai.Model.Variant;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
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

    private static final String TAG = "OrdersManageActivity";
    private static final String API_KEY = "SEC195C79FC4CCB09B48AA8";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView ordersRecyclerView;
    private OrdersAdapter ordersAdapter;
    private List<Order> ordersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView emptyStateText;
    private TabLayout tabLayout;

    private String currentFilter = "ALL";
    private Order currentOrderForPrinting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders_manage);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupTabs();

        fetchOrders();
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
            getSupportActionBar().setTitle("Manage Orders");
        }
    }

    private void setupRecyclerView() {
        ordersAdapter = new OrdersAdapter(filteredOrdersList,
                this::onOrderClicked,
                this::onStatusUpdateClicked);
        ordersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        ordersRecyclerView.setAdapter(ordersAdapter);
    }

    private void setupTabs() {
        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab().setText("All"));
            tabLayout.addTab(tabLayout.newTab().setText("Pending"));
            tabLayout.addTab(tabLayout.newTab().setText("Processing"));
            tabLayout.addTab(tabLayout.newTab().setText("Completed"));
        }

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

    private void onCancelClicked(Order order) {
        showCancelConfirmation(order);
    }

    private void showCancelConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cancel Order")
                .setMessage("Are you sure you want to cancel order #" + order.getOrderId() + "?")
                .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                    updateOrderStatus("CNL", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void updateOrderStatus(String status, int orderId) {
        progressBar.setVisibility(View.VISIBLE);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = new FormBody.Builder()
                .add("order_status", status)
                .build();

        Log.d(TAG, "Updating order " + orderId + " to status: " + status);

        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + getShopId() + "/orderUpdate/" + orderId)
                .post(requestBody)
                .addHeader("X-Api", API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrdersManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
                Log.e(TAG, "Status update failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Status update response: " + responseBodyString);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            Toast.makeText(OrdersManageActivity.this,
                                    "Order status updated successfully", Toast.LENGTH_SHORT).show();

                            autoSwitchTabAfterStatusUpdate(status);
                            fetchOrders();
                        } else {
                            String errorMessage = responseObject.optString("message", "Failed to update order status");
                            Toast.makeText(OrdersManageActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing failed: " + e.getMessage());
                        Toast.makeText(OrdersManageActivity.this,
                                "Error updating order status", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void autoSwitchTabAfterStatusUpdate(String newStatus) {
        int tabPosition = -1;

        switch (newStatus) {
            case "PRS":
                tabPosition = 2;
                break;
            case "COM":
                tabPosition = 3;
                break;
            case "CNL":
                tabPosition = 0;
                break;
        }

        if (tabPosition != -1 && tabLayout.getTabCount() > tabPosition) {
            TabLayout.Tab targetTab = tabLayout.getTabAt(tabPosition);
            if (targetTab != null) {
                targetTab.select();
            }
        }
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
            fetchOrders();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onOrderClicked(Order order) {
        showOrderDetails(order);
    }

    private void onStatusUpdateClicked(Order order) {
        String currentStatus = order.getOrderStatus();

        switch (currentStatus) {
            case "PENDING":
                showProcessConfirmation(order);
                break;
            case "PROCESSING":
                showCompleteConfirmation(order);
                break;
            case "COMPLETED":
            case "CANCELLED":
                showOrderDetails(order);
                break;
            default:

                showOrderDetails(order);
                break;
        }
    }

    private void showProcessConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Process Order")
                .setMessage("Are you sure you want to process order #" + order.getOrderId() + "?")
                .setPositiveButton("Yes, Process", (dialog, which) -> {
                    updateOrderStatus("PRS", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCompleteConfirmation(Order order) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Complete Order")
                .setMessage("Are you sure you want to mark order #" + order.getOrderId() + " as completed?")
                .setPositiveButton("Yes, Complete", (dialog, which) -> {
                    updateOrderStatus("COM", Integer.parseInt(order.getId()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showOrderDetails(Order order) {
        StringBuilder message = new StringBuilder();
        message.append("Customer: ").append(order.getCustomerName()).append("\n")
                .append("Amount: ₹").append(order.getAmount()).append("\n")
                .append("Status: ").append(order.getOrderStatus()).append("\n")
                .append("Date: ").append(order.getOrderedDate()).append("\n")
                .append("Address: ").append(order.getShippingAddress()).append(", ").append(order.getCity()).append("\n")
                .append("Payment: ").append(order.getPaymentMethod()).append("\n")
                .append("Items: ").append(order.getItemsCount()).append(" products\n\n");

        if (order.getVariants() != null && !order.getVariants().isEmpty()) {
            message.append("Order Items:\n");
            for (Variant variant : order.getVariants()) {
                message.append("• ").append(variant.getProdName())
                        .append(" (Qty: ").append(variant.getProdQty())
                        .append(", ₹").append(variant.getProdPrice()).append(")\n");
            }
        }

        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this)
                .setTitle("Order Details - #" + order.getOrderId())
                .setMessage(message.toString())
                .setPositiveButton("OK", null);

        if (order.getOrderStatus().equals("COMPLETED")){
            dialogBuilder.setNeutralButton("Print", (dialog, which) -> printOrder(order));
        }
        dialogBuilder.setNegativeButton("Share", (dialog, which) -> shareOrderDetails(order));

        dialogBuilder.show();
    }

    // CORRECTED PRINT METHOD - No permission check needed for modern approach
    private void printOrder(Order order) {
        String htmlContent = createPrintableHtmlContent(order);

        WebView webView = new WebView(this);
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                String jobName = "Order_" + order.getOrderId();
                PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(jobName);

                PrintAttributes.Builder builder = new PrintAttributes.Builder();
                builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4);
                builder.setColorMode(PrintAttributes.COLOR_MODE_COLOR);
                builder.setMinMargins(new PrintAttributes.Margins(50, 50, 50, 50));

                printManager.print(jobName, adapter, builder.build());
            }
        });
    }



    // CORRECTED PrintDocumentAdapter
    private PrintDocumentAdapter createPrintDocumentAdapter(Order order) {
        return new PrintDocumentAdapter() {
            private String documentContent;

            @Override
            public void onStart() {
                super.onStart();
                Log.d(TAG, "Print document adapter started");
                try {
                    documentContent = createPrintableHtmlContent(order);
                    if (documentContent == null || documentContent.isEmpty()) {
                        documentContent = createSimpleTextContent(order);
                    }
                    Log.d(TAG, "Document content prepared, length: " + documentContent.length());
                } catch (Exception e) {
                    Log.e(TAG, "Error in onStart: " + e.getMessage(), e);
                    documentContent = createSimpleTextContent(order);
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal, LayoutResultCallback callback,
                                 Bundle metadata) {

                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                try {
                    // Set explicit page count of 1
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder("order_" + order.getOrderId() + ".pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)  // Changed from PAGE_COUNT_UNKNOWN
                            .build();

                    callback.onLayoutFinished(info, !oldAttributes.equals(newAttributes));
                    Log.d(TAG, "Layout finished successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Layout failed: " + e.getMessage(), e);
                    callback.onLayoutFailed(e.getMessage());
                }
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal, WriteResultCallback callback) {

                PrintedPdfDocument pdfDocument = new PrintedPdfDocument(
                        OrdersManageActivity.this,
                        new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .setMinMargins(new PrintAttributes.Margins(50, 50, 50, 50))
                                .build()
                );

                try {
                    PdfDocument.Page page = pdfDocument.startPage(1);

                    Canvas canvas = page.getCanvas();

                    // You can either draw text directly or render HTML via WebView
                    Paint paint = new Paint();
                    paint.setTextSize(12);

                    // Simple fallback text layout
                    int y = 40;
                    for (String line : createSimpleTextContent(order).split("\n")) {
                        canvas.drawText(line, 40, y, paint);
                        y += paint.descent() - paint.ascent() + 5;
                    }

                    pdfDocument.finishPage(page);

                    FileOutputStream out = new FileOutputStream(destination.getFileDescriptor());
                    pdfDocument.writeTo(out);

                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

                } catch (IOException e) {
                    callback.onWriteFailed(e.toString());
                    Log.e(TAG, "PDF write failed", e);
                } finally {
                    pdfDocument.close();
                }
            }


            @Override
            public void onFinish() {
                super.onFinish();
                Log.d(TAG, "Print document adapter finished");
                documentContent = null;
            }
        };
    }

    private String createPrintableHtmlContent(Order order) {
        try {
            StringBuilder html = new StringBuilder();

            html.append("<!DOCTYPE html>")
                    .append("<html>")
                    .append("<head>")
                    .append("<meta charset=\"UTF-8\">")
                    .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
                    .append("<title>Order #").append(escapeHtml(order.getOrderId())).append("</title>")
                    .append("<style>")
                    .append("* { margin: 0; padding: 0; box-sizing: border-box; }")
                    .append("body { font-family: Arial, sans-serif; margin: 20px; line-height: 1.6; font-size: 12px; color: #333; }")
                    .append(".header { text-align: center; border-bottom: 2px solid #000; padding-bottom: 15px; margin-bottom: 20px; }")
                    .append(".store-name { font-size: 22px; font-weight: bold; margin-bottom: 5px; }")
                    .append(".invoice-title { font-size: 16px; color: #666; }")
                    .append(".section { margin-bottom: 20px; }")
                    .append(".section-title { font-weight: bold; font-size: 14px; border-bottom: 1px solid #ddd; padding-bottom: 5px; margin-bottom: 10px; }")
                    .append(".info-row { margin-bottom: 6px; }")
                    .append(".info-label { font-weight: bold; display: inline-block; width: 120px; }")
                    .append(".items-table { width: 100%; border-collapse: collapse; margin: 10px 0; }")
                    .append(".items-table th, .items-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
                    .append(".items-table th { background-color: #f5f5f5; font-weight: bold; }")
                    .append(".total-row { font-weight: bold; background-color: #f9f9f9; }")
                    .append(".footer { text-align: center; margin-top: 30px; padding-top: 15px; border-top: 2px solid #000; font-size: 11px; }")
                    .append("@media print { body { margin: 10px; } }")
                    .append("</style>")
                    .append("</head>")
                    .append("<body>");

            // Header
            html.append("<div class='header'>")
                    .append("<div class='store-name'>GRAMASANDHAI STORE</div>")
                    .append("<div class='invoice-title'>Order Invoice</div>")
                    .append("</div>");

            // Order Information
            html.append("<div class='section'>")
                    .append("<div class='section-title'>ORDER INFORMATION</div>")
                    .append("<div class='info-row'><span class='info-label'>Order ID:</span> #").append(escapeHtml(order.getOrderId())).append("</div>")
                    .append("<div class='info-row'><span class='info-label'>Date:</span> ").append(escapeHtml(order.getOrderedDate())).append("</div>")
                    .append("<div class='info-row'><span class='info-label'>Status:</span> ").append(escapeHtml(order.getOrderStatus())).append("</div>")
                    .append("</div>");

            // Customer Information
            html.append("<div class='section'>")
                    .append("<div class='section-title'>CUSTOMER INFORMATION</div>")
                    .append("<div class='info-row'><span class='info-label'>Name:</span> ").append(escapeHtml(order.getCustomerName())).append("</div>")
                    .append("<div class='info-row'><span class='info-label'>Address:</span> ").append(escapeHtml(order.getShippingAddress())).append("</div>")
                    .append("<div class='info-row'><span class='info-label'>City:</span> ").append(escapeHtml(order.getCity())).append("</div>")
                    .append("<div class='info-row'><span class='info-label'>Payment Method:</span> ").append(escapeHtml(order.getPaymentMethod())).append("</div>")
                    .append("</div>");

            // Order Items
            html.append("<div class='section'>")
                    .append("<div class='section-title'>ORDER ITEMS</div>")
                    .append("<table class='items-table'>")
                    .append("<thead>")
                    .append("<tr>")
                    .append("<th>Product Name</th>")
                    .append("<th>Qty</th>")
                    .append("<th>Unit Price</th>")
                    .append("<th>Total Price</th>")
                    .append("</tr>")
                    .append("</thead>")
                    .append("<tbody>");

            double grandTotal = 0;
            if (order.getVariants() != null && !order.getVariants().isEmpty()) {
                for (Variant variant : order.getVariants()) {
                    try {
                        double price = Double.parseDouble(variant.getProdPrice());
                        int qty = Integer.parseInt(variant.getProdQty());
                        double total = price * qty;
                        grandTotal += total;

                        html.append("<tr>")
                                .append("<td>").append(escapeHtml(variant.getProdName())).append("</td>")
                                .append("<td>").append(qty).append("</td>")
                                .append("<td>₹").append(String.format(Locale.getDefault(), "%.2f", price)).append("</td>")
                                .append("<td>₹").append(String.format(Locale.getDefault(), "%.2f", total)).append("</td>")
                                .append("</tr>");
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing price/quantity for item: " + variant.getProdName());
                        html.append("<tr>")
                                .append("<td>").append(escapeHtml(variant.getProdName())).append("</td>")
                                .append("<td>").append(escapeHtml(variant.getProdQty())).append("</td>")
                                .append("<td>₹").append(escapeHtml(variant.getProdPrice())).append("</td>")
                                .append("<td>N/A</td>")
                                .append("</tr>");
                    }
                }
            }

            // Total Row
            html.append("<tr class='total-row'>")
                    .append("<td colspan='3'><strong>Grand Total</strong></td>")
                    .append("<td><strong>₹").append(String.format(Locale.getDefault(), "%.2f", grandTotal)).append("</strong></td>")
                    .append("</tr>")
                    .append("</tbody>")
                    .append("</table>")
                    .append("<div><strong>Total Items:</strong> ").append(order.getItemsCount()).append("</div>")
                    .append("</div>");

            // Footer
            html.append("<div class='footer'>")
                    .append("<div><strong>Thank you for your order!</strong></div>")
                    .append("<div>Gramasandhai Store</div>")
                    .append("</div>")
                    .append("</body>")
                    .append("</html>");

            return html.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating HTML content: " + e.getMessage(), e);
            return createSimpleTextContent(order);
        }
    }

    private String createSimpleTextContent(Order order) {
        StringBuilder content = new StringBuilder();
        content.append("GRAMASANDHAI STORE\n")
                .append("Order Invoice\n")
                .append("================\n\n")
                .append("ORDER INFORMATION:\n")
                .append("Order ID: #").append(order.getOrderId()).append("\n")
                .append("Date: ").append(order.getOrderedDate()).append("\n")
                .append("Status: ").append(order.getOrderStatus()).append("\n\n")
                .append("CUSTOMER INFORMATION:\n")
                .append("Name: ").append(order.getCustomerName()).append("\n")
                .append("Address: ").append(order.getShippingAddress()).append("\n")
                .append("City: ").append(order.getCity()).append("\n")
                .append("Payment: ").append(order.getPaymentMethod()).append("\n\n")
                .append("ORDER ITEMS:\n");

        double grandTotal = 0;
        if (order.getVariants() != null && !order.getVariants().isEmpty()) {
            for (Variant variant : order.getVariants()) {
                content.append("- ").append(variant.getProdName())
                        .append(" (Qty: ").append(variant.getProdQty())
                        .append(", ₹").append(variant.getProdPrice()).append(")\n");
                try {
                    double price = Double.parseDouble(variant.getProdPrice());
                    int qty = Integer.parseInt(variant.getProdQty());
                    grandTotal += price * qty;
                } catch (NumberFormatException e) {
                    // Ignore parsing errors
                }
            }
        }

        content.append("\nGrand Total: ₹").append(String.format(Locale.getDefault(), "%.2f", grandTotal)).append("\n")
                .append("Total Items: ").append(order.getItemsCount()).append("\n\n")
                .append("Thank you for your order!\n")
                .append("Gramasandhai Store");

        return content.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private void shareOrderDetails(Order order) {
        String orderDetails = formatOrderDetailsForSharing(order);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Order #" + order.getOrderId() + " - Gramasandhai Store");
        shareIntent.putExtra(Intent.EXTRA_TEXT, orderDetails);

        try {
            startActivity(Intent.createChooser(shareIntent, "Share Order Details"));
        } catch (Exception e) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatOrderDetailsForSharing(Order order) {
        StringBuilder details = new StringBuilder();
        details.append("Order #").append(order.getOrderId()).append("\n")
                .append("Customer: ").append(order.getCustomerName()).append("\n")
                .append("Amount: ₹").append(order.getAmount()).append("\n")
                .append("Date: ").append(order.getOrderedDate()).append("\n")
                .append("Status: ").append(order.getOrderStatus()).append("\n")
                .append("Address: ").append(order.getShippingAddress()).append(", ").append(order.getCity()).append("\n")
                .append("Payment: ").append(order.getPaymentMethod()).append("\n\n");

        if (order.getVariants() != null && !order.getVariants().isEmpty()) {
            details.append("Items:\n");
            for (Variant variant : order.getVariants()) {
                details.append("• ").append(variant.getProdName())
                        .append(" (Qty: ").append(variant.getProdQty())
                        .append(", ₹").append(variant.getProdPrice()).append(")\n");
            }
        }

        details.append("\nThank you for shopping with Gramasandhai Store!");
        return details.toString();
    }

    private void filterOrders() {
        filteredOrdersList.clear();

        if ("ALL".equals(currentFilter)) {
            filteredOrdersList.addAll(ordersList);
        } else {
            for (Order order : ordersList) {
                if (currentFilter.equalsIgnoreCase(order.getOrderStatus())) {
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

    private void fetchOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        Log.d(TAG, "Fetching orders for shop: " + getShopId());

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(Attributes.Main_Url + "shop/" + getShopId() + "/orders")
                .get()
                .addHeader("X-Api", API_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OrdersManageActivity.this,
                            "Network error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    updateEmptyState();
                });
                Log.e(TAG, "Orders fetch failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBodyString = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "Orders response: " + responseBodyString);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject responseObject = new JSONObject(responseBodyString);

                        if (response.isSuccessful() && "success".equals(responseObject.optString("status", ""))) {
                            parseOrdersData(responseObject.getJSONArray("orders"));
                        } else {
                            String errorMessage = responseObject.optString("message", "Failed to load orders");
                            Toast.makeText(OrdersManageActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            updateEmptyState();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON parsing failed: " + e.getMessage());
                        Toast.makeText(OrdersManageActivity.this,
                                "Error parsing order data", Toast.LENGTH_LONG).show();
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

            try {
                Order order = new Order();

                order.setId(orderObj.getString("id"));
                order.setOrderId(orderObj.getString("order_id"));
                order.setAmount(orderObj.getString("amount"));
                order.setOrderStatus(mapStatus(orderObj.getString("order_status")));
                order.setPaymentMethod(orderObj.getString("payment_method"));
                order.setOrderedDate(formatDate(orderObj.getString("ordered_date")));

                if (orderObj.has("customer") && !orderObj.isNull("customer")) {
                    JSONObject customerObj = orderObj.getJSONObject("customer");
                    order.setCustomerName(customerObj.optString("name", "Unknown Customer"));
                } else {
                    order.setCustomerName("Unknown Customer");
                }

                order.setShippingAddress(orderObj.optString("shipping_address", "Not specified"));
                order.setCity(orderObj.optString("city", "Not specified"));

                if (orderObj.has("variants") && !orderObj.isNull("variants")) {
                    JSONArray variants = orderObj.getJSONArray("variants");
                    List<String> productNames = new ArrayList<>();
                    List<Variant> variantList = new ArrayList<>();

                    for (int j = 0; j < variants.length(); j++) {
                        JSONObject variant = variants.getJSONObject(j);

                        Variant variantObj = new Variant();
                        variantObj.setId(variant.optString("id", ""));
                        variantObj.setOrdTbId(variant.optString("ord_tb_id", ""));
                        variantObj.setOrderId(variant.optString("order_id", ""));
                        variantObj.setProdId(variant.optString("prod_id", ""));
                        variantObj.setProdName(variant.optString("prod_name", "Unknown Product"));
                        variantObj.setProdQty(variant.optString("prod_qty", "1"));
                        variantObj.setProdPrice(variant.optString("prod_price", "0"));
                        variantObj.setWeight(variant.optString("weight", ""));
                        variantObj.setImageName(variant.optString("imagename", ""));

                        variantList.add(variantObj);
                        productNames.add(variantObj.getProdName());
                    }

                    order.setProducts(productNames);
                    order.setVariants(variantList);
                    order.setItemsCount(variants.length());
                } else {
                    order.setProducts(new ArrayList<>());
                    order.setVariants(new ArrayList<>());
                    order.setItemsCount(0);
                }

                ordersList.add(order);
            } catch (JSONException e) {
                Log.e(TAG, "Error parsing order at index " + i + ": " + e.getMessage());
            }
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
        if (dateString == null || dateString.isEmpty()) {
            return "Unknown date";
        }

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error for: " + dateString);
            return dateString;
        }
    }

    private String getShopId() {
        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        return prefs.getString("shopId", "");
    }
}
