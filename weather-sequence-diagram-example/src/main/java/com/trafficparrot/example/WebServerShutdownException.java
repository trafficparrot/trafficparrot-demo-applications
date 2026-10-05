package com.trafficparrot.example;

public class WebServerShutdownException extends RuntimeException {
    public WebServerShutdownException(Exception e) {
        super(e);
    }
}
