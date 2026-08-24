# Service Profit R1 Demo Data Catalog

## Dataset

| Field | Value |
| --- | --- |
| Dataset ID | `AUTOVISION-SERVICE-PROFIT-R1-DEMO` |
| Version | `1.0.0` |
| Classification | `SYNTHETIC_DEMO_ONLY` |
| Market | `IN` |
| Default currency | `INR` |
| Policy version | `R1-DATA-READINESS-1` |
| Source | [`demo-data/service-profit/r1`](../../../demo-data/service-profit/r1/) |

The dataset is opt-in through the Service Profit demo compose override and is mounted read-only. It must never be represented as real dealer/customer data or production performance evidence.

## Source Files

Synthetic source files cover organization, customers, vehicles, repair orders, service jobs, recommendations, advisor notes, invoices/lines, costs, and service history. `manifest.json` defines expected coverage, capability, and scenario outcomes; `validation/validate-dataset.ps1` checks dataset consistency.

## Scenarios

| ID | Scenario | Opportunity type | Evidence | Actionability/outcome | Commercial expectation |
| --- | --- | --- | --- | --- | --- |
| SP-DEMO-001 | Explicit declined brake work | DECLINED_WORK | SOURCE_CONFIRMED / STRONG | READY opportunity | INR 12,400 potential |
| SP-DEMO-002 | Deferred tyre replacement | DEFERRED_WORK | SOURCE_CONFIRMED / STRONG | READY opportunity | INR 28,000 potential |
| SP-DEMO-003 | Note-derived declined brake work | DECLINED_WORK | EVIDENCE_DERIVED / STRONG | READY opportunity | INR 9,800 potential |
| SP-DEMO-004 | Declined work subsequently completed | DECLINED_WORK | SOURCE_CONFIRMED / STRONG | SUPPRESSED / already completed | INR 8,500 recorded potential; do not action |
| SP-DEMO-005 | Due scheduled service | DUE_SERVICE | POLICY_DERIVED / STRONG | READY opportunity | Amount not asserted in manifest |
| SP-DEMO-006 | Overdue scheduled service | OVERDUE_SERVICE | POLICY_DERIVED / STRONG | READY opportunity | Amount not asserted in manifest |
| SP-DEMO-007 | Inactive customer | INACTIVE_CUSTOMER | POLICY_DERIVED / MODERATE | READY opportunity | Amount not asserted in manifest |
| SP-DEMO-008 | Partial conversion | DECLINED_WORK | SOURCE_CONFIRMED / STRONG | PARTIAL_CONVERSION | INR 6,200 potential; INR 4,000 converted |
| SP-DEMO-009 | Revenue with incomplete cost | DECLINED_WORK | SOURCE_CONFIRMED / STRONG | REVENUE_ONLY | INR 14,500 potential/converted |
| SP-DEMO-010 | Ambiguous customer identity | DECLINED_WORK | EVIDENCE_DERIVED / MODERATE | REVIEW_REQUIRED | INR 18,000 potential |

## Governance

Changes require synchronized manifest, source files, validation, materialization tests, catalog, readiness matrix, script, and known limitations. Never add real customer data, secrets, or provider credentials. Demo-only loaders remain profile-gated and must not alter normal production startup.
