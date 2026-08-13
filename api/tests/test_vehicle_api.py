import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.main import app
from scripts.seed_demo import reset_demo_data, seed_demo_data

client = TestClient(app)


@pytest.fixture()
def seeded_demo_tenant() -> str:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        yield str(tenant.id)
    finally:
        session.close()
        reset_demo_data()


def test_list_vehicles_returns_seeded_records(seeded_demo_tenant: str) -> None:
    response = client.get("/v1/vehicles", headers={"X-Tenant-ID": seeded_demo_tenant})

    assert response.status_code == 200
    payload = response.json()
    assert len(payload) >= 3
    assert {item["modelName"] for item in payload} == {"City Compact", "Eclipse", "Cargo Max"}


def test_list_vehicles_filters_by_identifier(seeded_demo_tenant: str) -> None:
    response = client.get(
        "/v1/vehicles",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        params={"identifier": "AV-DEMO-EV-001"},
    )

    assert response.status_code == 200
    payload = response.json()
    assert len(payload) == 1
    assert payload[0]["modelName"] == "Eclipse"
    assert payload[0]["identifiers"][0]["identifierValue"] == "AV-DEMO-EV-001"


def test_get_vehicle_by_id_returns_full_payload(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle_id = session.execute(
            select(
                __import__("app.vehicle.models", fromlist=["Vehicle"]).Vehicle.id
            ).where(
                __import__("app.vehicle.models", fromlist=["Vehicle"]).Vehicle.tenant_id == tenant.id,
                __import__("app.vehicle.models", fromlist=["Vehicle"]).Vehicle.model_name == "City Compact",
            )
        ).scalar_one()
    finally:
        session.close()

    response = client.get(f"/v1/vehicles/{vehicle_id}", headers={"X-Tenant-ID": seeded_demo_tenant})

    assert response.status_code == 200
    payload = response.json()
    assert payload["id"] == str(vehicle_id)
    assert payload["vehicleClass"] == "PASSENGER"
    assert payload["modelName"] == "City Compact"
    assert payload["powertrain"] == "ICE"
    assert payload["latestUsageSnapshot"]["odometerKm"] == 45500.0
    assert payload["identifiers"]


def test_unknown_or_unauthorized_vehicle_returns_404(seeded_demo_tenant: str) -> None:
    random_vehicle_id = str(uuid.uuid4())
    response = client.get(f"/v1/vehicles/{random_vehicle_id}", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert response.status_code == 404

    unique_slug = f"other-demo-tenant-{uuid.uuid4()}"
    new_tenant = Tenant(slug=unique_slug, name="Other Demo Tenant")
    session = SessionLocal()
    try:
        session.add(new_tenant)
        session.commit()
        other_tenant_id = str(new_tenant.id)
    finally:
        session.close()

    session = SessionLocal()
    try:
        vehicle = session.execute(
            select(__import__("app.vehicle.models", fromlist=["Vehicle"]).Vehicle).where(
                __import__("app.vehicle.models", fromlist=["Vehicle"]).Vehicle.model_name == "City Compact"
            )
        ).scalar_one()
        response = client.get(f"/v1/vehicles/{vehicle.id}", headers={"X-Tenant-ID": other_tenant_id})
    finally:
        session.close()

    assert response.status_code == 404

    session = SessionLocal()
    try:
        session.execute(
            __import__("app.core.models", fromlist=["Tenant"]).Tenant.__table__.delete().where(
                __import__("app.core.models", fromlist=["Tenant"]).Tenant.slug == unique_slug
            )
        )
        session.commit()
    finally:
        session.close()
