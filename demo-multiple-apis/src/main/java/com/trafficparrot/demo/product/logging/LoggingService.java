package com.trafficparrot.demo.product.logging;

import com.google.protobuf.InvalidProtocolBufferException;
import com.trafficparrot.demo.product.price.Price;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.trafficparrot.demo.product.logging.LoggingQueueConfiguration.LOGGING_REQUEST_QUEUE;
import static com.trafficparrot.demo.product.price.ProductPriceQueueConfiguration.PRICE_REQUEST_QUEUE;
import static com.trafficparrot.demo.product.price.ProductPriceRepository.productPrice;

@Service
public class LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    @RabbitListener(queues = LOGGING_REQUEST_QUEUE)
    public Logging.LoggingResponse listen(Logging.LoggingRequest loggingRequest) throws InvalidProtocolBufferException {
        /*
         Receiving/sending a Java serialized object over RabbitMQ, not just the Protobuf content, as per sample client use case.
         */
        try {
            if (loggingRequest.getLevel() == Logging.LOG_LEVEL.INFO) {
                logger.info("Logging request from client '" + loggingRequest.getMessage() + "'");
                return Logging.LoggingResponse.newBuilder()
                        .setSuccess(Logging.LoggingResponseSuccess.newBuilder().build())
                        .build();
            } else {
                return Logging.LoggingResponse.newBuilder()
                        .setError(Logging.LoggingResponseError.newBuilder()
                                .setMessage("Unsupported logging level: " + loggingRequest.getLevel())
                                .build())
                        .build();
            }
        } catch (Exception e) {
            return Logging.LoggingResponse.newBuilder()
                    .setError(Logging.LoggingResponseError.newBuilder().setMessage(e.getMessage()).build())
                    .build();
        }
    }
}
