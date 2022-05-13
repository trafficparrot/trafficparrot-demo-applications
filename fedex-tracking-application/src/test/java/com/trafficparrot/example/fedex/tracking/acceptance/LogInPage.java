package com.trafficparrot.example.fedex.tracking.acceptance;

import com.trafficparrot.example.testing.framework.Password;
import io.qameta.allure.Step;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.url;
import static com.trafficparrot.example.testing.framework.TestData.testData;
import static org.openqa.selenium.By.name;

public class LogInPage {

    @Step
    public static void givenUserHasLoggedIn(FedExApiUser fedExApiUser) {
        givenTheUserVisitsTheLogInPage();
        whenTheUserLogsIn(fedExApiUser);
        thenTheTrackingPageIsDisplayed();
    }

    @Step
    public static void givenTheUserVisitsTheLogInPage() {
        open(testData(TrackingApplicationUrl.class) + "/login");
    }

    @Step
    public static void whenTheUserLogsIn(FedExApiUser fedExApiUser) {
        whenTheUserEntersTheirDetails(fedExApiUser.apiKey, fedExApiUser.secretKey, fedExApiUser.baseUrl);
        andClicksLogIn();
    }

    @Step
    public static void whenTheUserEntersTheirDetails(String apiKey, Password secretKey, String baseUrl) {
        $(name("fedex-api-key")).val(apiKey);
        $(name("fedex-secret-key")).val(secretKey.value);
        $(name("fedex-api-base-url")).val(baseUrl);
    }

    @Step
    public static void andClicksLogIn() {
        $(byText("Submit")).click();
    }

    @Step
    public static void thenTheTrackingPageIsDisplayed() {
        webdriver().shouldHave(url(testData(TrackingApplicationUrl.class) + "/track"));
    }

    @Step
    public static void thenAnErrorMessageIsDisplayed(String error) {
        $(byClassName("invalid-feedback")).shouldHave(text(error));
    }
}
