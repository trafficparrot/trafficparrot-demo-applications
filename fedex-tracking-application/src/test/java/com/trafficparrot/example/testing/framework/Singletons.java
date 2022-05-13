package com.trafficparrot.example.testing.framework;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.builder.ToStringStyle.JSON_STYLE;

class Singletons {

    private final Map<String, Object> singletons = new HashMap<>();
    private final String kind;

    Singletons(String kind) {
        this.kind = kind;
    }

    void declare(Object... states) {
        if (!singletons.isEmpty()) {
            throw new IllegalStateException("Test " + kind + " in use were already declared");
        }
        for (Object state : states) {
            Class<?> type = state.getClass();
            if (singletons.containsKey(type.getName())) {
                throw new IllegalStateException("There can only be one " + type.getSimpleName() + " per test!");
            }
            singletons.put(type.getName(), state);
        }
    }

    <T> T get(Class<T> type) {
        if (singletons.isEmpty()) {
            throw new IllegalStateException("There were no test " + kind + " declared in this test!");
        }
        if (!singletons.containsKey(type.getName())) {
            throw new IllegalStateException("There was no " + type.getSimpleName() + " declared in this test!");
        }
        return type.cast(singletons.get(type.getName()));
    }

    String renderTestState() {
        return singletons.values().stream()
                .map(Singletons::reflectionToString)
                .collect(joining(lineSeparator() + lineSeparator()));
    }

    private static String reflectionToString(Object object) {
        return object.getClass().getSimpleName() + ":" + lineSeparator() + ReflectionToStringBuilder.toString(object, JSON_STYLE);
    }
}
