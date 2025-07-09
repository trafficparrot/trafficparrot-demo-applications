package com.trafficparrot.example;


import com.google.gson.Gson;
import com.ibm.mq.jms.MQQueueConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;

import javax.jms.Queue;
import javax.jms.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

import static com.ibm.msg.client.jms.JmsConstants.WMQ_PROVIDER;
import static com.ibm.msg.client.wmq.common.CommonConstants.*;

public class MobileNetworkHardwareMicroservice {
    private final UUID deviceId = UUID.randomUUID();
    private Properties properties;
    private MessageConsumer consumer;

    public static void main(String[] args) throws Exception {
        MobileNetworkHardwareMicroservice mobileOnboardingMicroservice = new MobileNetworkHardwareMicroservice();
        mobileOnboardingMicroservice.start();
        mobileOnboardingMicroservice.receiveMessages();
    }

    public void receiveMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                receiveMessage();
            } catch (Exception e) {
                info("Error receiving message: " + e.getMessage());
                if (Thread.currentThread().isInterrupted()) {
                    info("Received interrupt signal, stopping...");
                    break;
                }
            }
        }
        info("Message receiving stopped");
    }

    public QueueConnectionFactory createConnectionFactory() throws JMSException {
        MQQueueConnectionFactory factory = new MQQueueConnectionFactory();
        factory.setHostName("localhost");
        factory.setPort(1414);
        factory.setQueueManager("QM1");
        factory.setChannel("SYSTEM.DEF.SVRCONN");
        return factory;
    }

    public void receiveMessage() {
        try {
            Message message = consumer.receive(1000);
            if (message == null) {
                info("No new messages");
            } else if (message instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                ProvisionRequest provisionRequest = new Gson().fromJson(text, ProvisionRequest.class);
                processProvisionRequest(provisionRequest);
            } else {
                info("Unsupported message type: " + message.getClass());
            }
        } catch (JMSException e) {
            info("Problem processing message " + e.getMessage());
        }
    }

    private void processProvisionRequest(ProvisionRequest provisionRequest) throws JMSException {
        info("Received: " + provisionRequest);
        if (!"data-and-voice".equals(provisionRequest.getMobileType())) {
            ProvisionConfirmation mobileProvisioned = new ProvisionConfirmation(provisionRequest.getMobileNumber(),
                    provisionRequest.getMobileType(),
                    "MOBILE_PROVISIONED",
                    deviceId.toString(),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
            sendConfirmation(mobileProvisioned);
            info("Sent: " + mobileProvisioned);
        } else {
            ProvisionError provisionError = new ProvisionError(provisionRequest.getMobileNumber(),
                    provisionRequest.getMobileType(),
                    "UNSUPPORTED_MOBILE_TYPE",
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
            sendConfirmation(provisionError);
            info("Sent: " + provisionError);
        }
    }

    public void start() throws Exception {
        info("Starting...");
        properties = loadProperties();
        processMobileProvisioning();
        info("Application started");
    }

    private void processMobileProvisioning() throws JMSException {
        Connection connection = getConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(properties.getProperty("ibmmq.order.queue"));
        consumer = session.createConsumer(queue);
        connection.start();
    }

    private static void info(String msg) {
        System.out.println(new Date() + ": " + msg);
    }

    private static Properties loadProperties() throws IOException {
        InputStream propertiesInputStream = getPropertiesInputStream();
        Properties properties = new Properties();
        properties.load(propertiesInputStream);
        return properties;
    }

    private static InputStream getPropertiesInputStream() throws IOException {
        String fileLocation = System.getProperty("application.properties");
        if (fileLocation != null) {
            return Files.newInputStream(Paths.get(fileLocation));
        } else {
            String classpathFileName = "application.properties";
            InputStream resourceAsStream = MobileNetworkHardwareMicroservice.class.getClassLoader().getResourceAsStream(classpathFileName);
            if (resourceAsStream != null) {
                return resourceAsStream;
            } else {
                throw new FileNotFoundException("property file '" + classpathFileName + "' not found in the classpath");
            }
        }
    }

    private void sendConfirmation(Object provisionConfirmation) throws JMSException {
        sendConfirmation(new Gson().toJson(provisionConfirmation));
    }

    public void sendConfirmation(String message) throws JMSException {
        try (Connection connection = getConnection()) {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            String orderQueueName = properties.getProperty("ibmmq.confirmation.queue");
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
}
