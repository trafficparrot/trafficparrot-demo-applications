package com.trafficparrot.example.fedex.tracking.integration;

import com.trafficparrot.example.fedex.tracking.FedExTrackingService;
import com.trafficparrot.example.fedex.tracking.LogInResponse;
import com.trafficparrot.example.fedex.tracking.TrackingResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.trafficparrot.example.fedex.tracking.FedExApiUsers.FED_EX_API_USER;
import static com.trafficparrot.example.testing.framework.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@Tag(INTEGRATION_TEST)
@SpringBootTest
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class TrackIntegrationTest {

    @Test
    void trackSuccess() {
        logInSuccess();

        TrackingResponse trackingResponse = trackingService.trackByTrackingNumber("123456789012");

        assertThat(trackingResponse.isError).isFalse();
        assertThat(trackingResponse.errorMessage).isEmpty();
        assertThat(trackingResponse.latestStatus).matches("(Picked up|In transit) at .*");
    }

    @Test
    void trackFailure() {
        logInSuccess();

        TrackingResponse trackingResponse = trackingService.trackByTrackingNumber("");

        assertThat(trackingResponse.isError).isTrue();
        assertThat(trackingResponse.errorMessage).isEqualTo("Please provide tracking number.");
        assertThat(trackingResponse.latestStatus).isEmpty();
    }

    void logInSuccess() {
        LogInResponse logInResponse = trackingService.loginToApi(FED_EX_API_USER.apiKey, FED_EX_API_USER.secretKey.value, FED_EX_API_USER.baseUrl);

        assertThat(logInResponse.isError).isFalse();
        assertThat(logInResponse.errorMessage).isEmpty();
        assertThat(trackingService.isLoggedIn()).isTrue();
    }

    private final FedExTrackingService trackingService;

    @Autowired
    public TrackIntegrationTest(FedExTrackingService trackingService) {
        this.trackingService = trackingService;
    }
}
