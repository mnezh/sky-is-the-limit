## 🚀 Technical Approach and Implementation Strategy

This project utilizes a **Behavior-Driven Development (BDD)** framework written in **Java** with **Cucumber** to create a structured and maintainable test suite for the `/auth` and `/booking` API endpoints. The strategy focuses on layering the test code to separate concerns: data management, HTTP execution, and endpoint-specific business logic. This separation is crucial for scalability and readability.

---

### Core Technology Stack

The test solution relies on **Gradle** for dependency management and build execution.

* **BDD Framework:** **Cucumber** is the foundation, defining feature files in a human-readable Gherkin language.
* **HTTP Client:** **RestAssured** is used for all API interaction, simplifying the construction of complex requests (e.g., handling URL-encoded forms and different Content-Types) and powerful response validation.
* **Data Handling:** **Jackson** libraries are integrated to seamlessly handle both **JSON** and **XML** payloads for requests and to parse responses, supporting the API's content negotiation requirements.
* **State Management:** **PicoContainer** is implemented to provide dependency injection, ensuring that the **`ScenarioContext`** object is safely shared between all step definition classes. This mechanism is key for persisting state, such as authentication tokens or booking IDs, across multiple steps in a single scenario.

---

## 🏗️ Implementation Strategy: Step Definitions

The testing logic is divided into four Java classes, adhering to a specialized form of the Page Object Model (POM) for API testing.

### 1. `BaseSteps.java` (Abstract Foundation)

* **Purpose:** Provides a shared, abstract layer for common test infrastructure.
* **Functionality:** Houses the shared **`ScenarioContext`** (state management) and utility functions, notably a logging method (`log(String message)`) that prints messages directly into the Cucumber report for enhanced debugging.

### 2. `HttpSteps.java` (Execution Layer)

* **Purpose:** Manages the entire HTTP lifecycle, independent of any specific endpoint.
* **Functionality:** Contains the generic `When` steps to execute **`POST`**, **`GET`**, **`OPTIONS`**, etc., requests using RestAssured. It also handles core validation steps:
    * Asserting the **response status code**.
    * Validating the presence and content of **HTTP headers** (`Content-Type`, `Allow`, `ETag`).
    * Validating **plain text response bodies**.
    * Configuring headers and raw request bodies.

### 3. `AuthSteps.java` (Authentication Logic)

* **Purpose:** Implements specific BDD steps for the `/auth` endpoint.
* **Functionality:** Handles the creation and parsing of the authentication payload. It contains assertions specific to tokens:
    * **Token Format Validation** (using regex).
    * Verification that **two sequential calls yield unique tokens**.
    * Storing and retrieving tokens from the `ScenarioContext`.

### 4. `BookingSteps.java` (Booking Logic)

* **Purpose:** Implements specific BDD steps for the `/booking` endpoint.
* **Functionality:** Responsible for complex payload generation (e.g., creating valid, invalid, or incomplete booking objects). It includes robust validation methods:
    * Comparing the **response booking details against the original request payload** (JSON and XML).
    * **Booking ID uniqueness** verification.
    * Logic to manipulate the payload for negative testing (missing fields, invalid dates).

---

## 💡 Recommended Modernization: Kotlin and Kotest

While the current **Java/Cucumber** implementation is robust, switching to **Kotlin** paired with the **Kotest framework's Behavior-Driven Development (BDD) style** could significantly improve the testing experience:

* **Reduced Boilerplate (Kotlin):** Kotlin's conciseness (e.g., data classes, null safety) would drastically reduce the boilerplate code currently required in the Java step definition files.
* **Built-in BDD (Kotest):** Kotest allows you to write the `Given/When/Then` structure directly in Kotlin code, eliminating the need for separate `.feature` files and the overhead of maintaining regex-based Cucumber glue code. This simplifies debugging and navigation, keeping business logic and implementation closer together.

---

## 🧪 Test Coverage Structure

The test scenarios are logically grouped by endpoint and test focus:

### I. `/auth` (Authentication Endpoint)

* **A. Stable Functionality (`auth.feature`)**
    * Successful Token Generation (JSON & URL-Encoded).
    * Token Uniqueness (Two sequential calls yield distinct tokens).
    * Token Format and Response Body Key Validation.
    * HTTP Method Check (`OPTIONS` returns `POST` allowed).
    * Malformed JSON Payload Error Handling (`400 Bad Request`).
    * Required Response Headers (`Content-Length`, `Content-Type`, `ETag`, `Server`).
* **B. Resilience and Security (`auth.resilience.feature`)**
    * Payload Size Limits for `username` and `password` (`413 Payload Too Large`).
    * Malicious String Injection Attempts (SQLi, XSS, Path Traversal, JSON Escape Sequence Bomb).
    * Max Boundary String Length Check.
* **C. Known Bugs (`auth.known-bugs.feature`)**
    * Unsupported Content-Type (Expected `415`, currently `200 OK`).
    * Unallowed HTTP Methods (`GET`, `PUT`, `DELETE`, `PATCH`) (Expected `405`, currently `404 Not Found`).
    * Invalid/Missing Credentials (Expected `401 Unauthorized`, currently `200 OK` for invalid credentials).
    * Missing `Connection` Response Header.

### II. `/booking` (Booking Creation Endpoint)

* **A. Stable Functionality (`booking.feature`)**
    * Successful Creation (JSON & XML Content-Type).
    * Content Negotiation based on `Accept` header (`*/*`, `application/json`, `application/xml`).
    * HTTP Method Check (`OPTIONS` returns `POST` allowed).
    * Response Headers (`Content-Type`, `ETag`, `Server`).
* **B. Resilience and Security (`booking.resilience.feature`)**
    * Payload Size Limits (`413 Payload Too Large`) across all fields (`firstname`, `lastname`, `totalprice`, `depositpaid`, `bookingdates`, `additionalneeds`).
    * Malicious String Injection Attempts (SQLi, XSS, Path Traversal, JSON Escape Sequence Bomb).
    * Max Boundary String Length Check.
* **C. Known Bugs (`booking.known-bugs.feature`)**
    * Duplicate Booking ID Bug (Expected unique ID for each request).
    * Valid XML Content-Type Processing Failure (Expected `200`, actual: `500 Internal Server Error`).
    * Missing Required Fields (Expected `400 Bad Request`, actual: `500 Internal Server Error`).
    * Invalid Date and Data Type Validation (Expected `400`, actual: `500 Internal Server Error` for some cases).
    * Incorrect Response Content-Type for XML `Accept` (Expected: `application/xml` for `application/xml` accept, currently incorrect).
    * Invalid Data Types being ignored (Expected `400 Bad Request`, actual: `200 OK` or different error).