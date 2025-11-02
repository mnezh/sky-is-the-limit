Feature: Simple math check

  Scenario: Verify that 2 + 2 equals 4
    Given I have two numbers 2 and 2
    When I add them
    Then the result should be 4
