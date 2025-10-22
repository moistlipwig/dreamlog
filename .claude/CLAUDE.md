# DreamLog - AI Agent Guidelines

**Version:** 1.0
**Last Updated:** 2025-10-12
**Language:** English (project communication may use Polish)

---

## Table of Contents

1. [Core Principles](#core-principles)
2. [Project Overview](#project-overview)
3. [Architecture & Repository Layout](#architecture--repository-layout)
4. [Technology Stack](#technology-stack)
5. [Development Workflow](#development-workflow)
6. [Coding Standards](#coding-standards)
7. [Database & Migrations](#database--migrations)
8. [Testing Strategy](#testing-strategy)
9. [Commit & PR Guidelines](#commit--pr-guidelines)
10. [Token Optimization for AI Agents](#token-optimization-for-ai-agents)
11. [Agent Feedback Protocol](#agent-feedback-protocol)
12. [Common Pitfalls & Troubleshooting](#common-pitfalls--troubleshooting)
13. [Project Roadmap Context](#project-roadmap-context)

---

## Core Principles

### Boy Scout Rule ⭐

** MOST IMPORTANT PRINCIPLE:**

If you notice poor code or bad practices, don't just follow instructions blindly — **explain why it's wrong, improve it, and leave the place better than you found it.** Always aim to educate through your fixes, not just to implement.
The same can be applied to user request! If you find that code which will be implented will not follow best practices (clean code, clean architecture DDD), suggest how it should be done.

**Examples:**

- User asks to add duplicate code → Suggest extracting to shared method and explain DRY principle
- User proposes inefficient query → Show optimized version with index usage explanation
- There is a code which uncle Bob wouldn't call clean code → suggest how it should be done

### KISS - Keep It Stupid Simple ⭐⭐⭐

**SECOND MOST IMPORTANT PRINCIPLE:**

**Simple, clean, working code beats clever, complex code EVERY TIME.**

Before writing ANY code, ask yourself:

- Is this the simplest solution that solves the problem?
- Am I adding unnecessary abstraction?
- Would this code be obvious to a junior developer?

**KISS does NOT mean:**

- ❌ Ignoring clean code principles
- ❌ Skipping error handling
- ❌ Writing spaghetti code
- ❌ Violating SOLID or DRY

**KISS DOES mean:**

- ✅ Straightforward, readable solutions
- ✅ Minimal layers of abstraction
- ✅ No premature optimization
- ✅ Code that does what it needs to, nothing more

**Examples:**

- User asks for complex async validator → Check if backend error handling is enough first
- Need password validation → Simple inline validator, not a 55-line component
- User suggests feature → Ask: "Can we solve this simpler?"

**BALANCE KISS + Boy Scout Rule:**

- Teach best practices, but keep solutions simple
- Refactor to clean code, but don't over-engineer
- Challenge complexity, embrace simplicity

### Token Efficiency Strategy

AI agents should work smart, not hard. Use these strategies:

1. **Guided Exploration:** Check this file's Quick Reference section BEFORE exploring codebase
2. **Pattern Reuse:** Look for existing similar implementations first (use Grep/Glob)
3. **Lazy Loading:** Don't read entire files if you need specific info (use Grep with context)
4. **Decision Trees:** Follow task-specific guides below instead of guessing

**Estimated token savings:** 60-80% reduction in exploration phase

---

## Project Overview

**Name:** DreamLog (Dziennik Snów)
**Purpose:** Personal dream journal application with AI analysis
**Status:** Phase 0 (Setup) → Phase 1 (Auth) in progress
**Architecture:** Monorepo with separate backend (Spring Boot) and frontend (Angular)

**Educational Goals:**

- Learn modern Java 25 features (Virtual Threads, Scoped Values, Records, Sealed classes)
- Master Angular 20 with Signals and standalone components
- Implement full-text search (PostgreSQL FTS + trigrams)
- Build real-time features (SSE, gRPC)
- Practice observability (Prometheus, Grafana, OpenTelemetry)
- Potential commercial deployment

**Key Technologies:**

- Backend: Java 25, Spring Boot 3.5+, PostgreSQL 17, Flyway
- Frontend: Angular 20, Material Design, TailwindCSS, Jest
- Infrastructure: Docker Compose, GitHub Actions CI/CD

---

## Architecture & Repository Layout

### Directory Structure

```
dreamlog/
├── .claude/                          # Claude Code configuration
│   ├── CLAUDE.md                     # This file (master guidelines)
│   └── commands/                     # Custom slash commands
├── .junie/                           # Junie (IntelliJ) configuration
│   └── guidelines.md                 # Redirects to .claude/CLAUDE.md
├── backend/                          # Spring Boot application
│   ├── src/main/
│   │   ├── java/pl/kalin/dreamlog/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # JPA repositories
│   │   │   ├── model/               # JPA entities
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   └── config/              # Spring configuration
│   │   └── resources/
│   │       ├── db/migration/        # Flyway migrations (V<n>__<desc>.sql)
│   │       ├── application.properties
│   │       └── application-docker.properties
│   ├── src/test/java/               # Tests mirror main structure
│   ├── build.gradle                 # Gradle build config
│   └── Dockerfile
├── frontend/                         # Angular application
│   ├── src/app/
│   │   ├── components/              # Angular components
│   │   ├── services/                # Angular services
│   │   ├── models/                  # TypeScript interfaces
│   │   └── guards/                  # Route guards
│   ├── .junie/
│   │   └── guidelines.md            # Angular-specific guidelines for all agents
│   ├── package.json
│   ├── jest.config.ts
│   └── proxy.conf.json              # Dev proxy to backend
├── docker-compose.yml                # Local stack (Postgres + services)
├── settings.gradle                   # Gradle multi-module config
└── README.md                         # Full project roadmap
```

### Quick Reference Paths

**Backend Critical Paths:**

- Controllers: `backend/src/main/java/pl/kalin/dreamlog/controller/`
- Services: `backend/src/main/java/pl/kalin/dreamlog/service/`
- Repositories: `backend/src/main/java/pl/kalin/dreamlog/repository/`
- Entities: `backend/src/main/java/pl/kalin/dreamlog/model/`
- DTOs: `backend/src/main/java/pl/kalin/dreamlog/dto/`
- Flyway migrations: `backend/src/main/resources/db/migration/`
- Tests (Groovy/Spock): `backend/src/test/groovy/`

**Frontend Critical Paths:**

- Components: `frontend/src/app/components/`
- Services: `frontend/src/app/services/`
- Models: `frontend/src/app/models/`
- Tests: Next to source files as `*.spec.ts`

**Configuration Files:**

- Backend config: `backend/src/main/resources/application*.properties`
- Frontend config: `frontend/package.json`, `frontend/angular.json`
- Docker: `docker-compose.yml` (Postgres on port 5432, backend on 8080)

---

## Technology Stack

### Backend (Spring Boot)

**Core Framework:**

- Java 25 (with Gradle toolchain enforcement)
- Spring Boot 3.5.x
- Spring Web (REST APIs)
- Spring Data JPA (with Hibernate)
- Spring Security (OAuth2/OIDC planned in Phase 1)
- Spring Validation (`@NotNull`, `@Valid` annotations)

**Database:**

- PostgreSQL 17 (with `unaccent` and `pg_trgm` extensions)
- Flyway for schema migrations
- HikariCP connection pool
- Future: pgvector for semantic search

**Build & Dependencies:**

- Gradle 8.x with Kotlin DSL
- Lombok for boilerplate reduction
- springdoc-openapi for API documentation

**Testing:**

- Groovy 4.0 + Spock Framework
- Testcontainers (Postgres 17 required for integration tests)
- Do not use MockMVC, instead use full integration test with rest template
- Spring Security Test

**Future Additions:**

- Micrometer + Prometheus (Phase 7)
- OpenTelemetry (Phase 7)
- gRPC for inter-service communication (Phase 4)

### Frontend (Angular)

**Core Framework:**

- Angular 20+ (standalone components only, NO NgModules)
- TypeScript 5.9
- RxJS 7.8 for reactive programming
- Zone.js 0.15

**UI Libraries:**

- Angular Material 20.2 (primary UI components)
- TailwindCSS 4.1 (utility-first styling)
- Future: ngx-charts for data visualization

**State Management:**

- Angular Signals (primary state mechanism)
- `computed()` for derived state
- Services with `providedIn: 'root'`
- Future: @ngrx/component-store for complex state

**Build & Tools:**

- Angular CLI 20.2
- esbuild (Angular's default builder)
- Jest 29.7 for unit testing (NO Karma/Jasmine)
- ESLint 9 + Prettier 3.6
- Husky for git hooks

**Code Quality:**

- ESLint with angular-eslint rules
- Prettier (100 char line width, single quotes)
- lint-staged for pre-commit formatting
- Strict TypeScript compilation

### Infrastructure

**Local Development:**

- Docker Compose (Postgres, backend, optional frontend)
- Hot reload: `./gradlew :backend:bootRun` and `npm start`

**CI/CD:**

- GitHub Actions (build, test, lint on every PR)
- Automated test reports
- Future: Docker image publishing, deployment to cloud

**Future Stack (Phases 4-11):**

- MinIO for object storage (Phase 8)
- Redis for caching (Phase 11 experiments)
- Kafka for event streaming (Phase 11)
- Prometheus + Grafana for monitoring (Phase 7)

---

## Development Workflow

**Backend URLs:**

- API: http://localhost:8080/api/v1/
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec: http://localhost:8080/v3/api-docs
- Actuator health: http://localhost:8080/actuator/health

**Frontend Development:**

```bash
cd frontend

# Dev server (with proxy to backend on :8080)
npm start
# Opens http://localhost:4200

# Type checking only
npm run typecheck

# Build for production
npm run build:prod

# Run tests
npm test

# Run tests in CI mode
npm run test:ci

# Lint and fix
npm run lint:fix

# Format code
npm run format

# Full verification (typecheck + build + test)
npm run verify
```

### Code Generation

**Backend OpenAPI:**

- Manual API design in `backend/src/main/resources/openapi.yaml`
- Spring annotations generate spec at runtime

**Frontend API Client:**

```bash
cd frontend
npm run generate:api  # Generates from backend OpenAPI spec
```

---

## Coding Standards

### Backend (Java)

**Language Level & Style:**

- Java 25 features encouraged (Virtual Threads, Scoped Values, Records, Sealed classes, Pattern matching)
- Indentation: 4 spaces
- Encoding: UTF-8
- Line endings: Unix (LF)
- Max line length: 120 characters (soft limit)

**Lombok Usage:**

Prefer constructor injection with Lombok
Use @Data for simple DTOs
Use @Builder for multiple argument contructors

**Testing (Groovy/Spock):**

**ALL backend tests MUST be written in Groovy using Spock Framework**

Why Spock?

- More readable with given/when/then blocks
- Better data-driven testing with `where:` blocks
- Cleaner mocking and stubbing
- Native Groovy power (closures, operator overloading)

**Spock Test Structure:**

```groovy
def "should do something meaningful"() {
  given: "initial context"
  def user = new User(email: "test@example.com")

  when: "action happens"
  def result = service.doSomething(user)

  then: "verify outcome"
  result.success
  result.data.size() == 1
}
```

**Integration Test Pattern:**

```groovy
@SpringBootTest
@Testcontainers
@Transactional
class MyIntegrationSpec extends Specification {

  @Shared
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl)
  }

  def "test name"() {
    // test implementation
  }
}
```

**Layering (Strict Separation):**

```
Controller (REST endpoints)
    ↓ calls
Service (Business logic)
    ↓ calls
Repository (Data access)
    ↓ calls
Database
```

**Rules:**

- Controllers only handle HTTP concerns (validation, response codes)
- Services contain all business logic
- Repositories are thin (extend JpaRepository)
- NO business logic in controllers or repositories

### Frontend (Angular)

**TypeScript Style:**

- Strict type checking enabled
- Prefer type inference when obvious
- NEVER use `any` — use `unknown` if type uncertain
- Use `interface` for data shapes, `type` for unions/intersections
-

**Forms:**

- ALWAYS use Reactive Forms (NOT Template-driven)

**Forbidden Patterns:**

- ❌ `@HostBinding`, `@HostListener` → Use `host` object in decorator
- ❌ NgModules → Use standalone components
- ❌ `ngClass`, `ngStyle` → Use `[class]`, `[style]` bindings
- ❌ `*ngIf`, `*ngFor` → Use `@if`, `@for`
- ❌ `@Input()`, `@Output()` decorators → Use `input()`, `output()` functions

---

## Database & Migrations

### PostgreSQL Configuration

**Version:** PostgreSQL 17
**Extensions Required:**

- `unaccent` - Remove diacritics for search
- `pg_trgm` - Trigram matching for fuzzy search
- `pgvector` - Vector similarity search (Phase 4.1)

### Flyway Migrations

**Location:** `backend/src/main/resources/db/migration/`

**Naming Convention:**

V<version>__<description>.sql
Examples:
1_init_schema.sql
2_add_dream_entries_table.sql
3_add_fulltext_search_indexes.sql
10_add_user_table.sql

**Best Practices:**

- NEVER modify existing migrations in production
- Always test migrations on clean database

---

## Testing Strategy

### Backend Testing

**Test Naming:** `*Spec.groovy` for Spock tests (Groovy) - **PRIMARY**

**CRITICAL RULE: ALL integration tests MUST extend `IntegrationSpec`**

Integration test base class (`backend/src/test/groovy/pl/kalin/dreamlog/IntegrationSpec.groovy`) provides:

- Shared PostgreSQL Testcontainer (faster test execution)
- Automatic Spring Boot configuration
- Dynamic property registration for database connection
- Proper setup/cleanup lifecycle

**Test Types:**

1. **Unit Tests (Fast, No Spring Context):**

- Plain Spock specifications
- Mock dependencies with Spock's built-in mocking
- NO `@SpringBootTest` annotation

2. **Integration Tests (Full Context + Testcontainers):**

- **MUST extend `IntegrationSpec`**
- Add `@Transactional` for automatic rollback
- Example:

**Running Tests:**

```bash
# All tests (requires Docker)
./gradlew :backend:test

# Only integration tests
./gradlew :backend:test --tests "*IntegrationSpec"

# Specific test class
./gradlew :backend:test --tests "GoogleOAuthIntegrationSpec"

# Test reports
# backend/build/reports/tests/test/index.html
```

### Frontend Testing

**Test Location:** Next to source file as `<name>.spec.ts`

**Jest Configuration:** `frontend/jest.config.ts` (uses `jest-preset-angular`)

---

## Commit & PR Guidelines

### Commit Message Format

Use **Conventional Commits** format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Rules:**

- Keep subject line ≤ 50 characters
- Use imperative mood ("add" not "added" or "adds")
- Don't end subject with period
- Separate subject from body with blank line
- Wrap body at 72 characters
- Use body to explain WHAT and WHY, not HOW

### Pull Request Guidelines

**PR Title:** Same format as commit messages
**PR Checklist:**

- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Tests added/updated
- [ ] Documentation updated (if needed)
- [ ] No new warnings or errors
- [ ] Migration tested (if database changes)

---

## Token Optimization for AI Agents

### Decision Trees for Common Tasks

#### Task: Add New REST Endpoint

```
1. Define API contract
   ├─ Check existing OpenAPI: backend/src/main/resources/openapi.yaml
   └─ OR auto-generate from annotations

2. Create/update controller
   ├─ Location: backend/src/main/java/pl/kalin/dreamlog/controller/
   ├─ Pattern: Grep for "@RestController" to find similar endpoints
   └─ Use @Valid for request validation

3. Implement service logic
   ├─ Location: backend/src/main/java/pl/kalin/dreamlog/service/
   └─ Keep business logic here (NOT in controller)

4. Create/update repository (if database access needed)
   ├─ Location: backend/src/main/java/pl/kalin/dreamlog/repository/
   └─ Extend JpaRepository<Entity, ID>

5. Add tests
   ├─ Controller test: @WebMvcTest pattern (see reference below)
   ├─ Service test: Unit test with mocks
   └─ Integration test: @SpringBootTest + Testcontainers (if needed)

6. Run verification
   └─ ./gradlew :backend:build
```

**Token savings:** Agent follows guided path instead of exploring 100+ files

#### Task: Add New Angular Component

```
1. Check if similar component exists
   └─ Glob: frontend/src/app/components/**/*.component.ts

2. Create component
   ├─ Location: frontend/src/app/components/<feature>/
   ├─ Use: ng generate component <name> --standalone
   └─ Pattern: Check DreamCardComponent for reference

3. Configure component
   ├─ Set changeDetection: ChangeDetectionStrategy.OnPush
   ├─ Use input()/output() functions (NOT decorators)
   ├─ Use signals for state
   └─ Use @if/@for in template (NOT *ngIf/*ngFor)

4. Add tests
   ├─ Create: <name>.component.spec.ts next to component
   └─ Pattern: Check existing *.spec.ts files

5. Import where needed
   └─ Add to standalone component's imports array

6. Run verification
   └─ npm run verify
```

#### Task: Add Database Migration

```
1. Determine version number
   ├─ Check: ls backend/src/main/resources/db/migration/
   └─ Next version: V<highest + 1>__<description>.sql

2. Create migration file
   ├─ Location: backend/src/main/resources/db/migration/
   └─ Naming: V5__add_mood_tracking.sql

3. Write SQL
   ├─ Pattern: Check V1__init_schema.sql for structure
   ├─ Include: CREATE TABLE, indexes, triggers
   └─ Add comments for rollback strategy

4. Update JPA entity
   └─ Location: backend/src/main/java/pl/kalin/dreamlog/model/

5. Test migration
   ├─ Stop backend: Ctrl+C
   ├─ Clean database: docker compose down -v && docker compose up -d db
   ├─ Run backend: ./gradlew :backend:bootRun
   └─ Check logs for Flyway success

6. Verify in database
   └─ docker compose exec db psql -U dreamlog -c "\d <table_name>"
```

#### Task: Fix Failing Test

```
1. Identify failure type
   ├─ Compilation error → Check imports and types
   ├─ Runtime error → Check mocks and test setup
   └─ Assertion error → Check expected vs actual values

2. For backend test failures
   ├─ Check if Docker is running (for Testcontainers)
   ├─ Run specific test: ./gradlew :backend:test --tests "TestClassName"
   └─ Check test reports: backend/build/reports/tests/test/index.html

3. For frontend test failures
   ├─ Check Jest config: frontend/jest.config.ts
   ├─ Run specific test: npm test -- <test-name>
   └─ Check for missing imports in standalone components

```

### Pattern Reference Library

When implementing new features, USE THESE as templates (saves 60% tokens):

**Backend Controller Pattern:**

```
Reference: backend/src/main/java/pl/kalin/dreamlog/controller/DreamEntryController.java:1
Search: Grep for "@RestController" in backend/src/main/java/
Pattern includes:
- @RestController + @RequestMapping
- @RequiredArgsConstructor for DI
- @Valid for request validation
- ResponseEntity return types
```

**Angular Component Pattern:**

```
Reference: frontend/src/app/components/landing-page/landing-page.component.ts:1
Search: Glob for "**/*.component.ts" in frontend/src/app/
Pattern includes:
- Standalone component (no NgModules)
- ChangeDetectionStrategy.OnPush
- Signal-based state
- Native control flow (@if/@for)
```

**Angular Service Pattern:**

```
Reference: frontend/src/app/services/*.service.ts
Search: Glob for "**/*.service.ts" in frontend/src/app/
Pattern includes:
- @Injectable({ providedIn: 'root' })
- inject() instead of constructor DI
- Observable-based API methods
```

### Guided Exploration vs Full Reads

**❌ INEFFICIENT (Full Read):**

```typescript
// Agent reads entire file to find one method
Read: backend / src / main / java / pl / kalin / dreamlog / service / DreamService.java
// 300 lines, 2000 tokens consumed
```

**✅ EFFICIENT (Guided Grep):**

```typescript
// Agent searches for specific method
Grep: pattern = "findDreamById"
path = "backend/src/main/java/pl/kalin/dreamlog/service/"
output_mode = "content" - B
2 - A
10
// Returns only relevant method (15 lines, 100 tokens)
```

**Commands for Efficient Exploration:**

```bash
# Find all controllers
Glob: **/controller/*Controller.java

# Find specific method implementation
Grep: pattern="@PostMapping.*dreams" path="backend/src/main/java"

# Find test for specific class
Glob: **/test/**/DreamServiceTests.java

# Find Angular component by name
Glob: **/components/**/dream-list.component.ts

# Find where a service is injected
Grep: pattern="inject\(DreamService\)" path="frontend/src/app"
```

### Caching Strategies

**Information to remember across tasks (reduces repeat reads):**

1. **Project structure** - Already documented in Quick Reference section above
2. **Build commands** - Listed in Development Workflow section
3. **Common patterns** - Listed in Pattern Reference Library
4. **Configuration paths** - Listed in Architecture section

**When agent needs specific info:**

- ✅ Check this CLAUDE.md FIRST
- ✅ Use Grep/Glob for targeted search
- ❌ Don't read entire files speculatively

---

## Agent Feedback Protocol

### When to Challenge User Instructions

**You MUST speak up when you detect:**

1. **Security Anti-patterns**

- Storing secrets in code (passwords, API keys)
- SQL injection vulnerabilities
- Missing input validation
- Disabled security features without reason

2. **Performance Issues**

- N+1 query problems
- Missing database indexes
- Inefficient loops over large datasets
- Memory leaks (unclosed resources)

3. **Maintenance Nightmares**

- Massive code duplication (violation of DRY principle)
- Tight coupling between layers
- God classes (classes doing too much)
- Magic numbers without explanation

4. **Better Alternatives Exist**

- Reinventing the wheel (library already exists)
- Feature already implemented elsewhere in codebase
- More modern API available (e.g., old Date vs LocalDate)

5. **Implementation validates clean code, clean architecture or KISS (Keep It Stupid Simple)

### How to Provide Feedback

**❌ BAD Feedback:**

```
"I'll do that, but it's not optimal."
"OK, implementing as requested."
```

**✅ GOOD Feedback:**

```
"I notice this approach has [specific problem].

The industry best practice is [alternative approach] because [reason].

Here's what I recommend:
[concrete code example]

Benefits:
- [benefit 1]
- [benefit 2]

Shall I implement the better approach?"
```

### Feedback Etiquette

**DO:**

- ✅ Explain the WHY, not just WHAT
- ✅ Provide concrete alternatives with code examples
- ✅ Mention benefits of suggested approach
- ✅ Ask for confirmation before implementing differently
- ✅ Be respectful and educational

**DON'T:**

- ❌ Just comply without raising concerns
- ❌ Be condescending ("That's wrong, do it this way")
- ❌ Criticize without offering solutions
- ❌ Implement differently without asking first (unless security critical)

---

## Common Pitfalls & Troubleshooting

```

### Frontend Issues

**Problem: `npm start` fails with "Cannot find module"**

```

Solution:

1. Remove node_modules: rm -rf node_modules package-lock.json
2. Clean install: npm ci
3. Verify Node version: node -v (must be 20/22/24)
4. Verify npm version: npm -v (must be >=10)

```

**Problem: Jest tests hang indefinitely**

```

Solution:

1. Check for open handles: npm test -- --detectOpenHandles
2. Ensure TestBed.inject() is called within test function, not globally
3. Add timeout: jest.setTimeout(10000) in problematic test

```

**Problem: Angular build fails with "Component is not standalone"**

```

Solution:

1. Ensure component has standalone: true (or omit, it's default in Angular 20)
2. Check imports array includes all used components/directives
3. Don't import NgModules in standalone components

```

**Problem: Proxy to backend not working (404 on API calls)**

```

Solution:

1. Check proxy.conf.json has correct backend URL (http://localhost:8080)
2. Ensure backend is running: curl http://localhost:8080/actuator/health
3. Restart dev server: npm start

## Project Roadmap Context

**Current Status:** Phase 0 (Setup) → Phase 1 (Auth) beginning

For detailed roadmap with learning objectives and DoD criteria, see: [README.md](../README.md)

**Completed:**

- ✅ Repository setup with CI/CD
- ✅ Docker Compose with PostgreSQL 17
- ✅ Spring Boot skeleton with Actuator, Swagger
- ✅ Angular 20 skeleton with Material + TailwindCSS
- ✅ Testing infrastructure (Jest, Testcontainers)

**Next Priorities (Phase 1 - Auth):**

- 🔄 Google OAuth2 integration (Authorization Code + PKCE)
- 🔄 BFF pattern with HttpOnly cookies
- 🔄 `/api/me` endpoint for user info
- 🔄 Angular auth guards and login flow

**Upcoming (Phase 2 - CRUD + Search):**

- DreamEntry entity and REST API
- Full-text search with PostgreSQL (unaccent + trigrams)
- Angular dream list and form components

**Future Phases:**

- Phase 3: Mood tracking and statistics
- Phase 4: AI analysis service with gRPC
- Phase 4.1: Semantic search with pgvector
- Phase 5: PWA with offline support
- Phase 6: Reminders and notifications
- Phase 7: Observability (Prometheus, Grafana)
- Phase 8: Image generation (Stable Diffusion)
- Phase 9-10: Java 25+ features, Kotlin migration
- Phase 11: Experiments (GraphQL, Redis, Kafka)

**When working on tasks:**

- Check which phase the task belongs to
- Follow DoD criteria from README.md
- Update phase status when milestone completed

---

## Additional Resources

**Documentation:**

- API Documentation: http://localhost:8080/swagger-ui.html (when backend running)
- Project Roadmap: [README.md](../README.md)
- Frontend Guidelines: [frontend/.junie/guidelines.md](../frontend/.junie/guidelines.md)

**External References:**

- Spring Boot 3.5: https://docs.spring.io/spring-boot/docs/current/reference/html/
- Angular 20: https://angular.dev
- PostgreSQL 17: https://www.postgresql.org/docs/17/
- Flyway: https://flywaydb.org/documentation/
- Testcontainers: https://www.testcontainers.org/
- Jest: https://jestjs.io/
- Conventional Commits: https://www.conventionalcommits.org/

---

**End of Guidelines - Version 1.0**

*For frontend-specific Angular guidelines, see: [frontend/.junie/guidelines.md](../frontend/.junie/guidelines.md)*
