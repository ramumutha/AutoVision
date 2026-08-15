from __future__ import annotations

from app.prediction.input_schemas import CanonicalPredictionContext, EffectiveFindingContext
from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionSeverity,
    PredictionUrgency,
)
from app.prediction.provider_schemas import (
    PredictionAssessmentCandidate,
    PredictionFactorCandidate,
)
from app.prediction.rules import PredictionRule


class ReviewedFindingAttentionRule:
    rule_code = "REVIEWED_FINDING_ATTENTION"
    rule_version = "1"

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        return bool(context.findings)

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        return tuple(self._assessment(finding) for finding in context.findings)

    @staticmethod
    def _assessment(finding: EffectiveFindingContext) -> PredictionAssessmentCandidate:
        description = finding.effective_description or _finding_description(finding)
        metadata = {
            "finding_code": finding.finding_code,
            "review_status": finding.review_status.value,
            "review_decision": finding.review_decision.value if finding.review_decision is not None else None,
            "review_id": finding.review_id,
            "evidence_sufficiency": finding.evidence_sufficiency.value,
            "supporting_evidence_ids": finding.supporting_evidence_ids,
        }
        factors = [
            PredictionFactorCandidate(
                factor_type=PredictionFactorType.FINDING,
                factor_code="REVIEWED_FINDING",
                label="Reviewed finding",
                description="A reviewed finding contributed to this prediction.",
                value_text=finding.effective_title,
                source_entity_type="FINDING",
                source_entity_id=finding.finding_id,
                metadata=metadata,
            )
        ]
        if finding.confidence is not None:
            factors.append(
                PredictionFactorCandidate(
                    factor_type=PredictionFactorType.FINDING,
                    factor_code="FINDING_CONFIDENCE",
                    label="Finding confidence",
                    value_numeric=finding.confidence,
                    source_entity_type="FINDING",
                    source_entity_id=finding.finding_id,
                )
            )

        return PredictionAssessmentCandidate(
            prediction_type="CONDITION_ATTENTION",
            prediction_code=f"FINDING_{finding.finding_code}",
            title=finding.effective_title,
            description=description,
            severity=PredictionSeverity.INFO,
            urgency=PredictionUrgency.ROUTINE,
            confidence=finding.confidence,
            horizon_type=PredictionHorizonType.UNSPECIFIED,
            recommended_action="Inspect the reviewed finding during the next appropriate service assessment.",
            factors=tuple(factors),
            metadata={"rule_code": ReviewedFindingAttentionRule.rule_code, "rule_version": ReviewedFindingAttentionRule.rule_version},
        )


class UsageServiceAttentionRule:
    rule_code = "USAGE_SERVICE_ATTENTION"
    rule_version = "1"

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        usage = context.usage
        return usage is not None and (usage.odometer_km is not None or usage.engine_hours is not None)

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        usage = context.usage
        if usage is None:
            return ()

        factors: list[PredictionFactorCandidate] = []
        descriptions: list[str] = []
        if usage.odometer_km is not None:
            descriptions.append("recorded odometer data")
            factors.append(
                PredictionFactorCandidate(
                    factor_type=PredictionFactorType.USAGE,
                    factor_code="CURRENT_ODOMETER",
                    label="Current odometer",
                    value_numeric=usage.odometer_km,
                    unit="KM",
                    source_entity_type="USAGE_SNAPSHOT",
                    source_entity_id=usage.usage_snapshot_id,
                )
            )
        if usage.engine_hours is not None:
            descriptions.append("recorded engine-hours data")
            factors.append(
                PredictionFactorCandidate(
                    factor_type=PredictionFactorType.USAGE,
                    factor_code="CURRENT_ENGINE_HOURS",
                    label="Current engine hours",
                    value_numeric=usage.engine_hours,
                    unit="HOURS",
                    source_entity_type="USAGE_SNAPSHOT",
                    source_entity_id=usage.usage_snapshot_id,
                )
            )

        return (
            PredictionAssessmentCandidate(
                prediction_type="USAGE_SERVICE_CONTEXT",
                prediction_code="USAGE_CONTEXT_AVAILABLE",
                title="Vehicle usage should be considered in service planning",
                description=f"{_join_descriptions(descriptions)} is available for service planning.",
                severity=PredictionSeverity.INFO,
                urgency=PredictionUrgency.MONITOR,
                horizon_type=PredictionHorizonType.UNSPECIFIED,
                recommended_action=(
                    "Use the latest recorded vehicle usage together with service history and inspection findings "
                    "when planning maintenance."
                ),
                factors=tuple(factors),
                metadata={"rule_code": UsageServiceAttentionRule.rule_code, "rule_version": UsageServiceAttentionRule.rule_version},
            ),
        )


class PowertrainContextRule:
    rule_code = "POWERTRAIN_CONTEXT"
    rule_version = "1"

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        return True

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        powertrain = context.vehicle.powertrain.value
        descriptions = {
            "ICE": "Internal-combustion powertrain context is available for downstream maintenance rules.",
            "HYBRID": "Hybrid powertrain context is available for downstream maintenance rules.",
            "EV": "Electric powertrain context is available for downstream maintenance rules.",
        }
        return (
            PredictionAssessmentCandidate(
                prediction_type="POWERTRAIN_CONTEXT",
                prediction_code=f"{powertrain}_POWERTRAIN_CONTEXT",
                title="Powertrain context is available",
                description=descriptions[powertrain],
                severity=PredictionSeverity.INFO,
                urgency=PredictionUrgency.MONITOR,
                horizon_type=PredictionHorizonType.UNSPECIFIED,
                factors=(
                    PredictionFactorCandidate(
                        factor_type=PredictionFactorType.VEHICLE,
                        factor_code="POWERTRAIN_TYPE",
                        label="Powertrain type",
                        value_text=powertrain,
                        source_entity_type="VEHICLE",
                        source_entity_id=context.vehicle.vehicle_id,
                    ),
                    PredictionFactorCandidate(
                        factor_type=PredictionFactorType.VEHICLE,
                        factor_code="VEHICLE_CLASS",
                        label="Vehicle class",
                        value_text=context.vehicle.vehicle_class.value,
                        source_entity_type="VEHICLE",
                        source_entity_id=context.vehicle.vehicle_id,
                    ),
                ),
                metadata={"rule_code": PowertrainContextRule.rule_code, "rule_version": PowertrainContextRule.rule_version},
            ),
        )


def default_deterministic_rules() -> tuple[PredictionRule, ...]:
    return (
        ReviewedFindingAttentionRule(),
        UsageServiceAttentionRule(),
        PowertrainContextRule(),
    )


def _finding_description(finding: EffectiveFindingContext) -> str:
    details = [finding.effective_title]
    if finding.effective_component:
        details.append(finding.effective_component)
    if finding.effective_location:
        details.append(finding.effective_location)
    return "Reviewed finding identified: " + "; ".join(details) + "."


def _join_descriptions(descriptions: list[str]) -> str:
    if len(descriptions) == 1:
        return descriptions[0].capitalize()
    return " and ".join(descriptions).capitalize()
