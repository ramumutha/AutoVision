# Troubleshooting

Use the first diagnostic at the boundary that owns the symptom. Never include Authorization headers, bearer tokens, passwords, cookies, or session data in logs, screenshots, issues, or chat.

| Symptom | Likely boundary | First diagnostics |
| --- | --- | --- |
| Frontend is not reachable | Docker/frontend | `docker compose ps frontend`; `docker compose logs frontend`; open `http://localhost:8080`. |
| Container is unhealthy | Container dependency or healthcheck | `docker compose ps`; `docker compose logs <service>`; check PostgreSQL and Keycloak dependencies before restarting the platform. |
| Sign in does not redirect | Angular OIDC runtime config | Inspect the non-secret `/assets/app-config.json` response, browser console, and Keycloak availability at `http://localhost:8081`. |
| Keycloak realm JSON appears instead of login | Wrong endpoint | The realm URL describes metadata. Start login through Angular; the OIDC authorization endpoint is under the realm's `protocol/openid-connect/auth` path and should not be hand-built with duplicate parameters. |
| Invalid or duplicate OIDC parameters | Client configuration or stale browser state | Clear the failed login state, inspect the single generated authorization request, and verify the public client, issuer, redirect URI, response type `code`, and PKCE settings. |
| Login returns to Angular but no session exists | Callback/token provider | Check the Angular callback route, `TokenProvider`, interceptor, and runtime OIDC configuration. Do not test by copying tokens out of browser storage. |
| `/api/v1/me` returns `401` | Missing/invalid JWT or issuer mismatch | Confirm the request came through same-origin `/api/`, inspect platform logs for issuer/JWT validation errors, and compare the configured runtime issuer. |
| `/api/v1/me` returns `403` | User claim or tenant mapping | Confirm the Keycloak user has `autovision_user_ref_id`, that it is a UUID, and that the active `public.user_refs` row exists. Run the bootstrap script rather than editing tokens. |
| `/api/v1/me` returns `502` or `504` | Nginx-to-platform path or platform health | `docker compose ps platform`; `docker compose logs platform frontend`; verify the platform healthcheck and `/api/` proxy target. |
| Platform JWT/JWK errors | Keycloak reachability or issuer/JWK configuration | Compare the public issuer (`localhost:8081`) with the internal JWK URI (`keycloak:8080`), then inspect platform and Keycloak logs without exposing tokens. |
| Missing `autovision_user_ref_id` | Keycloak profile/user bootstrap | Check `infra/keycloak/bootstrap/configure-user-profile.ps1`, the realm mapping, and the bootstrap output. The claim is managed identity configuration, not a frontend field. |
| Wrong or missing UserRef mapping | PostgreSQL/platform tenant boundary | Verify the UUIDs in ignored local configuration and run `scripts/verify-platform-db-boundary.ps1`; inspect the active `public.user_refs` mapping safely. |
| Tenant mismatch | UserRef-to-tenant data | Confirm the intended user reference and tenant UUID refer to the same local record. Do not bypass server-side authorization in Angular. |
| Keycloak bootstrap fails | Credentials, containers, or UUID inputs | Check `docker compose ps keycloak postgres`, required ignored environment values, existing tenant UUID, and the script's sanitized error. |
| PowerShell reports `NativeCommandError` with Docker | Native stderr handling | Read the command's native return code and sanitized diagnostics; Docker can write stderr while still succeeding. Use the repository script behavior as the reference instead of suppressing all errors. |
| Source change is missing in the browser | Stale frontend image | Rebuild with `docker compose up -d --build frontend`, then reload without cache. Confirm the changed file is included in the image build context. |
| CRLF/LF warning appears | Git line-ending normalization | Treat warnings separately from `git diff --check`. Run `git diff --check` and investigate only reported whitespace errors. |

For persistent failures, record the service, timestamp, safe error summary, and command used. Keep secrets and session material out of the report.
