package com.trafficparrot.demo.product.stock;

public class ProductStock {

    public final String productId;
    public final int stock;

    public ProductStock(String productId, int stock) {
        this.productId = productId;
        this.stock = stock;
    }
}