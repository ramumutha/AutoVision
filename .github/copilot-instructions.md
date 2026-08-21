# AutoVision Engineering Instructions

Read [README.md](../README.md) and [docs/README.md](../docs/README.md) before
scanning. Then read the relevant architecture or development document for the
requested task and inspect only the bounded module first. Avoid repository-wide
scans unless the change genuinely crosses boundaries.

## Current architecture

- Angular frontend with `core`, `features`, `shared`, and `shell` separation.
- Java/Spring Boot core platform with domain-oriented packages and PostgreSQL.
- Keycloak with OAuth2/OIDC Authorization Code + PKCE and a public SPA client.
- Python/FastAPI retained for AI/ML, prediction, and vision services only.
- Same-origin browser `/api` traffic through the frontend proxy.
- Server-side authorization and tenant isolation are authoritative.

Preserve DMS-neutral canonical contracts and provider portability. Use the
existing modular monolith boundaries; do not introduce microservices or change
architecture for fashion.

## Frozen business boundaries

- Quote, Appointment, ServiceOrder, and Invoice are core DMS transactions.
- Inspection/AI is optional and is not a prerequisite for ServiceOrder.
- AfterSalesCase is durable case context.
- CustomerAuthorization is an auditable business record.
- ServiceOrder, ServiceJob, and ServiceLine naming is frozen.
- ServiceLine belongs to ServiceOrder and may optionally belong to ServiceJob.
- Do not conflate lifecycle timestamps with mechanic time clocking.

## Coding and maintainability

Use meaningful domain-oriented package and folder names, cohesive classes and
components, thin controllers, business-focused services, persistence
repositories, and explicit DTOs/contracts. Extract responsibilities when files
grow materially. Add tests for authorization, validation, and negative paths.
Avoid speculative abstractions, broad refactors, and invented requirements.

## Development workflow

- Inspect before editing and make the smallest scoped change.
- Run focused tests or validation before broad quality gates.
- Use existing scripts under `scripts/dev` and preserve their quiet success output.
- Do not close the terminal from provided PowerShell examples; keep failures visible.
- Do not stage, commit, or push unless explicitly requested; preserve clean Git boundaries.
- Never place secrets in source, documentation, logs, or test fixtures.

## Documentation maintenance

Update the relevant docs when changing architecture boundaries, runtime setup,
security/identity behavior, developer workflow, or repository ownership. Trivial
internal refactors that do not change a contract do not require documentation.