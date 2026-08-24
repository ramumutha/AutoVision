# AutoVision Release Register

## Release Records

| Release ID | Scope | Status | Baseline/evidence | Product references | Validation evidence | Known limitations |
| --- | --- | --- | --- | --- | --- | --- |
| R1-SP | Service Profit opportunity foundation through responsive authenticated manager experience | IMPLEMENTED | Current frozen baseline `2482783`; feature commits in repository history | SP-F001..SP-F015; [manager UX](../service-profit-manager-ux.md) | Platform/frontend unit, integration, responsive, accessibility, and demo tests | Synthetic demo is not production evidence; partial disposition/cost capability; external KPI validation absent |
| FUTURE-UNASSIGNED | Bounded internal follow-up candidate, outreach, richer analytics, authoritative business context, broader design system | DEFERRED / RESEARCH REQUIRED | No approved baseline or target | SP-F016, SP-F016-A, SP-F017..SP-F019; [PDR-007](../decisions/PDR-007-bounded-internal-follow-up-disposition.md) | None | SP-F016-A is defined but not release-approved; remaining scope must not be represented as committed |

## Release Record Requirements

A future release row should include:

- stable release ID and owner;
- included feature IDs and explicit exclusions;
- approved target/status;
- baseline and freeze commit;
- product/architecture decisions;
- automated validation and security evidence;
- migration/runtime impacts;
- demo readiness and known limitations;
- release date only when actually recorded.

## Historical Caution

This foundation does not assign dates to earlier R1 slices because reliable dates were not captured in product governance records. Git history remains the implementation chronology. Future freeze processes should update this register at the time of decision.
