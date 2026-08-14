from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.evidence.models import AnalysisRun, AnalysisRunStatus, Evidence, EvidenceStatus
from app.service_intake.models import ServiceEvent


ACTIVE_ANALYSIS_STATUSES = (AnalysisRunStatus.QUEUED, AnalysisRunStatus.PROCESSING)


def get_service_event_for_analysis_update(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> ServiceEvent | None:
    return session.execute(
        select(ServiceEvent)
        .where(ServiceEvent.tenant_id == tenant_id, ServiceEvent.id == service_event_id)
        .with_for_update()
    ).scalar_one_or_none()


def list_ready_evidence_for_analysis(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> list[Evidence]:
    return session.execute(
        select(Evidence)
        .where(
            Evidence.tenant_id == tenant_id,
            Evidence.service_event_id == service_event_id,
            Evidence.status == EvidenceStatus.READY,
        )
        .order_by(Evidence.id.asc())
    ).scalars().all()


def get_active_analysis_run(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> AnalysisRun | None:
    return session.execute(
        select(AnalysisRun)
        .where(
            AnalysisRun.tenant_id == tenant_id,
            AnalysisRun.service_event_id == service_event_id,
            AnalysisRun.status.in_(ACTIVE_ANALYSIS_STATUSES),
        )
        .order_by(AnalysisRun.requested_at.desc(), AnalysisRun.id.desc())
        .limit(1)
    ).scalar_one_or_none()


def create_analysis_run(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    input_evidence_count: int,
) -> AnalysisRun:
    now = datetime.now(timezone.utc)
    analysis_run = AnalysisRun(
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        status=AnalysisRunStatus.QUEUED,
        requested_at=now,
        input_evidence_count=input_evidence_count,
        created_at=now,
        updated_at=now,
    )
    session.add(analysis_run)
    session.flush()
    return analysis_run


def get_analysis_run_for_tenant_event(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    analysis_run_id: uuid.UUID,
    for_update: bool = False,
) -> AnalysisRun | None:
    statement = select(AnalysisRun).where(
        AnalysisRun.tenant_id == tenant_id,
        AnalysisRun.service_event_id == service_event_id,
        AnalysisRun.id == analysis_run_id,
    )
    if for_update:
        statement = statement.with_for_update()
    return session.execute(statement).scalar_one_or_none()
