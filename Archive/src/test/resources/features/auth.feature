@auth @stable
Feature: Authentication endpoint

  Scenario Outline: Successful authentication with valid credentials (<Description>)
    Given I have username <valid> and password <valid>
    And the request Content-Type is set to "<content-type>"
    When I POST payload to "/auth"
    Then the response status code should be 200
    And the response should contain a token
    And the produced token is a valid format string
    And the response body should only contain keys: "token"
    And the response header "Server" should be present
    And the response header "Content-Type" should contain "application/json"

    Examples:
      | content-type                      |
      | application/json                  |
      | application/x-www-form-urlencoded |

  Scenario: Sending valid payload twice produces two distinct tokens
    Given I have username <valid> and password <valid>
    When I POST payload to "/auth"
    Then the response status code should be 200
    And I store the token as 'token1'
    When I POST payload to "/auth"
    Then the response status code should be 200
    And I store the token as 'token2'
    And the token 'token1' is different from 'token2'

  Scenario: HTTP Method Check: OPTIONS should return POST allowed
    When I OPTIONS raw to "/auth"
    Then the response status code should be 200
    And the response header "Allow" should contain "POST"

  Scenario Outline: Authentication fails: Malformed JSON payload (<Description>)
    Given the request body is set to:
      """
      <Payload>
      """
    When I POST raw to "/auth"
    Then the response status code should be 400
    And the response body should be plain text "Bad Request"

    Examples:
      | Description           | Payload                                                    |
      | Missing Comma         | {"username": "admin" "password": "password123"}            |
      | Missing Colon         | {"username" "admin", "password": "password123"}            |
      | Unquoted Key          | {admin: "admin", "password": "password123"}                |

