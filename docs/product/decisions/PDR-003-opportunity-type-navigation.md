# PDR-003: Opportunity Type as Primary Navigation

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F010

## Context / Problem

Managers need a predictable way to move among declined, deferred, lifecycle, and inactive-customer opportunity categories without treating the first queue page as the full dataset.

## Decision

Opportunity Type is the primary single-select category navigation. Priority and Actionability remain secondary filters; Sort controls ordering. Counts come only from the authoritative summary API.

## Rationale

Opportunity type reflects the business reason for attention and remains understandable across responsive layouts. Server counts avoid misleading client-derived totals.

## Implications

Selection is URL-backed, uses `aria-pressed`, and remains separate from mobile Sort & Filter. Adding or renaming types requires contract, localization, policy, UX, and test review.

## Related Sources

[Manager UX](../service-profit-manager-ux.md), [frontend flow](../../architecture/service-profit-frontend-flow.md), and [ADR-005](../../architecture/decisions/ADR-005-url-backed-service-profit-state.md).

## Review Trigger

Revisit when dealer research demonstrates a different primary mental model or opportunity taxonomy changes materially.
