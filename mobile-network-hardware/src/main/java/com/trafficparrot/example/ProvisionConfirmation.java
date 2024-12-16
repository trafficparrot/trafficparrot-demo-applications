package com.trafficparrot.example;

public class ProvisionConfirmation {
    private final String mobileNumber;
    private final String mobileType;
    private final String status;
    private final String deviceId;
    private final String date;

    public ProvisionConfirmation(String mobileNumber, String mobileType, String status, String deviceId, String date) {
        this.mobileNumber = mobileNumber;
        this.mobileType = mobileType;
        this.status = status;
        this.deviceId = deviceId;
        this.date = date;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getMobileType() {
        return mobileType;
    }
}
