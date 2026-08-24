# Service Profit Data Availability Research

## Record

- **Research ID:** RS-001
- **Question:** Which Service Profit signals and outcomes can the current R1 demo dataset support?
- **Date:** Not recorded
- **Market:** India (`IN`)
- **Vehicle applicability:** Not explicitly classified by vehicle class or powertrain
- **Source:** [`demo-data/service-profit/r1/manifest.json`](../../../demo-data/service-profit/r1/manifest.json), synthetic source files, validation script, materializer, and automated tests
- **Classification:** `SYNTHETIC_DEMO_ONLY`
- **Confidence:** High for deterministic synthetic behavior; not evidence of production prevalence or quality
- **Lifecycle:** Applied; revisit with production-like data

## Manifest Coverage

| Data area | Expected synthetic coverage |
| --- | ---: |
| Identity | 90% |
| Vehicle linkage | 96% |
| Service transaction | 100% |
| Recommendation evidence | 88% |
| Disposition | 55% |
| Mileage | 82% |
| Invoice linkage | 85% |
| Cost | 62% |

These percentages are expectations encoded in the demo manifest. Their empirical derivation is not documented and must not be presented as production benchmarks.

## Capability Profile

**AVAILABLE:** technician/advisor notes, recommendation history, service history, mileage history, customer activity history, invoice linkage, declined-work reconstruction, deferred work, due/overdue service, inactive customer, and revenue attribution.

**PARTIAL:** structured disposition, cost data, explicit declined work, and gross-profit attribution.

## Scenario Coverage

The ten manifest scenarios include source-confirmed and evidence/policy-derived opportunities, ready/suppressed/review-required actionability, partial conversion, revenue with incomplete cost, and ambiguous customer identity. See the [demo data catalog](../demo/demo-data-catalog.md) for scenario-level governance.

## Product Implications

The dataset is sufficient to demonstrate deterministic opportunity detection, safety states, manager workflows, and partial-data handling. It is not sufficient to establish:

- production dealer data readiness;
- cross-DMS or cross-market portability;
- market frequency or financial uplift;
- acceptable false-positive rates;
- vehicle-class/powertrain coverage.

## Decision and Revisit

Future provider/market work should create separate research records with representative, authorized, de-identified data. Any change to readiness thresholds or capability claims requires a Product Decision Record and corresponding policy tests.
