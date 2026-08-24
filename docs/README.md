# AutoVision Documentation

This is the human entry point to AutoVision product and engineering knowledge. New contributors should start with the [product vision](product/strategy/product-vision.md), [system overview](architecture/system-overview.md), [repository map](architecture/repository-map.md), and [coding standards](engineering/coding-standards.md). Use the [local runtime](development/local-runtime.md) when setting up the application.

Status terms are explicit: **IMPLEMENTED**, **PLANNED**, **DEFERRED**, and **RESEARCH REQUIRED**. A roadmap idea is not a release commitment unless the registers and decisions say so.

## Product Strategy

- [Product vision](product/strategy/product-vision.md): product purpose, principles, current R1 focus, and evidence boundary.
- [Market strategy](product/strategy/market-strategy.md): supported positioning, hypotheses, segmentation questions, and commercial research gaps.
- [Release strategy](product/strategy/release-strategy.md): progressive delivery, current R1 sequence, gates, and uncommitted future work.

## Research

- [Research index](product/research/research-index.md): research record schema, lifecycle, and current records.
- [Dealer pain points](product/research/dealer-pain-points.md): repository-supported problems versus hypotheses requiring validation.
- [Market opportunities](product/research/market-opportunities.md): opportunity areas without unsupported market claims.
- [Data availability research](product/research/data-availability-research.md): current synthetic R1 coverage, capability, and limitations.
- [RS-005 dealer follow-up workflow validation](product/research/RS-005-dealer-follow-up-workflow-validation.md): interview guide, evidence capture, decision gate, and pilot-evidence preparation for SP-F016-A.

## Roadmap

- [Product roadmap](product/roadmap/product-roadmap.md): current horizon, uncommitted opportunities, and AutoVision Opportunity Score.
- [Feature register](product/roadmap/feature-register.md): feature problems, personas, value, status, evidence, implementation, demo, and KPI traceability.
- [Release register](product/roadmap/release-register.md): implemented release evidence and unassigned future scope.

## Product Decisions

- [Product Decision Record index](product/decisions/product-decision-index.md): frozen R1 product choices and review triggers.
- [PDR template](product/decisions/PDR-template.md): required structure for future product decisions.
- [Product traceability matrix](product/traceability/product-traceability-matrix.md): evidence-to-KPI chains and explicit gaps.

## Architecture

- [System overview](architecture/system-overview.md): runtime architecture, boundaries, and design principles.
- [Controlled dealer data intake](architecture/controlled-dealer-data-intake.md): provider-neutral intake boundary, canonical contract, validation, quarantine, and G6.1-G6.6 phasing.
- [Repository map](architecture/repository-map.md): ownership of top-level modules and where changes belong.
- [Authentication and tenant context](architecture/authentication-and-tenant-context.md): Keycloak, Angular, Spring Security, and tenant mapping contract.
- [Architecture Decision Record index](architecture/decisions/architecture-decision-index.md): durable architecture decisions and review triggers.
- [ADR template](architecture/decisions/ADR-template.md): required structure for future architecture decisions.

## Engineering Standards

- [Coding standards](engineering/coding-standards.md): repository-wide design, security, maintainability, and Definition of Done.
- [Frontend standards](engineering/frontend-standards.md): Angular ownership, state, responsive UX, accessibility, localization, and testing.
- [Backend and API standards](engineering/backend-api-standards.md): service boundaries, DTOs, tenant authorization, validation, and compatibility.
- [Database standards](engineering/database-standards.md): Flyway immutability, tenant containment, integrity, and PostgreSQL validation.
- [Testing standards](engineering/testing-standards.md): responsibility-based tests, commands, fixtures, and quality gates.
- [Documentation standards](engineering/documentation-standards.md): human-first documentation, status vocabulary, records, and review.
- [Short Agent feature workflow](engineering/agent-feature-workflow.md): compact future feature-prompt format and examples.

## Functional UX and Flows

- [Service Profit Manager UX](product/service-profit-manager-ux.md): manager behavior, responsive interaction, safety states, and accessibility.
- [Service Profit frontend flow](architecture/service-profit-frontend-flow.md): routes, component responsibilities, URL state, API flow, and cancellation.

## Testing and Development

- [Local runtime](development/local-runtime.md): prerequisites, environment setup, compose startup, authenticated login, and shutdown.
- [Validation and Git workflow](development/validation-and-git-workflow.md): focused checks, quality gates, and review discipline.
- [Troubleshooting](development/troubleshooting.md): symptom-to-boundary diagnostics for runtime and authentication failures.

## Operations

- [Controlled dealer data intake operator runbook](operations/controlled-dealer-data-intake-operator-runbook.md): controlled FULL intake, reconciliation, replay, recovery, and safe operational handling.

## Demo Readiness

- [Demo readiness matrix](product/demo/demo-readiness-matrix.md): feature-by-feature evidence and GREEN/AMBER/RED status.
- [Service Profit demo script](product/demo/demo-script.md): repeatable product narrative and operational guardrails.
- [Demo business question register](product/demo/demo-business-question-register.md): evidence-backed dealer objections, answers, limitations, and reusable-media implications.
- [Demo data catalog](product/demo/demo-data-catalog.md): synthetic dataset, sources, and ten R1 scenarios.
- [Known demo limitations](product/demo/demo-known-limitations.md): data, capability, product, evidence, and operational constraints.

When unsure where code belongs, start with the repository map. Before changing identity or authorization, read the authentication contract. Before proposing a feature, check research, the feature register, PDRs, ADRs, and traceability rather than assuming an idea is approved.
