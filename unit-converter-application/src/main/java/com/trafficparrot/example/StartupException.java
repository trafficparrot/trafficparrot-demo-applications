package com.trafficparrot.example;

public class StartupException extends RuntimeException {
    public StartupException(Exception e) {
        super(e);
    }
}
