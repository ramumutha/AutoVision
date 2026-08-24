# PDR-001: Service Profit R1 Revenue-First Strategy

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F001..SP-F015

## Context / Problem

AutoVision needed a bounded product slice that demonstrates dealer value while preserving evidence, tenant authorization, and source-system portability. Repository history shows Service Profit delivered from opportunity foundation through an authenticated responsive manager experience.

## Decision

R1 prioritizes manager-facing service revenue opportunity discovery and review. It includes secure opportunity APIs, commercial summary/queue, deterministic evidence/explanation, data readiness, contextual detail, and safety states. It does not include customer outreach execution or longitudinal management analytics.

## Rationale

The slice connects platform capability to an understandable revenue workflow without requiring speculative automation. It remains demonstrable with synthetic data and exposes uncertainty through evidence, suppression, and review-required semantics.

## Implications

- Revenue potential is shown per currency and must not be combined incorrectly.
- Opportunity-to-contact workflow remains deferred.
- Production ROI, adoption, and market-fit KPIs require research.
- Demo evidence proves behavior, not production outcomes.

## Relationship to the Next Candidate

[PDR-007](PDR-007-bounded-internal-follow-up-disposition.md) defines `SP-F016-A`, a bounded internal follow-up/disposition candidate. It does not change implemented R1 scope, approve a release, or authorize customer outreach. PDR-001 remains the authoritative decision for frozen R1 behavior.

## Related Sources

[Product vision](../strategy/product-vision.md), [release strategy](../strategy/release-strategy.md), [feature register](../roadmap/feature-register.md), [manager UX](../service-profit-manager-ux.md), [demo data research](../research/data-availability-research.md), and [PDR-007](PDR-007-bounded-internal-follow-up-disposition.md).

## Review Trigger

Revisit when production dealer research, measurable outcome evidence, or an approved follow-up workflow changes the release strategy.
