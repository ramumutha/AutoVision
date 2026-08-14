import uuid
from datetime import datetime, timedelta, timezone

import pytest
from fastapi.testclient import TestClient
from pydantic import ValidationError
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import CaptureSource, Evidence, EvidenceStatus, EvidenceType
from app.evidence.schemas import EvidenceCreate
from app.main import app
from app.service_intake.models import ServiceEvent
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


def _ensure_service_event(tenant_id: str, vehicle_name: str = "City Compact") -> str:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.id == uuid.UUID(tenant_id))).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == vehicle_name)
        ).scalar_one()
    finally:
        session.close()

    response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": tenant_id},
        json={
            "vehicleId": str(vehicle.id),
            "source": "service-desk",
            "originalComplaint": "Evidence test complaint",
            "structuredSummary": "Evidence summary",
        },
    )
    assert response.status_code == 201, response.text
    return response.json()["id"]


def test_create_evidence_success(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    response = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "IMAGE",
            "captureSource": "UPLOAD",
            "title": "Front bumper",
            "description": "Initial inspection image",
            "capturedAt": "2026-08-14T12:00:00Z",
        },
    )

    assert response.status_code == 201, response.text
    payload = response.json()

    assert payload["tenantId"] == seeded_demo_tenant
    assert payload["serviceEventId"] == event_id
    assert payload["evidenceType"] == "IMAGE"
    assert payload["captureSource"] == "UPLOAD"
    assert payload["title"] == "Front bumper"
    assert payload["description"] == "Initial inspection image"
    assert payload["capturedAt"] == "2026-08-14T12:00:00Z"
    assert payload["capturedByUserRefId"] is None
    assert payload["status"] == "PENDING_UPLOAD"

    session = SessionLocal()
    try:
        db_record = session.execute(select(Evidence).where(Evidence.id == payload["id"])).scalar_one()
        assert db_record.tenant_id == uuid.UUID(seeded_demo_tenant)
        assert db_record.service_event_id == uuid.UUID(event_id)
        assert db_record.evidence_type == EvidenceType.IMAGE
        assert db_record.capture_source == CaptureSource.UPLOAD
        assert db_record.status == EvidenceStatus.PENDING_UPLOAD
        assert db_record.captured_by_user_ref_id is None
    finally:
        session.close()


def test_create_evidence_allows_optional_fields_and_defaults(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    response = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "DOCUMENT",
            "captureSource": "SYSTEM_IMPORT",
        },
    )

    assert response.status_code == 201, response.text
    payload = response.json()
    assert payload["title"] is None
    assert payload["description"] is None
    assert payload["capturedAt"] is None
    assert payload["capturedByUserRefId"] is None
    assert payload["status"] == "PENDING_UPLOAD"


def test_create_evidence_rejects_invalid_enums_and_server_controlled_fields(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    bad_type = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "NOT_A_TYPE",
            "captureSource": "UPLOAD",
        },
    )
    assert bad_type.status_code == 422, bad_type.text

    bad_source = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "IMAGE",
            "captureSource": "NOT_A_SOURCE",
        },
    )
    assert bad_source.status_code == 422, bad_source.text

    bad_extra = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "IMAGE",
            "captureSource": "UPLOAD",
            "tenantId": str(uuid.uuid4()),
        },
    )
    assert bad_extra.status_code == 422, bad_extra.text

    with pytest.raises(ValidationError):
        EvidenceCreate.model_validate({"evidenceType": "IMAGE", "captureSource": "UPLOAD", "status": "READY"})


def test_create_evidence_missing_service_event_returns_404(seeded_demo_tenant: str) -> None:
    response = client.post(
        f"/v1/service-events/{uuid.uuid4()}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "IMAGE",
            "captureSource": "UPLOAD",
        },
    )

    assert response.status_code == 404, response.text
    assert response.json()["detail"] == "Service event not found"


def test_create_evidence_cross_tenant_returns_404(seeded_demo_tenant: str) -> None:
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
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

    event_id = _ensure_service_event(seeded_demo_tenant)

    response = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": other_tenant_id},
        json={
            "evidenceType": "IMAGE",
            "captureSource": "UPLOAD",
        },
    )

    assert response.status_code == 404, response.text
    assert response.json()["detail"] == "Service event not found"

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
        session.commit()
    finally:
        session.close()


def test_list_evidence_for_service_event_and_ordering(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    session = SessionLocal()
    try:
        event = session.execute(select(ServiceEvent).where(ServiceEvent.id == uuid.UUID(event_id))).scalar_one()
        older = Evidence(
            tenant_id=uuid.UUID(seeded_demo_tenant),
            service_event_id=event.id,
            evidence_type=EvidenceType.DOCUMENT,
            capture_source=CaptureSource.UPLOAD,
            title="Older",
            description="Older record",
            status=EvidenceStatus.PENDING_UPLOAD,
            created_at=datetime.now(timezone.utc) - timedelta(minutes=5),
            updated_at=datetime.now(timezone.utc) - timedelta(minutes=5),
        )
        newer = Evidence(
            tenant_id=uuid.UUID(seeded_demo_tenant),
            service_event_id=event.id,
            evidence_type=EvidenceType.IMAGE,
            capture_source=CaptureSource.CAMERA,
            title="Newer",
            description="Newer record",
            status=EvidenceStatus.PENDING_UPLOAD,
            created_at=datetime.now(timezone.utc),
            updated_at=datetime.now(timezone.utc),
        )
        session.add_all([older, newer])
        session.commit()
        session.refresh(older)
        session.refresh(newer)
    finally:
        session.close()

    response = client.get(f"/v1/service-events/{event_id}/evidence", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert response.status_code == 200, response.text
    payload = response.json()
    assert len(payload) >= 2
    assert payload[0]["title"] == "Newer"
    assert payload[1]["title"] == "Older"


def test_list_evidence_empty_for_valid_service_event(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)
    response = client.get(f"/v1/service-events/{event_id}/evidence", headers={"X-Tenant-ID": seeded_demo_tenant})
    assert response.status_code == 200, response.text
    assert response.json() == []


def test_list_evidence_cross_tenant_service_event_isolation(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    other_tenant = Tenant(slug=f"other-demo-{uuid.uuid4()}", name="Other Demo Tenant")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    response = client.get(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": other_tenant_id},
    )

    assert response.status_code == 404, response.text
    assert response.json()["detail"] == "Service event not found"

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
        session.commit()
    finally:
        session.close()


def test_get_evidence_cross_service_event_isolation(seeded_demo_tenant: str) -> None:
    event_a_id = _ensure_service_event(seeded_demo_tenant)
    event_b_id = _ensure_service_event(seeded_demo_tenant)

    create_response = client.post(
        f"/v1/service-events/{event_a_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "VIDEO",
            "captureSource": "CAMERA",
            "title": "Evidence A",
        },
    )
    assert create_response.status_code == 201, create_response.text
    evidence_a_id = create_response.json()["id"]

    response = client.get(
        f"/v1/service-events/{event_b_id}/evidence/{evidence_a_id}",
        headers={"X-Tenant-ID": seeded_demo_tenant},
    )

    assert response.status_code == 404, response.text
    assert response.json()["detail"] == "Evidence not found"


@pytest.mark.parametrize("field_name", ["tenantId", "serviceEventId", "status"])
def test_create_evidence_rejects_server_controlled_fields_via_http(seeded_demo_tenant: str, field_name: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)

    payload = {
        "evidenceType": "IMAGE",
        "captureSource": "UPLOAD",
        field_name: "ignored",
    }

    response = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json=payload,
    )

    assert response.status_code == 422, response.text


def test_get_evidence_by_id_and_cross_tenant_not_found(seeded_demo_tenant: str) -> None:
    event_id = _ensure_service_event(seeded_demo_tenant)
    create_response = client.post(
        f"/v1/service-events/{event_id}/evidence",
        headers={"X-Tenant-ID": seeded_demo_tenant},
        json={
            "evidenceType": "VIDEO",
            "captureSource": "CAMERA",
            "title": "Video evidence",
        },
    )
    evidence_id = create_response.json()["id"]

    get_response = client.get(
        f"/v1/service-events/{event_id}/evidence/{evidence_id}",
        headers={"X-Tenant-ID": seeded_demo_tenant},
    )
    assert get_response.status_code == 200, get_response.text
    assert get_response.json()["id"] == evidence_id

    wrong_tenant = Tenant(slug=f"other-demo-{uuid.uuid4()}", name="Other Demo Tenant")
    session = SessionLocal()
    try:
        session.add(wrong_tenant)
        session.commit()
        wrong_tenant_id = str(wrong_tenant.id)
    finally:
        session.close()

    wrong_tenant_response = client.get(
        f"/v1/service-events/{event_id}/evidence/{evidence_id}",
        headers={"X-Tenant-ID": wrong_tenant_id},
    )
    assert wrong_tenant_response.status_code == 404, wrong_tenant_response.text
    assert wrong_tenant_response.json()["detail"] == "Service event not found"

    missing_in_scope = client.get(
        f"/v1/service-events/{event_id}/evidence/{uuid.uuid4()}",
        headers={"X-Tenant-ID": seeded_demo_tenant},
    )
    assert missing_in_scope.status_code == 404, missing_in_scope.text
    assert missing_in_scope.json()["detail"] == "Evidence not found"

    session = SessionLocal()
    try:
        session.execute(Tenant.__table__.delete().where(Tenant.id == wrong_tenant.id))
        session.commit()
    finally:
        session.close()
