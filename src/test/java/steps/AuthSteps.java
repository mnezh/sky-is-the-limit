package steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.specification.RequestSpecification;
import utils.ConfigReader;
import context.ScenarioContext;
import utils.TestUtils;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AuthSteps {
    private Scenario scenario;
    private final ScenarioContext context;

    public AuthSteps(ScenarioContext context) {
        this.context = context;
    }

    @Before
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    private void executeAndLogResponse(RequestSpecification requestSpec, String method, String endpoint) {
        var httpMethod = method.toUpperCase();
        var url = ConfigReader.get("base.url") + endpoint;

        scenario.log("Request URL: " + url);
        scenario.log("Request Method: " + httpMethod);

        var response = requestSpec
                .when()
                .request(httpMethod, url)
                .then()
                .extract()
                .response();

        context.setResponse(response);

        scenario.log("Response Status: " + response.getStatusCode());

        if (response.getContentType() != null && response.getContentType().toLowerCase().contains("json")) {
            scenario.log("Response Body:\n" + response.prettyPrint());
        } else {
            scenario.log("Response Body:\n" + response.body().asString());
        }
    }

    @Given("I have username {word} and password {word}")
    public void i_set_username_and_password(String usernameRaw, String passwordRaw) {
        context.setPayload("username", TestUtils.parseValue(usernameRaw, "username"));
        context.setPayload("password", TestUtils.parseValue(passwordRaw, "password"));
    }

    @Given("the request body contains a {int}KB string for the {word} field")
    public void the_request_body_contains_a_kb_string_for_the_field(int sizeKB, String fieldName) {
        var data = TestUtils.generateLargeString(sizeKB * 1024);
        scenario.log("Setting " + fieldName + " to " + data);
        context.setPayload(fieldName, data);
    }

    @Given("the request body is set to:")
    public void the_request_body_is_set_to(String docString) {
        context.setRawBody(docString);
    }

    @Given("the request Content-Type is set to {string}")
    public void the_request_content_type_is_set_to(String mimeType) {
        context.setContentType(mimeType);
    }

    @When("I {word} payload to {string}")
    public void i_send_payload_with_method_to_endpoint(String method, String endpoint) throws JsonProcessingException {
        RequestSpecification requestSpec = given();
        String contentType = context.getContentType("application/json");
        if ("application/x-www-form-urlencoded".equalsIgnoreCase(contentType)) {
            var payload = context.getPayload();
            requestSpec.contentType(contentType).formParams(payload);
            scenario.log("Content-Type: " + contentType + " form-data: " + payload);
        } else {
            try {
                String payload = new ObjectMapper().writeValueAsString(context.getPayload());
                requestSpec.header("Content-Type", contentType).body(payload.getBytes());
                scenario.log("Content-Type: " + contentType + " payload: " + payload);
            } catch (JsonProcessingException e) {
                scenario.log("⚠️ Failed to serialize payload: " + e.getMessage());
                throw e;
            }
        }
        executeAndLogResponse(requestSpec, method, endpoint);
    }

    @When("I {word} raw to {string}")
    public void i_send_raw_body_with_method_to_endpoint(String method, String endpoint) {
        String contentType = context.getContentType("application/json");
        RequestSpecification requestSpec = given().header("Content-Type", contentType);
        String payload = context.getRawBody();
        if (payload != null) requestSpec.body(payload.getBytes());
        scenario.log("Content-Type: " + contentType + " payload: " + payload);
        executeAndLogResponse(requestSpec, method, endpoint);
    }

    @Then("I store the token as {string}")
    public void i_store_the_token_as(String key) {
        String token = context.getResponseString("token");
        assertNotNull(token, "Cannot store a null token.");
        context.getData().put(key, token);
        scenario.log("Stored token: " + key + " = " + token);
    }

    @Then("the produced token is a valid format string")
    public void the_produced_token_is_a_valid_format_string() {
        String token = context.getResponseString("token");
        assertNotNull(token, "Token is missing or null.");
        assertFalse(token.trim().isEmpty(), "Token is empty.");

        Pattern alphanumericPattern = Pattern.compile("^[a-zA-Z0-9]+$");
        assertTrue(alphanumericPattern.matcher(token).matches(),
                "Token value is not a simple alphanumeric string. Actual: " + token);
    }

    @Then("the response body should be plain text {string}")
    public void the_response_body_should_be_plain_text(String expectedBody) {
        String contentType = context.getResponse().contentType();
        assertTrue(contentType.toLowerCase().startsWith("text/plain"),
                "Expected Content-Type 'text/plain', but found: " + contentType);

        String actualBody = context.getResponse().body().asString().trim();
        assertEquals(expectedBody, actualBody, "Response body content mismatch.");
    }

    @Then("the response body should only contain keys: {string}")
    public void the_response_body_should_only_contain_keys(String expectedKeysList) {
        Map<String, ?> actualMap = context.getJSONResponse().getMap("");
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
        String actualHeader = context.getResponse().header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        scenario.log("Verified Header: " + headerName + " is present with value: " + actualHeader);
    }

    @Then("the response header {string} should contain {string}")
    public void the_response_header_should_contain(String headerName, String expectedValue) {
        String actualHeader = context.getResponse().header(headerName);
        assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
        assertTrue(actualHeader.contains(expectedValue),
                "Header '" + headerName + "' value did not contain '" + expectedValue + "'. Actual: " + actualHeader);
        scenario.log("Verified Header: " + headerName + " contains " + expectedValue);
    }

    @Then("the response header {string} should not be present")
    public void the_response_header_should_not_be_present(String headerName) {
        String actualHeader = context.getResponse().header(headerName);
        assertNull(actualHeader,
                String.format("Header '%s' was expected to be missing (null), but was found with value: %s", headerName, actualHeader));
        scenario.log(String.format("Verified Header: %s is correctly missing (null).", headerName));
    }

    @Then("the response should contain a token")
    public void the_response_should_contain_a_token() {
        String token = context.getResponseString("token");
        assertNotNull(token, "Expected a token in the response");
        assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Then("the response should contain reason {string}")
    public void the_response_should_contain_reason(String expectedReason) {
        String reason = context.getResponseString("reason");
        assertEquals(expectedReason, reason, "Unexpected error reason");
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatus) {
        int actualStatus = context.getResponse().statusCode();
        assertEquals(expectedStatus, actualStatus,
                String.format("Unexpected status code. Expected: %d, but found: %d", expectedStatus, actualStatus));
    }

    @Then("the token {string} is different from {string}")
    public void the_token_is_different_from(String key1, String key2) {
        String token1 = context.getData().get(key1);
        String token2 = context.getData().get(key2);

        assertNotNull(token1, "Token '" + key1 + "' was not stored.");
        assertNotNull(token2, "Token '" + key2 + "' was not stored.");
        assertNotEquals(token1, token2, "The two generated tokens should be different.");

        scenario.log("Token 1: " + token1);
        scenario.log("Token 2: " + token2);
        scenario.log("Successfully validated that token1 != token2.");
    }
}
