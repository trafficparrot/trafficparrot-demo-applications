package com.trafficparrot.demo.product.price;

import java.util.Optional;

import static com.trafficparrot.demo.product.ProductDataRepository.loadProductData;

public class ProductPriceRepository {
    public static Optional<ProductPrice> productPrice(String productId) {
        return loadProductData().stream()
                .filter(productData -> productData.productId.equals(productId))
                .map(productData -> new ProductPrice(productData.productId, productData.price))
                .findFirst();
    }
}
