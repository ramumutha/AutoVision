from __future__ import annotations

import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal, settings
from app.core.models import Tenant
from app.evidence.analysis_schemas import AnalysisWorkItem
from app.evidence.analysis_worker import AnalysisWorker
from app.evidence.models import AnalysisRun, Evidence, EvidenceAsset, Finding, FindingEvidence
from app.main import app
from app.service_intake.models import ServiceEvent
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


class CapturingDispatcher:
    def __init__(self) -> None:
        self.items: list[AnalysisWorkItem] = []

    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        self.items.append(work_item)


@pytest.fixture()
def finding_api_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, str]:
    monkeypatch.setattr(settings, "EVIDENCE_LOCAL_STORAGE_ROOT", tmp_path / "evidence")
    reset_demo_data()
    seed_demo_data()
    previous_dispatcher = app.state.analysis_dispatcher
    dispatcher = CapturingDispatcher()
    app.state.analysis_dispatcher = dispatcher
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
            "source": "s2.6-read-test",
            "originalComplaint": "Finding API test",
            "structuredSummary": "Finding API summary",
        },
    )
    assert event_response.status_code == 201, event_response.text
    try:
        yield {"tenant_id": str(tenant.id), "event_id": event_response.json()["id"]}
    finally:
        app.state.analysis_dispatcher = previous_dispatcher
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == tenant.id))
            session.commit()
        finally:
            session.close()
        reset_demo_data()


def _create_ready_evidence(context: dict[str, str], filename: str) -> str:
    created = client.post(
        f"/v1/service-events/{context['event_id']}/evidence",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={"evidenceType": "IMAGE", "captureSource": "UPLOAD"},
    )
    assert created.status_code == 201, created.text
    evidence_id = created.json()["id"]
    uploaded = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/upload",
        headers={"X-Tenant-ID": context["tenant_id"]},
        files={"file": (filename, b"read api evidence", "image/jpeg")},
    )
    assert uploaded.status_code == 200, uploaded.text
    completed = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": context["tenant_id"]},
    )
    assert completed.status_code == 200, completed.text
    return evidence_id


def _create_finding(context: dict[str, str], filename: str = "finding.jpg") -> tuple[str, str]:
    evidence_id = _create_ready_evidence(context, filename)
    captured = app.state.analysis_dispatcher
    response = client.post(
        f"/v1/service-events/{context['event_id']}/analysis-runs",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={},
    )
    assert response.status_code == 202, response.text
    work_item = captured.items[-1]
    AnalysisWorker().execute(work_item)
    session = SessionLocal()
    try:
        finding = session.execute(
            select(Finding).where(
                Finding.tenant_id == uuid.UUID(context["tenant_id"]),
                Finding.service_event_id == uuid.UUID(context["event_id"]),
            ).order_by(Finding.created_at.desc(), Finding.id.desc())
        ).scalars().first()
        assert finding is not None
        return str(finding.id), evidence_id
    finally:
        session.close()


def test_findings_list_and_get_include_provenance_and_null_latest_review(finding_api_context: dict[str, str]) -> None:
    finding_id, evidence_id = _create_finding(finding_api_context)
    list_response = client.get(
        f"/v1/service-events/{finding_api_context['event_id']}/findings",
        headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
    )
    assert list_response.status_code == 200, list_response.text
    payload = list_response.json()
    assert len(payload) == 1
    assert payload[0]["id"] == finding_id
    assert payload[0]["supportingEvidenceIds"] == [evidence_id]
    assert payload[0]["latestReview"] is None

    get_response = client.get(
        f"/v1/service-events/{finding_api_context['event_id']}/findings/{finding_id}",
        headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
    )
    assert get_response.status_code == 200, get_response.text
    assert get_response.json()["supportingEvidenceIds"] == [evidence_id]


def test_findings_list_is_deterministic_and_empty_for_event_without_findings(finding_api_context: dict[str, str]) -> None:
    _create_finding(finding_api_context, "first.jpg")
    _create_finding(finding_api_context, "second.jpg")
    response = client.get(
        f"/v1/service-events/{finding_api_context['event_id']}/findings",
        headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
    )
    assert response.status_code == 200
    items = response.json()
    assert len(items) == 3
    assert [(item["createdAt"], item["id"]) for item in items] == sorted(
        ((item["createdAt"], item["id"]) for item in items),
        reverse=True,
    )

    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.id == uuid.UUID(finding_api_context["tenant_id"]))).scalar_one()
        vehicle = session.execute(select(Vehicle).where(Vehicle.tenant_id == tenant.id)).scalars().first()
    finally:
        session.close()
    other_event_response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
        json={
            "vehicleId": str(vehicle.id),
            "source": "s2.6-empty-test",
            "originalComplaint": "Empty findings event",
        },
    )
    assert other_event_response.status_code == 201
    empty = client.get(
        f"/v1/service-events/{other_event_response.json()['id']}/findings",
        headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
    )
    assert empty.status_code == 200
    assert empty.json() == []


def test_findings_read_cross_tenant_and_wrong_parent_are_404(finding_api_context: dict[str, str]) -> None:
    finding_id, _ = _create_finding(finding_api_context)
    other_tenant = Tenant(slug=f"s2.6-read-other-{uuid.uuid4()}", name="S2.6 Read Other")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()
    try:
        cross_list = client.get(
            f"/v1/service-events/{finding_api_context['event_id']}/findings",
            headers={"X-Tenant-ID": other_tenant_id},
        )
        assert cross_list.status_code == 404
        cross_get = client.get(
            f"/v1/service-events/{finding_api_context['event_id']}/findings/{finding_id}",
            headers={"X-Tenant-ID": other_tenant_id},
        )
        assert cross_get.status_code == 404
        wrong_parent = client.get(
            f"/v1/service-events/{uuid.uuid4()}/findings/{finding_id}",
            headers={"X-Tenant-ID": finding_api_context["tenant_id"]},
        )
        assert wrong_parent.status_code == 404
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
            session.commit()
        finally:
            session.close()
