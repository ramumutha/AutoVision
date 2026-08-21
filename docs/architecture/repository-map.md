# Repository Map

Use this map to choose an ownership boundary before editing. The current enterprise runtime is `frontend/` plus `platform/`, backed by `infra/` and PostgreSQL through `docker-compose.yml`.

| Path | Ownership and purpose |
| --- | --- |
| `platform/` | Current Java/Spring Boot core platform: domain modules, security, tenant context, workflow, persistence, and platform tests. |
| `frontend/` | Current Angular application, Carbon-based UI foundations, OIDC auth, shell, and feature workflows. |
| `infra/` | Runtime infrastructure, especially Keycloak realm configuration and bootstrap scripts. |
| `contracts/` | Shared contract area; currently sparse, so add portable integration contracts only with an explicit ownership decision. |
| `scripts/` | Developer validation and operational checks. `scripts/dev/` contains the review and platform validation entry points. |
| `docs/` | Repository-owned architecture and developer operating contract. |
| `api/` | Retained Python/FastAPI AI, prediction, evidence, and related POC/support modules. It is not the current enterprise core backend. |
| `worker/` | Retained support area; currently empty. Do not assume it owns current platform processing. |
| `web/` | Historical Next.js/POC application retained in the repository. Do not extend it for new core platform work unless explicitly requested. |
| `prompts/`, `safety-rules/`, `evals/`, `intelligence-packs/` | AI/ML, evaluation, prompt, and policy-support areas. Treat their ownership as separate from the Spring core and verify the local contract before changing them. |

## Common destinations

- Spring security: `platform/src/main/java/com/autovision/platform/security`
- Tenant identity: `platform/src/main/java/com/autovision/platform/tenant`
- Angular auth: `frontend/src/app/core/auth`
- Service-order UI: `frontend/src/app/features/service-orders`
- Keycloak config: `infra/keycloak`
- Runtime validation: `scripts/dev`
- Architecture docs: `docs/architecture`

When a change crosses these boundaries, update the relevant contract and tests. Do not rename current source directories as part of routine work.
