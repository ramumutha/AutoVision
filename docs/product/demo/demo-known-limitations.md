# Service Profit R1 Demo Known Limitations

## Data and Market

- The dataset is `SYNTHETIC_DEMO_ONLY`; it is not sampled from a production dealer.
- Market is `IN` and default currency is INR. Cross-market and cross-provider readiness are **RESEARCH REQUIRED**.
- Vehicle class and powertrain applicability are not explicitly classified.
- Manifest coverage percentages are encoded expectations, not measured production benchmarks.

## Capability

- Structured disposition is partial (55% expected demo coverage).
- Cost data is partial (62% expected demo coverage).
- Explicit declined-work capability is partial; reconstruction is available.
- Gross-profit attribution is partial. Revenue-only scenarios must not be presented as complete margin evidence.
- Context is a Service Profit read projection, not canonical Customer/Vehicle master data.

## Product Workflow

- Customer outreach, follow-up, consent, and disposition workflow are not implemented.
- PDR-007 defines a bounded internal follow-up/disposition candidate, but it has no implementation, release target, validated dealer workflow, or demo evidence.
- Rich longitudinal management analytics and benchmarking are not implemented.
- Current role/location business context is not available and must not be fabricated.
- A complete external design/theme-system adoption is not approved or implemented.
- The primary R1 manager intentionally does not expose the advanced filter surface. The underlying API/query filtering capability remains available; do not describe filtering as removed from the platform.
- Search is not implemented. **SEARCH DEFERRED — authoritative server search contract required.** The current endpoint is server-paginated and does not provide an approved authoritative contract for searching the complete opportunity dataset.

## Deferred UX Polish

The following accepted items are non-blocking and belong to the next frontend refinement
cycle: **UX-POLISH-01** Data Capability dialog close-button spacing/border/placement;
**UX-POLISH-02** Opportunities heading/count baseline alignment across breakpoints;
**UX-POLISH-03** accessible progress/skeleton treatment for initial loading; and
**UX-POLISH-04** validation of Refresh placement against its actual refresh scope.
These items do not change SP-F004 IMPLEMENTED status or demo readiness.

## Evidence

- Production conversion uplift, false-positive rate, handling-time reduction, adoption, willingness to pay, and ROI targets are unknown.
- Automated tests establish deterministic repository behavior; they do not prove production usability or market fit.
- Accessibility automation does not replace user research with assistive-technology users.

## Operational

- The demo requires the opt-in compose override and local authenticated identity mapping.
- Demo automation must not enable direct access grants, bypass JWT validation, or weaken tenant checks.
- Resetting Docker volumes is destructive and not part of normal demo operation.

Present these limitations during demonstrations and keep them synchronized with the [readiness matrix](demo-readiness-matrix.md), [data research](../research/data-availability-research.md), and [local runtime](../../development/local-runtime.md). Use the [demo business question register](demo-business-question-register.md) when preparing live Q&A, PPT material, or recorded demonstrations so answers remain tied to current evidence.
