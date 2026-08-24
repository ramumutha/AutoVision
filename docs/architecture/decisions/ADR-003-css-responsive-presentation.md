# ADR-003: CSS-Owned Responsive Presentation

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** SP-F011..SP-F013, PDR-002, PDR-004

## Context

Service Profit needs distinct mobile and table workflows, but presentation switching does not require business logic or runtime viewport state.

## Decision

Use component-scoped CSS media queries for presentation visibility and layout. Do not introduce `BreakpointObserver`, `matchMedia`, or a viewport service unless behavior cannot be expressed safely through CSS.

## Rationale

CSS keeps presentation declarative, avoids duplicate runtime state, supports server/test simplicity, and preserves the same authoritative data/state orchestration.

## Consequences

Both semantic presentations may exist in the DOM component tree while CSS selects the intended viewport experience. Automated responsive tests verify visibility and overflow at explicit sizes.

## Alternatives Considered

A shared viewport service was rejected as unnecessary state and dependency for the current need.

## Related Sources

[Frontend flow](../service-profit-frontend-flow.md), [frontend standards](../../engineering/frontend-standards.md), and responsive E2E tests.

## Review Trigger

A future interaction requires viewport-dependent data loading or behavior that CSS cannot own without accessibility/performance harm.
