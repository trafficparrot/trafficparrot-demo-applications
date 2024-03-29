package com.trafficparrot.example.testing.framework;

import io.qameta.allure.Attachment;

import static java.lang.ThreadLocal.withInitial;

/**
 * Provides a way to manage test state e.g. saving an ID generated by the system under test for use in a later step.
 *
 * Call {@link #usingTestState(Object...)} once per test to declare the test states that will be used in the test.
 * Call {@link #testState(Class)} to get the singleton test state of the given type.
 */
public class TestState {
    private static final ThreadLocal<Singletons> TEST_STATE = withInitial(() -> new Singletons("states"));

    private TestState() {
        // thread local only
    }

    /**
     * @param states singleton test state
     */
    public static void usingTestState(Object... states) {
        TEST_STATE.get().declare(states);
    }

    /**
     * @param type test state type
     * @return singleton test state of given type
     */
    public static <T> T testState(Class<T> type) {
        return TEST_STATE.get().get(type);
    }

    @Attachment(value = "Test state", type = "text/plain")
    static String renderTestState() {
        return TEST_STATE.get().renderTestState();
    }

    static void resetTestState() {
        TEST_STATE.remove();
    }
}
