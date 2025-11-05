package steps;

import static org.junit.jupiter.api.Assertions.*;

import context.ScenarioContext;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.HashMap;
import java.util.Map;
import utils.TestUtils;

public class BookingSteps extends BaseSteps {

  public BookingSteps(ScenarioContext context) {
    this.ctx = context;
  }

  @Before
  public void setScenario(Scenario scenario) {
    this.scenario = scenario;
  }

  @Given("I have a valid booking payload")
  public void i_have_a_valid_booking_payload() {
    Map<String, Object> bookingPayload = TestUtils.createValidBookingPayload();
    ctx.setPayload(bookingPayload);
  }

  @Given("I have a booking payload missing {word}")
  public void i_have_a_booking_payload_missing_firstname(String missingField) {
    Map<String, Object> bookingPayload = TestUtils.createBookingPayloadMissingField(missingField);
    ctx.setPayload(bookingPayload);
  }

  @Given("I have a booking payload with invalid date")
  public void i_have_a_booking_payload_with_invalid_date() {
    Map<String, Object> bookingPayload = TestUtils.createBookingPayloadWithInvalidDate();
    ctx.setPayload(bookingPayload);
  }

  @Given("I have a booking payload with checkin {string} and checkout {string}")
  public void i_have_a_booking_payload_with_checkin_and_checkout(String checkin, String checkout) {
    var bookingDates = new HashMap<String, Object>();
    bookingDates.put("checkin", TestUtils.parseValue(checkin, ""));
    bookingDates.put("checkout", TestUtils.parseValue(checkout, ""));
    ctx.setPayload(
        "payload", TestUtils.createValidBookingPayload(Map.of("bookingdates", bookingDates)));
  }

  @Then("the response should contain a booking id")
  public void the_response_should_contain_a_booking_id() {
    var response = ctx.getResponse();
    assertNotNull(response.jsonPath().get("bookingid"));
  }

  @Then("the JSON booking details should match the request")
  public void the_json_booking_details_should_match_the_request() {
    Map<String, Object> requestPayload = ctx.getPayload();
    var response = ctx.getResponse();

    var booking = response.jsonPath().getMap("booking");

    assertEquals(requestPayload.get("firstname"), booking.get("firstname"));
    assertEquals(requestPayload.get("lastname"), booking.get("lastname"));
    assertEquals(requestPayload.get("totalprice"), booking.get("totalprice"));
  }

  @Then("the XML booking details should match the request")
  public void the_booking_details_should_match_the_request() {
    Map<String, Object> requestPayload = ctx.getPayload();
    var response = ctx.getResponse();

    String contentType = response.getContentType();
    String body = response.asString().trim();

    Map<String, Object> booking;

    io.restassured.path.xml.XmlPath xml = new io.restassured.path.xml.XmlPath(body);
    // Manually read individual fields from XML
    booking =
        Map.of(
            "firstname", xml.getString("created-booking.booking.firstname"),
            "lastname", xml.getString("created-booking.booking.lastname"),
            "totalprice", parseIntSafe(xml.getString("created-booking.booking.totalprice")),
            "depositpaid", parseBooleanSafe(xml.getString("created-booking.booking.depositpaid")),
            "checkin", xml.getString("created-booking.booking.bookingdates.checkin"),
            "checkout", xml.getString("created-booking.booking.bookingdates.checkout"),
            "additionalneeds", xml.getString("created-booking.booking.additionalneeds"));
    ;

    assertEquals(requestPayload.get("firstname"), booking.get("firstname"));
    assertEquals(requestPayload.get("lastname"), booking.get("lastname"));
    assertEquals(requestPayload.get("totalprice"), booking.get("totalprice"));
  }

  @Then("I store the booking id as {string}")
  public void i_store_the_id_as(String key) {
    String token = ctx.getResponseString("bookingid");
    assertNotNull(token, "Cannot store a null bookingid.");
    ctx.getData().put(key, token);
    log("Stored bookingid: " + key + " = " + token);
  }

  @Then("the id {string} is different from {string}")
  public void the_id_is_different_from(String key1, String key2) {
    String token1 = ctx.getData().get(key1);
    String token2 = ctx.getData().get(key2);

    assertNotNull(token1, "Token '" + key1 + "' was not stored.");
    assertNotNull(token2, "Token '" + key2 + "' was not stored.");
    assertNotEquals(token1, token2, "The two generated tokens should be different.");

    log("Token 1: " + token1);
    log("Token 2: " + token2);
    log("Successfully validated that token1 != token2.");
  }

  private Object parseIntSafe(String value) {
    try {
      return Integer.parseInt(value);
    } catch (Exception e) {
      return value;
    }
  }

  private Object parseBooleanSafe(String value) {
    if ("true".equalsIgnoreCase(value)) return true;
    if ("false".equalsIgnoreCase(value)) return false;
    return value;
  }
}
