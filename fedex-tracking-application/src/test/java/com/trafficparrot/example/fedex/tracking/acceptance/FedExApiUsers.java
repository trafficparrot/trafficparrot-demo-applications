package com.trafficparrot.example.fedex.tracking.acceptance;

import static com.trafficparrot.example.fedex.tracking.acceptance.FedExApiUser.apiUser;
import static com.trafficparrot.example.fedex.tracking.acceptance.FedExApiUser.apiUserWithSecretKeyHidden;

public class FedExApiUsers {
    public static final FedExApiUser FED_EX_API_USER = apiUserWithSecretKeyHidden(
            "FedEx API User",
            "l7a16a9564476045798a17887faec0929a",
            "https://apis-sandbox.fedex.com"
    );

    public static final FedExApiUser INVALID_USER = apiUser(
            "Invalid User",
            "invalid",
            "invalid",
            "https://apis-sandbox.fedex.com"
    );
}
