from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.prediction.input_schemas import CanonicalPredictionContext, PredictionVehicleContext
from app.prediction.models import (
    PredictionHorizonType,
    PredictionInputQuality,
    PredictionSeverity,
    PredictionUrgency,
)
from app.prediction.provider_schemas import PredictionAssessmentCandidate
from app.prediction.rules import (
    PredictionRule,
    PredictionRuleConfigurationError,
    PredictionRuleError,
    PredictionRuleEvaluation,
    PredictionRuleExecutionError,
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


class NeverApplicableRule:
    rule_code = "never"
    rule_version = "1"

    def __init__(self) -> None:
        self.applicable_calls = 0
        self.evaluate_calls = 0

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        self.applicable_calls += 1
        return False

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        self.evaluate_calls += 1
        return (_assessment("never-evaluated"),)


class ApplicableEmptyRule:
    rule_code = "empty"
    rule_version = "1"

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        return True

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        return ()


class ApplicableAssessmentRule:
    def __init__(self, rule_code: str, *codes: str) -> None:
        self.rule_code = rule_code
        self.rule_version = "1"
        self.codes = codes

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        return True

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        return tuple(_assessment(code) for code in self.codes)


class FailingRule:
    rule_code = "failing"
    rule_version = "1"

    def __init__(self, error: Exception, *, fail_in_evaluate: bool = False) -> None:
        self.error = error
        self.fail_in_evaluate = fail_in_evaluate

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        if not self.fail_in_evaluate:
            raise self.error
        return True

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        raise self.error


class InvalidReturnRule:
    rule_code = "invalid"
    rule_version = "1"

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        return True

    def evaluate(self, context: CanonicalPredictionContext):
        return [_assessment("wrong-container")]


def test_empty_registry_and_immutable_result_work() -> None:
    result = PredictionRuleRegistry().evaluate(_context())
    assert result == PredictionRuleEvaluation()
    with pytest.raises(AttributeError):
        result.rules_evaluated = 4


def test_registry_preserves_order_and_aggregates_assessments() -> None:
    first = ApplicableAssessmentRule("first", "first-a", "first-b")
    second = ApplicableAssessmentRule("second", "second-a")
    registry = PredictionRuleRegistry((first, second))
    assert registry.rules == (first, second)
    result = registry.evaluate(_context())
    assert [assessment.prediction_code for assessment in result.assessments] == [
        "first-a", "first-b", "second-a"
    ]
    assert result.rules_evaluated == 2
    assert result.rules_matched == 2


def test_rule_metadata_is_exposed_and_empty_match_is_valid() -> None:
    rule = ApplicableEmptyRule()
    registry = PredictionRuleRegistry((rule,))
    result = registry.evaluate(_context())
    assert registry.rules[0].rule_code == "empty"
    assert registry.rules[0].rule_version == "1"
    assert result.assessments == ()
    assert result.rules_evaluated == 1
    assert result.rules_matched == 1


@pytest.mark.parametrize(
    "rule",
    [
        type("BlankCode", (), {"rule_code": " ", "rule_version": "1"})(),
        type("BlankVersion", (), {"rule_code": "valid", "rule_version": "\t"})(),
    ],
)
def test_invalid_rule_metadata_is_rejected(rule) -> None:
    with pytest.raises(PredictionRuleConfigurationError, match="Prediction rule metadata is invalid"):
        PredictionRuleRegistry((rule,))


def test_duplicate_rule_code_is_rejected() -> None:
    with pytest.raises(PredictionRuleConfigurationError, match="Duplicate prediction rule code"):
        PredictionRuleRegistry((ApplicableEmptyRule(), ApplicableEmptyRule()))


def test_non_applicable_rule_is_not_evaluated_and_applicable_rule_is_evaluated() -> None:
    never = NeverApplicableRule()
    applicable = ApplicableAssessmentRule("applicable", "assessment")
    result = PredictionRuleRegistry((never, applicable)).evaluate(_context())
    assert never.applicable_calls == 1
    assert never.evaluate_calls == 0
    assert [item.prediction_code for item in result.assessments] == ["assessment"]
    assert result.rules_evaluated == 2
    assert result.rules_matched == 1


def test_prediction_rule_error_is_propagated_unchanged() -> None:
    error = PredictionRuleError("rule failure")
    with pytest.raises(PredictionRuleError) as raised:
        PredictionRuleRegistry((FailingRule(error),)).evaluate(_context())
    assert raised.value is error


def test_unexpected_applicability_and_evaluation_errors_are_normalized() -> None:
    with pytest.raises(PredictionRuleExecutionError, match="Prediction rule execution failed") as raised:
        PredictionRuleRegistry((FailingRule(RuntimeError("applicability")),)).evaluate(_context())
    assert isinstance(raised.value.__cause__, RuntimeError)

    with pytest.raises(PredictionRuleExecutionError, match="Prediction rule execution failed") as raised:
        PredictionRuleRegistry((FailingRule(RuntimeError("evaluation"), fail_in_evaluate=True),)).evaluate(_context())
    assert isinstance(raised.value.__cause__, RuntimeError)


def test_invalid_assessment_return_is_normalized() -> None:
    with pytest.raises(PredictionRuleExecutionError, match="Prediction rule returned invalid assessments"):
        PredictionRuleRegistry((InvalidReturnRule(),)).evaluate(_context())


def test_runtime_protocol_compatibility_works() -> None:
    assert isinstance(ApplicableEmptyRule(), PredictionRule)


def test_rule_module_has_no_sqlalchemy_or_fastapi_dependency() -> None:
    import inspect
    import app.prediction.rules as rules_module

    source = inspect.getsource(rules_module)
    assert "sqlalchemy" not in source.lower()
    assert "fastapi" not in source.lower()
    assert "Session" not in source
