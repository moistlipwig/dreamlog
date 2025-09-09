# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Gradle project. Root includes `:backend`.
- Backend (Spring Boot):
  - Source: `backend/src/main/java/pl/kalin/dreamlog/...`
  - Resources: `backend/src/main/resources`
  - Tests: `backend/src/test/java`
  - Flyway migrations (add here): `backend/src/main/resources/db/migration`

## Build, Test, and Development Commands
- Build all modules: `./gradlew build`
- Run tests: `./gradlew test`
- Run backend locally: `./gradlew :backend:bootRun`
  - Dev profile: `SPRING_PROFILES_ACTIVE=dev ./gradlew :backend:bootRun`
- Create runnable jar: `./gradlew :backend:bootJar`
  - Example run: `java -jar backend/build/libs/backend-0.0.1-SNAPSHOT.jar` (version may differ)

## Coding Style & Naming Conventions
- Language: Java 21 (Gradle toolchain enforced).
- Indentation: 4 spaces; UTF-8; Unix line endings.
- Packages: lowercase (`pl.kalin.dreamlog`); Classes: PascalCase; methods/fields: camelCase; constants: UPPER_SNAKE_CASE.
- Prefer Lombok for boilerplate (`@Getter`, `@Setter`, `@RequiredArgsConstructor`).
- Keep Spring layers clear: controller → service → repository. Avoid business logic in controllers.

## Testing Guidelines
- Framework: JUnit 5; Spring Boot test starter included.
- Name tests `*Tests.java` and mirror package structure.
- Favor slice tests (`@DataJpaTest`, `@WebMvcTest`) over full `@SpringBootTest` unless necessary.
- Run all tests: `./gradlew test`
- Run specific tests: `./gradlew :backend:test --tests 'pl.kalin.dreamlog.*'`

## Commit & Pull Request Guidelines
- Use small, focused commits in imperative mood.
- Prefer Conventional Commits: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`.
- PRs must include: clear description, linked issues, steps to verify locally (commands, sample requests), and notes on tests/migrations.

## Security & Configuration Tips
- Do not commit secrets. Configure via env vars or `application-<profile>.properties`.
  - Example: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`.
- Use profiles (`dev`, `test`, `prod`) via `SPRING_PROFILES_ACTIVE`.
- Place DB changes in Flyway migrations under `db/migration` with `V<version>__<desc>.sql` (e.g., `V1__init.sql`).

## Agent-Specific Notes
- Add new modules by updating root `settings.gradle` and creating a module folder.
- Do not change Java version or Gradle wrapper without discussion.
- Keep changes minimal and aligned with existing structure and dependencies.
