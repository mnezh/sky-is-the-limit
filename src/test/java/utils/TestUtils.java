package utils;

import java.util.HashMap;
import java.util.Map;

// Import ConfigReader as it's used in parseValue
// Note: ConfigReader must be available in the utils package, as seen in previous context.
// Assuming your ConfigReader class is in the utils package based on the project structure.

public final class TestUtils {

  // Prevent direct instantiation
  private TestUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated.");
  }

  /** Generates a string composed of the character 'A' repeated 'length' times. */
  public static String generateLargeString(int length) {
    return "A".repeat(length);
  }

  /**
   * Parses a raw string value from a feature file, handling special placeholders (<missing>,
   * <valid>), stripping quotes, and converting to the appropriate primitive type (Boolean, Integer,
   * Double) or returning the string value.
   */
  public static Object parseValue(String raw, String field) {
    if ("<missing>".equalsIgnoreCase(raw)) return null;
    if ("<valid>".equalsIgnoreCase(raw)) return ConfigReader.get(field);

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

  public static Map<String, Object> createValidBookingPayload(Map<String, Object> overrides) {
    // Default payload
    Map<String, Object> payload = new HashMap<>();
    payload.put("firstname", "Jim");
    payload.put("lastname", "Brown");
    payload.put("totalprice", 111);
    payload.put("depositpaid", true);

    Map<String, Object> bookingDates = new HashMap<>();
    bookingDates.put("checkin", "2024-01-01");
    bookingDates.put("checkout", "2024-01-10");
    payload.put("bookingdates", bookingDates);

    payload.put("additionalneeds", "Breakfast");

    // Apply overrides (supports dot-notation for nested keys)
    if (overrides != null) {
      for (Map.Entry<String, Object> entry : overrides.entrySet()) {
        applyJsonPathOverride(payload, entry.getKey(), entry.getValue());
      }
    }

    return payload;
  }

  // Overload for convenience (no overrides)
  public static Map<String, Object> createValidBookingPayload() {
    return createValidBookingPayload(null);
  }

  // Helper to apply "dot" path overrides
  private static void applyJsonPathOverride(Map<String, Object> root, String path, Object value) {
    String[] parts = path.split("\\.");
    Map<String, Object> current = root;

    for (int i = 0; i < parts.length; i++) {
      String key = parts[i];
      if (i == parts.length - 1) {
        current.put(key, value);
      } else {
        Object next = current.get(key);
        if (!(next instanceof Map)) {
          next = new HashMap<String, Object>();
          current.put(key, next);
        }
        current = (Map<String, Object>) next;
      }
    }
  }

  public static Map<String, Object> createBookingPayloadMissingField(String field) {
    Map<String, Object> payload = new HashMap<>(createValidBookingPayload());
    payload.remove(field);
    return payload;
  }

  public static Map<String, Object> createBookingPayloadWithInvalidDate() {
    Map<String, Object> payload = new HashMap<>(createValidBookingPayload());
    Map<String, Object> bookingdates =
        new HashMap<>((Map<String, Object>) payload.get("bookingdates"));
    bookingdates.put("checkin", "2024/01/01"); // invalid format
    payload.put("bookingdates", bookingdates);
    return payload;
  }

  public static String escapeXml(String xml) {
    return xml.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
