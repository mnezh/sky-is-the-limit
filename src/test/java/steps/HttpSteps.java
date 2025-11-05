package steps;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import context.ScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import utils.ConfigReader;
import utils.RequestLoggingFilter;
import utils.TestUtils;

public class HttpSteps extends BaseSteps {
  private final XmlMapper xmlMapper = new XmlMapper();

  public HttpSteps(ScenarioContext context) {
    this.ctx = context;
  }

  @Before
  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  private void executeAndLogResponse(
      RequestSpecification requestSpec, String method, String endpoint) {
    ctx.setResponse(
        requestSpec
            .when()
            .filter(new RequestLoggingFilter(scenario))
            .request(method.toUpperCase(), ConfigReader.get("base.url") + endpoint)
            .then()
            .extract()
            .response());
  }

  @Given("the request body contains a {int}KB string for the {word} field")
  public void the_request_body_contains_a_kb_string_for_the_field(int sizeKB, String fieldName) {
    var data = TestUtils.generateLargeString(sizeKB * 1024);
    log("Setting " + fieldName + " to " + data);
    ctx.setPayload(fieldName, data);
  }

  @Given("I override field {word} with {string}")
  public void i_override_field_with(String fieldName, String value) {
    ctx.setPayload(fieldName, value);
  }

  @Given("the request body is set to:")
  public void the_request_body_is_set_to(String docString) {
    ctx.setRawBody(docString);
  }

  @Given("the request Content-Type is set to {string}")
  public void the_request_content_type_is_set_to(String mimeType) {
    ctx.setContentType(mimeType);
  }

  @Given("the request header {string} is set to {string}")
  public void the_request_header_is_set(String headerName, String headerValue) {
    ctx.setHeader(headerName, headerValue);
  }

  @When("I {word} payload to {string}")
  public void i_send_payload_with_method_to_endpoint(String method, String endpoint)
      throws JsonProcessingException {
    var requestSpec =
        given()
            .config(
                RestAssuredConfig.config()
                    .encoderConfig(
                        EncoderConfig.encoderConfig()
                            .appendDefaultContentCharsetToContentTypeIfUndefined(false)));

    var fullContentType = ctx.getContentType("application/json");
    var mediaType = fullContentType.split(";")[0].trim();
    var headers = ctx.getHeaders();
    for (var header : headers.entrySet()) {
      requestSpec.header(header.getKey(), header.getValue());
    }
    requestSpec.header("Content-Type", fullContentType);

    var payload = ctx.getPayload();

    switch (mediaType.toLowerCase()) {
      case "application/x-www-form-urlencoded" -> requestSpec.formParams(payload);
      case "text/xml", "application/xml" -> {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(
            com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION,
            true);
        xmlMapper.configure(
            com.fasterxml.jackson.databind.SerializationFeature.WRAP_ROOT_VALUE, true);

        var xmlPayload =
            xmlMapper
                .writerWithDefaultPrettyPrinter()
                .withRootName("booking")
                .writeValueAsString(payload);
        requestSpec.body(xmlPayload);
      }
      default -> {
        try {
          String jsonPayload = new ObjectMapper().writeValueAsString(payload);
          requestSpec.body(jsonPayload.getBytes());
        } catch (JsonProcessingException e) {
          log("⚠️ Failed to serialize payload: " + e.getMessage());
          throw e;
        }
      }
    }

    executeAndLogResponse(requestSpec, method, endpoint);
  }

  @When("I {word} raw to {string}")
  public void i_send_raw_body_with_method_to_endpoint(String method, String endpoint) {
    String contentType = ctx.getContentType("application/json");
    RequestSpecification requestSpec = given().header("Content-Type", contentType);
    String payload = ctx.getRawBody();
    if (payload != null) requestSpec.body(payload.getBytes());
    log("Content-Type: " + contentType + " payload: " + payload);
    executeAndLogResponse(requestSpec, method, endpoint);
  }

  @Then("the response body should be plain text {string}")
  public void the_response_body_should_be_plain_text(String expectedBody) {
    String contentType = ctx.getResponse().contentType();
    assertTrue(
        contentType.toLowerCase().startsWith("text/plain"),
        "Expected Content-Type 'text/plain', but found: " + contentType);

    String actualBody = ctx.getResponse().body().asString().trim();
    assertEquals(expectedBody, actualBody, "Response body content mismatch.");
  }

  @Then("the response body should only contain keys: {string}")
  public void the_response_body_should_only_contain_keys(String expectedKeysList) {
    Map<String, ?> actualMap = ctx.getJSONResponse().getMap("");
    Set<String> actualKeys = actualMap.keySet();
    Set<String> expectedKeys = Set.of(expectedKeysList.split("\\s*,\\s*"));

    expectedKeys.forEach(
        key -> assertTrue(actualKeys.contains(key), "Missing expected key: " + key));

    Set<String> unexpectedKeys =
        actualKeys.stream().filter(key -> !expectedKeys.contains(key)).collect(Collectors.toSet());

    assertTrue(
        unexpectedKeys.isEmpty(),
        "Unexpected extra key(s) found in response body: " + unexpectedKeys);

    log("Verified response body contains ONLY the expected keys: " + expectedKeys);
  }

  @Then("the response header {string} should be present")
  public void the_response_header_should_be_present(String headerName) {
    String actualHeader = ctx.getResponse().header(headerName);
    assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
    log("Verified Header: " + headerName + " is present with value: " + actualHeader);
  }

  @Then("the response header {string} should contain {string}")
  public void the_response_header_should_contain(String headerName, String expectedValue) {
    String actualHeader = ctx.getResponse().header(headerName);
    assertNotNull(actualHeader, "Header '" + headerName + "' was not found in the response.");
    assertTrue(
        actualHeader.contains(expectedValue),
        "Header '"
            + headerName
            + "' value did not contain '"
            + expectedValue
            + "'. Actual: "
            + actualHeader);
    log("Verified Header: " + headerName + " contains " + expectedValue);
  }

  @Then("the response header {string} should not be present")
  public void the_response_header_should_not_be_present(String headerName) {
    String actualHeader = ctx.getResponse().header(headerName);
    assertNull(
        actualHeader,
        String.format(
            "Header '%s' was expected to be missing (null), but was found with value: %s",
            headerName, actualHeader));
    log(String.format("Verified Header: %s is correctly missing (null).", headerName));
  }
}
