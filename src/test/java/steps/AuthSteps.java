package steps;

import static org.junit.jupiter.api.Assertions.*;

import context.ScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.regex.Pattern;
import utils.TestUtils;

public class AuthSteps extends BaseSteps {

  public AuthSteps(ScenarioContext context) {
    this.ctx = context;
  }

  @Before
  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  @Given("I have username {word} and password {word}")
  public void i_set_username_and_password(String usernameRaw, String passwordRaw) {
    ctx.setPayload("username", TestUtils.parseValue(usernameRaw, "username"));
    ctx.setPayload("password", TestUtils.parseValue(passwordRaw, "password"));
  }

  @Then("I store the token as {string}")
  public void i_store_the_token_as(String key) {
    String token = ctx.getResponseString("token");
    assertNotNull(token, "Cannot store a null token.");
    ctx.getData().put(key, token);
    log("Stored token: " + key + " = " + token);
  }

  @Then("the produced token is a valid format string")
  public void the_produced_token_is_a_valid_format_string() {
    String token = ctx.getResponseString("token");
    assertNotNull(token, "Token is missing or null.");
    assertFalse(token.trim().isEmpty(), "Token is empty.");

    Pattern alphanumericPattern = Pattern.compile("^[a-zA-Z0-9]+$");
    assertTrue(
        alphanumericPattern.matcher(token).matches(),
        "Token value is not a simple alphanumeric string. Actual: " + token);
  }

  @Then("the response should contain a token")
  public void the_response_should_contain_a_token() {
    String token = ctx.getResponseString("token");
    assertNotNull(token, "Expected a token in the response");
    assertFalse(token.isEmpty(), "Token should not be empty");
  }

  @Then("the response should contain reason {string}")
  public void the_response_should_contain_reason(String expectedReason) {
    String reason = ctx.getResponseString("reason");
    assertEquals(expectedReason, reason, "Unexpected error reason");
  }

  @Then("the response status code should be {int}")
  public void the_response_status_code_should_be(int expectedStatus) {
    int actualStatus = ctx.getResponse().statusCode();
    assertEquals(
        expectedStatus,
        actualStatus,
        String.format(
            "Unexpected status code. Expected: %d, but found: %d", expectedStatus, actualStatus));
  }

  @Then("the token {string} is different from {string}")
  public void the_token_is_different_from(String key1, String key2) {
    String token1 = ctx.getData().get(key1);
    String token2 = ctx.getData().get(key2);

    assertNotNull(token1, "Token '" + key1 + "' was not stored.");
    assertNotNull(token2, "Token '" + key2 + "' was not stored.");
    assertNotEquals(token1, token2, "The two generated tokens should be different.");

    log("Token 1: " + token1);
    log("Token 2: " + token2);
    log("Successfully validated that token1 != token2.");
  }
}
