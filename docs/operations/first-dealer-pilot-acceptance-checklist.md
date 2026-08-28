# First Dealer Pilot Acceptance Checklist

**Status:** PLANNED acceptance checklist for the first controlled R1 pilot

This checklist aligns dealer confirmation with the frozen G6.6-PRE approval
boundary. It does not approve provider integration, customer outreach, DMS
write-back, or production readiness.

## Owners

Use role placeholders until ownership is agreed: `[PRODUCT]`, `[DATA]`,
`[PLATFORM]`, `[SECURITY/PRIVACY]`, `[OPERATIONS]`, `[DEALER OWNER]`.

## Gate A - Discovery Complete

**Minimum evidence:** dealer and location scope; current service-process map;
DMS/source inventory; stable identity and relationship inventory; declined and
deferred workflow answers; financial and contact-data classification; source
semantics; privacy/security questions; unresolved decisions with owners.

Decision: `GO / HOLD` by `[PRODUCT]` and `[DEALER OWNER]`.

## Gate B - Data Request Approved

**Minimum evidence:** approved Level 1/2/3 field list; mapping worksheet;
status/disposition mapping questions; agreed effective window; source schema and
mapping version; permitted fields; purpose; transfer, retention, deletion, and
access controls; no credentials or secrets requested.

Decision: `GO / HOLD` by `[DATA]`, `[SECURITY/PRIVACY]`, and `[DEALER OWNER]`.

## Gate C - Package Received Safely

**Minimum evidence:** authorized operator-managed controlled transfer; package
reference below the configured controlled root; authenticated operator and
contained tenant/dealer/location; one `FULL` package; stable dataset ID/version;
source IDs and relationships; system-computed digest, byte size, receipt time,
correlation identity, and actual counts.

Decision: `GO / HOLD` by `[OPERATIONS]` and `[PLATFORM]`.

## Gate D - Validation and Reconciliation Accepted

**Minimum evidence:** envelope/schema/type/reference findings; accepted,
rejected, and quarantined counts; containment result; duplicate identity result;
unknown/ambiguous value list; mapping status; capability report using
`AVAILABLE`, `PARTIAL`, `UNAVAILABLE`, or `REVIEW REQUIRED`; PII/log review;
known limitations and exclusions.

No record with unsafe identity or containment may be silently materialized.

Decision: `GO / HOLD` by `[DATA]`, `[PRODUCT]`, `[SECURITY/PRIVACY]`, and
`[DEALER OWNER]`.

## Gate E - Materialization Approved

**Minimum evidence:** dataset identity, dealer/location, effective period,
counts, quarantine, mapping confirmation, unknown values, capability status,
limitations, exclusions, and explicit approval record. Approval must transition
`STAGED` to the existing `READY_FOR_MATERIALIZATION` state. The authenticated
operator must have the controlled intake permission for the authorized scope.

Decision: `GO / HOLD` by `[DEALER OWNER]`, `[PRODUCT]`, and `[PLATFORM]`.

## Gate F - First Service Profit Output Reviewed

**Minimum evidence:** manager Work Queue/Work Card review of opportunity identity,
type, priority, actionability, evidence strength, explanation, provenance,
suppression/review state, and recoverable potential by currency. Confirm that
potential is not represented as recovered or realized revenue and that no
customer contact or consent is implied.

Capture feedback as product research, not as an automatic workflow requirement.

Decision: `GO / HOLD` by `[PRODUCT]` and `[DEALER OWNER]`.

## Gate G - Controlled Pilot Ready

**Minimum evidence:** Gates A-F complete; transfer and access owners confirmed;
retention/deletion path tested; reconciliation and audit evidence retained;
materialization/replay/recovery behavior understood; first-pilot limitations
accepted; no provider, DMS write-back, outreach, ROI, gross-profit, or production
performance claim made.

Decision: `GO / HOLD` by `[PRODUCT]`, `[SECURITY/PRIVACY]`, `[OPERATIONS]`, and
`[DEALER OWNER]`.

## Pre-Meeting Checklist

- [ ] Confirm discovery participants by role and note-taking permission.
- [ ] Prepare contract, runbook, discovery guide, and mapping worksheet.
- [ ] Agree no customer records, credentials, or raw note content will be shared in the meeting.
- [ ] Prepare synthetic examples only.

## During-Meeting Checklist

- [ ] Map the dealer's actual process before discussing AutoVision output.
- [ ] Capture source-of-truth systems, identities, relationships, and semantics.
- [ ] Separate current behavior, pain, and research input from product requirements.
- [ ] Record IAM/RBAC discovery input without creating roles or permissions.

## Before Data Request

- [ ] Complete Gate A and approve Gate B.
- [ ] Confirm Level 1 minimum detection fields and optional Level 2/3 fields.
- [ ] Confirm permitted fields, transfer, retention, deletion, and authorized access.
- [ ] Confirm one authorized FULL snapshot and no secrets.

## On Data Receipt

- [ ] Use the authenticated controlled intake boundary and package-root reference.
- [ ] Confirm tenant/dealer/location containment and source identity.
- [ ] Review system-computed integrity metadata and actual counts.
- [ ] Review findings, quarantine, reconciliation, and capability report.

## Before Materialization

- [ ] Complete Gate D and obtain Gate E approval.
- [ ] Confirm no unresolved containment or fatal validation issue.
- [ ] Confirm explicit limitations and excluded capabilities.
- [ ] Record approval before invoking materialization.

## After Materialization

- [ ] Review lifecycle, audit, reconciliation, opportunities, suppression, and errors.
- [ ] Confirm replay/recovery behavior if exercised.
- [ ] Preserve safe evidence without raw payloads or PII in operational records.

## Before Pilot Review

- [ ] Complete Gate F and record dealer/product feedback.
- [ ] Separate opportunity potential from outcomes and revenue.
- [ ] Record remaining dealer, privacy/legal, data-quality, and product decisions.
- [ ] Decide Gate G without implying production readiness.
