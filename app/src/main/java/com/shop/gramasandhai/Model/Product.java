package com.shop.gramasandhai.Model;

public class Product {
    private String id;
    private String name;
    private String description; // Added this field
    private String imageUrl;
    private String price;
    private String stock;
    private String status;
    private String categoryId; // Added this field
    private String subcategoryId; // Added this field
    private String categoryName;
    private String subcategoryName;
    private String DiscountValue;

    // Constructors
    public Product() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; } // Added getter
    public void setDescription(String description) { this.description = description; } // Added setter

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getStock() { return stock; }
    public void setStock(String stock) { this.stock = stock; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCategoryId() { return categoryId; } // Added getter
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; } // Added setter

    public String getSubcategoryId() { return subcategoryId; } // Added getter
    public void setSubcategoryId(String subcategoryId) { this.subcategoryId = subcategoryId; } // Added setter

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getSubcategoryName() { return subcategoryName; }
    public void setSubcategoryName(String subcategoryName) { this.subcategoryName = subcategoryName; }

    public String getDiscountValue() {
        return DiscountValue;
    }

    public void setDiscountValue(String setDiscountValue) {
        this.DiscountValue = setDiscountValue;
    }
}