# System Overview

AutoVision is an adaptive vehicle service intelligence platform. The current enterprise runtime is a containerized modular platform with an Angular browser application, a Java/Spring Boot backend, PostgreSQL, and Keycloak identity.

## Runtime boundaries

| Area | Current responsibility |
| --- | --- |
| Angular | Browser experience using IBM Carbon Design System and AutoVision theming, feature workflows, OIDC login, and same-origin API calls. |
| Java/Spring Boot | Core platform APIs, domain workflows, authorization, tenant-aware persistence, and observability hooks. |
| PostgreSQL | Platform data, including tenant and user-reference mapping. |
| Keycloak | OIDC issuer, realm, public SPA client, and identity attributes. |
| Python/FastAPI | Retained boundary for AI/ML, prediction, and vision services only; it is not the core platform backend. |
| Integration boundary | DMS-neutral contracts and adapters, with event-driven integration where appropriate. |

The local browser enters through `http://localhost:8080`. Nginx serves Angular and proxies `/api/` to the platform, keeping browser API traffic same-origin. The platform validates the JWT and resolves the authenticated tenant context before domain access.

## Ownership principles

The backend is domain-oriented and modular. Keep controllers thin, put business decisions in services, keep persistence behind repositories, and make DTOs/contracts explicit. The frontend separates `core` cross-cutting concerns, `features` domain workflows, `shared` reusable UI, and `shell` application composition.

Server-side authorization is authoritative. Tenant isolation is a data and authorization concern, not a client-side routing feature. User identity reaches the platform through a managed `UserRef` mapping rather than direct coupling to a DMS or UI implementation.

## Direction

Observability should remain structured and correlation-aware across the browser proxy, platform, identity boundary, and integrations. Contracts should remain portable across DMS, OEM, and AI providers. Do not introduce provider-specific domain coupling or new services without an explicit architecture decision.

See [authentication and tenant context](authentication-and-tenant-context.md) and [repository map](repository-map.md) for the controlling boundaries.
