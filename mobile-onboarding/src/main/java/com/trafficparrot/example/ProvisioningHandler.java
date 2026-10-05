package com.trafficparrot.example;

import com.google.gson.Gson;
import com.ibm.msg.client.jakarta.jms.JmsFactoryFactory;
import org.eclipse.jetty.http.HttpHeader;
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
import static org.eclipse.jetty.http.HttpStatus.OK_200;

class ProvisioningHandler extends Handler.Abstract {
    private final Properties properties;
    private final CopyOnWriteArrayList<Confirmation> confirmations = new CopyOnWriteArrayList<>();

    public ProvisioningHandler(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        String target = Request.getPathInContext(request);
        try {
            if (target.startsWith("/provision-mobile")) {
                boolean status = sendPayment(Request.getParameters(request));
                response.setStatus(OK_200);
                Content.Sink.write(response, true, status ? "Success!" : "ERROR!", callback);
                return true;
            } else if (target.startsWith("/provision-confirmations")) {
                response.getHeaders().put(HttpHeader.CONTENT_TYPE, "application/json; charset=utf-8");
                Content.Sink.write(response, true, new Gson().toJson(getConfirmations()), callback);
                return true;
            }
        }  catch (NoClassDefFoundError  e) {
            if (e.getMessage().contains("com/ibm")) {
                System.err.println("This application needs IBM's com.ibm.mq.jakarta.client.jar in its lib directory to connect to IBM MQ. " +
                        "See the README file for more information");
            } else {
                throw e;
            }
        }
        return false;
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

    private boolean sendPayment(Fields parameters) throws JMSException {
        Map<String, String> jmsRequest = new HashMap<>();
        jmsRequest.put("mobileType", parameters.getValue("mobileType"));
        jmsRequest.put("mobileNumber", parameters.getValue("mobileNumber"));
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
