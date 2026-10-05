package com.trafficparrot.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ThriftCalculatorClientProperties {

    private static final String PROPERTIES_FILE = "thrift.calculator.client.properties";

    private final Properties properties;

    public ThriftCalculatorClientProperties(Properties properties) {
        this.properties = properties;
    }

    public static ThriftCalculatorClientProperties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = ThriftCalculatorClientProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            properties.load(inputStream);
        }
        return new ThriftCalculatorClientProperties(properties);
    }

    public int httpPort() {
        return Integer.parseInt(properties.getProperty("thrift.calculator.client.http.port"));
    }
}
