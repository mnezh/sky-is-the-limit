@booking @stable
Feature: Create booking

  Scenario Outline: Create booking with valid data (<content-type>)
    Given I have a valid booking payload
    And the request Content-Type is set to "<content-type>"
    When I POST payload to "/booking"
    Then the response status code should be 200
    And the response should contain a booking id
    And the JSON booking details should match the request
    And the response header "Content-Type" should contain "application/json"
    And the response header "Content-Type" should contain "charset"
    And the response header "ETag" should be present
    And the response header "Server" should be present

    Examples:
      | content-type     |
      | application/json |
      | text/xml         |

  Scenario Outline: Create booking with accept type (<accept>)
    Given I have a valid booking payload
    And the request header "Accept" is set to "<accept>"
    When I POST payload to "/booking"
    Then the response status code should be 200
    And the <response-type> booking details should match the request

    Examples:
      | accept           | response-type |
      | */*              | JSON          |
      | application/json | JSON          |
      | application/xml  | XML           |

  Scenario: HTTP Method Check: OPTIONS should return POST allowed
    When I OPTIONS raw to "/booking"
    Then the response status code should be 200
    And the response header "Allow" should contain "POST"