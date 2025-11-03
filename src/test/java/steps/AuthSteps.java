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
import utils.TestUtils;

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

    // --- SETUP AND EXECUTION ---

    @Before
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
        this.rawBody = null;
        this.customContentType = null;
        this.credentials.clear();
        this.scenarioContext.clear();
    }

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

        // Reverting to plain logging
        if (response.getContentType() != null && response.getContentType().toLowerCase().contains("json")) {
            scenario.log("Response Body:\n" + response.prettyPrint());
        } else {
            scenario.log("Response Body:\n" + response.body().asString());
        }
    }

    // --- GIVEN METHODS (Alphabetical) ---

    @Given("I have password {word}")
    public void i_have_password(String passwordRaw) {
        Object password = TestUtils.parseValue(passwordRaw, "password");
        if (password != null) credentials.put("password", password);
    }

    @Given("I have username {word}")
    public void i_have_username(String usernameRaw) {
        Object username = TestUtils.parseValue(usernameRaw, "username");
        if (username != null) credentials.put("username", username);
    }

    @Given("the request body contains a {int}KB string for the {word} field")
    public void the_request_body_contains_a_kb_string_for_the_field(int sizeKB, String fieldName) {
        int length = sizeKB * 1024;
        String enormousString = TestUtils.generateLargeString(length);

        String usernameValue = fieldName.equalsIgnoreCase("username") ? ConfigReader.get("username") : enormousString;
        String passwordValue = fieldName.equalsIgnoreCase("password") ? ConfigReader.get("password") : enormousString;

        String largeJsonBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}",
                usernameValue, passwordValue);

        this.rawBody = largeJsonBody;
        scenario.log("Set Raw Payload (Volume Test): " + sizeKB + "KB string for " + fieldName);
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

    // --- WHEN METHODS (Alphabetical) ---

    @When("I send {word} to {string}")
    public void i_send_http_method_to_auth(String method, String endpoint) {
        String httpMethod = method.toUpperCase();
        RequestSpecification requestSpec = given();

        String finalContentType = customContentType != null ? customContentType : "application/json";

        if (!credentials.isEmpty()) {
            if ("application/x-www-form-urlencoded".equalsIgnoreCase(finalContentType)) {
                // Log Form Data
                scenario.log("Content-Type: " + finalContentType + " (Form Data)");
                scenario.log("Request Parameters (Form Data):\n" + credentials);
                requestSpec.contentType(finalContentType).formParams(credentials);

            } else {
                // Log JSON Payload
                String jsonPayload;
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    jsonPayload = mapper.writeValueAsString(credentials);
                    requestSpec.header("Content-Type", finalContentType).body(jsonPayload.getBytes());

                    scenario.log("Content-Type: " + finalContentType);
                    scenario.log("Request Payload (JSON):\n" + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials));

                } catch (Exception e) {
                    scenario.log("⚠️ Failed to serialize credentials: " + e.getMessage());
                    requestSpec.header("Content-Type", finalContentType).body("{}");
                }
            }
        } else if (customContentType != null) {
            requestSpec.header("Content-Type", finalContentType);
            scenario.log("Content-Type: " + finalContentType + " (Custom, No Body)");
        }

        executeAndLogResponse(requestSpec, httpMethod, endpoint);
    }

    @When("I send POST to {string} with the raw body")
    public void i_send_post_to_auth_with_the_raw_body(String endpoint) {
        assertNotNull(rawBody, "Raw body must be set before this step.");

        RequestSpecification requestSpec = given()
                .header("Content-Type", "application/json")
                .body(rawBody);

        // Log Raw Body
        scenario.log("Content-Type: application/json (Raw Body)");
        scenario.log("Request Payload (Raw):\n" + rawBody);

        executeAndLogResponse(requestSpec, "POST", endpoint);
    }

    // --- THEN METHODS (Alphabetical) ---

    @Then("I store the token as {string}")
    public void i_store_the_token_as(String key) {
        String token = response.jsonPath().getString("token");
        assertNotNull(token, "Cannot store a null token.");
        scenarioContext.put(key, token);
        scenario.log("Stored token: " + key + " = " + token);
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

    @Then("the response body should be plain text {string}")
    public void the_response_body_should_be_plain_text(String expectedBody) {
        String contentType = response.contentType();
        assertTrue(contentType.toLowerCase().startsWith("text/plain"),
                "Expected Content-Type 'text/plain', but found: " + contentType);

        String actualBody = response.body().asString().trim();
        assertEquals(expectedBody, actualBody, "Response body content mismatch.");
    }

    @Then("the response body should only contain keys: {string}")
    public void the_response_body_should_only_contain_keys(String expectedKeysList) {
        Map<String, ?> actualMap = response.jsonPath().getMap("");
        Set<String> actualKeys = actualMap.keySet();
        Set<String> expectedKeys = Set.of(expectedKeysList.split("\\s*,\\s*"));

        expectedKeys.forEach(key -> assertTrue(actualKeys.contains(key), "Missing expected key: " + key));

        Set<String> unexpectedKeys = actualKeys.stream()
                .filter(key -> !expectedKeys.contains(key))
                .collect(Collectors.toSet());

        assertTrue(unexpectedKeys.isEmpty(),
                "Unexpected extra key(s) found in response body: " + unexpectedKeys);

        scenario.log("Verified response body contains ONLY the expected keys: " + expectedKeys);
    }

    @Then("the response header {string} should be present")
    public void the_response_header_should_be_present(String headerName) {
        String actualHeader = response.header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        scenario.log("Verified Header: " + headerName + " is present with value: " + actualHeader);
    }

    @Then("the response header {string} should contain {string}")
    public void the_response_header_should_contain(String headerName, String expectedValue) {
        String actualHeader = response.header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        assertTrue(actualHeader.contains(expectedValue),
                "Header '" + headerName + "' value did not contain '" + expectedValue + "'. Actual: " + actualHeader);
        scenario.log("Verified Header: " + headerName + " contains " + expectedValue);
    }

    @Then("the response header {string} should not be present")
    public void the_response_header_should_not_be_present(String headerName) {
        String actualHeader = response.header(headerName);
        assertNull(actualHeader,
                String.format("Header '%s' was expected to be missing (null), but was found with value: %s", headerName, actualHeader));
        scenario.log(String.format("Verified Header: %s is correctly missing (null).", headerName));
    }

    @Then("the response should contain a token")
    public void the_response_should_contain_a_token() {
        String token = response.jsonPath().getString("token");
        assertNotNull(token, "Expected a token in the response");
        assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Then("the response should contain reason {string}")
    public void the_response_should_contain_reason(String expectedReason) {
        String reason = response.jsonPath().getString("reason");
        assertEquals(expectedReason, reason, "Unexpected error reason");
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatus) {
        int actualStatus = response.statusCode();
        assertEquals(expectedStatus, actualStatus,
                String.format("Unexpected status code. Expected: %d, but found: %d", expectedStatus, actualStatus));
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
}
