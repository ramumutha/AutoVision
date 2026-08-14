from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.evidence.models import (
    EvidenceSufficiency,
    Finding,
    FindingEvidence,
    FindingReview,
    FindingReviewDecision,
    FindingReviewStatus,
)
from app.service_intake.models import ServiceEvent


def get_service_event_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> ServiceEvent | None:
    return session.execute(
        select(ServiceEvent).where(
            ServiceEvent.tenant_id == tenant_id,
            ServiceEvent.id == service_event_id,
        )
    ).scalar_one_or_none()


def list_findings_for_tenant_event(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> list[Finding]:
    return session.execute(
        select(Finding)
        .where(
            Finding.tenant_id == tenant_id,
            Finding.service_event_id == service_event_id,
        )
        .order_by(Finding.created_at.desc(), Finding.id.desc())
    ).scalars().all()


def get_finding_for_tenant_event(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    finding_id: uuid.UUID,
    for_update: bool = False,
) -> Finding | None:
    statement = select(Finding).where(
        Finding.tenant_id == tenant_id,
        Finding.service_event_id == service_event_id,
        Finding.id == finding_id,
    )
    if for_update:
        statement = statement.with_for_update()
    return session.execute(statement).scalar_one_or_none()


def list_finding_evidence_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    finding_ids: list[uuid.UUID],
) -> list[FindingEvidence]:
    if not finding_ids:
        return []
    return session.execute(
        select(FindingEvidence)
        .where(
            FindingEvidence.tenant_id == tenant_id,
            FindingEvidence.finding_id.in_(finding_ids),
        )
        .order_by(FindingEvidence.finding_id.asc(), FindingEvidence.created_at.asc(), FindingEvidence.id.asc())
    ).scalars().all()


def list_finding_reviews_for_tenant(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    finding_ids: list[uuid.UUID],
) -> list[FindingReview]:
    if not finding_ids:
        return []
    return session.execute(
        select(FindingReview)
        .where(
            FindingReview.tenant_id == tenant_id,
            FindingReview.finding_id.in_(finding_ids),
        )
        .order_by(
            FindingReview.finding_id.asc(),
            FindingReview.reviewed_at.desc().nullslast(),
            FindingReview.created_at.desc(),
            FindingReview.id.desc(),
        )
    ).scalars().all()


def create_finding_review(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    finding_id: uuid.UUID,
    decision: FindingReviewDecision,
    reviewed_at: datetime,
    reason_code: str | None,
    comment: str | None,
    modified_title: str | None,
    modified_description: str | None,
    modified_component: str | None,
    modified_location: str | None,
) -> FindingReview:
    review = FindingReview(
        tenant_id=tenant_id,
        finding_id=finding_id,
        decision=decision,
        reviewed_by_user_ref_id=None,
        reviewed_at=reviewed_at,
        reason_code=reason_code,
        comment=comment,
        modified_title=modified_title,
        modified_description=modified_description,
        modified_component=modified_component,
        modified_location=modified_location,
        created_at=reviewed_at,
    )
    session.add(review)
    session.flush()
    return review


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