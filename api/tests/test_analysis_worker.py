from __future__ import annotations

import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal, settings
from app.core.models import Tenant
from app.evidence.analysis_provider import FakeFindingProvider, ProviderExecutionError
from app.evidence.analysis_schemas import AnalysisWorkItem, FindingAnalysisRequest
from app.evidence.analysis_worker import AnalysisWorker
from app.evidence.models import AnalysisRun, AnalysisRunStatus, Evidence, EvidenceAsset, EvidenceStatus, Finding
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


class CountingProvider:
    def __init__(self, *, fail: bool = False) -> None:
        self.delegate = FakeFindingProvider(fail=fail)
        self.calls = 0
        self.requests: list[FindingAnalysisRequest] = []

    def analyze(self, request: FindingAnalysisRequest):
        self.calls += 1
        self.requests.append(request)
        return self.delegate.analyze(request)


class InvalidProvider:
    def analyze(self, request: FindingAnalysisRequest):
        return {"provider": "invalid"}


@pytest.fixture()
def worker_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, object]:
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
            "source": "s2.4-worker-test",
            "originalComplaint": "Worker test",
            "structuredSummary": "Worker summary",
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


def _create_ready_evidence(context: dict[str, object]) -> str:
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
        files={"file": ("worker.jpg", b"worker evidence", "image/jpeg")},
    )
    assert uploaded.status_code == 200, uploaded.text
    completed = client.post(
        f"/v1/service-events/{context['event_id']}/evidence/{evidence_id}/complete",
        headers={"X-Tenant-ID": context["tenant_id"]},
    )
    assert completed.status_code == 200, completed.text
    return evidence_id


def _create_run(context: dict[str, object]) -> tuple[str, AnalysisWorkItem]:
    response = client.post(
        f"/v1/service-events/{context['event_id']}/analysis-runs",
        headers={"X-Tenant-ID": context["tenant_id"]},
        json={},
    )
    assert response.status_code == 202, response.text
    dispatcher = context["dispatcher"]
    assert isinstance(dispatcher, CapturingDispatcher)
    return response.json()["id"], dispatcher.items[-1]


def _load_run(run_id: str) -> AnalysisRun:
    session = SessionLocal()
    try:
        return session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(run_id))).scalar_one()
    finally:
        session.close()


def test_worker_success_records_metadata_and_persists_findings(worker_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    provider = CountingProvider()

    AnalysisWorker(provider=provider).execute(work_item)

    run = _load_run(run_id)
    assert run.status == AnalysisRunStatus.SUCCEEDED
    assert run.started_at is not None
    assert run.completed_at is not None
    assert run.provider == "fake"
    assert run.model == "autovision-fake"
    assert run.model_version == "1"
    assert run.schema_version == "s2.4"
    assert run.prompt_name == "s2.4-fake-analysis"
    assert run.prompt_version == "1"
    assert run.latency_ms is not None
    assert run.input_tokens == 1
    assert run.output_tokens == 1
    assert provider.calls == 1
    assert str(work_item.evidenceIds[0]) == evidence_id

    session = SessionLocal()
    try:
        findings = session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all()
        assert len(findings) == 1
        evidence = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == evidence.id)).scalar_one()
        assert evidence.status == EvidenceStatus.READY
        assert asset.file_size_bytes == len(b"worker evidence")
    finally:
        session.close()


def test_worker_does_not_rerun_succeeded_processing_or_failed_runs(worker_context: dict[str, object]) -> None:
    _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    provider = CountingProvider()
    worker = AnalysisWorker(provider=provider)
    worker.execute(work_item)
    assert provider.calls == 1

    worker.execute(work_item)
    assert provider.calls == 1

    session = SessionLocal()
    try:
        run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(run_id))).scalar_one()
        run.status = AnalysisRunStatus.PROCESSING
        session.commit()
    finally:
        session.close()
    worker.execute(work_item)
    assert provider.calls == 1

    session = SessionLocal()
    try:
        run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(run_id))).scalar_one()
        run.status = AnalysisRunStatus.FAILED
        session.commit()
    finally:
        session.close()
    worker.execute(work_item)
    assert provider.calls == 1


def test_worker_fails_when_evidence_is_no_longer_ready(worker_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    session = SessionLocal()
    try:
        evidence = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        evidence.status = EvidenceStatus.UPLOADED
        session.commit()
    finally:
        session.close()

    provider = CountingProvider()
    AnalysisWorker(provider=provider).execute(work_item)
    run = _load_run(run_id)
    assert run.status == AnalysisRunStatus.FAILED
    assert run.error_code == "EVIDENCE_NOT_READY"
    assert run.completed_at is not None
    assert provider.calls == 0


def test_worker_fails_when_asset_or_storage_object_is_missing(worker_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        session.delete(asset)
        evidence = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        evidence.status = EvidenceStatus.UPLOADED
        session.commit()
    finally:
        session.close()
    AnalysisWorker().execute(work_item)
    assert _load_run(run_id).error_code == "EVIDENCE_NOT_READY"


def test_worker_fails_when_asset_is_missing(worker_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        session.delete(asset)
        session.commit()
    finally:
        session.close()
    AnalysisWorker().execute(work_item)
    assert _load_run(run_id).error_code == "EVIDENCE_ASSET_NOT_FOUND"


def test_worker_fails_when_storage_object_is_missing(worker_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    session = SessionLocal()
    try:
        asset = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == uuid.UUID(evidence_id))).scalar_one()
        storage_key = asset.storage_key
    finally:
        session.close()
    Path(settings.EVIDENCE_LOCAL_STORAGE_ROOT, storage_key).unlink()
    AnalysisWorker().execute(work_item)
    missing_object_run = _load_run(run_id)
    assert missing_object_run.status == AnalysisRunStatus.FAILED
    assert missing_object_run.error_code == "STORAGE_OBJECT_NOT_FOUND"


def test_worker_records_provider_failure_and_invalid_response(worker_context: dict[str, object]) -> None:
    _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    provider = CountingProvider(fail=True)
    AnalysisWorker(provider=provider).execute(work_item)
    failed = _load_run(run_id)
    assert failed.status == AnalysisRunStatus.FAILED
    assert failed.error_code == "PROVIDER_ERROR"
    assert failed.error_message == "Fake analysis provider failed"

    _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    AnalysisWorker(provider=InvalidProvider()).execute(work_item)
    invalid = _load_run(run_id)
    assert invalid.status == AnalysisRunStatus.FAILED
    assert invalid.error_code == "INVALID_PROVIDER_RESPONSE"


def test_tampered_work_item_cannot_process_another_tenant_run(worker_context: dict[str, object]) -> None:
    _create_ready_evidence(worker_context)
    run_id, work_item = _create_run(worker_context)
    tampered = work_item.model_copy(update={"tenantId": uuid.uuid4()})
    provider = CountingProvider()
    AnalysisWorker(provider=provider).execute(tampered)

    run = _load_run(run_id)
    assert run.status == AnalysisRunStatus.QUEUED
    assert provider.calls == 0
