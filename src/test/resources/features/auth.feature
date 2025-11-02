Feature: Authentication endpoint

  Scenario: Successful authentication with valid credentials
    Given I have username <valid>
    And I have password <valid>
    When I send POST to auth
    Then the response status code should be 200
    And the response should contain a token
    And the produced token is a valid format string

  Scenario: Sending valid payload twice produces two distinct tokens
    Given I have username <valid>
    And I have password <valid>
    When I send POST to auth
    Then the response status code should be 200
    And I store the token as 'token1'
    When I send POST to auth
    Then the response status code should be 200
    And I store the token as 'token2'
    And the token 'token1' is different from 'token2'

  Scenario Outline: Authentication fails: <Description>
    Given I have username <username>
    And I have password <password>
    When I send POST to auth
    Then the response status code should be 200
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

  Scenario Outline: Authentication fails: Malformed JSON payload (<Description>)
    Given the request body is set to:
      """
      <Payload>
      """
    When I send POST to auth with the raw body
    Then the response status code should be 400
    And the response body should be plain text "Bad Request"

    Examples:
      | Description           | Payload                                                    |
      | Missing Comma         | {"username": "admin" "password": "password123"}            |
      | Missing Colon         | {"username" "admin", "password": "password123"}            |
      | Unquoted Key          | {admin: "admin", "password": "password123"}                |
