package com.trafficparrot.demo.product.price;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_REQUEST_QUEUE;
import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_RESPONSE_QUEUE;

@Service
public class ProductPriceService {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public ProductPriceService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = PRICE_REQUEST_QUEUE, ackMode = "AUTO")
    public void listen(byte[] requestBytes) throws InvalidProtocolBufferException {
        Price.PriceRequest priceRequest = Price.PriceRequest.parseFrom(requestBytes);
        System.out.println("request = " + priceRequest);
        Price.PriceResponse response = Price.PriceResponse.newBuilder()
                .setProductId(priceRequest.getProductId())
                .setPrice(1234)
                .build();
        byte[] responseBytes = response.toByteArray();
        rabbitTemplate.send(PRICE_RESPONSE_QUEUE, new Message(responseBytes));
    }
}
