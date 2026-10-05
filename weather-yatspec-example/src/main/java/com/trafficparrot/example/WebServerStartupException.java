package com.trafficparrot.example;

public class WebServerStartupException extends RuntimeException {
    public WebServerStartupException(Exception e) {
        super(e);
    }
}
