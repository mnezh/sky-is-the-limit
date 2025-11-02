package steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import utils.ConfigReader;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AuthSteps {
    private final Map<String, Object> credentials = new HashMap<>();
    private final Map<String, String> scenarioContext = new HashMap<>();
    private Response response;
    private Scenario scenario;
    private String rawBody;

    @Before
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
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

    @Given("the request body is set to:")
    public void the_request_body_is_set_to(String docString) {
        this.rawBody = docString;
        scenario.log("Set Raw Payload:\n" + rawBody);
    }

    private Object parseValue(String raw, String field) {
        // Handle special placeholders
        if ("<missing>".equalsIgnoreCase(raw)) return null;
        if ("<valid>".equalsIgnoreCase(raw))
            return ConfigReader.get(field);

        // Strip quotes if present
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
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
            // not numeric, fall through
        }

        // Fallback
        return raw;
    }

    @When("I send POST to auth")
    public void i_send_post_to_auth() {
        String url = ConfigReader.get("base.url") + "/auth";
        String jsonPayload;

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonPayload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials);
        } catch (Exception e) {
            jsonPayload = "⚠️ Failed to serialize credentials: " + e.getMessage();
        }

        // Attach to report (visible in HTML output per step)
        scenario.log("Request URL: " + url);
        scenario.log("Request Payload:\n" + jsonPayload);

        response = given()
                .header("Content-Type", "application/json")
                .body(credentials)
                .post(url)
                .then()
                .extract()
                .response();

        // Attach response to report
        scenario.log("Response Status: " + response.getStatusCode());
        scenario.log("Response Body:\n" + response.prettyPrint());
    }

    @When("I send POST to auth with the raw body")
    public void i_send_post_to_auth_with_the_raw_body() {
        String url = ConfigReader.get("base.url") + "/auth";

        scenario.log("Request URL: " + url);
        scenario.log("Request Payload (Raw): " + rawBody);

        response = given()
                .header("Content-Type", "application/json")
                .body(rawBody) // Use the raw, malformed string
                .post(url)
                .then()
                .extract()
                .response();

        // Attach response to report
        scenario.log("Response Status: " + response.getStatusCode());
        scenario.log("Response Body:\n" + response.body().asString()); // Use asString for non-JSON content
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatus) {
        assertEquals(expectedStatus, response.statusCode(), "Unexpected status code");
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
    }

    @Then("the response should contain reason {string}")
    public void the_response_should_contain_reason(String expectedReason) {
        String reason = response.jsonPath().getString("reason");
        assertEquals(expectedReason, reason, "Unexpected error reason");
    }

    @Then("the response body should be plain text {string}")
    public void the_response_body_should_be_plain_text(String expectedBody) {
        // 1. Check Content-Type header starts with text/plain
        String contentType = response.contentType();
        assertTrue(contentType.toLowerCase().startsWith("text/plain"),
                "Expected Content-Type 'text/plain', but found: " + contentType);

        // 2. Check body content, trimmed for whitespace consistency
        String actualBody = response.body().asString().trim();
        assertEquals(expectedBody, actualBody, "Response body content mismatch.");
    }
}
