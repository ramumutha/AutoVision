from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError

import app.prediction.service as prediction_service
from app.core.database import SessionLocal
from app.core.models import Tenant
from app.prediction.api_schemas import PredictionRunCreateRequest
from app.prediction.models import PredictionRun
from app.prediction.repository import create_prediction_run
from app.prediction.service import build_prediction_request_fingerprint, request_prediction_run
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


def _create_winner(tenant: Tenant, vehicle: Vehicle, *, fingerprint: str, key: str):
    session = SessionLocal()
    try:
        run = create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            requested_at=datetime(2026, 8, 15, 12, 0, tzinfo=timezone.utc),
            idempotency_key=key,
            request_fingerprint=fingerprint,
            correlation_id="winner-correlation",
        )
        session.commit()
        session.refresh(run)
        session.expunge(run)
        return run
    finally:
        session.close()


def _force_collision_lookup(monkeypatch: pytest.MonkeyPatch, winner: PredictionRun) -> None:
    original_lookup = prediction_service.get_prediction_run_by_idempotency_key
    calls = {"count": 0}

    def lookup(session, *, tenant_id, idempotency_key):
        calls["count"] += 1
        if calls["count"] == 1:
            return None
        return original_lookup(session, tenant_id=tenant_id, idempotency_key=idempotency_key)

    monkeypatch.setattr(prediction_service, "get_prediction_run_by_idempotency_key", lookup)


def test_unique_collision_same_fingerprint_recovers_winner_without_duplicate(prediction_context, monkeypatch) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    key = "race-key"
    fingerprint = build_prediction_request_fingerprint(
        vehicle_id=vehicle.id, service_event_id=None, analysis_run_id=None
    )
    winner = _create_winner(tenant, vehicle, fingerprint=fingerprint, key=key)
    _force_collision_lookup(monkeypatch, winner)
    session = SessionLocal()
    try:
        result = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
            idempotency_key=key,
            correlation_id="loser-correlation",
            requested_at=datetime(2026, 8, 16, tzinfo=timezone.utc),
        )
        assert result.created is False
        assert result.run.id == winner.id
        assert session.in_transaction()
        session.commit()
    finally:
        session.close()

    verification = SessionLocal()
    try:
        assert verification.execute(
            select(PredictionRun).where(
                PredictionRun.tenant_id == tenant.id,
                PredictionRun.idempotency_key == key,
            )
        ).scalars().all() == [verification.get(PredictionRun, winner.id)]
        persisted = verification.get(PredictionRun, winner.id)
        assert persisted.correlation_id == "winner-correlation"
        assert persisted.requested_at == datetime(2026, 8, 15, 12, 0, tzinfo=timezone.utc)
    finally:
        verification.close()


def test_unique_collision_different_fingerprint_raises_exact_conflict(prediction_context, monkeypatch) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    key = "conflict-key"
    winner = _create_winner(tenant, vehicle, fingerprint="winner-fingerprint", key=key)
    _force_collision_lookup(monkeypatch, winner)
    session = SessionLocal()
    try:
        with pytest.raises(ValueError, match="^Idempotency key was already used for a different prediction request$"):
            request_prediction_run(
                session,
                tenant_id=tenant.id,
                payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
                idempotency_key=key,
            )
        session.commit()
    finally:
        session.close()


def test_unrelated_integrity_error_is_reraised_and_outer_transaction_survives(prediction_context, monkeypatch) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    original_create = prediction_service.create_prediction_run

    def unrelated_failure(*args, **kwargs):
        raise IntegrityError("insert prediction run", {}, RuntimeError("foreign key violation"))

    monkeypatch.setattr(prediction_service, "create_prediction_run", unrelated_failure)
    session = SessionLocal()
    try:
        outer_tenant = Tenant(slug=f"outer-{uuid4().hex[:8]}", name="Outer transaction tenant")
        outer_tenant_slug = outer_tenant.slug
        session.add(outer_tenant)
        session.flush()
        with pytest.raises(IntegrityError):
            request_prediction_run(
                session,
                tenant_id=tenant.id,
                payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
                idempotency_key="unrelated-error",
            )
        assert session.in_transaction()
        session.commit()
    finally:
        monkeypatch.setattr(prediction_service, "create_prediction_run", original_create)
        session.close()

    verification = SessionLocal()
    try:
        assert verification.execute(select(Tenant).where(Tenant.slug == outer_tenant_slug)).scalar_one_or_none() is not None
    finally:
        verification.close()


def test_no_idempotency_path_remains_simple_and_allows_repeated_runs(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        first = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
        )
        second = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
        )
        assert first.created is True
        assert second.created is True
        assert first.run.id != second.run.id
        session.rollback()
    finally:
        session.close()


def test_collision_recovery_does_not_commit_or_rollback_outer_transaction(prediction_context, monkeypatch) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    key = "transaction-key"
    fingerprint = build_prediction_request_fingerprint(
        vehicle_id=vehicle.id, service_event_id=None, analysis_run_id=None
    )
    winner = _create_winner(tenant, vehicle, fingerprint=fingerprint, key=key)
    _force_collision_lookup(monkeypatch, winner)
    session = SessionLocal()
    commit_called = False
    rollback_called = False
    original_commit = session.commit
    original_rollback = session.rollback

    def commit_spy(*args, **kwargs):
        nonlocal commit_called
        commit_called = True
        return original_commit(*args, **kwargs)

    def rollback_spy(*args, **kwargs):
        nonlocal rollback_called
        rollback_called = True
        return original_rollback(*args, **kwargs)

    session.commit = commit_spy
    session.rollback = rollback_spy
    try:
        result = request_prediction_run(
            session,
            tenant_id=tenant.id,
            payload=PredictionRunCreateRequest(vehicleId=vehicle.id),
            idempotency_key=key,
        )
        assert result.created is False
        assert commit_called is False
        assert rollback_called is False
    finally:
        session.close()
