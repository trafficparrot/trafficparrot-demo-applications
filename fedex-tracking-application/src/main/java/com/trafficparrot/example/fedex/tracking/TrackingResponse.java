package com.trafficparrot.example.fedex.tracking;

public class TrackingResponse {
    public final boolean isError;
    public final String errorMessage;
    public final String latestStatus;

    public TrackingResponse(boolean isError, String errorMessage, String latestStatus) {
        this.isError = isError;
        this.errorMessage = errorMessage;
        this.latestStatus = latestStatus;
    }
}
