---
description: "Use when changing the Angular frontend, components, templates, styles, routing, localization, responsive behavior, or browser state."
applyTo: "frontend/**/*.ts, frontend/**/*.html, frontend/**/*.scss, frontend/**/*.css"
---
# Frontend Instructions

- Follow the existing standalone Angular, `OnPush`, signals, and RxJS patterns. Use `switchMap` when a newer request must cancel stale work.
- Keep components cohesive: containers own orchestration, display components own presentation, and API services own transport. Reuse the shared design system and proven feature components.
- Before creating an interactive control, inspect existing AutoVision patterns and the adopted IBM Carbon implementation, if present. Prefer an accessible Carbon primitive and compose domain components from it; create custom interaction behavior only when the workflow cannot be represented adequately by the established primitive. Do not add Carbon or other UI dependencies as an incidental upgrade.
- Match information density to the role: manager/enterprise views support comparison and operational visibility; advisor/technician views optimize task completion and current-state clarity; customer views prioritize plain language, trust, and low cognitive load.
- Do not calculate business rules, authorization, or authoritative totals in templates or browser code.
- Prefer URL-backed state for filters, sorting, selection, or views that should survive refresh and browser navigation.
- Design mobile intentionally. Use progressive disclosure and semantic cards when dense desktop tables do not fit; preserve efficient tablet/desktop workflows.
- Use native semantics first, provide keyboard and focus behavior, avoid color-only meaning, and localize all user-visible and accessible text.
- Avoid giant components and speculative generic UI frameworks. Apply the file-size guidance in [frontend standards](../../docs/engineering/frontend-standards.md).
- Add focused component/service tests and responsive/accessibility E2E coverage when interaction behavior changes.
- Significant surfaces deliberately cover applicable initial, loading, loaded, empty, partial, warning, error, forbidden, disabled, stale/conflict, and offline/degraded states. Validate desktop, tablet, and mobile behavior for the target role rather than assuming the framework provides it automatically.
