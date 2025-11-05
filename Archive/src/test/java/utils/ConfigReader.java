package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration values from src/main/resources/config.properties,
 * allowing overrides via Java System Properties (e.g., from Gradle CLI).
 */
public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {

            if (input == null) {
                throw new RuntimeException("Unable to find config.properties in classpath");
            }

            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config.properties", e);
        }
    }

    public static String get(String key) {
        String cliValue = System.getProperty(key);
        if (cliValue != null) {
            return cliValue.trim();
        }

        String fileValue = properties.getProperty(key);
        if (fileValue == null) {
            throw new IllegalArgumentException("Missing configuration key: " + key);
        }
        return fileValue.trim();
    }
}