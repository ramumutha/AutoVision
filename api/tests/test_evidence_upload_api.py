from __future__ import annotations

import hashlib
import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal, settings
from app.core.models import Tenant
from app.evidence.models import AnalysisRun, Evidence, EvidenceAsset, EvidenceStatus, Finding
from app.main import app
from app.service_intake.models import ServiceEvent
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


@pytest.fixture()
def upload_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, str]:
    monkeypatch.setattr(settings, "EVIDENCE_LOCAL_STORAGE_ROOT", tmp_path / "evidence")
    monkeypatch.setattr(settings, "EVIDENCE_MAX_FILE_SIZE_BYTES", 25 * 1024 * 1024)
    reset_demo_data()
    seed_demo_data()
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
        headers={"X-Tenant-ID": str(tenant.id)},
        json={
            "vehicleId": str(vehicle.id),
            "source": "s2.3-test",
            "originalComplaint": "Evidence upload test",
            "structuredSummary": "Evidence upload summary",
        },
    )
    assert event_response.status_code == 201, event_response.text
    event_id = event_response.json()["id"]

    try:
        yield {"tenant_id": str(tenant.id), "event_id": event_id}
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == tenant.id))
            session.commit()
        finally:
            session.close()
        reset_demo_data()


def _create_evidence(context: dict[str, str], evidence_type: str = "IMAGE") -> str:
    response = client.post(
        f"/v1/service-events/{context['event_id']}/evidence",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={"evidenceType": evidence_type, "captureSource": "UPLOAD"},
    )
    assert response.status_code == 201, response.text
    return response.json()["id"]


def _upload(context: dict[str, str], evidence_id: str, content: bytes, filename: str, media_type: str, **kwargs):
    return client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/upload",
        headers={"X-Tenant-ID": context["tenant_id"]},
        files={"file": (filename, content, media_type)},
        data=kwargs,
    )


def test_image_upload_persists_asset_checksum_size_and_scoped_generated_key(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context)
    content = b"fake image bytes"
    response = _upload(upload_context, evidence_id, content, "../front bumper.jpg", "image/jpeg")

    assert response.status_code == 200, response.text
    assert response.json()["status"] == "UPLOADED"

    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        key_parts = Path(asset.storage_key).parts
        assert key_parts[:3] == (
            upload_context["tenant_id"],
            upload_context["event_id"],
            evidence_id,
        )
        assert "front bumper" not in asset.storage_key
        assert asset.storage_provider == "local"
        assert asset.file_name == "front bumper.jpg"
        assert asset.file_size_bytes == len(content)
        assert asset.checksum_sha256 == hashlib.sha256(content).hexdigest()
    finally:
        session.close()

    complete = client.post(
        f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": upload_context["tenant_id"]},
    )
    assert complete.status_code == 200, complete.text
    assert complete.json()["status"] == "READY"


def test_video_upload_and_lifecycle_conflicts(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context, "VIDEO")
    response = _upload(upload_context, evidence_id, b"video bytes", "clip.mp4", "video/mp4")
    assert response.status_code == 200, response.text
    assert response.json()["status"] == "UPLOADED"

    repeated_upload = _upload(upload_context, evidence_id, b"other", "clip.mp4", "video/mp4")
    assert repeated_upload.status_code == 409

    complete_url = f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/complete"
    complete = client.post(complete_url, headers={"X-Tenant-ID": upload_context["tenant_id"]})
    assert complete.status_code == 200
    repeated_complete = client.post(complete_url, headers={"X-Tenant-ID": upload_context["tenant_id"]})
    assert repeated_complete.status_code == 409


def test_completion_before_upload_is_conflict(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context)
    response = client.post(
        f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": upload_context["tenant_id"]},
    )
    assert response.status_code == 409


def test_validation_failures_leave_pending_and_allow_retry(upload_context: dict[str, str], monkeypatch: pytest.MonkeyPatch) -> None:
    evidence_id = _create_evidence(upload_context)
    cases = [
        (b"bytes", "x.bin", "application/octet-stream", 415),
        (b"bytes", "x.mp4", "video/mp4", 415),
        (b"", "x.jpg", "image/jpeg", 400),
    ]
    for content, filename, media_type, expected_status in cases:
        response = _upload(upload_context, evidence_id, content, filename, media_type)
        assert response.status_code == expected_status, response.text

    monkeypatch.setattr(settings, "EVIDENCE_MAX_FILE_SIZE_BYTES", 2)
    oversized = _upload(upload_context, evidence_id, b"123", "x.jpg", "image/jpeg")
    assert oversized.status_code == 413
    monkeypatch.setattr(settings, "EVIDENCE_MAX_FILE_SIZE_BYTES", 25 * 1024 * 1024)

    checksum = _upload(upload_context, evidence_id, b"bytes", "x.jpg", "image/jpeg", checksum="0" * 64)
    assert checksum.status_code == 400

    session = SessionLocal()
    try:
        evidence = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        assert evidence.status == EvidenceStatus.PENDING_UPLOAD
        assert session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == evidence.id)).scalar_one_or_none() is None
    finally:
        session.close()

    valid = _upload(upload_context, evidence_id, b"bytes", "C:\\absolute\\x.jpg", "image/jpeg")
    assert valid.status_code == 200, valid.text
    assert Path(settings.EVIDENCE_LOCAL_STORAGE_ROOT).resolve() in Path(
        settings.EVIDENCE_LOCAL_STORAGE_ROOT
    ).resolve().parents or Path(settings.EVIDENCE_LOCAL_STORAGE_ROOT).exists()


def test_missing_parent_and_cross_tenant_requests_are_not_revealing(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context)
    other_tenant = Tenant(slug=f"other-{uuid.uuid4()}", name="Other")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()

    try:
        missing_evidence = _upload(upload_context, str(uuid.uuid4()), b"x", "x.jpg", "image/jpeg")
        assert missing_evidence.status_code == 404
        wrong_event = _upload({**upload_context, "event_id": str(uuid.uuid4())}, evidence_id, b"x", "x.jpg", "image/jpeg")
        assert wrong_event.status_code == 404
        cross_tenant_upload = client.post(
            f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/upload",
            headers={"X-Tenant-ID": other_tenant_id},
            files={"file": ("x.jpg", b"x", "image/jpeg")},
        )
        assert cross_tenant_upload.status_code == 404
        cross_tenant = client.post(
            f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/complete",
            headers={"X-Tenant-ID": other_tenant_id},
        )
        assert cross_tenant.status_code == 404
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
            session.commit()
        finally:
            session.close()


def test_completion_fails_when_object_is_missing(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context)
    assert _upload(upload_context, evidence_id, b"bytes", "x.jpg", "image/jpeg").status_code == 200
    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        storage_key = asset.storage_key
    finally:
        session.close()
    Path(settings.EVIDENCE_LOCAL_STORAGE_ROOT, storage_key).unlink()

    response = client.post(
        f"/v1/service-events/{upload_context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": upload_context["tenant_id"]},
    )
    assert response.status_code == 500
    session = SessionLocal()
    try:
        assert session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one().status == EvidenceStatus.UPLOADED
    finally:
        session.close()


def test_db_failure_compensates_final_file(upload_context: dict[str, str], monkeypatch: pytest.MonkeyPatch) -> None:
    evidence_id = _create_evidence(upload_context)

    def fail_asset(*args, **kwargs):
        raise RuntimeError("simulated database failure")

    monkeypatch.setattr("app.evidence.service.create_evidence_asset_for_tenant", fail_asset)
    response = _upload(upload_context, evidence_id, b"bytes", "x.jpg", "image/jpeg")
    assert response.status_code == 500
    assert not [path for path in Path(settings.EVIDENCE_LOCAL_STORAGE_ROOT).rglob("*") if path.is_file()]


def test_no_binary_or_ai_records_are_created(upload_context: dict[str, str]) -> None:
    evidence_id = _create_evidence(upload_context)
    content = b"plain evidence"
    assert _upload(upload_context, evidence_id, content, "/tmp/../x.txt", "image/jpeg").status_code == 200

    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        assert "binary_data" not in EvidenceAsset.__table__.columns
        assert session.execute(select(AnalysisRun)).scalars().all() == []
        assert session.execute(select(Finding)).scalars().all() == []
        assert asset.file_size_bytes == len(content)
    finally:
        session.close()
