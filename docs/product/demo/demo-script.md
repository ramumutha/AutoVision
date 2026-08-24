# Service Profit R1 Demo Script

## Purpose

Demonstrate the implemented Service Profit R1 manager workflow using the synthetic, profile-gated dataset. This script does not claim production dealer ROI or market validation.

## Preparation

1. Follow the [local runtime Service Profit demo runbook](../../development/local-runtime.md#service-profit-r1-dealer-demo).
2. Validate `demo-data/service-profit/r1/manifest.json` with the provided validation script.
3. Start the normal compose file plus `docker-compose.service-profit-demo.yml`.
4. Confirm the browser application, Keycloak, platform health, and authenticated `/api/v1/me` flow.
5. Do not place credentials or tokens in screenshots, recordings, notes, or logs.
6. Review the [demo business question register](demo-business-question-register.md) and select answers relevant to the audience. Do not improvise unsupported pricing, ROI, integration, or automation claims.

## Narrative

### 1. Authenticate and establish trust

Sign in through the configured public SPA/PKCE flow. Explain that the UI guard improves navigation while server authorization and tenant isolation remain authoritative. Do not claim a role or location that the authenticated contract does not provide.

### 2. Open the manager summary

Navigate to `/service-profit`. Show recoverable potential per currency, total opportunities, high priority, review required, and ready to action. Explain that counts come from the summary API, not the first queue page.

### 3. Triage the queue

Use Opportunity Type as primary navigation, then Priority/Actionability and Sort. Refresh once to show retained-content behavior. Explain that state is URL-backed and requests cancel stale results.

### 4. Review an evidence-backed opportunity

Open SP-DEMO-001 or SP-DEMO-003. Show customer/vehicle/service projection, commercial value, deterministic explanation, evidence basis, recommended action, and collapsed provenance. State that context is a Service Profit projection, not canonical Customer/Vehicle master data.

### 5. Show suppression safety

Open SP-DEMO-004. Point out the explicit suppressed state, “Do not action,” reason, and completed-work scenario. Do not describe it as a lead.

### 6. Show human review

Open SP-DEMO-010. Point out moderate/derived evidence and `REVIEW_REQUIRED`. Explain that AutoVision does not present ambiguity as ready for customer contact.

### 7. Demonstrate responsive behavior

At desktop/tablet, show inline detail beneath one selected row. At 390px or 430px width, show concise cards, compact Sort & Filter, routed detail, and Back restoration. Avoid presenting viewport tests as customer research.

## Close

Summarize implemented value: prioritized opportunities, explainable evidence, commercial context, tenant-safe access, and safety states. State current limitations: synthetic India dataset, partial disposition/cost capability, no outreach workflow, no rich analytics, and no validated production KPI uplift.

If discussing the next candidate, describe the future journey as `Detected → Reviewed → Accountable follow-up → Outcome/disposition → eventual conversion/revenue measurement`. State that [PDR-007](../decisions/PDR-007-bounded-internal-follow-up-disposition.md) defines only the internal follow-up/disposition candidate; it is not implemented or release-approved, and conversion/revenue attribution remains future work.

Use the [business question register](demo-business-question-register.md) for Q&A and reusable PPT/video language. The product should eventually answer ownership, due state, actionability restriction, and disposition directly in the detail journey so a presenter does not need to explain those basic states verbally.

## Failure Handling

If authentication, demo materialization, or data validation fails, stop and use the [troubleshooting guide](../../development/troubleshooting.md). Never bypass JWT validation, tenant mapping, or profile gating to continue a demo.
