# ADR-001: Server-Authoritative Tenant Isolation and Authorization

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** SP-F002, SP-F009, PDR-006

## Context

AutoVision is multi-tenant. Angular guards improve navigation but browser state and identifiers are user-controlled. Keycloak identity maps through a managed UserRef to platform tenant context.

## Decision

The Spring platform derives tenant context from authenticated server claims/mapping and enforces authorization for every tenant-owned read and mutation. Client guards and tenant parameters are never the authoritative boundary.

## Rationale

Server enforcement prevents direct-object and cross-tenant access that UI-only controls cannot stop. Managed mapping decouples product identity from DMS/UI details.

## Consequences

Services/repositories need tenant-constrained paths and security tests. Missing, malformed, inactive, or unauthorized identities fail safely. Demo automation must preserve JWT validation.

## Alternatives Considered

Client-only filtering and trusted tenant query parameters were rejected because they are not security boundaries.

## Related Sources

[Authentication and tenant context](../authentication-and-tenant-context.md), platform security/tenant code, and access integration tests.

## Review Trigger

Revisit only if the identity/tenant model changes; server authority remains an invariant.
