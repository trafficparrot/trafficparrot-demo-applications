package com.trafficparrot.example.testing.framework;

import io.qameta.allure.Attachment;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Platform;
import org.openqa.selenium.remote.RemoteWebDriver;

import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

class ReportBrowser {

    @Attachment(value = "Browser information", type = "text/plain")
    static String reportBrowserInformation() {
        Capabilities capabilities = ((RemoteWebDriver) getWebDriver()).getCapabilities();
        String browserName = capabilities.getBrowserName();
        Platform platform = capabilities.getPlatform();
        String version = capabilities.getVersion();
        return String.format(
                "Browser: %s%n" +
                "Version: %s%n" +
                "Platform: %s%n", browserName, version, platform);
    }
}
