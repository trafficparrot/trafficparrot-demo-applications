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

    @Override
    public String toString() {
        return "ProvisionRequest{" +
                "mobileNumber='" + mobileNumber + '\'' +
                ", mobileType='" + mobileType + '\'' +
                '}';
    }
}
