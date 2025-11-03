@auth @stable @resilience
Feature: Authentication endpoint is resilient
  Scenario Outline: Authentication fails: Enormous payload size for <Field> (Expected: 400 or 413)
    Given the request body contains a 500KB string for the <Field> field
    When I send POST to "/auth" with the raw body
    Then the response status code should be 413
    And the response body should be plain text "Payload Too Large"

    Examples:
      | Field    |
      | username |
      | password |

  Scenario Outline: Authentication fails: Malicious or boundary strings (<Description>)
    Given the request body is set to:
      """
      {"username": "<username>", "password": "<password>"}
      """
    When I send POST to "/auth" with the raw body
    Then the response status code should be 400
    And the response body should be plain text "Bad Request"

    Examples:
      | Description                               | username                  | password                  |
      | SQL Injection Attempt (username)          | "' OR 1=1 --"              | "password123"             |
      | XSS Payload (password)                    | "admin"                   | "<script>alert(1)</script>" |
      | Directory Traversal (username)            | "../../etc/passwd"        | "password123"             |
      | JSON Escape Sequence Bomb (password)      | "admin"                   | "\u0000" * 100            |
      | Max Boundary String (username)            | "1234567890" * 25         | "password123"             |