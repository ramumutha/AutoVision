# R1 Controlled Dealer File Contract

- **Contract ID:** AV-R1-CDF-001
- **Version:** `1.0`
- **Status:** PLANNED; architecture and contract foundation only
- **Scope:** Controlled `FULL` file intake for authorized dealer data
- **Owner:** AutoVision architecture with Service Profit and data-operations owners
- **Classification:** Provider-neutral; not a DMS/provider implementation

This contract defines the boundary between an authorized external source and
AutoVision canonical staging. It is not the synthetic demo schema, a runtime
adapter, a database migration, or a promise of production provider support.
The first implementation may accept one controlled file per dataset. Future
REST, SFTP, batch, and provider-specific adapters must produce the same
envelope and canonical records.

## Dataset Envelope

The envelope is metadata and lineage. It must not contain credentials, access
tokens, cookies, passwords, or raw authentication material.

| Field | Type | Requirement | Meaning and validation |
| --- | --- | --- | --- |
| `datasetId` | UUID/string | Required; system-generated or source-stable | Dataset identity within the tenant. Immutable for a delivery lineage. |
| `datasetVersion` | string | Required | Monotonic source or delivery version; opaque to AutoVision. |
| `tenantId` | UUID | Required; system-resolved | Authenticated AutoVision tenant. Must not be trusted from an uncontained source value. |
| `dealerId` | UUID/string | Required | Dealer identity mapped to the tenant. Must be authorized and contained by `tenantId`. |
| `locationId` | UUID/string | Optional | Dealer location where applicable; if supplied, must belong to `dealerId`. |
| `sourceSystem` | string | Required | Source system name supplied by the authorized source/operator. |
| `sourceProvider` | string | Optional | Known provider/DMS label. Absence is valid; no provider is assumed. |
| `sourceSchemaVersion` | string | Required | Version of the delivered source layout or agreed export profile. |
| `receivedAt` | timestamp | Required; system-generated | UTC receipt time, not a dealer event time. |
| `effectiveFrom` | timestamp/date | Optional | Start of the source data validity window. |
| `effectiveTo` | timestamp/date | Optional | End of the source data validity window; must not precede `effectiveFrom`. |
| `deliveryType` | enum | Required | `FULL` or `DELTA`. R1 materialization supports `FULL` only. |
| `recordCounts` | object | Required; system-computed | Per-record-type counts, accepted counts, rejected counts, and warning counts. Counts must reconcile to the staged payload. |
| `contentIntegrity` | object | Required; system-computed/verified | Algorithm and checksum/digest for the received bytes plus byte size. No secret or credential. |
| `mappingVersion` | string | Required | Version of the approved mapping profile used for assessment/materialization. |
| `processingCorrelationId` | UUID/string | Required; system-generated | Correlates validation, quarantine, assessment, and audit events. |

`tenantId`, `receivedAt`, `recordCounts`, `contentIntegrity`, and
`processingCorrelationId` are AutoVision-controlled values. Source identity,
source schema, business effective dates, and source record identifiers remain
lineage facts and must not be silently rewritten.

## Canonical File Shape

The file contains one envelope and zero or more typed record collections. Each
record has a source identity and is tenant-contained through the envelope.
Record identity is the tuple `(datasetId, recordType, sourceRecordId)` for the
delivery. A stable source business key should also be retained when available
to support corrections and future delta processing.

The following semantic tables are the R1 contract. `sourceRecordId` is required
on every record. UUIDs are preferred for canonical IDs, but an adapter must not
invent an entity identity when the source identity is absent or ambiguous.

### Organization

| Canonical field | Type | Req./null | Source identity / relationship | Validation | Capability affected |
| --- | --- | --- | --- | --- | --- |
| `organization.sourceRecordId` | string | Required/non-null | Stable source organization key | Non-empty, unique within dataset | Tenant/dealer containment |
| `organization.organizationId` | UUID/string | Optional/nullable | Canonical identity only after approved mapping | Must be stable within tenant | Organization linkage |
| `organization.name` | string | Required/non-null | Source value | Bounded, non-blank | Dealer display/readiness |
| `organization.dealerId` | UUID/string | Required/non-null | Envelope dealer | Must equal contained dealer | Tenant/dealer containment |
| `organization.locationId` | UUID/string | Optional/nullable | Source location key | Must belong to dealer | Location-scoped capability |

### Customer and Vehicle

| Canonical field | Type | Req./null | Source identity / relationship | Validation | Capability affected |
| --- | --- | --- | --- | --- | --- |
| `customer.sourceRecordId` | string | Required/non-null | Stable source customer key | Non-empty, unique in dataset | Customer identity |
| `customer.customerId` | UUID/string | Optional/nullable | Approved mapped identity | Never guessed from contact fields | Opportunity identity/review |
| `customer.displayName` | string | Optional/nullable | Source value | PII minimization; bounded | Review context |
| `customer.phone` | string | Optional/nullable | Source value | Normalize/validate format; no consent inference | Contactability only |
| `customer.email` | string | Optional/nullable | Source value | Normalize/validate format; no consent inference | Contactability only |
| `vehicle.sourceRecordId` | string | Required/non-null | Stable source vehicle key | Non-empty, unique in dataset | Vehicle linkage |
| `vehicle.vehicleId` | UUID/string | Optional/nullable | Approved mapped identity | Never guessed from partial attributes | Vehicle-dependent detection |
| `vehicle.customerSourceRecordId` | string | Required/non-null | References `customer.sourceRecordId` | Must resolve exactly once | Customer/vehicle linkage |
| `vehicle.registration` | string | Optional/nullable | Source value | Treat as sensitive; bounded | Vehicle review context |
| `vehicle.vin` | string | Optional/nullable | Source value | Format and uniqueness checks where present | Vehicle linkage |
| `vehicle.make`, `vehicle.model` | string | Optional/nullable | Source value | Bounded text | Vehicle context |
| `vehicle.modelYear` | integer | Optional/nullable | Source value | Plausible year range | Date/policy capability |

### Service Transaction, Job, and Line

| Canonical field | Type | Req./null | Source identity / relationship | Validation | Capability affected |
| --- | --- | --- | --- | --- | --- |
| `serviceOrder.sourceRecordId` | string | Required/non-null | Stable source order/visit key | Unique within dataset | Opportunity detection |
| `serviceOrder.customerSourceRecordId` | string | Required/non-null | References customer | Must resolve exactly once | Opportunity identity |
| `serviceOrder.vehicleSourceRecordId` | string | Required/non-null | References vehicle | Must resolve exactly once | Vehicle/service linkage |
| `serviceOrder.serviceDate` | date | Required/non-null | Source event time | Valid date | Date-based policy/service history |
| `serviceOrder.advisorSourceRecordId` | string | Optional/nullable | Source staff key | Must be contained if mapped | Explanation/context |
| `serviceOrder.mileage` | integer | Optional/nullable | Source event measurement | Non-negative; unit must be agreed | Mileage-dependent policy |
| `serviceOrder.notes` | string | Optional/nullable | Source note body | Sensitive; bounded and redacted from logs | Evidence/explanation |
| `serviceOrder.disposition` | enum/string | Optional/nullable | Source disposition only | Controlled vocabulary or `UNKNOWN`; never default to `DECLINED` | Declined-work/readiness |
| `serviceJob.sourceRecordId` | string | Required/non-null | Stable source job key | Unique within dataset | Job linkage |
| `serviceJob.serviceOrderSourceRecordId` | string | Required/non-null | References service order | Must resolve exactly once | Detection context |
| `serviceJob.status` | enum/string | Optional/nullable | Source status | Controlled mapping or review | Completion/suppression |
| `serviceLine.sourceRecordId` | string | Required/non-null | Stable source line key | Unique within dataset | Recommendation/service evidence |
| `serviceLine.serviceJobSourceRecordId` | string | Required/non-null | References job | Must resolve exactly once | Service linkage |
| `serviceLine.description` | string | Required/non-null | Source line description | Bounded, non-blank | Recommendation evidence |
| `serviceLine.quantity` | decimal | Optional/nullable | Source quantity | Non-negative | Revenue/cost calculation |
| `serviceLine.amount` | decimal | Optional/nullable | Source charge | Currency required when present | Revenue attribution |
| `serviceLine.currency` | string | Optional/nullable | Source currency | ISO-like agreed code | Currency-safe totals |

### Recommendation, History, Invoice, and Cost

| Canonical field | Type | Req./null | Source identity / relationship | Validation | Capability affected |
| --- | --- | --- | --- | --- | --- |
| `recommendation.sourceRecordId` | string | Required/non-null | Stable recommendation key | Unique within dataset | Opportunity detection |
| `recommendation.serviceOrderSourceRecordId` | string | Required/non-null | References service order | Must resolve exactly once | Recommendation linkage |
| `recommendation.description` | string | Required/non-null | Source recommendation | Bounded, non-blank | Detection/explanation |
| `recommendation.recommendedAt` | timestamp/date | Optional/nullable | Source event time | Valid date/time | Aging/readiness |
| `recommendation.status` | enum/string | Optional/nullable | Source status | Controlled mapping; absence is unknown | Disposition/review |
| `serviceHistory.sourceRecordId` | string | Required/non-null | Stable historical event key | Unique within dataset | History-dependent policy |
| `serviceHistory.vehicleSourceRecordId` | string | Required/non-null | References vehicle | Must resolve exactly once | Service history |
| `serviceHistory.serviceDate` | date | Required/non-null | Source event time | Valid date | Date-based policy |
| `serviceHistory.summary` | string | Optional/nullable | Source summary | Sensitive; bounded | Explanation/context |
| `invoice.sourceRecordId` | string | Required/non-null | Stable invoice key | Unique within dataset | Conversion/revenue attribution |
| `invoice.serviceOrderSourceRecordId` | string | Required/non-null | References service order | Must resolve exactly once | Invoice linkage |
| `invoice.invoiceDate` | date | Optional/nullable | Source event time | Valid date | Revenue timing |
| `invoice.totalAmount` | decimal | Optional/nullable | Source total | Non-negative; currency required | Revenue attribution |
| `invoice.currency` | string | Optional/nullable | Source currency | Agreed code | Currency-safe totals |
| `invoiceLine.sourceRecordId` | string | Required/non-null | Stable invoice-line key | Unique within dataset | Invoice linkage |
| `invoiceLine.invoiceSourceRecordId` | string | Required/non-null | References invoice | Must resolve exactly once | Conversion attribution |
| `invoiceLine.serviceLineSourceRecordId` | string | Optional/nullable | References service line where known | Must resolve if supplied | Recommendation conversion |
| `invoiceLine.amount` | decimal | Optional/nullable | Source line charge | Non-negative; currency context required | Revenue attribution |
| `cost.sourceRecordId` | string | Required/non-null | Stable cost key | Unique within dataset | Cost/gross-profit attribution |
| `cost.serviceLineSourceRecordId` | string | Optional/nullable | References service line where known | Must resolve if supplied | Cost attribution |
| `cost.amount` | decimal | Optional/nullable | Source cost | Non-negative; never inferred | Gross-profit attribution |
| `cost.currency` | string | Optional/nullable | Source currency | Agreed code | Gross-profit attribution |

All amounts require an explicit currency and source basis. Missing cost never
permits inferred margin. Notes and contact fields are PII/sensitive data and
must be minimized, access-controlled, and excluded from analytics/log bodies.

## Missing-Data Safety Semantics

- Missing disposition is unknown, not confirmed `DECLINED`; capability may
  reduce or evidence reconstruction/review may be required.
- Missing cost can preserve opportunity/revenue analysis, but gross-profit
  attribution is `PARTIAL` or unavailable. Margin is never inferred.
- Missing invoice can preserve opportunity detection, but recovered revenue or
  conversion attribution is unavailable.
- Missing phone/email does not invalidate detection; contactability is not
  assumed. Presence does not imply consent.
- Missing mileage disables or degrades mileage-dependent policy; supported
  date-based policy may remain available.
- Missing or ambiguous customer/vehicle identity quarantines the affected
  record for review; the adapter must not guess.

## Delivery and Replay Semantics

R1 recommends `FULL` snapshots only. A same-identity, unchanged delivery is an
idempotent no-op after integrity and mapping checks. A corrected delivery must
use a new `datasetVersion` and retain predecessor lineage; it is assessed as a
new version before controlled replacement. A changed `mappingVersion` creates a
new assessment/materialization lineage and must not silently rewrite prior
evidence. Opportunity materialization is keyed to canonical source lineage and
must not duplicate an opportunity for the same unchanged source evidence.

`DELTA` is reserved for a future compatible contract. It must define upserts,
deletes/tombstones, ordering, replay windows, and snapshot reconciliation
before implementation; this document does not implement delta processing.

## Validation and Materialization Contract

Validation stages are: envelope, schema, type/format, referential integrity,
tenant/dealer/location containment, business semantics, capability/readiness,
and materialization eligibility. Findings have severity `FATAL`, `ERROR`,
`WARNING`, or `INFO`.

| Severity | Default effect |
| --- | --- |
| `FATAL` | Reject dataset; no materialization. |
| `ERROR` | Quarantine affected record, or reject dataset when containment/integrity is compromised. |
| `WARNING` | Permit safe materialization where the affected capability is explicitly reduced. |
| `INFO` | Permit materialization and retain audit evidence. |

Readiness is capability-specific, not one opaque score. At minimum report
identity/linkage, opportunity detection, recommendation evidence, disposition,
invoice/revenue attribution, cost/gross-profit attribution, mileage policy,
and contactability as `AVAILABLE`, `PARTIAL`, `UNAVAILABLE`, or `REVIEW_REQUIRED`.
Materialization requires no fatal dataset finding, no unresolved containment
failure, and an explicit capability report. Quarantined records cannot create
opportunities.

## Contract Boundary

Tenant isolation, organization containment, least privilege, authorized
source/operator verification, checksum verification, redacted structured
logging, retention/deletion policy, quarantine access, and auditability are
mandatory design requirements. Legal/privacy contractual review is required
before partner data intake; this contract does not claim legal compliance.

The synthetic demo loader remains a demo-profile capability and is not an
implementation of this contract.