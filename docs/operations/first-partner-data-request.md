# First-Partner Controlled Data Request

**Status:** IMPLEMENTED request checklist for a controlled R1 pilot. It does
not request credentials, production access, or unnecessary personal data.

Provide one authorized, de-identified `FULL` export that conforms to
[AV-R1-CDF-001](../../contracts/r1-controlled-dealer-file-contract.md). Preserve
source IDs and relationships. AutoVision will validate the package before any
controlled materialization.

| Classification | Data requested | Effect when absent |
| --- | --- | --- |
| REQUIRED FOR ONBOARDING | Tenant-contained dealer and optional location IDs; source dataset ID/version; source system/schema/mapping versions; record IDs; service-order, customer, vehicle, job, and line relationships; service dates | The package cannot be safely contained, reconciled, or mapped. Invalid relationships quarantine affected records. |
| RECOMMENDED | Recommendations; declined/deferred source dispositions; advisor/service-note evidence; service history; invoice and invoice-line linkage; source timestamps | Detection coverage, disposition evidence, due/overdue interpretation, and conversion/revenue attribution are reduced. Missing disposition is unknown, never declined. |
| OPTIONAL / IMPROVES CAPABILITY | Cost amounts with currency; mileage; phone/email only where already authorized and minimized | Missing cost prevents complete gross-profit attribution. Missing contact data does not invalidate an opportunity and does not imply consent. |
| NOT REQUIRED FOR INITIAL PILOT | Credentials, tokens, passwords, cookies, raw DMS access, marketing consent, customer outreach history, DELTA feeds, provider-specific integration details | These are outside the controlled R1 FULL-file pilot. |

All monetary values need a currency and source basis. Do not include raw note
bodies, customer names, VINs, registrations, phone numbers, or email addresses
in operational tickets, logs, or status extracts. Transfer, retention, deletion,
and access authorization require the applicable security/privacy review before
partner data is handled.