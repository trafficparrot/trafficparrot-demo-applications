package com.trafficparrot.demo.product.price;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProductPriceQueueConfiguration {

    public static final String PRICE_REQUEST_QUEUE = "price-request-queue";

    @Bean
    public Queue priceRequestQueue() {
        return new Queue(PRICE_REQUEST_QUEUE, false);
    }

}
