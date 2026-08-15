from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import AnalysisRun, AnalysisRunStatus
from app.prediction.models import (
    PredictionAssessment,
    PredictionFactor,
    PredictionInputQuality,
    PredictionRun,
    PredictionRunStatus,
)
from app.prediction.worker import PredictionExecutionFailure, PredictionWorker
from app.prediction.worker_schemas import PredictionWorkItem
from app.service_intake.models import ServiceEvent, ServiceEventState
from app.vehicle.models import Vehicle
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


def _run(
    session,
    tenant: Tenant,
    vehicle: Vehicle,
    *,
    status: PredictionRunStatus = PredictionRunStatus.QUEUED,
    service_event_id=None,
    analysis_run_id=None,
) -> PredictionRun:
    requested_at = datetime(2026, 8, 10, tzinfo=timezone.utc)
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        service_event_id=service_event_id,
        analysis_run_id=analysis_run_id,
        status=status,
        input_quality=PredictionInputQuality.PARTIAL,
        provider_name="autovision",
        provider_version="original-provider",
        schema_version="s3.3",
        input_snapshot={"original": True},
        requested_at=requested_at,
        error_code="ORIGINAL_ERROR",
        error_message="Original error",
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _scoped_ids(session, tenant: Tenant, vehicle: Vehicle):
    event = ServiceEvent(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        source="prediction-worker-test",
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


def _fresh_run(run_id):
    session = SessionLocal()
    try:
        return session.get(PredictionRun, run_id)
    finally:
        session.close()


def test_queued_run_claims_and_stops_at_processing(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
        requested_at = run.requested_at
        worker = PredictionWorker()
        worker.execute(_item(run, tenant, vehicle))
    finally:
        session.close()

    claimed = _fresh_run(run.id)
    assert claimed.status == PredictionRunStatus.COMPLETED
    assert claimed.started_at is not None
    assert claimed.updated_at is not None
    assert claimed.completed_at is not None
    assert claimed.requested_at == requested_at
    assert claimed.input_quality == PredictionInputQuality.COMPLETE
    assert claimed.provider_name == "autovision"
    assert claimed.input_snapshot is not None
    assert claimed.input_snapshot["schema_version"] == "s3.1"
    assert claimed.error_code is None
    assert claimed.error_message is None


def test_duplicate_delivery_and_nonqueued_statuses_are_ignored(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    for status in (
        PredictionRunStatus.PROCESSING,
        PredictionRunStatus.COMPLETED,
        PredictionRunStatus.PARTIALLY_COMPLETED,
        PredictionRunStatus.FAILED,
    ):
        session = SessionLocal()
        try:
            run = _run(session, tenant, vehicle, status=status)
        finally:
            session.close()
        worker = PredictionWorker()
        worker.execute(_item(run, tenant, vehicle))
        assert _fresh_run(run.id).status == status

    session = SessionLocal()
    try:
        queued = _run(session, tenant, vehicle)
    finally:
        session.close()
    worker = PredictionWorker()
    worker.execute(_item(queued, tenant, vehicle))
    claimed_once = _fresh_run(queued.id)
    started_at = claimed_once.started_at
    worker.execute(_item(queued, tenant, vehicle))
    claimed_twice = _fresh_run(queued.id)
    assert claimed_twice.status == PredictionRunStatus.COMPLETED
    assert claimed_twice.started_at == started_at


def test_tampered_work_items_do_not_claim_legitimate_run(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        event, analysis_run = _scoped_ids(session, tenant, vehicle)
        run = _run(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
    finally:
        session.close()

    worker = PredictionWorker()
    tampered_items = (
        _item(run, tenant, vehicle, tenantId=uuid4()),
        _item(run, tenant, vehicle, vehicleId=uuid4()),
        _item(run, tenant, vehicle, serviceEventId=uuid4()),
        _item(run, tenant, vehicle, analysisRunId=uuid4()),
    )
    for tampered in tampered_items:
        worker.execute(tampered)
    assert _fresh_run(run.id).status == PredictionRunStatus.QUEUED


def test_none_optional_scopes_resolve_authoritative_run(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        event, analysis_run = _scoped_ids(session, tenant, vehicle)
        run = _run(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
    finally:
        session.close()
    PredictionWorker().execute(
        PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id)
    )
    assert _fresh_run(run.id).status == PredictionRunStatus.COMPLETED


def test_claim_session_closes_for_success_and_rejection(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()

    successful_factory = TrackingSessionFactory()
    PredictionWorker(session_factory=successful_factory).execute(_item(run, tenant, vehicle))
    assert len(successful_factory.instances) == 4
    assert all(instance.closed for instance in successful_factory.instances)

    rejected_factory = TrackingSessionFactory()
    PredictionWorker(session_factory=rejected_factory).execute(_item(run, tenant, vehicle))
    assert len(rejected_factory.instances) == 1
    assert rejected_factory.instances[0].closed is True


def test_claim_creates_no_assessments_or_factors_and_failure_type_is_available(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    PredictionWorker().execute(_item(run, tenant, vehicle))

    session = SessionLocal()
    try:
        assert session.execute(
            select(func.count()).select_from(PredictionAssessment).where(PredictionAssessment.prediction_run_id == run.id)
        ).scalar_one() == 2
        assert session.execute(
            select(func.count()).select_from(PredictionFactor)
        ).scalar_one() >= 3
    finally:
        session.close()
    failure = PredictionExecutionFailure("TEST_CODE", "Test message")
    assert failure.error_code == "TEST_CODE"
    assert failure.error_message == "Test message"
