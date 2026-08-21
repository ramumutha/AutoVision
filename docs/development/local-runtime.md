# Local Runtime

The examples below use Windows PowerShell from `D:\Projects\AutoVision`.

## Prerequisites

Install Docker Desktop with Compose, a supported JDK, the repository Maven wrapper, and Node.js/npm. Use the checked-in wrapper and the versions reflected by the project files rather than relying on a globally installed Maven version.

## Configure and start

```powershell
Set-Location D:\Projects\AutoVision
Copy-Item .env.example .env
```

Edit the ignored `.env` and replace every placeholder with local-only values. The compose runtime requires `POSTGRES_PASSWORD`, `KEYCLOAK_ADMIN_PASSWORD`, and `AUTOVISION_PLATFORM_CLIENT_SECRET`. The local browser proof additionally requires `AUTOVISION_LOCAL_DEV_USERNAME`, `AUTOVISION_LOCAL_DEV_PASSWORD`, `AUTOVISION_LOCAL_DEV_USER_REF_ID`, and `AUTOVISION_LOCAL_DEV_TENANT_ID`; the UUID values must identify an existing tenant and the intended user reference. Keep `.env` untracked.

Start the complete local runtime:

```powershell
docker compose up -d --build
docker compose ps
```

Wait for `postgres`, `keycloak`, and `platform` health to settle. The frontend is available at `http://localhost:8080`; Keycloak is available at `http://localhost:8081`.

Bootstrap or refresh the local user after the containers are ready. The script reads its values from the environment, so load the ignored file into the current PowerShell session according to your local tooling, then run:

```powershell
.\infra\keycloak\bootstrap\bootstrap-local-keycloak-user.ps1
```

That script upserts the platform `user_refs` row and the Keycloak user/claim mapping. It does not expose credentials in its output. Use the configured username and local password to sign in through the Angular application. The browser follows the Keycloak Authorization Code + PKCE flow and returns to Angular.

Validate the authenticated contract at `/api/v1/me` from the application session. An unauthenticated request is expected to return `401`; an authenticated, correctly mapped user returns `200`.

## Stop and restart

```powershell
docker compose down
docker compose up -d --build
docker compose ps
```

Use `docker compose down` for a normal stop. Do not casually delete the PostgreSQL volume or Keycloak realm state; they contain local database and identity state needed for repeatable development. A clean data reset is an intentional operation and should be agreed on before removing volumes.

Treat local credentials like real credentials: use unique local values, keep them in ignored files or a local secret store, and do not paste them into issues, logs, screenshots, or chat. See [troubleshooting](troubleshooting.md) when a health or authentication step fails.
