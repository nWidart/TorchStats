### Torchstats — Project‑specific Development Guidelines

This document captures build, configuration, testing, and development specifics for this Vaadin + Spring Boot project. It is intentionally focused on details that matter for this repository rather than generic guidance.

---

#### Build and Configuration

- Stack and versions
  - Spring Boot: 3.5.7 (parent in `pom.xml`)
  - Vaadin: 24.9.4 (`vaadin-bom` and `vaadin-maven-plugin`)
  - JDK: Target and runtime are effectively Java 21 in CI/Docker. The POM has `<java.version>25</java.version>`, but build images use Eclipse Temurin 21 (see Dockerfile). For consistency, develop and build with JDK 21 unless there is a deliberate move to Java 25 with toolchains.

- Local dev run
  - First run will prepare Vaadin frontend automatically via the plugin execution.
  - Command:
    ```bash
    ./mvnw spring-boot:run
    ```
  - The app starts with embedded server and Vaadin dev tools. Theme is `default` (see `@Theme("default")` in `com.nwidart.Application`).

- Packaging
  - Regular dev build:
    ```bash
    ./mvnw -DskipTests package
    ```
  - Production build (frontend compiled, dev deps excluded via profile):
    ```bash
    ./mvnw clean package -Pproduction -DskipTests
    ```
  - The `production` profile triggers Vaadin `build-frontend` at `compile` phase.

- Vaadin frontend specifics
  - `vaadin-maven-plugin` binds `prepare-frontend` by default; for clean environments:
    ```bash
    ./mvnw -Pproduction vaadin:build-frontend
    ```
  - Frontend assets and generated files live under `src/main/frontend` and `src/main/frontend/generated/*`.

- Docker
  - Multi-stage build uses Eclipse Temurin 21 JDK/JRE and copies `jq` for optional key handling:
    ```Dockerfile
    FROM eclipse-temurin:21-jdk AS build
    # ...
    RUN --mount=type=cache,target=/root/.m2 \
        --mount=type=secret,id=proKey \
        --mount=type=secret,id=offlineKey \
        sh -c 'PRO_KEY=$(jq -r ".proKey // empty" /run/secrets/proKey 2>/dev/null || echo "") && \
        OFFLINE_KEY=$(cat /run/secrets/offlineKey 2>/dev/null || echo "") && \
        ./mvnw clean package -Pproduction -DskipTests -Dvaadin.proKey=${PRO_KEY} -Dvaadin.offlineKey=${OFFLINE_KEY}'
    ```
  - Build and run:
    ```bash
    docker build -t torchstats .
    docker run --rm -p 8080:8080 torchstats
    ```
  - The runtime entrypoint activates Spring profile `prod`.

- Persistence & profiles
  - H2 is used for tests and local runs via `spring-boot-starter-data-jpa` + `com.h2database:h2`. No explicit datasource config is required for tests.

---

#### Testing

- Frameworks
  - JUnit 5 via Spring Boot starter (`spring-boot-starter-test`).
  - AssertJ is available by default (used in `TaskServiceTest`).

- Running tests
  - All tests:
    ```bash
    ./mvnw test
    ```
  - One test class (Surefire):
    ```bash
    ./mvnw -Dtest=com.nwidart.examplefeature.TaskServiceTest test
    ```
  - One specific test method:
    ```bash
    ./mvnw -Dtest=com.nwidart.examplefeature.TaskServiceTest#tasks_are_validated_before_they_are_stored test
    ```
  - Fail fast on the first failure:
    ```bash
    ./mvnw -Dsurefire.skipAfterFailureCount=1 test
    ```

- Spring tests
  - Typical pattern:
    ```java
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    @Transactional
    class TaskServiceTest { /* ... */ }
    ```
  - Notes:
    - H2 in-memory DB is auto-configured for tests; transactional tests roll back between methods.
    - Use `PageRequest` from Spring Data for paging (see `TaskServiceTest`).

- Example: adding a new test
  - Below is a minimal self-contained test class that does not start Spring. It demonstrates that JUnit/AssertJ are wired and that project classes are on the classpath.
    ```java
    package com.nwidart.smoke;

    import org.junit.jupiter.api.Test;
    import static org.assertj.core.api.Assertions.assertThat;

    class BuildSmokeTest {
        @Test
        void math_and_classpath() {
            assertThat(1 + 1).isEqualTo(2);
            assertThat(canLoad("com.nwidart.Application")).isTrue();
        }
        private boolean canLoad(String fqn) {
            try { Class.forName(fqn); return true; } catch (ClassNotFoundException e) { return false; }
        }
    }
    ```
  - Place under `src/test/java/com/nwidart/smoke/BuildSmokeTest.java`, run with `./mvnw -Dtest=com.nwidart.smoke.BuildSmokeTest test`, then remove the file when no longer needed.
  - Verified during guideline authoring: this sample compiles and runs in this project.

- Tips
  - Avoid starting the Spring context for pure logic tests to keep the suite fast; only use `@SpringBootTest` when needed.
  - For repository-layer tests, prefer `@DataJpaTest` if suitable.

---

#### Development Notes and Conventions

- UI and routing
  - Vaadin Flow views live under `src/main/java/com/nwidart/**/ui`. Examples:
    - `com.nwidart.examplefeature.ui.TaskListView` (menu-annotated view)
    - `com.nwidart.loganalyzer.ui.LogFileView` (route `@Route("log-file")`, uses Lumo utility classes)
  - `MainLayout` provides application-wide layout and toolbar components.

- Services & data
  - `TaskService` encapsulates task creation and listing using Spring Data repositories. Creation timestamps use `Instant.now()`; validation is indirectly enforced by the domain model and tests.

- Code style
  - Follow existing formatting and imports (standard Spring + Vaadin idioms). Avoid wildcard imports. Prefer constructor injection for Spring beans (see `TaskService`).
  - Nullness annotations: code uses `org.jspecify.annotations.Nullable` for optional parameters.

- Development practices
  - Keep methods short and focused on a single responsibility. Avoid long methods that perform multiple unrelated tasks.
  - Use meaningful variable names that describe their purpose. Avoid single-letter variable names, except for loop counters.
  - Write unit tests and integration tests for all new code.

- keep a changelog.md file in the root of the project up to date with the changes you make.

- Logging
  - SLF4J is used; example in `LogFileView` shows logger acquisition and simple info log when parsing logs.

- Frontend/dev server
  - On first run, Vaadin will trigger frontend preparation. If frontend changes are not picked up, run `./mvnw vaadin:prepare-frontend` or clean and restart.

- Java version alignment
  - While the POM states Java 25, Docker and the Vaadin/Spring ecosystem are currently aligned on Java 21 LTS. Use JDK 21 locally to match Docker and avoid toolchain inconsistencies. If upgrading to 25, add a Maven toolchain configuration to enforce consistent JDK selection across environments.

---

#### Quick Commands Reference

- Start app in dev mode: `./mvnw spring-boot:run`
- Run all tests: `./mvnw test`
- Run one test class: `./mvnw -Dtest=com.nwidart.examplefeature.TaskServiceTest test`
- Package for prod: `./mvnw clean package -Pproduction -DskipTests`
- Docker build/run: `docker build -t torchstats . && docker run --rm -p 8080:8080 torchstats`
