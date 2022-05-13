package com.trafficparrot.example.fedex.tracking;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class FedExTrackingServiceTest {

    private final FedExApiCredentials fedExApiCredentials = mock(FedExApiCredentials.class);
    private final HttpClientFactory httpClientFactory = mock(HttpClientFactory.class);
    private final OkHttpClient httpClient = mock(OkHttpClient.class);
    private final Call httpCall = mock(Call.class);
    private final Response httpResponse = mock(Response.class);
    private final FedExTrackingService fedExTrackingService = new FedExTrackingService(fedExApiCredentials, httpClientFactory);

    @Test
    void isLoggedInWhenHasApiCredentialsTrue() {
        given(fedExApiCredentials.hasApiCredentials()).willReturn(true);

        assertThat(fedExTrackingService.isLoggedIn()).isTrue();
    }

    @Test
    void isNotLoggedInWhenHasApiCredentialsFalse() {
        given(fedExApiCredentials.hasApiCredentials()).willReturn(false);

        assertThat(fedExTrackingService.isLoggedIn()).isFalse();
    }

    @Test
    void loginToApiLogsErrorWhenUrlIsInvalid(CapturedOutput output) {
        LogInResponse logInResponse = fedExTrackingService.loginToApi("any", "any", "invalid");

        assertThat(logInResponse.isError).isTrue();
        assertThat(logInResponse.errorMessage).isEqualTo("Technical error, see logs for details");

        assertThat(output).containsSubsequence(
                "ERROR",  "FedExTrackingService",  "Uncaught exception",
                "java.lang.IllegalArgumentException: Expected URL scheme 'http' or 'https' but no colon was found"
        );
    }

    @Test
    void loginToApiLogsErrorWhenResponseBodyIsMissing(CapturedOutput output) {
        given(httpResponse.body()).willReturn(null);

        LogInResponse logInResponse = fedExTrackingService.loginToApi("any", "any", "http://localhost:1234");

        assertThat(logInResponse.isError).isTrue();
        assertThat(logInResponse.errorMessage).isEqualTo("Technical error, see logs for details");

        assertThat(output).contains("ERROR", "FedExTrackingService", "Missing response body");
    }

    @Test
    void trackByTrackingNumberLogsErrorWhenNotLoggedIn(CapturedOutput output) {
        TrackingResponse trackingResponse = fedExTrackingService.trackByTrackingNumber("any");

        assertThat(trackingResponse.isError).isTrue();
        assertThat(trackingResponse.errorMessage).isEqualTo("Technical error, see logs for details");

        assertThat(output).containsSubsequence(
                "ERROR", "FedExTrackingService", "Uncaught exception",
                "java.lang.IllegalArgumentException: Expected URL scheme 'http' or 'https' but no colon was found"
        );
    }

    @Test
    void trackByTrackingNumberLogsErrorWhenResponseBodyIsMissing(CapturedOutput output) {
        given(httpResponse.body()).willReturn(null);
        given(fedExApiCredentials.getApiBaseUrl()).willReturn("http://localhost:1234");

        TrackingResponse trackingResponse = fedExTrackingService.trackByTrackingNumber("any");

        assertThat(trackingResponse.isError).isTrue();
        assertThat(trackingResponse.errorMessage).isEqualTo("Technical error, see logs for details");

        assertThat(output).containsSubsequence("ERROR", "FedExTrackingService",  "Missing response body");
    }

    @Test
    void trackByTrackingNumberReturnsErrorInSuccessfulTrackResult(CapturedOutput output) {
        String testErrorMessage = "test error message";

        given(fedExApiCredentials.getApiBaseUrl()).willReturn("http://localhost:1234");
        given(httpResponse.body()).willReturn(ResponseBody.create(MediaType.parse("application/json"), "{\n" +
                "  \"output\": {\n" +
                "    \"completeTrackResults\": [{\n" +
                "      \"trackResults\": [{\n" +
                "        \"error\": {\n" +
                "          \"message\": \"" + testErrorMessage + "\"\n" +
                "        }\n" +
                "      }]\n" +
                "    }]\n" +
                "  }\n" +
                "}"));
        given(httpResponse.code()).willReturn(200);

        TrackingResponse trackingResponse = fedExTrackingService.trackByTrackingNumber("any");

        assertThat(trackingResponse.isError).isTrue();
        assertThat(trackingResponse.errorMessage).isEqualTo(testErrorMessage);

        assertThat(output).doesNotContain("ERROR");
    }

    @BeforeEach
    void setUp() throws IOException {
        given(httpClientFactory.httpClient()).willReturn(httpClient);
        given(httpClient.newCall(any())).willReturn(httpCall);
        given(httpCall.execute()).willReturn(httpResponse);
    }
}