from __future__ import annotations

from app.prediction.deterministic_rules import default_deterministic_rules
from app.prediction.provider import (
    PredictionProviderConfigurationError,
    PredictionProviderError,
    PredictionProviderExecutionError,
)
from app.prediction.provider_schemas import PredictionProviderUsage, PredictionRequest, PredictionResponse
from app.prediction.rules import (
    PredictionRuleConfigurationError,
    PredictionRuleExecutionError,
    PredictionRuleRegistry,
)


class DeterministicPredictionProvider:
    provider_name = "autovision"
    model_name = "deterministic-rules"
    model_version = "1"
    schema_version = "s3.3"

    def __init__(self, registry: PredictionRuleRegistry | None = None) -> None:
        self.registry = registry if registry is not None else PredictionRuleRegistry(default_deterministic_rules())

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        if request.schema_version != self.schema_version:
            raise PredictionProviderConfigurationError("Unsupported prediction provider schema version")

        try:
            evaluation = self.registry.evaluate(request.context)
        except PredictionProviderError:
            raise
        except PredictionRuleConfigurationError as exc:
            raise PredictionProviderConfigurationError("Prediction rule configuration failed") from exc
        except PredictionRuleExecutionError as exc:
            raise PredictionProviderExecutionError("Prediction rule execution failed") from exc
        except Exception as exc:
            raise PredictionProviderExecutionError("Prediction provider execution failed") from exc

        return PredictionResponse(
            schema_version=self.schema_version,
            provider=self.provider_name,
            model=self.model_name,
            model_version=self.model_version,
            assessments=evaluation.assessments,
            warnings=(),
            usage=PredictionProviderUsage(
                rules_evaluated=evaluation.rules_evaluated,
                rules_matched=evaluation.rules_matched,
                duration_ms=None,
            ),
        )
