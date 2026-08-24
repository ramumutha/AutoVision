---
description: "Use when changing Markdown, product records, architecture decisions, engineering standards, runbooks, diagrams, or documentation indexes."
applyTo: "**/*.md"
---
# Documentation Instructions

- Write for humans first. A developer, product manager, QA engineer, or architect must understand the repository without an AI assistant.
- State `IMPLEMENTED`, `PLANNED`, `DEFERRED`, or `RESEARCH REQUIRED` explicitly when status matters. Do not present proposals as commitments.
- Separate functional/product intent from technical architecture and implementation detail; cross-link authoritative sources instead of copying them.
- Keep docs synchronized with behavior, architecture, security, runtime, ownership, and workflow changes. Documentation is part of Definition of Done.
- Use stable IDs for decisions, research, features, releases, and traceability. Do not fabricate dates, evidence, market claims, KPI targets, or external citations.
- Use Mermaid when a flow or relationship is clearer as a diagram. Keep indexes current and links relative.
- Avoid AI-specific assumptions or instructions in human operating docs. See [documentation standards](../../docs/engineering/documentation-standards.md).
