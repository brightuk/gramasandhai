package com.shop.gramasandhai.Model;

public class Category {
    private String id;
    private String name;
    private String subtitle;
    private String imageUrl;
    private String status;
    private String position;
    private String setSubcategoriesCount;

    // Constructors, Getters and Setters
    public Category() {

    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getSubcategoriesCount() {
        return setSubcategoriesCount;
    }

    public void setSubcategoriesCount(String setSubcategoriesCount) {
        this.setSubcategoriesCount = setSubcategoriesCount;
    }
}