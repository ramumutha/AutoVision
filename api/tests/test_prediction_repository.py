from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from sqlalchemy import select

from app.core.database import SessionLocal
from app.core.models import Tenant
from app.evidence.models import AnalysisRun, AnalysisRunStatus
from app.prediction.models import (
    PredictionAssessment,
    PredictionFactor,
    PredictionHorizonType,
    PredictionRun,
    PredictionRunStatus,
    PredictionSeverity,
    PredictionUrgency,
)
from app.prediction.provider_schemas import (
    PredictionAssessmentCandidate,
    PredictionFactorCandidate,
    PredictionResponse,
)
from app.prediction.repository import (
    add_prediction_assessment,
    get_prediction_run_for_scope,
    get_prediction_run_for_work_item,
    has_prediction_assessments,
    list_prediction_assessments_for_run,
    persist_prediction_response,
)
from app.prediction.worker_schemas import PredictionWorkItem
from app.service_intake.models import ServiceEvent, ServiceEventState
from app.vehicle.models import Vehicle
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


def _run(session, tenant: Tenant, vehicle: Vehicle, *, service_event_id=None, analysis_run_id=None) -> PredictionRun:
    run = PredictionRun(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        service_event_id=service_event_id,
        analysis_run_id=analysis_run_id,
        status=PredictionRunStatus.QUEUED,
        requested_at=datetime.now(timezone.utc),
        provider_name="autovision",
        schema_version="s3.3",
        input_snapshot={"immutable": True},
    )
    session.add(run)
    session.flush()
    return run


def _scoped_run(session, tenant: Tenant, vehicle: Vehicle) -> tuple[PredictionRun, ServiceEvent, AnalysisRun]:
    event = ServiceEvent(
        tenant_id=tenant.id,
        vehicle_id=vehicle.id,
        source="prediction-repository-test",
        state=ServiceEventState.OPEN,
        revision=1,
        opened_at=datetime.now(timezone.utc),
    )
    session.add(event)
    session.flush()
    analysis_run = AnalysisRun(
        tenant_id=tenant.id,
        service_event_id=event.id,
        status=AnalysisRunStatus.SUCCEEDED,
        requested_at=datetime.now(timezone.utc),
    )
    session.add(analysis_run)
    session.flush()
    return _run(session, tenant, vehicle, service_event_id=event.id, analysis_run_id=analysis_run.id), event, analysis_run


def _factor(*, source_type="VEHICLE", source_id=None, code="POWERTRAIN_TYPE", value=Decimal("12.50")):
    return PredictionFactorCandidate(
        factor_type="VEHICLE",
        factor_code=code,
        label="Powertrain type",
        description="Persisted factor",
        value_numeric=value,
        value_text="ICE",
        unit="KM",
        weight=Decimal("0.25"),
        source_entity_type=source_type,
        source_entity_id=source_id or uuid4(),
        metadata={"source": "test"},
    )


def _candidate(code="PREDICTED_CONDITION", *, factors=None):
    return PredictionAssessmentCandidate(
        prediction_type="CONDITION_ATTENTION",
        prediction_code=code,
        title="Ignored persistence title",
        description="  Persisted prediction description  ",
        severity=PredictionSeverity.INFO,
        urgency=PredictionUrgency.ROUTINE,
        confidence=Decimal("0.8750"),
        horizon_type=PredictionHorizonType.DISTANCE,
        horizon_distance_km=Decimal("125.50"),
        recommended_action="Review during service planning",
        factors=tuple(factors or (_factor(),)),
        metadata={"rule_code": "TEST_RULE", "rule_version": "1"},
    )


def _response(*candidates: PredictionAssessmentCandidate) -> PredictionResponse:
    return PredictionResponse(
        schema_version="s3.3",
        provider="autovision",
        model="deterministic-rules",
        model_version="1",
        assessments=tuple(candidates),
        usage={"rules_evaluated": 1, "rules_matched": 1},
    )


def test_run_lookup_is_tenant_and_vehicle_scoped(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
        assert get_prediction_run_for_scope(
            session, tenant_id=tenant.id, prediction_run_id=run.id
        ) is run
        assert get_prediction_run_for_scope(
            session, tenant_id=tenant.id, prediction_run_id=run.id, vehicle_id=vehicle.id
        ) is run
        assert get_prediction_run_for_scope(
            session, tenant_id=tenant.id, prediction_run_id=run.id, vehicle_id=uuid4()
        ) is None
        assert get_prediction_run_for_scope(
            session, tenant_id=uuid4(), prediction_run_id=run.id
        ) is None
    finally:
        session.close()


def test_for_update_lookup_and_work_item_scopes(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run, event, analysis_run = _scoped_run(session, tenant, vehicle)
        session.begin_nested()
        assert get_prediction_run_for_scope(
            session, tenant_id=tenant.id, prediction_run_id=run.id, for_update=True
        ) is run
        item = PredictionWorkItem(
            predictionRunId=run.id,
            tenantId=tenant.id,
            vehicleId=vehicle.id,
            serviceEventId=event.id,
            analysisRunId=analysis_run.id,
        )
        assert get_prediction_run_for_work_item(session, work_item=item, for_update=True) is run
        assert get_prediction_run_for_work_item(
            session,
            work_item=item.model_copy(update={"tenantId": uuid4()}),
        ) is None
        assert get_prediction_run_for_work_item(
            session,
            work_item=item.model_copy(update={"vehicleId": uuid4()}),
        ) is None
        assert get_prediction_run_for_work_item(
            session,
            work_item=item.model_copy(update={"serviceEventId": uuid4()}),
        ) is None
        assert get_prediction_run_for_work_item(
            session,
            work_item=item.model_copy(update={"analysisRunId": uuid4()}),
        ) is None
    finally:
        session.close()


def test_none_optional_work_item_scopes_do_not_force_null_columns(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run, _, _ = _scoped_run(session, tenant, vehicle)
        item = PredictionWorkItem(predictionRunId=run.id, tenantId=tenant.id, vehicleId=vehicle.id)
        assert get_prediction_run_for_work_item(session, work_item=item) is run
    finally:
        session.close()


def test_assessment_mapping_preserves_core_fields_decimal_and_provenance(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
        assessment = add_prediction_assessment(
            session,
            tenant_id=tenant.id,
            prediction_run_id=run.id,
            candidate=_candidate(),
        )
        assert assessment.prediction_type == "CONDITION_ATTENTION"
        assert assessment.system_code == "PREDICTED_CONDITION"
        assert assessment.component_code is None
        assert assessment.predicted_condition == "Persisted prediction description"
        assert assessment.explanation == "Persisted prediction description"
        assert assessment.confidence == Decimal("0.8750")
        assert assessment.horizon_distance == Decimal("125.50")
        assert assessment.horizon_distance_unit == "KM"
        assert assessment.horizon_days is None
        assert assessment.rule_code == "TEST_RULE"
        assert assessment.rule_version == "1"
        assert assessment.prediction_run_id == run.id
        assert run.status == PredictionRunStatus.QUEUED
        assert run.provider_name == "autovision"
        assert run.input_snapshot == {"immutable": True}
    finally:
        session.close()


def test_time_horizon_mapping_and_factor_persistence(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
        candidate = _candidate()
        candidate = candidate.model_copy(
            update={
                "horizon_type": PredictionHorizonType.TIME,
                "horizon_distance_km": None,
                "horizon_time_days": 30,
            }
        )
        assessment = add_prediction_assessment(
            session, tenant_id=tenant.id, prediction_run_id=run.id, candidate=candidate
        )
        factor = _factor(source_id=vehicle.id)
        from app.prediction.repository import add_prediction_factor

        persisted_factor = add_prediction_factor(
            session,
            tenant_id=tenant.id,
            prediction_assessment_id=assessment.id,
            candidate=factor,
        )
        assert assessment.horizon_days == 30
        assert assessment.horizon_distance_unit is None
        assert persisted_factor.factor_type.value == "VEHICLE"
        assert persisted_factor.source_entity_type == "VEHICLE"
        assert persisted_factor.source_entity_id == vehicle.id
        assert persisted_factor.factor_code == "POWERTRAIN_TYPE"
        assert persisted_factor.value_numeric == Decimal("12.50")
        assert persisted_factor.weight == Decimal("0.25")
        assert persisted_factor.metadata_json == {"source": "test"}
    finally:
        session.close()


def test_batch_order_factor_order_and_existence_listing_are_tenant_safe(prediction_context) -> None:
    tenant = prediction_context["tenant"]
    vehicle = prediction_context["vehicle"]
    session = SessionLocal()
    try:
        run = _run(session, tenant, vehicle)
        assert has_prediction_assessments(session, tenant_id=tenant.id, prediction_run_id=run.id) is False
        first = _candidate("FIRST", factors=(_factor(code="FIRST_A"), _factor(code="FIRST_B")))
        second = _candidate("SECOND", factors=(_factor(code="SECOND_A"),))
        persisted = persist_prediction_response(
            session,
            tenant_id=tenant.id,
            prediction_run_id=run.id,
            response=_response(first, second),
        )
        assert [item.system_code for item in persisted] == ["FIRST", "SECOND"]
        assert [item.factor_code for item in persisted[0].factors] == ["FIRST_A", "FIRST_B"]
        assert has_prediction_assessments(session, tenant_id=tenant.id, prediction_run_id=run.id) is True
        listed = list_prediction_assessments_for_run(
            session, tenant_id=tenant.id, prediction_run_id=run.id
        )
        expected_listing = sorted(persisted, key=lambda item: (item.created_at, item.id))
        assert {item.id for item in listed} == {item.id for item in persisted}
        assert [item.id for item in listed] == [item.id for item in expected_listing]
        assert [item.system_code for item in listed] == [item.system_code for item in expected_listing]
        assert sum(len(item.factors) for item in listed) == 3
        assert list_prediction_assessments_for_run(
            session, tenant_id=uuid4(), prediction_run_id=run.id
        ) == []
        assert has_prediction_assessments(session, tenant_id=uuid4(), prediction_run_id=run.id) is False
        assert session.in_transaction()
    finally:
        session.close()
