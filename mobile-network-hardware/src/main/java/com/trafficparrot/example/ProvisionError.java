package com.trafficparrot.example;

public class ProvisionError {
    private final String mobileNumber;
    private final String mobileType;
    private final String error;
    private final String format;

    public ProvisionError(String mobileNumber, String mobileType, String error, String format) {
        this.mobileNumber = mobileNumber;
        this.mobileType = mobileType;
        this.error = error;
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public String getError() {
        return error;
    }

    public String getMobileType() {
        return mobileType;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    @Override
    public String toString() {
        return "ProvisionError{" +
                "mobileNumber='" + mobileNumber + '\'' +
                ", mobileType='" + mobileType + '\'' +
                ", error='" + error + '\'' +
                ", format='" + format + '\'' +
                '}';
    }
}
