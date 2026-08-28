# First Dealer Data Mapping Worksheet

**Status:** PLANNED reusable mapping worksheet for the first R1 pilot

Use one row per AutoVision field or relationship. Do not invent canonical
fields. If the implementation or [AV-R1-CDF-001](../../contracts/r1-controlled-dealer-file-contract.md)
does not define a field, write `NOT CURRENTLY MODELED` and record the business
need separately.

## Mapping Rules

- Preserve dealer source IDs and relationship keys; do not guess identity from contact fields.
- Tenant, receipt time, processing correlation, digest, byte size, and actual counts are AutoVision-controlled.
- Use `REQUIRED`, `OPTIONAL`, or `NOT REQUIRED FOR FIRST PILOT` for pilot scope.
- Use `PII`, `NON-PII`, or `POTENTIALLY SENSITIVE` for handling classification.
- Use `CONFIRMED`, `PROPOSED`, `REVIEW REQUIRED`, or `NOT MAPPED` for mapping status.
- Record source examples as synthetic or abstract examples; do not paste customer data.

## Worksheet

| AutoVision Entity | AutoVision Field | Business Meaning | Required/Optional | Dealer Source System | Dealer Table/Object | Dealer Field | Dealer Data Type | Example Value | Transformation | Source Status Mapping | Null/Unknown Semantics | Source Identifier | PII Classification | Validation Rule | Mapping Status | Dealer Confirmation | AutoVision Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| organization | dealerId | Contained dealer scope | REQUIRED |  |  |  | UUID/string |  | Normalize only |  | Missing rejects containment | Yes | NON-PII | Must belong to tenant |  |  |  |
| organization | locationId | Optional contained branch | OPTIONAL |  |  |  | UUID/string |  | Normalize only |  | Missing means dealer scope | Yes | NON-PII | Must belong to dealer |  |  |  |
| customer | sourceRecordId | Stable customer source identity | REQUIRED |  |  |  | string |  | Preserve |  | Missing quarantines affected records | Yes | POTENTIALLY SENSITIVE | Non-blank, unique in dataset |  |  |  |
| vehicle | sourceRecordId | Stable vehicle source identity | REQUIRED |  |  |  | string |  | Preserve |  | Missing quarantines affected records | Yes | POTENTIALLY SENSITIVE | Non-blank, unique in dataset |  |  |  |
| serviceOrder | sourceRecordId | Stable visit/order identity | REQUIRED |  |  |  | string |  | Preserve |  | Missing quarantines affected records | Yes | NON-PII | Non-blank, unique in dataset |  |  |  |
| serviceOrder | customerSourceRecordId | Order-to-customer relationship | REQUIRED |  |  |  | string |  | Preserve |  | Missing or broken link quarantines | Yes | POTENTIALLY SENSITIVE | Resolves exactly once |  |  |  |
| serviceOrder | vehicleSourceRecordId | Order-to-vehicle relationship | REQUIRED |  |  |  | string |  | Preserve |  | Missing or broken link quarantines | Yes | POTENTIALLY SENSITIVE | Resolves exactly once |  |  |  |
| serviceOrder | serviceDate | Source service event date | REQUIRED |  |  |  | date |  | Parse with timezone policy |  | Blank is invalid for order | No | NON-PII | Valid source date |  |  |  |
| serviceJob | sourceRecordId | Stable job identity | REQUIRED |  |  |  | string |  | Preserve |  | Missing quarantines job | Yes | NON-PII | Non-blank, unique in dataset |  |  |  |
| serviceLine | sourceRecordId | Stable service-line identity | REQUIRED |  |  |  | string |  | Preserve |  | Missing quarantines line | Yes | NON-PII | Non-blank, unique in dataset |  |  |  |
| recommendation | sourceRecordId | Stable recommendation identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing reduces recommendation evidence | Yes | POTENTIALLY SENSITIVE | Unique when present |  |  |  |
| recommendation | description | Recommended work explanation | OPTIONAL |  |  |  | text |  | Bound and minimize |  | Missing reduces explanation | No | POTENTIALLY SENSITIVE | Bounded text |  |  |  |
| disposition | sourceRecordId | Stable disposition identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing means unknown | Yes | NON-PII | Unique when present |  |  |  |
| disposition | disposition | Source disposition fact | OPTIONAL |  |  |  | enum/string |  | Map only when confirmed | Blank/unknown remains unknown | Never default to DECLINED | No | POTENTIALLY SENSITIVE | Controlled or review-required mapping |  |  |  |
| serviceHistory | sourceRecordId | Stable historical event identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing reduces history capability | Yes | NON-PII | Unique when present |  |  |  |
| serviceHistory | serviceDate | Historical service date | OPTIONAL |  |  |  | date |  | Parse with timezone policy |  | Invalid date is quarantined | No | NON-PII | Valid date |  |  |  |
| invoice | sourceRecordId | Stable invoice identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing removes invoice linkage | Yes | NON-PII | Unique when present |  |  |  |
| invoiceLine | sourceRecordId | Stable invoice-line identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing reduces linkage | Yes | NON-PII | Unique when present |  |  |  |
| cost | sourceRecordId | Stable cost identity | OPTIONAL |  |  |  | string |  | Preserve |  | Missing prevents complete GP attribution | Yes | POTENTIALLY SENSITIVE | Unique when present |  |  |  |
| cost | amount/currency | Source cost basis | OPTIONAL |  |  |  | decimal/code |  | No inference | Missing cost stays unavailable | No | POTENTIALLY SENSITIVE | Non-negative amount and explicit currency |  |  |  |
| customer | displayName/phone/email | Context or contactability | NOT REQUIRED FOR FIRST PILOT |  |  |  | text |  | Minimize or omit | Presence is not consent | Omit unless separately approved | No | PII | Approved purpose and format required |  |  |  |

## Level Decisions

### Level 1 - Minimum Detection Data

Request contained dealer/location scope, stable source IDs, customer and vehicle
identity keys, service-order identity and date, order-to-customer and
order-to-vehicle relationships, service job/line identity and relationships,
and recommendation/disposition evidence where the dealer has it. These support
useful detection while preserving source uncertainty.

### Level 2 - Enhanced Explanation Data

Request service history, recommendation descriptions and timestamps, source
disposition and reason, mileage, bounded notes only if approved, advisor/source
context, service status, and additional relationship keys. These improve
explanation, prioritization, and capability disclosure but are not all required
for detection.

### Level 3 - Outcome / Commercial Data

Treat invoice and invoice-line linkage, quote, completion outcomes, cost,
currency, gross profit, appointment linkage, and later outcome evidence as
optional later work. Level 3 is not mandatory for the first detection pilot.

## Status and Disposition Worksheet

| Dealer Source Value | Dealer Meaning | AutoVision Meaning | Mapping Confidence | Dealer Confirmed | Notes |
| --- | --- | --- | --- | --- | --- |
|  | recommended | `RECOMMENDATION` evidence |  |  |  |
|  | declined | `DECLINED_WORK` only when confirmed |  |  |  |
|  | deferred | `DEFERRED_WORK` only when confirmed |  |  |  |
|  | approved | Source approval fact; not completion |  |  |  |
|  | completed | Completion/suppression evidence if link is proven |  |  |  |
|  | cancelled | Source cancellation; do not infer outcome |  |  |  |
|  | unknown/blank | `UNKNOWN` |  |  |  |
|  | duplicate | Duplicate source condition; preserve lineage |  |  |  |
|  | already completed | Suppression evidence if link is proven |  |  |  |
|  | not applicable | Source meaning requires confirmation |  |  |  |

Ambiguous values are `REVIEW REQUIRED`. Source disposition is distinct from any
future AutoVision follow-up disposition.

## Data-Quality Profile

Complete on the first sample:

- total records by `StagedRecordType`;
- missing, duplicate, and invalid source identifiers;
- broken customer, vehicle, order, job, line, recommendation, and invoice links;
- unknown dispositions, invalid or future dates, missing mileage, invalid amounts;
- currency consistency and distance units;
- tenant/dealer/location containment;
- accepted, rejected, and quarantined records;
- finding severity and stable validation codes;
- capability status for detection, recommendation evidence, disposition,
  revenue attribution, gross-profit attribution, mileage policy, and contactability.

Do not invent production thresholds. Mark thresholds `REQUIRES PILOT EVIDENCE`
until a representative sample and owner-approved decision establish them.

## Capability Translation

Use the implemented capability statuses `AVAILABLE`, `PARTIAL`, and
`UNAVAILABLE`. Use `REVIEW REQUIRED` for unresolved mapping or evidence that
cannot safely be represented by the current status vocabulary. Report separately:

- declined-work detection;
- deferred-work detection;
- recommendation detection;
- due/overdue service;
- revenue estimation or attribution;
- gross-profit estimation or attribution;
- customer contact availability.

Contact availability is not consent. Potential is not realized revenue.
