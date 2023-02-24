package com.trafficparrot.demo.product.price;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_REQUEST_QUEUE;
import static com.trafficparrot.demo.product.price.ProductPriceRepository.productPrice;

@Service
public class ProductPriceService {
    private static final Logger logger = LoggerFactory.getLogger(ProductPriceService.class);

    @RabbitListener(queues = PRICE_REQUEST_QUEUE)
    public byte[] listen(byte[] requestBytes) throws InvalidProtocolBufferException {
        Price.PriceRequest priceRequest = Price.PriceRequest.parseFrom(requestBytes);
        logger.info("Request for price for product id: " + priceRequest.getProductId());
        Optional<ProductPrice> productPrice = productPrice(priceRequest.getProductId());
        if (productPrice.isPresent()) {
            return successResponse(productPrice.get().productId, productPrice.get().price);
        } else {
            return errorResponse("Could not find product with id: " + priceRequest.getProductId());
        }
    }

    private byte[] successResponse(String productId, double price) {
        Price.PriceResponse successResponse = Price.PriceResponse.newBuilder()
                .setSuccess(Price.PriceResponseSuccess.newBuilder()
                        .setProductId(productId)
                        .setPrice(price)
                        .build())
                .build();
        return successResponse.toByteArray();
    }

    private byte[] errorResponse(String error) {
        Price.PriceResponse successResponse = Price.PriceResponse.newBuilder()
                .setError(Price.PriceResponseError.newBuilder()
                        .setMessage(error)
                        .build())
                .build();
        return successResponse.toByteArray();
    }
}
