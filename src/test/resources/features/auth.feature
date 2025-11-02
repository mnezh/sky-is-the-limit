Feature: Authentication endpoint

  Scenario: Successful authentication with valid credentials
    Given I have username <valid>
    And I have password <valid>
    When I send POST to auth
    Then the response status code should be 200
    And the response should contain a token

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
