# ADR-002: Projection Versus Canonical Ownership

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** SP-F008, PDR-005

## Context

Service Profit detail needs contextual customer, vehicle, and service display data, but creating canonical master-data domains is outside the established R1 boundary.

## Decision

Service Profit owns a tenant-contained read projection for its detail use case. Canonical transaction/domain references remain authoritative and projection fields are not promoted to master data.

## Rationale

This enables useful context without premature ownership claims and allows future canonical domains to replace projection sources behind a stable detail contract.

## Consequences

Partial/null context is valid. Projection lineage and tenant containment must be preserved. Consumers cannot use the projection as a write model or cross-feature master source.

## Alternatives Considered

Creating canonical Customer/Vehicle domains during the Service Profit slice and omitting context entirely were rejected as respectively premature and insufficient for the product need.

## Related Sources

[System overview](../system-overview.md), PDR-005, context schema/services/tests.

## Review Trigger

Approval of canonical Customer/Vehicle ownership or new consistency requirements.
