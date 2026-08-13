import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.main import app
from app.service_intake.models import Complaint, ServiceEvent
from app.vehicle.models import Vehicle
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


def test_create_service_event_success(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        vehicle_id = str(vehicle.id)
    finally:
        session.close()

    original_complaint = "  Warning light after startup  "
    structured_summary = "Warning light during startup"

    response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": vehicle_id,
            "source": "service-desk",
            "originalComplaint": original_complaint,
            "structuredSummary": structured_summary,
            "language": "en-US",
            "capturedBy": "advisor-42",
        },
    )

    assert response.status_code == 201, response.text
    payload = response.json()

    assert payload["vehicleId"] == vehicle_id
    assert payload["source"] == "service-desk"
    assert payload["state"] == "DRAFT"
    assert payload["revision"] == 1
    assert payload["complaint"]["originalText"] == original_complaint
    assert payload["complaint"]["structuredSummary"] == structured_summary
    assert payload["complaint"]["originalText"] != payload["complaint"]["structuredSummary"]

    session = SessionLocal()
    try:
        db_event = session.execute(select(ServiceEvent).where(ServiceEvent.id == payload["id"])).scalar_one()
        db_complaint = session.execute(select(Complaint).where(Complaint.event_id == db_event.id)).scalar_one()
        assert db_event.state.value == "DRAFT"
        assert db_event.tenant_id == uuid.UUID(seeded_demo_tenant)
        assert db_event.vehicle_id == uuid.UUID(vehicle_id)
        assert db_complaint.original_text == original_complaint
        assert db_complaint.structured_summary == structured_summary
    finally:
        session.close()


def test_create_service_event_wrong_tenant_vehicle_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
    finally:
        session.close()

    other_tenant = Tenant(slug=f"other-demo-{uuid.uuid4()}", name="Other Demo Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": other_tenant_id},
        json={
            "vehicleId": str(vehicle.id),
            "source": "service-desk",
            "originalComplaint": "This should fail because tenant mismatch.",
        },
    )

    assert response.status_code == 404, response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant.slug))
        session.commit()
    finally:
        session.close()


def test_get_service_event_by_id_returns_created_event(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Cargo Max")
        ).scalar_one()
        vehicle_id = str(vehicle.id)
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": vehicle_id,
            "source": "chat",
            "originalComplaint": "Cargo bed rattles at idle.",
            "structuredSummary": "Rattle in cargo bed during idle.",
            "language": "en",
            "capturedBy": "driver",
        },
    )
    event_id = create_response.json()["id"]

    response = client.get(f"/v1/service-events/{event_id}", headers={"X-Tenant-ID": seeded_demo_tenant})

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["id"] == event_id
    assert payload["vehicleId"] == vehicle_id
    assert payload["source"] == "chat"
    assert payload["state"] == "DRAFT"
    assert payload["complaint"]["originalText"] == "Cargo bed rattles at idle."
    assert payload["complaint"]["structuredSummary"] == "Rattle in cargo bed during idle."


def test_get_service_event_wrong_tenant_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "text",
            "originalComplaint": "Oil pressure flicker.",
        },
    )
    event_id = create_response.json()["id"]

    other_tenant = Tenant(slug=f"other-event-{uuid.uuid4()}", name="Other Event Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    response = client.get(f"/v1/service-events/{event_id}", headers={"X-Tenant-ID": other_tenant_id})
    assert response.status_code == 404, response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant.slug))
        session.commit()
    finally:
        session.close()


def test_patch_complaint_creates_revision_2_and_preserves_history(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "phone",
            "originalComplaint": "  Warning light after startup  ",
            "structuredSummary": "Warning light during startup",
            "language": "en-US",
            "capturedBy": "advisor-42",
        },
    )
    event_id = create_response.json()["id"]

    response = client.patch(
        f"/v1/service-events/{event_id}/complaint",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "structuredSummary": "Warning light during startup after engine crank",
            "language": "en-GB",
            "capturedBy": "advisor-99",
        },
    )

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["state"] == "DRAFT"
    assert payload["complaint"]["revision"] == 2
    assert payload["complaint"]["originalText"] == "  Warning light after startup  "
    assert payload["complaint"]["structuredSummary"] == "Warning light during startup after engine crank"
    assert payload["complaint"]["language"] == "en-GB"
    assert payload["complaint"]["capturedBy"] == "advisor-99"

    session = SessionLocal()
    try:
        event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        complaints = session.execute(select(Complaint).where(Complaint.event_id == event.id).order_by(Complaint.revision)).scalars().all()
        assert len(complaints) == 2
        assert complaints[0].revision == 1
        assert complaints[1].revision == 2
        assert complaints[0].original_text == "  Warning light after startup  "
        assert complaints[1].original_text == "  Warning light after startup  "
        assert complaints[1].structured_summary == "Warning light during startup after engine crank"
    finally:
        session.close()


def test_patch_complaint_with_new_original_text_preserves_exact_value(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "email",
            "originalComplaint": "Customer says CEL on.",
            "structuredSummary": "Check engine light is on.",
        },
    )
    event_id = create_response.json()["id"]

    response = client.patch(
        f"/v1/service-events/{event_id}/complaint",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "originalComplaint": "   Customer says CEL on.   ",
        },
    )

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["complaint"]["originalText"] == "   Customer says CEL on.   "
    assert payload["complaint"]["structuredSummary"] == "Check engine light is on."


def test_patch_complaint_rejects_empty_payload_and_non_draft_state(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "portal",
            "originalComplaint": "Red warning.",
        },
    )
    event_id = create_response.json()["id"]

    empty_response = client.patch(
        f"/v1/service-events/{event_id}/complaint",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={},
    )
    assert empty_response.status_code == 400, empty_response.text

    session = SessionLocal()
    try:
        event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        event.state = __import__("app.service_intake.models", fromlist=["ServiceEventState"]).ServiceEventState.OPEN
        session.commit()
    finally:
        session.close()

    non_draft_response = client.patch(
        f"/v1/service-events/{event_id}/complaint",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"structuredSummary": "Updated summary"},
    )
    assert non_draft_response.status_code == 400, non_draft_response.text


def test_patch_complaint_wrong_tenant_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    create_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "portal",
            "originalComplaint": "Noise in cabin.",
        },
    )
    event_id = create_response.json()["id"]

    other_tenant = Tenant(slug=f"complaint-other-{uuid.uuid4()}", name="Complaint Other Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    response = client.patch(
        f"/v1/service-events/{event_id}/complaint",
        headers={"X-Tenant-ID": other_tenant_id},
        json={"structuredSummary": "Noise in cabin summary"},
    )
    assert response.status_code == 404, response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant.slug))
        session.commit()
    finally:
        session.close()
