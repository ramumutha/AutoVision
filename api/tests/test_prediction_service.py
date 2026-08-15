from __future__ import annotations

import hashlib
import inspect
import json
from datetime import datetime, timezone
from uuid import uuid4

import pytest
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.identity.models import UserRef
from app.prediction.api_schemas import PredictionRunCreateRequest
from app.prediction.models import PredictionRun, PredictionRunStatus
from app.prediction.service import (
    build_prediction_request_fingerprint,
    request_prediction_run,
)
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
        user = session.execute(select(UserRef).where(UserRef.tenant_id == tenant.id)).scalars().first()
        return {"tenant": tenant, "vehicle": vehicle, "user": user}
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    yield
    reset_demo_data()


def _scope(session, tenant: Tenant, vehicle: Vehicle):
    event = ServiceEvent(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        source="prediction-service-test",
        state=ServiceEventState.OPEN,
        revision=1,
        opened_at=datetime.now(timezone.utc),
    )
    session.add(event)
    session.flush()
    from app.evidence.models import AnalysisRun, AnalysisRunStatus

    analysis_run = AnalysisRun(
        tenant_id=tenant.id,
        service_event_id=event.id,
        status=AnalysisRunStatus.SUCCEEDED,
        requested_at=datetime.now(timezone.utc),
    )
    session.add(analysis_run)
    session.flush()
    return event, analysis_run


def test_fingerprint_is_deterministic_and_semantic_only() -> None:
    vehicle_id = uuid4()
    event_id = uuid4()
    analysis_id = uuid4()
    first = build_prediction_request_fingerprint(
        vehicle_id=vehicle_id, service_event_id=event_id, analysis_run_id=analysis_id
    )
    second = build_prediction_request_fingerprint(
        vehicle_id=vehicle_id, service_event_id=event_id, analysis_run_id=analysis_id
    )
    expected_source = {
        "vehicleId": str(vehicle_id),
        "serviceEventId": str(event_id),
        "analysisRunId": str(analysis_id),
    }
    expected = hashlib.sha256(
        json.dumps(expected_source, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()
    assert first == second == expected
    assert first != build_prediction_request_fingerprint(
        vehicle_id=uuid4(), service_event_id=event_id, analysis_run_id=analysis_id
    )
    assert first != build_prediction_request_fingerprint(
        vehicle_id=vehicle_id, service_event_id=uuid4(), analysis_run_id=analysis_id
    )
    assert first != build_prediction_request_fingerprint(
        vehicle_id=vehicle_id, service_event_id=event_id, analysis_run_id=uuid4()
    )


def test_creates_queued_run_with_scopes_user_and_normalized_transport_values(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    user = prediction_context["user"]
    user = prediction_context["user"]
    session = SessionLocal()
    try:
        event, analysis_run = _scope(session, tenant, vehicle)
        requested_at = datetime(2026, 8, 15, 12, 30, tzinfo=timezone.utc)
        result = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(
                vehicleId=vehicle.id,
                serviceEventId=event.id,
                analysisRunId=analysis_run.id,
            ),
            requested_by_user_ref_id=user.id,
            idempotency_key="  key-1  ",
            correlation_id="  correlation-1  ",
            requested_at=requested_at,
        )
        assert result.created is True
        assert result.run.status == PredictionRunStatus.QUEUED
        assert result.run.tenant_id == tenant.id
        assert result.run.vehicle_id == vehicle.id
        assert result.run.service_event_id == event.id
        assert result.run.analysis_run_id == analysis_run.id
        assert result.run.requested_by_user_ref_id == user.id
        assert result.run.requested_at == requested_at
        assert result.run.idempotency_key == "key-1"
        assert result.run.correlation_id == "correlation-1"
        assert result.run.request_fingerprint is not None
        assert result.run.provider_name is None
        assert result.run.input_snapshot is None
        assert session.in_transaction()
        session.commit()
    finally:
        session.close()


def test_generated_requested_at_is_timezone_aware_and_blank_correlation_becomes_none(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        result = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
            correlation_id="   ",
        )
        assert result.run.requested_at.tzinfo is not None
        assert result.run.correlation_id is None
        session.rollback()
    finally:
        session.close()


def test_blank_idempotency_key_is_rejected(prediction_context) -> None:
    session = SessionLocal()
    try:
        with pytest.raises(ValueError, match="Idempotency key must not be blank"):
            request_prediction_run(
                session,
                tenant_id=prediction_context["tenant"].id,
                payload=PredictionRunCreateRequest(vehicleId=prediction_context["vehicle"].id),
                idempotency_key="  ",
            )
    finally:
        session.close()


def test_idempotent_replay_returns_same_run_without_mutation(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    user = prediction_context["user"]
    session = SessionLocal()
    try:
        payload = PredictionRunCreateRequest(vehicleId=vehicle.id)
        first = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=payload,
            requested_by_user_ref_id=user.id,
            idempotency_key="same-key",
            correlation_id="original-correlation",
        )
        session.commit()
        before = session.get(PredictionRun, first.run.id)
        second = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=payload,
            idempotency_key="  same-key ",
            correlation_id="different-correlation",
            requested_by_user_ref_id=user.id,
        )
        assert second.created is False
        assert second.run.id == first.run.id
        assert second.run.correlation_id == before.correlation_id
        assert second.run.requested_at == before.requested_at
        assert session.execute(select(PredictionRun).where(PredictionRun.tenant_id == tenant.id)).scalars().all().__len__() == 1
        session.rollback()
    finally:
        session.close()


def test_same_key_different_request_is_rejected_and_other_tenant_isolated(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        other_same_tenant_vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
        request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
            idempotency_key="shared-key",
        )
        session.commit()
        with pytest.raises(ValueError, match="Idempotency key was already used"):
            request_prediction_run(
                session,
                tenant_id=tenant.id,
                payload=PredictionRunCreateRequest(vehicleId=other_same_tenant_vehicle.id),
                idempotency_key="shared-key",
            )
        other_tenant = Tenant(slug=f"other-{uuid4().hex[:8]}", name="Other Tenant")
        session.add(other_tenant)
        session.flush()
        other_vehicle = Vehicle(
            tenant_id=other_tenant.id,
            vehicle_class=vehicle.vehicle_class,
            powertrain=vehicle.powertrain,
            model_name="Other Vehicle",
            year=2026,
            color="Black",
        )
        session.add(other_vehicle)
        session.flush()
        other = request_prediction_run(
            session,
            tenant_id=other_tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=other_vehicle.id),
            idempotency_key="shared-key",
        )
        assert other.created is True
        session.rollback()
    finally:
        session.close()


def test_scope_and_user_validation_errors_are_tenant_safe(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        with pytest.raises(LookupError, match="Vehicle not found"):
            request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=uuid4()))
        with pytest.raises(LookupError, match="Vehicle not found"):
            request_prediction_run(session, tenant_id=uuid4(), payload=PredictionRunCreateRequest(vehicleId=vehicle.id))
        with pytest.raises(LookupError, match="Service event not found"):
            request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id, serviceEventId=uuid4()))
        with pytest.raises(ValueError, match="serviceEventId is required"):
            request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id, analysisRunId=uuid4()))
        event, _ = _scope(session, tenant, vehicle)
        with pytest.raises(LookupError, match="Analysis run not found"):
            request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id, serviceEventId=event.id, analysisRunId=uuid4()))
        with pytest.raises(LookupError, match="User not found"):
            request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id), requested_by_user_ref_id=uuid4())
    finally:
        session.rollback()
        session.close()


def test_no_idempotency_key_allows_multiple_runs_and_rollback_owns_transaction(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        first = request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id))
        second = request_prediction_run(session, tenant_id=tenant.id, payload=PredictionRunCreateRequest(vehicleId=vehicle.id))
        assert first.created and second.created and first.run.id != second.run.id
        assert session.in_transaction()
        session.rollback()
    finally:
        session.close()
    verification = SessionLocal()
    try:
        assert verification.execute(select(PredictionRun).where(PredictionRun.tenant_id == tenant.id)).scalars().all() == []
    finally:
        verification.close()


def test_service_has_no_dispatch_worker_or_http_dependency() -> None:
    import app.prediction.service as service_module

    source = inspect.getsource(service_module)
    assert "PredictionWorker" not in source
    assert "FastAPI" not in source
    assert "APIRouter" not in source
    assert "AuditEvent" not in source
