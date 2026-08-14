from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.evidence.models import CaptureSource, Evidence, EvidenceAsset, EvidenceStatus, EvidenceType
from app.service_intake.models import ServiceEvent


def get_service_event_for_tenant(session: Session, tenant_id: uuid.UUID, service_event_id: uuid.UUID) -> ServiceEvent | None:
    return session.execute(
        select(ServiceEvent).where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == service_event_id,
        )
    ).scalar_one_or_none()


def create_evidence_for_tenant(
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
    evidence = Evidence(
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_type=evidence_type,
        capture_source=capture_source,
        title=title,
        description=description,
        captured_at=captured_at,
        status=EvidenceStatus.PENDING_UPLOAD,
        created_at=datetime.now(timezone.utc),
        updated_at=datetime.now(timezone.utc),
    )
    session.add(evidence)
    session.flush()
    return evidence


def list_evidence_for_tenant_service_event(
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
        )
        .order_by(Evidence.created_at.desc(), Evidence.id.desc())
    ).scalars().all()


def get_evidence_for_tenant_service_event(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> Evidence | None:
    return session.execute(
        select(Evidence).where(
            Evidence.tenant_id == tenant_id,
            Evidence.service_event_id == service_event_id,
            Evidence.id == evidence_id,
        )
    ).scalar_one_or_none()


def create_evidence_asset_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    evidence_id: uuid.UUID,
    storage_provider: str,
    storage_key: str,
    file_name: str,
    media_type: str,
    file_size_bytes: int,
    checksum_sha256: str,
) -> EvidenceAsset:
    asset = EvidenceAsset(
        tenant_id=tenant_id,
        evidence_id=evidence_id,
        storage_provider=storage_provider,
        storage_key=storage_key,
        file_name=file_name,
        media_type=media_type,
        file_size_bytes=file_size_bytes,
        checksum_sha256=checksum_sha256,
    )
    session.add(asset)
    session.flush()
    return asset


def get_evidence_asset_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> EvidenceAsset | None:
    return session.execute(
        select(EvidenceAsset).where(
            EvidenceAsset.tenant_id == tenant_id,
            EvidenceAsset.evidence_id == evidence_id,
        ).order_by(EvidenceAsset.created_at.desc(), EvidenceAsset.id.desc())
    ).scalars().first()
