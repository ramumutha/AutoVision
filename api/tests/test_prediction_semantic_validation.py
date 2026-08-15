from __future__ import annotations

import inspect
from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest

from app.evidence.models import EvidenceSufficiency, FindingReviewDecision, FindingReviewStatus
from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_schemas import (
    CanonicalPredictionContext,
    EffectiveFindingContext,
    PredictionServiceHistoryItem,
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
from app.prediction.provider_schemas import (
    PredictionAssessmentCandidate,
    PredictionFactorCandidate,
    PredictionProviderUsage,
    PredictionRequest,
    PredictionResponse,
)
from app.prediction.semantic_validation import PredictionSemanticValidationError, PredictionSemanticValidator
from app.vehicle.models import PowertrainType, VehicleClass

_UNSET = object()


@pytest.fixture()
def identifiers() -> dict[str, object]:
    return {
        "vehicle_id": uuid4(),
        "usage_id": uuid4(),
        "finding_id": uuid4(),
        "service_event_id": uuid4(),
    }


def _context(identifiers: dict[str, object], *, usage: bool = False, service_event: bool = False, finding: bool = False):
    vehicle_id = identifiers["vehicle_id"]
    findings = ()
    if finding:
        findings = (
            EffectiveFindingContext(
                finding_id=identifiers["finding_id"],
                service_event_id=identifiers["service_event_id"],
                analysis_run_id=uuid4(),
                finding_code="F-001",
                original_title="Finding",
                original_description="Finding description",
                effective_title="Finding",
                effective_description="Finding description",
                confidence=Decimal("0.8"),
                evidence_sufficiency=EvidenceSufficiency.SUFFICIENT,
                review_status=FindingReviewStatus.CONFIRMED,
                review_id=uuid4(),
                review_decision=FindingReviewDecision.CONFIRMED,
            ),
        )
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=uuid4(),
        vehicle_id=vehicle_id,
        service_event_id=identifiers["service_event_id"] if service_event else None,
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=vehicle_id,
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
        ),
        usage=(
            PredictionUsageContext(
                usage_snapshot_id=identifiers["usage_id"],
                recorded_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
                odometer_km=Decimal("45500.25"),
            )
            if usage
            else None
        ),
        service_history=(
            PredictionServiceHistoryItem(
                service_event_id=identifiers["service_event_id"],
                source="test",
                state="OPEN",
                revision=1,
                created_at=datetime(2026, 8, 14, tzinfo=timezone.utc),
            ),
        )
        if service_event
        else (),
        findings=findings,
        quality={"quality": PredictionInputQuality.PARTIAL},
    )


def _request(context: CanonicalPredictionContext, *, schema_version: str = "s3.3") -> PredictionRequest:
    return PredictionRequest(schema_version=schema_version, context=context)


def _factor(
    identifiers: dict[str, object],
    *,
    source_type: str | None = "VEHICLE",
    source_id: object = _UNSET,
    factor_code: str = "POWERTRAIN_TYPE",
    label: str = "Powertrain type",
) -> PredictionFactorCandidate:
    return PredictionFactorCandidate(
        factor_type=PredictionFactorType.VEHICLE,
        factor_code=factor_code,
        label=label,
        value_text="ICE",
        source_entity_type=source_type,
        source_entity_id=(
            identifiers["vehicle_id"]
            if source_type == "VEHICLE" and source_id is _UNSET
            else None
            if source_id is _UNSET
            else source_id
        ),
    )


def _assessment(
    identifiers: dict[str, object],
    *,
    factor: PredictionFactorCandidate | None = None,
    metadata: dict | None = None,
    **overrides,
) -> PredictionAssessmentCandidate:
    values = {
        "prediction_type": "TEST",
        "prediction_code": "TEST_CODE",
        "title": "Test title",
        "description": "Test description",
        "severity": PredictionSeverity.INFO,
        "urgency": PredictionUrgency.MONITOR,
        "horizon_type": PredictionHorizonType.UNSPECIFIED,
        "factors": (factor or _factor(identifiers),),
        "metadata": metadata if metadata is not None else {"rule_code": "TEST_RULE", "rule_version": "1"},
    }
    values.update(overrides)
    return PredictionAssessmentCandidate(**values)


def _response(
    context: CanonicalPredictionContext,
    assessment: PredictionAssessmentCandidate | None = None,
    *,
    usage: PredictionProviderUsage | None = None,
    **overrides,
) -> PredictionResponse:
    values = {
        "schema_version": "s3.3",
        "provider": "autovision",
        "model": "deterministic-rules",
        "model_version": "1",
        "assessments": (assessment,) if assessment is not None else (),
        "usage": usage or PredictionProviderUsage(rules_evaluated=0, rules_matched=0),
    }
    values.update(overrides)
    return PredictionResponse(**values)


def _validate(context, response):
    return PredictionSemanticValidator().validate(request=_request(context), response=response)


def test_default_provider_response_passes_and_original_response_is_returned(identifiers) -> None:
    context = _context(identifiers)
    request = _request(context)
    response = DeterministicPredictionProvider().predict(request)
    assert PredictionSemanticValidator().validate(request=request, response=response) is response
    assert response.schema_version == "s3.3"


def test_provider_metadata_and_schema_alignment_are_required(identifiers) -> None:
    context = _context(identifiers)
    for field in ("provider", "model", "model_version"):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction provider metadata is invalid"):
            _validate(context, _response(context, **{field: " "}))
    with pytest.raises(PredictionSemanticValidationError, match="Prediction response schema version is invalid"):
        _validate(context, _response(context, schema_version="s3.2"))
    with pytest.raises(PredictionSemanticValidationError, match="Prediction request and response schema versions do not match"):
        PredictionSemanticValidator().validate(request=_request(context, schema_version="s3.2"), response=_response(context))


def test_canonical_s31_context_is_valid_inside_s33_provider_contract(identifiers) -> None:
    context = _context(identifiers)
    response = _response(context)
    assert context.schema_version == "s3.1"
    assert _validate(context, response) is response


@pytest.mark.parametrize("field", ["prediction_type", "prediction_code", "title", "description"])
def test_blank_assessment_identity_is_rejected(identifiers, field: str) -> None:
    with pytest.raises(PredictionSemanticValidationError, match="Prediction assessment metadata is invalid"):
        _validate(_context(identifiers), _response(_context(identifiers), _assessment(identifiers, **{field: " "})))


def test_duplicate_assessment_code_is_rejected(identifiers) -> None:
    first = _assessment(identifiers)
    second = _assessment(identifiers, factors=(_factor(identifiers, factor_code="OTHER"),))
    with pytest.raises(PredictionSemanticValidationError, match="Duplicate prediction assessment code"):
        _validate(_context(identifiers), _response(_context(identifiers), assessments=(first, second)))


@pytest.mark.parametrize(
    ("horizon_type", "distance", "days"),
    [
        (PredictionHorizonType.DISTANCE, Decimal("100"), None),
        (PredictionHorizonType.TIME, None, 30),
        (PredictionHorizonType.DISTANCE_OR_TIME, Decimal("100"), None),
        (PredictionHorizonType.DISTANCE_OR_TIME, None, 30),
        (PredictionHorizonType.UNSPECIFIED, None, None),
    ],
)
def test_valid_horizon_combinations_are_accepted(identifiers, horizon_type, distance, days) -> None:
    context = _context(identifiers)
    assessment = _assessment(
        identifiers,
        horizon_type=horizon_type,
        horizon_distance_km=distance,
        horizon_time_days=days,
    )
    assert _validate(context, _response(context, assessment))


@pytest.mark.parametrize(
    ("horizon_type", "distance", "days"),
    [
        (PredictionHorizonType.DISTANCE, None, None),
        (PredictionHorizonType.DISTANCE, Decimal("100"), 30),
        (PredictionHorizonType.TIME, None, None),
        (PredictionHorizonType.TIME, Decimal("100"), 30),
        (PredictionHorizonType.UNSPECIFIED, Decimal("100"), None),
        (PredictionHorizonType.UNSPECIFIED, None, 30),
        (PredictionHorizonType.DISTANCE_OR_TIME, None, None),
    ],
)
def test_invalid_horizon_combinations_are_rejected(identifiers, horizon_type, distance, days) -> None:
    context = _context(identifiers)
    assessment = _assessment(identifiers, horizon_type=horizon_type, horizon_distance_km=distance, horizon_time_days=days)
    with pytest.raises(PredictionSemanticValidationError, match="Prediction horizon is invalid"):
        _validate(context, _response(context, assessment))


def test_factor_presence_metadata_pairing_and_duplicate_identity_are_validated(identifiers) -> None:
    context = _context(identifiers)
    base = _assessment(identifiers)
    with pytest.raises(PredictionSemanticValidationError, match="Prediction assessment requires explainability factors"):
        _validate(context, _response(context, base.model_copy(update={"factors": ()})))
    for field in ("factor_code", "label"):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction factor metadata is invalid"):
            _validate(context, _response(context, _assessment(identifiers, factor=_factor(identifiers, **{field: " "}))))
    for factor in (
        _factor(identifiers, source_type="VEHICLE", source_id=None),
        _factor(identifiers, source_type=None, source_id=identifiers["vehicle_id"]),
        _factor(identifiers, source_type=" ", source_id=identifiers["vehicle_id"]),
    ):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction factor source reference is invalid"):
            _validate(context, _response(context, _assessment(identifiers, factor=factor)))
    duplicate = _factor(identifiers)
    with pytest.raises(PredictionSemanticValidationError, match="Duplicate prediction factor"):
        _validate(context, _response(context, _assessment(identifiers, factors=(duplicate, duplicate))))


def test_known_source_lineage_is_validated_and_unknown_sources_are_allowed(identifiers) -> None:
    contexts_and_factors = [
        (_context(identifiers), _factor(identifiers, source_type="VEHICLE")),
        (_context(identifiers, usage=True), _factor(identifiers, source_type="USAGE_SNAPSHOT", source_id=identifiers["usage_id"])),
        (_context(identifiers, finding=True), _factor(identifiers, source_type="FINDING", source_id=identifiers["finding_id"])),
        (_context(identifiers, service_event=True), _factor(identifiers, source_type="SERVICE_EVENT", source_id=identifiers["service_event_id"])),
    ]
    for context, factor in contexts_and_factors:
        assert _validate(context, _response(context, _assessment(identifiers, factor=factor)))

    invalid_factors = [
        _factor(identifiers, source_type="VEHICLE", source_id=uuid4()),
        _factor(identifiers, source_type="USAGE_SNAPSHOT", source_id=identifiers["usage_id"]),
        _factor(identifiers, source_type="FINDING", source_id=uuid4()),
        _factor(identifiers, source_type="SERVICE_EVENT", source_id=uuid4()),
    ]
    invalid_contexts = [
        _context(identifiers),
        _context(identifiers),
        _context(identifiers, finding=True),
        _context(identifiers, service_event=True),
    ]
    for context, factor in zip(invalid_contexts, invalid_factors):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction factor source lineage is invalid"):
            _validate(context, _response(context, _assessment(identifiers, factor=factor)))

    unknown = _factor(identifiers, source_type="CLIMATE_SNAPSHOT", source_id=uuid4())
    assert _validate(_context(identifiers), _response(_context(identifiers), _assessment(identifiers, factor=unknown)))


def test_rule_provenance_and_usage_counts_are_required(identifiers) -> None:
    context = _context(identifiers)
    for metadata in ({}, {"rule_code": "RULE"}, {"rule_version": "1"}, {"rule_code": " ", "rule_version": "1"}):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction rule provenance is missing"):
            _validate(context, _response(context, _assessment(identifiers, metadata=metadata)))
    for usage in (
        PredictionProviderUsage(rules_evaluated=None, rules_matched=0),
        PredictionProviderUsage(rules_evaluated=1, rules_matched=None),
        PredictionProviderUsage(rules_evaluated=1, rules_matched=2),
    ):
        with pytest.raises(PredictionSemanticValidationError, match="Prediction provider usage is invalid"):
            _validate(context, _response(context, usage=usage))
    assert _validate(context, _response(context, usage=PredictionProviderUsage(rules_evaluated=2, rules_matched=2)))


def test_semantic_validator_has_no_sqlalchemy_or_fastapi_dependency() -> None:
    import app.prediction.semantic_validation as validation_module

    source = inspect.getsource(validation_module)
    assert "sqlalchemy" not in source.lower()
    assert "fastapi" not in source.lower()
    assert "Session" not in source
