package com.wbsoftwareconsutlancy;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;

public class AppProperties {
    public static Properties loadProperties() {
        try {
            InputStream propertiesInputStream = getPropertiesInputStream();
            Properties properties = new Properties();
            properties.load(propertiesInputStream);
            return properties;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    private static InputStream getPropertiesInputStream() throws IOException {
        String fileLocation = System.getProperty("finance.application.properties", System.getenv("finance.application.properties"));
        if (fileLocation != null) {
            return Files.newInputStream(new File(fileLocation).toPath());
        } else {
            String classpathFileName = "finance-application.properties";
            InputStream resourceAsStream = FinanceApplication.class.getClassLoader().getResourceAsStream(classpathFileName);
            if (resourceAsStream != null) {
                return resourceAsStream;
            } else {
                throw new FileNotFoundException("property file '" + classpathFileName + "' not found in the classpath");
            }
        }
    }
}
