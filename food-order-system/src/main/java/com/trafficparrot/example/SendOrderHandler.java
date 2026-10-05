package com.trafficparrot.example;

import com.google.gson.Gson;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.Fields;

import jakarta.jms.*;
import jakarta.jms.Queue;
import java.util.*;
import java.util.concurrent.*;

import static com.ibm.msg.client.jakarta.jms.JmsConstants.JAKARTA_WMQ_PROVIDER;
import static com.ibm.msg.client.jakarta.wmq.common.CommonConstants.*;

class SendOrderHandler extends Handler.Abstract {
    private final Properties properties;
    private final CopyOnWriteArrayList<OrderConfirmation> orderConfirmations = new CopyOnWriteArrayList<>();

    public SendOrderHandler(Properties properties) {
        this.properties = properties;
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
                Content.Sink.write(response, true, new Gson().toJson(getOrderConfirmations()), callback);
                return true;
            }
        } catch (NoClassDefFoundError  e) {
            if (e.getMessage().contains("com/ibm")) {
                System.err.println("In order to use IBM® MQ you need jar files that will allow Food Order System to establish connections with MQ. " +
                        "See README file for more information");
            } else {
                throw e;
            }
        }
        return false;
    }

    private List<OrderConfirmation> getOrderConfirmations() throws JMSException {
        try (Connection connection = getConnection()) {
            connection.start();
            Session session = connection.createSession();
            String queueName = properties.getProperty("ibmmq.confirmation.queue");
            System.out.println("Receiving confirmation messages form queue " + queueName);
            Queue queue = session.createQueue(queueName);
            MessageConsumer messageConsumer = session.createConsumer(queue);

            TextMessage message;
            while ((message = (TextMessage) messageConsumer.receiveNoWait()) != null) {
                String text = message.getText();
                System.out.println(new Date() + " Received: " + text);
                orderConfirmations.add(new Gson().fromJson(text, OrderConfirmation.class));
            }
            System.out.println(new Date() + " No new messages");

        }
        return orderConfirmations;
    }

    private boolean sendOrder(Fields parameters) throws JMSException {
        Map<String, String> jmsRequest = new HashMap<>();
        jmsRequest.put("orderItemName", parameters.getValue("orderItemName"));
        jmsRequest.put("quantity", parameters.getValue("quantity"));
        sendMessage(new Gson().toJson(jmsRequest));
        return true;
    }

    public void sendMessage(String message) throws JMSException {
        try (Connection connection = getConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            String orderQueueName = properties.getProperty("ibmmq.order.queue");
            Queue queue = session.createQueue(orderQueueName);
            MessageProducer messageProducer = session.createProducer(queue);
            TextMessage textMessage = session.createTextMessage(message);
            messageProducer.send(textMessage);
            System.out.println("Sending message to " + orderQueueName + ": " + textMessage);
        }
    }

    private Connection getConnection() throws JMSException {
        com.ibm.msg.client.jakarta.jms.JmsConnectionFactory factory = JmsFactoryFactory
                .getInstance(JAKARTA_WMQ_PROVIDER)
                .createConnectionFactory();
        factory.setIntProperty(WMQ_CONNECTION_MODE, WMQ_CM_CLIENT);
        factory.setStringProperty(WMQ_HOST_NAME, properties.get("ibmmq.hostname").toString());
        factory.setIntProperty(WMQ_PORT, Integer.valueOf(properties.get("ibmmq.port").toString()));
        factory.setStringProperty(WMQ_QUEUE_MANAGER, properties.get("ibmmq.queueManager").toString());
        factory.setStringProperty(WMQ_CHANNEL, properties.get("ibmmq.channel").toString());

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        final Future<Connection> handler = executor.submit(
                () -> factory.createConnection(properties.get("ibmmq.username").toString(), properties.get("ibmmq.password").toString()));
        executor.schedule(() -> {
            handler.cancel(true);
        }, 5, TimeUnit.SECONDS);

        try {
            return handler.get();
        } catch (CancellationException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
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
