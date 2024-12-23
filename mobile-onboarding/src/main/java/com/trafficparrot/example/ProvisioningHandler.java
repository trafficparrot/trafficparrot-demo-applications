package com.trafficparrot.example;

import com.google.gson.Gson;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.jms.*;
import javax.jms.Queue;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.ibm.msg.client.wmq.common.CommonConstants.*;
import static javax.servlet.http.HttpServletResponse.SC_OK;

class ProvisioningHandler extends AbstractHandler {
    private final Properties properties;
    private final CopyOnWriteArrayList<Confirmation> confirmations = new CopyOnWriteArrayList<>();

    public ProvisioningHandler(Properties properties) {
        this.properties = properties;
    }

    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {
        try {
            if (target.startsWith("/provision-mobile")) {
                boolean status = sendPayment(request);
                response.setStatus(SC_OK);
                response.getWriter().print(status ? "Success!" : "ERROR!");
                baseRequest.setHandled(true);
            } else if (target.startsWith("/provision-confirmations")) {
                response.setHeader("Content-Type", "application/json; charset=utf-8");
                response.getWriter().print(new Gson().toJson(getConfirmations()));
                baseRequest.setHandled(true);
            }
        } catch (JMSException  e) {
            throw new ServletException(e);
        }  catch (NoClassDefFoundError  e) {
            if (e.getMessage().contains("com/ibm")) {
                System.err.println("In order to use IBM® MQ you need jar files that will allow Food Order System to establish connections with MQ. " +
                        "See README.md file for more information");
            } else {
                throw e;
            }
        }
    }

    private List<Confirmation> getConfirmations() throws JMSException {
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
                Enumeration<?> propertyNames = message.getPropertyNames();
                System.out.println("JMS message properties:");
                while (propertyNames.hasMoreElements()) {
                    String propertyName = (String) propertyNames.nextElement();
                    Object propertyValue = message.getObjectProperty(propertyName);
                    System.out.println("   " + propertyName + ": " + propertyValue);
                }
                confirmations.add(new Gson().fromJson(text, Confirmation.class));
            }
            System.out.println(new Date() + " No new messages");

        }
        return confirmations;
    }

    private boolean sendPayment(HttpServletRequest request) throws JMSException {
        Map<String, String> jmsRequest = new HashMap<>();
        jmsRequest.put("mobileType", request.getParameter("mobileType"));
        jmsRequest.put("mobileNumber", request.getParameter("mobileNumber"));
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
        com.ibm.msg.client.jms.JmsConnectionFactory factory = JmsFactoryFactory
                .getInstance(WMQ_PROVIDER)
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

    private static class Confirmation {
        public final String status;
        public final String mobileNumber;
        public final String mobileType;
        public final String error;
        public final String deviceId;
        public final Date date;

        private Confirmation(String status, String mobileNumber, String mobileType, String error, String deviceId, Date date) {
            this.status = status;
            this.mobileNumber = mobileNumber;
            this.mobileType = mobileType;
            this.error = error;
            this.deviceId = deviceId;
            this.date = date;
        }
    }
}
