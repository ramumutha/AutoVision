---
description: "Use when changing Spring Boot or Python backend APIs, domain services, authorization, persistence repositories, DTOs, validation, or integrations."
applyTo: "platform/src/**/*.java, api/**/*.py"
---
# Backend Instructions

- Keep controllers thin. Put business decisions in domain-oriented services and persistence behind repositories.
- Enforce tenant containment and authorization on the server for every read and mutation; client guards are not security controls.
- Use explicit request/response DTOs and validated contracts. Evolve APIs backward-compatibly unless a breaking change is approved and documented.
- Distinguish canonical domain ownership from read projections. Do not promote Service Profit customer/vehicle context into canonical master data without an architecture decision.
- Validate identifiers, state transitions, required fields, and negative paths. Return safe errors without leaking internals.
- Never log secrets, bearer tokens, cookies, credentials, or sensitive payloads.
- Preserve DMS-neutral naming and provider portability; do not embed demo-only or provider-specific shortcuts in production paths.
- Test success, validation, authorization, tenant-isolation, not-found, conflict, null, and partial-data behavior as applicable. See [backend/API standards](../../docs/engineering/backend-api-standards.md).
