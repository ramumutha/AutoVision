# Public Website Foundation

**Status: IMPLEMENTED (VSP-W3 foundation; W3D runtime amendment; W3E-R1
pre-auth and access-hub hardening)**

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

## Implemented Surface

- Standalone Angular bootstrap with a public route table and not-found route.
- Responsive, keyboard-accessible header with skip link, visible focus, and a
  collapsible mobile navigation.
- Footer with product, company, evaluation, and legal navigation.
- Local design tokens and semantic native HTML controls. No Carbon, Material,
  Bootstrap, Tailwind, PrimeNG, analytics, or paid service was added.
- Color tokens distinguish canvas, primary/subtle/dark surfaces, ink levels,
  borders, brand, signal accent, focus, success, warning, danger, and
  informational states. Brand is not used as success, and signal is not used
  as warning. Contrast is designed for WCAG 2.2 AA review; final browser and
  assistive-technology review remains **DEFERRED**.
- Responsive layouts use constrained normal, wide, and reading measures,
  shared gutters, section rhythm, twelve-column grids, and mobile stacks.
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

## Deferred Work

- W4: approved homepage and product content, reviewed claims, media, and
  conversion copy.
- Secure lead intake, privacy consent, anti-abuse controls, notification
  routing, and CRM integration.
- Production Product Demo login destination configuration and deployment
  topology.
- Final privacy, terms, accessibility, brand, domain, and public-launch review.

These items are **DEFERRED**, not implied commitments. VERSPEN naming and
website content remain provisional pending the required human approvals.

## Validation

From `website/`, the independent commands are:

```text
npm install
npm run build
npm test
```

The W3 implementation uses no incremental paid software or service spend
(₹0). Dependencies are limited to the Angular runtime and its development
toolchain, independently declared in `website/package.json` and locked in
`website/package-lock.json`.