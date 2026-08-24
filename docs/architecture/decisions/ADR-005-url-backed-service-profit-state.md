# ADR-005: URL-Backed Service Profit State

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** SP-F010, SP-F012, SP-F013, PDR-003, PDR-004

## Context

Managers expect filters and sorting to survive refresh, direct links, Back, and Forward. Mobile detail navigation must restore the list state without relying on a still-alive manager component.

## Decision

Store supported opportunity type, priority, actionability, and sort values in query parameters. Validate values against allow-lists and use safe defaults. Carry the state into mobile detail links and Back fallback.

## Rationale

The URL provides durable, inspectable navigation state and native browser behavior. It avoids hidden singleton state and makes direct links meaningful.

## Consequences

Query values are public browser state and must not contain sensitive/tenant context. Router changes drive cancellable API requests. Defaults and compatibility require tests.

## Alternatives Considered

Component-only and global in-memory state were rejected because refresh/direct navigation loses them.

## Related Sources

[Frontend flow](../service-profit-frontend-flow.md), manager/page tests, and responsive E2E tests.

## Review Trigger

State becomes too large/sensitive for URLs or a new workflow requires persisted user preferences with an explicit contract.
