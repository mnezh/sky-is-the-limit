@booking @stable @resilience
Feature: Authentication endpoint is resilient

  Scenario Outline: Payload Too Large for enormous <Field>
    Given I have a valid booking payload
    And the request body contains a 500KB string for the <Field> field
    When I POST payload to "/booking"
    Then the response status code should be 413
    And the response body should be plain text "Payload Too Large"

    Examples:
      | Field           |
      | firstname       |
      | lastname        |
      | totalprice      |
      | depositpaid     |
      | bookingdates    |
      | additionalneeds |

  Scenario Outline: Booking creation fails: Malicious or boundary strings (<Description>)
    Given the request body is set to:
    """
    {
      "firstname": "<firstname>",
      "lastname": "<lastname>",
      "totalprice": 123,
      "depositpaid": true,
      "bookingdates": {
        "checkin": "2023-01-01",
        "checkout": "2023-01-02"
      },
      "additionalneeds": "Breakfast"
    }
    """
    And the request Content-Type is set to "application/json"
    When I POST raw to "/booking"
    Then the response status code should be 400
    And the response body should be plain text "Bad Request"

    Examples:
      | Description                          | firstname          | lastname                    |
      | SQL Injection Attempt (firstname)    | "' OR 1=1 --"      | "Smith"                     |
      | XSS Payload (lastname)               | "John"             | "<script>alert(1)</script>" |
      | Directory Traversal (firstname)      | "../../etc/passwd" | "Doe"                       |
      | JSON Escape Sequence Bomb (lastname) | "John"             | "\u0000" * 100              |
      | Max Boundary String (firstname)      | "1234567890" * 25  | "Doe"                       |
