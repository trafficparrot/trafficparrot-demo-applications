package com.trafficparrot.example;

public class ProvisionRequest {
    private final String mobileNumber;
    private final String mobileType;

    public ProvisionRequest(String mobileNumber, String mobileType) {
        this.mobileNumber = mobileNumber;
        this.mobileType = mobileType;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getMobileType() {
        return mobileType;
    }
}
