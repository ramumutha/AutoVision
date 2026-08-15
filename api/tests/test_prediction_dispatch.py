from __future__ import annotations

import inspect
from datetime import datetime, timezone
from uuid import uuid4

import pytest
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.prediction.dispatch import (
    InProcessPredictionDispatcher,
    PredictionDispatchError,
    PredictionDispatcher,
    build_prediction_work_item,
)
from app.prediction.models import PredictionRun, PredictionRunStatus
from app.prediction.repository import create_prediction_run
from app.prediction.service import PredictionRunRequestResult, dispatch_prediction_run
from app.prediction.worker_schemas import PredictionWorkItem
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


def _run(session, tenant: Tenant, vehicle: Vehicle) -> PredictionRun:
    run = create_prediction_run(
        session,
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        requested_at=datetime(2026, 8, 15, 12, 0, tzinfo=timezone.utc),
        correlation_id="run-correlation",
    )
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


class CapturingWorker:
    def __init__(self) -> None:
        self.calls: list[PredictionWorkItem] = []

    def execute(self, work_item: PredictionWorkItem) -> None:
        self.calls.append(work_item)


class FailingWorker:
    def __init__(self, error: Exception) -> None:
        self.error = error

    def execute(self, work_item: PredictionWorkItem) -> None:
        raise self.error


class CapturingDispatcher:
    def __init__(self) -> None:
        self.calls: list[PredictionWorkItem] = []

    def dispatch(self, work_item: PredictionWorkItem) -> None:
        self.calls.append(work_item)


class FailingDispatcher:
    def dispatch(self, work_item: PredictionWorkItem) -> None:
        raise PredictionDispatchError("dispatch boundary failed")


def test_protocol_and_in_process_dispatcher_injection() -> None:
    worker = CapturingWorker()
    dispatcher = InProcessPredictionDispatcher(worker=worker)
    item = PredictionWorkItem(predictionRunId=uuid4(), tenantId=uuid4(), vehicleId=uuid4())
    assert isinstance(dispatcher, PredictionDispatcher)
    assert dispatcher.worker is worker
    dispatcher.dispatch(item)
    assert worker.calls == [item]


def test_work_item_mapping_excludes_non_transport_run_fields() -> None:
    run = PredictionRun(
        id=uuid4(),
        tenant_id=uuid4(),
        vehicle_id=uuid4(),
        service_event_id=uuid4(),
        analysis_run_id=uuid4(),
        correlation_id="correlation",
        idempotency_key="not-in-work-item",
        request_fingerprint="not-in-work-item",
        status=PredictionRunStatus.QUEUED,
        requested_at=datetime.now(timezone.utc),
    )
    item = build_prediction_work_item(run=run)
    assert item.predictionRunId == run.id
    assert item.tenantId == run.tenant_id
    assert item.vehicleId == run.vehicle_id
    assert item.serviceEventId == run.service_event_id
    assert item.analysisRunId == run.analysis_run_id
    assert item.correlationId == run.correlation_id
    assert set(item.model_dump()) == {
        "predictionRunId",
        "tenantId",
        "vehicleId",
        "serviceEventId",
        "analysisRunId",
        "correlationId",
    }


def test_dispatch_calls_worker_once_and_normalizes_unexpected_errors() -> None:
    item = PredictionWorkItem(predictionRunId=uuid4(), tenantId=uuid4(), vehicleId=uuid4())
    worker = CapturingWorker()
    InProcessPredictionDispatcher(worker=worker).dispatch(item)
    assert worker.calls == [item]
    original = RuntimeError("raw worker error")
    with pytest.raises(PredictionDispatchError, match="Prediction dispatch failed") as raised:
        InProcessPredictionDispatcher(worker=FailingWorker(original)).dispatch(item)
    assert raised.value.__cause__ is original
    expected = PredictionDispatchError("expected")
    with pytest.raises(PredictionDispatchError) as propagated:
        InProcessPredictionDispatcher(worker=FailingWorker(expected)).dispatch(item)
    assert propagated.value is expected


def test_created_result_dispatches_and_replay_result_does_not(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    dispatcher = CapturingDispatcher()
    dispatch_prediction_run(
        result=PredictionRunRequestResult(run=run, created=True),
        dispatcher=dispatcher,
    )
    dispatch_prediction_run(
        result=PredictionRunRequestResult(run=run, created=False),
        dispatcher=dispatcher,
    )
    assert len(dispatcher.calls) == 1
    assert dispatcher.calls[0].predictionRunId == run.id


def test_dispatch_helper_does_not_open_session_or_mark_failure(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    with pytest.raises(PredictionDispatchError):
        dispatch_prediction_run(
            result=PredictionRunRequestResult(run=run, created=True),
            dispatcher=FailingDispatcher(),
        )
    verification = SessionLocal()
    try:
        persisted = verification.get(PredictionRun, run.id)
        assert persisted.status == PredictionRunStatus.QUEUED
    finally:
        verification.close()


def test_real_in_process_dispatcher_completes_committed_queued_run(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    InProcessPredictionDispatcher().dispatch(build_prediction_work_item(run=run))
    verification = SessionLocal()
    try:
        persisted = verification.get(PredictionRun, run.id)
        assert persisted.status == PredictionRunStatus.COMPLETED
    finally:
        verification.close()


def test_dispatch_module_is_synchronous_and_has_no_async_framework_dependency() -> None:
    import app.prediction.dispatch as dispatch_module

    source = inspect.getsource(dispatch_module)
    assert "ThreadPoolExecutor" not in source
    assert "asyncio" not in source
    assert "BackgroundTasks" not in source
    assert "FastAPI" not in source
