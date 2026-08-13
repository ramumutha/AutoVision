from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.service_intake.models import Complaint, ServiceEvent, ServiceEventState
from app.vehicle.models import Vehicle


def get_vehicle_for_tenant(session: Session, tenant_id: uuid.UUID, vehicle_id: uuid.UUID) -> Vehicle | None:
    return session.execute(
        select(Vehicle).where(
            Vehicle.tenant_id == tenant_id,
            Vehicle.id == vehicle_id,
        )
    ).scalar_one_or_none()


def get_service_event_for_tenant(session: Session, tenant_id: uuid.UUID, event_id: uuid.UUID) -> ServiceEvent | None:
    return session.execute(
        select(ServiceEvent)
        .options(selectinload(ServiceEvent.complaints))
        .where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()


def create_service_event_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    vehicle_id: uuid.UUID,
    source: str,
    original_complaint: str,
    structured_summary: str | None = None,
    language: str | None = None,
    captured_by: str | None = None,
) -> ServiceEvent:
    event = ServiceEvent(
        tenant_id=tenant_id,
        vehicle_id=vehicle_id,
        source=source,
        state=ServiceEventState.DRAFT,
        revision=1,
        opened_at=None,
    )
    session.add(event)
    session.flush()

    complaint = Complaint(
        event=event,
        original_text=original_complaint,
        structured_summary=structured_summary,
        language=language,
        captured_by=captured_by,
        captured_at=datetime.now(timezone.utc),
        revision=1,
    )
    session.add(complaint)
    session.flush()
    return event
