package com.trafficparrot.example.testing.framework;

import com.codeborne.selenide.Browsers;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.logevents.SelenideLogger;
import io.qameta.allure.selenide.AllureSelenide;
import io.qameta.allure.selenide.LogType;
import org.apache.logging.log4j.ThreadContext;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.logging.Level;

import static com.codeborne.selenide.WebDriverRunner.getAndCheckWebDriver;
import static com.codeborne.selenide.WebDriverRunner.getSelenideProxy;
import static com.trafficparrot.example.testing.framework.RecordBrowser.recordBrowser;
import static com.trafficparrot.example.testing.framework.ReportBrowser.reportBrowserInformation;
import static com.trafficparrot.example.testing.framework.TestData.renderTestData;
import static com.trafficparrot.example.testing.framework.TestData.resetTestData;
import static com.trafficparrot.example.testing.framework.TestState.renderTestState;
import static com.trafficparrot.example.testing.framework.TestState.resetTestState;

public class AcceptanceTestFramework implements TestWatcher, BeforeEachCallback, AfterEachCallback {

    private static final String LOGGING_TEST_NAME = "testName";
    private final RecordNetworkActivity recordNetworkActivity = new RecordNetworkActivity();

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        String testName = testName(extensionContext);
        ThreadContext.put(LOGGING_TEST_NAME, testName);

        Configuration.browser = Browsers.CHROME;
        Configuration.proxyEnabled = true;
        Configuration.headless = false;
        Configuration.downloadsFolder = "target/selenide/downloads";
        Configuration.reportsFolder = "target/selenide/reports";

        resetTestData();
        resetTestState();

        getAndCheckWebDriver();
        if (getSelenideProxy().responseFilter(recordNetworkActivity.getClass().getSimpleName()) == null) {
            getSelenideProxy().addResponseFilter(recordNetworkActivity.getClass().getSimpleName(), recordNetworkActivity);
        }
        recordBrowser().testStarted(testName);
        SelenideLogger.addListener(AllureSelenide.class.getSimpleName(), new AllureSelenide()
                .enableLogs(LogType.BROWSER, Level.ALL)
                .includeSelenideSteps(false)
                .savePageSource(false)
                .screenshots(true));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        reportBrowserInformation();

        renderTestData();
        renderTestState();

        recordNetworkActivity.renderNetworkActivity();
        recordBrowser().testFinished();

        ThreadContext.remove(LOGGING_TEST_NAME);
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        recordBrowser().recordFrame();
    }

    private static String testName(ExtensionContext context) {
        String testName = context.getTestClass().orElseThrow(() -> new IllegalStateException("Unknown test class")).getName();
        String testMethod = context.getTestMethod().orElseThrow(() -> new IllegalStateException("Unknown test method")).getName();
        return testName + "." + testMethod;
    }
}
