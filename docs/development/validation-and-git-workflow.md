# Validation and Git Workflow

Keep validation proportional to the change. Start with the narrowest check that can disprove the current hypothesis, then use the broader gate at a freeze or integration boundary.

## Working sequence

1. Check the baseline with `git status --short`, `git branch --show-current`, and `git diff --check`.
2. Inspect the bounded module and run focused tests or a focused build first.
3. At a defined freeze boundary, run the platform and frontend quality gates.
4. Review the diff, stage explicit files when requested, and verify the tree and remote state.

Existing entry points:

- `scripts/dev/validate-platform.ps1` runs the quiet platform test summary and diff check.
- From `frontend/`, `npm run quality` runs Angular unit tests, build, and Playwright end-to-end checks.
- `scripts/verify-platform-db-boundary.ps1` checks PostgreSQL schema and migration ownership.
- `scripts/dev/git-review.ps1` reviews the working tree and unstaged diff.
- `scripts/dev/staged-review.ps1` reviews only explicitly staged content.
- `scripts/dev/post-commit-check.ps1` reports status, the new commit, and recent history after a commit.

For compose changes, run `docker compose config` before startup. For every slice, run `git diff --check`; CRLF/LF warnings are not the same as a real whitespace failure. The scripts keep successful output concise and surface detailed diagnostics only on failure.

Do not use `git add .` for controlled slices when explicit file staging is safer. Do not stage, commit, or push unless the task explicitly asks for it. A completed change should have a clean working tree after the intended commit and should be checked against its remote branch before handoff.

PowerShell examples in repository documentation should not close the terminal; allow errors to remain visible to the developer and use the existing scripts as the operational authority.
