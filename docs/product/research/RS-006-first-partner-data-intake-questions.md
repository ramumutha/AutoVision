# RS-006: First-Partner Data Intake Questions

- **Research ID:** RS-006
- **Question:** What authorized dealer export, identity, lineage, privacy, and operating evidence is needed to validate the R1 controlled-file contract?
- **Date:** 2026-08-24
- **Market:** Not specified
- **Vehicle applicability:** Not specified
- **Source:** Planned first-partner discovery; no dealer evidence recorded yet
- **Finding:** No first-partner data or workflow evidence is recorded in the repository. The questions below are a research plan, not validated findings.
- **Confidence:** High confidence in the evidence gap; no fieldwork confidence
- **Product implication:** Use an operator-controlled FULL-file pilot and update the capability/release decision only after mapping, privacy, and reconciliation evidence exists.
- **Related features:** SP-F004, SP-F020
- **Decision required:** G6.6 pilot gate and any provider/market-specific contract change
- **Review date:** After first authorized partner discovery
- **Lifecycle:** Captured

## Questions for the Dealer

1. Which source system(s) produce organization, location, customer, vehicle,
   service order/job/line, recommendation, disposition, invoice, and cost data?
2. Which stable source identifiers and correction/version indicators are
   available, and can they be retained for lineage and replay?
3. Which fields are consistently populated, which are optional, and what do
   blank, unknown, cancelled, and deleted values mean?
4. How are source disposition values defined, and can they be distinguished
   from future AutoVision workflow disposition?
5. Are mileage, invoice, invoice-line, currency, and cost values available with
   an agreed unit and business-effective date?
6. Can the dealer verify organization, dealer, and location containment and
   confirm the mapping before materialization?
7. What transfer channel, authorized operator, retention/deletion expectation,
   and permitted PII fields are contractually approved?
8. What correction, resubmission, snapshot coverage, and reconciliation process
   should the pilot use?

## Evidence Gate

Capture a de-identified sample or governed field inventory, source-to-canonical
mapping confirmation, record-count/checksum reconciliation, missing-data
capability report, quarantine outcomes, and dealer approval. Do not claim
provider readiness, production ROI, recovered revenue, complete gross profit,
or customer consent from this research package alone.