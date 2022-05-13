package com.trafficparrot.example.fedex.tracking.acceptance;

public class TrackingApplicationUrl {
    public final String url;

    public TrackingApplicationUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }
}
