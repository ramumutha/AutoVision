# Authentication and Tenant Context

The established local authenticated flow is:

```text
Browser -> Angular -> Keycloak -> Authorization Code + PKCE
        -> Angular callback -> TokenProvider/interceptor -> same-origin /api
        -> nginx -> Spring resource server JWT validation
        -> autovision_user_ref_id -> TenantContextResolver -> public.user_refs
        -> tenant context
```

## Identity contract

- Keycloak realm: `autovision`.
- Browser client: `autovision-web`, a public SPA client.
- Flow: Authorization Code with PKCE. The Angular configuration keeps PKCE enabled and does not use a client secret.
- Runtime issuer: `http://localhost:8081/realms/autovision` locally.
- Platform JWK discovery uses the internal container URI `http://keycloak:8080/realms/autovision/protocol/openid-connect/certs`; this is for platform-to-Keycloak traffic, not a browser URL.
- Angular reads runtime OIDC configuration from `/assets/app-config.json`, obtains tokens through its OIDC adapter and `TokenProvider`, and the auth interceptor attaches them to API requests.
- Nginx proxies same-origin `/api/` requests to the platform. The browser should call `/api`, not a separately exposed platform origin.

Spring resource-server JWT validation checks the configured issuer and JWK set. After authentication, `TenantContextResolver` reads the managed `autovision_user_ref_id` claim, parses it as a UUID, and looks up an active row in `public.user_refs`. That row supplies the canonical user reference, tenant, and external identity used for tenant-aware authorization.

## Acceptance endpoint

`GET /api/v1/me` is the lightweight runtime proof:

- no valid authentication: `401`;
- authenticated identity with a valid active mapping: `200`;
- missing, malformed, inactive, or unmapped user reference: authorization failure, normally `403`.

Angular guards improve navigation and user experience, but they are not a security boundary. Authorization and tenant isolation must remain enforced by the platform.

## Configuration ownership

Keycloak realm/client and user-profile configuration belongs under `infra/keycloak`. Angular runtime OIDC configuration belongs under `frontend/src/app/core/auth` and its runtime asset configuration. Issuer and JWK settings belong to the platform security configuration and compose environment. User-reference and tenant resolution belong under `platform/src/main/java/com/autovision/platform/tenant`.

Never document or commit passwords, bearer tokens, cookies, or client secrets. See [local runtime](../development/local-runtime.md) for the safe setup flow.
