# Restful Booker API Tests

Functional API test automation framework for the **Restful Booker API** using **Java**, **Gradle**, **Cucumber**, and **Rest-Assured**.

It implements the assignment requirements following a specific approach (see [ASSIGNMENT.md](ASSIGNMENT.md) and [SOLUTION.md](SOLUTION.md)).

---

## 📚 Table of Contents
* [💻 Requirements & Setup](#-requirements--setup)
* [🚀 Execution & Reporting](#-execution--reporting)
    * [Run Tests](#run-tests)
    * [Code Formatting and Linting](#code-formatting-and-linting)
    * [Reports](#reports)
* [🐳 Running Tests in Docker](#-running-tests-in-docker)
* [🛠️ Configuration Overrides (CLI)](#️-configuration-overrides-cli)
* [Filtering Tests using Cucumber Tags](#filtering-tests-using-cucumber-tags)

---

## 💻 Requirements & Setup

This project uses the **Gradle wrapper** (`./gradlew`) to manage all dependencies and the required Gradle version.

* **JDK** is required to run the tests. It was implemented using `openjdk 25.0.1`, but it should work with other recent JDK versions.
* All project dependencies are downloaded automatically by `gradle`.

---

## 🚀 Execution & Reporting

### Run Tests

Execute the following command from the project root directory:

```bash
./gradlew clean test
```

### Code Formatting and Linting

This project uses the **Spotless** Gradle plugin with **Google Java Format** to maintain consistent code style and formatting.

* To check for formatting violations:
  ```bash
  ./gradlew spotlessCheck
  ```
* To automatically apply the correct formatting:
  ```bash
  ./gradlew spotlessApply
  ```

### Reports

The detailed **Cucumber Report** is automatically generated after test completion at the following path, unifying local and Docker output:

* Report Location: `cucumber-reports/cucumber/cucumber-report.html`

---

## 🐳 Running Tests in Docker

This framework is configured for consistent test execution using a single Docker image and the included `Makefile`.

### 1. Build the Docker Image
The `build` target uses the `Dockerfile` to create a self-contained image with all Java 25 and Gradle dependencies.

```bash
make build
```

### 2. Run All Tests
The `run` target executes all tests. It automatically mounts your local `./cucumber-reports` directory to the container's `/reports` path.

```bash
make run
```

### 3. Run Tests with Tags/Overrides
Use `run-with-args` to pass Cucumber tags or configuration properties via environment variables.

```bash
make run-with-args TAGS="@smoke" BASE_URL="http://dev.api"
```

### 4. Access Reports
The reports are generated in a unified path on your host machine, whether running locally or in Docker.

**Report Location:** `./cucumber-reports/cucumber/cucumber-report.html`

---

## 🛠️ Configuration Overrides (CLI)

Default configuration values (e.g., API base URL, credentials) are read from `src/main/resources/config.properties`.

To override a property for a specific test run, use the **`-P` flag**. CLI values take precedence over file defaults.

* **`base.url`**: API base URL
    * *Example:* `-Pbase.url=http://dev.booker.com`
* **`username`**: Authentication username
    * *Example:* `-Pusername=ci_user`
* **`password`**: Authentication password
    * *Example:* `-Ppassword=ci_token`

Example command:

```shell
$ ./gradlew test -Pbase.url=https://new-api.com
```

---

## Filtering Tests using Cucumber Tags

You can filter which scenarios run by using the **`-Ptags`** flag, which sets the `cucumber.filter.tags` system property.

The general syntax is:
```shell
$ ./gradlew test -Ptags="@TAG_NAME"
```

Commonly used tags:

* **Filter by Feature/Endpoint:**
    * `@booking`: Scenarios for the core Booking endpoints.
        * *Example:* `./gradlew test -Ptags="@booking"`
    * `@auth`: Scenarios for the Authentication (`/auth`) endpoint.
        * *Example:* `./gradlew test -Ptags="@auth"`
* **Filter by Type/Status:**
    * `@stable`: Runs core functional scenarios that are considered stable and healthy.
        * *Example:* `./gradlew test -Ptags="@stable"`
    * `@resilience`: Runs tests focused on non-functional robustness (e.g., enormous payloads and malicious string fuzzing).
        * *Example:* `./gradlew test -Ptags="@resilience"`
    * `@bug`: Runs scenarios that document known bugs.
        * *Example:* `./gradlew test -Ptags="@bug"`

### Combining Tags

You can combine tags using logical operators (e.g., `and`, `or`, `not`):

* **`@auth and @stable`**: Runs stable tests **only** for the `/auth` endpoint.
    * *Example:* `./gradlew test -Ptags="@auth and @stable"`
* **`@booking or @auth`**: Runs all scenarios for **both** the booking and auth features.
    * *Example:* `./gradlew test -Ptags="@booking or @auth"`