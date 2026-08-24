# ADR-004: Single Service Profit Detail Presentation

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** SP-F008, SP-F011..SP-F015, PDR-004

## Context

Desktop/tablet inline detail and mobile routed detail must present identical customer, vehicle, service, explanation, evidence, suppression, review-required, and provenance semantics.

## Decision

`ServiceProfitOpportunityDetailComponent` is the one authoritative dealer-facing detail presentation. Containers/pages own loading, routing, selection, and errors, then pass the typed detail response to it.

## Rationale

A single display component prevents safety and explanation drift across responsive workflows while keeping orchestration responsibilities separate.

## Consequences

New detail fields and safeguards are implemented/tested once. Routed and inline wrappers must not duplicate markup or fetch inside the presentation.

## Alternatives Considered

Separate desktop/mobile detail templates were rejected because they would duplicate business-critical semantics.

## Related Sources

[Frontend flow](../service-profit-frontend-flow.md), [manager UX](../../product/service-profit-manager-ux.md), and detail component tests.

## Review Trigger

A genuinely different product workflow requires different semantics, not merely a different viewport layout.
