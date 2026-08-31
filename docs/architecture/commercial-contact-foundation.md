# Commercial Contact Foundation

**Status: IMPLEMENTED (R1-COMMERCIAL-1A foundation; R2 structured intake extension; R4 UX validation)**

This record governs the public commercial entry point without creating a CRM or
bypassing the authenticated platform boundary.

## Public Entry

`Contact Us` is the universal public commercial CTA. `Request a Demo` remains a
contextual shortcut from relevant product surfaces and opens the same Contact
Us route with the approved `PRODUCT_DEMO` purpose and `SERVICE_PROFIT_AI`
product preselected:

`/contact?purpose=product-demo&product=service-profit-ai`

The website validates query values against this allow-list and defaults to
`GENERAL_ENQUIRY` for arbitrary values. The form submits to the exact public
platform API below, through the website Nginx proxy to the internal platform
service.

The initial contact experience is intentionally concise: purpose, contact and
company details, product or service interest, evaluation preference where
useful, and a short message. Dealer-pilot qualification is progressive and
optional; DMS, volume, location, and data-readiness questions are later-stage
qualification rather than a normal first-contact burden. The richer V42
qualification contract remains available for compatible pilot submissions.

## Purpose Model

The approved authoritative purpose values are:

- `PRODUCT_DEMO`
- `ADVISORY_IMPLEMENTATION`
- `PARTNERSHIP`
- `GENERAL_ENQUIRY`

Only `PRODUCT_DEMO` may later enter demo qualification. A product demo must
use the approved `AUTOVISION` / `SERVICE_PROFIT_AI` product pair. Advisory
interest may use `AUTOMOTIVE_TECHNOLOGY_ADVISORY`,
`AI_DIGITAL_TRANSFORMATION`, `INTEGRATION_IMPLEMENTATION`, or `OTHER`.
Purpose-specific messages are explanatory text and do not replace the enum.

## Domain Contract

The platform model is `CommercialEnquiry`, stored outside tenant tables because
it is a public commercial record rather than tenant data. It stores identity,
company, normalized business email, role, market, message, email classification,
approved product/advisory enums, timestamps, and status. Its lifecycle is
`VERIFICATION_PENDING`, `VERIFIED`, `UNDER_REVIEW`, `QUALIFIED`, `CLOSED`, and
`REJECTED`; non-demo enquiries do not pass through demo-specific states.

Migration `V39__create_commercial_enquiry_foundation.sql` creates the enquiry
and verification tables. Verification records store only a SHA-256 token
digest, expiry, one-time consumption time, and bound enquiry ID.

Migration `V42__extend_commercial_enquiry_qualification.sql` adds the
non-null `qualification` JSONB object. It stores canonicalized, purpose-specific
collections and optional qualification values while preserving the original
singular product and advisory columns for backward compatibility. The website
and service trim, deduplicate, and sort submitted collections; unsupported
product and evaluation values are rejected before persistence. The JSONB field
is additive and does not change the public verification or operator lifecycle.

The public contract is:

- `POST /api/v1/public/commercial-enquiries` returns `202` and a safe ID/status response.
- `GET /api/v1/public/commercial-enquiries/verify?token=...` consumes a valid token once and returns a safe status.

There is no public enquiry listing or read endpoint.

The future public API must be limited to the exact intake and verification
endpoints, with bounded DTOs, normalization, safe duplicate handling,
rate/abuse protection, safe errors, and security-safe logging. The existing
platform API remains authenticated by default; no broad anonymous access is
approved.

## Verification and Routing

Business email verification must use a cryptographically strong, one-time,
expiring token bound to the enquiry and stored hashed where practical. Tokens
must never be logged or exposed in ordinary responses. Free-mail domains still
require verification and manual review; they are not automatically rejected.

After verification, a Product Demo enquiry requires human qualification and
approval before either a guided demo or evaluator access. Advisory,
partnership, and general enquiries route to manual business follow-up and never
provision a demo tenant, entitlement, or identity automatically.

## Demo Controls (RESEARCH REQUIRED)

Evaluator access must provision an isolated `DEMO` tenant from the existing
versioned, synthetic Service Profit golden dataset. Dealer tenants cannot share
mutable state. Invitations must be enquiry/contact-specific, one-time,
non-guessable, and time-bound. Invitation activation expiry, demo access
expiry, and requested meeting time are separate values.

A future `ResetDemoTenant(tenantId)` operation must fail closed unless the
server-authoritative tenant classification is `DEMO`; `PILOT`, `PRODUCTION`,
and `UNKNOWN` are denied. Reset must preserve other tenants and the immutable
golden source. Keycloak remains the sole password owner and must perform
password activation.

## Privacy and Operations

The form copy states the permitted response/demonstration/service-use purpose
and does not imply marketing consent. Final privacy wording requires legal
review. R1 operator handling may use secure APIs or scripts for review,
qualification/rejection, approval, provisioning, reset, expiry, and revocation;
a CRM and marketing automation are out of scope.

## Current Gaps

- Production email provider: **DEFERRED**.
- Commercial audit/event integration: **MISSING**; no unrelated audit subsystem was invented for this gate.
- Distributed rate limiting across multiple platform instances: **DEFERRED**.
- Demo evaluator provisioning, invitation, expiry, and reset: **MISSING**.
- Browser responsive and semantic DOM review: **IMPLEMENTED** for the private/local R2 candidate; assistive-technology and formal contrast review: **DEFERRED**.

The local/test delivery implementation keeps only the most recent generated
verification link in bounded process memory and never logs it. Local Compose
enables the explicit `local` profile and provides the link through the exact
development-only route `/internal/dev/commercial-verifications/latest`. The
route remains authenticated by the default platform security policy and is
available only as a local operator inspection boundary; it is not anonymous
and is not covered by a broad `/internal/dev/**` permit rule. The route and
bean are unavailable under the `production` profile; production uses a
deliberately unconfigured delivery implementation that fails closed.
Submission validation, duplicate suppression, and per-client hourly throttling
provide application-level abuse protection; distributed production rate
limiting remains a deployment concern.

The R1 duplicate policy suppresses the same normalized email, purpose, and
Product Demo product combination for seven days with a deterministic `429 Too
Many Requests` response. Future enquiries remain possible after that window.
For R2/R3 structured qualification, duplicate identity also includes the
canonical qualification object. Collection values are trimmed, validated
against their allow-lists, deduplicated, and sorted before comparison, so
checkbox ordering cannot bypass suppression while materially different intent
remains distinct. Legacy rows with the V42 default empty object remain valid
and continue to participate through their preserved singular fields.
The in-process throttle allows ten submissions per client address per hour and
returns `429 Too Many Requests` for the eleventh request. Processing order is
rate-limit check, purpose validation, duplicate check, persistence, then local
verification capture. Invalid payloads are rejected before business processing
by Jakarta validation.

The controller uses the servlet `remoteAddr` and does not trust arbitrary
`X-Forwarded-For` or `X-Real-IP` headers. In local Compose this is the website
Nginx proxy address, so the throttle is intentionally shared by requests
through that proxy. A production deployment with multiple trusted proxies
must provide a trusted proxy address-resolution policy or distributed limiter
before horizontal scaling; no forwarded header is treated as authoritative by
this foundation.

R1-COMMERCIAL-1C live validation confirmed anonymous submission returns `202`,
duplicate and throttle service paths return `429` in focused tests, the local
verification link can be consumed once, replay and expiry are rejected, and
unrelated APIs remain `401` anonymously. Earlier live `401` observations were
caused by malformed JSON in the PowerShell/curl diagnostic command, confirmed
by the platform JSON parser log; correctly encoded requests exercise the public
submission matcher. Public service rejections return bounded error identifiers;
unexpected controller failures return `500 REQUEST_FAILED` without exception or
database details.

R1-COMMERCIAL-1D confirmed that the earlier successful live verification used
the plaintext link from a historical Nginx access-log entry created before
verification-route logging was disabled. It was not obtained from the local
retrieval endpoint. That historical exposure was local development evidence,
not production evidence. Current exact verification routes have access logging
disabled. The approved local operator flow is to submit through the public
endpoint, retrieve the in-memory link through the authenticated local operator
route, then open the link; no production email provider or plaintext database
token is required.

The website presents idle, submitting, success, and safe failure states. It does
not claim that an email was delivered; it asks the user to check email for
verification after the API accepts the enquiry.
