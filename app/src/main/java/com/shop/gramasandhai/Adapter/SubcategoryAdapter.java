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
import com.shop.gramasandhai.Model.Subcategory;
import com.shop.gramasandhai.R;

import java.util.List;

public class SubcategoryAdapter extends RecyclerView.Adapter<SubcategoryAdapter.SubcategoryViewHolder> {

    private Context context;
    private List<Subcategory> subcategoryList;
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Subcategory subcategory, int position);
    }

    public SubcategoryAdapter(Context context, List<Subcategory> subcategoryList) {
        this.context = context;
        this.subcategoryList = subcategoryList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public SubcategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category, parent, false);
        return new SubcategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubcategoryViewHolder holder, int position) {
        Subcategory subcategory = subcategoryList.get(position);

        holder.tvItemName.setText(subcategory.getName());
        holder.tvItemSubtitle.setText(subcategory.getSubtitle());

        // Set status
        if ("1".equals(subcategory.getStatus())) {
            holder.tvItemStatus.setText("Active");
            holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.green));
        } else {
            holder.tvItemStatus.setText("Inactive");
            holder.tvItemStatus.setTextColor(context.getResources().getColor(R.color.red));
        }

        // Hide count for subcategories as it's not in the API response
        holder.tvItemCount.setVisibility(View.VISIBLE);
        // Hide count for categories as it's not in the API response

        holder.tvItemCount.setText(subcategory.getProductCount()+" items");

        // Load image with Glide
        if (subcategory.getImageUrl() != null && !subcategory.getImageUrl().isEmpty()) {
            String fullImageUrl = subcategory.getImageUrl();
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
                onItemClickListener.onItemClick(subcategory, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subcategoryList.size();
    }

    public void updateList(List<Subcategory> newList) {
        subcategoryList = newList;
        notifyDataSetChanged();
    }

    static class SubcategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivItemImage;
        TextView tvItemName, tvItemSubtitle, tvItemCount, tvItemStatus;
        ImageView ivArrow;

        public SubcategoryViewHolder(@NonNull View itemView) {
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