# Controlled Dealer Data Intake Operator Runbook

**Status:** IMPLEMENTED for the bounded R1 G6.5 internal operational layer and
authenticated G6.6-PRE command boundary

This runbook covers controlled `FULL` dataset intake, reconciliation, replay,
and explicit recovery of abandoned materialization. It does not provide a
public API, upload workflow, scheduler, provider adapter, or production
provider support claim. The [R1 controlled dealer file contract](../../contracts/r1-controlled-dealer-file-contract.md)
and [controlled intake architecture](../architecture/controlled-dealer-data-intake.md)
remain authoritative.

## 1. Prerequisite Checks

- Confirm the request is from an authorized operator or approved application.
- Confirm the tenant, dealer, and optional location are contained and authorized.
- Confirm the dataset is a `FULL` delivery with a stable dataset ID and version.
- Confirm checksum, byte size, source schema version, mapping version, and
  processing correlation ID are available.
- Confirm the operator or application has the controlled intake process
  permission for the target dealer and, when supplied, location.
- Confirm the evidence is synthetic or approved controlled input. Never use
  `.env`, credentials, tokens, cookies, or raw authentication material as data.

## 2. Authorized FULL Dataset Receipt

Receive the source through the approved controlled transfer process. Preserve
source record identity and lineage. Do not infer missing identity, disposition,
consent, cost, currency, or margin. A same-identity unchanged delivery is an
idempotent no-op; a corrected delivery uses a new dataset version.

## 3. Controlled Intake Execution

Use the secured `POST /api/v1/controlled-data-intake` command with a relative
`packageReference` beneath the configured controlled-intake package root. The
platform resolves tenant and actor identity from the authenticated request,
checks dealer/location authorization, and treats package tenant metadata as an
assertion. Absolute paths and traversal references are rejected. The existing
intake path then validates the envelope and typed records, persists source
lineage, stores safe findings, and quarantines unsafe records. No
provider-specific adapter is implied.

The safe operational response contains dataset identity, lifecycle status,
record counts, finding counts, and duplicate status. It does not return raw
records or payloads.

## 4. Validation Outcome Interpretation

Review findings by severity, validation stage, stable code, and capability.
`FATAL` findings reject the dataset. `ERROR` findings quarantine affected
records or reject the dataset when containment is unsafe. `WARNING` and `INFO`
findings may permit safe processing when the affected capability is explicitly
reduced or documented.

## 5. Quarantine Interpretation

Quarantined records retain safe lineage and are excluded from opportunity
materialization. Quarantine is not evidence that the source entity is invalid;
it means the current record cannot be safely used without correction or review.
Do not bypass quarantine by editing lifecycle state directly.

## 6. Capability and Readiness Interpretation

Readiness is capability-specific. Identity/linkage, detection, recommendation
evidence, disposition, invoice/revenue attribution, cost/gross-profit
attribution, mileage policy, and contactability may each be available, partial,
unavailable, or review-required. Missing cost never permits inferred margin.
Missing phone or email does not invalidate detection and does not imply consent.

## 7. Materialization Execution

Materialization requires an explicit approval checkpoint. After reviewing
validation, quarantine, reconciliation, and capability/readiness evidence, use
`POST /api/v1/controlled-data-intake/{datasetProcessingId}/approve`. This
transitions only an eligible `STAGED` dataset to the existing
`READY_FOR_MATERIALIZATION` state and records the authenticated approving
actor. Then use
`POST /api/v1/controlled-data-intake/{datasetProcessingId}/materialize`.
That command requires the same authorized server-derived scope and executes the
existing controlled materializer with the authenticated actor identity. A
`STAGED` dataset cannot bypass approval through this boundary. The
materialization identity and attempt are durable. Existing Service Profit
opportunity idempotency semantics apply to repeated processing.

## 8. Reconciliation Review

Use the internal operational status boundary to review the persisted dataset
state, record populations, finding totals, pre-staging rejection count,
materialization identity and timestamps, attempt count, structured failure, and
operational events. Reconciliation must remain consistent with durable source
lineage; it must not assume every parsed record was staged.

## 9. Duplicate and No-op Interpretation

An unchanged same-identity delivery is a deterministic no-op. It must not create
extra source records, opportunities, or materialization attempts. Existing
opportunity outcomes are evidence of idempotent persistence, not new demand.

## 10. MATERIALIZATION_FAILED Handling

Review the structured failure stage, stable failure code, failed attempt,
failure timestamp, safe reason, and replayability. Do not persist or request
stack traces. Confirm that prior opportunities remain intact before any replay.

## 11. Replay Eligibility

Controlled replay is allowed only for `MATERIALIZATION_FAILED` datasets marked
replayable and requires an authorized principal for materializer execution.
Replay is not allowed for `RECEIVED`, non-ready `STAGED`, `VALIDATION_FAILED`,
or `QUARANTINED` datasets. `READY_FOR_MATERIALIZATION` uses normal
materialization, not replay.

## 12. Controlled Replay

Record and review `REPLAY_REQUESTED`, `REPLAY_STARTED`, and the resulting
`REPLAY_SUCCEEDED` or `REPLAY_FAILED` event. Replay preserves dataset/version,
source lineage, and materialization identity semantics. The existing
opportunity key and persistence rules prevent duplicates. A failed replay stays
failed and remains auditable.

A `MATERIALIZED` dataset with the same materialization identity is a
no-op. It does not increment attempts, rewrite the success timestamp, or create
success history that represents work that did not occur.

## 13. Abandoned MATERIALIZING Recovery

Recovery is explicit and operator/application controlled. Confirm that the
current state is `MATERIALIZING`; do not guess whether external work succeeded.
Require an actor or approved application identity, a stable recovery code, and
a safe reason. Recovery transitions the dataset to `MATERIALIZATION_FAILED`,
preserves the original materialization identity and attempt, records
`ABANDONED_MATERIALIZATION_MARKED_FAILED`, and marks replayability explicitly.
It never marks the dataset `MATERIALIZED`.

## 14. Operational Event Interpretation

Events are tenant-contained and retain dataset identity, attempt, actor or
application, timestamp, stable code, safe reason, correlation, and materialization
identity where applicable. Receipt and materialization approval are recorded as
safe operational events. Event history is evidence of requested and completed
operations, not proof of external provider behavior.

## 15. Safe Troubleshooting

Start with tenant and dataset identity, then inspect lifecycle state, lineage,
checksums, record populations, findings, rejection evidence, structured failure,
and event order. Avoid raw payloads and PII in tickets, logs, screenshots, and
chat. Repeat only explicit operations with the same controlled identity rules.

## 16. Escalation Conditions

Escalate when tenant containment, checksum, source identity, materialization
identity, finding counts, event ordering, or replayability is contradictory.
Escalate suspected external materialization success rather than marking the
local dataset successful. Escalate privacy, authorization, deletion, or
retention concerns to the appropriate security/privacy owner.

## 17. Data and Privacy Handling

Use least privilege and tenant-scoped access. Do not copy phone, email, notes,
customer names, VINs, registrations, raw JSON, secrets, or tokens into
operational evidence. Contact information is not consent. Follow the approved
retention and deletion policy.

## 18. Rollback and Non-destructive Principles

Do not delete prior opportunities or lineage to simplify replay. Do not rewrite
historical migrations or successful materialization evidence. Use a new dataset
version for corrections. Preserve failure and recovery events so an operator
can reconstruct what happened.

## 19. Claims Operators Must Not Make

- **Recoverable Potential != Recovered Revenue**
- **Revenue attribution capability != Gross-profit capability**
- **Contact information != Consent**
- **Synthetic/demo proof != Production provider support**

Do not claim DMS/provider production validation, automated retry coverage,
DELTA support, customer outreach, or recovered revenue/ROI from this layer.
