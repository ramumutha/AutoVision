from __future__ import annotations

import inspect
from datetime import datetime, timezone
from uuid import uuid4

import pytest

from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_schemas import CanonicalPredictionContext, PredictionVehicleContext
from app.prediction.models import PredictionInputQuality, PredictionSeverity, PredictionUrgency, PredictionHorizonType
from app.prediction.provider import (
    PredictionProvider,
    PredictionProviderConfigurationError,
    PredictionProviderError,
    PredictionProviderExecutionError,
)
from app.prediction.provider_schemas import PredictionAssessmentCandidate, PredictionRequest
from app.prediction.rules import (
    PredictionRuleConfigurationError,
    PredictionRuleExecutionError,
    PredictionRuleEvaluation,
    PredictionRuleRegistry,
)
from app.vehicle.models import PowertrainType, VehicleClass


def _context() -> CanonicalPredictionContext:
    vehicle_id = uuid4()
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=uuid4(),
        vehicle_id=vehicle_id,
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=vehicle_id,
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
        ),
        quality={"quality": PredictionInputQuality.INSUFFICIENT},
    )


def _request(*, schema_version: str = "s3.3", context: CanonicalPredictionContext | None = None) -> PredictionRequest:
    return PredictionRequest(schema_version=schema_version, context=context or _context())


def _assessment(code: str) -> PredictionAssessmentCandidate:
    return PredictionAssessmentCandidate(
        prediction_type="TEST",
        prediction_code=code,
        title=code,
        description="Test assessment",
        severity=PredictionSeverity.INFO,
        urgency=PredictionUrgency.MONITOR,
        horizon_type=PredictionHorizonType.UNSPECIFIED,
    )


class RecordingRegistry:
    def __init__(self, evaluation: PredictionRuleEvaluation | None = None, error: Exception | None = None) -> None:
        self.evaluation = evaluation
        self.error = error
        self.contexts: list[CanonicalPredictionContext] = []

    def evaluate(self, context: CanonicalPredictionContext) -> PredictionRuleEvaluation:
        self.contexts.append(context)
        if self.error is not None:
            raise self.error
        assert self.evaluation is not None
        return self.evaluation


def test_provider_structurally_satisfies_protocol_and_metadata_is_exact() -> None:
    provider = DeterministicPredictionProvider()
    assert isinstance(provider, PredictionProvider)
    assert provider.provider_name == "autovision"
    assert provider.model_name == "deterministic-rules"
    assert provider.model_version == "1"
    assert provider.schema_version == "s3.3"


def test_default_registry_has_expected_order_and_provider_preserves_supplied_registry() -> None:
    provider = DeterministicPredictionProvider()
    assert [rule.rule_code for rule in provider.registry.rules] == [
        "REVIEWED_FINDING_ATTENTION",
        "USAGE_SERVICE_ATTENTION",
        "POWERTRAIN_CONTEXT",
    ]
    supplied = PredictionRuleRegistry(())
    assert DeterministicPredictionProvider(supplied).registry is supplied


def test_provider_accepts_s33_request_with_s31_canonical_context() -> None:
    response = DeterministicPredictionProvider(PredictionRuleRegistry(())).predict(
        _request(schema_version="s3.3", context=_context())
    )
    assert response.schema_version == "s3.3"


def test_unsupported_provider_schema_version_is_rejected() -> None:
    with pytest.raises(PredictionProviderConfigurationError, match="Unsupported prediction provider schema version"):
        DeterministicPredictionProvider().predict(_request(schema_version="s3.1"))


def test_empty_registry_returns_metadata_response_with_empty_usage_defaults() -> None:
    response = DeterministicPredictionProvider(PredictionRuleRegistry(())).predict(_request())
    assert response.assessments == ()
    assert response.warnings == ()
    assert response.usage.rules_evaluated == 0
    assert response.usage.rules_matched == 0
    assert response.usage.duration_ms is None
    assert response.provider == "autovision"
    assert response.model == "deterministic-rules"
    assert response.model_version == "1"


def test_evaluation_counts_and_assessment_order_are_propagated() -> None:
    supplied = RecordingRegistry(
        PredictionRuleEvaluation(
            assessments=(_assessment("first"), _assessment("second")),
            rules_evaluated=3,
            rules_matched=2,
        )
    )
    response = DeterministicPredictionProvider(supplied).predict(_request())
    assert [assessment.prediction_code for assessment in response.assessments] == ["first", "second"]
    assert response.usage.rules_evaluated == 3
    assert response.usage.rules_matched == 2
    assert len(supplied.contexts) == 1


def test_default_provider_evaluates_representative_context() -> None:
    response = DeterministicPredictionProvider().predict(_request())
    assert [assessment.prediction_code for assessment in response.assessments] == ["ICE_POWERTRAIN_CONTEXT"]
    assert response.usage.rules_evaluated == 3
    assert response.usage.rules_matched == 1


@pytest.mark.parametrize(
    ("error", "expected"),
    [
        (PredictionRuleConfigurationError("bad config"), PredictionProviderConfigurationError),
        (PredictionRuleExecutionError("bad execution"), PredictionProviderExecutionError),
    ],
)
def test_rule_boundary_errors_are_normalized_with_cause(error: Exception, expected: type[Exception]) -> None:
    with pytest.raises(expected) as raised:
        DeterministicPredictionProvider(RecordingRegistry(error=error)).predict(_request())
    assert raised.value.__cause__ is error


def test_provider_error_is_reraised_unchanged() -> None:
    error = PredictionProviderError("provider failure")
    with pytest.raises(PredictionProviderError) as raised:
        DeterministicPredictionProvider(RecordingRegistry(error=error)).predict(_request())
    assert raised.value is error


def test_unexpected_registry_error_is_normalized_with_cause() -> None:
    error = RuntimeError("unexpected")
    with pytest.raises(PredictionProviderExecutionError, match="Prediction provider execution failed") as raised:
        DeterministicPredictionProvider(RecordingRegistry(error=error)).predict(_request())
    assert raised.value.__cause__ is error


def test_repeated_prediction_has_equivalent_content_and_no_database_boundary() -> None:
    provider = DeterministicPredictionProvider()
    request = _request()
    first = provider.predict(request)
    second = provider.predict(request)
    assert first.model_dump() == second.model_dump()

    import app.prediction.deterministic_provider as provider_module

    source = inspect.getsource(provider_module)
    assert "sqlalchemy" not in source.lower()
    assert "fastapi" not in source.lower()
    assert "Session" not in source
    assert "repository" not in source.lower()
