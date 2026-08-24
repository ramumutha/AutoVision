# Dealer Pain Points

## Evidence Status

This document distinguishes product problems evidenced by implemented workflows from market claims that still require dealer research. No interview transcript or external dealer study is currently stored in the repository.

## Repository-Supported Problems

| Problem | Repository evidence | Product response | Confidence |
| --- | --- | --- | --- |
| Managers need a prioritized view of service revenue opportunities | Service Profit manager UX, summary/queue APIs, tests | Currency-aware KPIs, priority/actionability filters, opportunity-type navigation | High for product intent; external prevalence unvalidated |
| Users need to understand why an opportunity exists | Deterministic explanation resolver, evidence fields, reusable detail UX | Explanation, rationale, evidence basis, recommended action, provenance disclosure | High for implemented response |
| Acting on completed or ambiguous work can be unsafe | Suppression and `REVIEW_REQUIRED` domain/UI tests; SP-DEMO-004/010 | “Do not action” suppression and review-before-contact guidance | High for implemented safeguards |
| Dense desktop workflows do not translate directly to mobile | Responsive UX and automated viewport tests | Concise cards, compact Sort & Filter, routed detail | High for implemented interaction decision |
| Partial source data affects actionability and commercial attribution | R1 demo manifest readiness/capability profile | Data readiness assessment, evidence strength, partial/review states | High for synthetic dataset behavior |

## Hypotheses Requiring Validation

The following are **RESEARCH REQUIRED**:

- frequency and financial impact of missed declined/deferred work;
- manager time spent assembling opportunity lists;
- trust threshold for source-confirmed versus derived evidence;
- acceptable review burden and false-positive rate;
- outreach ownership, channels, consent, and follow-up workflow;
- differences by dealer size, market, brand, and vehicle class/powertrain.

## Research Next Steps

Create research records with explicit market, persona, sample, source, and confidence. Findings should lead to Product Decision Records before they create roadmap commitments or KPI targets.
