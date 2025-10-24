package com.shop.gramasandhai.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.shop.gramasandhai.Model.Category;
import com.shop.gramasandhai.Model.Product;
import com.shop.gramasandhai.Model.Subcategory;
import com.shop.gramasandhai.R;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private Context context;
    private List<Category> categoryList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Category category, int position);
    }

    public CategoryAdapter(Context context, List<Category> categoryList) {
        this.context = context;
        this.categoryList = categoryList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categoryList.get(position);

        holder.tvItemName.setText(category.getName());
        holder.tvItemSubtitle.setText(category.getSubtitle());

        // Hide count for categories as it's not in the API response
        holder.tvItemCount.setVisibility(View.VISIBLE);
        holder.tvItemCount.setText(category.getSubcategoriesCount()+" subcatagories");

        // Set status
        if ("1".equals(category.getStatus())) {
            holder.tvItemStatus.setText("Active");
            holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.green));
        } else {
            holder.tvItemStatus.setText("Inactive");
            holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.red));
        }




        // Load image with Glide
        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            String fullImageUrl = category.getImageUrl();
            Glide.with(context)
                    .load(fullImageUrl)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(holder.ivItemImage);
        } else {
            holder.ivItemImage.setImageResource(R.drawable.placeholder_image);
        }

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(category, position);
            }
        });
    }



    @Override
    public int getItemCount() {
        return categoryList.size();
    }

    public void updateList(List<Category> newList) {
        categoryList = newList;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName, tvItemSubtitle, tvItemCount, tvItemStatus;
        ImageView ivArrow;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivItemImage = itemView.findViewById(R.id.ivItemImage);
            tvItemName = itemView.findViewById(R.id.tvItemName);
            tvItemSubtitle = itemView.findViewById(R.id.tvItemSubtitle);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvItemStatus = itemView.findViewById(R.id.tvItemStatus);
            ivArrow = itemView.findViewById(R.id.ivArrow);
        }
    }
}