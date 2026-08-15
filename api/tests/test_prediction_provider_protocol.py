from __future__ import annotations

import inspect

from app.prediction.provider import (
    PredictionProvider,
    PredictionProviderConfigurationError,
    PredictionProviderError,
    PredictionProviderExecutionError,
    StubPredictionProvider,
)
from app.prediction.provider_schemas import PredictionRequest, PredictionResponse
from tests.test_prediction_provider_schemas import _context


def test_stub_structurally_satisfies_prediction_provider() -> None:
    provider = StubPredictionProvider()
    assert isinstance(provider, PredictionProvider)


def test_provider_metadata_is_available() -> None:
    provider = StubPredictionProvider()
    assert provider.provider_name == "stub"
    assert provider.model_name == "autovision-stub"
    assert provider.model_version == "1"
    assert provider.schema_version == "s3.3"


def test_predict_accepts_request_and_returns_empty_response() -> None:
    provider = StubPredictionProvider()
    request = PredictionRequest(schema_version="s3.3", context=_context())
    response = provider.predict(request)
    assert isinstance(response, PredictionResponse)
    assert response.schema_version == "s3.3"
    assert response.provider == "stub"
    assert response.model == "autovision-stub"
    assert response.model_version == "1"
    assert response.assessments == ()
    assert response.warnings == ()


def test_exception_hierarchy_is_normalized() -> None:
    assert issubclass(PredictionProviderExecutionError, PredictionProviderError)
    assert issubclass(PredictionProviderConfigurationError, PredictionProviderError)
    assert issubclass(PredictionProviderError, Exception)


def test_provider_module_has_no_http_or_session_boundary_dependencies() -> None:
    source = inspect.getsource(__import__("app.prediction.provider", fromlist=["PredictionProvider"]))
    assert "fastapi" not in source.lower()
    assert "Session" not in source
    assert "sqlalchemy.orm" not in source


def test_stub_does_not_require_persistence_models_to_execute() -> None:
    response = StubPredictionProvider().predict(
        PredictionRequest(schema_version="s3.3", context=_context())
    )
    assert isinstance(response, PredictionResponse)
