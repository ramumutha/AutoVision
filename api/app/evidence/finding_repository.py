from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.evidence.models import EvidenceSufficiency, Finding, FindingEvidence, FindingReviewStatus


def add_finding(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    analysis_run_id: uuid.UUID,
    finding_code: str,
    title: str,
    description: str | None,
    component: str | None,
    location: str | None,
    confidence: float | None,
    evidence_sufficiency: EvidenceSufficiency,
) -> Finding:
    finding = Finding(
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        analysis_run_id=analysis_run_id,
        finding_code=finding_code,
        title=title,
        description=description,
        component=component,
        location=location,
        confidence=confidence,
        evidence_sufficiency=evidence_sufficiency,
        review_status=FindingReviewStatus.PENDING_REVIEW,
        created_at=datetime.now(timezone.utc),
    )
    session.add(finding)
    return finding


def add_finding_evidence(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    finding_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> FindingEvidence:
    link = FindingEvidence(
        tenant_id=tenant_id,
        finding_id=finding_id,
        evidence_id=evidence_id,
        relationship_type="supporting",
        created_at=datetime.now(timezone.utc),
    )
    session.add(link)
    return link