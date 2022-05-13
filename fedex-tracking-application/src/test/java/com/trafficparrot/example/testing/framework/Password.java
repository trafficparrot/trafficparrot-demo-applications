package com.trafficparrot.example.testing.framework;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class Password {

    public final String value;

    private Password(String value) {
        this.value = value;
    }

    public static Password specifyPassword(String value) {
        return new Password(value);
    }

    public static Password loadPassword(String key) {
        String passwordFromEnvironment = System.getenv(key);
        if (passwordFromEnvironment != null) {
            return new Password(passwordFromEnvironment);
        }
        Path propertiesFile = Paths.get("passwords.properties");
        if (Files.exists(propertiesFile)) {
            Properties properties = new Properties();
            try (FileInputStream fileInputStream = new FileInputStream(propertiesFile.toFile())) {
                properties.load(fileInputStream);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load password properties file", e);
            }
            String passwordFromProperties = properties.getProperty(key);
            if (passwordFromProperties != null) {
                return new Password(passwordFromProperties);
            }
        }
        throw new IllegalStateException("Could not find password for '" + key + "', please either:" + System.lineSeparator() +
                " * set an environment variable '" + key + "=password'" + System.lineSeparator() +
                " * or populate '" + propertiesFile.toAbsolutePath() + "' with line '" + key + "=password'");
    }

    @Override
    public String toString() {
        return "*****";
    }
}
