package com.trafficparrot.example.fedex.tracking;

public class LogInResponse {
    public final boolean isError;
    public final String errorMessage;

    public LogInResponse(boolean isError, String errorMessage) {
        this.isError = isError;
        this.errorMessage = errorMessage;
    }
}
