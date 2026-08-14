from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.evidence.finding_repository import (
    create_finding_review,
    get_finding_for_tenant_event,
    get_service_event_for_tenant,
    list_finding_evidence_for_tenant,
    list_finding_reviews_for_tenant,
    list_findings_for_tenant_event,
)
from app.evidence.finding_schemas import FindingRead, FindingReviewCreate, FindingReviewRead
from app.evidence.models import Finding, FindingReviewDecision, FindingReviewStatus


def _serialize_review(review) -> dict:
    return {
        "id": review.id,
        "decision": review.decision,
        "reviewedByUserRefId": review.reviewed_by_user_ref_id,
        "reviewedAt": review.reviewed_at,
        "reasonCode": review.reason_code,
        "comment": review.comment,
        "modifiedTitle": review.modified_title,
        "modifiedDescription": review.modified_description,
        "modifiedComponent": review.modified_component,
        "modifiedLocation": review.modified_location,
        "createdAt": review.created_at,
    }


def _serialize_finding(finding: Finding, evidence_ids: list[uuid.UUID], latest_review) -> dict:
    return {
        "id": finding.id,
        "tenantId": finding.tenant_id,
        "serviceEventId": finding.service_event_id,
        "analysisRunId": finding.analysis_run_id,
        "findingCode": finding.finding_code,
        "title": finding.title,
        "description": finding.description,
        "component": finding.component,
        "location": finding.location,
        "confidence": finding.confidence,
        "evidenceSufficiency": finding.evidence_sufficiency,
        "reviewStatus": finding.review_status,
        "createdAt": finding.created_at,
        "supportingEvidenceIds": evidence_ids,
        "latestReview": _serialize_review(latest_review) if latest_review is not None else None,
    }


def _require_service_event(session: Session, tenant_id: uuid.UUID, service_event_id: uuid.UUID) -> None:
    if get_service_event_for_tenant(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
    ) is None:
        raise LookupError("Service event not found")


def list_findings_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
) -> list[FindingRead]:
    _require_service_event(session, tenant_id, service_event_id)
    findings = list_findings_for_tenant_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
    )
    finding_ids = [finding.id for finding in findings]
    evidence_links = list_finding_evidence_for_tenant(
        session,
        tenant_id=tenant_id,
        finding_ids=finding_ids,
    )
    reviews = list_finding_reviews_for_tenant(
        session,
        tenant_id=tenant_id,
        finding_ids=finding_ids,
    )
    evidence_by_finding: dict[uuid.UUID, list[uuid.UUID]] = {}
    for link in evidence_links:
        evidence_by_finding.setdefault(link.finding_id, []).append(link.evidence_id)
    latest_review_by_finding = {}
    for review in reviews:
        latest_review_by_finding.setdefault(review.finding_id, review)
    return [
        FindingRead.model_validate(
            _serialize_finding(
                finding,
                evidence_by_finding.get(finding.id, []),
                latest_review_by_finding.get(finding.id),
            )
        )
        for finding in findings
    ]


def get_finding_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    finding_id: uuid.UUID,
) -> FindingRead:
    _require_service_event(session, tenant_id, service_event_id)
    finding = get_finding_for_tenant_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        finding_id=finding_id,
    )
    if finding is None:
        raise LookupError("Finding not found")
    evidence_links = list_finding_evidence_for_tenant(
        session,
        tenant_id=tenant_id,
        finding_ids=[finding.id],
    )
    reviews = list_finding_reviews_for_tenant(
        session,
        tenant_id=tenant_id,
        finding_ids=[finding.id],
    )
    latest_review = reviews[0] if reviews else None
    return FindingRead.model_validate(
        _serialize_finding(
            finding,
            [link.evidence_id for link in evidence_links],
            latest_review,
        )
    )


def submit_finding_review_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    finding_id: uuid.UUID,
    payload: FindingReviewCreate,
) -> FindingReviewRead:
    with session.begin():
        _require_service_event(session, tenant_id, service_event_id)
        finding = get_finding_for_tenant_event(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            finding_id=finding_id,
            for_update=True,
        )
        if finding is None:
            raise LookupError("Finding not found")
        now = datetime.now(timezone.utc)
        review = create_finding_review(
            session,
            tenant_id=tenant_id,
            finding_id=finding.id,
            decision=payload.decision,
            reviewed_at=now,
            reason_code=payload.reasonCode,
            comment=payload.comment,
            modified_title=payload.modifiedTitle,
            modified_description=payload.modifiedDescription,
            modified_component=payload.modifiedComponent,
            modified_location=payload.modifiedLocation,
        )
        finding.review_status = FindingReviewStatus(payload.decision.value)
        session.flush()
        return FindingReviewRead.model_validate(_serialize_review(review))
