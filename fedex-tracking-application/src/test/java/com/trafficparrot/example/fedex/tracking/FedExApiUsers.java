package com.trafficparrot.example.fedex.tracking;

import static com.trafficparrot.example.fedex.tracking.FedExApiUser.apiUser;
import static com.trafficparrot.example.fedex.tracking.FedExApiUser.apiUserWithSecretKeyHidden;
import static com.trafficparrot.example.testing.framework.TestDataGenerator.apiKey;
import static com.trafficparrot.example.testing.framework.TestDataGenerator.secretKey;

public class FedExApiUsers {

    public static final FedExApiUser FED_EX_API_USER = apiUserWithSecretKeyHidden(
            "FedEx API User",
            "l7a16a9564476045798a17887faec0929a",
            "https://apis-sandbox.fedex.com"
    );

    public static final FedExApiUser INVALID_USER = apiUser(
            "Invalid User",
            apiKey(),
            secretKey(),
            "https://apis-sandbox.fedex.com"
    );
}
