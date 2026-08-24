# Documentation Standards

## Audience and Purpose

AutoVision documentation is an operating system for people: junior developers, senior engineers, architects, product managers, QA, support, and delivery stakeholders. It must remain understandable without an AI assistant.

Write the decision, behavior, ownership, and evidence a reader needs. Do not reproduce source code or another document when a link is more accurate.

## Documentation Types

| Type | Primary question | Location |
| --- | --- | --- |
| Product strategy | Why and for whom are we building? | `docs/product/strategy/` |
| Research | What evidence do we have and how confident are we? | `docs/product/research/` |
| Roadmap/registers | What is implemented, planned, deferred, or uncommitted? | `docs/product/roadmap/` |
| Product decisions | What product choice was made and why? | `docs/product/decisions/` |
| Architecture/ADRs | How is the system shaped and why? | `docs/architecture/` and `docs/architecture/decisions/` |
| Functional UX/flows | How should a user workflow behave? | `docs/product/` |
| Engineering standards | How do contributors work safely? | `docs/engineering/` |
| Runbooks | How is the system operated or validated? | `docs/development/` |
| Demo governance | What can be demonstrated with evidence? | `docs/product/demo/` |

Keep functional intent separate from technical implementation. Cross-link them through feature IDs and traceability.

## Status Vocabulary

Use status labels consistently:

- **IMPLEMENTED**: code and evidence exist in the repository.
- **PLANNED**: approved direction exists, but implementation is incomplete.
- **DEFERRED**: intentionally outside the current release/scope.
- **RESEARCH REQUIRED**: evidence or a decision is insufficient.

Do not use PLANNED to turn an idea into a commitment. Registers should identify the evidence or decision behind status.

## Records and Stable IDs

Use stable IDs such as `SP-F001`, `PDR-001`, `ADR-001`, and `RS-001`. A record should retain its ID even if its title changes. Templates explain required fields; completed records link to related evidence, features, architecture, implementation, tests, and review triggers.

Do not fabricate dates. Use `Not recorded` when history cannot establish one. Do not fabricate market statistics, user quotes, KPI targets, confidence, or citations.

## Links, Diagrams, and Duplication

Use repository-relative Markdown links. Link to an authoritative contract instead of restating it in multiple files. Indexes should explain where a newcomer starts and who owns updates.

Use Mermaid for flows, state transitions, or traceability when it improves comprehension. Keep diagrams close to the prose that defines their meaning and use stable labels rather than implementation trivia.

Duplicated rules drift. Concise agent instructions may summarize enforcement, but rationale and examples belong in these human standards.

## Documentation Definition of Done

Update documentation when a change affects:

- user-visible behavior or safety semantics;
- architecture, ownership, or domain boundaries;
- API, event, schema, or integration contracts;
- authentication, authorization, tenant behavior, or sensitive-data handling;
- runtime setup, validation commands, or operational troubleshooting;
- feature, release, demo, research, or decision status.

A trivial internal refactor that preserves behavior and contracts normally needs no document change. The completion report should still state that documentation impact was assessed.

## Review Checklist

1. Is the intended audience clear?
2. Are facts supported by code, tests, existing decisions, or cited research?
3. Are IMPLEMENTED, PLANNED, DEFERRED, and RESEARCH REQUIRED used accurately?
4. Are product intent and technical design separated but linked?
5. Are secrets, private data, and unsupported claims absent?
6. Do links and Mermaid syntax validate?
7. Does the documentation index expose the new source?
8. Is an owner or review trigger clear for records that can become stale?
