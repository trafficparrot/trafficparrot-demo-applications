package com.trafficparrot.demo.product.price;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ProductPriceQueueConfiguration {

    public static final String PRICE_REQUEST_QUEUE = "${price.service.rabbitmq.request.queue.name}";

    @Value(PRICE_REQUEST_QUEUE)
    private String requestQueueName;

    @Bean
    public Queue priceRequestQueue() {
        return new Queue(requestQueueName, false);
    }

}
