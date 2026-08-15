from __future__ import annotations

from uuid import UUID

from sqlalchemy import exists, select
from sqlalchemy.orm import Session, selectinload

from app.prediction.models import PredictionAssessment, PredictionFactor, PredictionRun
from app.prediction.provider_schemas import (
    PredictionAssessmentCandidate,
    PredictionFactorCandidate,
    PredictionResponse,
)
from app.prediction.worker_schemas import PredictionWorkItem


def get_prediction_run_for_scope(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_run_id: UUID,
    vehicle_id: UUID | None = None,
    for_update: bool = False,
) -> PredictionRun | None:
    statement = select(PredictionRun).where(
        PredictionRun.tenant_id == tenant_id,
        PredictionRun.id == prediction_run_id,
    )
    if vehicle_id is not None:
        statement = statement.where(PredictionRun.vehicle_id == vehicle_id)
    if for_update:
        statement = statement.with_for_update()
    return session.execute(statement).scalar_one_or_none()


def get_prediction_run_for_work_item(
    session: Session,
    *,
    work_item: PredictionWorkItem,
    for_update: bool = False,
) -> PredictionRun | None:
    statement = select(PredictionRun).where(
        PredictionRun.tenant_id == work_item.tenantId,
        PredictionRun.id == work_item.predictionRunId,
        PredictionRun.vehicle_id == work_item.vehicleId,
    )
    if work_item.serviceEventId is not None:
        statement = statement.where(PredictionRun.service_event_id == work_item.serviceEventId)
    if work_item.analysisRunId is not None:
        statement = statement.where(PredictionRun.analysis_run_id == work_item.analysisRunId)
    if for_update:
        statement = statement.with_for_update()
    return session.execute(statement).scalar_one_or_none()


def add_prediction_assessment(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_run_id: UUID,
    candidate: PredictionAssessmentCandidate,
) -> PredictionAssessment:
    rule_code = candidate.metadata.get("rule_code")
    rule_version = candidate.metadata.get("rule_version")
    assessment = PredictionAssessment(
        tenant_id=tenant_id,
        prediction_run_id=prediction_run_id,
        prediction_type=candidate.prediction_type.strip(),
        system_code=candidate.prediction_code.strip(),
        component_code=None,
        predicted_condition=candidate.description.strip(),
        severity=candidate.severity,
        urgency=candidate.urgency,
        confidence=candidate.confidence,
        horizon_type=candidate.horizon_type,
        horizon_distance=candidate.horizon_distance_km,
        horizon_distance_unit="KM" if candidate.horizon_distance_km is not None else None,
        horizon_days=candidate.horizon_time_days,
        recommended_action=candidate.recommended_action,
        explanation=candidate.description.strip(),
        rule_code=rule_code if isinstance(rule_code, str) and rule_code.strip() else None,
        rule_version=rule_version if isinstance(rule_version, str) and rule_version.strip() else None,
    )
    session.add(assessment)
    session.flush()
    return assessment


def add_prediction_factor(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_assessment_id: UUID,
    candidate: PredictionFactorCandidate,
) -> PredictionFactor:
    factor = PredictionFactor(
        tenant_id=tenant_id,
        prediction_assessment_id=prediction_assessment_id,
        factor_type=candidate.factor_type,
        source_entity_type=candidate.source_entity_type,
        source_entity_id=candidate.source_entity_id,
        factor_code=candidate.factor_code.strip(),
        description=candidate.description,
        value_numeric=candidate.value_numeric,
        value_text=candidate.value_text,
        unit=candidate.unit,
        weight=candidate.weight,
        metadata_json=candidate.metadata or {},
    )
    session.add(factor)
    session.flush()
    return factor


def persist_prediction_response(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_run_id: UUID,
    response: PredictionResponse,
) -> list[PredictionAssessment]:
    assessments: list[PredictionAssessment] = []
    for candidate in response.assessments:
        assessment = add_prediction_assessment(
            session,
            tenant_id=tenant_id,
            prediction_run_id=prediction_run_id,
            candidate=candidate,
        )
        for factor_candidate in candidate.factors:
            add_prediction_factor(
                session,
                tenant_id=tenant_id,
                prediction_assessment_id=assessment.id,
                candidate=factor_candidate,
            )
        assessments.append(assessment)
    return assessments


def has_prediction_assessments(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_run_id: UUID,
) -> bool:
    statement = select(
        exists().where(
            PredictionAssessment.tenant_id == tenant_id,
            PredictionAssessment.prediction_run_id == prediction_run_id,
        )
    )
    return bool(session.execute(statement).scalar_one())


def list_prediction_assessments_for_run(
    session: Session,
    *,
    tenant_id: UUID,
    prediction_run_id: UUID,
) -> list[PredictionAssessment]:
    return session.execute(
        select(PredictionAssessment)
        .options(selectinload(PredictionAssessment.factors))
        .where(
            PredictionAssessment.tenant_id == tenant_id,
            PredictionAssessment.prediction_run_id == prediction_run_id,
        )
        .order_by(PredictionAssessment.created_at.asc(), PredictionAssessment.id.asc())
    ).scalars().all()
