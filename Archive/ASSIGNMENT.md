# QA Automation Engineer Project

## Overview

This assignment is designed to evaluate your ability to:
* Design a concise test approach for a RESTful API
* Implement basic automated functional tests using Java and a BDD framework (Cucumber or Gauge)
* Communicate clearly and document your work

## Assignment

You will be testing the public [Restful Booker API](https://restful-booker.herokuapp.com/apidoc/index.html).

Please focus on these two endpoints:
* POST /auth – Create an authentication token
* POST /booking – Create a new booking
* (Optional): PUT /booking/{id} – Update an existing booking

Make sure your tests check for HTTP status code, response body structure, key response headers. Consider covering some negative scenarios too.

## Deliverables

Please submit: 

* **A brief test approach document** (max 1 page). Bullet points are fine. 
* **Source code for the automated tests**
  * Cover positive and negative test cases for the two endpoints above 
  * Use a BDD (JUnit, TestNG, Cucumber, etc.)
* **Instructions to run the tests** e.g. a short README or a comment block
* **(Optional)**: Suggest possible improvements to your submission
