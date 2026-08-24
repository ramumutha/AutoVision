# AutoVision Product Roadmap

## Purpose

This roadmap communicates product direction without converting every idea into a commitment. The [feature register](feature-register.md) is the detailed source; the [release register](release-register.md) records release evidence.

## Current Horizon

### Now: Service Profit R1

**IMPLEMENTED:** secure opportunity foundation, deterministic detection/explanation, data readiness and synthetic demo, manager summary/queue, authenticated UX, contextual detail, opportunity-type/filter/sort navigation, desktop/tablet inline detail, mobile cards/routed detail, suppression, and `REVIEW_REQUIRED` safeguards.

### Next Decisions

**RESEARCH REQUIRED:** validate dealer pain, production data readiness, measurable outcome KPIs, target markets/personas, and commercial packaging.

**PLANNED — UNASSIGNED:** `SP-F020` G6.1 defines the controlled dealer-data
intake architecture and canonical contract. It is a documentation/contract
foundation only; G6.2-G6.6 require separate gates and do not imply production
ingestion or provider support.

### Next Implementation Candidate

**DEFERRED — RELEASE UNASSIGNED:** `SP-F016-A` is the bounded internal follow-up/disposition candidate defined by [PDR-007](../decisions/PDR-007-bounded-internal-follow-up-disposition.md). It may proceed to implementation planning after dealer workflow research validates ownership and disposition semantics. Selection does not approve a release target or customer outreach.

### Uncommitted Opportunities

- **DEFERRED:** customer outreach/follow-up workflow;
- **RESEARCH REQUIRED:** richer management analytics;
- **DEFERRED:** role/location current-business context until authoritative identity/business context exists;
- **DEFERRED:** complete design/theme adoption from external design sources until governed assets and acceptance criteria exist.

These items have no target release and must not be treated as promises.

## AutoVision Opportunity Score

The AutoVision Opportunity Score (AOS) structures prioritization discussion across eight dimensions:

| Dimension | Direction |
| --- | --- |
| Dealer pain severity | Higher increases benefit |
| Revenue/ROI potential | Higher increases benefit |
| Market reach | Higher increases benefit |
| Differentiation | Higher increases benefit |
| Evidence confidence | Higher increases confidence |
| Implementation effort | Higher reduces attractiveness |
| Data dependency | Higher uncertainty reduces attractiveness |
| Operational/security risk | Higher reduces attractiveness |

Each planning cycle may score dimensions from 1–5 and approve weights appropriate to strategy. A normalized concept is:

`AOS = weighted benefits + evidence confidence - weighted effort/data/risk constraints`

The register should retain the dimension scores and rationale, not only a total. AOS supports comparison and exposes assumptions; it does not replace Product Owner judgment, architecture/security review, research, or release approval. No current numerical scores are asserted because approved weights and evidence are not recorded.

## Roadmap Governance

A feature moves from research to planned only when it has:

1. a supported problem/evidence record;
2. Product Owner decision and priority;
3. data and security feasibility;
4. bounded acceptance criteria and exclusions;
5. target release in the release register;
6. traceability and demo implications.
