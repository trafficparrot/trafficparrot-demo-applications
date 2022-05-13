package com.trafficparrot.example.testing.framework;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import java.util.Arrays;

import static com.trafficparrot.example.testing.framework.RecordBrowser.recordBrowser;

@Aspect
public class RecordBrowserAspect {

    @AfterReturning("execution(public org.openqa.selenium.WebElement org.openqa.selenium.SearchContext+.findElement(org.openqa.selenium.By))")
    public void screenshotAfterFindElement() {
        recordOneFrame();
    }

    @AfterReturning("execution(public java.util.List<org.openqa.selenium.WebElement> org.openqa.selenium.SearchContext+.findElements(org.openqa.selenium.By))")
    public void screenshotAfterFindElements() {
        recordOneFrame();
    }

    private void recordOneFrame() {
        if (notRecordingFrame(Thread.currentThread().getStackTrace())) {
            recordBrowser().recordFrame();
        }
    }

    private boolean notRecordingFrame(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace).noneMatch(element -> element.getClassName().equals(RecordBrowser.class.getName()));
    }
}

