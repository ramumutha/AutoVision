from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.main import app
from app.prediction.audit import (
    PREDICTION_RUN_REPLAYED_ACTION,
    PREDICTION_RUN_REPLAYED_EVENT_TYPE,
    PREDICTION_RUN_REQUESTED_ACTION,
    PREDICTION_RUN_REQUESTED_EVENT_TYPE,
    record_prediction_audit_event,
)
from app.prediction.api_schemas import PredictionRunCreateRequest
from app.prediction.models import PredictionRun
from app.prediction.service import request_prediction_run
from app.vehicle.models import AuditEvent, Vehicle
from app.identity.models import UserRef
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


@pytest.fixture()
def audit_context() -> dict[str, object]:
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


def _events(tenant_id):
    session = SessionLocal()
    try:
        return session.execute(select(AuditEvent).where(AuditEvent.tenant_id == tenant_id).order_by(AuditEvent.created_at)).scalars().all()
    finally:
        session.close()


def test_new_request_audit_has_safe_request_provenance(audit_context) -> None:
    tenant, vehicle, user = audit_context["tenant"], audit_context["vehicle"], audit_context["user"]
    response = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, user), "Idempotency-Key": "audit-key", "X-Correlation-ID": "audit-correlation"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert response.status_code == 201
    run_id = response.json()["id"]
    events = _events(tenant.id)
    event = next(item for item in events if item.event_type == PREDICTION_RUN_REQUESTED_EVENT_TYPE)
    assert event.action == PREDICTION_RUN_REQUESTED_ACTION
    assert event.vehicle_id == vehicle.id
    assert event.user_ref_id == user.id
    assert event.entity_type == "prediction_run"
    assert event.entity_id == run_id
    assert event.event_metadata == {
        "predictionRunId": run_id,
        "serviceEventId": None,
        "analysisRunId": None,
        "correlationId": "audit-correlation",
        "idempotencyKey": "audit-key",
    }
    assert "input_snapshot" not in event.event_metadata
    assert "request_fingerprint" not in event.event_metadata
    assert "provider_response" not in event.event_metadata


def test_replay_audit_is_new_event_and_does_not_mutate_run(audit_context) -> None:
    tenant, vehicle, user = audit_context["tenant"], audit_context["vehicle"], audit_context["user"]
    headers = {**_headers(tenant, user), "Idempotency-Key": "replay-audit"}
    first = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    second = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert first.status_code == 201
    assert second.status_code == 200
    events = _events(tenant.id)
    assert len([event for event in events if event.event_type == PREDICTION_RUN_REQUESTED_EVENT_TYPE]) == 1
    replayed = [event for event in events if event.event_type == PREDICTION_RUN_REPLAYED_EVENT_TYPE]
    assert len(replayed) == 1
    assert replayed[0].action == PREDICTION_RUN_REPLAYED_ACTION
    assert replayed[0].entity_id == first.json()["id"]
    session = SessionLocal()
    try:
        run = session.get(PredictionRun, first.json()["id"])
        assert run.requested_by_user_ref_id == user.id
    finally:
        session.close()


def test_two_replays_are_allowed_and_other_tenant_isolated(audit_context) -> None:
    tenant, vehicle, user = audit_context["tenant"], audit_context["vehicle"], audit_context["user"]
    headers = {**_headers(tenant, user), "Idempotency-Key": "multi-replay"}
    client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    events = _events(tenant.id)
    assert len([event for event in events if event.event_type == PREDICTION_RUN_REPLAYED_EVENT_TYPE]) == 2
    session = SessionLocal()
    try:
        other = Tenant(slug=f"audit-other-{uuid4().hex[:8]}", name="Audit Other")
        session.add(other)
        session.flush()
        other_vehicle = Vehicle(
            tenant_id=other.id,
            vehicle_class=vehicle.vehicle_class,
            powertrain=vehicle.powertrain,
            model_name="Other",
            year=2026,
            color="Blue",
        )
        session.add(other_vehicle)
        other_vehicle_id = other_vehicle.id
        session.commit()
        other_tenant_id = other.id
    finally:
        session.close()
    session = SessionLocal()
    try:
        record_prediction_audit_event(
            session,
            tenant_id=other_tenant_id,
            vehicle_id=other_vehicle_id,
            prediction_run_id=uuid4(),
            event_type=PREDICTION_RUN_REQUESTED_EVENT_TYPE,
            action=PREDICTION_RUN_REQUESTED_ACTION,
            actor_user_ref_id=None,
        )
        session.commit()
    finally:
        session.close()
    assert len(_events(tenant.id)) == 6
    assert len(_events(other_tenant_id)) == 1


def test_audit_helper_is_caller_transaction_owned_and_rolls_back(audit_context) -> None:
    tenant, vehicle, user = audit_context["tenant"], audit_context["vehicle"], audit_context["user"]
    session = SessionLocal()
    before = len(_events(tenant.id))
    try:
        record_prediction_audit_event(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            prediction_run_id=uuid4(),
            event_type=PREDICTION_RUN_REQUESTED_EVENT_TYPE,
            action=PREDICTION_RUN_REQUESTED_ACTION,
            actor_user_ref_id=user.id,
        )
        assert session.in_transaction()
        session.rollback()
    finally:
        session.close()
    assert len(_events(tenant.id)) == before


def test_gets_do_not_create_audit_and_dispatch_failure_keeps_request_audit(audit_context) -> None:
    tenant, vehicle, user = audit_context["tenant"], audit_context["vehicle"], audit_context["user"]
    created = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, user), "Idempotency-Key": "dispatch-audit"},
        json={"vehicleId": str(vehicle.id)},
    )
    before = len(_events(tenant.id))
    client.get(f"/v1/prediction-runs/{created.json()['id']}", headers=_headers(tenant, user))
    client.get(f"/v1/prediction-runs/{created.json()['id']}/assessments", headers=_headers(tenant, user))
    assert len(_events(tenant.id)) == before

    class FailingDispatcher:
        def dispatch(self, work_item):
            from app.prediction.dispatch import PredictionDispatchError
            raise PredictionDispatchError("raw dispatch")

    app.state.prediction_dispatcher = FailingDispatcher()
    failed = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, user), "Idempotency-Key": "dispatch-audit-failure"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert failed.status_code == 503
    events = _events(tenant.id)
    assert any(event.event_metadata.get("idempotencyKey") == "dispatch-audit-failure" for event in events)
