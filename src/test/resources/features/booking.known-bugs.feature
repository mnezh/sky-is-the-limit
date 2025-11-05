@booking @bugs
Feature: Known Bugs of Create booking

  # BUG: duplicated booking id for the same payload
  Scenario: Sending valid payload twice produces two distinct bookings
    Given I have a valid booking payload
    When I POST payload to "/booking"
    Then I store the booking id as 'id1'
    When I POST payload to "/booking"
    Then I store the booking id as 'id2'
    And the id 'id1' is different from 'id1'

  # BUG: valid XML Content-Type causes 500
  Scenario Outline: Create booking with valid data (<content-type>)
    Given I have a valid booking payload
    And the request Content-Type is set to "<content-type>"
    When I POST payload to "/booking"
    Then the response status code should be 200
    And the response should contain a booking id
    And the JSON booking details should match the request

    Examples:
      | content-type            |
      | text/xml; charset=UTF-8 |
      | application/xml         |


  # BUG: incomplete payload should cause 400 (Bad Request),
  # but actual response if 500 (Internal Server Error)
  Scenario Outline: Create booking with missing field
    Given I have a booking payload missing <field>
    When I POST payload to "/booking"
    Then the response status code should be 400

    Examples:
      | field           |
      | firstname       |
      | lastname        |
      | totalprice      |
      | depositpaid     |
      | bookingdates    |
      | additionalneeds |

  # BUG: invalid dates in payload should cause 400 (Bad Request),
  # but actual response if 500 (Internal Server Error)
  Scenario Outline: Create booking with invalid date: <description>
    Given I have a booking payload with checkin "<checkin>" and checkout "<checkout>"
    When I POST payload to "/booking"
    Then the response status code should be <status>

    Examples:
      | checkin    | checkout   | status | description                  |
      | 01-01-2024 | 10-01-2024 | 400    | Invalid date format          |
      | 2024/01/01 | 2024/01/10 | 400    | Invalid date format          |
      | 2024-13-01 | 2024-14-10 | 400    | Invalid date value           |
      | 2024-12-01 | <missing>  | 400    | Missing date value           |
      | <missing>  | 2024-12-01 | 400    | Missing date value           |
      | not-a-date | 2024-01-10 | 400    | Invalid date format          |
      | 2024-01-01 | 2023-12-31 | 400    | Checkout before checkin date |

  # BUG: wrong content type in response for XML accept or "I'm a Teapot" for text/xml
  Scenario Outline: Create booking with accept type (<accept>) and get <content-type>
    Given I have a valid booking payload
    And the request header "Accept" is set to "<accept>"
    When I POST payload to "/booking"
    Then the response status code should be 200
    And the response header "Content-Type" should contain "<content-type>"

    Examples:
      | accept          | content-type    |
      | application/xml | application/xml |
      | text/xml        | text/xml        |

  # BUG: some of the invalid fields are ignored and booking is created anyways
  Scenario Outline: Create booking with invalid data type (<Description>)
    Given I have a valid booking payload
    And I override field <field> with <value>
    When I POST payload to "/booking"
    Then the response status code should be 400
    And the response body should be plain text "Bad Request"

    Examples:
      | field        | value         | Description                                             |
      | totalprice   | "one hundred" | Non-numeric string instead of number                    |
      | depositpaid  | "true"        | String instead of boolean                               |
      | bookingdates | "2024-01-01"  | String instead of object {"checkin":...,"checkout":...} |
