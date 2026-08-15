from __future__ import annotations

from datetime import datetime, timezone
from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.main import app
from app.identity.models import UserRef
from app.prediction.dispatch import PredictionDispatchError
from app.prediction.models import PredictionAssessment, PredictionFactor, PredictionRun, PredictionRunStatus
from app.prediction.repository import add_prediction_assessment, add_prediction_factor, create_prediction_run
from app.prediction.provider_schemas import PredictionAssessmentCandidate, PredictionFactorCandidate
from app.prediction.models import PredictionFactorType, PredictionHorizonType, PredictionSeverity, PredictionUrgency
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


@pytest.fixture()
def api_context() -> dict[str, object]:
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
    reset_demo_data()


def _headers(tenant_id) -> dict[str, str]:
    headers = {"X-Tenant-ID": str(tenant_id)}
    session = SessionLocal()
    try:
        user = session.execute(select(UserRef).where(UserRef.tenant_id == tenant_id)).scalars().first()
        if user is not None:
            headers["X-User-ID"] = str(user.id)
    finally:
        session.close()
    return headers


def test_post_creates_commits_dispatches_and_returns_sync_state(api_context) -> None:
    tenant = api_context["tenant"]
    vehicle = api_context["vehicle"]
    response = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant.id), "Idempotency-Key": "api-key", "X-Correlation-ID": " api-correlation "},
        json={"vehicleId": str(vehicle.id)},
    )
    assert response.status_code == 201, response.text
    body = response.json()
    assert body["vehicleId"] == str(vehicle.id)
    assert body["tenantId"] == str(tenant.id)
    assert body["status"] in {"COMPLETED", "PARTIALLY_COMPLETED", "FAILED"}
    assert body["correlationId"] == "api-correlation"
    assert "idempotencyKey" in body


def test_post_replay_returns_200_same_run_without_redispatch(api_context) -> None:
    tenant = api_context["tenant"]
    vehicle = api_context["vehicle"]
    headers = {**_headers(tenant.id), "Idempotency-Key": "replay-key"}
    first = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    second = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert first.status_code == 201
    assert second.status_code == 200
    assert second.json()["id"] == first.json()["id"]
    session = SessionLocal()
    try:
        assert len(session.execute(select(PredictionRun).where(PredictionRun.tenant_id == tenant.id)).scalars().all()) == 1
    finally:
        session.close()


def test_post_validation_and_tenant_errors(api_context) -> None:
    tenant = api_context["tenant"]
    vehicle = api_context["vehicle"]
    response = client.post("/v1/prediction-runs", headers=_headers(tenant.id), json={})
    assert response.status_code == 422
    assert client.post(
        "/v1/prediction-runs", headers=_headers(tenant.id), json={"vehicleId": str(uuid4())}
    ).status_code == 404
    assert client.post(
        "/v1/prediction-runs", headers=_headers(uuid4()), json={"vehicleId": str(vehicle.id)}
    ).status_code == 401
    assert client.post(
        "/v1/prediction-runs",
        headers=_headers(tenant.id),
        json={"vehicleId": str(vehicle.id), "analysisRunId": str(uuid4())},
    ).status_code == 400
    assert client.post(
        "/v1/prediction-runs",
        headers=_headers(tenant.id),
        json={"vehicleId": str(vehicle.id), "tenantId": str(tenant.id)},
    ).status_code == 422


def test_idempotency_conflict_and_dispatch_failure_semantics(api_context) -> None:
    tenant = api_context["tenant"]
    vehicle = api_context["vehicle"]
    headers = {**_headers(tenant.id), "Idempotency-Key": "conflict-key"}
    first = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    assert first.status_code == 201
    other_vehicle = None
    session = SessionLocal()
    try:
        other_vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
    finally:
        session.close()
    conflict = client.post(
        "/v1/prediction-runs", headers=headers, json={"vehicleId": str(other_vehicle.id)}
    )
    assert conflict.status_code == 409

    class FailingDispatcher:
        def dispatch(self, work_item):
            raise PredictionDispatchError("internal dispatch detail")

    app.state.prediction_dispatcher = FailingDispatcher()
    failed_dispatch = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant.id), "Idempotency-Key": "dispatch-failure"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert failed_dispatch.status_code == 503
    session = SessionLocal()
    try:
        run = session.execute(
            select(PredictionRun).where(PredictionRun.idempotency_key == "dispatch-failure")
        ).scalar_one()
        assert run.status == PredictionRunStatus.QUEUED
    finally:
        session.close()


def test_get_run_and_assessments_are_tenant_safe_and_nested(api_context) -> None:
    tenant = api_context["tenant"]
    vehicle = api_context["vehicle"]
    session = SessionLocal()
    try:
        run = create_prediction_run(
            session,
            tenant_id=tenant.id,
            vehicle_id=vehicle.id,
            requested_at=datetime.now(timezone.utc),
        )
        session.flush()
        candidate = PredictionAssessmentCandidate(
            prediction_type="TEST",
            prediction_code="TEST_CODE",
            title="Test",
            description="Test description",
            severity=PredictionSeverity.INFO,
            urgency=PredictionUrgency.MONITOR,
            horizon_type=PredictionHorizonType.UNSPECIFIED,
            factors=(PredictionFactorCandidate(
                factor_type=PredictionFactorType.VEHICLE,
                factor_code="VEHICLE",
                label="Vehicle",
                source_entity_type="VEHICLE",
                source_entity_id=vehicle.id,
            ),),
            metadata={"rule_code": "TEST", "rule_version": "1"},
        )
        assessment = add_prediction_assessment(session, tenant_id=tenant.id, prediction_run_id=run.id, candidate=candidate)
        add_prediction_factor(session, tenant_id=tenant.id, prediction_assessment_id=assessment.id, candidate=candidate.factors[0])
        session.commit()
        run_id = run.id
    finally:
        session.close()
    get_response = client.get(f"/v1/prediction-runs/{run_id}", headers=_headers(tenant.id))
    assert get_response.status_code == 200
    assert get_response.json()["assessments"][0]["factors"][0]["factorCode"] == "VEHICLE"
    assessments = client.get(f"/v1/prediction-runs/{run_id}/assessments", headers=_headers(tenant.id))
    assert assessments.status_code == 200
    assert assessments.json()[0]["factors"][0]["factorCode"] == "VEHICLE"
    assert client.get(f"/v1/prediction-runs/{run_id}", headers=_headers(uuid4())).status_code == 404
    assert client.get(f"/v1/prediction-runs/{uuid4()}", headers=_headers(tenant.id)).status_code == 404
    assert client.get(f"/v1/prediction-runs/{uuid4()}/assessments", headers=_headers(tenant.id)).status_code == 404
