# First-Partner Controlled Data Request

**Status:** IMPLEMENTED request checklist for a controlled R1 pilot. It does
not request credentials, production access, or unnecessary personal data.

Use this request after the [first dealer discovery guide](first-dealer-discovery-guide.md)
and record decisions in the [data mapping worksheet](first-dealer-data-mapping-worksheet.md).
The [pilot acceptance checklist](first-dealer-pilot-acceptance-checklist.md)
defines the evidence required before materialization.

Provide one authorized, de-identified `FULL` export that conforms to
[AV-R1-CDF-001](../../contracts/r1-controlled-dealer-file-contract.md). Preserve
source IDs and relationships. AutoVision will validate the package before any
controlled materialization.

| Level | Classification | Data requested | Effect when absent |
| --- | --- | --- | --- |
| Level 1 - Minimum detection | REQUIRED | Tenant-contained dealer and optional location IDs; source dataset ID/version; source system/schema/mapping versions; stable source IDs; customer, vehicle, service-order, service-job, and service-line relationships; service dates; recommendation/disposition evidence where available | The package cannot be safely contained, reconciled, or mapped without the identity and relationship fields. Invalid relationships quarantine affected records. Missing disposition reduces evidence but remains unknown, never declined. |
| Level 2 - Enhanced explanation | OPTIONAL / RECOMMENDED | Recommendation descriptions and timestamps; confirmed declined/deferred source dispositions and reasons; bounded service context; service history; mileage; source timestamps; advisor/source context; completion/cancellation semantics | Detection may remain available while explanation, prioritization, due/overdue interpretation, and capability disclosure are reduced. Ambiguous values require review. |
| Level 3 - Outcome / commercial | OPTIONAL / NOT REQUIRED FOR INITIAL PILOT | Quote, invoice and invoice-line linkage; labor, parts, discounts, tax, currency, cost, gross profit; appointment, completion, and later outcome linkage | These support later outcome, revenue, and gross-profit analysis. Missing cost never permits inferred margin. Level 3 is not required for first detection. |
| Excluded from first pilot | NOT REQUIRED FOR INITIAL PILOT | Credentials, tokens, passwords, cookies, raw DMS access, marketing consent, customer outreach history, DELTA feeds, provider-specific integration details, or customer communication data | These are outside the controlled R1 FULL-file pilot. |

All monetary values need a currency and source basis. Do not include raw note
bodies, customer names, VINs, registrations, phone numbers, or email addresses
in operational tickets, logs, or status extracts. Transfer, retention, deletion,
and access authorization require the applicable security/privacy review before
partner data is handled.