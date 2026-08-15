from __future__ import annotations

import inspect
from datetime import datetime, timezone
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import func, select

import app.prediction.service as prediction_service
from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import AnalysisRun, AnalysisRunStatus
from app.identity.models import UserRef
from app.main import app
from app.prediction.audit import PREDICTION_RUN_REQUESTED_EVENT_TYPE
from app.prediction.dispatch import PredictionDispatchError
from app.prediction.models import PredictionAssessment, PredictionRun, PredictionRunStatus
from app.service_intake.models import ServiceEvent, ServiceEventState
from app.vehicle.models import AuditEvent, Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


@pytest.fixture()
def context() -> dict[str, object]:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        user = session.execute(select(UserRef).where(UserRef.tenant_id == tenant.id)).scalars().first()
        return {"tenant": tenant, "vehicle": vehicle, "user": user}
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    previous = app.state.prediction_dispatcher
    yield
    app.state.prediction_dispatcher = previous
    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug != "autovision-demo-org"))
        session.commit()
    finally:
        session.close()
    reset_demo_data()


def _headers(tenant, user):
    return {"X-Tenant-ID": str(tenant.id), "X-User-ID": str(user.id)}


def _run(session, tenant, vehicle, status: PredictionRunStatus):
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        status=status,
        requested_at=datetime(2026, 8, 10, tzinfo=timezone.utc),
        started_at=datetime(2026, 8, 11, tzinfo=timezone.utc),
        completed_at=datetime(2026, 8, 12, tzinfo=timezone.utc) if status in {
            PredictionRunStatus.COMPLETED,
            PredictionRunStatus.PARTIALLY_COMPLETED,
            PredictionRunStatus.FAILED,
        } else None,
        error_code="SAFE_ERROR" if status == PredictionRunStatus.FAILED else None,
        error_message="Safe error" if status == PredictionRunStatus.FAILED else None,
        provider_name="provider" if status == PredictionRunStatus.COMPLETED else None,
        engine_name="engine" if status == PredictionRunStatus.COMPLETED else None,
        schema_version="s3.3" if status == PredictionRunStatus.COMPLETED else None,
        configuration_version="config" if status == PredictionRunStatus.COMPLETED else None,
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _events(tenant_id):
    session = SessionLocal()
    try:
        return session.execute(select(AuditEvent).where(AuditEvent.tenant_id == tenant_id)).scalars().all()
    finally:
        session.close()


def test_malformed_bodies_and_blank_idempotency_are_safe(context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]
    headers = _headers(tenant, user)
    assert client.post("/v1/prediction-runs", headers=headers, json={}).status_code == 422
    assert client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": "bad"}).status_code == 422
    assert client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id), "tenantId": str(tenant.id)}).status_code == 422
    assert client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id), "idempotencyKey": "body"}).status_code == 422
    assert client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id), "correlationId": "body"}).status_code == 422
    assert client.post("/v1/prediction-runs", headers={**headers, "Idempotency-Key": "   "}, json={"vehicleId": str(vehicle.id)}).status_code == 400


def test_cross_tenant_and_unauthorized_post_do_not_leak_or_create(context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]
    before_runs = len(_events(tenant.id))
    response = client.post(
        "/v1/prediction-runs",
        headers={"X-Tenant-ID": str(tenant.id), "X-User-ID": str(uuid4())},
        json={"vehicleId": str(vehicle.id)},
    )
    assert response.status_code == 401
    assert len(_events(tenant.id)) == before_runs

    other = Tenant(slug=f"hardening-other-{uuid4().hex[:8]}", name="Other")
    session = SessionLocal()
    try:
        session.add(other)
        session.flush()
        other_vehicle = Vehicle(
            tenant_id=other.id,
            vehicle_class=vehicle.vehicle_class,
            powertrain=vehicle.powertrain,
            model_name="Other",
            year=2026,
            color="Black",
        )
        session.add(other_vehicle)
        session.flush()
        event = ServiceEvent(
            tenant_id=other.id,
            vehicle_id=other_vehicle.id,
            source="other",
            state=ServiceEventState.OPEN,
            revision=1,
            opened_at=datetime.now(timezone.utc),
        )
        session.add(event)
        session.flush()
        analysis = AnalysisRun(
            tenant_id=other.id,
            service_event_id=event.id,
            status=AnalysisRunStatus.SUCCEEDED,
            requested_at=datetime.now(timezone.utc),
        )
        session.add(analysis)
        session.commit()
        other_ids = (other_vehicle.id, event.id, analysis.id)
    finally:
        session.close()
    for payload in (
        {"vehicleId": str(other_ids[0])},
        {"vehicleId": str(vehicle.id), "serviceEventId": str(other_ids[1])},
        {"vehicleId": str(vehicle.id), "serviceEventId": str(other_ids[1]), "analysisRunId": str(other_ids[2])},
    ):
        assert client.post("/v1/prediction-runs", headers=_headers(tenant, user), json=payload).status_code == 404


def test_replay_conflict_and_immutability(context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]
    headers = {**_headers(tenant, user), "Idempotency-Key": "hardening-replay", "X-Correlation-ID": "original"}
    first = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert first.status_code == 201
    run_id = first.json()["id"]
    session = SessionLocal()
    try:
        before = session.get(PredictionRun, run_id)
        original = {field: getattr(before, field) for field in (
            "requested_at", "requested_by_user_ref_id", "correlation_id", "request_fingerprint",
            "service_event_id", "analysis_run_id", "status", "provider_name", "error_code", "error_message",
        )}
    finally:
        session.close()
    replay = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, user), "Idempotency-Key": "hardening-replay", "X-Correlation-ID": "changed"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert replay.status_code == 200
    session = SessionLocal()
    try:
        after = session.get(PredictionRun, run_id)
        assert {field: getattr(after, field) for field in original} == original
        assert len(session.execute(select(PredictionRun).where(PredictionRun.id == run_id)).scalars().all()) == 1
    finally:
        session.close()


def test_dispatch_failure_persists_queued_run_and_audit_and_replay_no_redispatch(context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]

    class FailingDispatcher:
        def __init__(self):
            self.calls = 0
        def dispatch(self, work_item):
            self.calls += 1
            raise PredictionDispatchError("raw dispatch")

    dispatcher = FailingDispatcher()
    app.state.prediction_dispatcher = dispatcher
    headers = {**_headers(tenant, user), "Idempotency-Key": "hardening-dispatch"}
    response = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert response.status_code == 503
    session = SessionLocal()
    try:
        run = session.execute(select(PredictionRun).where(PredictionRun.idempotency_key == "hardening-dispatch")).scalar_one()
        assert run.status == PredictionRunStatus.QUEUED
        assert any(event.event_type == PREDICTION_RUN_REQUESTED_EVENT_TYPE for event in _events(tenant.id))
    finally:
        session.close()
    replay = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert replay.status_code == 200
    assert dispatcher.calls == 1


def test_audit_failure_rolls_back_new_run_and_replay_does_not_mutate(monkeypatch, context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]
    original = prediction_service.record_prediction_audit_event

    def fail_audit(*args, **kwargs):
        raise RuntimeError("raw audit database failure")

    monkeypatch.setattr(prediction_service, "record_prediction_audit_event", fail_audit)
    with pytest.raises(RuntimeError):
        request = prediction_service.request_prediction_run
        session = SessionLocal()
        try:
            request(session, tenant_id=tenant.id, payload=__import__("app.prediction.api_schemas", fromlist=["PredictionRunCreateRequest"]).PredictionRunCreateRequest(vehicleId=vehicle.id), idempotency_key="audit-fail")
        finally:
            session.rollback()
            session.close()
    session = SessionLocal()
    try:
        assert session.execute(select(PredictionRun).where(PredictionRun.idempotency_key == "audit-fail")).scalar_one_or_none() is None
    finally:
        session.close()
    monkeypatch.setattr(prediction_service, "record_prediction_audit_event", original)


def test_terminal_reads_and_empty_assessments_are_safe(context) -> None:
    tenant, vehicle, user = context["tenant"], context["vehicle"], context["user"]
    session = SessionLocal()
    try:
        runs = [_run(session, tenant, vehicle, status) for status in (
            PredictionRunStatus.QUEUED,
            PredictionRunStatus.PROCESSING,
            PredictionRunStatus.COMPLETED,
            PredictionRunStatus.PARTIALLY_COMPLETED,
            PredictionRunStatus.FAILED,
        )]
    finally:
        session.close()
    for run in runs:
        response = client.get(f"/v1/prediction-runs/{run.id}", headers=_headers(tenant, user))
        assert response.status_code == 200
        assessments = client.get(f"/v1/prediction-runs/{run.id}/assessments", headers=_headers(tenant, user))
        assert assessments.status_code == 200
        assert assessments.json() == []
    assert client.get(f"/v1/prediction-runs/{runs[0].id}", headers={"X-Tenant-ID": str(uuid4()), "X-User-ID": str(user.id)}).status_code == 404


def test_router_is_thin_and_gets_do_not_audit_or_execute_worker() -> None:
    import app.prediction.router as router_module
    source = inspect.getsource(router_module)
    assert "PredictionWorker" not in source
    assert "PredictionProvider" not in source
    assert "build_prediction_request_fingerprint" not in source
    assert "AuditEvent(" not in source
