# Service Profit Frontend Flow

## Route Structure

`/service-profit` is a lazy standalone Angular route protected by `authenticatedGuard`. The route renders `ServiceProfitManagerComponent` inside `AvShellComponent`.

Planned for C2-C: `/service-profit/opportunities/:opportunityId` will provide dedicated mobile detail. It is not implemented in C2-B.

```mermaid
flowchart LR
    Browser[Browser URL] --> Guard[authenticatedGuard]
    Guard --> Shell[AvShellComponent]
    Shell --> Manager[ServiceProfitManagerComponent]
    Manager --> Api[ServiceProfitApiService]
    Api --> Client[ApiClientService /api]
    Client --> Platform[Spring platform]
```

## Component Responsibilities

`AvShellComponent` owns global navigation and the authenticated-user popup. It closes the popup on outside click, Escape, Sign out, and route navigation. Escape restores focus to the trigger.

`ServiceProfitManagerComponent` owns manager query state, summary and queue loading, selection state, responsive presentation, and display-oriented domain labels. It does not authorize requests or calculate authoritative opportunity counts.

`ServiceProfitOpportunityControlsComponent` presents opportunity-type navigation, Priority and Actionability filters, Sort, refresh state, and refresh feedback. It emits explicit user intents to the manager and does not own URL or API state.

`ServiceProfitOpportunityDetailComponent` receives one `ServiceProfitOpportunityResponse` and owns dealer-facing presentation only. It does not call an API, own selection or routing, mutate data, or perform authorization. Desktop/tablet inline detail uses it now; the planned C2-C mobile page will reuse the same component.

`ServiceProfitApiService` translates typed filters, pagination, and sort values into `/api/v1/service-profit/opportunities` requests. It also loads summary and detail endpoints.

`AvStatusComponent` displays compact status labels. `AvFeedbackComponent` displays empty and error feedback.

## API Interaction

The manager calls:

- `GET /api/v1/service-profit/opportunities/summary` for KPIs and authoritative category counts;
- `GET /api/v1/service-profit/opportunities?page=0&size=25&sort=...` for the queue;
- `GET /api/v1/service-profit/opportunities/{id}` for authoritative detail.

Summary and queue requests run together. RxJS `switchMap` cancels an older request when a newer URL state or refresh command arrives. Detail requests also use `switchMap`, preventing an older selection response from replacing a newer one.

No client-side sorting or count calculation is performed on the 25 loaded queue rows.

## Filter And Sort State

The URL is the source of interaction state:

| Query parameter | Domain value | Fallback |
| --- | --- | --- |
| `type` | existing opportunity types | All |
| `priority` | `HIGH`, `MEDIUM`, `LOW` | All |
| `actionability` | existing actionability values | All |
| `sort` | detected/potential ascending or descending | `DETECTED_DESC` |

Unknown values are ignored by explicit allow-list parsing. Controls navigate with merged query parameters, so browser refresh, Back, and Forward are predictable. No tenant, customer, vehicle, or other sensitive identifiers are added by these controls.

```mermaid
sequenceDiagram
    participant U as User
    participant R as Angular Router
    participant M as Manager
    participant A as API service
    U->>R: Select type/filter/sort
    R->>M: queryParamMap
    M->>M: Validate and set signals
    M->>A: Summary + queue request
    Note over M,A: switchMap cancels older request
    A-->>M: Latest result
    M-->>U: Updated summary and queue
```

## Selection State

The manager owns selected ID, loading, error, and response state. Selecting an opportunity title stores its ID and requests authoritative detail. A valid sibling table row is inserted immediately after the selected queue row with one cell spanning all seven columns. That cell contains loading, error, or `ServiceProfitOpportunityDetailComponent`.

Only one inline detail row exists. Selecting another opportunity moves it. Selecting the same opportunity again emits a null selection command, collapses the row, and cancels any pending detail request. If a queue reload no longer contains the selected ID, the manager clears the orphan selection.

```mermaid
sequenceDiagram
    participant U as User
    participant M as Manager
    participant A as API service
    participant D as Detail component
    U->>M: Select queue opportunity
    M->>A: GET opportunity detail
    Note over M,A: switchMap cancels older selection
    A-->>M: Authoritative detail
    M->>D: Pass response input
    D-->>U: Present inline dealer detail
```

Planned for C2-C: mobile cards navigate to a dedicated full-detail route and preserve list query state for browser Back.

## Authentication Boundary

The frontend guard protects navigation for user experience. The shared auth interceptor supplies browser credentials to same-origin `/api` requests. Spring authorization and tenant isolation remain authoritative. Service Profit components must not infer tenant scope or bypass the shared API client.

## Responsive Behavior

CSS owns presentation; there is no runtime viewport service.

- `<=720px`: type navigation scrolls horizontally and toolbar controls wrap compactly.
- `721px–1100px`: compact/tablet table hides Opportunity Type, Evidence Strength, and Detected while inline detail retains them.
- `>1100px`: desktop layout.

Current: the opportunity table and inline detail remain the mobile fallback. Planned C2-C replaces them with mobile decision cards, routed detail, and consolidated mobile Sort/Filter controls.

## Loading And Errors

Initial and query-driven loads show the manager loading state. A failed initial/query load shows the manager error and Retry action.

Background refresh keeps loaded content visible, marks the queue busy, disables Refresh, and reports refresh failure without discarding data.

Detail loading and errors remain independent and render inside the selected opportunity's inline row. A detail `401` displays session-renewal guidance; `403` explicitly reports that the user cannot view the opportunity.

## Accessibility Architecture

- Native buttons and selects provide keyboard behavior.
- Type navigation is a labelled group of single-select buttons using `aria-pressed`.
- Labels wrap their associated select controls.
- The queue exposes `aria-busy`; loading uses polite live status.
- Selected opportunity text and safety notices prevent color-only communication.
- Opportunity titles are table row headers. Disclosure buttons use `aria-expanded` and `aria-controls`; loaded detail is a uniquely labelled region.
- The profile popup exposes `aria-expanded`, closes globally on Escape, and restores trigger focus.
- Native `<details>` provides keyboard-accessible audit disclosure.

## Important Deferred Items

C2-C:

- mobile opportunity cards;
- consolidated mobile Sort/Filter disclosure with Apply/Clear behavior;
- guarded mobile detail route and direct-load error states;
- browser Back restoration from mobile detail;
- responsive and accessibility E2E expansion for those interactions.

These deferred items must preserve current API contracts, tenant authorization, evidence/provenance meaning, suppressed-opportunity safeguards, and `REVIEW_REQUIRED` behavior.

## Tests

Use the repository-configured commands from `frontend/`:

```text
npm test
npm run build
```

Do not invoke bare Vitest. Add selection/request tests beside the manager, presentation tests beside `ServiceProfitOpportunityDetailComponent`, shell popup tests beside the shell, and query serialization tests beside `ServiceProfitApiService`.
