# PDR-005: Service Profit Context Projection

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F008

## Context / Problem

Opportunity review needs customer, vehicle, and originating-service display context, while canonical Customer and Vehicle master-data ownership is not established in the current platform.

## Decision

Store and expose a tenant-contained, read-only context snapshot owned by Service Profit. Do not represent it as canonical Customer or Vehicle data and do not replace ServiceOrder, ServiceJob, or ServiceLine references.

## Rationale

The projection supports actionable R1 detail without prematurely defining master-data domains. It keeps the contract stable while allowing future canonical sources to enrich or replace projection inputs.

## Implications

Context can be partial or absent and must render safely. Tenant containment and source evidence remain required. Canonical-domain introduction requires migration and architecture review.

## Related Sources

[System overview](../../architecture/system-overview.md), [ADR-002](../../architecture/decisions/ADR-002-projection-vs-canonical-ownership.md), and Service Profit context implementation/tests.

## Review Trigger

Revisit when canonical Customer or Vehicle ownership is approved or projection staleness/consistency requirements change.
