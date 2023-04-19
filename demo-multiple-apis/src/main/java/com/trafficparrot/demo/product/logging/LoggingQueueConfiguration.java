package com.trafficparrot.demo.product.logging;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class LoggingQueueConfiguration {
    public static final String LOGGING_REQUEST_QUEUE = "${logging.service.rabbitmq.request.queue.name}";

    @Value(LOGGING_REQUEST_QUEUE)
    private String requestQueueName;

    @Bean
    public Queue loggingQueue() {
        return new Queue(requestQueueName, false);
    }
}
