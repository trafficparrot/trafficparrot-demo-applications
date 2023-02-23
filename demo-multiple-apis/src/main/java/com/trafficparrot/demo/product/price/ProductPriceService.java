package com.trafficparrot.demo.product.price;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_REQUEST_QUEUE;

@Service
public class ProductPriceService {
    private static final Logger logger = LoggerFactory.getLogger(ProductPriceService.class);

    @RabbitListener(queues = PRICE_REQUEST_QUEUE)
    public byte[] listen(byte[] requestBytes) throws InvalidProtocolBufferException {
        Price.PriceRequest priceRequest = Price.PriceRequest.parseFrom(requestBytes);
        logger.info("Request for price for product id: " + priceRequest.getProductId());
        Price.PriceResponse response = Price.PriceResponse.newBuilder()
                .setProductId(priceRequest.getProductId())
                .setPrice(ProductPriceRepository.productPrice(priceRequest.getProductId()).get().price)
                .build();
        return response.toByteArray();
    }
}
