package com.trafficparrot.example.fedex.tracking.integration;

import com.trafficparrot.example.fedex.tracking.FedExTrackingService;
import com.trafficparrot.example.fedex.tracking.LogInResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import static com.trafficparrot.example.fedex.tracking.FedExApiUsers.FED_EX_API_USER;
import static com.trafficparrot.example.fedex.tracking.FedExApiUsers.INVALID_USER;
import static com.trafficparrot.example.testing.framework.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD;

@Tag(INTEGRATION_TEST)
@SpringBootTest
@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)
public class LogInIntegrationTest {

    @Test
    void logInSuccess() {
        LogInResponse logInResponse = trackingService.loginToApi(FED_EX_API_USER.apiKey, FED_EX_API_USER.secretKey.value, FED_EX_API_USER.baseUrl);

        assertThat(logInResponse.isError).isFalse();
        assertThat(logInResponse.errorMessage).isEmpty();
        assertThat(trackingService.isLoggedIn()).isTrue();
    }

    @Test
    void logInFailure() {
        LogInResponse logInResponse = trackingService.loginToApi(INVALID_USER.apiKey, INVALID_USER.secretKey.value, INVALID_USER.baseUrl);

        assertThat(logInResponse.isError).isTrue();
        assertThat(logInResponse.errorMessage).isEqualTo("The given client credentials were not valid. Please modify your request and try again.");
        assertThat(trackingService.isLoggedIn()).isFalse();
    }

    private final FedExTrackingService trackingService;

    @Autowired
    public LogInIntegrationTest(FedExTrackingService trackingService) {
        this.trackingService = trackingService;
    }
}
