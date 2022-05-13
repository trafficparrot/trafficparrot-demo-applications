package com.trafficparrot.example.fedex.tracking.acceptance;

import io.qameta.allure.Step;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selectors.byClassName;
import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverConditions.url;
import static com.trafficparrot.example.testing.framework.TestState.testState;
import static org.openqa.selenium.By.name;

public class TrackPage {
    @Step
    public static void whenTheUserLooksUpTrackingNumber(String trackingNumber) {
        whenTheUserEntersTrackingNumber(trackingNumber);
        andClicksTrack();
    }

    @Step
    public static void whenTheUserEntersTrackingNumber(String trackingNumber) {
        $(name("fedex-tracking-number")).val(trackingNumber);
    }

    @Step
    public static void andClicksTrack() {
        $(byText("Submit")).click();
    }

    @Step
    public static void thenTheLatestTrackingStatusIsDisplayed(String status) {
        $(byClassName("valid-feedback")).shouldHave(text(status));
    }

    @Step
    public static void thenAnErrorMessageIsDisplayed(String error) {
        $(byClassName("invalid-feedback")).shouldHave(text(error));
    }
}
