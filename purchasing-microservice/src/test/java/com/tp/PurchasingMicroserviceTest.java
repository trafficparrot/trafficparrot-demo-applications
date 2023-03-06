package com.tp;

import com.wbsoftwareconsutlancy.PurchasingMicroservice;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static junit.framework.TestCase.assertEquals;
import static org.openqa.selenium.support.ui.ExpectedConditions.not;
import static org.openqa.selenium.support.ui.ExpectedConditions.textToBePresentInElement;

public class PurchasingMicroserviceTest {
    public static final By STOCK_PRICE_LOCATOR = By.id("stock-quote-last-price");
    private PurchasingMicroservice purchasingMicroservice = new PurchasingMicroservice();
    private ChromeDriver driver;

    @Before
    public void setUp() throws Exception {
        System.setProperty("webdriver.chrome.driver","/optf/chromedriver_linux64/chromedriver");
        purchasingMicroservice.start();
    }

    @After
    public void tearDown() throws Exception {
        purchasingMicroservice.stop();
        driver.quit();
    }

    @Test
    public void showsAppleStockLastPriceFromQuoteFetchedFromMarketDataApi() throws Exception {
        driver = new ChromeDriver();
        driver.get("http://localhost:8282");
        WebElement span = driver.findElement(STOCK_PRICE_LOCATOR);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(not(textToBePresentInElement(span, "(please wait...)")));
        assertEquals("103.17", span.getText());
    }
}