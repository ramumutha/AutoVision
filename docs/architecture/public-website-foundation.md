# Public Website Foundation

**Status: IMPLEMENTED (VSP-W3 foundation)**

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
- `Request a Demo` as a non-submitting, accessible form shell. It does not
  collect, transmit, or persist lead data.
- `Product Demo` as a separate authorized-access journey. Its destination is
  read from `public/site-config.json`, copied to `/assets/site-config.json`,
  and remains intentionally empty until
  the approved AutoVision login URL is configured. The route must hand off to
  the existing login boundary; it must not implement authentication locally.
  An empty, invalid, or non-HTTPS value renders a disabled control with an
  explicit unavailable message rather than a misleading anchor.

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