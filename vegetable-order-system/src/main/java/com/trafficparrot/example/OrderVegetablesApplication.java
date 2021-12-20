package com.trafficparrot.example;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import java.io.*;
import java.util.Date;
import java.util.Properties;

public class OrderVegetablesApplication {
    private Server server;
    public final int port = 8282;

    public static void main(String[] args) throws Exception {
        OrderVegetablesApplication orderVegetablesApplication = new OrderVegetablesApplication();
        orderVegetablesApplication.start();
        orderVegetablesApplication.join();
    }

    public void start() throws Exception {
        info("Starting...");
        server = new Server(port);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{
                new SendOrderHandler(loadProperties()),
                getResourceHandler("html"),
                new DefaultHandler()});
        server.setHandler(handlers);
        server.start();
        info("Application started on http://localhost:" + port);
    }

    private static void info(String msg) {
        System.out.println(new Date() + ": " + msg);
    }

    private static ResourceHandler getResourceHandler(String resourceBase) {
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase(Resource.newClassPathResource(resourceBase).getName());
        return resourceHandler;
    }

    public void join() throws InterruptedException {
        server.join();
    }

    private static Properties loadProperties() throws IOException {
        InputStream propertiesInputStream = getPropertiesInputStream();
        Properties properties = new Properties();
        properties.load(propertiesInputStream);
        return properties;
    }

    private static InputStream getPropertiesInputStream() throws FileNotFoundException {
        String fileLocation = System.getProperty("application.properties");
        if (fileLocation != null) {
            return new FileInputStream(new File(fileLocation));
        } else {
            String classpathFileName = "application.properties";
            InputStream resourceAsStream = OrderVegetablesApplication.class.getClassLoader().getResourceAsStream(classpathFileName);
            if (resourceAsStream != null) {
                return resourceAsStream;
            } else {
                throw new FileNotFoundException("property file '" + classpathFileName + "' not found in the classpath");
            }
        }
    }

    public void stop() throws Exception {
        server.stop();
    }
}
