package com.trafficparrot.demo.product.details;

public class ProductDetails {

    public final String productId;
    public final String name;
    public final String description;

    public ProductDetails(String productId, String name, String description) {
        this.productId = productId;
        this.name = name;
        this.description = description;
    }
}