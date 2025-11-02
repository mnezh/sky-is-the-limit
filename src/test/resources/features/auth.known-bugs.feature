@bug
Feature: Known Bugs on Authentication Endpoint

  # BUG: The API processes requests with unsupported Content-Types and returns a 200 OK,
  # instead of the expected 415 Unsupported Media Type.
  # It appears to ignore the Content-Type header and process the body as if it were valid JSON.
  Scenario Outline: Unsupported Content-Type (<Content-Type>) should return 415 and "Unsupported Media Type"
    Given I have username <valid>
    And I have password <valid>
    And the request Content-Type is set to "<Content-Type>"
    When I send POST to "/auth"
    Then the response status code should be 415
    And the response body should be plain text "Unsupported Media Type"

    Examples:
      | Content-Type         |
      | text/xml             |
      | application/xml      |
      | text/plain           |
      | application/x-yaml   |
      | image/jpeg           |
      | application/zip      |
      | text/html            |
      | application/protobuf |

  # BUG: The API returns 404 Not Found for unallowed methods (Expected: 405 Method Not Allowed).
  Scenario Outline: HTTP Method Check: <Method> should return 405 and "Not Found"
    When I send <Method> to "/auth"
    Then the response status code should be 405

    Examples:
      | Method  |
      | GET     |
      | PUT     |
      | DELETE  |
      | PATCH   |

  # BUG: The API returns 200 OK for invalid credentials (Expected: 401 Unauthorized).
  Scenario Outline: Invalid credentials returns 401 (<Description>) and "Bad credentials"
    Given I have username <username>
    And I have password <password>
    When I send POST to "/auth"
    Then the response status code should be 401
    And the response body should only contain keys: "reason"
    And the response should contain reason "Bad credentials"

    Examples:
      | Description                      | username     | password     |
      | Invalid Password String          | <valid>      | "wrongpass"  |
      | Invalid Username String          | "wronguser"  | <valid>      |
      | Empty Password String            | <valid>      | ""           |
      | Empty Username String            | ""           | <valid>      |
      | Missing Username Placeholder     | "<missing>"  | <valid>      |
      | Missing Password Placeholder     | <valid>      | "<missing>"  |
      | Numeric Username (Integer)       | 123          | <valid>      |
      | Numeric Password (Integer)       | <valid>      | 123          |
      | Boolean Username (True)          | true         | <valid>      |
      | Boolean Password (False)         | <valid>      | false        |
      | Numeric Username (Float)         | 3.14         | <valid>      |
      | Numeric Password (Float)         | <valid>      | 0.0          |

  # BUG: The API omits the Connection header, which is expected to be present
  # (e.g., Connection: keep-alive) for persistent connection management.
  Scenario Outline: Connection-management header on <Method>
    Given I have username <valid>
    And I have password <valid>
    When I send <Method> to "/auth"
    Then the response status code should be 200
    And the response header "Connection" should be present

    Examples:
      | Method  |
      | POST    |
      | OPTIONS |