# AutoVision Product Vision

## Vision

AutoVision is an adaptive vehicle service intelligence platform that helps vehicle-service organizations turn trustworthy operational evidence into safer, more effective decisions and workflows.

The current product direction combines core DMS transactions, tenant-aware platform services, and optional intelligence. AI/inspection capability can enrich work but is not a prerequisite for a ServiceOrder. Product experiences must preserve source evidence, authorization, and auditability rather than hide uncertainty behind automation.

## Product Principles

1. **Dealer value before novelty.** Prioritize measurable service outcomes and workflow clarity over technology demonstrations.
2. **Evidence before action.** Explain why an opportunity or recommendation exists and identify evidence strength, suppression, and required review.
3. **Human accountability.** AutoVision supports decisions; it does not fabricate customer context or bypass authorized human review.
4. **Portable domain contracts.** Keep DMS, OEM, market, and provider-specific details behind integration boundaries.
5. **Secure multi-tenancy.** Server-side authorization and tenant containment are non-negotiable product behavior.
6. **Progressive delivery.** Release bounded, demonstrable slices with backward-compatible contracts and explicit limitations.

## Current R1 Product Focus

**IMPLEMENTED:** Service Profit R1 is the best-evidenced current product slice. It identifies service revenue opportunities, provides currency-aware manager summaries, supports opportunity-type/filter/sort navigation, explains evidence and recommended action, and protects suppressed or review-required cases. Desktop/tablet use inline detail; mobile uses concise cards and routed detail.

The authoritative functional behavior is in [Service Profit Manager UX](../service-profit-manager-ux.md). Technical boundaries are in [system overview](../../architecture/system-overview.md) and [Service Profit frontend flow](../../architecture/service-profit-frontend-flow.md).

## Intended Users

Repository evidence directly supports a dealership manager experience. Other possible users, roles, and organizational contexts require product research before being treated as committed personas. The shell must not fabricate role or location context that the authenticated contract does not provide.

## Outcome Direction

AutoVision should help teams:

- find credible service opportunities;
- understand commercial potential without combining currencies incorrectly;
- review evidence before customer action;
- avoid action on suppressed or ambiguous cases;
- operate through authenticated, tenant-contained workflows.

Production KPI targets, market sizing, customer willingness to pay, and comparative benchmarks are **RESEARCH REQUIRED**. See [market strategy](market-strategy.md), [research index](../research/research-index.md), and the [feature register](../roadmap/feature-register.md).
