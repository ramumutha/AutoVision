# AutoVision Coding Standards

## Purpose

These standards define how AutoVision code remains understandable, secure, and changeable for engineers at every experience level. They apply across the Angular application, Spring platform, PostgreSQL schema, and retained Python AI/ML services. Framework-specific guidance is linked below.

## Core Principles

### Inspect and bound the change

Start from the owning module, failing behavior, contract, or test. Identify what is in scope and what is not. A small, explicit boundary reduces regressions and protects concurrent user work.

Never discard uncommitted work that you did not create. Staging, committing, pushing, resetting, and stashing require explicit instruction.

### Name by domain responsibility

Prefer names such as `ServiceProfitOpportunityAccessService` and `CustomerAuthorizationRepository` over generic names such as `Manager`, `Helper`, or `DataService`. A name should tell a reader what business responsibility the code owns.

Preserve established terms: ServiceOrder, ServiceJob, and ServiceLine are distinct concepts. Do not invent aliases that blur frozen domain boundaries.

### Keep one cohesive reason to change

A class or component may coordinate several collaborators when that is its responsibility, but it should not also own unrelated presentation, persistence, and policy decisions. Extract a responsibility when it can be named clearly and tested independently.

Reuse removes real duplication. It should not introduce a generic framework before two or more proven use cases establish a stable abstraction.

### Preserve contracts and security

- Keep APIs, events, and persisted contracts backward-compatible by default.
- Treat tenant isolation and server-side authorization as invariants.
- Never expose secrets, bearer tokens, credentials, cookies, or sensitive payloads in code, logs, documentation, fixtures, or error responses.
- Never weaken authentication, authorization, validation, or tenant checks to make a test pass.
- Keep demo-only behavior outside production execution paths.

## Maintainability Guidance

File size is a review signal, not a substitute for design judgment. Generated files are assessed separately.

| File type | Preferred | Review | Strong decomposition | Maximum |
| --- | ---: | ---: | ---: | ---: |
| Angular TypeScript | `<300` | `>500` | `>700` | `2000` production lines |
| Angular HTML | `<250` | `>500` | `>700` | `2000` production lines |
| SCSS/CSS | `<300` | `>500` | `>700` | `2000` production lines |
| Java service/controller | `<300` | `>500` | `>700` | `2000` production lines |
| Other production source | responsibility-based | `>500` | `>700` | `2000` production lines |
| Tests | preferably `<500` | `>500` | decompose `>700` | no arbitrary compiler limit |

A production file over 500 lines requires an explicit cohesion review. Above 700 lines, decomposition is strongly preferred and should be declined only with a documented reason. A production file over 2000 lines fails review.

Useful decomposition boundaries include orchestration versus presentation, transport versus domain policy, command versus query behavior, and reusable fixture builders versus scenario assertions. Do not split code only to satisfy a number if the result obscures ownership.

## Dependencies and Abstractions

Use the repository's existing framework and helper APIs before adding a dependency. A new dependency needs:

- a concrete capability the current toolchain cannot reasonably provide;
- active maintenance and acceptable licensing/security posture;
- a bounded integration point;
- tests and documentation for operational impact.

Avoid wrappers that merely rename framework APIs. Add an abstraction when it protects a domain contract, centralizes a cross-cutting invariant, or removes meaningful duplication.

### Repository-pinned technology

Checked-in manifests, lockfiles, build files, container definitions, and established implementation patterns define the adopted technology versions. Do not select a latest release or perform a framework, runtime, library, Carbon, database, Node.js, Maven, or Docker upgrade as incidental feature work. An upgrade is a separate maintenance or architecture task and requires a compatibility assessment, migration impact, security assessment, test evidence, and rollback strategy. A bounded security remediation may change a dependency when the risk and validation are explicit.

Before creating a new entity, DTO, controller, service, repository, mapper, validator, exception, UI component, utility, migration, test helper, Docker construct, or configuration mechanism, search for the nearest equivalent. Prefer reuse, extension, and composition in that order; introduce a new abstraction only when the existing boundary cannot express the requirement without harming cohesion or correctness.

## Comments and Errors

Code should explain the normal path through names and structure. Comments are appropriate for non-obvious invariants, compatibility constraints, or reasoning that cannot be encoded in types. Do not narrate individual assignments.

Errors exposed to users or clients should be safe, actionable, and stable. Internal causes belong in controlled, correlation-aware diagnostics without sensitive data.

## Definition of Done

A change is complete when:

1. the requested behavior and explicit exclusions are satisfied;
2. focused tests pass, followed by appropriate regression/build gates;
3. authorization, tenant, validation, null, partial-data, accessibility, localization, and responsive impacts were considered;
4. contracts remain compatible or an approved migration is documented;
5. relevant product, architecture, workflow, or runbook documentation is synchronized;
6. file sizes and accidental duplication were reviewed;
7. `git diff --check` passes and every changed path is reported.

## Related Standards

- [Frontend standards](frontend-standards.md)
- [Backend and API standards](backend-api-standards.md)
- [Database standards](database-standards.md)
- [Testing standards](testing-standards.md)
- [Documentation standards](documentation-standards.md)
