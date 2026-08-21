# AutoVision Documentation

Use this index to find the operating contract for the current repository.

## Architecture

- [System overview](architecture/system-overview.md): current runtime architecture, boundaries, and design principles.
- [Repository map](architecture/repository-map.md): ownership of top-level modules and where common changes belong.
- [Authentication and tenant context](architecture/authentication-and-tenant-context.md): the Keycloak, Angular, Spring Security, and tenant mapping contract.

## Development

- [Local runtime](development/local-runtime.md): prerequisites, environment setup, compose startup, login, and shutdown.
- [Validation and Git workflow](development/validation-and-git-workflow.md): focused checks, quality gates, and review discipline.
- [Troubleshooting](development/troubleshooting.md): symptom-to-boundary diagnostics for local runtime and authentication failures.

Start with the repository map when you are unsure where a change belongs. Start with the local runtime runbook when the environment is not running. For identity or authorization behavior, read the authentication contract before inspecting implementation details.
