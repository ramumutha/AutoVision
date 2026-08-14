import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.identity.models import UserRef
from app.main import app
from app.service_intake.models import (
    Complaint,
    ServiceEvent,
    ServiceEventAssignment,
    ServiceEventContext,
    ServiceEventState,
)
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


def test_list_service_events_for_vehicle_returns_draft_and_open_events_newest_first(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        other_vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Cargo Max")
        ).scalar_one()
        vehicle_id = str(vehicle.id)
        other_vehicle_id = str(other_vehicle.id)
    finally:
        session.close()

    first_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": vehicle_id,
            "source": "chat",
            "originalComplaint": "First complaint",
            "structuredSummary": "First summary",
        },
    )
    first_event_id = first_response.json()["id"]

    patched_response = client.patch(
        f"/v1/service-events/{first_event_id}/complaint",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"structuredSummary": "First summary revised"},
    )
    assert patched_response.status_code == 200, patched_response.text

    second_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": vehicle_id,
            "source": "phone",
            "originalComplaint": "Second complaint",
            "structuredSummary": "Second summary",
        },
    )
    second_event_id = second_response.json()["id"]

    open_response = client.post(
        f"/v1/service-events/{second_event_id}/open",
        headers={"X-Tenant-ID": seeded_demo_tenant},
    )
    assert open_response.status_code == 200, open_response.text

    client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": other_vehicle_id,
            "source": "service-desk",
            "originalComplaint": "Other vehicle complaint",
        },
    )

    response = client.get("/v1/service-events", headers={"X-Tenant-ID": seeded_demo_tenant}, params={"vehicleId": vehicle_id})

    assert response.status_code == 200, response.text
    payload = response.json()
    assert len(payload) == 2
    assert payload[0]["id"] == second_event_id
    assert payload[0]["state"] == "OPEN"
    assert payload[0]["complaint"]["revision"] == 1
    assert payload[1]["id"] == first_event_id
    assert payload[1]["state"] == "DRAFT"
    assert payload[1]["complaint"]["revision"] == 2
    assert payload[1]["complaint"]["structuredSummary"] == "First summary revised"


def test_list_service_events_excludes_other_vehicle_and_wrong_tenant_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        other_vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
        vehicle_id = str(vehicle.id)
        other_vehicle_id = str(other_vehicle.id)
    finally:
        session.close()

    client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": vehicle_id,
            "source": "phone",
            "originalComplaint": "Vehicle A complaint",
        },
    )
    client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": other_vehicle_id,
            "source": "phone",
            "originalComplaint": "Vehicle B complaint",
        },
    )

    list_response = client.get("/v1/service-events", headers={"X-Tenant-ID": seeded_demo_tenant}, params={"vehicleId": vehicle_id})
    assert list_response.status_code == 200, list_response.text
    assert len(list_response.json()) == 1
    assert list_response.json()[0]["vehicleId"] == vehicle_id

    other_tenant = Tenant(slug=f"other-service-list-{uuid.uuid4()}", name="Other Service List Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    wrong_tenant_response = client.get("/v1/service-events", headers={"X-Tenant-ID": other_tenant_id}, params={"vehicleId": vehicle_id})
    assert wrong_tenant_response.status_code == 404, wrong_tenant_response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant.slug))
        session.commit()
    finally:
        session.close()


def test_list_service_events_empty_for_vehicle_without_events(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
        vehicle_id = str(vehicle.id)
    finally:
        session.close()

    response = client.get("/v1/service-events", headers={"X-Tenant-ID": seeded_demo_tenant}, params={"vehicleId": vehicle_id})

    assert response.status_code == 200, response.text
    assert response.json() == []


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


def test_create_service_event_assignment_success(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "service-desk",
            "originalComplaint": "Warning light is on.",
        },
    )
    event_id = event_response.json()["id"]

    response = client.post(
        f"/v1/service-events/{event_id}/assignments",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"roleCode": "TECHNICIAN"},
    )

    assert response.status_code == 201, response.text
    payload = response.json()
    assert payload["eventId"] == event_id
    assert payload["roleCode"] == "TECHNICIAN"
    assert payload["userRef"] is None
    assert payload["assignedAt"] is not None

    session = SessionLocal()
    try:
        assignment = session.execute(select(ServiceEventAssignment).where(ServiceEventAssignment.event_id == uuid.UUID(event_id))).scalar_one()
        assert assignment.role_code == "TECHNICIAN"
        assert assignment.tenant_id == uuid.UUID(seeded_demo_tenant)
        assert assignment.user_ref_id is None
    finally:
        session.close()


def test_create_service_event_assignment_with_user_ref_and_get_event_returns_assignment(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Cargo Max")
        ).scalar_one()
        user = session.execute(select(UserRef).where(UserRef.tenant_id == tenant.id, UserRef.external_user_id == "tech-01")).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "phone",
            "originalComplaint": "Cabin noise.",
        },
    )
    event_id = event_response.json()["id"]

    response = client.post(
        f"/v1/service-events/{event_id}/assignments",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"roleCode": "SERVICE_ADVISOR", "userRef": str(user.id)},
    )

    assert response.status_code == 201, response.text
    payload = response.json()
    assert payload["roleCode"] == "SERVICE_ADVISOR"
    assert payload["userRef"] == str(user.id)

    get_response = client.get(f"/v1/service-events/{event_id}", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert get_response.status_code == 200, get_response.text
    assignment_payload = get_response.json()["assignments"][0]
    assert assignment_payload["roleCode"] == "SERVICE_ADVISOR"
    assert assignment_payload["userRef"] == str(user.id)


def test_create_service_event_assignment_wrong_tenant_and_cross_tenant_user_ref_return_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
        tenant_user = session.execute(select(UserRef).where(UserRef.tenant_id == tenant.id, UserRef.external_user_id == "svc-advisor-01")).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "email",
            "originalComplaint": "Vehicle shakes.",
        },
    )
    event_id = event_response.json()["id"]

    other_tenant_slug = f"assign-other-{uuid.uuid4()}"
    other_tenant = Tenant(slug=other_tenant_slug, name="Assignment Other Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)

        other_user = UserRef(tenant_id=other_tenant.id, external_user_id="other-user-1", display_name="Other User", is_active=True)
        session.add(other_user)
        session.commit()
        other_user_id = str(other_user.id)
    finally:
        session.close()

    wrong_event_response = client.post(
        f"/v1/service-events/{event_id}/assignments",
        headers={"X-Tenant-ID": other_tenant_id},
        json={"roleCode": "TECHNICIAN"},
    )
    assert wrong_event_response.status_code == 404, wrong_event_response.text

    cross_user_response = client.post(
        f"/v1/service-events/{event_id}/assignments",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"roleCode": "TECHNICIAN", "userRef": other_user_id},
    )
    assert cross_user_response.status_code == 404, cross_user_response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant_slug))
        session.commit()
    finally:
        session.close()


def test_create_service_event_context_success_and_round_trip_and_get_event_returns_context(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "portal",
            "originalComplaint": "Battery warning is on.",
            "structuredSummary": "Battery warning.",
        },
    )
    event_id = event_response.json()["id"]
    snapshot = {"vehicle": {"odometerKm": 12345}, "signals": ["battery", "warning"], "meta": {"source": "dashboard"}}

    response = client.post(
        f"/v1/service-events/{event_id}/contexts",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={"contextType": "DIAGNOSTIC_SNAPSHOT", "sourceRef": "dashboard-1", "snapshotJson": snapshot},
    )

    assert response.status_code == 201, response.text
    payload = response.json()
    assert payload["eventId"] == event_id
    assert payload["contextType"] == "DIAGNOSTIC_SNAPSHOT"
    assert payload["sourceRef"] == "dashboard-1"
    assert payload["snapshotJson"] == snapshot
    assert payload["capturedAt"] is not None

    session = SessionLocal()
    try:
        context = session.execute(select(ServiceEventContext).where(ServiceEventContext.event_id == uuid.UUID(event_id))).scalar_one()
        assert context.context_type == "DIAGNOSTIC_SNAPSHOT"
        assert context.snapshot_json == snapshot
        assert context.source_ref == "dashboard-1"
    finally:
        session.close()

    get_response = client.get(f"/v1/service-events/{event_id}", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert get_response.status_code == 200, get_response.text
    assert get_response.json()["contexts"][0]["contextType"] == "DIAGNOSTIC_SNAPSHOT"
    assert get_response.json()["contexts"][0]["snapshotJson"] == snapshot


def test_create_service_event_context_wrong_tenant_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "portal",
            "originalComplaint": "Airbag warning.",
        },
    )
    event_id = event_response.json()["id"]

    other_tenant_slug = f"context-other-{uuid.uuid4()}"
    other_tenant = Tenant(slug=other_tenant_slug, name="Context Other Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    response = client.post(
        f"/v1/service-events/{event_id}/contexts",
        headers={"X-Tenant-ID": other_tenant_id},
        json={"contextType": "DIAGNOSTIC_SNAPSHOT", "snapshotJson": {"k": "v"}},
    )
    assert response.status_code == 404, response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant_slug))
        session.commit()
    finally:
        session.close()


def test_open_service_event_successful_transition(seeded_demo_tenant: str) -> None:
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
            "originalComplaint": "Warning light is on.",
            "structuredSummary": "Engine warning light on.",
        },
    )
    event_id = create_response.json()["id"]
    before_revision = create_response.json()["revision"]
    before_complaint_revision = create_response.json()["complaint"]["revision"]

    response = client.post(f"/v1/service-events/{event_id}/open", headers={"X-Tenant-ID": seeded_demo_tenant})

    assert response.status_code == 200, response.text
    payload = response.json()
    assert payload["state"] == "OPEN"
    assert payload["revision"] == before_revision + 1
    assert payload["openedAt"] is not None
    assert payload["complaint"]["revision"] == before_complaint_revision
    assert payload["complaint"]["originalText"] == "Warning light is on."

    session = SessionLocal()
    try:
        event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        complaint = session.execute(select(Complaint).where(Complaint.event_id == event.id).order_by(Complaint.revision.desc())).scalars().first()
        assert event.state == ServiceEventState.OPEN
        assert event.revision == before_revision + 1
        assert event.opened_at is not None
        assert complaint is not None
        assert complaint.revision == before_complaint_revision
    finally:
        session.close()


def test_open_service_event_rejects_second_open_and_preserves_history(seeded_demo_tenant: str) -> None:
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
            "originalComplaint": "Battery warning is flashing.",
        },
    )
    event_id = create_response.json()["id"]

    first_open = client.post(f"/v1/service-events/{event_id}/open", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert first_open.status_code == 200, first_open.text

    second_open = client.post(f"/v1/service-events/{event_id}/open", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert second_open.status_code == 409, second_open.text

    session = SessionLocal()
    try:
        event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        complaint_count = session.execute(select(Complaint).where(Complaint.event_id == event.id)).scalars().all()
        assert event.state == ServiceEventState.OPEN
        assert event.revision == 2
        assert event.opened_at is not None
        assert len(complaint_count) == 1
    finally:
        session.close()


def test_open_service_event_wrong_tenant_and_unknown_event_return_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Eclipse")
        ).scalar_one()
    finally:
        session.close()

    event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "vehicleId": str(vehicle.id),
            "source": "email",
            "originalComplaint": "Vehicle shakes at idle.",
        },
    )
    event_id = event_response.json()["id"]

    other_tenant = Tenant(slug=f"open-other-{uuid.uuid4()}", name="Open Other Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    wrong_tenant_response = client.post(f"/v1/service-events/{event_id}/open", headers={"X-Tenant-ID": other_tenant_id})
    assert wrong_tenant_response.status_code == 404, wrong_tenant_response.text

    unknown_response = client.post(f"/v1/service-events/{uuid.uuid4()}/open", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert unknown_response.status_code == 404, unknown_response.text

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.slug == other_tenant.slug))
        session.commit()
    finally:
        session.close()


def test_open_service_event_requires_complaint_and_keeps_draft_on_failure(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "Cargo Max")
        ).scalar_one()
    finally:
        session.close()

    event = ServiceEvent(
        tenant_id=uuid.UUID(seeded_demo_tenant),
        vehicle_id=vehicle.id,
        source="manual",
        state=ServiceEventState.DRAFT,
        revision=1,
        opened_at=None,
    )
    session = SessionLocal()
    try:
        session.add(event)
        session.commit()
        event_id = str(event.id)
    finally:
        session.close()

    response = client.post(f"/v1/service-events/{event_id}/open", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert response.status_code == 409, response.text

    session = SessionLocal()
    try:
        stored_event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        assert stored_event.state == ServiceEventState.DRAFT
        assert stored_event.revision == 1
        assert stored_event.opened_at is None
    finally:
        session.close()
