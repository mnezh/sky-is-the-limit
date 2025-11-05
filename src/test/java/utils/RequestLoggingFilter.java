package utils;

import io.cucumber.java.Scenario;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class RequestLoggingFilter implements Filter {

  private final Scenario scenario;

  public RequestLoggingFilter(Scenario scenario) {
    this.scenario = scenario;
  }

  @Override
  public Response filter(
      FilterableRequestSpecification requestSpec,
      FilterableResponseSpecification responseSpec,
      FilterContext context) {

    StringBuilder sb = new StringBuilder();
    sb.append("➡️ [REQUEST]\n")
        .append(requestSpec.getMethod())
        .append(" ")
        .append(requestSpec.getURI())
        .append("\n");

    sb.append("Headers:\n");
    requestSpec
        .getHeaders()
        .forEach(
            h ->
                sb.append("  ").append(h.getName()).append(": ").append(h.getValue()).append("\n"));

    if (requestSpec.getBody() != null) {
      Object body = requestSpec.getBody();
      String bodyString;

      if (body instanceof byte[]) {
        bodyString = new String((byte[]) body, java.nio.charset.StandardCharsets.UTF_8);
      } else {
        bodyString = String.valueOf(body);
      }

      sb.append("Body:\n").append(TestUtils.escapeXml(bodyString));
    }
    scenario.log(sb.toString());
    sb.setLength(0);

    // Send actual request
    Response response = context.next(requestSpec, responseSpec);

    sb.append("⬅️ [RESPONSE]\n")
        .append("Status: ")
        .append(response.statusCode())
        .append("\n")
        .append("Headers:\n");
    response
        .getHeaders()
        .forEach(
            h ->
                sb.append("  ").append(h.getName()).append(": ").append(h.getValue()).append("\n"));

    if (response.getBody() != null) {
      var body = TestUtils.escapeXml(response.asString());
      sb.append("Body:\n").append(body);
    }

    scenario.log(sb.toString());

    return response;
  }
}
