# AutoVision Agent Working Agreement

This checklist complements the repository constitution in [.github/copilot-instructions.md](.github/copilot-instructions.md). Human-readable standards live under [docs/engineering](docs/engineering/).

## Before Work

1. Verify the requested branch, baseline/HEAD, `git status --short`, and staged state.
2. Read `README.md`, `docs/README.md`, applicable `.github/instructions`, and the nearest product/architecture document.
3. Inspect the bounded owner and a relevant test or call site before editing.
4. Preserve user-owned changes. Never reset, stash, discard, stage, commit, or push unless explicitly requested.
5. Declare the primary Work Mode (`DISCOVERY`, `PRODUCT`, `UX`, `ARCHITECTURE`, `IMPLEMENTATION`, `TESTING`, `DEVOPS`, or `RELEASE`) and keep all standing security, architecture, Git, accessibility, documentation, and validation rules active.

## During Work

1. State the change boundary and keep edits inside it.
2. Search for the nearest established equivalent before creating a new abstraction, dependency, component, contract, persistence object, migration, or infrastructure construct.
3. Implement the smallest behaviorally complete change at the owning responsibility.
4. Reuse proven domain abstractions and presentations; do not build speculative generic frameworks.
5. Keep code, tests, contracts, and documentation readable without agent assistance.
6. Protect tenant isolation, server-authoritative authorization, secrets, API compatibility, accessibility, localization, and responsive behavior.

## Before Completion

1. Run focused tests first, then applicable regression, build, database, responsive, accessibility, or E2E gates.
2. Run `git diff --check` and review every changed/untracked path.
3. Report production and test file sizes; review files above 500 lines and decompose tests above 700 lines.
4. Search for unintended duplication when adding a reusable presentation, contract, query, or domain rule.
5. Assess documentation impact and update authoritative docs when behavior or boundaries changed.
6. Report changed files, validation results, residual risks, and staged state. Do not commit unless requested.
