package com.trafficparrot.example;

import com.google.gson.Gson;
import io.vertx.amqp.*;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;

class SendOrderHandler extends Handler.Abstract {
    private final Properties properties;
    private final CopyOnWriteArrayList<OrderConfirmation> orderConfirmations = new CopyOnWriteArrayList<>();

    private volatile AmqpSender sender;

    public SendOrderHandler(Properties properties) {
        this.properties = properties;
        startSenderAndReceiver();
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String target = Request.getPathInContext(request);
        try {
            if (target.startsWith("/send-order")) {
                boolean status = sendOrder(Request.getParameters(request));
                response.setStatus(HttpStatus.OK_200);
                Content.Sink.write(response, true, status ? "Success!" : "ERROR!", callback);
                return true;
            } else if (target.startsWith("/order-confirmations")) {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=utf-8");
                Content.Sink.write(response, true, new Gson().toJson(orderConfirmations), callback);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        return false;
    }

    private void startSenderAndReceiver() {
        AmqpClient client = createClient();
        client.connect(conn -> {
            if (conn.failed()) {
                throw new IllegalStateException(conn.cause());
            }
            String confirmationQueueName = properties.getProperty("amqp.broker.confirmation.queue");
            conn.result().createReceiver(confirmationQueueName,
                    done -> {
                        if (done.failed()) {
                            System.out.println("Unable to create receiver to queue '" + confirmationQueueName + "'");
                            throw new IllegalStateException(done.cause());
                        } else {
                            AmqpReceiver receiver = done.result();
                            receiver.handler(msg -> {
                                String message = msg.bodyAsString();
                                System.out.println(new Date() + " Received: " + message);
                                orderConfirmations.add(new Gson().fromJson(message, OrderConfirmation.class));
                            });
                        }
                    }
            );
            System.out.println("Started receiver thread for queue '" + confirmationQueueName + "'");

            String orderQueueName = properties.getProperty("amqp.broker.order.queue");
            conn.result().createSender(orderQueueName, done -> {
                if (done.failed()) {
                    System.out.println("Unable to create a sender for '" + orderQueueName + "' ");
                    throw new IllegalStateException(done.cause());
                } else {
                    sender = done.result();
                    System.out.println("Sender created for '" + orderQueueName + "'");
                }
            });
            System.out.println("Started sender thread for queue '" + orderQueueName + "'");
        });
    }

    private AmqpClient createClient() {
        AmqpClientOptions amqpClientOptions = new AmqpClientOptions()
                .setHost(properties.getProperty("amqp.broker.hostname"))
                .setPort(parseInt(properties.getProperty("amqp.broker.port")));
//                .setUsername(properties.getProperty("amqp.broker.username"))
//                .setPassword(properties.getProperty("amqp.broker.password"));
        return AmqpClient.create(amqpClientOptions);
    }

    private boolean sendOrder(Fields parameters) {
        Map<String, String> requestMessageMap = new HashMap<>();
        requestMessageMap.put("orderItemName", parameters.getValue("orderItemName"));
        requestMessageMap.put("quantity", parameters.getValue("quantity"));
        sendMessage(new Gson().toJson(requestMessageMap));
        return true;
    }

    public void sendMessage(String message) {
        AmqpMessageBuilder builder = AmqpMessage.create();
        AmqpMessage m1 = builder.withBody(message).build();
        sender.send(m1);
        System.out.println("Sent '" + message + "'");
    }

    private static class OrderConfirmation {
        public final String orderItemName;
        public final String quantity;
        public final Date date;

        public OrderConfirmation(String orderItemName, String quantity, Date date) {
            this.orderItemName = orderItemName;
            this.quantity = quantity;
            this.date = date;
        }
    }
}
