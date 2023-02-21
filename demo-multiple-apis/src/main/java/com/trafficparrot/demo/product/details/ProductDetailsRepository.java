package com.trafficparrot.demo.product.details;

import java.util.Optional;

import static com.trafficparrot.demo.product.ProductDataRepository.loadProductData;

public class ProductDetailsRepository {
    public static Optional<ProductDetails> searchProductDetails(String searchTerm) {
        return loadProductData().stream()
                .filter(productData -> productData.name.contains(searchTerm) || productData.description.contains(searchTerm))
                .map(productData -> new ProductDetails(productData.productId, productData.name, productData.description))
                .findFirst();
    }
}
