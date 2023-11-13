package com.trafficparrot.example.fedex.tracking.acceptance;

import com.trafficparrot.example.testing.framework.AcceptanceTestFramework;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;

import static com.trafficparrot.example.fedex.tracking.FedExApiUsers.FED_EX_API_USER;
import static com.trafficparrot.example.fedex.tracking.acceptance.Epics.FEDEX_TRACKING;
import static com.trafficparrot.example.fedex.tracking.acceptance.Features.TRACK;
import static com.trafficparrot.example.fedex.tracking.acceptance.LogInPage.givenUserHasLoggedIn;
import static com.trafficparrot.example.fedex.tracking.acceptance.TrackPage.*;
import static com.trafficparrot.example.testing.framework.TestData.usingTestData;
import static com.trafficparrot.example.testing.framework.TestTags.ACCEPTANCE_TEST;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Epic(FEDEX_TRACKING)
@Feature(TRACK)
@Tag(ACCEPTANCE_TEST)
@ExtendWith(AcceptanceTestFramework.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class TrackAcceptanceTest {

    @Test
    void trackUsingTrackingNumberSuccess() {
        givenUserHasLoggedIn(FED_EX_API_USER);
        whenTheUserLooksUpTrackingNumber("123456789012");
        thenTheLatestTrackingStatusIsDisplayed("In transit at 2023-05-19T19:03:00-04:00");
    }

    @Test
    void trackUsingTrackingNumberFailure() {
        givenUserHasLoggedIn(FED_EX_API_USER);
        whenTheUserLooksUpTrackingNumber("");
        thenAnErrorMessageIsDisplayed("Please provide tracking number.");
    }

    @BeforeEach
    void setUp() {
        usingTestData(new TrackingApplicationUrl("http://localhost:" + port));
    }

    private final int port;

    @Autowired
    public TrackAcceptanceTest(@LocalServerPort int port) {
        this.port = port;
    }
}
