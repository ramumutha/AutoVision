from __future__ import annotations

from typing import Protocol, runtime_checkable

from app.prediction.provider_schemas import PredictionRequest, PredictionResponse


class PredictionProviderError(Exception):
    pass


class PredictionProviderExecutionError(PredictionProviderError):
    pass


class PredictionProviderConfigurationError(PredictionProviderError):
    pass


@runtime_checkable
class PredictionProvider(Protocol):
    provider_name: str
    model_name: str
    model_version: str
    schema_version: str

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        ...


class StubPredictionProvider:
    provider_name = "stub"
    model_name = "autovision-stub"
    model_version = "1"
    schema_version = "s3.3"

    def predict(self, request: PredictionRequest) -> PredictionResponse:
        return PredictionResponse(
            schema_version=self.schema_version,
            provider=self.provider_name,
            model=self.model_name,
            model_version=self.model_version,
        )
