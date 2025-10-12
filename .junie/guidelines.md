Project guidelines for dreamlog

Project description:

- A personal dream journal application.

Teach Me Rule
If you notice poor code or bad practices, don’t just follow instructions blindly — explain why it’s wrong, improve it,
and leave the place better than you found it.
Always aim to educate through your fixes, not just to implement.

Repository layout
- backend: Spring Boot 3.5 (Java 21), JPA, Flyway, Spring Security, springdoc-openapi. Gradle 8.x.
- frontend: Angular 20 (standalone), Jest for unit tests, ESLint + Prettier, Husky hooks.
- docker-compose.yml: Postgres 17 + backend app.

Prerequisites

- Java: JDK 21 (Gradle uses toolchains).
- Node.js: g24.x; npm >= 10 (enforced via package.json engines).
- Docker: Required for backend integration tests using Testcontainers and for local stack via docker-compose.
- Optional: Docker Desktop (macOS/Windows) or a running Docker daemon (Linux).

Backend (Spring Boot)
Build and run

- Build: ./gradlew :backend:build
- Run locally: ./gradlew :backend:bootRun
- Run with Docker Compose:
    - docker compose up -d db
    - Optionally build and run backend container: docker compose up --build app
- Profiles/config:
    - Default profile uses application.properties; database defaults to Postgres per application config and/or env vars.
    - docker profile is used in docker-compose (SPRING_PROFILES_ACTIVE=docker) and expects DB at jdbc:postgresql://db:
        5432.
- Database/Migrations: Flyway auto-runs on startup; dependencies include flyway-core and flyway-database-postgresql.

Testing (backend)

- Full test suite: ./gradlew :backend:test
    - Integration tests extend IntegrationTestBase and start a Postgres 17 Testcontainers container. A running Docker
      daemon is mandatory.
    - If Docker is not available, these tests will fail with an error like:
      Previous attempts to find a Docker environment failed. Will not retry. Please see logs and check configuration
- Run only unit/web-slice tests that do not require Docker (useful when Docker is unavailable):
    - ./gradlew :backend:test --tests "*controller*"
    - Example (verified): DreamEntryControllerTests (4 tests) passed with 100% success. See
      backend/build/reports/tests/test.
- Test reports: backend/build/reports/tests/test/index.html (per execution scope).
- Useful tips:
    - Testcontainers will pull images (postgres:17-alpine). Network access required on first run.
    - To speed up, enable reusable containers (optional): export TESTCONTAINERS_REUSE_ENABLE=true and configure ~
      /.testcontainers.properties (if desired).

Frontend (Angular)
Install, build, run

- Install deps: cd frontend && npm ci
- Dev server: npm start (uses Angular CLI with proxy.conf.json)
- Production build: npm run build:prod
- Type checking: npm run typecheck
- Lint/format: npm run lint and npm run format; on commit, husky + lint-staged run Prettier on staged files.

Testing (frontend)

- Run tests: cd frontend && npm test
- Jest configuration:
    - jest.config.ts uses jest-preset-angular with jsdom environment, setup at src/setup-jest.ts.
    - ts-jest transpilation is configured via jest-preset-angular transform and tsconfig.spec.json.
- Adding a new unit test (example):
    - Create a file like frontend/src/app/example.spec.ts with:
      describe('math', () => {
      it('adds', () => {
      expect(1 + 2).toBe(3);
      });
      });
    - Run: npm test (the suite will include *.spec.ts files). This was verified by creating a temporary demo.spec.ts
      which passed, then removed to keep the repo clean.

API and documentation

- OpenAPI is exposed at /v3/api-docs and Swagger UI typically at /swagger-ui.html when the backend is running.
- There is an OpenApiSpecTests integration test that asserts /v3/api-docs is available; it requires Docker (via
  Testcontainers) because it boots the full context with a database.

Code style and conventions
Backend (Java)

- Language level: Java 21 via Gradle toolchain.
- Frameworks: Spring Boot 3.5.x, Spring Web, Spring Data JPA, Spring Validation, Spring Security.
- Database: PostgreSQL 17. Flyway for schema migrations. Runtime driver: org.postgresql:postgresql.
- Lombok is used. Prefer constructor injection or Lombok builders for immutability. Keep validation annotations (
  @NotNull, @Valid) on DTOs/controllers.
- Testing:
    - Unit/web-slice: @WebMvcTest for controllers with @AutoConfigureMockMvc(addFilters = false) to bypass security
      filters in tests.
    - Integration: @SpringBootTest with Testcontainers; extend IntegrationTestBase.
    - Consider tagging tests (e.g., @Tag("integration")) in future to separate runs: ./gradlew test -Dgroups=... or
      --tests filters.
- OpenAPI: springdoc-openapi-starter-webmvc-ui is present; keep public endpoints documented via annotations if
  applicable.

Frontend (Angular)

- Angular 20 standalone components (no NgModules). Use Angular CLI.
- Linting: angular-eslint with eslint-config-prettier; Prettier config in package.json (printWidth=100,
  singleQuote=true).
- Testing: Jest with jest-preset-angular, jsdom environment; prefer shallow tests for components/services. Store tests
  next to code as *.spec.ts.
- Styling: Prettier + ESLint; run npm run lint:fix to auto-fix.

Local stack (docker-compose)

- Services:
    - db: postgres:17 (mapped 5432:5432). Configurable via DB_USER, DB_PASSWORD, DB_NAME env vars.
    - app: builds backend Dockerfile in backend/, depends_on healthy db, exposes 8080.
- Usage:
    - docker compose up -d db # start only Postgres
    - docker compose up --build app # (optional) build/run backend container once DB is up

Troubleshooting

- Backend tests fail with IllegalStateException about Docker: start Docker Desktop or ensure DOCKER_HOST is set, then
  rerun ./gradlew :backend:test.
- First Testcontainers runs may be slow due to image pulls; subsequent runs are cached.
- If Angular Jest tests hang, ensure Node version satisfies engines and that npm ci has completed; clear cache with rm
  -rf node_modules && npm ci.

Selective and CI-friendly commands (verified)

- Frontend quick test: cd frontend && npm test --runInBand --ci
- Backend unit-slice only: ./gradlew :backend:test --tests "*controller*"  # avoids Testcontainers
- Full backend test suite (requires Docker): ./gradlew :backend:test

Contribution workflow

- Create feature branches, run backend unit-slice tests and frontend tests locally before PRs: npm run verify (
  typecheck + build + tests) on frontend; ./gradlew build on backend (with Docker for full tests).
- Keep commits atomic and well-described. Update tests alongside code.
