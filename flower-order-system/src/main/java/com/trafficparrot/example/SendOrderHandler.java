package com.trafficparrot.example;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;

class SendOrderHandler extends AbstractHandler {
    private final Properties properties;
    private final CopyOnWriteArrayList<OrderConfirmation> orderConfirmations = new CopyOnWriteArrayList<>();

    public SendOrderHandler(Properties properties) throws IOException, TimeoutException {
        this.properties = properties;
        startOrderOrderConfirmationsThread();
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

    private boolean sendOrder(HttpServletRequest request) throws IOException, TimeoutException {
        Map<String, String> requestMessageMap = new HashMap<>();
        requestMessageMap.put("orderItemName", request.getParameter("orderItemName"));
        requestMessageMap.put("quantity", request.getParameter("quantity"));
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
