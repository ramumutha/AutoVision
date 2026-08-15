from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import AnalysisRun, AnalysisRunStatus
from app.prediction.input_schemas import (
    CanonicalPredictionContext,
    PredictionInputQualityContext,
    PredictionUsageContext,
    PredictionVehicleContext,
)
from app.prediction.models import (
    PredictionAssessment,
    PredictionFactor,
    PredictionInputQuality,
    PredictionRun,
    PredictionRunStatus,
)
from app.prediction.worker import PredictionWorker
from app.prediction.worker_schemas import PredictionWorkItem
from app.service_intake.models import ServiceEvent, ServiceEventState
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


def _run(session, tenant: Tenant, vehicle: Vehicle, *, service_event_id=None, analysis_run_id=None) -> PredictionRun:
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        service_event_id=service_event_id,
        analysis_run_id=analysis_run_id,
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
        error_code="ORIGINAL_ERROR",
        error_message="Original error",
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _scope(session, tenant: Tenant, vehicle: Vehicle):
    event = ServiceEvent(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        source="prediction-worker-input-test",
        state=ServiceEventState.OPEN,
        revision=1,
        opened_at=datetime.now(timezone.utc),
    )
    session.add(event)
    session.flush()
    analysis_run = AnalysisRun(
        tenant_id=tenant.id,
        service_event_id=event.id,
        status=AnalysisRunStatus.SUCCEEDED,
        requested_at=datetime.now(timezone.utc),
    )
    session.add(analysis_run)
    session.flush()
    return event, analysis_run


def _item(run: PredictionRun, tenant: Tenant, vehicle: Vehicle, **overrides) -> PredictionWorkItem:
    values = {
        "predictionRunId": run.id,
        "tenantId": tenant.id,
        "vehicleId": vehicle.id,
        "serviceEventId": run.service_event_id,
        "analysisRunId": run.analysis_run_id,
    }
    values.update(overrides)
    return PredictionWorkItem(**values)


def _context(
    tenant: Tenant,
    vehicle: Vehicle,
    *,
    quality: PredictionInputQuality,
    usage: bool = False,
) -> CanonicalPredictionContext:
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
        usage=(
            PredictionUsageContext(
                usage_snapshot_id=uuid4(),
                recorded_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
                odometer_km=Decimal("45500.25"),
            )
            if usage
            else None
        ),
        quality=PredictionInputQualityContext(quality=quality),
    )


class StubInputBuilder:
    def __init__(self, context: CanonicalPredictionContext | None = None, error: Exception | None = None) -> None:
        self.context = context
        self.error = error
        self.calls: list[dict[str, object]] = []

    def build(self, session, **kwargs):
        self.calls.append(kwargs)
        if self.error is not None:
            raise self.error
        assert self.context is not None
        return self.context


class TrackingSession:
    def __init__(self) -> None:
        self.session = SessionLocal()
        self.closed = False

    def __getattr__(self, name: str):
        return getattr(self.session, name)

    def close(self) -> None:
        self.closed = True
        self.session.close()


class TrackingSessionFactory:
    def __init__(self) -> None:
        self.instances: list[TrackingSession] = []

    def __call__(self) -> TrackingSession:
        session = TrackingSession()
        self.instances.append(session)
        return session


def _fresh(run_id):
    session = SessionLocal()
    try:
        return session.get(PredictionRun, run_id)
    finally:
        session.close()


def test_real_builder_freezes_complete_input_snapshot_and_preserves_run_metadata(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()

    worker = PredictionWorker()
    worker.execute(_item(run, tenant, vehicle))
    persisted = _fresh(run.id)
    assert persisted.status == PredictionRunStatus.COMPLETED
    assert persisted.input_quality == PredictionInputQuality.COMPLETE
    assert persisted.input_snapshot["schema_version"] == "s3.1"
    assert persisted.input_snapshot["vehicle"]["vehicle_id"] == str(vehicle.id)
    assert persisted.input_snapshot["usage"] is not None
    assert persisted.input_snapshot["service_history"] == []
    assert persisted.input_snapshot["findings"] == []
    assert persisted.input_snapshot["external_context"] == {
        "climate": None,
        "road_condition": None,
        "driving_behavior": None,
        "load_profile": None,
        "additional_context": {},
    }
    assert persisted.provider_name == "autovision"
    assert persisted.provider_version == "1"
    assert persisted.engine_name == "deterministic-rules"
    assert persisted.engine_version == "1"
    assert persisted.schema_version == "s3.3"
    assert persisted.configuration_version == "deterministic-rules-v1"
    assert persisted.completed_at is not None
    assert persisted.error_code is None
    assert persisted.error_message is None
    assert session is not None


def test_snapshot_matches_real_builder_contract_and_scope_is_forwarded(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        event, analysis_run = _scope(session, tenant, vehicle)
        event_id = event.id
        analysis_run_id = analysis_run.id
        run = _run(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
    finally:
        session.close()

    builder = StubInputBuilder(_context(tenant, vehicle, quality=PredictionInputQuality.PARTIAL, usage=True))
    worker = PredictionWorker(input_builder=builder)
    worker.execute(_item(run, tenant, vehicle))
    context = builder.context
    persisted = _fresh(run.id)
    assert context is not None
    assert persisted.input_snapshot == context.model_dump(mode="json")
    assert persisted.input_quality == PredictionInputQuality.PARTIAL
    assert builder.calls == [{
        "tenant_id": tenant.id,
        "vehicle_id": vehicle.id,
        "service_event_id": event_id,
        "analysis_run_id": analysis_run_id,
    }]


def test_insufficient_input_fails_with_safe_error_and_completed_at(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = StubInputBuilder(_context(tenant, vehicle, quality=PredictionInputQuality.INSUFFICIENT))
    PredictionWorker(input_builder=builder).execute(_item(run, tenant, vehicle))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == "INSUFFICIENT_INPUT"
    assert failed.error_message == "Prediction input was insufficient"
    assert failed.completed_at is not None
    assert failed.input_snapshot == {"before": True}


@pytest.mark.parametrize(
    ("error", "code", "message"),
    [
        (LookupError("secret lookup details"), "PREDICTION_INPUT_ERROR", "Prediction input could not be resolved"),
        (ValueError("secret validation details"), "PREDICTION_INPUT_ERROR", "Prediction input was invalid"),
        (RuntimeError("secret internal details"), "PREDICTION_WORKER_ERROR", "Prediction worker failed"),
    ],
)
def test_input_resolution_errors_are_safe_and_mark_failed(prediction_context, error, code, message) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    PredictionWorker(input_builder=StubInputBuilder(error=error)).execute(_item(run, tenant, vehicle))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == code
    assert failed.error_message == message
    assert "secret" not in failed.error_message


def test_tampered_item_cannot_claim_or_freeze_legitimate_run(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    worker = PredictionWorker(input_builder=StubInputBuilder(_context(tenant, vehicle, quality=PredictionInputQuality.COMPLETE)))
    worker.execute(_item(run, tenant, vehicle, tenantId=uuid4()))
    unchanged = _fresh(run.id)
    assert unchanged.status == PredictionRunStatus.QUEUED
    assert unchanged.input_snapshot == {"before": True}


def test_duplicate_execute_after_freeze_does_not_rebuild_input(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    builder = StubInputBuilder(_context(tenant, vehicle, quality=PredictionInputQuality.COMPLETE))
    worker = PredictionWorker(input_builder=builder)
    item = _item(run, tenant, vehicle)
    worker.execute(item)
    worker.execute(item)
    assert len(builder.calls) == 1
    assert _fresh(run.id).status == PredictionRunStatus.COMPLETED


def test_resolution_and_persistence_sessions_close_and_no_assessments_or_factors(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    setup = SessionLocal()
    try:
        run = _run(setup, tenant, vehicle)
    finally:
        setup.close()
    factory = TrackingSessionFactory()
    builder = StubInputBuilder(_context(tenant, vehicle, quality=PredictionInputQuality.PARTIAL))
    PredictionWorker(session_factory=factory, input_builder=builder).execute(_item(run, tenant, vehicle))
    assert len(factory.instances) == 4
    assert all(instance.closed for instance in factory.instances)
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 1
        assert session.execute(select(func.count()).select_from(PredictionFactor)).scalar_one() == 2
    finally:
        session.close()
