from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal

import pytest
from sqlalchemy import func, select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.prediction import deterministic_provider as deterministic_provider_module
from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_schemas import CanonicalPredictionContext, PredictionInputQualityContext, PredictionVehicleContext
from app.prediction.models import (
    PredictionAssessment,
    PredictionFactor,
    PredictionInputQuality,
    PredictionRun,
    PredictionRunStatus,
)
from app.prediction.provider_schemas import PredictionProviderUsage, PredictionResponse
from app.prediction.repository import persist_prediction_response as repository_persist_prediction_response
from app.prediction.rules import PredictionRuleRegistry
from app.prediction.worker import PredictionWorker
from app.vehicle.models import PowertrainType, Vehicle, VehicleClass
from scripts.seed_demo import reset_demo_data, seed_demo_data


@pytest.fixture()
def prediction_context() -> dict[str, object]:
    reset_demo_data()
    seed_demo_data()
    session = SessionLocal()
    try:
        tenant = session.execute(select(Tenant).where(Tenant.slug == "autovision-demo-org")).scalar_one()
        vehicle = session.execute(
            select(Vehicle).where(Vehicle.tenant_id == tenant.id, Vehicle.model_name == "City Compact")
        ).scalar_one()
        return {"tenant": tenant, "vehicle": vehicle}
    finally:
        session.close()


@pytest.fixture(autouse=True)
def cleanup_database():
    yield
    reset_demo_data()


def _run(session, tenant: Tenant, vehicle: Vehicle, *, quality=PredictionInputQuality.PARTIAL) -> PredictionRun:
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        status=PredictionRunStatus.QUEUED,
        provider_name="original-provider",
        provider_version="original-version",
        engine_name="original-engine",
        engine_version="original-engine-version",
        schema_version="original-schema",
        configuration_version="original-config",
        input_snapshot={"frozen": True},
        input_quality=quality,
        requested_at=datetime(2026, 8, 10, tzinfo=timezone.utc),
        started_at=datetime(2026, 8, 11, tzinfo=timezone.utc),
        error_code="STALE_ERROR",
        error_message="Stale error",
    )
    session.add(run)
    session.flush()
    session.commit()
    session.refresh(run)
    session.expunge(run)
    return run


def _fresh(run_id):
    session = SessionLocal()
    try:
        return session.get(PredictionRun, run_id)
    finally:
        session.close()


def _context(tenant: Tenant, vehicle: Vehicle, quality: PredictionInputQuality) -> CanonicalPredictionContext:
    return CanonicalPredictionContext(
        schema_version="s3.1",
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        generated_at=datetime(2026, 8, 15, tzinfo=timezone.utc),
        vehicle=PredictionVehicleContext(
            vehicle_id=vehicle.id,
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
        ),
        quality=PredictionInputQualityContext(quality=quality),
    )


def _empty_provider() -> DeterministicPredictionProvider:
    return DeterministicPredictionProvider(PredictionRuleRegistry(()))


def test_real_deterministic_provider_persists_results_and_completes(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()

    PredictionWorker().execute(
        __import__("app.prediction.worker_schemas", fromlist=["PredictionWorkItem"]).PredictionWorkItem(
            predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id
        )
    )
    persisted = _fresh(run.id)
    assert persisted.status == PredictionRunStatus.COMPLETED
    assert persisted.completed_at is not None
    assert persisted.started_at is not None
    assert persisted.input_quality == PredictionInputQuality.COMPLETE
    assert persisted.provider_name == "autovision"
    assert persisted.provider_version == "1"
    assert persisted.engine_name == "deterministic-rules"
    assert persisted.engine_version == "1"
    assert persisted.schema_version == "s3.3"
    assert persisted.configuration_version == "deterministic-rules-v1"
    assert persisted.error_code is None
    assert persisted.error_message is None

    session = SessionLocal()
    try:
        assessments = session.execute(
            select(PredictionAssessment).where(PredictionAssessment.prediction_run_id == run.id)
        ).scalars().all()
        factors = session.execute(
            select(PredictionFactor).where(PredictionFactor.tenant_id == tenant.id)
        ).scalars().all()
        assert {assessment.system_code for assessment in assessments} == {
            "USAGE_CONTEXT_AVAILABLE",
            "ICE_POWERTRAIN_CONTEXT",
        }
        assert len(factors) >= 3
        assert any(factor.source_entity_type == "USAGE_SNAPSHOT" for factor in factors)
        assert any(factor.source_entity_type == "VEHICLE" for factor in factors)
        assert all(assessment.rule_code for assessment in assessments)
        assert all(assessment.rule_version == "1" for assessment in assessments)
    finally:
        session.close()


def test_partial_empty_response_partially_completes_without_rows(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    context = _context(tenant, vehicle, PredictionInputQuality.PARTIAL)
    worker = PredictionWorker(
        input_builder=type("Builder", (), {"build": lambda self, session, **kwargs: context})(),
        provider=_empty_provider(),
    )
    from app.prediction.worker_schemas import PredictionWorkItem

    worker.execute(PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id))
    persisted = _fresh(run.id)
    assert persisted.status == PredictionRunStatus.PARTIALLY_COMPLETED
    assert persisted.completed_at is not None
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 0
        assert session.execute(select(func.count()).select_from(PredictionFactor)).scalar_one() == 0
    finally:
        session.close()


def test_complete_empty_response_completes_and_clears_stale_errors(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    context = _context(tenant, vehicle, PredictionInputQuality.COMPLETE)
    builder = type("Builder", (), {"build": lambda self, session, **kwargs: context})()
    from app.prediction.worker_schemas import PredictionWorkItem

    PredictionWorker(input_builder=builder, provider=_empty_provider()).execute(
        PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id)
    )
    persisted = _fresh(run.id)
    assert persisted.status == PredictionRunStatus.COMPLETED
    assert persisted.error_code is None
    assert persisted.error_message is None


def test_persistence_failure_rolls_back_and_marks_run_failed(prediction_context, monkeypatch: pytest.MonkeyPatch) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()

    def persist_then_fail(session, **kwargs):
        repository_persist_prediction_response(session, **kwargs)
        raise RuntimeError("raw database secret")

    monkeypatch.setattr(deterministic_provider_module, "persist_prediction_response", persist_then_fail, raising=False)
    import app.prediction.worker as worker_module

    monkeypatch.setattr(worker_module, "persist_prediction_response", persist_then_fail)
    from app.prediction.worker_schemas import PredictionWorkItem

    PredictionWorker().execute(PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == "PREDICTION_PERSISTENCE_ERROR"
    assert failed.error_message == "Prediction persistence failed"
    assert "raw" not in failed.error_message
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 0
        assert session.execute(select(func.count()).select_from(PredictionFactor)).scalar_one() == 0
    finally:
        session.close()


def test_duplicate_execute_does_not_duplicate_results_or_rerun_provider(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    provider = DeterministicPredictionProvider()
    calls = {"count": 0}
    original_predict = provider.predict

    def counted(request):
        calls["count"] += 1
        return original_predict(request)

    provider.predict = counted
    from app.prediction.worker_schemas import PredictionWorkItem

    item = PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id)
    worker = PredictionWorker(provider=provider)
    worker.execute(item)
    first = _fresh(run.id)
    session = SessionLocal()
    try:
        first_count = session.execute(
            select(func.count()).select_from(PredictionAssessment).where(PredictionAssessment.prediction_run_id == run.id)
        ).scalar_one()
    finally:
        session.close()
    worker.execute(item)
    second = _fresh(run.id)
    assert calls["count"] == 1
    assert second.status == PredictionRunStatus.COMPLETED
    session = SessionLocal()
    try:
        second_count = session.execute(
            select(func.count()).select_from(PredictionAssessment).where(PredictionAssessment.prediction_run_id == run.id)
        ).scalar_one()
        assert second_count == first_count
        assert first.completed_at == second.completed_at
    finally:
        session.close()


def test_insufficient_input_never_persists_results(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
    finally:
        session.close()
    context = _context(tenant, vehicle, PredictionInputQuality.INSUFFICIENT)
    provider = _empty_provider()
    from app.prediction.worker_schemas import PredictionWorkItem

    PredictionWorker(
        input_builder=type("Builder", (), {"build": lambda self, session, **kwargs: context})(),
        provider=provider,
    ).execute(PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id))
    failed = _fresh(run.id)
    assert failed.status == PredictionRunStatus.FAILED
    assert failed.error_code == "INSUFFICIENT_INPUT"
    session = SessionLocal()
    try:
        assert session.execute(select(func.count()).select_from(PredictionAssessment)).scalar_one() == 0
    finally:
        session.close()
