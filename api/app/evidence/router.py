from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.evidence.models import EvidenceStatus
from app.evidence.schemas import EvidenceCreate, EvidenceRead
from app.evidence.service import create_evidence_for_scope, get_evidence_for_scope, list_evidence_for_scope, serialize_evidence

router = APIRouter(prefix="/v1", tags=["evidence"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


@router.post("/service-events/{service_event_id}/evidence", response_model=EvidenceRead, status_code=201)
def create_evidence(
    service_event_id: UUID,
    payload: EvidenceCreate,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> EvidenceRead:
    try:
        with db.begin():
            evidence = create_evidence_for_scope(
                db,
                tenant_id=tenant_id,
                service_event_id=service_event_id,
                evidence_type=payload.evidenceType,
                capture_source=payload.captureSource,
                title=payload.title,
                description=payload.description,
                captured_at=payload.capturedAt,
            )
            return EvidenceRead.model_validate(serialize_evidence(evidence))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc


@router.get("/service-events/{service_event_id}/evidence", response_model=list[EvidenceRead])
def list_evidence(
    service_event_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> list[EvidenceRead]:
    try:
        evidence_items = list_evidence_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc

    return [EvidenceRead.model_validate(serialize_evidence(item)) for item in evidence_items]


@router.get("/service-events/{service_event_id}/evidence/{evidence_id}", response_model=EvidenceRead)
def get_evidence(
    service_event_id: UUID,
    evidence_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> EvidenceRead:
    try:
        evidence = get_evidence_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            evidence_id=evidence_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc

    return EvidenceRead.model_validate(serialize_evidence(evidence))
