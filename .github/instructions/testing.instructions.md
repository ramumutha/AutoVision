---
description: "Use when adding or changing unit, integration, API, security, responsive, accessibility, or end-to-end tests and fixtures."
applyTo: "**/*.spec.ts, frontend/e2e/**, platform/src/test/**, api/tests/**, api/test_*.py"
---
# Testing Instructions

- Use repository-configured commands and frameworks. For Angular, use `npm test`; do not call bare Vitest.
- Write behavior-oriented tests at the owning responsibility boundary. Cover success plus relevant failure, validation, security, tenant, null, partial-data, cancellation, and retry states.
- Keep fixtures deterministic, synthetic, minimal, and free of secrets. Do not weaken production behavior, authorization, validation, or types to make tests pass.
- Split specs by component/service/workflow responsibility. Review test files above 500 lines and decompose any test file above 700 lines.
- Run the narrowest discriminating test first, then the appropriate regression/build/E2E gate.
- Test responsive interaction and accessibility when user-visible behavior changes; assertions should verify semantics and outcomes, not fragile implementation detail.
- See [testing standards](../../docs/engineering/testing-standards.md) and the existing [validation workflow](../../docs/development/validation-and-git-workflow.md).
