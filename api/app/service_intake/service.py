from __future__ import annotations

import uuid

from sqlalchemy.orm import Session

from app.service_intake.models import Complaint, ServiceEvent, ServiceEventAssignment, ServiceEventContext
from app.service_intake.repository import (
    create_service_event_assignment_for_tenant,
    create_service_event_context_for_tenant,
    create_service_event_for_tenant,
    get_latest_complaint_for_event,
    get_service_event_for_tenant,
    get_vehicle_for_tenant,
    list_service_events_for_tenant,
    open_service_event_for_tenant,
    update_complaint_for_tenant,
)


def _serialize_complaint(complaint: Complaint) -> dict:
    return {
        "id": complaint.id,
        "originalText": complaint.original_text,
        "structuredSummary": complaint.structured_summary,
        "language": complaint.language,
        "capturedBy": complaint.captured_by,
        "capturedAt": complaint.captured_at,
        "revision": complaint.revision,
    }


def create_service_event_for_scope(
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
    vehicle = get_vehicle_for_tenant(session, tenant_id, vehicle_id)
    if vehicle is None:
        raise LookupError("Vehicle not found")

    return create_service_event_for_tenant(
        session,
        tenant_id=tenant_id,
        vehicle_id=vehicle_id,
        source=source,
        original_complaint=original_complaint,
        structured_summary=structured_summary,
        language=language,
        captured_by=captured_by,
    )


def get_service_event_for_scope(session: Session, tenant_id: uuid.UUID, event_id: uuid.UUID) -> ServiceEvent | None:
    return get_service_event_for_tenant(session, tenant_id, event_id)


def list_service_events_for_scope(session: Session, tenant_id: uuid.UUID, vehicle_id: uuid.UUID) -> list[ServiceEvent]:
    return list_service_events_for_tenant(session, tenant_id, vehicle_id)


def update_complaint_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    original_complaint: str | None = None,
    structured_summary: str | None = None,
    language: str | None = None,
    captured_by: str | None = None,
) -> ServiceEvent:
    return update_complaint_for_tenant(
        session,
        tenant_id=tenant_id,
        event_id=event_id,
        original_complaint=original_complaint,
        structured_summary=structured_summary,
        language=language,
        captured_by=captured_by,
    )


def open_service_event_for_scope(session: Session, *, tenant_id: uuid.UUID, event_id: uuid.UUID) -> ServiceEvent:
    return open_service_event_for_tenant(
        session,
        tenant_id=tenant_id,
        event_id=event_id,
    )


def create_service_event_assignment_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    role_code: str,
    user_ref_id: uuid.UUID | None = None,
) -> ServiceEventAssignment:
    return create_service_event_assignment_for_tenant(
        session,
        tenant_id=tenant_id,
        event_id=event_id,
        role_code=role_code,
        user_ref_id=user_ref_id,
    )


def create_service_event_context_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    event_id: uuid.UUID,
    context_type: str,
    source_ref: str | None = None,
    snapshot_json: dict | None = None,
) -> ServiceEventContext:
    return create_service_event_context_for_tenant(
        session,
        tenant_id=tenant_id,
        event_id=event_id,
        context_type=context_type,
        source_ref=source_ref,
        snapshot_json=snapshot_json,
    )


def _serialize_assignment(assignment: ServiceEventAssignment) -> dict:
    return {
        "id": assignment.id,
        "eventId": assignment.event_id,
        "roleCode": assignment.role_code,
        "userRef": str(assignment.user_ref_id) if assignment.user_ref_id else None,
        "assignedAt": assignment.assigned_at,
    }


def _serialize_context(context: ServiceEventContext) -> dict:
    return {
        "id": context.id,
        "eventId": context.event_id,
        "contextType": context.context_type,
        "sourceRef": context.source_ref,
        "snapshotJson": context.snapshot_json,
        "capturedAt": context.captured_at,
    }


def serialize_service_event(event: ServiceEvent) -> dict:
    complaint = max(event.complaints, key=lambda item: item.revision) if event.complaints else None
    assignments = sorted(event.assignments, key=lambda item: item.assigned_at) if event.assignments else []
    contexts = sorted(event.contexts, key=lambda item: item.captured_at) if event.contexts else []
    return {
        "id": event.id,
        "vehicleId": event.vehicle_id,
        "source": event.source,
        "state": event.state.value,
        "revision": event.revision,
        "openedAt": event.opened_at,
        "createdAt": event.created_at,
        "updatedAt": event.updated_at,
        "complaint": _serialize_complaint(complaint) if complaint else None,
        "assignments": [_serialize_assignment(item) for item in assignments],
        "contexts": [_serialize_context(item) for item in contexts],
    }
