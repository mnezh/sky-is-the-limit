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

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

public class AuthSteps {
    private final Map<String, Object> credentials = new HashMap<>();
    private Response response;
    private Scenario scenario;

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

    @Then("the response should contain reason {string}")
    public void the_response_should_contain_reason(String expectedReason) {
        String reason = response.jsonPath().getString("reason");
        assertEquals(expectedReason, reason, "Unexpected error reason");
    }
}
