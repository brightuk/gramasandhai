package com.shop.gramasandhai.Model;

public class Variant {
    private String id;
    private String ordTbId;
    private String orderId;
    private String prodId;
    private String prodName;
    private String prodQty;
    private String prodPrice;
    private String weight;
    private String imageName;

    // Default constructor
    public Variant() {
    }

    // Parameterized constructor
    public Variant(String id, String ordTbId, String orderId, String prodId,
                   String prodName, String prodQty, String prodPrice,
                   String weight, String imageName) {
        this.id = id;
        this.ordTbId = ordTbId;
        this.orderId = orderId;
        this.prodId = prodId;
        this.prodName = prodName;
        this.prodQty = prodQty;
        this.prodPrice = prodPrice;
        this.weight = weight;
        this.imageName = imageName;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrdTbId() {
        return ordTbId;
    }

    public void setOrdTbId(String ordTbId) {
        this.ordTbId = ordTbId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getProdId() {
        return prodId;
    }

    public void setProdId(String prodId) {
        this.prodId = prodId;
    }

    public String getProdName() {
        return prodName;
    }

    public void setProdName(String prodName) {
        this.prodName = prodName;
    }

    public String getProdQty() {
        return prodQty;
    }

    public void setProdQty(String prodQty) {
        this.prodQty = prodQty;
    }

    public String getProdPrice() {
        return prodPrice;
    }

    public void setProdPrice(String prodPrice) {
        this.prodPrice = prodPrice;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    // Helper method to get formatted weight with unit
    public String getFormattedWeight() {
        if (weight == null || weight.isEmpty()) {
            return "N/A";
        }
        try {
            double weightValue = Double.parseDouble(weight);
            if (weightValue >= 1000) {
                return (weightValue / 1000) + " kg";
            } else {
                return weight + " g";
            }
        } catch (NumberFormatException e) {
            return weight + " g";
        }
    }

    // Helper method to get total price for this variant (qty * price)
    public String getTotalPrice() {
        try {
            int quantity = Integer.parseInt(prodQty);
            double price = Double.parseDouble(prodPrice);
            double total = quantity * price;
            return String.format("%.2f", total);
        } catch (NumberFormatException e) {
            return "0.00";
        }
    }

    @Override
    public String toString() {
        return "Variant{" +
                "id='" + id + '\'' +
                ", prodName='" + prodName + '\'' +
                ", prodQty='" + prodQty + '\'' +
                ", prodPrice='" + prodPrice + '\'' +
                ", weight='" + weight + '\'' +
                '}';
    }
}