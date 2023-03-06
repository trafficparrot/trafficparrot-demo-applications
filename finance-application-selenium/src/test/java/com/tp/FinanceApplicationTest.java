package com.tp;

import com.wbsoftwareconsutlancy.FinanceApplication;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import static junit.framework.TestCase.assertEquals;

public class FinanceApplicationTest {
    private FinanceApplication financeApplication = new FinanceApplication();
    private ChromeDriver driver;

    @Before
    public void setUp() throws Exception {
        System.setProperty("webdriver.chrome.driver","/optf/chromedriver_linux64/chromedriver");
        financeApplication.start();
    }

    @After
    public void tearDown() throws Exception {
        financeApplication.stop();
        driver.quit();
    }

    @Test
    public void parsesLastPriceFromStockQuote() throws Exception {
        whenUserOpensTheFinanceApplicationMainPage();
        thenThePriceIsShown();
    }

    private void thenThePriceIsShown() {
        WebElement span = driver.findElement(By.id("stock-quote-last-price"));
        assertEquals("103.17", span.getText());
    }

    private void whenUserOpensTheFinanceApplicationMainPage() {
        driver = new ChromeDriver();
        driver.get("http://localhost:8282");
    }
}