# PDR-006: Do Not Fabricate Current Business Context

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F009, SP-F018

## Context / Problem

A shell can appear richer by displaying inferred role, branch, or location, but the current authenticated contract does not provide authoritative current-business context for those labels.

## Decision

Display only authenticated identity/context supplied by authoritative contracts. Do not infer or fabricate role, branch, location, or permissions for UI decoration or demo convenience.

## Rationale

Fabricated context can mislead users, diverge from server authorization, and become a security or trust defect. Honest absence is preferable to unsupported specificity.

## Implications

Role/location UI remains deferred until identity/organization ownership and authorization semantics are defined. Demo setup must not weaken JWT or tenant behavior.

## Related Sources

[Authentication and tenant context](../../architecture/authentication-and-tenant-context.md), [manager UX](../service-profit-manager-ux.md), and [ADR-001](../../architecture/decisions/ADR-001-server-authoritative-tenant-authorization.md).

## Review Trigger

Revisit when a server-authoritative business-context contract and product need are approved.
