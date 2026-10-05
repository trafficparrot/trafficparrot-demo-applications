package com.trafficparrot.example;

public class ShutdownException extends RuntimeException {
    public ShutdownException(Exception e) {
        super(e);
    }
}
