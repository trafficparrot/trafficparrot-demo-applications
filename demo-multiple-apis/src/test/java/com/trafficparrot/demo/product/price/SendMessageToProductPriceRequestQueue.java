package com.trafficparrot.demo.product.price;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_REQUEST_QUEUE;
import static java.util.Collections.emptyMap;

/**
 * Use a RabbitMQ docker image to run this script, with {@link com.trafficparrot.demo.DemoApplication} running
 * docker run -d --hostname my-rabbit --name some-rabbit -p 15672:15672 -p 5672:5672 rabbitmq:3-management
 */
public class SendMessageToProductPriceRequestQueue {
    private static final String PRICE_RESPONSE_QUEUE = "price-response-queue";
    private static final String PRODUCT_ID = "1";

    public static void main(String[] args) throws IOException, TimeoutException {
        new SendMessageToProductPriceRequestQueue().sendPriceRequest(PRODUCT_ID);
    }

    private void sendPriceRequest(String productId) throws IOException, TimeoutException {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");
        connectionFactory.setPort(5672);
        connectionFactory.setUsername("guest");
        connectionFactory.setPassword("guest");

        Connection connection = connectionFactory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(PRICE_RESPONSE_QUEUE, false, false, false, emptyMap());

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            Price.PriceResponse response = Price.PriceResponse.parseFrom(delivery.getBody());
            System.out.println("Received: '" + response + "'");
            try {
                channel.close();
                connection.close();
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        };
        channel.basicConsume(PRICE_RESPONSE_QUEUE, true, deliverCallback, consumerTag -> { });

        Price.PriceRequest request = Price.PriceRequest.newBuilder()
                .setProductId(productId)
                .build();
        byte[] requestBytes = request.toByteArray();
        AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .replyTo(PRICE_RESPONSE_QUEUE)
                .build();
        channel.basicPublish("", PRICE_REQUEST_QUEUE, properties, requestBytes);
    }
}
