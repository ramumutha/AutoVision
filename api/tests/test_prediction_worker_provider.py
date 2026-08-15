from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from pydantic import BaseModel, ValidationError
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import AnalysisRun
from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_schemas import CanonicalPredictionContext, PredictionInputQualityContext, PredictionVehicleContext
from app.prediction.models import PredictionAssessment, PredictionInputQuality, PredictionRun, PredictionRunStatus
from app.prediction.provider import (
    PredictionProviderConfigurationError,
    PredictionProviderError,
    PredictionProviderExecutionError,
)
from app.prediction.provider_schemas import PredictionRequest, PredictionResponse
from app.prediction.semantic_validation import PredictionSemanticValidationError
from app.prediction.worker import PredictionWorker
from app.prediction.worker_schemas import PredictionWorkItem
from app.vehicle.models import PowertrainType, Vehicle, VehicleClass
from scripts.seed_demo import reset_demo_data, seed_demo_data


@pytest.fixture()
def prediction_context() -> dict[str, object]:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        return {"tenant": tenant, "vehicle": vehicle}
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    yield
    reset_demo_data()


def _run(session, tenant: Tenant, vehicle: Vehicle) -> PredictionRun:
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        status=PredictionRunStatus.QUEUED,
        provider_name="original-provider",
        provider_version="original-version",
        engine_name="original-engine",
        engine_version="original-engine-version",
        schema_version="s3.3",
        configuration_version="original-config",
        input_snapshot={"before": True},
        input_quality=PredictionInputQuality.PARTIAL,
        requested_at=datetime(2026, 8, 10, tzinfo=timezone.utc),
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _item(run: PredictionRun, tenant: Tenant, vehicle: Vehicle) -> PredictionWorkItem:
    return PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id)


def _fresh(run_id):
    session = SessionLocal()
    try:
        return session.get(PredictionRun, run_id)
    finally:
        session.close()


def _context(tenant: Tenant, vehicle: Vehicle, quality: PredictionInputQuality = PredictionInputQuality.COMPLETE):
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=vehicle.id,
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
        ),
        quality=PredictionInputQualityContext(quality=quality),
    )


def _response() -> PredictionResponse:
    return PredictionResponse(
        schema_version="s3.3",
        provider="test-provider",
        model="test-model",
        model_version="1",
        usage={"rules_evaluated": 0, "rules_matched": 0},
    )


class CapturingProvider:
    provider_name = "test-provider"
    model_name = "test-model"
    model_version = "1"
    schema_version = "s3.3"

    def __init__(self, response: object | None = None, error: Exception | None = None) -> None:
        self.requests: list[PredictionRequest] = []
        self.response = response if response is not None else _response()
        self.error = error

    def predict(self, request: PredictionRequest):
        self.requests.append(request)
        if self.error is not None:
            raise self.error
        return self.response


class CapturingValidator:
    def __init__(self, error: Exception | None = None) -> None:
        self.requests: list[PredictionRequest] = []
        self.responses: list[PredictionResponse] = []
        self.error = error

    def validate(self, *, request: PredictionRequest, response: PredictionResponse) -> PredictionResponse:
        self.requests.append(request)
        self.responses.append(response)
        if self.error is not None:
            raise self.error
        return response


class StubBuilder:
    def __init__(self, context: CanonicalPredictionContext, error: Exception | None = None) -> None:
        self.context = context
        self.error = error
        self.calls = 0

    def build(self, session, **kwargs):
        self.calls += 1
        if self.error is not None:
            raise self.error
        return self.context


@pytest.mark.parametrize(
    "error_code,error",
    [
        ("PROVIDER_CONFIGURATION_ERROR", PredictionProviderConfigurationError("raw config")),
        ("PROVIDER_EXECUTION_ERROR", PredictionProviderExecutionError("raw execution")),
        ("PROVIDER_EXECUTION_ERROR", PredictionProviderError("raw provider")),
    ],
)
def test_provider_failures_are_safely_marked_failed(prediction_context, error_code, error) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    worker = PredictionWorker(
        input_builder=StubBuilder(_context(tenant, vehicle)),
        provider=CapturingProvider(error=error),
    )
    worker.execute(_item(run, tenant, vehicle))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == error_code
    assert failed.error_message in {
        "Prediction provider configuration was invalid",
        "Prediction provider execution failed",
        "Prediction provider failed",
    }
    assert "raw" not in failed.error_message


def test_provider_request_and_validator_use_same_frozen_context(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = StubBuilder(_context(tenant, vehicle))
    provider = CapturingProvider()
    validator = CapturingValidator()
    PredictionWorker(input_builder=builder, provider=provider, semantic_validator=validator).execute(
        _item(run, tenant, vehicle)
    )
    persisted = _fresh(run.id)
    assert builder.calls == 1
    assert len(provider.requests) == 1
    assert len(validator.requests) == 1
    assert validator.responses[0] is not None
    assert provider.requests[0].schema_version == "s3.3"
    assert provider.requests[0].context.schema_version == "s3.1"
    assert provider.requests[0].context.model_dump(mode="json") == persisted.input_snapshot
    assert validator.requests[0].context.model_dump(mode="json") == persisted.input_snapshot
    assert persisted.status == PredictionRunStatus.COMPLETED
    assert persisted.completed_at is not None
    assert persisted.provider_name == "test-provider"
    assert persisted.provider_version == "1"
    assert persisted.engine_name == "test-model"
    assert persisted.engine_version == "1"
    assert persisted.schema_version == "s3.3"
    assert persisted.configuration_version == "deterministic-rules-v1"
    assert persisted.input_snapshot == persisted.input_snapshot


def test_real_deterministic_provider_and_semantic_validator_succeed(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    PredictionWorker().execute(_item(run, tenant, vehicle))
    persisted = _fresh(run.id)
    assert persisted.status == PredictionRunStatus.COMPLETED
    assert persisted.input_snapshot is not None
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 2
    finally:
        session.close()


def test_structural_and_semantic_response_failures_are_safe(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    for response, code, message in (
        ({"invalid": "response"}, "INVALID_PROVIDER_RESPONSE", "Prediction provider response was invalid"),
        (_response(), "SEMANTIC_VALIDATION_ERROR", "Prediction provider response failed semantic validation"),
    ):
        session = SessionLocal()
        try:
            run = _run(session, tenant, vehicle)
        finally:
            session.close()
        validator = CapturingValidator(
            error=PredictionSemanticValidationError("raw semantic details") if code.startswith("SEMANTIC") else None
        )
        PredictionWorker(
            input_builder=StubBuilder(_context(tenant, vehicle)),
            provider=CapturingProvider(response=response),
            semantic_validator=validator,
        ).execute(_item(run, tenant, vehicle))
        failed = _fresh(run.id)
        assert failed.status == PredictionRunStatus.FAILED
        assert failed.error_code == code
        assert failed.error_message == message
        assert "raw" not in failed.error_message


def test_insufficient_or_builder_failure_prevents_provider_call(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    for builder in (
        StubBuilder(_context(tenant, vehicle, PredictionInputQuality.INSUFFICIENT)),
        StubBuilder(_context(tenant, vehicle), error=LookupError("raw lookup")),
    ):
        session = SessionLocal()
        try:
            run = _run(session, tenant, vehicle)
        finally:
            session.close()
        provider = CapturingProvider()
        PredictionWorker(input_builder=builder, provider=provider).execute(_item(run, tenant, vehicle))
        assert provider.requests == []
        assert _fresh(run.id).status == PredictionRunStatus.FAILED


def test_duplicate_execute_does_not_call_provider_again_and_no_response_persistence(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = StubBuilder(_context(tenant, vehicle))
    provider = CapturingProvider()
    worker = PredictionWorker(input_builder=builder, provider=provider)
    item = _item(run, tenant, vehicle)
    worker.execute(item)
    snapshot = _fresh(run.id).input_snapshot
    worker.execute(item)
    assert len(provider.requests) == 1
    assert builder.calls == 1
    assert _fresh(run.id).input_snapshot == snapshot


def test_provider_and_validator_are_injectable(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    provider = CapturingProvider()
    validator = CapturingValidator()
    worker = PredictionWorker(
        input_builder=StubBuilder(_context(tenant, vehicle)),
        provider=provider,
        semantic_validator=validator,
    )
    assert worker.provider is provider
    assert worker.semantic_validator is validator
    worker.execute(_item(run, tenant, vehicle))
    assert len(provider.requests) == 1
    assert len(validator.responses) == 1
