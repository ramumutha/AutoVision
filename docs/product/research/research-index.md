# Product Research Index

## Purpose

This index governs product, dealer, market, workflow, and data research. Research evidence informs decisions but does not itself approve a feature or release.

## Research Record Structure

Each research item should record:

| Field | Meaning |
| --- | --- |
| Research ID | Stable ID such as `RS-001` |
| Question | Decision-relevant question being investigated |
| Date | Collection/publication date; use `Not recorded` when unknown |
| Market | Geographic/regulatory market or `Not specified` |
| Vehicle applicability | Vehicle class and powertrain scope |
| Source | Interview, repository dataset, telemetry, external publication, experiment, or analysis |
| Finding | What the evidence actually supports |
| Confidence | Low, Medium, High, with rationale |
| Product implication | Potential consequence, not automatic commitment |
| Related features | Feature IDs from the feature register |
| Decision required | Product/architecture decision needed |
| Review date | Revisit date or trigger |
| Lifecycle | Current research state |

Do not fabricate citations, interview quotes, dates, sample sizes, or confidence.

## Lifecycle

```mermaid
stateDiagram-v2
    Captured --> Researching
    Researching --> Validated
    Researching --> Archived
    Validated --> DecisionNeeded
    DecisionNeeded --> Applied
    Applied --> RevisitDue
    RevisitDue --> Researching
    RevisitDue --> Archived
```

Use these exact states: **Captured**, **Researching**, **Validated**, **Decision Needed**, **Applied**, **Revisit Due**, **Archived**.

## Current Records

| ID | Question | Source | Finding | Confidence | Lifecycle | Related features | Decision required |
| --- | --- | --- | --- | --- | --- | --- | --- |
| RS-001 | Which Service Profit signals can the R1 demo data support? | [`manifest.json`](../../../demo-data/service-profit/r1/manifest.json) and validation code | Ten synthetic scenarios cover five opportunity types, evidence/actionability states, revenue attribution, and partial cost/disposition capability | High for synthetic behavior; none for production representativeness | Applied | SP-F001, SP-F004, SP-F005 | Revisit with production-like data |
| RS-002 | Are current dealer pain hypotheses externally validated? | No repository source | No interview or external study corpus is recorded | High confidence in the evidence gap | Captured | Future workflow/analytics features | Product research plan required |
| RS-003 | How portable is data readiness across DMS providers/markets? | R1 synthetic India dataset only | Current implementation is DMS-neutral, but cross-provider data availability is unvalidated | Medium confidence in gap | Captured | SP-F004 and future integrations | Select research markets/providers |
| RS-004 | Which vehicle classes/powertrains need distinct policies? | No repository source | Applicability beyond current generic/synthetic scenarios is unknown | High confidence in gap | Captured | Detection policy features | Define research scope |
| RS-005 | Do self-claiming, due-state, and proposed disposition semantics fit dealer operations? | [Dealer follow-up workflow validation package](RS-005-dealer-follow-up-workflow-validation.md), repository workflow analysis, and PDR-007; no dealer study recorded | A bounded model and executable research package exist, but no participant findings validate ownership, vocabulary, retention, or pilot measures | High confidence in the evidence gap; no fieldwork confidence yet | Captured | SP-F016, SP-F016-A, SP-F017 | Conduct governed fieldwork and apply the documented decision gate before release approval |
| [RS-006](RS-006-first-partner-data-intake-questions.md) | What authorized dealer export, identity, lineage, privacy, and operating evidence is needed for the R1 controlled-file contract? | Planned first-partner discovery; no dealer evidence recorded | Evidence gap is documented; operator-controlled FULL-file pilot questions are defined | High confidence in the evidence gap; no fieldwork confidence | Captured | SP-F004, SP-F020 | Complete mapping, privacy, reconciliation, and dealer-confirmation evidence before G6.6 decision |

## Related Research Summaries

- [Dealer pain points](dealer-pain-points.md)
- [Market opportunities](market-opportunities.md)
- [Data availability research](data-availability-research.md)
- [RS-005 dealer follow-up workflow validation package](RS-005-dealer-follow-up-workflow-validation.md)
