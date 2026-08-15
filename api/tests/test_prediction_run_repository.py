from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.prediction.api_schemas import PredictionRunRead
from app.prediction.models import PredictionAssessment, PredictionFactor, PredictionRun, PredictionRunStatus
from app.prediction.repository import (
    add_prediction_assessment,
    add_prediction_factor,
    create_prediction_run,
    get_prediction_run_by_idempotency_key,
    get_prediction_run_for_api,
)
from app.prediction.provider_schemas import PredictionAssessmentCandidate, PredictionFactorCandidate
from app.prediction.models import PredictionFactorType, PredictionHorizonType, PredictionSeverity, PredictionUrgency
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


def _candidate() -> PredictionAssessmentCandidate:
    return PredictionAssessmentCandidate(
        prediction_type="POWERTRAIN_CONTEXT",
        prediction_code="ICE_POWERTRAIN_CONTEXT",
        title="Powertrain context",
        description="Powertrain context is available.",
        severity=PredictionSeverity.INFO,
        urgency=PredictionUrgency.MONITOR,
        horizon_type=PredictionHorizonType.UNSPECIFIED,
        factors=(
            PredictionFactorCandidate(
                factor_type=PredictionFactorType.VEHICLE,
                factor_code="POWERTRAIN_TYPE",
                label="Powertrain type",
                value_text="ICE",
                source_entity_type="VEHICLE",
                source_entity_id=uuid4(),
                value_numeric=Decimal("12.50"),
                weight=Decimal("0.25"),
                metadata={"origin": "test"},
            ),
        ),
        metadata={"rule_code": "POWERTRAIN_CONTEXT", "rule_version": "1"},
    )


def test_create_prediction_run_maps_scope_and_starts_queued(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    requested_at = datetime(2026, 8, 15, 12, 0, tzinfo=timezone.utc)
    session = SessionLocal()
    try:
        run = create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            service_event_id=None,
            analysis_run_id=None,
            requested_by_user_ref_id=None,
            requested_at=requested_at,
            idempotency_key="idem-1",
            request_fingerprint="fingerprint-1",
            correlation_id="corr-1",
        )
        assert run.status == PredictionRunStatus.QUEUED
        assert run.requested_at == requested_at
        assert run.tenant_id == tenant.id
        assert run.vehicle_id == vehicle.id
        assert run.idempotency_key == "idem-1"
        assert run.request_fingerprint == "fingerprint-1"
        assert run.correlation_id == "corr-1"
        assert session.in_transaction()
        session.commit()
    finally:
        session.close()


def test_idempotency_lookup_is_tenant_scoped(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            requested_at=datetime.now(timezone.utc),
            idempotency_key="same-key",
        )
        session.commit()
        assert get_prediction_run_by_idempotency_key(
            session, tenant_id=tenant.id, idempotency_key="same-key"
        ).id == run.id
        assert get_prediction_run_by_idempotency_key(
            session, tenant_id=uuid4(), idempotency_key="same-key"
        ) is None
    finally:
        session.close()


def test_api_lookup_is_tenant_safe_and_loads_nested_assessment_factor(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            requested_at=datetime.now(timezone.utc),
        )
        session.commit()
        candidate = _candidate()
        assessment = add_prediction_assessment(
            session,
            tenant_id=tenant.id,
            prediction_run_id=run.id,
            candidate=candidate,
        )
        factor = candidate.factors[0]
        factor = factor.model_copy(update={"source_entity_id": vehicle.id})
        add_prediction_factor(
            session,
            tenant_id=tenant.id,
            prediction_assessment_id=assessment.id,
            candidate=factor,
        )
        session.commit()
        loaded = get_prediction_run_for_api(
            session, tenant_id=tenant.id, prediction_run_id=run.id
        )
        assert loaded is not None
        assert len(loaded.assessments) == 1
        assert len(loaded.assessments[0].factors) == 1
        assert get_prediction_run_for_api(
            session, tenant_id=uuid4(), prediction_run_id=run.id
        ) is None
        api_payload = PredictionRunRead.model_validate({
            "id": loaded.id,
            "tenantId": loaded.tenant_id,
            "vehicleId": loaded.vehicle_id,
            "status": loaded.status,
            "requestedAt": loaded.requested_at,
            "createdAt": loaded.created_at,
            "updatedAt": loaded.updated_at,
            "assessments": [],
        })
        assert api_payload.id == run.id
    finally:
        session.close()


def test_repository_does_not_commit_implicitly(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            requested_at=datetime.now(timezone.utc),
        )
        assert session.in_transaction()
        session.rollback()
    finally:
        session.close()
    verification = SessionLocal()
    try:
        assert verification.execute(
            select(PredictionRun).where(PredictionRun.tenant_id == tenant.id)
        ).scalars().all() == []
    finally:
        verification.close()
