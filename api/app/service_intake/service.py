from __future__ import annotations

import uuid

from sqlalchemy.orm import Session

from app.service_intake.models import Complaint, ServiceEvent
from app.service_intake.repository import (
    create_service_event_for_tenant,
    get_service_event_for_tenant,
    get_vehicle_for_tenant,
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


def serialize_service_event(event: ServiceEvent) -> dict:
    complaint = event.complaints[0] if event.complaints else None
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
    }
