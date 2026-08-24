# ADR-006: Flyway Migration Immutability

- **Status:** Accepted
- **Date:** Not recorded
- **Related features/PDRs:** All platform schema features

## Context

Flyway records applied migration versions and checksums. Editing historical files causes environment divergence and invalidates repeatable deployment evidence.

## Decision

Never modify an applied historical Flyway migration. Correct schema or data through the next ordered forward migration. Destructive/incompatible changes require explicit approval and a deployment/rollback plan.

## Rationale

Immutable history keeps local, shared, and deployed databases reproducible and auditable.

## Consequences

Reviewers must verify the next version, ownership, compatibility, tenant constraints, and PostgreSQL behavior. Rollback may require a tested compensating migration rather than history edits.

## Alternatives Considered

Rewriting old migrations was rejected because it only works for empty databases and breaks existing environments.

## Related Sources

[Database standards](../../engineering/database-standards.md), platform migration directory, and database-boundary validation script.

## Review Trigger

A formally approved migration-tool or deployment-strategy replacement.
