from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.evidence.models import AnalysisRun, Finding
from app.service_intake.models import ServiceEvent
from app.vehicle.models import UsageSnapshot, Vehicle

DEFAULT_SERVICE_HISTORY_LIMIT = 20


def get_prediction_vehicle_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    vehicle_id: uuid.UUID,
) -> Vehicle | None:
    return session.execute(
        select(Vehicle).where(
            Vehicle.tenant_id == tenant_id,
            Vehicle.id == vehicle_id,
        )
    ).scalar_one_or_none()


def list_prediction_service_history(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    vehicle_id: uuid.UUID,
    limit: int = DEFAULT_SERVICE_HISTORY_LIMIT,
) -> list[ServiceEvent]:
    if limit <= 0:
        return []

    return session.execute(
        select(ServiceEvent)
        .where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.vehicle_id == vehicle_id,
        )
        .order_by(
            ServiceEvent.opened_at.desc().nullslast(),
            ServiceEvent.created_at.desc(),
            ServiceEvent.id.desc(),
        )
        .limit(limit)
    ).scalars().all()


def get_latest_usage_snapshot_for_vehicle(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    vehicle_id: uuid.UUID,
) -> UsageSnapshot | None:
    return session.execute(
        select(UsageSnapshot)
        .where(
            UsageSnapshot.tenant_id == tenant_id,
            UsageSnapshot.vehicle_id == vehicle_id,
        )
        .order_by(UsageSnapshot.recorded_at.desc(), UsageSnapshot.id.desc())
    ).scalars().first()


def get_prediction_analysis_run_for_tenant_event(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    analysis_run_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> AnalysisRun | None:
    return session.execute(
        select(AnalysisRun).where(
            AnalysisRun.tenant_id == tenant_id,
            AnalysisRun.id == analysis_run_id,
            AnalysisRun.service_event_id == service_event_id,
        )
    ).scalar_one_or_none()


def list_prediction_findings_for_tenant_vehicle(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    vehicle_id: uuid.UUID,
    service_event_id: uuid.UUID | None = None,
    analysis_run_id: uuid.UUID | None = None,
    limit: int | None = None,
) -> list[Finding]:
    statement = (
        select(Finding)
        .join(ServiceEvent, ServiceEvent.id == Finding.service_event_id)
        .where(
            Finding.tenant_id == tenant_id,
            ServiceEvent.vehicle_id == vehicle_id,
            ServiceEvent.tenant_id == tenant_id,
        )
    )

    if service_event_id is not None:
        statement = statement.where(Finding.service_event_id == service_event_id)
    if analysis_run_id is not None:
        statement = statement.where(Finding.analysis_run_id == analysis_run_id)

    statement = statement.order_by(Finding.created_at.desc(), Finding.id.desc())
    if limit is not None:
        statement = statement.limit(limit)

    return session.execute(statement).scalars().all()
