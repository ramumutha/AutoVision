from __future__ import annotations

import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal, settings
from app.core.models import Tenant
from app.evidence.analysis_schemas import AnalysisWorkItem
from app.evidence.models import AnalysisRun, AnalysisRunStatus, Evidence, EvidenceStatus
from app.main import app
from app.service_intake.models import ServiceEvent
from app.vehicle.models import Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


class CapturingDispatcher:
    def __init__(self) -> None:
        self.items: list[AnalysisWorkItem] = []
        self.persisted_statuses: list[AnalysisRunStatus | None] = []

    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        self.items.append(work_item)
        session = SessionLocal()
        try:
            run = session.execute(select(AnalysisRun).where(AnalysisRun.id == work_item.analysisRunId)).scalar_one()
            self.persisted_statuses.append(run.status)
        finally:
            session.close()


class FailingDispatcher:
    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        raise RuntimeError("executor internals must not escape")


@pytest.fixture()
def analysis_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, object]:
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

    response = client.post(
        "/v1/service-events",
        headers={"X-Tenant-ID": str(tenant.id)},
        json={
            "vehicleId": str(vehicle.id),
            "source": "s2.4-test",
            "originalComplaint": "Analysis request test",
            "structuredSummary": "Analysis request summary",
        },
    )
    assert response.status_code == 201, response.text
    context = {"tenant_id": str(tenant.id), "event_id": response.json()["id"], "dispatcher": dispatcher}

    try:
        yield context
    finally:
        app.state.analysis_dispatcher = previous_dispatcher
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == tenant.id))
            session.commit()
        finally:
            session.close()
        reset_demo_data()


def _create_evidence(context: dict[str, object], media_type: str = "image/jpeg") -> str:
    evidence_type = "VIDEO" if media_type.startswith("video/") else "IMAGE"
    response = client.post(
        f"/v1/service-events/{context['event_id']}/evidence",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={"evidenceType": evidence_type, "captureSource": "UPLOAD"},
    )
    assert response.status_code == 201, response.text
    evidence_id = response.json()["id"]
    upload = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/upload",
        headers={"X-Tenant-ID": context["tenant_id"]},
        files={"file": ("evidence.bin", b"deterministic evidence", media_type)},
    )
    assert upload.status_code == 200, upload.text
    complete = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": context["tenant_id"]},
    )
    assert complete.status_code == 200, complete.text
    return evidence_id


def _request_analysis(context: dict[str, object]):
    return client.post(
        f"/v1/service-events/{context['event_id']}/analysis-runs",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={},
    )


def test_request_analysis_commits_queued_run_and_freezes_ready_evidence(analysis_context: dict[str, object]) -> None:
    evidence_a = _create_evidence(analysis_context)
    evidence_b = _create_evidence(analysis_context, "video/mp4")

    response = _request_analysis(analysis_context)
    assert response.status_code == 202, response.text
    payload = response.json()
    assert payload["status"] == "QUEUED"
    assert payload["inputEvidenceCount"] == 2

    dispatcher = analysis_context["dispatcher"]
    assert isinstance(dispatcher, CapturingDispatcher)
    assert len(dispatcher.items) == 1
    item = dispatcher.items[0]
    assert set(str(evidence_id) for evidence_id in item.evidenceIds) == {evidence_a, evidence_b}
    assert dispatcher.persisted_statuses == [AnalysisRunStatus.QUEUED]

    session = SessionLocal()
    try:
        run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(payload["id"]))).scalar_one()
        assert run.status == AnalysisRunStatus.QUEUED
        assert run.input_evidence_count == 2
    finally:
        session.close()


def test_request_analysis_requires_ready_evidence(analysis_context: dict[str, object]) -> None:
    response = _request_analysis(analysis_context)
    assert response.status_code == 409, response.text
    assert response.json()["detail"] == "No READY evidence available"


def test_request_analysis_missing_or_cross_tenant_event_is_404(analysis_context: dict[str, object]) -> None:
    missing = client.post(
        f"/v1/service-events/{uuid.uuid4()}/analysis-runs",
        headers={"X-Tenant-ID": analysis_context["tenant_id"]},
        json={},
    )
    assert missing.status_code == 404

    other_tenant = Tenant(slug=f"analysis-other-{uuid.uuid4()}", name="Analysis Other")
    session = SessionLocal()
    try:
        session.add(other_tenant)
        session.commit()
        other_tenant_id = str(other_tenant.id)
    finally:
        session.close()
    try:
        cross_tenant = client.post(
            f"/v1/service-events/{analysis_context['event_id']}/analysis-runs",
            headers={"X-Tenant-ID": other_tenant_id},
            json={},
        )
        assert cross_tenant.status_code == 404
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == other_tenant.id))
            session.commit()
        finally:
            session.close()


def test_active_analysis_run_is_rejected_for_queued_and_processing(analysis_context: dict[str, object]) -> None:
    _create_evidence(analysis_context)
    first = _request_analysis(analysis_context)
    assert first.status_code == 202
    duplicate = _request_analysis(analysis_context)
    assert duplicate.status_code == 409

    session = SessionLocal()
    try:
        run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(first.json()["id"]))).scalar_one()
        run.status = AnalysisRunStatus.PROCESSING
        session.commit()
    finally:
        session.close()
    duplicate_processing = _request_analysis(analysis_context)
    assert duplicate_processing.status_code == 409


def test_get_analysis_run_is_parent_and_tenant_scoped(analysis_context: dict[str, object]) -> None:
    _create_evidence(analysis_context)
    created = _request_analysis(analysis_context)
    run_id = created.json()["id"]

    response = client.get(
        f"/v1/service-events/{analysis_context['event_id']}/analysis-runs/{run_id}",
        headers={"X-Tenant-ID": analysis_context["tenant_id"]},
    )
    assert response.status_code == 200
    assert response.json()["status"] == "QUEUED"

    wrong_event = client.get(
        f"/v1/service-events/{uuid.uuid4()}/analysis-runs/{run_id}",
        headers={"X-Tenant-ID": analysis_context["tenant_id"]},
    )
    assert wrong_event.status_code == 404

    wrong_tenant = client.get(
        f"/v1/service-events/{analysis_context['event_id']}/analysis-runs/{run_id}",
        headers={"X-Tenant-ID": str(uuid.uuid4())},
    )
    assert wrong_tenant.status_code == 404


def test_dispatch_failure_marks_committed_run_failed_without_internal_details(analysis_context: dict[str, object]) -> None:
    _create_evidence(analysis_context)
    app.state.analysis_dispatcher = FailingDispatcher()

    response = _request_analysis(analysis_context)
    assert response.status_code == 500
    assert response.json()["detail"] == "Analysis work could not be dispatched"
    assert "executor internals" not in response.text

    session = SessionLocal()
    try:
        run = session.execute(
            select(AnalysisRun).where(
                AnalysisRun.tenant_id == uuid.UUID(analysis_context["tenant_id"]),
                AnalysisRun.service_event_id == uuid.UUID(analysis_context["event_id"]),
            )
        ).scalar_one()
        assert run.status == AnalysisRunStatus.FAILED
        assert run.error_code == "DISPATCH_ERROR"
        assert run.completed_at is not None
    finally:
        session.close()
