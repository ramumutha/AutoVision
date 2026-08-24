# ADR-007: Controlled Dealer Data Intake Boundary

- **Status:** Accepted for G6.1 architecture/contract foundation; implementation deferred
- **Date:** 2026-08-24
- **Owners:** Architecture, Service Profit, data operations, security/privacy reviewers
- **Related features/PDRs:** SP-F004; future controlled-intake feature linkage; PDR-001

## Context

Service Profit currently proves deterministic detection and capability disclosure
against a `SYNTHETIC_DEMO_ONLY` dataset. Production dealer/DMS evidence is not
validated, and no provider-specific support is approved. The product must accept
future controlled file, REST/API, SFTP/batch, and provider-specific adapters
without coupling detection semantics to one source layout. `SP-F016-A` concerns
future internal follow-up/disposition and is outside this intake boundary.

## Decision

Adopt a provider-neutral pipeline of external source, intake adapter, canonical
staging/source envelope, mapping and validation, capability assessment,
validated canonical detection input, Service Profit detection, and opportunity
persistence. Adapters produce the versioned contract in
[`AV-R1-CDF-001`](../../../contracts/r1-controlled-dealer-file-contract.md).

R1 defines a controlled `FULL` file contract first. It does not implement an
adapter, staging persistence, mapping runtime, delta processing, or partner
integration. Tenant/dealer/location containment and source lineage are
authoritative boundaries. Readiness is capability-specific and missing data
degrades or quarantines safely; it never becomes an unsafe business assertion.

## Rationale

This preserves the existing DMS-neutral integration boundary and keeps Service
Profit detection semantics independent of delivery mechanism. A FULL snapshot
is the smallest replayable first-partner contract and provides reconciliation
before the operational complexity of DELTA. The envelope and lineage fields
leave room for future adapters without pretending that provider readiness exists.

## Consequences

- The first partner process can remain operator-controlled; no self-service UI,
  SFTP, or API implementation is required for G6.1.
- Canonical staging and quarantine become required future persistence concerns.
- Capability reports must distinguish detection, revenue, cost/gross-profit,
  disposition, identity, mileage, and contactability.
- PII minimization, redaction, retention/deletion, authorization, integrity,
  and legal/privacy review are release gates for partner intake.
- Synthetic demo data remains demo-profile input and cannot be represented as
  production readiness evidence.

## Alternatives Considered

- **Make the synthetic CSV canonical:** rejected because it leaks demo shape
  into production and does not cover the canonical business boundary.
- **Build a DMS-specific adapter first:** rejected because provider evidence is
  absent and detection semantics must remain portable.
- **Use one readiness score:** rejected because missing cost, invoice,
  disposition, identity, and mileage affect different capabilities.
- **Start with DELTA:** rejected for R1 because replay, tombstones, ordering,
  and reconciliation are not yet governed.

## Related Sources

- [R1 controlled dealer file contract](../../../contracts/r1-controlled-dealer-file-contract.md)
- [System overview](../system-overview.md)
- [Data availability research](../../product/research/data-availability-research.md)
- [Feature register](../../product/roadmap/feature-register.md)
- [Release register](../../product/roadmap/release-register.md)

## Review Trigger

Reopen when a first partner, provider, market, legal/privacy requirement,
staging design, delta requirement, or validated production data evidence changes
the contract boundary or capability policy.