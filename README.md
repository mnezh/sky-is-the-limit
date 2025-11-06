# Restful Booker API Tests

Functional API test automation framework for the **Restful Booker API** using **Java**, **Gradle**, **Cucumber**, and **Rest-Assured**.

It implements the assignment requirements following a specific approach (see `ASSIGNMENT.md` and `SOLUTION.md`).

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

The detailed **Cucumber Report** is automatically generated after test completion at the following path:

* Report Path: `build/reports/cucumber/cucumber-report.html`

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