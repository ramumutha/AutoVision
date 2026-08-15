from __future__ import annotations

import uuid
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.finding_repository import add_finding, add_finding_evidence, create_finding_review
from app.evidence.models import (
    AnalysisRun,
    CaptureSource,
    Evidence,
    EvidenceSufficiency,
    EvidenceStatus,
    EvidenceType,
    FindingReviewDecision,
    FindingReviewStatus,
)
from app.prediction.input_builder import PredictionInputBuilder
from app.prediction.models import PredictionAssessment, PredictionFactor, PredictionInputQuality, PredictionRun
from app.service_intake.models import ServiceEvent, ServiceEventState
from app.vehicle.models import UsageSnapshot, Vehicle
from scripts.seed_demo import reset_demo_data, seed_demo_data


@pytest.fixture()
def seeded_context() -> tuple[Tenant, Vehicle]:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        return tenant, vehicle
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    yield
    reset_demo_data()


def _event(session, tenant: Tenant, vehicle: Vehicle, *, days_ago: int = 1, opened: bool = True) -> ServiceEvent:
    occurred_at = datetime.now(timezone.utc) - timedelta(days=days_ago)
    service_event = ServiceEvent(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        source=f"builder-{uuid.uuid4()}",
        state=ServiceEventState.OPEN if opened else ServiceEventState.DRAFT,
        revision=1,
        opened_at=occurred_at if opened else None,
        created_at=occurred_at,
    )
    session.add(service_event)
    session.flush()
    return service_event


def _usage(session, tenant: Tenant, vehicle: Vehicle, recorded_at: datetime, **values: object) -> UsageSnapshot:
    snapshot = UsageSnapshot(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        recorded_at=recorded_at,
        odometer_km=values.get("odometer_km"),
        engine_hours=values.get("engine_hours"),
        fuel_level_pct=values.get("fuel_level_pct"),
        payload=values.get("payload"),
        data_source="builder-test",
    )
    session.add(snapshot)
    session.flush()
    return snapshot


def _finding(
    session,
    tenant: Tenant,
    *,
    event: ServiceEvent,
    decision: FindingReviewDecision | None,
    title: str = "Original title",
    modified_title: str | None = None,
    modified_description: str | None = None,
    modified_component: str | None = None,
    modified_location: str | None = None,
    analysis_run: AnalysisRun | None = None,
):
    if analysis_run is None:
        analysis_run = AnalysisRun(
            tenant_id=tenant.id,
            service_event_id=event.id,
            status="SUCCEEDED",
            requested_at=datetime.now(timezone.utc),
        )
        session.add(analysis_run)
        session.flush()
    finding = add_finding(
        session,
        tenant_id=tenant.id,
        service_event_id=event.id,
        analysis_run_id=analysis_run.id,
        finding_code=f"F-{uuid.uuid4().hex[:8]}",
        title=title,
        description="Original description",
        component="Original component",
        location="Original location",
        confidence=0.91,
        evidence_sufficiency=EvidenceSufficiency.SUFFICIENT,
    )
    session.flush()
    if decision is not None:
        create_finding_review(
            session,
            tenant_id=tenant.id,
            finding_id=finding.id,
            decision=decision,
            reviewed_at=datetime.now(timezone.utc),
            reason_code="TEST",
            comment=None,
            modified_title=modified_title,
            modified_description=modified_description,
            modified_component=modified_component,
            modified_location=modified_location,
        )
        finding.review_status = FindingReviewStatus(decision.value)
        session.flush()
    return finding, analysis_run


def _build(session, tenant: Tenant, vehicle: Vehicle, **kwargs):
    limit = kwargs.pop("service_history_limit", 20)
    return PredictionInputBuilder(service_history_limit=limit).build(
        session,
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        **kwargs,
    )


def test_vehicle_level_build_and_history_work_without_event(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        event = _event(session, tenant, vehicle)
        context = _build(session, tenant, vehicle)
        assert context.vehicle.vehicle_id == vehicle.id
        assert [item.service_event_id for item in context.service_history] == [event.id]
    finally:
        session.close()


def test_vehicle_and_service_event_access_are_tenant_safe(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        with pytest.raises(LookupError, match="^Vehicle not found$"):
            PredictionInputBuilder().build(session, tenant_id=uuid.uuid4(), vehicle_id=vehicle.id)
        with pytest.raises(LookupError, match="^Service event not found$"):
            _build(session, tenant, vehicle, service_event_id=uuid.uuid4())
    finally:
        session.close()


def test_analysis_run_requires_matching_event_and_is_tenant_safe(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        event = _event(session, tenant, vehicle)
        analysis_run = AnalysisRun(
            tenant_id=tenant.id,
            service_event_id=event.id,
            status="SUCCEEDED",
            requested_at=datetime.now(timezone.utc),
        )
        session.add(analysis_run)
        session.flush()
        with pytest.raises(ValueError, match="service_event_id is required"):
            _build(session, tenant, vehicle, analysis_run_id=analysis_run.id)
        with pytest.raises(LookupError, match="^Analysis run not found$"):
            _build(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=uuid.uuid4())
        assert _build(
            session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id
        ).analysis_run_id == analysis_run.id
    finally:
        session.close()


def test_latest_usage_is_deterministic_and_numeric_values_are_preserved(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        recorded_at = datetime(2026, 2, 1, tzinfo=timezone.utc)
        older = _usage(session, tenant, vehicle, recorded_at, odometer_km=100)
        latest = _usage(session, tenant, vehicle, recorded_at + timedelta(days=1), engine_hours=Decimal("12.50"))
        context = _build(session, tenant, vehicle)
        assert context.usage is not None
        assert context.usage.usage_snapshot_id == latest.id
        assert context.usage.engine_hours == Decimal("12.50")
        assert context.usage.usage_snapshot_id != older.id
    finally:
        session.close()


@pytest.mark.parametrize(
    ("values", "quality"),
    [
        ({"odometer_km": 100}, PredictionInputQuality.COMPLETE),
        ({"engine_hours": Decimal("3.25")}, PredictionInputQuality.COMPLETE),
        ({"fuel_level_pct": 80, "payload": {"only": "fuel"}}, PredictionInputQuality.INSUFFICIENT),
    ],
)
def test_usage_semantics_drive_quality(seeded_context, values, quality) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        _usage(session, tenant, vehicle, datetime.now(timezone.utc), **values)
        context = _build(session, tenant, vehicle)
        assert context.quality.quality == quality
        if quality == PredictionInputQuality.COMPLETE:
            assert "USAGE" not in context.quality.missing_sources
        else:
            assert "USAGE" in context.quality.missing_sources
    finally:
        session.close()


def test_service_history_limit_and_ordering_are_sql_level(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        newest_open = _event(session, tenant, vehicle, days_ago=1, opened=True)
        null_opened = _event(session, tenant, vehicle, days_ago=0, opened=False)
        older_open = _event(session, tenant, vehicle, days_ago=3, opened=True)
        context = _build(session, tenant, vehicle, service_history_limit=2)
        assert [item.service_event_id for item in context.service_history] == [newest_open.id, older_open.id]
        assert null_opened.id not in [item.service_event_id for item in context.service_history]
    finally:
        session.close()


def test_finding_eligibility_effective_fields_reviews_and_evidence(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        event = _event(session, tenant, vehicle)
        pending, _ = _finding(session, tenant, event=event, decision=None)
        rejected, _ = _finding(session, tenant, event=event, decision=FindingReviewDecision.REJECTED)
        confirmed, _ = _finding(session, tenant, event=event, decision=FindingReviewDecision.CONFIRMED)
        modified, analysis_run = _finding(
            session, tenant, event=event, decision=FindingReviewDecision.MODIFIED,
            modified_title="Modified title", modified_component=None,
        )
        evidence = Evidence(
            tenant_id=tenant.id, service_event_id=event.id, evidence_type=EvidenceType.IMAGE,
            capture_source=CaptureSource.UPLOAD, status=EvidenceStatus.READY,
        )
        session.add(evidence)
        session.flush()
        add_finding_evidence(session, tenant_id=tenant.id, finding_id=modified.id, evidence_id=evidence.id)
        session.flush()
        context = _build(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
        ids = {finding.finding_id for finding in context.findings}
        assert pending.id not in ids and rejected.id not in ids and confirmed.id not in ids
        modified_context = next(finding for finding in context.findings if finding.finding_id == modified.id)
        assert modified_context.effective_title == "Modified title"
        assert modified_context.effective_component == "Original component"
        assert modified_context.supporting_evidence_ids == (evidence.id,)

        vehicle_context = _build(session, tenant, vehicle)
        confirmed_context = next(item for item in vehicle_context.findings if item.finding_id == confirmed.id)
        assert confirmed_context.effective_title == confirmed_context.original_title
        assert confirmed_context.effective_description == confirmed_context.original_description
    finally:
        session.close()


def test_latest_append_only_review_wins_and_inconsistent_provenance_fails(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        event = _event(session, tenant, vehicle)
        finding, analysis_run = _finding(
            session, tenant, event=event, decision=FindingReviewDecision.MODIFIED, modified_title="First title"
        )
        create_finding_review(
            session, tenant_id=tenant.id, finding_id=finding.id, decision=FindingReviewDecision.MODIFIED,
            reviewed_at=datetime.now(timezone.utc) + timedelta(minutes=1), reason_code="TEST-2", comment=None,
            modified_title="Latest title", modified_description=None, modified_component=None, modified_location=None,
        )
        session.flush()
        context = _build(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
        assert context.findings[0].effective_title == "Latest title"
        finding.review_status = FindingReviewStatus.CONFIRMED
        session.flush()
        with pytest.raises(ValueError, match="Finding review provenance is inconsistent"):
            _build(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
    finally:
        session.close()


def test_missing_review_provenance_fails_for_confirmed_or_modified_status(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        event = _event(session, tenant, vehicle)
        finding, analysis_run = _finding(session, tenant, event=event, decision=None)
        finding.review_status = FindingReviewStatus.CONFIRMED
        session.flush()
        with pytest.raises(ValueError, match="Finding review provenance is inconsistent"):
            _build(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id)
    finally:
        session.close()


def test_vehicle_event_and_analysis_scopes_narrow_eligible_findings(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        first_event = _event(session, tenant, vehicle, days_ago=1)
        second_event = _event(session, tenant, vehicle, days_ago=2)
        first_finding, first_run = _finding(session, tenant, event=first_event, decision=FindingReviewDecision.CONFIRMED)
        second_finding, _ = _finding(session, tenant, event=second_event, decision=FindingReviewDecision.CONFIRMED)
        vehicle_context = _build(session, tenant, vehicle)
        event_context = _build(session, tenant, vehicle, service_event_id=first_event.id)
        run_context = _build(session, tenant, vehicle, service_event_id=first_event.id, analysis_run_id=first_run.id)
        assert {item.finding_id for item in vehicle_context.findings} == {first_finding.id, second_finding.id}
        assert {item.finding_id for item in event_context.findings} == {first_finding.id}
        assert {item.finding_id for item in run_context.findings} == {first_finding.id}
    finally:
        session.close()


def test_quality_codes_external_context_time_schema_and_read_only(seeded_context) -> None:
    tenant, vehicle = seeded_context
    session = SessionLocal()
    try:
        orphan_vehicle = Vehicle(
            tenant_id=tenant.id,
            vehicle_class=vehicle.vehicle_class,
            powertrain=vehicle.powertrain,
            model_name="No Input Vehicle",
            year=2026,
            color="White",
        )
        session.add(orphan_vehicle)
        session.flush()
        before = {
            model: session.execute(select(func.count()).select_from(model)).scalar_one()
            for model in (PredictionRun, PredictionAssessment, PredictionFactor)
        }
        generated_at = datetime(2026, 8, 15, 12, 30, tzinfo=timezone.utc)
        context = _build(session, tenant, orphan_vehicle, generated_at=generated_at)
        after = {
            model: session.execute(select(func.count()).select_from(model)).scalar_one()
            for model in (PredictionRun, PredictionAssessment, PredictionFactor)
        }
        assert context.generated_at == generated_at
        assert context.schema_version == "s3.1"
        assert context.external_context.model_dump() == {
            "climate": None, "road_condition": None, "driving_behavior": None,
            "load_profile": None, "additional_context": {},
        }
        assert context.quality.missing_sources == ("USAGE", "SERVICE_HISTORY", "FINDINGS")
        assert context.quality.warnings == ()
        assert before == after
    finally:
        session.close()


def test_invalid_service_history_limit_is_rejected() -> None:
    with pytest.raises(ValueError, match="service_history_limit must be greater than zero"):
        PredictionInputBuilder(service_history_limit=0)
