package com.trafficparrot.demo.product.logging;

import com.google.protobuf.GeneratedMessageV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static com.trafficparrot.demo.product.logging.LoggingQueueConfiguration.LOGGING_REQUEST_QUEUE;

/**
 * Receiving/sending a Java serialized object over RabbitMQ, not just the Protobuf content, as per sample client use case.
 */
@Service
public class LoggingService {
    private static final Logger logger = LoggerFactory.getLogger(LoggingService.class);

    @RabbitListener(queues = LOGGING_REQUEST_QUEUE)
    public Logging.LoggingResponse listen(GeneratedMessageV3 loggingRequest) {
        if (loggingRequest instanceof Logging.LoggingRequestDebug) {
            Logging.LoggingRequestDebug loggingRequestDebug = (Logging.LoggingRequestDebug) loggingRequest;
            return loggingResponse(logger::debug, loggingRequestDebug.getMessage());
        } else if (loggingRequest instanceof Logging.LoggingRequestInfo) {
            Logging.LoggingRequestInfo loggingRequestInfo = (Logging.LoggingRequestInfo) loggingRequest;
            return loggingResponse(logger::info, loggingRequestInfo.getMessage());
        } else if (loggingRequest instanceof Logging.LoggingRequestError) {
            Logging.LoggingRequestError loggingRequestError = (Logging.LoggingRequestError) loggingRequest;
            return loggingResponse(logger::error, loggingRequestError.getMessage());
        } else {
            throw new UnsupportedOperationException("Unsupported message type: " + loggingRequest.getClass().getName());
        }
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
