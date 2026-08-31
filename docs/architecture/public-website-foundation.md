# Public Website Foundation

**Status: IMPLEMENTED (VSP-W3 foundation; W3D runtime amendment; W3E-R1
pre-auth and access-hub hardening; R5.2 technical visual review candidate)**

This record defines the independent public website application delivered by
VSP-W3. Product intent, visual direction, and conversion governance remain in
the [commercial website strategy](../product/strategy/commercial-website-strategy.md)
and [UX foundation](../product/strategy/commercial-website-ux-foundation.md).

## Boundary

The public website lives in `website/` as an independently buildable Angular
application. It does not import from `frontend/`, use the authenticated
application's OIDC runtime, call protected APIs, embed product screens, or
share browser storage and runtime state with AutoVision.

The existing authenticated application and its Docker/Nginx topology are
unchanged. Public hosting and DNS are **DEFERRED**; W3 supplies a buildable
application foundation only.

For local Docker development, the independent `website` service serves the
production Angular artifact through unprivileged Nginx at host port `8090`.
The existing authenticated `frontend` service remains at host port `8080`;
Keycloak remains at `8081`, and PostgreSQL remains at `5432`. The platform
service continues to expose port `8080` only inside the Compose network.

Run the normal local stack with `docker compose up --build` and stop it with
`docker compose down`. The website container has its own multi-stage
Node-build/Nginx-runtime Dockerfile and an Nginx SPA fallback, with no public
website proxy to the platform API. Direct public routes therefore resolve to
the Angular application while the authenticated product keeps its existing
API proxy and OIDC boundary.

The site uses a system-first typography stack (`Segoe UI`, `Helvetica Neue`,
Arial, sans-serif) for enterprise maturity, predictable numerals,
multilingual fallback, performance, and zero font licensing or CDN risk. This
is an intentional W3 decision; no font binaries or runtime font service are
used. The token layer defines display, hero, heading, body, label, caption,
navigation, and button roles, plus content, reading, section, stack, and grid
layout primitives.

## R5.2 Hero Remediation

**Status: IMPLEMENTED for technical review; product-owner visual approval is
DEFERRED.**

The homepage hero uses the existing native CSS grid and shared spacing tokens,
which follow the repository's Carbon-informed responsive discipline. No IBM
Carbon package is installed in `website/`, so no Carbon component or API is
claimed. The rejected Evidence/Decisions/Outcomes artwork, diagonal lines, and
large outlined geometry were removed.

The hero now presents one repository-owned inline SVG with a responsive
`viewBox`, semantic token-driven styling, and the corporate cycle:
Understand -> Design -> Deliver -> Measure -> Improve. Its SVG is decorative
to assistive technology; an ordered semantic equivalent remains in the DOM.
AutoVision's six-stage product cycle and Service Profit's linear workflow are
unchanged.

## Controlled Visual Pattern Architecture

**Status: IMPLEMENTED**

The public website uses one shared component, layout, content, navigation,
responsive, and accessibility system. Brand presentation is selected through
the Sass token configuration at
`website/src/styles/_tokens.scss`:

```scss
$spenmer-visual-pattern: p1 !default;
```

P1 is the active R5 pattern. The token file defines these approved
alternatives:

| Pattern | Palette | Character |
| --- | --- | --- |
| P1 — Violet | Graphite, Ivory, Electric Violet | Premium, distinctive, technical |
| P2 — Carbon | Carbon Blue, Ice White, Graphite | Enterprise, precise, trustworthy |
| P3 — Indigo | Midnight Navy, Ice White, Indigo | Confident, intelligent, balanced |
| P4 — Plum | Midnight Plum, Soft White, Graphite | Sophisticated, executive, controlled |

P2, P3, and P4 are approved design alternatives only. They are activated only
through an explicit product or brand decision by changing the centralized
configuration. They are not public themes and are not selected by route, user,
customer, query parameter, cookie, local storage, or random rotation.

R5 presents the public company as **SPENMER**, with **AutoVision by SPENMER**
as the product presentation. The header includes an original inline SVG concept
mark designed to remain legible in monochrome graphite or white treatments; it
inherits the semantic graphic token and does not introduce a runtime brand-color
dependency. Product and workflow graphics likewise use inherited color or
semantic tokens. **Service Profit** is the public product name, while
`/service-profit-ai` and `SERVICE_PROFIT_AI` remain technical compatibility
identifiers at the route and integration boundaries.

An internal review selector is rendered discreetly in the footer. It updates
`data-spenmer-pattern` on the document root and therefore changes only the
shared semantic custom properties. It has no route-specific styling, backend
state, analytics, cookie, or browser-storage dependency. Angular navigation
keeps the selected presentation while the application is open; a fresh load
returns to P1. The selector and its service are review tooling and must be
removed or disabled before public launch.

Components consume the stable semantic `--site-*` tokens emitted from the
active pattern, including page and inverse surfaces, primary and secondary
text, borders, interactions, accents, graphics, and focus. Switching patterns
therefore does not require component duplication, content changes, routing
changes, business logic, backend changes, or security changes. Reusable
graphics should use inherited color or semantic tokens rather than a literal
pattern accent. The company mark remains recognizable in full-color,
monochrome graphite, and monochrome white treatments.

Brand presentation is separate from application semantics. Success, warning,
danger, and informational colors remain dedicated accessible status tokens and
do not change meaning when the corporate pattern changes. The defined pattern
values are selected for token-level contrast across primary text, inverse text,
controls, links, focus, and essential borders; P1 receives the complete browser
and accessibility validation for the active release. Full browser regression
of inactive patterns is **DEFERRED** until one is explicitly activated.

## Implemented Surface

- Standalone Angular bootstrap with a public route table and not-found route.
- Responsive, keyboard-accessible header with skip link, visible focus, and a
  collapsible mobile navigation.
- Footer with product, company, evaluation, and legal navigation.
- Corporate treatment is the temporary typography-first wordmark `SPENMER`;
  final logo design is deferred. AutoVision is listed under Products and
  appears contextually rather than in the persistent company lockup.
- Local design tokens and semantic native HTML controls. No Carbon, Material,
  Bootstrap, Tailwind, PrimeNG, analytics, or paid service was added.
- Color tokens distinguish canvas, primary/subtle/dark surfaces, ink levels,
  borders, brand, signal accent, focus, success, warning, danger, and
  informational states. Brand is not used as success, and signal is not used
  as warning. Contrast is designed for WCAG 2.2 AA review. R5 browser checks
  cover representative contrast, landmarks, disclosure behavior, responsive
  overflow, and public naming at the requested viewport sizes. Formal
  assistive-technology review, brand or trademark approval, and public launch
  approval remain **DEFERRED**.
- Responsive layouts use constrained normal, wide, and reading measures,
  shared gutters, section rhythm, twelve-column grids, and mobile stacks.
- Typography uses the approved system-first family with tokenized Display/Hero,
  H1-H4, Body Large/Body/Body Small, Label, Navigation, Button, Caption, and
  form helper roles. The spacing scale is tokenized from 0.25rem through 6rem,
  with 72rem content and 84rem wide limits plus responsive gutters.
- Typography uses explicit responsive roles and reduced-motion defaults. The
  header disclosure supports `aria-expanded`, `aria-controls`, Escape close,
  focus return, and close-on-route-selection without modal focus trapping.
- Route-level document title, description, and Open Graph metadata.
- `Request a Demo` as an accessible contextual shortcut into the shared Contact
  Us workflow. It does not create a separate demo request model.
- `Contact Us` as the universal commercial entry point with one adaptive form
  for Product Demo, Advisory & Implementation, Partnership, and General
  Enquiry. `Request a Demo` remains a contextual shortcut into this route with
  allow-listed Product Demo and Service Profit AI query values. The form is
  still non-submitting until the governed public intake boundary is approved.
- `Product Demo` as a separate authorized-access journey. Its destination is
  read from `public/site-config.json`, copied to `/assets/site-config.json`,
  and remains intentionally empty until
  the approved AutoVision login URL is configured. The route must hand off to
  the existing login boundary; it must not implement authentication locally.
  An empty, invalid, or non-HTTPS value renders a disabled control with an
  explicit unavailable message rather than a misleading anchor. The sole
  exception is HTTP to `localhost`, `127.0.0.1`, or `[::1]` for local Docker
  development.
- Docker Compose mounts the local-only `public/site-config.local.json` over
  the runtime asset so local `Product Demo` navigation reaches the existing
  authenticated frontend at `http://localhost:8080`. This local HTTP value is
  not part of the safe source default and must not be used as a production
  destination. Production configuration and hosting remain **DEFERRED**.
- `Product Demo` is an access hub, not a login or product route. It exposes
  only the approved `VSPAV Service Profit` entry. A configured entry opens the
  existing authenticated frontend in a new tab with `noopener noreferrer`;
  the public website remains open. Empty, invalid, or disallowed configuration
  shows an explicit unavailable state. The runtime shape is
  `productDemo.serviceProfit.{label,url}`; the legacy `productDemoUrl` shape
  remains accepted for compatibility.
- The authenticated frontend has an explicit bootstrap readiness state. Until
  OIDC and Platform identity confirmation resolve, it renders only a minimal
  `Sign in to AutoVision` access boundary and never the workspace, product
  navigation, tenant content, or protected routes. The existing shell renders
  only for an authenticated session. Logout, session expiry, and failed
  identity confirmation clear local state and return to that boundary.
- Authenticated route guards require an authenticated session in every
  environment; development configuration does not bypass the boundary.
  OIDC Authorization Code + PKCE, server authorization, tenant isolation, and
  protected API behavior are unchanged.
- Browser logging is intentionally sparse. Startup failures emit only a
  concise high-level message; tokens, credentials, cookies, headers, URLs with
  query state, protected responses, tenant data, and customer data are not
  logged. Normal successful authentication and navigation produce no console
  output.

Metadata is updated per route for title, description, and Open Graph title,
description, and type. No production canonical hostname, structured data, or
public crawl approval is invented; the current robots policy is `noindex,
nofollow` while the site is private/local.

Request Demo remains a semantic, required-field form shell with a visible
status message and disabled submission. It does not transmit, persist, or
send analytics about visitor data.

## English-First Locale Readiness

**Status: IMPLEMENTED; product-owner review DEFERRED**

The current public launch is English-first with `en-US` as the default locale.
The website keeps a small typed locale contract for `en-US`, `en-GB`, and
`de-DE`, without fabricating translations or exposing a non-functional
language selector. Arabic content and RTL presentation remain future scope.

When translated resources are approved, the recommended routing strategy is to
retain `/` and the existing unprefixed routes as the English default, then add
backward-compatible locale-prefixed aliases such as `/en-gb/` and `/de/`.
Arabic-prefixed routes should be introduced only with translated content and
RTL review. This keeps current links stable while allowing future localized
metadata, content resources, and document direction.

## Deferred Work

- W4: approved homepage and product content, reviewed claims, media, and
  conversion copy.
- Secure lead intake, privacy consent, anti-abuse controls, notification
  routing, and CRM integration.
- Production Product Demo login destination configuration and deployment
  topology.
- Final privacy, terms, accessibility, brand, domain, and public-launch review.

These items are **DEFERRED**, not implied commitments. SPENMER naming and
website content remain provisional pending the required human approvals.

## Validation

From `website/`, the independent commands are:

```text
npm install
npm run build
npm test
```

R5 validation also includes the production website Docker service at
`http://localhost:8090`, route and disclosure checks at the required mobile,
tablet, and desktop widths, screenshot evidence for the primary public
surfaces, and the repository platform gate. English and the single active P1
presentation are covered by this release validation.

## W3 Compliance Matrix

| Requirement | Source/reference | Applicable | Evidence | Result | Notes |
| --- | --- | --- | --- | --- | --- |
| Carbon grid discipline | R5.2 brief; frontend standards | Y | Existing native `.site-container`, `.site-grid`, and responsive `.hero-grid` CSS composition | PASS | No Carbon grid package is installed in `website/`; this is Carbon-informed layout discipline, not a claim of Carbon component compliance. |
| Carbon breakpoints | R5.2 brief; frontend standards | Y | Continuous CSS media-query behavior at the existing 800px and 1100px layout boundaries | PASS | No device-specific coordinates or per-viewport SVG CSS are used. |
| Carbon spacing | R5.2 brief; frontend standards | Y | Shared `--site-space-*`, gutter, content-max, and section tokens | PASS | Native token implementation; no Carbon token package is installed. |
| Carbon typography | R5.2 brief; frontend standards | Y | Shared type roles and system-first font stack in `_tokens.scss` | PASS | No paid or remote font dependency; Carbon typography APIs are not present in this project. |
| Responsive hero composition | R5.2 brief | Y | Desktop grid, mobile stack, fluid SVG `viewBox`, Docker sweep across required samples | PASS | Product-owner visual approval remains DEFERRED. |
| SVG/process accessibility | R5.2 brief; frontend standards | Y | Decorative SVG plus ordered DOM equivalent for all five corporate stages | PASS | Meaning does not depend on color; formal AT review remains DEFERRED. |
| Accessibility | Frontend standards; W3/W3D website foundation | Y | Native landmarks, one H1 per route, focus ring, disclosure keyboard behavior, labelled form controls, reduced-motion CSS, decorative SVG plus semantic ordered cycle | PASS | Formal assistive-technology certification remains DEFERRED. |
| Typography | Frontend standards; W3/W3D website foundation | Y | System-first stack, tokenized type roles, controlled `clamp()` scale, readable measures, browser review at required widths | PASS | No paid or remote font dependency. |
| Spacing | W3/W3D website foundation | Y | Shared `--site-space-*` scale, common gutters, content/wide limits, section rhythm | PASS | Existing legacy page styles remain outside the R5 refactor boundary. |
| Responsive UX | W3/W3D website foundation | Y | Docker browser sweep, no overflow, mobile menu and form checks | PASS | Required viewport evidence captured. |
| Screen resolution coverage | W3/W3D website foundation | Y | 320, 360, 375, 390, 412, 768, 820, 1024, 1280, 1366, 1440, 1536, 1920 checks | PASS | Representative DOM/geometry checks plus screenshot review. |
| Compatibility | W3/W3D website foundation | Y | Independent Angular build, SPA fallback, technical routes preserved, semantic CSS tokens | PASS | No authenticated application coupling introduced. |
| Theming/design tokens | Controlled Visual Pattern Architecture | Y | P1-P4 maps, root `data-spenmer-pattern`, shared semantic variables, stable status tokens | PASS | P1 default; preview-only P2-P4 review. |
| Maintainability | Frontend standards; W3/W3D website foundation | Y | Shared shell, one preview service, no duplicated templates, focused test coverage | PASS | Review tooling is explicitly removable. |
| Localization readiness | W3/W3D website foundation | Y | No width-dependent component logic, wrapping labels, responsive controls | PASS | Full localization service integration remains outside this public foundation. |
| Performance | W3/W3D website foundation | Y | Static SVG/CSS approach, no new dependency, 307.69 kB initial raw build | PASS | Core Web Vitals measurement is DEFERRED until an approved host exists. |
| Portability | W3/W3D website foundation | Y | Independent `website/` build and unprivileged Nginx Docker service | PASS | Public hosting remains DEFERRED. |
| Security | W3/W3D website foundation; security standards | Y | No auth/API changes, no secrets, technical Product Demo handoff preserved, private robots policy retained | PASS | Lead abuse controls and production deployment controls remain DEFERRED. |
| Test discipline | Testing standards; validation workflow | Y | 19/19 website tests, route/disclosure browser checks, Docker-served viewport sweep | PASS | Platform regression is outside this website-only change and remains a separate gate. Automation is baseline, not formal usability proof. |
| Repository hygiene | AGENTS.md; validation workflow | Y | Correct branch/HEAD, no staged files, no conflicts, `git diff --check` passes | PASS | Existing intentional uncommitted changes preserved. |

The W3 implementation uses no incremental paid software or service spend
(₹0). Dependencies are limited to the Angular runtime and its development
toolchain, independently declared in `website/package.json` and locked in
`website/package-lock.json`.