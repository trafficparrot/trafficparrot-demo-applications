package com.trafficparrot.demo.product.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static com.trafficparrot.demo.product.logging.LoggingQueueConfiguration.LOGGING_REQUEST_QUEUE;

/**
 * Receiving/sending a Java serialized object over RabbitMQ, not just the Protobuf content, as per sample client use case.
 */
@Service
@RabbitListener(queues = LOGGING_REQUEST_QUEUE)
public class LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    @RabbitHandler
    public Logging.LoggingResponse listen(Logging.LoggingRequestDebug debug) {
        return loggingResponse(logger::debug, debug.getMessage());
    }

    @RabbitHandler
    public Logging.LoggingResponse debugLogging(Logging.LoggingRequestInfo info) {
        return loggingResponse(logger::info, info.getMessage());
    }

    @RabbitHandler
    public Logging.LoggingResponse errorLogging(Logging.LoggingRequestError error) {
        return loggingResponse(logger::error, error.getMessage());
    }

    private static Logging.LoggingResponse loggingResponse(Consumer<String> logger, String message) {
        try {
            logger.accept("Logging request from client '" + message + "'");
            return Logging.LoggingResponse.newBuilder()
                    .setSuccess(Logging.LoggingResponseSuccess.newBuilder().build())
                    .build();
        } catch (Exception e) {
            return Logging.LoggingResponse.newBuilder()
                    .setError(Logging.LoggingResponseError.newBuilder().setMessage(e.getMessage()).build())
                    .build();
        }
    }
}
