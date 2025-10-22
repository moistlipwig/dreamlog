# Review Change

Authoritative checklist for `/review` command. Applies to every change request.

## Scope

- Always diff against the source branch (typically `main`). Do **not** limit the review to staged files or excerpts supplied by the user.
- Inspect the **complete** change: Java/Groovy/TypeScript/HTML/SQL/config/test files, build scripts, generated assets that are under version control.
- Validate that migrations, scripts, and configuration stay compatible with existing environments (local, CI, production).

## Review Process

1. **Recon** – Pull the full diff; list modules, packages, and features touched.
2. **Architecture sanity** – Check that the change preserves clean architecture boundaries (presentation/application/domain/infrastructure) and Domain-Driven Design concepts (entities, aggregates, value objects, repositories, bounded contexts).
3. **Code quality** – Enforce SOLID, DRY, KISS, YAGNI, and clear naming. Reject code smells (god classes, primitive obsession, cyc dependencies, feature envy, duplicated logic).
4. **Security & auth** – Evaluate authentication, authorization, CSRF, XSS, SQL injection, secrets handling, and transport security.
5. **Persistence & migrations** – Ensure Flyway/DDL changes are reversible, idempotent, and tested. Verify indexes, constraints, and data integrity.
6. **Testing** – Confirm coverage for critical paths. Flag missing or brittle tests. Require integration tests when behaviour spans multiple layers.
7. **DX & automation** – Build scripts, Gradle/Node toolchains, and CI configs must keep the build reproducible. Highlight regressions (e.g. bumping Java version without toolchain support).
8. **Documentation** – README, ADRs, inline comments, and API docs should reflect the change. Require updates when behaviour or contracts change.

## Blocking Issues

Raise **blocking** findings when any of the following occur:

- Violations of clean code / clean architecture / DDD fundamentals.
- Behavioral regressions or broken use cases.
- Security vulnerabilities.
- Database migrations that fail on a clean environment or risk data loss.
- Missing automated tests for new logic or regressions.
- Toolchain changes that break the build on supported environments.

Minor issues (style, typos, non-critical optimisations) may be noted but mark them explicitly as non-blocking.

## Deliverable

Structure feedback as:

1. **Findings** – Ordered by severity, with `file:path:line` references. Describe the impact and required fix. Include architectural reasoning where relevant.
2. **Open questions / assumptions** – Clarify uncertainties.
3. **Change summary** – Optional, brief overview once findings are listed.
4. **Next steps** – Tests to run, docs to update, or follow-up tasks.

Always educate: explain the principle being enforced (Clean Code, DDD tactic, security best practice) so the author understands the “why”, not just the “what”.
