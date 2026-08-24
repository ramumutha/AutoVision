# Database Standards

## Ownership

PostgreSQL is the authoritative platform database. Spring platform migrations live under `platform/src/main/resources/db/migration`; retained Python migrations live under `api/migrations` and must not be assumed to own Spring platform schemas. Confirm ownership through the [repository map](../architecture/repository-map.md) before changing data structures.

## Flyway Immutability

A historical Flyway migration may already have executed in local, shared, or production-like environments. Editing it breaks checksum history and makes environments diverge. Never modify an applied migration. Add the next ordered migration with a descriptive name.

If an earlier migration is wrong, write a forward correction. Record why the correction is safe and how mixed-version deployments behave.

## Migration Design

A migration should be deterministic, reviewable, and safe to apply once. It should:

- qualify schemas explicitly;
- establish ownership, nullability, defaults, constraints, and references deliberately;
- preserve existing data or include an approved transformation;
- avoid environment-specific values and real customer data;
- remain compatible with the deployment sequence.

Destructive operations, bulk rewrites, irreversible type changes, and table/column drops require explicit approval, an impact assessment, backup/rollback strategy, and coordinated application release.

## Tenant Containment

Tenant ownership must be represented in the schema and preserved in foreign keys, uniqueness, queries, and indexes. A globally unique UUID does not replace tenant authorization. Where child data belongs to a tenant-owned aggregate, constraints should prevent cross-tenant relationships when practical.

Repositories must still enforce authenticated scope; schema containment is defense in depth, not the only authorization layer.

## Canonical and Projection Data

Canonical transaction tables and feature-owned read projections have different ownership. Name and document projections so consumers do not mistake snapshots for master data. For example, Service Profit opportunity context can display customer/vehicle information without becoming canonical Customer/Vehicle ownership.

## Constraints and Indexes

Prefer database constraints for durable invariants such as required ownership, legal uniqueness, and valid references. Application validation improves feedback but does not replace integrity constraints.

Add an index only when supported by a query path, foreign-key maintenance need, uniqueness requirement, or measured performance evidence. Consider tenant columns and sort predicates. Every index adds write and maintenance cost; document non-obvious choices in the migration or related ADR.

## PostgreSQL Validation

Validate against PostgreSQL, not an in-memory substitute. Depending on scope:

1. inspect the next Flyway version and schema owner;
2. run `scripts/verify-platform-db-boundary.ps1`;
3. run focused repository/integration tests;
4. apply the migration to a disposable PostgreSQL database or transaction-based validation environment;
5. verify constraints, indexes, and representative query behavior;
6. run platform regression gates.

A rollback can be a forward compensating migration when Flyway history must remain immutable. Do not claim rollback support without testing the actual plan.

## Data Safety

Never commit database passwords, connection tokens, real customer records, production extracts, or identifying data. Fixtures and demo data must be synthetic and clearly labelled. See [demo data governance](../product/demo/demo-data-catalog.md) and [security guidance](../../SECURITY.md).
