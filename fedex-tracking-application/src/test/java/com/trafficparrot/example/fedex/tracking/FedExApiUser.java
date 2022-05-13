package com.trafficparrot.example.fedex.tracking;


import com.trafficparrot.example.testing.framework.Password;

import static com.trafficparrot.example.testing.framework.Password.loadPassword;
import static com.trafficparrot.example.testing.framework.Password.specifyPassword;

public class FedExApiUser {
    public final String displayName;
    public final String apiKey;
    public final Password secretKey;
    public final String baseUrl;

    private FedExApiUser(String displayName, String apiKey, Password secretKey, String baseUrl) {
        this.displayName = displayName;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }

    public static FedExApiUser apiUser(String displayName, String apiKey, String secretKey, String baseUrl) {
        return new FedExApiUser(displayName, apiKey, specifyPassword(secretKey), baseUrl);
    }

    public static FedExApiUser apiUserWithSecretKeyHidden(String displayName, String apiKey, String baseUrl) {
        return new FedExApiUser(displayName, apiKey, loadPassword(apiKey), baseUrl);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
