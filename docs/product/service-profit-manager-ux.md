# Service Profit Manager UX

## Purpose

The Service Profit Manager helps dealership managers find service revenue opportunities, understand their commercial potential, and review the evidence and recommended action before contacting a customer.

The screen is a decision-support view. It does not replace authoritative DMS transactions, tenant authorization, or source evidence.

## Summary KPIs

The summary shows:

- recoverable potential separately for each currency;
- total opportunities;
- high-priority opportunities;
- opportunities requiring review;
- opportunities ready for action.

Currency amounts must never be combined across currency codes.

The manager follows this information order: **Money -> Focus -> Organize -> Inspect -> Act**.
Recoverable Potential is the primary commercial signal. Data-capability information is
available on demand from that presentation so data confidence and attribution limits are
visible without dominating the dashboard.

## Business Focus

The four KPI lenses are labelled business views: Total Opportunities, High Priority,
Review Required, and Ready to Action. Each lens updates the authoritative manager query
and its count comes from the summary API. Selecting a lens is not a client-side filter
over the currently loaded rows.

The Opportunities heading shows the authoritative count. Counts are not calculated from
the first queue page. The selected button uses text, border treatment, and `aria-pressed`;
selection is not communicated by color alone.

## Grouping And Organization

Group By offers None, Opportunity Type, Priority, and Actionability. Grouped sections are
collapsed by default, can be expanded independently, and allow multiple open groups.
Grouping organizes the queue for scanning; it does not add a hidden business filter and
does not replace the KPI lenses.

## Sorting And Filtering

Desktop sorting supports only values implemented by the API:

- Potential descending (`POTENTIAL_DESC`);
- Potential ascending (`POTENTIAL_ASC`);
- Detected newest (`DETECTED_DESC`);
- Detected oldest (`DETECTED_ASC`).

The primary R1 manager does not expose the advanced filter surface. The underlying API
and query capability remains available for reusable or future experiences. This keeps
KPI business lenses and Group By as the simpler dealer-manager workflow.

Supported query state remains URL-backed for compatible links and future/opt-in controls.
Filtering and sorting never rearrange a partially loaded queue in the browser. Search is
not implemented: **SEARCH DEFERRED — authoritative server search contract required.**
The server-paginated endpoint has no approved contract for searching the complete
opportunity dataset, so client-only search would be misleading.

Mobile may provide appropriate sorting controls where desktop headers are not present.

## Refresh

Refresh is a compact icon-only utility action in the opportunity-list toolbar. A background refresh:

- keeps the loaded summary and queue visible;
- disables the action and uses a subtle busy treatment while active;
- cancels an older manager request if a newer query or refresh starts;
- retains existing data and displays an error if refresh fails;
- does not poll.

## Profile Menu

The authenticated-user trigger toggles the profile popup. The popup closes after an outside click, Escape, Sign out, or route navigation. Escape returns focus to the trigger. Sign out does not add or infer role, location, or profile data.

## Progressive Disclosure

The queue presents the minimum information needed to choose an opportunity. On desktop and tablet, selecting an opportunity inserts its detail immediately after that table row. Only one opportunity is expanded at a time; selecting another moves the detail, and selecting the expanded opportunity again collapses it.

Loading and errors appear in the same inline position, so feedback remains beside the selected record. Detailed customer, vehicle, service, explanation, evidence, and provenance data is disclosed after selection. Audit details use native disclosure behavior and are collapsed initially.

## Safety States

A suppressed opportunity must remain visibly identified as suppressed and must show Do not action with the authoritative suppression reason when available.

`REVIEW_REQUIRED` means a human review is required before customer contact. It must not be presented as Ready to Action.

## Responsive Interaction

Current ranges are:

- mobile: `720px` and below;
- compact/tablet: `721px` through `1100px`;
- desktop: above `1100px`.

Current desktop/tablet behavior keeps the opportunity table and inserts detail immediately after the selected row. At compact/tablet widths, Opportunity Type, Evidence Strength, and Detected are hidden from the table; they remain available in detail. The type rail and filter/sort/refresh toolbar remain responsive.

At mobile widths, the table is replaced with semantic opportunity cards. Each card contains the title, type, recoverable amount, priority, actionability, and concise evidence strength, and links to `/service-profit/opportunities/:opportunityId`. The link preserves current list query parameters. The dedicated page loads authoritative detail and reuses the same dealer detail presentation as inline desktop/tablet detail.

Back returns through browser history when the user arrived from the list. For a direct detail link without meaningful in-app history, Back navigates to `/service-profit` with the supported list query parameters preserved. Direct-load errors distinguish expired sessions, unauthorized access, unavailable opportunities, and retryable failures.

Suppressed detail retains the Do not action safeguard. `REVIEW_REQUIRED` detail retains review-before-contact guidance.

## Accessibility Expectations

- Every icon-only or compact utility action has an accessible name and visible focus.
- Type navigation has a group label and each button exposes `aria-pressed`.
- Select labels are programmatically associated with their controls.
- Loading is announced with a polite live status and the queue exposes busy state.
- Suppression, review requirements, and selection are expressed in text, not color alone.
- The profile menu supports outside dismissal, Escape, and focus restoration.
- Existing table headers retain `scope="col"`; opportunity selection remains a native button.
- Opportunity titles are row headers. Selection buttons expose `aria-expanded` and `aria-controls`, and inline detail is a labelled region.
- Mobile opportunities are semantic list items containing descriptive native links. Sort & Filter exposes `aria-expanded` and `aria-controls`, and uses native labelled selects inside a fieldset.

## Safe Test Locations

Inline selection and request tests belong in `frontend/src/app/features/service-profit/service-profit-manager.component.spec.ts`. Mobile card, mobile filter, and routed-page tests remain in their focused component specs. Dealer-detail presentation tests belong beside `ServiceProfitOpportunityDetailComponent`. Profile-menu tests belong beside the shell. API query serialization tests belong beside `ServiceProfitApiService`.

## Deferred UX Polish

These are accepted, non-blocking follow-up items for the next frontend refinement cycle:

- **UX-POLISH-01:** refine Data Capability dialog close-button spacing, border, and placement so it does not visually interfere with Assessment Policy content.
- **UX-POLISH-02:** refine Opportunities heading/count baseline alignment consistently across desktop, tablet, and mobile.
- **UX-POLISH-03:** replace plain initial loading text with a subtle accessible progress or skeleton treatment.
- **UX-POLISH-04:** validate Refresh scope against actual behavior. If it reloads all Service Profit data, move it to a clearly global manager location; if it reloads only the opportunity workspace, retain it beside Group By.

These items do not change SP-F004 IMPLEMENTED status or demo readiness, and are not
production-readiness blockers.
