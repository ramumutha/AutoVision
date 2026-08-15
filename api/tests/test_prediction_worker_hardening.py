from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_schemas import CanonicalPredictionContext, PredictionInputQualityContext, PredictionVehicleContext
from app.prediction.models import PredictionAssessment, PredictionInputQuality, PredictionRun, PredictionRunStatus
from app.prediction.provider import PredictionProviderError
from app.prediction.provider_schemas import PredictionProviderUsage, PredictionResponse
from app.prediction.repository import persist_prediction_response as repository_persist_prediction_response
from app.prediction.rules import PredictionRuleRegistry
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


def _run(session, tenant: Tenant, vehicle: Vehicle, *, status=PredictionRunStatus.QUEUED, parent_id=None):
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        status=status,
        provider_name="original-provider",
        provider_version="original-version",
        engine_name="original-engine",
        engine_version="original-engine-version",
        schema_version="original-schema",
        configuration_version="original-config",
        input_snapshot={"frozen": True},
        input_quality=PredictionInputQuality.PARTIAL,
        requested_at=datetime(2026, 8, 10, tzinfo=timezone.utc),
        started_at=datetime(2026, 8, 11, tzinfo=timezone.utc),
        completed_at=datetime(2026, 8, 12, tzinfo=timezone.utc) if status != PredictionRunStatus.PROCESSING else None,
        error_code="SAFE_ERROR" if status == PredictionRunStatus.FAILED else None,
        error_message="Safe error" if status == PredictionRunStatus.FAILED else None,
        retry_of_prediction_run_id=parent_id,
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _fresh(run_id):
    session = SessionLocal()
    try:
        return session.get(PredictionRun, run_id)
    finally:
        session.close()


def _item(run, tenant, vehicle, **overrides):
    values = {"predictionRunId": run.id, "tenantId": tenant.id, "vehicleId": vehicle.id}
    values.update(overrides)
    return PredictionWorkItem(**values)


def _context(tenant, vehicle, quality=PredictionInputQuality.COMPLETE):
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


class Builder:
    def __init__(self, context, error=None):
        self.context = context
        self.error = error
        self.calls = 0

    def build(self, session, **kwargs):
        self.calls += 1
        if self.error:
            raise self.error
        return self.context


class Provider:
    def __init__(self, response=None, error=None):
        self.response = response or PredictionResponse(
            schema_version="s3.3",
            provider="test",
            model="test",
            model_version="1",
            usage=PredictionProviderUsage(rules_evaluated=0, rules_matched=0),
        )
        self.error = error
        self.calls = 0

    def predict(self, request):
        self.calls += 1
        if self.error:
            raise self.error
        return self.response


def _empty_provider():
    return DeterministicPredictionProvider(PredictionRuleRegistry(()))


def test_terminal_and_processing_redelivery_are_immutable(prediction_context) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    for status in (PredictionRunStatus.COMPLETED, PredictionRunStatus.PARTIALLY_COMPLETED, PredictionRunStatus.FAILED, PredictionRunStatus.PROCESSING):
        session = SessionLocal()
        try:
            run = _run(session, tenant, vehicle, status=status)
        finally:
            session.close()
        before = _fresh(run.id)
        PredictionWorker(provider=Provider()).execute(_item(run, tenant, vehicle))
        after = _fresh(run.id)
        assert after.status == before.status
        assert after.started_at == before.started_at
        assert after.completed_at == before.completed_at
        assert after.input_snapshot == before.input_snapshot
        assert after.error_code == before.error_code
        assert after.error_message == before.error_message


def test_tampered_items_and_random_id_are_noops(prediction_context) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    provider = Provider()
    worker = PredictionWorker(provider=provider)
    for item in (
        _item(run, tenant, vehicle, tenantId=uuid4()),
        _item(run, tenant, vehicle, vehicleId=uuid4()),
        _item(run, tenant, vehicle, serviceEventId=uuid4()),
        _item(run, tenant, vehicle, analysisRunId=uuid4()),
        PredictionWorkItem(predictionRunId=uuid4(), tenantId=tenant.id, vehicleId=vehicle.id),
    ):
        worker.execute(item)
    assert _fresh(run.id).status == PredictionRunStatus.QUEUED
    assert provider.calls == 0


@pytest.mark.parametrize(
    "failure,code,message",
    [
        (PredictionProviderError("raw provider"), "PROVIDER_EXECUTION_ERROR", "Prediction provider failed"),
        (PredictionSemanticValidationError("raw semantic"), "SEMANTIC_VALIDATION_ERROR", "Prediction provider response failed semantic validation"),
    ],
)
def test_failures_after_freeze_preserve_snapshot_and_safe_errors(prediction_context, failure, code, message) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = Builder(_context(tenant, vehicle))
    provider = Provider(error=failure) if isinstance(failure, PredictionProviderError) else Provider()
    validator = type("Validator", (), {"validate": lambda self, **kwargs: (_ for _ in ()).throw(failure)})()
    PredictionWorker(input_builder=builder, provider=provider, semantic_validator=validator).execute(_item(run, tenant, vehicle))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == code
    assert failed.error_message == message
    assert failed.input_snapshot != {"frozen": True}
    assert failed.input_quality == PredictionInputQuality.COMPLETE
    assert "raw" not in failed.error_message


def test_persistence_rollback_removes_partial_rows(prediction_context, monkeypatch: pytest.MonkeyPatch) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()

    def fail_after_insert(session, **kwargs):
        repository_persist_prediction_response(session, **kwargs)
        raise RuntimeError("raw database failure")

    import app.prediction.worker as worker_module

    monkeypatch.setattr(worker_module, "persist_prediction_response", fail_after_insert)
    PredictionWorker().execute(_item(run, tenant, vehicle))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == "PREDICTION_PERSISTENCE_ERROR"
    assert failed.error_message == "Prediction persistence failed"
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 0
    finally:
        session.close()


def test_zero_assessment_completion_is_terminal_and_retry_link_isolated(prediction_context) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    session = SessionLocal()
    try:
        parent = _run(session, tenant, vehicle, status=PredictionRunStatus.FAILED)
        child = _run(session, tenant, vehicle, parent_id=parent.id)
    finally:
        session.close()
    builder = Builder(_context(tenant, vehicle))
    provider = _empty_provider()
    worker = PredictionWorker(input_builder=builder, provider=provider)
    item = _item(child, tenant, vehicle)
    worker.execute(item)
    first = _fresh(child.id)
    worker.execute(item)
    second = _fresh(child.id)
    original = _fresh(parent.id)
    assert first.status == PredictionRunStatus.COMPLETED
    assert second.status == PredictionRunStatus.COMPLETED
    assert first.completed_at == second.completed_at
    assert provider is not None
    assert original.status == PredictionRunStatus.FAILED
    assert original.error_code == "SAFE_ERROR"


def test_requested_and_started_timestamps_remain_stable_after_redelivery(prediction_context) -> None:
    tenant, vehicle = prediction_context["tenant"], prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = Builder(_context(tenant, vehicle))
    worker = PredictionWorker(input_builder=builder, provider=_empty_provider())
    item = _item(run, tenant, vehicle)
    worker.execute(item)
    first = _fresh(run.id)
    worker.execute(item)
    second = _fresh(run.id)
    assert second.requested_at == first.requested_at
    assert second.started_at == first.started_at
    assert second.completed_at == first.completed_at
    assert builder.calls == 1
