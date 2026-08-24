# Backend and API Standards

## Runtime Boundaries

The Java/Spring Boot platform under `platform/` is the core transactional and domain backend. Python/FastAPI under `api/` remains bounded to AI/ML, prediction, vision, and retained support capabilities. Do not move core DMS transactions into Python or split the modular platform into services without an approved architecture decision.

## Layer Responsibilities

### Controllers

Controllers translate HTTP input into validated requests and map service outcomes to stable responses. They should not contain persistence queries, tenant filtering, policy decisions, or complex assembly logic.

### Domain and application services

Services own business decisions, workflow coordination, authorization checks, and transaction boundaries. Name them for the domain capability they provide. Keep policy behavior deterministic where possible and explicit about authoritative evidence.

### Repositories

Repositories own persistence access and tenant-constrained queries. They do not decide user-facing policy. Query methods should make ownership and scope clear; avoid generic unrestricted finders for tenant data.

### DTOs and contracts

Use explicit request and response DTOs rather than exposing persistence entities. Contracts should remain DMS-neutral and portable across providers. Add fields compatibly where possible; do not silently change meaning, enum semantics, nullability, or currency behavior.

## Canonical Domains and Projections

A canonical domain owns lifecycle and business truth. A projection is a feature-owned read model assembled for a bounded use case.

Service Profit customer, vehicle, and originating-service context is currently a tenant-contained read projection. It supports opportunity explanation but is not canonical Customer or Vehicle master data. A future ownership change requires an ADR, migration plan, and contract review.

## Tenant Isolation and Authorization

Every backend read and mutation involving tenant data must derive scope from authenticated server context and enforce it in service/repository behavior. Never trust a browser-provided tenant ID or rely on an Angular route guard.

Tests should prove:

- a valid tenant can access its permitted record;
- another tenant cannot access it;
- missing, malformed, inactive, or unauthorized identity is rejected safely;
- direct object identifiers do not bypass containment.

See [authentication and tenant context](../architecture/authentication-and-tenant-context.md).

## Validation and State

Validate identifiers, required fields, formats, ranges, enum values, optimistic versions, and legal state transitions at the correct boundary. Reject invalid requests before side effects. Distinguish not found, unauthorized, validation, conflict, and unexpected failure without exposing implementation detail.

Idempotency and concurrency behavior must be explicit for commands that can be retried or processed by workers. Use database and transactional constraints as the final integrity boundary.

## API Evolution

Backward-compatible evolution is the default:

- add optional response fields rather than changing existing meaning;
- introduce new endpoints or versions for incompatible workflows;
- preserve status-code and error-contract semantics;
- document deprecation and migration windows;
- update consumers, contract tests, and API docs together.

Breaking changes require explicit product and architecture approval.

## Logging and Observability

Use structured, correlation-aware diagnostics. Log stable identifiers only when policy permits. Never log passwords, tokens, cookies, client secrets, raw authorization headers, or sensitive customer payloads. User-facing errors must be safe; detailed causes remain in controlled server diagnostics.

## Testing Expectations

Test controllers for contract/validation mapping, services for business and authorization behavior, repositories for tenant containment and persistence semantics, and integration points for actual configuration. Include success, negative, null/partial data, conflict, and security cases.

Prefer focused tests first, then the repository platform validation scripts. Apply the [coding-standard size guidance](coding-standards.md#maintainability-guidance): Java controllers/services should preferably remain below 300 lines, with review above 500 and strong decomposition above 700.
