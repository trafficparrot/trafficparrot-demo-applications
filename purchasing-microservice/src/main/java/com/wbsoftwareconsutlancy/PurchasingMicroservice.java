package com.wbsoftwareconsutlancy;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;

import java.util.Date;

public class PurchasingMicroservice {
    private Server server;
    public final int port = 8282;

    public static void main(String[] args) throws Exception {
        PurchasingMicroservice purchasingMicroservice = new PurchasingMicroservice();
        purchasingMicroservice.start();
        purchasingMicroservice.join();
    }

    public void start() throws Exception {
        info("Starting GUI...");
        server = new Server(port);
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[]{
                new PointsBalanceHandler(),
                new ReadyHandler(),
                getResourceHandler("html"),
                new DefaultHandler()});
        server.setHandler(handlers);
        server.start();
        info("Purchasing microservice GUI started on http://localhost:" + port);
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

    public void stop() throws Exception {
        server.stop();
    }
}
