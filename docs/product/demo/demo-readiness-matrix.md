# Demo Readiness Matrix

## Rating Rules

- **GREEN:** repository evidence supports a repeatable demo path.
- **AMBER:** demonstrable with an explicit limitation, partial data, or manual dependency.
- **RED:** not ready or no evidence exists.

Ratings describe demo readiness, not production readiness or market validation.

| Feature | Implemented | Automated Tests | Demo Data | UI Ready | Auth Ready | KPI/Value Story | Demo Script | Known Limitation | Readiness |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Opportunity foundation/detection | Yes | Platform domain/detection/demo suites | Ten synthetic scenarios | Via manager | Yes | Evidence-backed revenue opportunity story | Step 2â€“3 | Synthetic behavior only | GREEN |
| Secure opportunity API | Yes | Access/controller/query tests | Demo tenant and opportunities | Consumed by UI | Keycloak/UserRef flow | Tenant-safe access | Step 1â€“2 | Local demo identity setup required | GREEN |
| Manager summary and queue | Yes | Backend summary/query + Angular/E2E | Currency/count scenarios | Desktop/tablet/mobile | Yes | Prioritized commercial overview | Step 2 | Production KPI uplift not established | GREEN |
| Opportunity Type/filter/sort | Yes | Manager/component/E2E | Multiple opportunity types | Responsive | Yes | Faster triage hypothesis | Step 3 | Authoritative counts depend on loaded demo | GREEN |
| Contextual explanation/detail | Yes | Context/explanation/detail tests | Context materialized by demo loader | Shared inline/routed detail | Yes | Explain why and what to do | Step 4 | Projection is not canonical master data | GREEN |
| Suppression safeguard | Yes | Domain/repository/UI/demo tests | SP-DEMO-004 | Yes | Yes | Prevent inappropriate action | Step 5 | Production incident rate unknown | GREEN |
| Review-required safeguard | Yes | Detection/UI/demo tests | SP-DEMO-010 | Yes | Yes | Human review before contact | Step 6 | Review workflow itself is not implemented | GREEN |
| Mobile cards/routed detail | Yes | Focused Angular + responsive/Axe E2E | Shared queue/detail fixture | 390/430 tested | Guarded route | Mobile access to same decision flow | Step 7 | Device/user research not recorded | GREEN |
| Data readiness/capability | Yes | Policy/loader/manifest plus capability API/UI tests | Coverage/capability manifest | Manager-facing capability disclosure | Yes; underlying synthetic loader remains demo profile gated | Makes supported and partial commercial attribution capability explicit | Manager disclosure / presenter note | Disclosure is implemented, but coverage values are synthetic expectations and provider-specific production readiness is unvalidated | AMBER |
| Gross-profit attribution | Partial | Capability/demo tests | Cost coverage 62%; partial capability | Potential/revenue story only | Yes | Margin story requires caution | Optional note | Partial cost data | AMBER |
| Internal follow-up/disposition (`SP-F016-A`) | No | None | None | No | No implementation; security model defined only | Potential accountability/pilot-learning story | Future journey only | [RS-005 package](../research/RS-005-dealer-follow-up-workflow-validation.md) prepared; fieldwork, findings, and release approval pending | RED |
| Customer outreach/follow-up | No | None | None | No | Not designed | Unvalidated | None | Deferred; do not demo as implemented | RED |
| Rich management analytics | No | None | None | No | Not designed | Unvalidated | None | Research required | RED |

## Gate Ownership

Demo owner verifies the script and data. Engineering verifies runtime and automated gates. QA verifies the critical path and known limitations. Product owns the value narrative and must not present synthetic results as production outcomes. Anticipated stakeholder questions and evidence-safe answers are governed by the [demo business question register](demo-business-question-register.md).
