package com.trafficparrot.demo.product.details;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import static com.trafficparrot.demo.product.details.ProductDetailsRepository.searchProductDetails;
import static org.springframework.graphql.execution.ErrorType.NOT_FOUND;

@Controller
public class ProductDetailsController {
    @QueryMapping
    public ProductDetails product(@Argument String searchTerm) {
        return searchProductDetails(searchTerm).orElseThrow(() -> new GraphQLErrorException("Product not found: " + searchTerm, NOT_FOUND));
    }
}