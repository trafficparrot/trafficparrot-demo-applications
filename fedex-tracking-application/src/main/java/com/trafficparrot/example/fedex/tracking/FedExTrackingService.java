package com.trafficparrot.example.fedex.tracking;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.jayway.jsonpath.Option.SUPPRESS_EXCEPTIONS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
public class FedExTrackingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FedExTrackingService.class);
    private static final String FEDEX_JSON_PATH_FIRST_TRACK_RESULT = "$.output.completeTrackResults[0].trackResults[0]";
    private static final String FEDEX_JSON_PATH_TRACK_MOST_RECENT_SCAN_EVENT = FEDEX_JSON_PATH_FIRST_TRACK_RESULT + ".scanEvents[0]";
    private static final String FEDEX_JSON_PATH_ERROR_MESSAGE = "$.errors[0].message";

    private final FedExApiCredentials fedExApiCredentials;
    private final HttpClientFactory httpClientFactory;

    @Autowired
    public FedExTrackingService(FedExApiCredentials fedExApiCredentials, HttpClientFactory httpClientFactory) {
        this.fedExApiCredentials = fedExApiCredentials;
        this.httpClientFactory = httpClientFactory;
    }

    public boolean isLoggedIn() {
        return fedExApiCredentials.hasApiCredentials();
    }

    public LogInResponse loginToApi(String apiKey, String secretKey, String apiBaseUrl) {
        try {
            OkHttpClient client = httpClientFactory.httpClient();
            RequestBody body = new FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .add("client_id", apiKey)
                    .add("client_secret", secretKey)
                    .build();
            Request request = new Request.Builder()
                    .url(apiBaseUrl + "/oauth/token")
                    .post(body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    LOGGER.error("Missing response body");
                    return logInTechnicalErrorResponse();
                }
                DocumentContext responseJson = parseJson(responseBody.string());
                if (response.code() == 200) {
                    String accessToken = responseJson.read("$.access_token");
                    fedExApiCredentials.setApiCredentials(apiBaseUrl, accessToken);
                    return new LogInResponse(false, "");
                } else {
                    String errorMessage = responseJson.read(FEDEX_JSON_PATH_ERROR_MESSAGE);
                    return new LogInResponse(true, errorMessage);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Uncaught exception", e);
            return logInTechnicalErrorResponse();
        }
    }

    public TrackingResponse trackByTrackingNumber(String trackingNumber) {
        if (isBlank(trackingNumber)) {
            return new TrackingResponse(true, "Please provide tracking number.", "");
        }
        String requestJson = "{\n" +
                "  \"includeDetailedScans\": true,\n" +
                "  \"trackingInfo\": [\n" +
                "    {\n" +
                "      \"trackingNumberInfo\": {\n" +
                "        \"trackingNumber\": \"" + trackingNumber + "\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        try {
            OkHttpClient client = httpClientFactory.httpClient();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, requestJson);
            Request request = new Request.Builder()
                    .url(fedExApiCredentials.getApiBaseUrl() + "/track/v1/trackingnumbers")
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("X-locale", "en_US")
                    .addHeader("Authorization", "Bearer " + fedExApiCredentials.getBearerToken())
                    .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    LOGGER.error("Missing response body");
                    return trackingTechnicalErrorResponse();
                }
                DocumentContext responseJson = parseJson(responseBody.string());
                if (response.code() == 200) {
                    String errorMessage = responseJson.read(FEDEX_JSON_PATH_FIRST_TRACK_RESULT + ".error.message");
                    if (isNotBlank(errorMessage)) {
                        return new TrackingResponse(true, errorMessage, "");
                    }

                    String derivedStatus = responseJson.read(FEDEX_JSON_PATH_TRACK_MOST_RECENT_SCAN_EVENT + ".derivedStatus");
                    String date = responseJson.read(FEDEX_JSON_PATH_TRACK_MOST_RECENT_SCAN_EVENT + ".date");
                    return new TrackingResponse(false, "", derivedStatus + " at " + date);
                } else {
                    String errorMessage = responseJson.read(FEDEX_JSON_PATH_ERROR_MESSAGE);
                    return new TrackingResponse(true, errorMessage, "");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Uncaught exception", e);
            return trackingTechnicalErrorResponse();
        }
    }

    private DocumentContext parseJson(String responseBody) {
        return JsonPath.parse(responseBody, Configuration.builder().options(SUPPRESS_EXCEPTIONS).build());
    }

    private TrackingResponse trackingTechnicalErrorResponse() {
        return new TrackingResponse(true, "Technical error, see logs for details", "");
    }

    private LogInResponse logInTechnicalErrorResponse() {
        return new LogInResponse(true, "Technical error, see logs for details");
    }
}
