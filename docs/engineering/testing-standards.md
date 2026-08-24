# Testing Standards

## Purpose

Tests provide executable evidence that product behavior, contracts, security, and workflows remain intact. Test at the responsibility that owns the behavior, then use broader gates to detect integration regressions.

## Validation Sequence

1. Run the narrowest test that can disprove the implementation hypothesis.
2. Run the owning module or feature suite.
3. Run applicable full regression and production build gates.
4. Run database, responsive, accessibility, or E2E checks when the change crosses those concerns.
5. Run `git diff --check` and review changed files.

Use repository commands, including:

- Angular unit tests: `npm test` from `frontend/`;
- Angular production build: `npm run build`;
- configured Playwright scripts in `frontend/package.json`;
- platform validation: `scripts/dev/validate-platform.ps1`;
- database boundary validation: `scripts/verify-platform-db-boundary.ps1`.

Do not invoke bare Vitest; Angular's configured test command supplies the expected environment.

## Behavior Coverage

Select cases according to risk rather than writing a fixed number of tests.

| Concern | Expected evidence when relevant |
| --- | --- |
| Success | Requested outcome and persisted/rendered contract |
| Validation | Required, malformed, boundary, unsupported, and oversized input |
| Security | Unauthenticated, unauthorized, cross-tenant, inactive/malformed identity |
| Failure | Not found, conflict, dependency failure, safe retry/error behavior |
| Data shape | Null, empty, partial, multiple currencies, optional context |
| Asynchrony | Cancellation, stale response, idempotency, concurrency, retry |
| UX | Semantics, keyboard/focus, loading, empty/error states, URL restoration |
| Responsive | Intended mobile/tablet/desktop presentation and no document overflow |

Do not assert private implementation details when a public outcome is available. Contract and semantic assertions are more durable than CSS-order or call-count assertions without behavioral meaning.

## Responsibility-Based Test Design

- Component tests own rendering, accessibility semantics, focus, and emitted intents.
- Container/page tests own orchestration, routing, request state, and cancellation.
- Service tests own business policy and authorization decisions.
- Repository tests own persistence, constraints, and tenant-scoped queries.
- API tests own status codes, validation, serialization, and safe errors.
- E2E tests own a small number of critical integrated workflows.

When a new child component owns behavior, add its own spec instead of continuously growing a manager/container spec.

## Fixtures and Test Data

Fixtures must be deterministic, synthetic, minimal, and purpose-labelled. Never use secrets, real tokens, live customer data, or production extracts. Keep fixture builders close to their ownership and avoid one global fixture that couples unrelated suites.

A demo fixture may validate a demo story, but production behavior must not special-case that fixture. Tests must not disable authorization, tenant isolation, validation, or typing to become green.

## File Maintainability

Test files should preferably remain below 500 lines. Above 500 lines, review whether scenarios belong to separate responsibilities or fixture helpers. Any test file above 700 lines must be decomposed before freeze unless it is generated and reviewed separately.

Splitting by arbitrary line ranges is not useful. Split by behavior such as routing, authorization, responsive presentation, persistence, or worker concurrency.

## Accessibility and Responsive Testing

Automated accessibility scans detect important classes of defects but do not replace keyboard and screen-reader reasoning. Test native semantics, accessible names, focus movement, disclosure state, and status announcements in focused specs. Use Playwright/Axe on critical workflows.

Responsive tests should set explicit viewports, assert which workflow is primary, and check document overflow. Component CSS alone is not proof that a user can complete the workflow.

## Reporting

Completion reports should state exact commands and results, warnings, skipped gates, residual risks, and test-file sizes. Never describe a test as passing if it was not run in the current environment.

See the [validation and Git workflow](../development/validation-and-git-workflow.md) for operational sequencing.
