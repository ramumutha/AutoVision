from __future__ import annotations

from uuid import uuid4

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.identity.models import RoleAssignment, UserRef
from app.main import app
from app.prediction.models import PredictionRun
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


@pytest.fixture()
def security_context() -> dict[str, object]:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(select(Vehicle).where(Vehicle.tenant_id == tenant.id)).scalars().first()
        users = session.execute(select(UserRef).where(UserRef.tenant_id == tenant.id)).scalars().all()
        roles = {}
        for user in users:
            roles[user.external_user_id] = user
        return {"tenant": tenant, "vehicle": vehicle, "users": roles}
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    yield
    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug != "autovision-demo-org"))
        session.commit()
    finally:
        session.close()
    reset_demo_data()


def _headers(tenant, user):
    user_id = user.id if hasattr(user, "id") else user
    return {"X-Tenant-ID": str(tenant.id), "X-User-ID": str(user_id)}


def test_request_roles_and_requested_by_provenance(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    users = security_context["users"]
    advisor = users["svc-advisor-01"]
    response = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, advisor), "Idempotency-Key": "security-key"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert response.status_code == 201
    session = SessionLocal()
    try:
        run = session.execute(select(PredictionRun).where(PredictionRun.id == response.json()["id"])).scalar_one()
        assert run.requested_by_user_ref_id == advisor.id
    finally:
        session.close()


def test_technician_can_read_but_cannot_request(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    technician = security_context["users"]["tech-01"]
    post = client.post(
        "/v1/prediction-runs",
        headers=_headers(tenant, technician),
        json={"vehicleId": str(vehicle.id)},
    )
    assert post.status_code == 403

    advisor = security_context["users"]["svc-advisor-01"]
    created = client.post(
        "/v1/prediction-runs",
        headers=_headers(tenant, advisor),
        json={"vehicleId": str(vehicle.id)},
    )
    assert created.status_code == 201
    assert client.get(
        f"/v1/prediction-runs/{created.json()['id']}", headers=_headers(tenant, technician)
    ).status_code == 200


def test_missing_or_unassigned_user_is_rejected(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    assert client.post(
        "/v1/prediction-runs",
        headers={"X-Tenant-ID": str(tenant.id)},
        json={"vehicleId": str(vehicle.id)},
    ).status_code == 401
    unassigned = UserRef(tenant_id=tenant.id, external_user_id=f"unassigned-{uuid4()}", is_active=True)
    session = SessionLocal()
    try:
        session.add(unassigned)
        session.commit()
        user_id = unassigned.id
    finally:
        session.close()
    assert client.post(
        "/v1/prediction-runs",
        headers={"X-Tenant-ID": str(tenant.id), "X-User-ID": str(user_id)},
        json={"vehicleId": str(vehicle.id)},
    ).status_code == 403


def test_cross_tenant_user_and_role_do_not_grant_target_access(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    session = SessionLocal()
    try:
        other_tenant = Tenant(slug=f"security-other-{uuid4().hex[:8]}", name="Other")
        session.add(other_tenant)
        session.flush()
        other_user = UserRef(tenant_id=other_tenant.id, external_user_id="other-user", is_active=True)
        session.add(other_user)
        session.flush()
        session.add(RoleAssignment(tenant_id=other_tenant.id, user_ref_id=other_user.id, role_name="ADMIN"))
        session.commit()
        user_id = other_user.id
    finally:
        session.close()
    response = client.post(
        "/v1/prediction-runs",
        headers={"X-Tenant-ID": str(tenant.id), "X-User-ID": str(user_id)},
        json={"vehicleId": str(vehicle.id)},
    )
    assert response.status_code == 401


def test_unauthorized_read_and_wrong_tenant_resource_do_not_leak(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    advisor = security_context["users"]["svc-advisor-01"]
    technician = security_context["users"]["tech-01"]
    created = client.post(
        "/v1/prediction-runs",
        headers=_headers(tenant, advisor),
        json={"vehicleId": str(vehicle.id)},
    )
    run_id = created.json()["id"]
    session = SessionLocal()
    try:
        viewer = security_context["users"]["admin-viewer-01"]
        session.execute(
            select(RoleAssignment).where(RoleAssignment.user_ref_id == viewer.id)
        ).scalars().all()
    finally:
        session.close()
    assert client.get(f"/v1/prediction-runs/{run_id}", headers=_headers(tenant, uuid4())).status_code == 401
    assert client.get(f"/v1/prediction-runs/{run_id}/assessments", headers=_headers(tenant, technician)).status_code == 200
    assert client.get(f"/v1/prediction-runs/{run_id}", headers={"X-Tenant-ID": str(uuid4()), "X-User-ID": str(advisor.id)}).status_code == 404
    assert client.get(f"/v1/prediction-runs/{run_id}/assessments", headers={"X-Tenant-ID": str(uuid4()), "X-User-ID": str(advisor.id)}).status_code == 404


def test_idempotent_replay_preserves_original_user(security_context) -> None:
    tenant = security_context["tenant"]
    vehicle = security_context["vehicle"]
    advisor = security_context["users"]["svc-advisor-01"]
    admin = security_context["users"]["admin-viewer-01"]
    headers = {**_headers(tenant, advisor), "Idempotency-Key": "user-preservation"}
    first = client.post("/v1/prediction-runs", headers=headers, json={"vehicleId": str(vehicle.id)})
    replay = client.post(
        "/v1/prediction-runs",
        headers={**_headers(tenant, admin), "Idempotency-Key": "user-preservation"},
        json={"vehicleId": str(vehicle.id)},
    )
    assert first.status_code == 201
    assert replay.status_code == 200
    session = SessionLocal()
    try:
        run = session.get(PredictionRun, first.json()["id"])
        assert run.requested_by_user_ref_id == advisor.id
    finally:
        session.close()
