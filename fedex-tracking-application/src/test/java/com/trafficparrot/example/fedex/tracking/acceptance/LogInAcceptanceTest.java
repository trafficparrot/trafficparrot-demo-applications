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
import static com.trafficparrot.example.fedex.tracking.FedExApiUsers.INVALID_USER;
import static com.trafficparrot.example.fedex.tracking.acceptance.Epics.FEDEX_TRACKING;
import static com.trafficparrot.example.fedex.tracking.acceptance.Features.LOG_IN;
import static com.trafficparrot.example.fedex.tracking.acceptance.LogInPage.*;
import static com.trafficparrot.example.testing.framework.TestData.usingTestData;
import static com.trafficparrot.example.testing.framework.TestTags.ACCEPTANCE_TEST;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Epic(FEDEX_TRACKING)
@Feature(LOG_IN)
@Tag(ACCEPTANCE_TEST)
@ExtendWith(AcceptanceTestFramework.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class LogInAcceptanceTest {

    @Test
    void logInToTheTrackingApplicationSuccess() {
        givenTheUserVisitsTheLogInPage();
        whenTheUserLogsIn(FED_EX_API_USER);
        thenTheTrackingPageIsDisplayed();
    }

    @Test
    void logInToTheTrackingApplicationFailure() {
        givenTheUserVisitsTheLogInPage();
        whenTheUserLogsIn(INVALID_USER);
        thenAnErrorMessageIsDisplayed("The given client credentials were not valid. Please modify your request and try again.");
    }

    @BeforeEach
    void setUp() {
        usingTestData(new TrackingApplicationUrl("http://localhost:" + port));
    }

    private final int port;

    @Autowired
    public LogInAcceptanceTest(@LocalServerPort int port) {
        this.port = port;
    }
}
