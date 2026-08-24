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

## Opportunity Type Navigation

Opportunity types are a labelled, single-select group: All, Declined Work, Deferred Work, Due Service, Overdue Service, and Inactive Customer. Selecting a type updates the server-side `opportunityType` filter. Selecting All clears it.

A count is shown only when supplied by the summary API. Counts are not calculated from the first 25 queue rows. The selected button uses text, border treatment, and `aria-pressed`; selection is not communicated by color alone.

On mobile, the type controls form a horizontally scrollable touch rail. On larger screens they wrap into a compact strip.

## Sorting And Filtering

Priority and Actionability are server-side filters. Sort supports only values implemented by the API:

- Newest;
- Oldest;
- Highest potential;
- Lowest potential.

The current values are stored in the URL as `type`, `priority`, `actionability`, and `sort`. A browser refresh, Back, or Forward therefore preserves or restores the view. Invalid values safely fall back to All filters and Newest sort.

Filtering and sorting never rearrange the partially loaded queue in the browser.

On mobile, Priority, Actionability, and Sort are consolidated behind one Sort & Filter disclosure. Changes remain pending until Apply. Clear resets both filters to All and Sort to Newest; Opportunity Type remains separate. The active count includes only non-default Priority and Actionability filters.

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
