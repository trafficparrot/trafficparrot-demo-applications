package com.trafficparrot.demo.product.price;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProductPriceQueueConfiguration {

    public static final String PRICE_REQUEST_QUEUE = "price-request-queue";
    public static final String PRICE_RESPONSE_QUEUE = "price-response-queue";

    @Bean
    public Queue priceRequestQueue() {
        return new Queue(PRICE_REQUEST_QUEUE, false);
    }

    @Bean
    public Queue priceResponseQueue() {
        return new Queue(PRICE_RESPONSE_QUEUE, false);
    }
}
