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
from app.evidence.models import AnalysisRun, Evidence, EvidenceAsset, Finding, FindingReview, FindingReviewStatus
from app.main import app
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


class CapturingDispatcher:
    def __init__(self) -> None:
        self.items: list[AnalysisWorkItem] = []

    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        self.items.append(work_item)


@pytest.fixture()
def review_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, str]:
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
            "source": "s2.6-review-test",
            "originalComplaint": "Finding review test",
            "structuredSummary": "Finding review summary",
        },
    )
    assert event_response.status_code == 201, event_response.text
    try:
        yield {"tenant_id": str(tenant.id), "event_id": event_response.json()["id"], "dispatcher": dispatcher}
    finally:
        app.state.analysis_dispatcher = previous_dispatcher
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == tenant.id))
            session.commit()
        finally:
            session.close()
        reset_demo_data()


def _create_finding(context: dict[str, str]) -> tuple[str, dict[str, object]]:
    created = client.post(
        f"/v1/service-events/{context['event_id']}/evidence",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={"evidenceType": "IMAGE", "captureSource": "UPLOAD"},
    )
    assert created.status_code == 201
    evidence_id = created.json()["id"]
    uploaded = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/upload",
        headers={"X-Tenant-ID": context["tenant_id"]},
        files={"file": ("review.jpg", b"review evidence", "image/jpeg")},
    )
    assert uploaded.status_code == 200, uploaded.text
    completed = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": context["tenant_id"]},
    )
    assert completed.status_code == 200, completed.text
    run_response = client.post(
        f"/v1/service-events/{context['event_id']}/analysis-runs",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={},
    )
    assert run_response.status_code == 202, run_response.text
    dispatcher = context["dispatcher"]
    assert isinstance(dispatcher, CapturingDispatcher)
    AnalysisWorker().execute(dispatcher.items[-1])
    session = SessionLocal()
    try:
        finding = session.execute(
            select(Finding).where(
                Finding.tenant_id == uuid.UUID(context["tenant_id"]),
                Finding.service_event_id == uuid.UUID(context["event_id"]),
            )
        ).scalars().first()
        assert finding is not None
        original = {
            "id": str(finding.id),
            "finding_code": finding.finding_code,
            "title": finding.title,
            "description": finding.description,
            "component": finding.component,
            "location": finding.location,
            "confidence": finding.confidence,
            "evidence_sufficiency": finding.evidence_sufficiency,
            "analysis_run_id": str(finding.analysis_run_id),
            "service_event_id": str(finding.service_event_id),
            "tenant_id": str(finding.tenant_id),
        }
        return str(finding.id), original
    finally:
        session.close()


def _review_url(context: dict[str, str], finding_id: str) -> str:
    return f"/v1/service-events/{context['event_id']}/findings/{finding_id}/reviews"


def test_confirmed_review_persists_and_keeps_original_finding_unchanged(review_context: dict[str, str]) -> None:
    finding_id, original = _create_finding(review_context)
    response = client.post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json={"decision": "CONFIRMED", "comment": "Reviewed"},
    )
    assert response.status_code == 201, response.text
    assert response.json()["decision"] == "CONFIRMED"
    assert response.json()["reviewedByUserRefId"] is None

    session = SessionLocal()
    try:
        finding = session.execute(select(Finding).where(Finding.id == uuid.UUID(finding_id))).scalar_one()
        assert finding.review_status == FindingReviewStatus.CONFIRMED
        assert {key: str(getattr(finding, key)) if key.endswith("_id") else getattr(finding, key) for key in original if key not in {"id", "tenant_id", "service_event_id", "analysis_run_id"}} == {
            key: str(value) if key.endswith("_id") else value for key, value in original.items() if key not in {"id", "tenant_id", "service_event_id", "analysis_run_id"}
        }
        assert session.execute(select(FindingReview).where(FindingReview.finding_id == finding.id)).scalars().all()
    finally:
        session.close()

    read = client.get(
        f"/v1/service-events/{review_context['event_id']}/findings/{finding_id}",
        headers={"X-Tenant-ID": review_context["tenant_id"]},
    )
    assert read.status_code == 200
    assert read.json()["latestReview"]["decision"] == "CONFIRMED"


def test_modified_review_preserves_original_and_exposes_latest_review(review_context: dict[str, str]) -> None:
    finding_id, original = _create_finding(review_context)
    response = client.post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json={"decision": "MODIFIED", "modifiedTitle": "Human title", "modifiedLocation": "rear"},
    )
    assert response.status_code == 201, response.text
    assert response.json()["modifiedTitle"] == "Human title"
    session = SessionLocal()
    try:
        finding = session.execute(select(Finding).where(Finding.id == uuid.UUID(finding_id))).scalar_one()
        assert finding.review_status == FindingReviewStatus.MODIFIED
        assert finding.title == original["title"]
        assert finding.location == original["location"]
    finally:
        session.close()

    read = client.get(
        f"/v1/service-events/{review_context['event_id']}/findings/{finding_id}",
        headers={"X-Tenant-ID": review_context["tenant_id"]},
    )
    assert read.json()["title"] == original["title"]
    assert read.json()["latestReview"]["modifiedTitle"] == "Human title"


def test_rejected_review_and_append_only_latest_review(review_context: dict[str, str]) -> None:
    finding_id, _ = _create_finding(review_context)
    first = client.post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json={"decision": "CONFIRMED"},
    )
    second = client.post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json={"decision": "REJECTED", "reasonCode": "NOT_SUPPORTED"},
    )
    assert first.status_code == 201
    assert second.status_code == 201
    session = SessionLocal()
    try:
        reviews = session.execute(
            select(FindingReview).where(FindingReview.finding_id == uuid.UUID(finding_id)).order_by(FindingReview.created_at.asc(), FindingReview.id.asc())
        ).scalars().all()
        finding = session.execute(select(Finding).where(Finding.id == uuid.UUID(finding_id))).scalar_one()
        assert len(reviews) == 2
        assert reviews[0].decision.value == "CONFIRMED"
        assert reviews[1].decision.value == "REJECTED"
        assert finding.review_status == FindingReviewStatus.REJECTED
    finally:
        session.close()
    read = client.get(
        f"/v1/service-events/{review_context['event_id']}/findings/{finding_id}",
        headers={"X-Tenant-ID": review_context["tenant_id"]},
    )
    assert read.json()["latestReview"]["decision"] == "REJECTED"


@pytest.mark.parametrize(
    "payload",
    [
        {"decision": "NOT_A_DECISION"},
        {"decision": "MODIFIED"},
        {"decision": "CONFIRMED", "modifiedTitle": "nope"},
        {"decision": "REJECTED", "modifiedLocation": "nope"},
    ],
)
def test_invalid_review_payloads_return_422(review_context: dict[str, str], payload: dict[str, object]) -> None:
    finding_id, _ = _create_finding(review_context)
    response = client.post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json=payload,
    )
    assert response.status_code == 422, response.text


def test_cross_tenant_and_wrong_parent_review_return_404(review_context: dict[str, str]) -> None:
    finding_id, _ = _create_finding(review_context)
    other_tenant = Tenant(slug=f"s2.6-review-other-{uuid.uuid4()}", name="S2.6 Review Other")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()
    try:
        cross_tenant = client.post(
            _review_url(review_context, finding_id),
            headers={"X-Tenant-ID": other_tenant_id},
            json={"decision": "CONFIRMED"},
        )
        wrong_parent = client.post(
            f"/v1/service-events/{uuid.uuid4()}/findings/{finding_id}/reviews",
            headers={"X-Tenant-ID": review_context["tenant_id"]},
            json={"decision": "CONFIRMED"},
        )
        assert cross_tenant.status_code == 404
        assert wrong_parent.status_code == 404
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
            session.commit()
        finally:
            session.close()


def test_review_failure_keeps_status_and_creates_no_review(review_context: dict[str, str], monkeypatch: pytest.MonkeyPatch) -> None:
    finding_id, _ = _create_finding(review_context)
    monkeypatch.setattr("app.evidence.finding_service.create_finding_review", lambda *args, **kwargs: (_ for _ in ()).throw(RuntimeError("db failure")))
    response = TestClient(app, raise_server_exceptions=False).post(
        _review_url(review_context, finding_id),
        headers={"X-Tenant-ID": review_context["tenant_id"]},
        json={"decision": "CONFIRMED"},
    )
    assert response.status_code == 500
    session = SessionLocal()
    try:
        finding = session.execute(select(Finding).where(Finding.id == uuid.UUID(finding_id))).scalar_one()
        assert finding.review_status == FindingReviewStatus.PENDING_REVIEW
        assert session.execute(select(FindingReview).where(FindingReview.finding_id == finding.id)).scalars().all() == []
    finally:
        session.close()
