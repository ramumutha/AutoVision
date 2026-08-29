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

For internal commercial verification validation, use a separate local operator
identity. Keep these values in the ignored `.env`; do not reuse the dealer/demo
identity or place credentials in source:

```dotenv
AUTOVISION_LOCAL_OPERATOR_USERNAME=verspen-commercial-operator
AUTOVISION_LOCAL_OPERATOR_PASSWORD=change-me-local-operator-password
AUTOVISION_LOCAL_OPERATOR_USER_REF_ID=00000000-0000-0000-0000-000000000000
AUTOVISION_LOCAL_OPERATOR_TENANT_ID=00000000-0000-0000-0000-000000000000
```

After the platform is healthy, run:

```powershell
.\infra\keycloak\bootstrap\bootstrap-local-commercial-operator.ps1
```

This creates or refreshes a distinct Keycloak user and assigns only the
system-scoped `VERSPEN_COMMERCIAL_OPERATOR` role with
`COMMERCIAL_ENQUIRY.OPERATE`. The ordinary local product user is not changed.

Validate the authenticated contract at `/api/v1/me` from the application session. An unauthenticated request is expected to return `401`; an authenticated, correctly mapped user returns `200`.

## Stop and restart

```powershell
docker compose down
docker compose up -d --build
docker compose ps
```

Use `docker compose down` for a normal stop. Do not casually delete the PostgreSQL volume or Keycloak realm state; they contain local database and identity state needed for repeatable development. A clean data reset is an intentional operation and should be agreed on before removing volumes.

Treat local credentials like real credentials: use unique local values, keep them in ignored files or a local secret store, and do not paste them into issues, logs, screenshots, or chat. See [troubleshooting](troubleshooting.md) when a health or authentication step fails.

## Service Profit R1 dealer demo

The Service Profit dealer demo is opt-in. It uses
`docker-compose.service-profit-demo.yml`; the normal `docker-compose.yml`
does not activate or mount synthetic demo data.

Set the following non-secret identity values in the ignored `.env` alongside
the required local passwords and client secret:

```dotenv
AUTOVISION_LOCAL_DEV_USERNAME=service-profit-demo-manager
AUTOVISION_LOCAL_DEV_USER_REF_ID=00000000-0000-4000-8000-000000000001
AUTOVISION_LOCAL_DEV_TENANT_ID=10000000-0000-4000-8000-000000000001
```

Validate and start the effective demo configuration:

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.service-profit-demo.yml `
  config

docker compose `
  -f docker-compose.yml `
  -f docker-compose.service-profit-demo.yml `
  up -d --build

docker compose `
  -f docker-compose.yml `
  -f docker-compose.service-profit-demo.yml `
  ps
```

The override activates the `service-profit-demo` Spring profile, enables the
existing bootstrap and load/materialization runner, and mounts
`demo-data/service-profit/r1` read-only at
`/opt/autovision/demo-data/service-profit/r1`. PostgreSQL health gates the
platform; Flyway completes during platform startup; the runner bootstraps the
organization and authorization state before loading readiness data and
materializing opportunities; frontend startup waits for platform health.

After the platform is healthy, load the ignored `.env` into the current
PowerShell process without printing values:

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable(
            $matches[1].Trim(),
            $matches[2],
            'Process'
        )
    }
}
```

Configure the managed Keycloak user-profile attribute before creating the
user, then run the user bootstrap:

```powershell
.\infra\keycloak\bootstrap\configure-user-profile.ps1 `
  -AdminPassword $env:KEYCLOAK_ADMIN_PASSWORD

.\infra\keycloak\bootstrap\bootstrap-local-keycloak-user.ps1
```

The profile script uses Keycloak's supported `users/profile` admin API because
Keycloak 26.7 does not accept a top-level `userProfile` object during realm
import. The user bootstrap upserts the same platform user reference configured
in the demo override and assigns its UUID to the Keycloak
`autovision_user_ref_id` user attribute. The realm mapper includes that
attribute as the `autovision_user_ref_id` access-token claim. Signing in through the Angular
Authorization Code + PKCE flow and receiving `200` from `/api/v1/me` proves
the normal claim-to-user-reference tenant resolution path. Do not enable
direct access grants or weaken JWT validation for demo automation.

Inspect startup and persisted state without exposing secrets. SP-DEMO-004 is
materialized from service job `50000000-0000-4000-8000-000000000004`, and
SP-DEMO-010 is materialized from service job
`50000000-0000-4000-8000-000000000008`:

```powershell
docker compose `
	-f docker-compose.yml `
	-f docker-compose.service-profit-demo.yml `
	logs platform

$verificationQuery = @'
SELECT
	COUNT(*) AS opportunity_count,
	COUNT(*) FILTER (WHERE status = 'SUPPRESSED') AS suppressed_count,
	COUNT(*) FILTER (WHERE currency_code = 'INR') AS inr_count
FROM platform.service_profit_opportunities
WHERE tenant_id = '10000000-0000-4000-8000-000000000001';

SELECT source_entity_id, status, actionability, suppression_reason
FROM platform.service_profit_opportunities
WHERE tenant_id = '10000000-0000-4000-8000-000000000001'
	AND source_entity_id IN (
			'50000000-0000-4000-8000-000000000004',
			'50000000-0000-4000-8000-000000000008'
	)
ORDER BY source_entity_id;
'@

docker exec autovision-postgres psql `
	-U autovision -d autovision -x -c $verificationQuery
```

An ordinary restart must retain exactly ten opportunities and reuse the
existing readiness assessment and opportunity keys:

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.service-profit-demo.yml `
  restart platform

docker compose `
  -f docker-compose.yml `
  -f docker-compose.service-profit-demo.yml `
  ps
```

Use `http://localhost:8080/service-profit` for the authenticated manager UI.
Unauthenticated requests to
`/api/v1/service-profit/opportunities/summary` remain rejected. The browser
continues to use the public `autovision-web` client with Authorization Code +
PKCE and same-origin `/api` proxying.

For a deliberate factory reset only, stop the stack and explicitly remove its
volumes before starting again. Volume removal is not part of normal demo
restart:

```powershell
docker compose `
	-f docker-compose.yml `
	-f docker-compose.service-profit-demo.yml `
	down --volumes
```
