# AutoVision Engineering Constitution

Read [README.md](../README.md), [docs/README.md](../docs/README.md), and the applicable path instructions before editing. Inspect the bounded module and its authoritative product/architecture docs first.

## Working Agreement

- Verify branch, HEAD, status, and the requested change boundary. Preserve user-owned uncommitted work.
- Make the smallest justified change. Do not stage, commit, push, reset, or stash unless explicitly requested.
- Use domain-oriented names and cohesive responsibilities. Reuse proven abstractions, but do not create speculative generic frameworks.
- Preserve the modular monolith, DMS-neutral contracts, provider portability, and frozen ServiceOrder, ServiceJob, and ServiceLine terminology.
- Quote, Appointment, ServiceOrder, and Invoice remain core DMS transactions. Inspection/AI is optional; AfterSalesCase is durable case context; CustomerAuthorization is an auditable business record.
- ServiceLine belongs to ServiceOrder and may optionally belong to ServiceJob. Do not conflate lifecycle timestamps with mechanic time clocking.
- Server authorization and tenant isolation are authoritative. Never weaken security to make a test pass.
- Never place secrets, credentials, tokens, cookies, or sensitive payloads in source, fixtures, documentation, logs, or responses.
- Evolve APIs and persisted contracts backward-compatibly unless an approved breaking change and migration plan exist.
- Do not add demo-only shortcuts to production paths or dependencies without a demonstrated need.

## Quality Bar

- Test changed behavior, including validation, failure, authorization, null, partial-data, and cancellation paths as relevant. Use repository-configured commands.
- Keep interfaces keyboard accessible, semantically structured, localized, responsive, and usable on mobile. Prefer progressive disclosure over compressed desktop layouts.
- Keep business calculations out of templates and authorization decisions out of clients.
- Update functional, architecture, workflow, security, and ownership documentation when their behavior changes. Documentation is part of Definition of Done.
- Preferred sizes and rationale are in [coding standards](../docs/engineering/coding-standards.md). Review production and test files above 500 lines; strongly prefer decomposition above 700 lines; decompose test files above 700 lines. No production file may exceed 2000 lines.

See [AGENTS.md](../AGENTS.md) for the operational checklist and [docs/engineering](../docs/engineering/) for human-readable standards.