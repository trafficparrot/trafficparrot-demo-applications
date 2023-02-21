package com.trafficparrot.demo.product;

public class ProductData {
    public final String productId;
    public final String name;
    public final String description;
    public final double price;
    public final int stock;

    public ProductData(String productId, String name, String description, double price, int stock) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }
}
