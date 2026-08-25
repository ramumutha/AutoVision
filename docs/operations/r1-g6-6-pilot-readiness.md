# R1 G6.6 Pilot Readiness Gate

**Status:** IMPLEMENTED fixture-based validation evidence; not a production
readiness or ROI claim.

## Validation Evidence

The synthetic first-partner fixture at
`platform/src/test/resources/intake/g6-6-first-partner` has 22 records and one
intentional invalid cost relationship. It verifies file adaptation, record
validation, record quarantine, canonical mapping, partial capability treatment,
and safe source lineage with the current production classes.

| Gate | Result |
| --- | --- |
| Contract, required identities, tenant/dealer containment | Required before partner receipt; test fixture validates contract-shaped IDs and relationships. |
| Mixed valid and quarantined records | PASS: record-scoped errors quarantine the unsafe record while valid records remain eligible. |
| Missing disposition | PASS: maps to `UNKNOWN`, never `DECLINED`. |
| Missing cost and invoice | PASS: capability policy remains partial; no margin or recovered-revenue claim is created. |
| Duplicate/replay, failure recovery, reconciliation | Covered by existing G6.5 integration tests; rerun is required for each pilot candidate. |
| No unsafe PII in operational output | Existing persistence/reconciliation tests verify payload omission from ordinary representations. |
| Disposition source lineage | PASS: eligible disposition source IDs are persisted in the tenant-contained Service Profit opportunity evidence child table; missing, quarantined, and unrelated dispositions are excluded. |

## Acceptance Criteria

Data: the contract is accepted, containment and identities validate, quarantine
is reviewed, and the capability report is explicitly accepted by the dealer.

Functional: expected opportunity classes and safe suppression/review behavior
are validated against an agreed manifest; quarantined evidence creates no
opportunity; duplicate FULL delivery is a no-op.

Operational: reconciliation is consistent, the runbook is exercised, and a
materialization failure/replay plus abandoned-materializing recovery are
evidenced.

Security and privacy: authorized handling, retention/deletion expectations,
and no PII/raw payload in operational evidence are confirmed.

Business: a manager can review opportunities; Recoverable Potential remains
opportunity value, not recovered revenue; baseline measurements are captured
without claiming production ROI.

## Pilot KPI Availability

| KPI | Availability |
| --- | --- |
| Opportunities identified; high priority; review required; ready to action; suppressed; recoverable potential | AVAILABLE NOW |
| Opportunities reviewed; opportunities actioned; time-to-action | REQUIRES SP-F016-A |
| Customer contacted; customer accepted; work booked; work completed | REQUIRES DEALER OUTCOME DATA |
| Revenue invoiced | DERIVABLE when invoice linkage is validated |
| Attributable recovered revenue; conversion rate | REQUIRES DEALER OUTCOME DATA |
| Gross profit | DERIVABLE only with validated invoice and cost linkage |

## Classification

- **IMPLEMENTED HARDENING:** disposition-level source lineage is persisted in
  the Service Profit-owned opportunity evidence child table. The evidence
  projection is intentionally not added to the public opportunity response in
  this slice.
- **FIRST-PARTNER CONFIGURATION:** confirm source mapping, identity keys,
  currencies, access, retention, and a category-level expected-opportunity
  manifest with the partner.
- **PRODUCTION HARDENING:** provider validation, authorized transfer,
  privacy/legal review, retention/deletion execution, and representative-scale
  performance evidence.
- **NON-BLOCKING IMPROVEMENT:** fixture expansion with an agreed outcome
  manifest and local performance comparison.
- **FUTURE FEATURE:** DELTA, provider adapters, REST upload, SFTP, customer
  outreach, Search, and SP-F016-A.

The [controlled intake operator runbook](controlled-dealer-data-intake-operator-runbook.md)
remains the operational procedure. This gate does not change AMBER governance
for readiness or gross-profit attribution.