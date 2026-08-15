from __future__ import annotations

import inspect
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest

from app.prediction.deterministic_rules import (
    PowertrainContextRule,
    ReviewedFindingAttentionRule,
    UsageServiceAttentionRule,
    default_deterministic_rules,
)
from app.prediction.input_schemas import (
    CanonicalPredictionContext,
    EffectiveFindingContext,
    PredictionUsageContext,
    PredictionVehicleContext,
)
from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionInputQuality,
    PredictionSeverity,
    PredictionUrgency,
)
from app.prediction.provider_schemas import PredictionAssessmentCandidate, PredictionFactorCandidate
from app.prediction.rules import PredictionRule, PredictionRuleRegistry
from app.evidence.models import EvidenceSufficiency, FindingReviewDecision, FindingReviewStatus
from app.vehicle.models import PowertrainType, VehicleClass


def _context(*, findings=(), usage=None, powertrain=PowertrainType.ICE, vehicle_class=VehicleClass.PASSENGER):
    vehicle_id = uuid4()
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=uuid4(),
        vehicle_id=vehicle_id,
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=vehicle_id,
            vehicle_class=vehicle_class,
            powertrain=powertrain,
        ),
        usage=usage,
        findings=tuple(findings),
        quality={"quality": PredictionInputQuality.PARTIAL},
    )


def _finding(*, title="Original title", confidence=Decimal("0.82"), code="BRAKE_FINDING"):
    finding_id = uuid4()
    return EffectiveFindingContext(
        finding_id=finding_id,
        service_event_id=uuid4(),
        analysis_run_id=uuid4(),
        finding_code=code,
        original_title="Original title",
        original_description="Original description",
        original_component="Brake system",
        original_location="Front axle",
        effective_title=title,
        effective_description="Reviewed effective description",
        effective_component="Brake system",
        effective_location="Front axle",
        confidence=confidence,
        evidence_sufficiency=EvidenceSufficiency.SUFFICIENT,
        review_status=FindingReviewStatus.MODIFIED,
        review_id=uuid4(),
        review_decision=FindingReviewDecision.MODIFIED,
        reviewed_at=datetime(2026, 8, 14, tzinfo=timezone.utc),
        supporting_evidence_ids=(uuid4(), uuid4()),
    )


def _usage(*, odometer=None, engine_hours=None, fuel_level=None):
    return PredictionUsageContext(
        usage_snapshot_id=uuid4(),
        recorded_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        odometer_km=odometer,
        engine_hours=engine_hours,
        fuel_level_pct=fuel_level,
    )


def test_reviewed_finding_rule_not_applicable_without_findings() -> None:
    rule = ReviewedFindingAttentionRule()
    context = _context()
    assert isinstance(rule, PredictionRule)
    assert rule.is_applicable(context) is False
    assert rule.evaluate(context) == ()


def test_reviewed_finding_rule_uses_effective_fields_confidence_and_lineage() -> None:
    finding = _finding(title="Modified reviewed title")
    assessment = ReviewedFindingAttentionRule().evaluate(_context(findings=(finding,)))[0]
    assert assessment.prediction_code == "FINDING_BRAKE_FINDING"
    assert assessment.title == "Modified reviewed title"
    assert assessment.confidence == Decimal("0.82")
    assert assessment.factors[0].factor_type == PredictionFactorType.FINDING
    assert assessment.factors[0].source_entity_id == finding.finding_id
    assert assessment.factors[0].value_text == "Modified reviewed title"
    assert assessment.factors[0].metadata["supporting_evidence_ids"] == finding.supporting_evidence_ids
    assert assessment.factors[1].value_numeric == Decimal("0.82")
    assert assessment.recommended_action is not None


def test_reviewed_finding_rule_preserves_multiple_finding_order() -> None:
    findings = (_finding(code="FIRST"), _finding(code="SECOND"))
    assessments = ReviewedFindingAttentionRule().evaluate(_context(findings=findings))
    assert [item.prediction_code for item in assessments] == ["FINDING_FIRST", "FINDING_SECOND"]


def test_usage_rule_applicability_and_factors() -> None:
    rule = UsageServiceAttentionRule()
    assert rule.is_applicable(_context()) is False
    assert rule.is_applicable(_context(usage=_usage(fuel_level=70))) is False

    odometer_context = _context(usage=_usage(odometer=Decimal("45500.25"), fuel_level=70))
    odometer_assessment = rule.evaluate(odometer_context)[0]
    assert rule.is_applicable(odometer_context) is True
    assert odometer_assessment.factors[0].factor_code == "CURRENT_ODOMETER"
    assert odometer_assessment.factors[0].unit == "KM"
    assert odometer_assessment.factors[0].value_numeric == Decimal("45500.25")
    assert all(factor.factor_code != "FUEL_LEVEL" for factor in odometer_assessment.factors)

    engine_context = _context(usage=_usage(engine_hours=Decimal("120.50")))
    engine_assessment = rule.evaluate(engine_context)[0]
    assert rule.is_applicable(engine_context) is True
    assert engine_assessment.factors[0].factor_code == "CURRENT_ENGINE_HOURS"
    assert engine_assessment.factors[0].unit == "HOURS"

    both_assessment = rule.evaluate(_context(usage=_usage(odometer=10, engine_hours=20)))[0]
    assert [factor.factor_code for factor in both_assessment.factors] == [
        "CURRENT_ODOMETER",
        "CURRENT_ENGINE_HOURS",
    ]


def test_powertrain_rule_covers_supported_powertrains_and_vehicle_classes() -> None:
    for powertrain in PowertrainType:
        for vehicle_class in (
            VehicleClass.TWO_WHEELER,
            VehicleClass.PASSENGER,
            VehicleClass.TRUCK,
            VehicleClass.BUS_COACH,
        ):
            assessment = PowertrainContextRule().evaluate(
                _context(powertrain=powertrain, vehicle_class=vehicle_class)
            )[0]
            assert assessment.prediction_code == f"{powertrain.value}_POWERTRAIN_CONTEXT"
            assert assessment.factors[0].value_text == powertrain.value
            assert assessment.factors[1].value_text == vehicle_class.value
            assert assessment.recommended_action is None


def test_all_rule_outputs_are_provider_schema_candidates() -> None:
    contexts = (
        _context(findings=(_finding(),), usage=_usage(odometer=10)),
        _context(usage=_usage(engine_hours=20)),
        _context(powertrain=PowertrainType.EV),
    )
    for rule, context in zip(default_deterministic_rules(), contexts):
        assessments = rule.evaluate(context)
        assert all(isinstance(item, PredictionAssessmentCandidate) for item in assessments)
        assert all(
            isinstance(factor, PredictionFactorCandidate)
            for assessment in assessments
            for factor in assessment.factors
        )


def test_default_rule_factory_order_codes_and_registry_compatibility() -> None:
    rules = default_deterministic_rules()
    assert [rule.rule_code for rule in rules] == [
        "REVIEWED_FINDING_ATTENTION",
        "USAGE_SERVICE_ATTENTION",
        "POWERTRAIN_CONTEXT",
    ]
    assert len({rule.rule_code for rule in rules}) == len(rules)
    result = PredictionRuleRegistry(rules).evaluate(_context())
    assert result.rules_evaluated == 3
    assert result.rules_matched == 1
    assert result.assessments[0].prediction_code == "ICE_POWERTRAIN_CONTEXT"


def test_deterministic_rules_have_no_sqlalchemy_or_fastapi_imports() -> None:
    import app.prediction.deterministic_rules as rules_module

    source = inspect.getsource(rules_module)
    assert "sqlalchemy" not in source.lower()
    assert "fastapi" not in source.lower()
    assert "Session" not in source
    assert "PredictionRun" not in source
    assert "OEM" not in source
