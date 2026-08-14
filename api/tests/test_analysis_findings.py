from __future__ import annotations

import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import select

from app.core.database import SessionLocal, settings
from app.core.models import Tenant
from app.evidence.analysis_provider import FakeFindingProvider
from app.evidence.analysis_schemas import (
    AnalysisObservation,
    AnalysisWorkItem,
    FindingAnalysisResponse,
    ProviderUsage,
)
from app.evidence.analysis_worker import AnalysisWorker
from app.evidence.models import (
    AnalysisRun,
    AnalysisRunStatus,
    Evidence,
    EvidenceAsset,
    EvidenceSufficiency,
    EvidenceStatus,
    Finding,
    FindingEvidence,
    FindingReview,
    FindingReviewStatus,
)
from app.main import app
from app.service_intake.models import ServiceEvent
from app.vehicle.models import PowertrainType, Vehicle, VehicleClass
from scripts.seed_demo import reset_demo_data, seed_demo_data


client = TestClient(app)


class CapturingDispatcher:
    def __init__(self) -> None:
        self.items: list[AnalysisWorkItem] = []

    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        self.items.append(work_item)


class ResponseProvider:
    def __init__(self, response: object) -> None:
        self.response = response

    def analyze(self, request):
        return self.response


class CountingResponseProvider(ResponseProvider):
    def __init__(self, response: object) -> None:
        super().__init__(response)
        self.calls = 0

    def analyze(self, request):
        self.calls += 1
        return super().analyze(request)


@pytest.fixture()
def finding_context(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> dict[str, object]:
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
            "source": "s2.5-test",
            "originalComplaint": "Finding persistence test",
            "structuredSummary": "Finding persistence summary",
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


def _create_ready_evidence(context: dict[str, object], filename: str = "finding.jpg") -> str:
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
        files={"file": (filename, b"finding evidence", "image/jpeg")},
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


def _base_response(observations: tuple[dict, ...], **kwargs) -> FindingAnalysisResponse:
    return FindingAnalysisResponse(
        schema_version="s2.4",
        provider="test-provider",
        model="test-model",
        model_version="1",
        observations=observations,
        evidence_assessment=kwargs.pop("evidence_assessment", EvidenceSufficiency.SUFFICIENT),
        warnings=(),
        usage=ProviderUsage(input_tokens=3, output_tokens=len(observations)),
        **kwargs,
    )


def _observation(evidence_ids: tuple[uuid.UUID, ...], *, code: str = "TEST_FINDING", confidence: float | None = 0.82) -> dict:
    return {
        "finding_code": code,
        "title": "Validated finding",
        "description": "A deterministic validated observation.",
        "component": "camera",
        "location": "front",
        "confidence": confidence,
        "evidence_sufficiency": EvidenceSufficiency.PARTIAL,
        "supporting_evidence_ids": evidence_ids,
    }


def test_valid_observation_persists_finding_and_explicit_provenance(finding_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    response = _base_response((_observation((uuid.UUID(evidence_id),)),))
    AnalysisWorker(provider=ResponseProvider(response)).execute(work_item)

    session = SessionLocal()
    try:
        finding = session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalar_one()
        assert finding.tenant_id == uuid.UUID(finding_context["tenant_id"])
        assert finding.service_event_id == uuid.UUID(finding_context["event_id"])
        assert finding.analysis_run_id == uuid.UUID(run_id)
        assert finding.finding_code == "TEST_FINDING"
        assert finding.title == "Validated finding"
        assert finding.description == "A deterministic validated observation."
        assert finding.component == "camera"
        assert finding.location == "front"
        assert float(finding.confidence) == pytest.approx(0.82)
        assert finding.evidence_sufficiency == EvidenceSufficiency.PARTIAL
        assert finding.review_status == FindingReviewStatus.PENDING_REVIEW
        links = session.execute(select(FindingEvidence).where(FindingEvidence.finding_id == finding.id)).scalars().all()
        assert len(links) == 1
        assert links[0].evidence_id == uuid.UUID(evidence_id)
        assert links[0].relationship_type == "supporting"
        assert session.execute(select(FindingReview)).scalars().all() == []
    finally:
        session.close()
    assert _load_run(run_id).status == AnalysisRunStatus.SUCCEEDED


def test_multiple_observations_and_multiple_explicit_links(finding_context: dict[str, object]) -> None:
    evidence_a = _create_ready_evidence(finding_context, "a.jpg")
    evidence_b = _create_ready_evidence(finding_context, "b.jpg")
    run_id, work_item = _create_run(finding_context)
    response = _base_response(
        (
            _observation((uuid.UUID(evidence_a),), code="FINDING_A"),
            _observation((uuid.UUID(evidence_a), uuid.UUID(evidence_b)), code="FINDING_B"),
        )
    )
    AnalysisWorker(provider=ResponseProvider(response)).execute(work_item)

    session = SessionLocal()
    try:
        findings = session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all()
        assert {finding.finding_code for finding in findings} == {"FINDING_A", "FINDING_B"}
        links = session.execute(
            select(FindingEvidence).where(FindingEvidence.finding_id.in_([finding.id for finding in findings]))
        ).scalars().all()
        assert len(links) == 3
        finding_b = next(finding for finding in findings if finding.finding_code == "FINDING_B")
        assert {link.evidence_id for link in links if link.finding_id == finding_b.id} == {
            uuid.UUID(evidence_a),
            uuid.UUID(evidence_b),
        }
    finally:
        session.close()


def test_valid_then_invalid_observation_rolls_back_all_findings(finding_context: dict[str, object]) -> None:
    evidence_a = _create_ready_evidence(finding_context, "atomic-a.jpg")
    _create_ready_evidence(finding_context, "atomic-b.jpg")
    run_id, work_item = _create_run(finding_context)
    response = _base_response(
        (
            _observation((uuid.UUID(evidence_a),), code="VALID_FIRST"),
            _observation((uuid.uuid4(),), code="INVALID_SECOND"),
        )
    )
    provider = CountingResponseProvider(response)

    AnalysisWorker(provider=provider).execute(work_item)

    session = SessionLocal()
    try:
        run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(run_id))).scalar_one()
        findings = session.execute(select(Finding).where(Finding.analysis_run_id == run.id)).scalars().all()
        finding_ids = [finding.id for finding in findings]
        links = session.execute(
            select(FindingEvidence).where(FindingEvidence.finding_id.in_(finding_ids))
        ).scalars().all() if finding_ids else []
        assert provider.calls == 1
        assert run.status == AnalysisRunStatus.FAILED
        assert run.error_code == "INVALID_EVIDENCE_REFERENCE"
        assert findings == []
        assert links == []
    finally:
        session.close()


def test_cross_tenant_evidence_reference_fails_without_cross_tenant_link(
    finding_context: dict[str, object],
) -> None:
    tenant_b = Tenant(slug=f"s2.5-other-{uuid.uuid4()}", name="S2.5 Other Tenant")
    vehicle_b = Vehicle(
        tenant=tenant_b,
        vehicle_class=VehicleClass.PASSENGER,
        powertrain=PowertrainType.ICE,
        model_name="Other Tenant Vehicle",
        year=2024,
        color="Blue",
    )
    session = SessionLocal()
    try:
        session.add_all([tenant_b, vehicle_b])
        session.commit()
        tenant_b_id = str(tenant_b.id)
        vehicle_b_id = str(vehicle_b.id)
    finally:
        session.close()

    context_b = {"tenant_id": tenant_b_id, "event_id": "", "dispatcher": finding_context["dispatcher"]}
    try:
        event_response = client.post(
            "/v1/service-events",
            headers={"X-Tenant-ID": tenant_b_id},
            json={
                "vehicleId": vehicle_b_id,
                "source": "s2.5-cross-tenant-test",
                "originalComplaint": "Other tenant complaint",
                "structuredSummary": "Other tenant summary",
            },
        )
        assert event_response.status_code == 201, event_response.text
        context_b["event_id"] = event_response.json()["id"]
        evidence_b = _create_ready_evidence(context_b, "tenant-b.jpg")
        session = SessionLocal()
        try:
            evidence_before = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_b))).scalar_one()
            evidence_b_status = evidence_before.status
        finally:
            session.close()

        evidence_a = _create_ready_evidence(finding_context, "tenant-a.jpg")
        run_id, work_item = _create_run(finding_context)
        response = _base_response((_observation((uuid.UUID(evidence_b),), code="CROSS_TENANT"),))
        AnalysisWorker(provider=ResponseProvider(response)).execute(work_item)

        session = SessionLocal()
        try:
            run = session.execute(select(AnalysisRun).where(AnalysisRun.id == uuid.UUID(run_id))).scalar_one()
            findings = session.execute(select(Finding).where(Finding.analysis_run_id == run.id)).scalars().all()
            links = session.execute(
                select(FindingEvidence).where(FindingEvidence.evidence_id == uuid.UUID(evidence_b))
            ).scalars().all()
            evidence_after = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_b))).scalar_one()
            assert evidence_a != evidence_b
            assert run.status == AnalysisRunStatus.FAILED
            assert run.error_code == "INVALID_EVIDENCE_REFERENCE"
            assert findings == []
            assert links == []
            assert evidence_after.status == evidence_b_status == EvidenceStatus.READY
        finally:
            session.close()
    finally:
        session = SessionLocal()
        try:
            session.execute(Tenant.__table__.delete().where(Tenant.id == tenant_b.id))
            session.commit()
        finally:
            session.close()


def test_zero_observations_succeeds_without_findings(finding_context: dict[str, object]) -> None:
    _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    AnalysisWorker(provider=ResponseProvider(_base_response(()))).execute(work_item)
    session = SessionLocal()
    try:
        assert _load_run(run_id).status == AnalysisRunStatus.SUCCEEDED
        assert session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all() == []
        assert session.execute(select(FindingEvidence)).scalars().all() == []
    finally:
        session.close()


@pytest.mark.parametrize(
    ("response", "expected_error"),
    [
        (_base_response((_observation((uuid.uuid4(),)),)), "INVALID_EVIDENCE_REFERENCE"),
        (_base_response((_observation((uuid.uuid4(), uuid.uuid4())),)), "INVALID_EVIDENCE_REFERENCE"),
    ],
)
def test_invalid_evidence_provenance_fails_without_findings(
    finding_context: dict[str, object], response: FindingAnalysisResponse, expected_error: str
) -> None:
    _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    AnalysisWorker(provider=ResponseProvider(response)).execute(work_item)
    session = SessionLocal()
    try:
        assert _load_run(run_id).status == AnalysisRunStatus.FAILED
        assert _load_run(run_id).error_code == expected_error
        assert session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all() == []
        assert session.execute(select(FindingEvidence)).scalars().all() == []
    finally:
        session.close()


def test_invalid_confidence_and_duplicate_supporting_ids_fail(finding_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    invalid = _base_response((_observation((uuid.UUID(evidence_id),)),)).model_dump()
    invalid["observations"][0]["confidence"] = 1.5
    AnalysisWorker(provider=ResponseProvider(invalid)).execute(work_item)
    assert _load_run(run_id).error_code == "INVALID_PROVIDER_RESPONSE"

    _create_ready_evidence(finding_context, "second.jpg")
    run_id, work_item = _create_run(finding_context)
    duplicate = _base_response((_observation((uuid.UUID(evidence_id), uuid.UUID(evidence_id))),))
    AnalysisWorker(provider=ResponseProvider(duplicate)).execute(work_item)
    assert _load_run(run_id).error_code == "INVALID_EVIDENCE_REFERENCE"


def test_forbidden_provider_field_is_rejected(finding_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    response = _base_response((_observation((uuid.UUID(evidence_id),)),))
    forbidden = response.model_dump()
    forbidden["observations"][0]["severity"] = "HIGH"
    AnalysisWorker(provider=ResponseProvider(forbidden)).execute(work_item)
    assert _load_run(run_id).error_code == "INVALID_PROVIDER_RESPONSE"


def test_persistence_failure_rolls_back_findings_and_links(
    finding_context: dict[str, object], monkeypatch: pytest.MonkeyPatch
) -> None:
    evidence_id = _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    response = _base_response((_observation((uuid.UUID(evidence_id),)),))

    def fail_link(*args, **kwargs):
        raise RuntimeError("simulated link failure")

    monkeypatch.setattr("app.evidence.analysis_worker.add_finding_evidence", fail_link)
    AnalysisWorker(provider=ResponseProvider(response)).execute(work_item)
    session = SessionLocal()
    try:
        assert _load_run(run_id).status == AnalysisRunStatus.FAILED
        assert _load_run(run_id).error_code == "FINDING_PERSISTENCE_ERROR"
        assert session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all() == []
        assert session.execute(select(FindingEvidence)).scalars().all() == []
    finally:
        session.close()


def test_evidence_and_asset_remain_unchanged_and_reexecution_is_safe(finding_context: dict[str, object]) -> None:
    evidence_id = _create_ready_evidence(finding_context)
    run_id, work_item = _create_run(finding_context)
    before = SessionLocal()
    try:
        evidence_before = before.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        asset_before = before.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == evidence_before.id)).scalar_one()
        evidence_status = evidence_before.status
        checksum = asset_before.checksum_sha256
        storage_key = asset_before.storage_key
    finally:
        before.close()

    worker = AnalysisWorker(provider=FakeFindingProvider())
    worker.execute(work_item)
    worker.execute(work_item)
    session = SessionLocal()
    try:
        assert session.execute(select(Finding).where(Finding.analysis_run_id == uuid.UUID(run_id))).scalars().all()
        evidence_after = session.execute(select(Evidence).where(Evidence.id == uuid.UUID(evidence_id))).scalar_one()
        asset_after = session.execute(select(EvidenceAsset).where(EvidenceAsset.evidence_id == evidence_after.id)).scalar_one()
        assert evidence_after.status == evidence_status == EvidenceStatus.READY
        assert asset_after.checksum_sha256 == checksum
        assert asset_after.storage_key == storage_key
    finally:
        session.close()
