from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.prediction.input_schemas import (
    CanonicalPredictionContext,
    PredictionInputQualityContext,
    PredictionVehicleContext,
)
from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionInputQuality,
    PredictionSeverity,
    PredictionUrgency,
)
from app.prediction.provider_schemas import (
    PredictionAssessmentCandidate,
    PredictionFactorCandidate,
    PredictionProviderUsage,
    PredictionRequest,
    PredictionResponse,
)
from app.vehicle.models import PowertrainType, VehicleClass


def _context() -> CanonicalPredictionContext:
    tenant_id = uuid4()
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=tenant_id,
        vehicle_id=uuid4(),
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=uuid4(),
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
        ),
        quality=PredictionInputQualityContext(quality=PredictionInputQuality.PARTIAL),
    )


def _factor(**overrides) -> PredictionFactorCandidate:
    values = {
        "factor_type": PredictionFactorType.USAGE,
        "factor_code": "USAGE_ODOMETER",
        "label": "Odometer",
    }
    values.update(overrides)
    return PredictionFactorCandidate(**values)


def _assessment(**overrides) -> PredictionAssessmentCandidate:
    values = {
        "prediction_type": "MAINTENANCE",
        "prediction_code": "BRAKE_WEAR",
        "title": "Brake wear risk",
        "description": "Brake wear may require service.",
        "severity": PredictionSeverity.MEDIUM,
        "urgency": PredictionUrgency.SOON,
        "horizon_type": PredictionHorizonType.DISTANCE_OR_TIME,
    }
    values.update(overrides)
    return PredictionAssessmentCandidate(**values)


def test_prediction_request_wraps_canonical_context() -> None:
    context = _context()
    request = PredictionRequest(schema_version="s3.3", context=context)
    assert request.context is context
    assert request.schema_version == "s3.3"


def test_provider_schemas_are_frozen_and_reject_extra_fields() -> None:
    factor = _factor()
    with pytest.raises(ValidationError):
        factor.factor_code = "changed"
    with pytest.raises(ValidationError):
        PredictionRequest(schema_version="s3.3", context=_context(), unexpected=True)
    with pytest.raises(ValidationError):
        _factor(unexpected=True)
    with pytest.raises(ValidationError):
        _assessment(unexpected=True)
    with pytest.raises(ValidationError):
        PredictionProviderUsage(unexpected=True)
    with pytest.raises(ValidationError):
        PredictionResponse(
            schema_version="s3.3",
            provider="rules",
            model="deterministic",
            model_version="1",
            unexpected=True,
        )


def test_factor_candidate_supports_provider_explainability_fields() -> None:
    source_id = uuid4()
    factor = _factor(
        description="Recent mileage contributes to the assessment.",
        value_numeric=Decimal("45500.25"),
        value_text="high mileage",
        unit="km",
        weight=Decimal("0.75"),
        source_entity_type="usage_snapshot",
        source_entity_id=source_id,
        metadata={"rule": "mileage-threshold"},
    )
    assert factor.value_numeric == Decimal("45500.25")
    assert factor.weight == Decimal("0.75")
    assert factor.source_entity_id == source_id


def test_assessment_candidate_and_empty_factor_default() -> None:
    assessment = _assessment(confidence=Decimal("0.875"), horizon_distance_km=Decimal("1200.50"), horizon_time_days=90)
    assert assessment.confidence == Decimal("0.875")
    assert assessment.horizon_distance_km == Decimal("1200.50")
    assert assessment.factors == ()


def test_response_empty_defaults() -> None:
    response = PredictionResponse(
        schema_version="any-version",
        provider="rules",
        model="deterministic",
        model_version="1.0",
    )
    assert response.assessments == ()
    assert response.warnings == ()
    assert response.usage == PredictionProviderUsage()


@pytest.mark.parametrize("field,value", [("confidence", Decimal("-0.01")), ("confidence", Decimal("1.01"))])
def test_confidence_bounds(field: str, value: Decimal) -> None:
    with pytest.raises(ValidationError):
        _assessment(**{field: value})


@pytest.mark.parametrize("field", ["horizon_distance_km", "horizon_time_days"])
def test_negative_horizon_values_rejected(field: str) -> None:
    with pytest.raises(ValidationError):
        _assessment(**{field: -1})


@pytest.mark.parametrize("field", ["rules_evaluated", "rules_matched", "duration_ms"])
def test_negative_usage_metrics_rejected(field: str) -> None:
    with pytest.raises(ValidationError):
        PredictionProviderUsage(**{field: -1})


def test_decimal_values_are_preserved() -> None:
    factor = _factor(value_numeric=Decimal("1.2300"), weight=Decimal("0.1250"))
    assessment = _assessment(confidence=Decimal("0.5000"), horizon_distance_km=Decimal("10.00"))
    assert factor.value_numeric == Decimal("1.2300")
    assert factor.weight == Decimal("0.1250")
    assert assessment.confidence == Decimal("0.5000")
    assert assessment.horizon_distance_km == Decimal("10.00")


def test_schema_versions_are_structurally_arbitrary() -> None:
    response = PredictionResponse(
        schema_version="future-provider-contract",
        provider="rules",
        model="deterministic",
        model_version="future",
    )
    request = PredictionRequest(schema_version="experimental", context=_context())
    assert response.schema_version == "future-provider-contract"
    assert request.schema_version == "experimental"
