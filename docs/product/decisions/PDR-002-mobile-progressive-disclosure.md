# PDR-002: Mobile Progressive Disclosure

- **Status:** Accepted
- **Date:** Not recorded
- **Related features:** SP-F012, SP-F013

## Context / Problem

The desktop opportunity table and detailed evidence presentation are too dense to remain usable as the primary small-screen workflow.

## Decision

At `720px` and below, present concise semantic opportunity cards and compact Sort & Filter controls. Navigate to a dedicated detail page for full context while preserving list query state.

## Rationale

Cards retain decision-critical information without exposing detailed provenance in the list. Routed detail provides space for the authoritative presentation and supports direct links and browser navigation.

## Implications

Mobile cards show title, type, potential, priority, actionability, and concise evidence. Safety/detail semantics remain in the shared detail component. Responsive and accessibility tests must cover both mobile sizes and document overflow.

## Related Sources

[Manager UX](../service-profit-manager-ux.md), [frontend flow](../../architecture/service-profit-frontend-flow.md), and [ADR-003](../../architecture/decisions/ADR-003-css-responsive-presentation.md).

## Review Trigger

Revisit if validated mobile research, supported device constraints, or workflow telemetry shows the card-to-detail model is ineffective.
