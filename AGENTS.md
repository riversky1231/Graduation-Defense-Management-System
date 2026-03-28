# Repository Guidelines

## Project Structure & Module Organization
This repository is a single-module Spring Boot 2.7 application built with Maven and Java 17. Main code lives under `src/main/java/com/example/defensemanagement`, organized by responsibility: `controller`, `service`, `service/impl`, `mapper`, `entity`, `config`, `interceptor`, `security`, and `common`. MyBatis XML mapper files are in `src/main/resources/mapper`, Thymeleaf templates are in `src/main/resources/templates`, and runtime configuration is in `src/main/resources/application.yml`. Seed data and schema bootstrap SQL live in `src/main/resources/data.sql`. Tests are under `src/test/java` and mirror the main package structure.

## Build, Test, and Development Commands
- `mvn spring-boot:run`: start the app locally on port `8080`.
- `mvn clean package`: compile, run tests, and build the JAR.
- `mvn test`: run the JUnit 5 test suite only.

Before running Maven, ensure `JAVA_HOME` points to a Java 17 JDK. For local setup, create the MySQL database, import `src/main/resources/data.sql`, then update credentials in `src/main/resources/application.yml`.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, one top-level class per file, `PascalCase` for classes, `camelCase` for methods and fields, and `UPPER_SNAKE_CASE` for constants. Keep controllers thin, business logic in services, and SQL definitions in mapper XML files matched to mapper interfaces. Name Spring components by role, for example `ScoreController`, `ScoreService`, and `ScoreServiceImpl`.

## Testing Guidelines
Tests use JUnit 5 with Spring Boot Test and Mockito. Name test classes after the unit under test with a `Test` suffix, for example `SessionAuthenticationFilterTest`. Prefer focused unit tests for service and security logic, and cover boundary cases for score calculation, authentication, and file handling. Run `mvn test` before opening a PR.

## Commit & Pull Request Guidelines
Recent history mixes clear commits such as `feat(scoring): support total-to-item auto split...` with vague messages like `commit` and `final version`. Prefer Conventional Commit style going forward: `feat:`, `fix:`, `docs:`, `test:`. Keep each commit scoped to one change. PRs should include a short summary, affected modules, database or config changes, test evidence, and screenshots when UI templates change.

## Security & Configuration Tips
Do not commit real database passwords, API keys, or production export templates with secrets. Treat `application.yml` values as local defaults only, and verify upload size and session settings when changing file or auth flows.
