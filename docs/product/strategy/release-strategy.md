# AutoVision Release Strategy

## Principles

AutoVision releases bounded product slices that are secure, demonstrable, backward-compatible, and traceable to evidence. A release label is not proof of readiness; readiness comes from implementation, automated tests, documentation, demo data, known limitations, and an explicit product decision.

## Status Language

- **IMPLEMENTED:** present and validated in the repository.
- **PLANNED:** approved direction with an identified target.
- **DEFERRED:** intentionally outside the current release.
- **RESEARCH REQUIRED:** insufficient evidence or approval.

## Current R1 Strategy

**IMPLEMENTED:** the current Service Profit R1 sequence is revenue-first and manager-facing. Repository history and implementation support these delivered layers:

1. opportunity domain and persistence foundation;
2. secure tenant-aware APIs and deterministic detection/explanation;
3. dealer-data readiness and synthetic demo materialization;
4. actionable customer/vehicle/service context projection;
5. authenticated manager summary and queue;
6. opportunity-type navigation and URL-backed filters/sort;
7. desktop/tablet inline detail;
8. mobile cards, compact filters, and routed detail;
9. suppression and `REVIEW_REQUIRED` safeguards throughout.

The exact historic dates for these slices are not recorded in governance records. Commit history is the implementation chronology; do not invent release dates.

## Release Gates

A product slice is ready for freeze when:

- acceptance criteria and exclusions are satisfied;
- authorization, tenant isolation, validation, and safe errors are tested;
- API/schema changes are compatible and migrated;
- desktop/mobile/accessibility impact is validated where applicable;
- functional and architecture docs describe implemented behavior;
- traceability and demo readiness are updated;
- known limitations and research gaps are explicit;
- working-tree and validation evidence are reported.

## Future Releases

Customer outreach workflow, richer management analytics, role/location current-business context, and a complete design/theme adoption are not committed by this foundation. They remain **DEFERRED** or **RESEARCH REQUIRED** in the [feature register](../roadmap/feature-register.md) until evidence and Product Owner approval establish a release target.
