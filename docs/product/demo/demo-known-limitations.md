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
- Rich longitudinal management analytics and benchmarking are not implemented.
- Current role/location business context is not available and must not be fabricated.
- A complete external design/theme-system adoption is not approved or implemented.

## Evidence

- Production conversion uplift, false-positive rate, handling-time reduction, adoption, willingness to pay, and ROI targets are unknown.
- Automated tests establish deterministic repository behavior; they do not prove production usability or market fit.
- Accessibility automation does not replace user research with assistive-technology users.

## Operational

- The demo requires the opt-in compose override and local authenticated identity mapping.
- Demo automation must not enable direct access grants, bypass JWT validation, or weaken tenant checks.
- Resetting Docker volumes is destructive and not part of normal demo operation.

Present these limitations during demonstrations and keep them synchronized with the [readiness matrix](demo-readiness-matrix.md), [data research](../research/data-availability-research.md), and [local runtime](../../development/local-runtime.md).
