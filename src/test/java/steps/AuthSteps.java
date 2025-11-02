package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import utils.ConfigReader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AuthSteps {
    private final Map<String, Object> credentials = new HashMap<>();
    private final Map<String, String> scenarioContext = new HashMap<>();
    private Response response;
    private Scenario scenario;
    private String rawBody;
    private String customContentType;

    @Before
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
        this.rawBody = null;
        this.customContentType = null;
        this.credentials.clear();
        this.scenarioContext.clear();
    }

    private String generateLargeString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append('A');
        }
        return sb.toString();
    }

    @Given("I have username {word}")
    public void i_have_username(String usernameRaw) {
        Object username = parseValue(usernameRaw, "username");
        if (username != null) credentials.put("username", username);
    }

    @Given("I have password {word}")
    public void i_have_password(String passwordRaw) {
        Object password = parseValue(passwordRaw, "password");
        if (password != null) credentials.put("password", password);
    }

    @Given("the request body contains a {int}KB string for the {word} field")
    public void the_request_body_contains_a_kb_string_for_the_field(int sizeKB, String fieldName) {
        int length = sizeKB * 1024; // 1KB = 1024 bytes/chars
        String enormousString = generateLargeString(length);

        String usernameValue = fieldName.equalsIgnoreCase("username") ? enormousString : ConfigReader.get("username");
        String passwordValue = fieldName.equalsIgnoreCase("password") ? enormousString : ConfigReader.get("password");

        // Create the raw JSON body
        String largeJsonBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                usernameValue, passwordValue);

        this.rawBody = largeJsonBody;
        scenario.log("Set Raw Payload (Volume Test): " + sizeKB + "KB string for " + fieldName + ". Size: " + largeJsonBody.length() + " bytes.");
    }

    @Given("the request body is set to:")
    public void the_request_body_is_set_to(String docString) {
        this.rawBody = docString;
        scenario.log("Set Raw Payload:\n" + rawBody);
    }

    @Given("the request Content-Type is set to {string}")
    public void the_request_content_type_is_set_to(String mimeType) {
        this.customContentType = mimeType;
        scenario.log("Set custom Content-Type: " + mimeType);
    }

    private Object parseValue(String raw, String field) {
        // Handle special placeholders
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
            // Fallback
        }

        return raw;
    }

    /**
     * Executes the request, extracts the response, and logs details to the report.
     */
    private void executeAndLogResponse(RequestSpecification requestSpec, String httpMethod, String endpoint) {
        String url = ConfigReader.get("base.url") + endpoint;

        scenario.log("Request URL: " + url);
        scenario.log("Request Method: " + httpMethod);

        response = requestSpec
                .when()
                .request(httpMethod, url)
                .then()
                .extract()
                .response();

        scenario.log("Response Status: " + response.getStatusCode());

        // Use prettyPrint for JSON content, otherwise use asString
        if (response.getContentType() != null && response.getContentType().contains("json")) {
            scenario.log("Response Body:\n" + response.prettyPrint());
        } else {
            scenario.log("Response Body:\n" + response.body().asString());
        }
    }

    @When("I send {word} to auth")
    public void i_send_http_method_to_auth(String method) {
        String httpMethod = method.toUpperCase();
        RequestSpecification requestSpec = given();

        // 1. Determine Content-Type
        String finalContentType = customContentType != null ? customContentType : "application/json";

        // 2. Attach Body/Params if credentials exist
        if (!credentials.isEmpty()) {

            if ("application/x-www-form-urlencoded".equalsIgnoreCase(finalContentType)) {
                // CASE 1: Form URL Encoded
                scenario.log("Content-Type: " + finalContentType + " (Form Data)");
                scenario.log("Request Parameters (Form Data):\n" + credentials);

                requestSpec
                        .contentType(finalContentType)
                        .formParams(credentials);
            } else {
                // CASE 2: JSON (or any other format with a body)
                // Manually serialize JSON and pass as byte array to bypass RestAssured's encoder on unsupported Content-Types.
                String jsonPayload;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    jsonPayload = mapper.writeValueAsString(credentials);

                    scenario.log("Content-Type: " + finalContentType + (customContentType != null ? " (Custom)" : " (Default JSON)"));
                    scenario.log("Request Payload (JSON):\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials));
                } catch (Exception e) {
                    jsonPayload = "⚠️ Failed to serialize credentials: " + e.getMessage();
                    scenario.log(jsonPayload);
                    jsonPayload = "{}";
                }

                // Pass body as bytes to prevent RestAssured from trying to encode the String body using the unsupported Content-Type.
                requestSpec
                        .header("Content-Type", finalContentType)
                        .body(jsonPayload.getBytes());
            }
        } else if (customContentType != null) {
            // CASE 3: No body given, but custom header
            requestSpec.header("Content-Type", finalContentType);
            scenario.log("Content-Type: " + finalContentType + " (Custom, No Body)");
        }

        executeAndLogResponse(requestSpec, httpMethod, "/auth");
    }

    @When("I send POST to auth with the raw body")
    public void i_send_post_to_auth_with_the_raw_body() {
        assertNotNull(rawBody, "Raw body must be set before this step.");

        RequestSpecification requestSpec = given()
                .header("Content-Type", "application/json")
                .body(rawBody);

        scenario.log("Content-Type: application/json (Raw Body)");
        scenario.log("Request Payload (Raw): " + rawBody);

        executeAndLogResponse(requestSpec, "POST", "/auth");
    }


    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatus) {
        int actualStatus = response.statusCode();
        assertEquals(expectedStatus, actualStatus,
                String.format("Unexpected status code. Expected: %d, but found: %d", expectedStatus, actualStatus));
    }

    @Then("the response header {string} should contain {string}")
    public void the_response_header_should_contain(String headerName, String expectedValue) {
        String actualHeader = response.header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        assertTrue(actualHeader.contains(expectedValue),
                "Header '" + headerName + "' value did not contain '" + expectedValue + "'. Actual: " + actualHeader);
        scenario.log("Verified Header: " + headerName + " contains " + expectedValue);
    }

    @Then("the response header {string} should be present")
    public void the_response_header_should_be_present(String headerName) {
        String actualHeader = response.header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        scenario.log("Verified Header: " + headerName + " is present with value: " + actualHeader);
    }

    @Then("the response body should only contain keys: {string}")
    public void the_response_body_should_only_contain_keys(String expectedKeysList) {
        // Get the actual keys from the root of the JSON response
        Map<String, ?> actualMap = response.jsonPath().getMap("");
        Set<String> actualKeys = actualMap.keySet();

        // Prepare the expected keys set
        Set<String> expectedKeys = Set.of(expectedKeysList.split("\\s*,\\s*"));

        expectedKeys.forEach(key -> assertTrue(actualKeys.contains(key), "Missing expected key: " + key));

        // Check for unexpected extra keys
        Set<String> unexpectedKeys = actualKeys.stream()
                .filter(key -> !expectedKeys.contains(key))
                .collect(Collectors.toSet());

        assertTrue(unexpectedKeys.isEmpty(),
                "Unexpected extra key(s) found in response body: " + unexpectedKeys);

        scenario.log("Verified response body contains ONLY the expected keys: " + expectedKeys);
    }

    @Then("the response should contain a token")
    public void the_response_should_contain_a_token() {
        String token = response.jsonPath().getString("token");
        assertNotNull(token, "Expected a token in the response");
        assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Then("the produced token is a valid format string")
    public void the_produced_token_is_a_valid_format_string() {
        String token = response.jsonPath().getString("token");

        assertNotNull(token, "Token is missing or null.");
        assertFalse(token.trim().isEmpty(), "Token is empty.");

        Pattern alphanumericPattern = Pattern.compile("^[a-zA-Z0-9]+$");
        assertTrue(alphanumericPattern.matcher(token).matches(),
                "Token value is not a simple alphanumeric string. Actual: " + token);
    }

    @Then("I store the token as {string}")
    public void i_store_the_token_as(String key) {
        String token = response.jsonPath().getString("token");
        assertNotNull(token, "Cannot store a null token.");
        scenarioContext.put(key, token);
        scenario.log("Stored token: " + key + " = " + token);
    }

    @Then("the token {string} is different from {string}")
    public void the_token_is_different_from(String key1, String key2) {
        String token1 = scenarioContext.get(key1);
        String token2 = scenarioContext.get(key2);

        assertNotNull(token1, "Token '" + key1 + "' was not stored.");
        assertNotNull(token2, "Token '" + key2 + "' was not stored.");
        assertNotEquals(token1, token2, "The two generated tokens should be different.");

        scenario.log("Token 1: " + token1);
        scenario.log("Token 2: " + token2);
        scenario.log("Successfully validated that token1 != token2.");
    }

    @Then("the response should contain reason {string}")
    public void the_response_should_contain_reason(String expectedReason) {
        String reason = response.jsonPath().getString("reason");
        assertEquals(expectedReason, reason, "Unexpected error reason");
    }

    @Then("the response body should be plain text {string}")
    public void the_response_body_should_be_plain_text(String expectedBody) {
        String contentType = response.contentType();
        assertTrue(contentType.toLowerCase().startsWith("text/plain"),
                "Expected Content-Type 'text/plain', but found: " + contentType);

        String actualBody = response.body().asString().trim();
        assertEquals(expectedBody, actualBody, "Response body content mismatch.");
    }
}