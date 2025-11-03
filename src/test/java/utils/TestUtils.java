package utils;

// Import ConfigReader as it's used in parseValue
// Note: ConfigReader must be available in the utils package, as seen in previous context.
// Assuming your ConfigReader class is in the utils package based on the project structure.

public final class TestUtils {

    // Prevent direct instantiation
    private TestUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
    }

    /**
     * Generates a string composed of the character 'A' repeated 'length' times.
     */
    public static String generateLargeString(int length) {
        return "A".repeat(length);
    }

    /**
     * Parses a raw string value from a feature file, handling special placeholders 
     * (<missing>, <valid>), stripping quotes, and converting to the appropriate 
     * primitive type (Boolean, Integer, Double) or returning the string value.
     */
    public static Object parseValue(String raw, String field) {
        if ("<missing>".equalsIgnoreCase(raw)) return null;
        if ("<valid>".equalsIgnoreCase(raw))
            return ConfigReader.get(field);

        // Strip quotes if present
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            return raw.substring(1, raw.length() - 1);
        }

        // Booleans
        if ("true".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw)) return false;

        // Numbers (int or float)
        try {
            if (raw.contains(".")) return Double.parseDouble(raw);
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
        }

        return raw;
    }
}