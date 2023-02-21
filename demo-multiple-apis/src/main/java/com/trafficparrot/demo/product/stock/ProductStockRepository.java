package com.trafficparrot.demo.product.stock;

import java.util.Optional;

import static com.trafficparrot.demo.product.ProductDataRepository.loadProductData;

public class ProductStockRepository {
    public static Optional<ProductStock> productStock(String productId) {
        return loadProductData().stream()
                .filter(productData -> productData.productId.equals(productId))
                .map(productData -> new ProductStock(productData.productId, productData.stock))
                .findFirst();
    }
}
