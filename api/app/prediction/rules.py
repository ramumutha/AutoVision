from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterable, Protocol, runtime_checkable

from app.prediction.input_schemas import CanonicalPredictionContext
from app.prediction.provider_schemas import PredictionAssessmentCandidate


class PredictionRuleError(Exception):
    pass


class PredictionRuleConfigurationError(PredictionRuleError):
    pass


class PredictionRuleExecutionError(PredictionRuleError):
    pass


@runtime_checkable
class PredictionRule(Protocol):
    rule_code: str
    rule_version: str

    def is_applicable(self, context: CanonicalPredictionContext) -> bool:
        ...

    def evaluate(self, context: CanonicalPredictionContext) -> tuple[PredictionAssessmentCandidate, ...]:
        ...


@dataclass(frozen=True)
class PredictionRuleEvaluation:
    assessments: tuple[PredictionAssessmentCandidate, ...] = field(default_factory=tuple)
    rules_evaluated: int = 0
    rules_matched: int = 0


class PredictionRuleRegistry:
    def __init__(self, rules: Iterable[PredictionRule] = ()) -> None:
        configured_rules = tuple(rules)
        self._validate_rules(configured_rules)
        self._rules = configured_rules

    @property
    def rules(self) -> tuple[PredictionRule, ...]:
        return self._rules

    def evaluate(self, context: CanonicalPredictionContext) -> PredictionRuleEvaluation:
        assessments: list[PredictionAssessmentCandidate] = []
        rules_evaluated = 0
        rules_matched = 0

        for rule in self._rules:
            rules_evaluated += 1
            try:
                applicable = rule.is_applicable(context)
            except PredictionRuleError:
                raise
            except Exception as exc:
                raise PredictionRuleExecutionError("Prediction rule execution failed") from exc

            if not applicable:
                continue

            rules_matched += 1
            try:
                rule_assessments = rule.evaluate(context)
            except PredictionRuleError:
                raise
            except Exception as exc:
                raise PredictionRuleExecutionError("Prediction rule execution failed") from exc

            if not isinstance(rule_assessments, tuple) or not all(
                isinstance(assessment, PredictionAssessmentCandidate)
                for assessment in rule_assessments
            ):
                raise PredictionRuleExecutionError("Prediction rule returned invalid assessments")
            assessments.extend(rule_assessments)

        return PredictionRuleEvaluation(
            assessments=tuple(assessments),
            rules_evaluated=rules_evaluated,
            rules_matched=rules_matched,
        )

    @staticmethod
    def _validate_rules(rules: tuple[PredictionRule, ...]) -> None:
        rule_codes: set[str] = set()
        for rule in rules:
            rule_code = getattr(rule, "rule_code", None)
            rule_version = getattr(rule, "rule_version", None)
            if not isinstance(rule_code, str) or not rule_code.strip():
                raise PredictionRuleConfigurationError("Prediction rule metadata is invalid")
            if not isinstance(rule_version, str) or not rule_version.strip():
                raise PredictionRuleConfigurationError("Prediction rule metadata is invalid")
            if rule_code in rule_codes:
                raise PredictionRuleConfigurationError("Duplicate prediction rule code")
            rule_codes.add(rule_code)
