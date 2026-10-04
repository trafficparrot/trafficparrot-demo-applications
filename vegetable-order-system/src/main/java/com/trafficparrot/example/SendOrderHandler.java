package com.trafficparrot.example;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;

class SendOrderHandler extends Handler.Abstract {
    private final Properties properties;
    private final CopyOnWriteArrayList<OrderConfirmation> orderConfirmations = new CopyOnWriteArrayList<>();

    public SendOrderHandler(Properties properties) throws IOException, TimeoutException {
        this.properties = properties;
        startOrderOrderConfirmationsThread();
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

    private void startOrderOrderConfirmationsThread() throws IOException, TimeoutException {
        ConnectionFactory factory = createFactory();

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = properties.getProperty("rabbitmq.confirmation.queue");
        channel.queueDeclare(queueName, true, false, false, null);
        System.out.println("Receiving confirmation messages form queue named '" + queueName + "'");
        DefaultConsumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body) {
                String message = new String(body, UTF_8);
                System.out.println(new Date() + " Received: " + message);
                orderConfirmations.add(new Gson().fromJson(message, OrderConfirmation.class));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        System.out.println("Started received thread for queue '" + queueName + "'");
    }

    private boolean sendOrder(Fields parameters) throws IOException, TimeoutException {
        Map<String, String> requestMessageMap = new HashMap<>();
        requestMessageMap.put("orderItemName", parameters.getValue("orderItemName"));
        requestMessageMap.put("quantity", parameters.getValue("quantity"));
        sendMessage(new Gson().toJson(requestMessageMap));
        return true;
    }

    public void sendMessage(String message) throws IOException, TimeoutException {
        ConnectionFactory factory = createFactory();
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            String queueName = properties.getProperty("rabbitmq.order.queue");
            channel.queueDeclare(queueName, true, false, false, null);

            channel.basicPublish("", queueName, null, message.getBytes());
            System.out.println("Sent '" + message + "'");
        }
    }

    private ConnectionFactory createFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getProperty("rabbitmq.hostname"));
        factory.setPort(parseInt(properties.getProperty("rabbitmq.port")));
        factory.setUsername(properties.getProperty("rabbitmq.username"));
        factory.setPassword(properties.getProperty("rabbitmq.password"));
        return factory;
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
