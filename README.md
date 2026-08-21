# AutoVision

AutoVision is an adaptive vehicle service intelligence platform. The current
enterprise runtime combines an Angular application, a Java/Spring Boot core
platform, PostgreSQL, and Keycloak identity. Python/FastAPI remains a boundary
for AI/ML, prediction, and vision services; it is not the core backend.

## Repository at a glance

- `frontend/`: Angular browser application and feature workflows.
- `platform/`: Java/Spring Boot domain APIs, security, tenant context, and persistence.
- `infra/`: Keycloak realm and local runtime configuration.
- `contracts/`: portable integration contract area.
- `scripts/`: validation and developer workflow checks.
- `api/`, `web/`, `worker/`, and AI support directories: retained POC/support areas; see the [repository map](docs/architecture/repository-map.md).

## Local runtime

The containerized local entry point is [http://localhost:8080](http://localhost:8080).
Keycloak is available at [http://localhost:8081](http://localhost:8081). The
authenticated proof endpoint is `/api/v1/me`.

For prerequisites, environment setup, local-user bootstrap, login, and
shutdown, follow the [local runtime runbook](docs/development/local-runtime.md).

## Documentation

Start at the [documentation index](docs/README.md). It links the architecture
contract, ownership map, authentication and tenant model, validation workflow,
and troubleshooting guide.

## Prerequisites

Docker Desktop with Compose, a supported JDK, the checked-in Maven wrapper, and
Node.js/npm are required for the current runtime and validation workflows.

Do not commit `.env`, credentials, tokens, or other local secrets. See
[SECURITY.md](SECURITY.md) for repository handling rules.
