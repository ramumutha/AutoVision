# First Dealer Discovery Guide

**Status:** PLANNED discovery pack for the first R1 design-partner conversation

This guide is for a practical discovery meeting before requesting a controlled
`FULL` package. It is grounded in [AV-R1-CDF-001](../../contracts/r1-controlled-dealer-file-contract.md),
the [controlled intake architecture](../architecture/controlled-dealer-data-intake.md),
and the [Service Profit manager workflow](../product/service-profit-manager-ux.md).
It records dealer facts and open questions; it does not approve a provider
integration, a workflow feature, or a production claim.

## Meeting Use

Start with the dealer's current process and a recent representative example.
Capture answers as `CONFIRMED`, `PARTIAL`, `UNKNOWN`, or `RESEARCH REQUIRED`.
Do not ask for customer names, contact details, credentials, raw notes, or live
production extracts during the conversation. Record the participant role, not a
person's name, unless an approved recruitment record owns that information.

## A. Dealer and Organization

- Dealer group, legal/operating dealer, and agreed AutoVision tenant/dealer IDs.
- Branches or locations in scope; workshop structure; centralized service or BDC teams.
- Service operation ownership and approximate repair-order volume band.
- DMS/provider name and version, if known; other service, CRM, inspection, quote, or reporting systems.
- Which locations and service departments can participate in the first sample.

## B. Current Service Process

Walk through arrival -> repair/service order -> inspection -> recommendation ->
quote -> approval or decline -> workshop -> invoice -> follow-up. Do not assume
this sequence is universal. Ask where each step is recorded, which system is
authoritative, what happens when a step is skipped, and how cancellation,
reopening, and correction are represented.

## C. Declined and Deferred Work

- Where is recommended work stored when not approved?
- Are decline and deferral distinct, and are reasons recorded?
- Does the recommendation remain visible on later visits?
- Do repeated declines update one record or create new records?
- Can a later completed job be linked to the earlier recommendation?
- Which source value means unknown, not applicable, or already completed?

Do not convert ambiguous values into `DECLINED`. Preserve the source value and
mark the mapping `REVIEW REQUIRED`.

## D. Customer and Vehicle Identity

- Which stable source IDs exist for customer, vehicle, order, job, line, and recommendation?
- Which relationship keys link customer to vehicle and vehicle/order/job/line?
- Are IDs stable across exports and locations? How are merges, duplicates, and corrections represented?
- Can the first sample use pseudonymous IDs without names, VINs, registration, phone, or email?

## E. Service History

Confirm availability and meaning of repair orders, service jobs, service lines,
recommendations, dispositions, notes, service history, mileage, service dates,
invoice linkage, completion, cancellation, and reopened work. Agree the history
window from the pilot question and dealer availability; do not impose a universal
duration.

## F. Financial Data

Assess each separately: quoted amount, labor, parts, invoice amount, discounts,
tax, currency, cost, and gross profit. Missing cost or gross profit does not
necessarily prevent opportunity detection. No margin, recovered revenue, or
conversion claim is allowed without validated source linkage and outcome data.

## G. Contact and Consent

Ask whether customer name, phone, and email exist and whether each is permitted
for this pilot. Record `CONTACT DATA EXISTS` separately from `CONSENT TO CONTACT`.
AutoVision must not infer consent from field presence. Contact fields are not
required for first detection unless a separately approved purpose requires them.

## H. Source Semantics

Document statuses, dispositions, blank and unknown values, cancelled/deleted/
corrected/reopened/duplicate records, record versioning, source timestamps,
timezone, currency, distance units, and source identifiers. Every unresolved
meaning becomes a mapping question, not an assumed normalization.

## I. Extraction and Transfer

Ask about manual export, report export, CSV, JSON, database extract, API, history
export, approximate counts, file sizes, and feasible frequency. An API is not
required for the first pilot. The first transfer remains an authorized,
operator-managed controlled transfer; no public upload, SFTP product, provider
API, streaming, or DELTA feed is required.

## J. Security and Privacy

Capture permitted fields, purpose, transfer mechanism, authorized contacts,
retention, deletion, quarantine handling, support/debug access, permitted
environments, pseudonymization requirements, and the data-processing/legal
approval owner. Do not place raw data, credentials, tokens, or customer examples
in meeting notes or tickets.

## Small IAM/RBAC Discovery Input

Capture organizational facts only:

- Who reviews Service Profit opportunities?
- Who follows up?
- Who manages service operations and workshop execution?
- Who may see financial information?
- Who administers dealer users?
- Do staff work across locations? Are service or BDC teams centralized?

Label answers `IAM/RBAC DISCOVERY INPUT`. Do not create roles, rename users, or
turn job titles into permissions. `ROLE` remains an organizational function;
`PERMISSION` remains an application capability.

## RS-005 Research Input

Discovery may surface workflow observations useful to RS-005. Record them
separately as `RS-005 RESEARCH INPUT`: self-claim, manager assignment, routing,
due dates, reminders, disposition language, completion, reopen behavior, or
notes. Do not freeze these observations as product requirements or mutate the
RS-005 workflow.

## Discovery Output

Before requesting data, produce:

- confirmed dealer, location, source, and authorized contact boundary;
- current-process map and source-of-truth notes;
- stable identity and relationship inventory;
- status/disposition mapping questions;
- Level 1, 2, and 3 field decisions;
- privacy, retention, deletion, and transfer decisions;
- open risks, owners, and decision dates;
- recommendation for Gate A and Gate B in the
  [acceptance checklist](first-dealer-pilot-acceptance-checklist.md).
