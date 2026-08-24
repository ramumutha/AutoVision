# Agent Feature Workflow

## Purpose

Repository instructions and linked product/architecture documents carry AutoVision's standing engineering rules. A feature prompt should therefore describe only what is unique to the slice. Humans remain responsible for product approval and can understand every requirement without an agent.

## Required Prompt Shape

```text
Feature / Slice ID:
Baseline commit:
Objective:

Feature-specific scope:
- ...

Acceptance criteria:
- ...

Explicit exclusions:
- ...

DO NOT COMMIT.
```

Add a branch only when it matters. Link the relevant feature, product decision, UX flow, ADR, or API contract rather than copying it into the prompt.

## Agent Operating Flow

1. Verify branch, baseline, status, and staging.
2. Read `.github/copilot-instructions.md`, applicable path instructions, `AGENTS.md`, and linked authoritative docs.
3. Inspect the bounded owner and a discriminating test/call site.
4. State the local hypothesis and make the smallest justified edit.
5. Run focused validation immediately, then broader gates proportional to risk.
6. Update affected product, architecture, traceability, demo, or runbook records.
7. Report changed files, sizes, duplication review, validation, warnings, residual risks, and staged state.

The agent must not infer approval for roadmap additions, breaking contracts, destructive migrations, new dependencies, or security changes from a short prompt.

## Example: Frontend Slice

```text
Feature / Slice ID:
SP-R1-CARD-STATUS

Baseline commit:
2482783

Objective:
Add a text-visible evidence-strength indicator to the existing mobile Service Profit card.

Feature-specific scope:
- Service Profit mobile card presentation and focused tests
- Existing localization entries if required

Acceptance criteria:
- Evidence strength is understandable without color
- Existing routed link and query preservation remain intact
- 390x844 and 430x932 have no document overflow

Explicit exclusions:
- No API or database changes
- No desktop detail redesign
- No new dependency

DO NOT COMMIT.
```

The standing frontend, accessibility, testing, documentation, security, and maintainability rules come from repository instructions.

## Example: Backend Slice

```text
Feature / Slice ID:
SP-R1-EXPORT-READINESS

Baseline commit:
<verified commit>

Objective:
Expose an additive readiness field in the existing Service Profit summary response.

Feature-specific scope:
- Explicit response DTO field
- Domain service calculation from existing authoritative state
- Contract and authorization tests

Acceptance criteria:
- Existing clients remain compatible
- Cross-tenant access remains impossible
- Null/empty source state is defined

Explicit exclusions:
- No schema change
- No client implementation
- No provider-specific logic

DO NOT COMMIT.
```

## Completion Prompt

For freeze or audit work, request exact validation gates and the expected changed-file boundary. Staging or committing must be a separate explicit instruction.
