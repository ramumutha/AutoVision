from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy.orm import Session

from app.evidence.models import CaptureSource, Evidence, EvidenceStatus, EvidenceType
from app.evidence.repository import (
    create_evidence_for_tenant,
    get_evidence_for_tenant_service_event,
    get_service_event_for_tenant,
    list_evidence_for_tenant_service_event,
)


def _serialize_evidence(evidence: Evidence) -> dict:
    evidence_type = getattr(evidence.evidence_type, "value", evidence.evidence_type)
    capture_source = getattr(evidence.capture_source, "value", evidence.capture_source)
    status = getattr(evidence.status, "value", evidence.status)
    return {
        "id": evidence.id,
        "tenantId": evidence.tenant_id,
        "serviceEventId": evidence.service_event_id,
        "evidenceType": evidence_type,
        "captureSource": capture_source,
        "title": evidence.title,
        "description": evidence.description,
        "capturedAt": evidence.captured_at,
        "capturedByUserRefId": evidence.captured_by_user_ref_id,
        "status": status,
        "createdAt": evidence.created_at,
        "updatedAt": evidence.updated_at,
    }


def create_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_type: EvidenceType,
    capture_source: CaptureSource,
    title: str | None = None,
    description: str | None = None,
    captured_at: datetime | None = None,
) -> Evidence:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")

    evidence = create_evidence_for_tenant(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_type=evidence_type,
        capture_source=capture_source,
        title=title,
        description=description,
        captured_at=captured_at,
    )
    return evidence


def list_evidence_for_scope(session: Session, *, tenant_id: uuid.UUID, service_event_id: uuid.UUID) -> list[Evidence]:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")
    return list_evidence_for_tenant_service_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
    )


def get_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> Evidence | None:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")
    evidence = get_evidence_for_tenant_service_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_id=evidence_id,
    )
    if evidence is None:
        raise LookupError("Evidence not found")
    return evidence


def serialize_evidence(evidence: Evidence) -> dict:
    return _serialize_evidence(evidence)
