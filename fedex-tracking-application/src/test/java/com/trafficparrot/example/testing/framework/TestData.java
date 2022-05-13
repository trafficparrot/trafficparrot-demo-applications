package com.trafficparrot.example.testing.framework;

import io.qameta.allure.Attachment;

import static java.lang.ThreadLocal.withInitial;

/**
 * Provides a way to manage test data e.g. the test data used to fill out forms.
 *
 * Call {@link #usingTestData(Object...)} once per test to declare the test data that will be used in the test.
 * Call {@link #testData(Class)} to get the singleton test data of the given type.
 */
public class TestData {
    private static final ThreadLocal<Singletons> TEST_STATE = withInitial(() -> new Singletons("data"));

    private TestData() {
        // thread local only
    }

    /**
     * @param testData singleton test data
     */
    public static void usingTestData(Object... testData) {
        TEST_STATE.get().declare(testData);
    }

    /**
     * @param testDataType test data type
     * @return singleton test data of given type
     */
    public static <T> T testData(Class<T> testDataType) {
        return TEST_STATE.get().get(testDataType);
    }

    @Attachment(value = "Test data", type = "text/plain")
    static String renderTestData() {
        return TEST_STATE.get().renderTestState();
    }

    static void resetTestData() {
        TEST_STATE.remove();
    }
}
