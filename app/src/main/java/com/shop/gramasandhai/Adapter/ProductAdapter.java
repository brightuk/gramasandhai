package com.shop.gramasandhai.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.shop.gramasandhai.Activity.ProductViewActivity;
import com.shop.gramasandhai.Model.Product;
import com.shop.gramasandhai.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private Context context;
    private List<Product> productList;
    private JSONObject apiResponseData; // Add this to store the full API response
    private OnItemClickListener onItemClickListener;
    private OnMoreOptionsClickListener onMoreOptionsClickListener;

    public interface OnItemClickListener {
        void onItemClick(Product product, int position);
    }

    public interface OnMoreOptionsClickListener {
        void onMoreOptionsClick(Product product, int position, View view);
    }

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    // Add this method to set the API response data
    public void setApiResponseData(JSONObject apiResponseData) {
        this.apiResponseData = apiResponseData;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnMoreOptionsClickListener(OnMoreOptionsClickListener listener) {
        this.onMoreOptionsClickListener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Product product = productList.get(position);

        holder.tvProductName.setText(product.getName());

        // Use description instead of subtitle
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            holder.tvProductDescription.setText(product.getDescription());
            holder.tvProductDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvProductDescription.setVisibility(View.GONE);
        }

        holder.layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pass the product data to ProductViewActivity
                Intent intent = new Intent(context, ProductViewActivity.class);

                // Create a JSON object with product data and variants
                try {
                    JSONObject productData = new JSONObject();
                    productData.put("prod_id", product.getId());
                    productData.put("prod_name", product.getName());
                    productData.put("description", product.getDescription());
                    productData.put("image_url", product.getImageUrl());
                    productData.put("category_id", product.getCategoryId());
                    productData.put("subcategory_id", product.getSubcategoryId());

                    // Get variants for this product from the API response
                    if (apiResponseData != null && apiResponseData.has("products_variants")) {
                        JSONArray allVariants = apiResponseData.getJSONArray("products_variants");
                        JSONArray productVariants = new JSONArray();

                        // Filter variants for this specific product
                        for (int i = 0; i < allVariants.length(); i++) {
                            JSONArray variantArray = allVariants.getJSONArray(i);
                            if (variantArray.length() > 0) {
                                JSONObject variant = variantArray.getJSONObject(0);
                                if (variant.getString("prod_id").equals(product.getId())) {
                                    productVariants.put(variantArray);
                                }
                            }
                        }
                        productData.put("products_variants", productVariants);
                    } else {
                        productData.put("products_variants", new JSONArray());
                    }

                    intent.putExtra("product_data", productData.toString());
                    intent.putExtra("prod_id",product.getId());
                    context.startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                    // Fallback: just pass basic product info
                    intent.putExtra("product_id", product.getId());
                    intent.putExtra("product_name", product.getName());
                    context.startActivity(intent);
                }
            }
        });

        // Load image with Glide
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(product.getImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(product, position);
            }
        });

        // Set more options click listener
        holder.ibMoreOptions.setOnClickListener(v -> {
            if (onMoreOptionsClickListener != null) {
                onMoreOptionsClickListener.onMoreOptionsClick(product, position, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void updateList(List<Product> newList) {
        productList = newList;
        notifyDataSetChanged();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductCategory, tvProductDescription, tvProductPrice, tvProductStock;
        ImageButton ibMoreOptions;
        LinearLayout layout;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductDescription = itemView.findViewById(R.id.tvProductDescription);
            layout = itemView.findViewById(R.id.layout);
            ibMoreOptions = itemView.findViewById(R.id.ibMoreOptions);
        }
    }
}