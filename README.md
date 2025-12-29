# Smart Store Testing 

Automated testing project for a Java **Smart Store** application 
**The Smart Store application itself was instructor-provided; my work focused on building the test suite** (unit, integration, and end-to-end) and validating REST behavior with both internal and external mock servers.

## What’s in this repo

- **Unit tests** across controller / service / repository / model layers (Mockito + JUnit 5)
- **Integration tests** that validate layer-to-layer behavior and REST contracts
- **End-to-end tests** that exercise full workflows
- **Internal mock server tests** using MockServer (local controlled responses)
- **External integration tests** against an **Apidog cloud mock API** (verifies real HTTP request/response behavior)

## Tech stack

- Java **21**
- Maven
- JUnit 5, Mockito
- Rest Assured + Hamcrest (HTTP assertions)
- MockServer (internal mock REST server)
- JaCoCo (coverage)

---

## Project structure

```
src/
  main/java/com/se300/store/        # Instructor-provided Smart Store app code
  test/java/com/se300/store/
    controller/
      unit/
      integration/
      internalmockserver/
      externalmockserver/           # Apidog-based external integration tests
    service/
      unit/
      integration/
    repository/
      unit/
      integration/
    model/unit/
    EndToEndSmartStoreTest.java
```

---

## Getting started

### Prerequisites
- **JDK 21**
- **Maven 3.9+** (or use the included Maven wrapper if present)

### Run all tests
```bash
mvn test
```

### Run a specific test class (example)
```bash
mvn -Dtest=EndToEndSmartStoreTest test
```

---

## Coverage (JaCoCo)

Generate a coverage report:

```bash
mvn test jacoco:report
```

Open:

- `target/site/jacoco/index.html`

---

## External integration tests (Apidog)

This repo includes tests that hit an external mock Smart Store API hosted on **Apidog** using Rest Assured.

- Test class: `ExternalMockServerTest`
- Base URL (update if your Apidog mock changes):
  - `https://mock.apidog.com/m1/1143674-1136088-default`

Run only the external integration tests:

```bash
mvn -Dtest=ExternalMockServerTest test
```

**Notes**
- These tests require internet access and can fail if the Apidog mock is down/changed.
- If the mock URL or schema changes, update the `EXTERNAL_API_BASE_URL` constant in `ExternalMockServerTest`.

---

## Collaboration / version control

This project was developed collaboratively using **GitHub for version control**, including:
- feature branching
- pull requests
- code review

---

## Disclaimer

This repository includes instructor-provided application code for educational context.  
The primary contribution of this repo is the **test suite** and supporting test infrastructure.

