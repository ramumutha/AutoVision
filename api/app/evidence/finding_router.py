from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, status
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.evidence.finding_schemas import FindingRead, FindingReviewCreate, FindingReviewRead
from app.evidence.finding_service import (
    get_finding_for_scope,
    list_findings_for_scope,
    submit_finding_review_for_scope,
)


router = APIRouter(prefix="/v1", tags=["findings"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


@router.get("/service-events/{service_event_id}/findings", response_model=list[FindingRead])
def list_findings(
    service_event_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> list[FindingRead]:
    try:
        return list_findings_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.get("/service-events/{service_event_id}/findings/{finding_id}", response_model=FindingRead)
def get_finding(
    service_event_id: UUID,
    finding_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> FindingRead:
    try:
        return get_finding_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            finding_id=finding_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.post(
    "/service-events/{service_event_id}/findings/{finding_id}/reviews",
    response_model=FindingReviewRead,
    status_code=status.HTTP_201_CREATED,
)
def submit_finding_review(
    service_event_id: UUID,
    finding_id: UUID,
    payload: FindingReviewCreate,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> FindingReviewRead:
    try:
        return submit_finding_review_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            finding_id=finding_id,
            payload=payload,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
