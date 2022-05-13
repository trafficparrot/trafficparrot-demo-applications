package com.trafficparrot.example.testing.framework;

import com.github.javafaker.Faker;

public class TestDataGenerator {
    private static final Faker FAKER = new Faker();

    public static String apiKey() {
        return FAKER.random().hex(20).toLowerCase();
    }

    public static String secretKey() {
        return FAKER.random().hex(20).toLowerCase();
    }
}
