# PDR-004: Responsive Opportunity Detail Pattern

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F011, SP-F012, SP-F013

## Context / Problem

Managers need queue context while reviewing detail on large screens, but mobile requires a focused full-width experience.

## Decision

Desktop and tablet insert one inline detail immediately after the selected table row. Mobile cards navigate to a guarded routed page. Both use the same dealer detail presentation.

## Rationale

Inline detail minimizes context loss during high-density scanning. Routed mobile detail avoids compressed tables and nested panels. Shared presentation prevents semantic drift.

## Implications

Selection/loading/error feedback remains adjacent on desktop/tablet. Mobile Back restores URL state. One expanded desktop row and stale-request cancellation are required. See [ADR-004](../../architecture/decisions/ADR-004-single-service-profit-detail-presentation.md).

## Related Sources

[Manager UX](../service-profit-manager-ux.md) and [frontend flow](../../architecture/service-profit-frontend-flow.md).

## Review Trigger

Revisit if user research or workflow telemetry supports a materially different detail interaction.
