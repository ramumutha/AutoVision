from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.identity.models import UserRef
from app.service_intake.models import (
    Complaint,
    ServiceEvent,
    ServiceEventAssignment,
    ServiceEventContext,
    ServiceEventState,
)
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
        .options(
            selectinload(ServiceEvent.complaints),
            selectinload(ServiceEvent.assignments),
            selectinload(ServiceEvent.contexts),
        )
        .where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()


def get_latest_complaint_for_event(session: Session, event_id: uuid.UUID) -> Complaint | None:
    return session.execute(
        select(Complaint)
        .where(Complaint.event_id == event_id)
        .order_by(Complaint.revision.desc())
    ).scalars().first()


def open_service_event_for_tenant(session: Session, *, tenant_id: uuid.UUID, event_id: uuid.UUID) -> ServiceEvent:
    event = session.execute(
        select(ServiceEvent)
        .with_for_update()
        .where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()
    if event is None:
        raise LookupError("Service event not found")

    if event.state == ServiceEventState.OPEN:
        raise ValueError("Service event is already OPEN")
    if event.state != ServiceEventState.DRAFT:
        raise ValueError("Service event is not in DRAFT state")

    latest_complaint = get_latest_complaint_for_event(session, event.id)
    if latest_complaint is None:
        raise ValueError("Service event has no complaint")

    event.state = ServiceEventState.OPEN
    event.opened_at = datetime.now(timezone.utc)
    event.revision += 1
    session.flush()
    return event


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


def create_service_event_assignment_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    role_code: str,
    user_ref_id: uuid.UUID | None = None,
) -> ServiceEventAssignment:
    event = session.execute(
        select(ServiceEvent).where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()
    if event is None:
        raise LookupError("Service event not found")

    if user_ref_id is not None:
        user_ref = session.execute(
            select(UserRef).where(
                UserRef.id == user_ref_id,
                UserRef.tenant_id == tenant_id,
            )
        ).scalar_one_or_none()
        if user_ref is None:
            raise LookupError("User not found")

    assignment = ServiceEventAssignment(
        event_id=event.id,
        tenant_id=tenant_id,
        role_code=role_code,
        user_ref_id=user_ref_id,
        assigned_at=datetime.now(timezone.utc),
    )
    session.add(assignment)
    session.flush()
    return assignment


def create_service_event_context_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    context_type: str,
    source_ref: str | None = None,
    snapshot_json: dict | None = None,
) -> ServiceEventContext:
    event = session.execute(
        select(ServiceEvent).where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()
    if event is None:
        raise LookupError("Service event not found")

    context = ServiceEventContext(
        event_id=event.id,
        context_type=context_type,
        source_ref=source_ref,
        snapshot_json=snapshot_json or {},
        captured_at=datetime.now(timezone.utc),
    )
    session.add(context)
    session.flush()
    return context


def update_complaint_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    original_complaint: str | None = None,
    structured_summary: str | None = None,
    language: str | None = None,
    captured_by: str | None = None,
) -> ServiceEvent:
    event = session.execute(
        select(ServiceEvent).where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == event_id,
        )
    ).scalar_one_or_none()
    if event is None:
        raise LookupError("Service event not found")

    if event.state != ServiceEventState.DRAFT:
        raise ValueError("Service event is not in DRAFT state")

    previous_complaint = get_latest_complaint_for_event(session, event.id)
    if previous_complaint is None:
        raise LookupError("Complaint not found")

    next_original = previous_complaint.original_text if original_complaint is None else original_complaint
    next_structured = previous_complaint.structured_summary if structured_summary is None else structured_summary
    next_language = previous_complaint.language if language is None else language
    next_captured_by = previous_complaint.captured_by if captured_by is None else captured_by

    new_complaint = Complaint(
        event=event,
        original_text=next_original,
        structured_summary=next_structured,
        language=next_language,
        captured_by=next_captured_by,
        captured_at=datetime.now(timezone.utc),
        revision=previous_complaint.revision + 1,
    )
    session.add(new_complaint)
    session.flush()
    return event
