---
description: "Use when changing PostgreSQL schemas, Flyway migrations, SQL, persistence constraints, indexes, or database validation."
applyTo: "platform/src/main/resources/db/migration/**, api/migrations/**, **/*.sql"
---
# Database Instructions

- Never edit a historical Flyway migration that may have run. Add the next ordered migration.
- Preserve tenant containment in keys, constraints, queries, and indexes. Enforce referential integrity where ownership is known.
- Add indexes only for an evidenced query or integrity need; document non-obvious choices.
- Do not drop, truncate, rewrite, or make an incompatible schema change without explicit approval and a migration/rollback plan.
- Keep canonical domain tables separate from feature-owned read projections.
- Validate migrations against PostgreSQL and run the repository database-boundary checks. Do not rely on an alternate database dialect as proof.
- Never put real credentials, customer data, or production extracts in migrations or fixtures. See [database standards](../../docs/engineering/database-standards.md).
