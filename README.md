# Restful Booker API Tests

Functional API test automation framework for the **Restful Booker API** using Java, Gradle, Cucumber, and Rest-Assured.

Implements requirements based on the [assignment document](ASSIGNMENT.md).

---

## 💻 Requirements & Setup

This project uses the gradle wrapper (`./gradlew`) to manage all dependencies and the required Gradle version.

* **JDK** is required to run the tests.
Implemented using `openjdk 25.0.1`, but is expected to work in other recent JDK versions.
* All project dependencies are downloaded automatically by `gradle`.

---

## 🚀 Execution & Reporting

### Run Tests

Execute the following command from the project root directory:

```bash
./gradlew clean test
```

---

## 🛠️ Configuration Overrides (CLI)

Default configuration values (e.g., API base URL, credentials) are read from [src/main/resources/config.properties](src/main/resources/config.properties).

To override a property for a specific run, use the -P flag.
CLI values take precedence over file defaults.

|Property|Description| CLI Override Example             |
|---|---|----------------------------------|
|base.url|API base URL| `-Pbase.url=http://dev.booker.com` |
|username|Auth username| `-Pusername=ci_user`               |
|password|Auth password| `-Ppassword=ci_token`              |

```shell
$ ./gradlew test -Pbase.url=[https://new-api.com](https://new-api.com)
```

---

## Filtering Tests using Cucumber Tags

You can filter which scenarios run using the cucumber.filter.tags system property via the -P flag.

The general syntax is:
```shell
$ ./gradlew test -Pcucumber.filter.tags="@TAG_NAME"
```

| Tag         | 	Description                                                                                             | 	Command Example                                      |
|-------------|----------------------------------------------------------------------------------------------------------|-------------------------------------------------------|
| @stable     | 	Runs core functional scenarios for the /auth endpoint (positive, negative, and structural checks).      | `./gradlew test -Pcucumber.filter.tags="@stable"`     |
| @bug        | 	Runs scenarios that document known bugs.                                                                | `./gradlew test -Pcucumber.filter.tags="@bug"`        |
| @resilience | 	Runs tests focused on non-functional robustness (e.g., enormous payloads and malicious string fuzzing). | `./gradlew test -Pcucumber.filter.tags="@resilience"` |

---

## 📊 Reports

The full, detailed HTML report is automatically generated after test completion at the standard Gradle location:
* HTML Report Path: `build/reports/tests/test/index.html`