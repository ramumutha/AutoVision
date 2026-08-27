# Frontend Engineering Standards

## Scope and Ownership

The current browser application is Angular under `frontend/`. Preserve the established separation:

| Area | Responsibility |
| --- | --- |
| `core` | Authentication, API infrastructure, localization, errors, and application-wide services |
| `features` | Domain workflows and feature-specific presentation |
| `shared` | Proven reusable UI and design-system building blocks |
| `shell` | Navigation and application composition |

A feature container coordinates route state and requests. A display component renders explicit inputs. An API service translates typed contracts into HTTP calls. Keep these responsibilities visible to junior developers rather than hiding them behind generic facades.

## Angular Patterns

Use standalone components and `ChangeDetectionStrategy.OnPush`, matching current code. Signals are appropriate for local view state. RxJS owns asynchronous streams and cancellation; use `switchMap` when only the latest query, selection, or route request may render.

Avoid `subscribe` chains that create hidden ordering or leak cancellation. Clean up subscriptions with Angular lifecycle integration such as `takeUntilDestroyed`.

Do not introduce a runtime viewport service when CSS media queries can own presentation. Do not add memoization or wrappers by habit; follow current Angular/compiler patterns.

## Component Design

A component should have one explainable role. For example, Service Profit uses:

- a manager for URL state and list orchestration;
- controls for user intents;
- a mobile list for concise card navigation;
- one authoritative detail presentation;
- a routed page for route-specific loading and errors.

This is preferable to duplicating detail markup or building one giant responsive component. Reuse the shared design system and existing feedback/status components before creating feature-local alternatives.

Apply the [coding-standard size guidance](coding-standards.md#maintainability-guidance). Angular TS should preferably remain below 300 lines, HTML below 250, and SCSS below 300.

### Carbon-first composition

Before creating a custom interactive control, inspect the current shared design-system components and the repository's adopted IBM Carbon dependencies and patterns. Prefer a Carbon primitive when it represents the interaction accessibly, then compose the domain component around it. Custom interaction behavior is justified only when an established primitive cannot represent the domain workflow without degrading comprehension or accessibility. Do not introduce or upgrade Carbon as incidental feature work.

## State and Navigation

Use URL-backed state when users expect refresh, deep links, Back, or Forward to preserve filters, sorting, tabs, or views. Validate query parameters against domain allow-lists and use safe defaults. Do not place tenant IDs, sensitive identifiers, or authorization decisions in URL state.

Keep transient draft controls local until Apply when immediate navigation would create noisy or expensive requests. When a disclosure removes the focused control, restore focus predictably.

## Templates and Business Rules

Templates may format and conditionally present already-decided state. They must not:

- calculate authoritative totals or commercial policy;
- infer permissions or tenant scope;
- reproduce domain validation;
- sort or filter a partial server result as if it were authoritative;
- parse structured data with ad hoc string logic.

Move non-trivial decisions to typed TypeScript or, when authoritative, to the backend.

## Responsive Product Experience

Responsive behavior is a workflow decision, not just smaller spacing.

- Desktop should support efficient scanning and repeated action.
- Tablet may reduce secondary information while retaining the primary workflow.
- Mobile should use progressive disclosure, concise cards, and routed detail when a dense table no longer works.
- Preserve visible feedback, touch targets of about 44 CSS pixels where practical, and document-level overflow checks.

Do not hide essential safety states such as suppression or required review. Horizontal scrolling inside an intentional control rail can be acceptable; horizontal document overflow is not.

Information density follows the user role. Manager and enterprise surfaces support comparison, exceptions, trends, and drill-down. Advisor and technician surfaces prioritize speed, current-state clarity, and actionable exceptions. Customer surfaces prioritize plain language, trust, and low cognitive load. Consider desktop, tablet, and mobile for the target role and validate navigation, tables/cards, forms, long text, values, evidence, actions, and failure states explicitly.

Significant interactive surfaces deliberately handle applicable initial, loading, loaded, empty, partial, warning, error, forbidden, disabled, stale/conflict, and offline/degraded states. Framework defaults do not count as validation.

## Accessibility

Use native elements before ARIA: links for navigation, buttons for commands, fieldsets/legends for related controls, tables for tabular data, and `details` for simple disclosure. Ensure:

- meaningful accessible names;
- visible keyboard focus;
- correct heading order and labelled regions;
- `aria-expanded`/`aria-controls` where disclosure state is custom;
- polite live status for loading where appropriate;
- focus restoration when a focused popup or panel closes;
- status and selection expressed in text, not color alone.

Run Axe/Playwright coverage for meaningful workflow changes, but treat automated scans as a baseline rather than proof of usability.

## Localization

All visible text and accessible labels belong in the localization service or established translation mechanism. Do not concatenate sentence fragments when grammar may vary. Domain-code labels may use a centralized formatter only when product terminology does not require a specific translation.

## Testing

Place tests beside the responsibility they verify. Test component semantics and emitted behavior, route/query restoration, cancellation, loading/errors, focus behavior, and responsive workflow. Avoid continually enlarging a container spec when a focused child component or page owns the behavior.

Use `npm test`, `npm run build`, and the configured Playwright scripts. See [testing standards](testing-standards.md).
