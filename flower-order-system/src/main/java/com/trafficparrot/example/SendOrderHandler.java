package com.trafficparrot.example;

import com.google.gson.Gson;
import io.vertx.amqp.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static javax.servlet.http.HttpServletResponse.SC_OK;

class SendOrderHandler extends AbstractHandler {
    private final Properties properties;
    private final CopyOnWriteArrayList<OrderConfirmation> orderConfirmations = new CopyOnWriteArrayList<>();

    private volatile AmqpSender sender;

    public SendOrderHandler(Properties properties) {
        this.properties = properties;
        startSenderAndReceiver();
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {
        try {
            if (target.startsWith("/send-order")) {
                boolean status = sendOrder(request);
                response.setStatus(SC_OK);
                response.getWriter().print(status ? "Success!" : "ERROR!");
                baseRequest.setHandled(true);
            } else if (target.startsWith("/order-confirmations")) {
                response.setHeader("Content-Type", "application/json; charset=utf-8");
                response.getWriter().print(new Gson().toJson(orderConfirmations));
                baseRequest.setHandled(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException(e);
        }
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

    private boolean sendOrder(HttpServletRequest request) {
        Map<String, String> requestMessageMap = new HashMap<>();
        requestMessageMap.put("orderItemName", request.getParameter("orderItemName"));
        requestMessageMap.put("quantity", request.getParameter("quantity"));
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
