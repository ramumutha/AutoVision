from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, Header, HTTPException, UploadFile
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.evidence.schemas import EvidenceCreate, EvidenceRead
from app.evidence.service import (
    StorageFailure,
    UploadValidationError,
    complete_evidence_for_scope,
    create_evidence_for_scope,
    get_evidence_for_scope,
    list_evidence_for_scope,
    mark_upload_failed_for_scope,
    serialize_evidence,
    upload_evidence_for_scope,
)

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


@router.post("/service-events/{service_event_id}/evidence/{evidence_id}/upload", response_model=EvidenceRead)
def upload_evidence(
    service_event_id: UUID,
    evidence_id: UUID,
    file: UploadFile = File(...),
    checksum: str | None = Form(None),
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> EvidenceRead:
    try:
        with db.begin():
            evidence = upload_evidence_for_scope(
                db,
                tenant_id=tenant_id,
                service_event_id=service_event_id,
                evidence_id=evidence_id,
                upload=file,
                client_checksum=checksum,
            )
            return EvidenceRead.model_validate(serialize_evidence(evidence))
    except UploadValidationError as exc:
        raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except StorageFailure as exc:
        try:
            with db.begin():
                mark_upload_failed_for_scope(
                    db,
                    tenant_id=tenant_id,
                    service_event_id=service_event_id,
                    evidence_id=evidence_id,
                )
        except Exception:
            pass
        raise HTTPException(status_code=500, detail="Evidence storage failed") from exc


@router.post("/service-events/{service_event_id}/evidence/{evidence_id}/complete", response_model=EvidenceRead)
def complete_evidence(
    service_event_id: UUID,
    evidence_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> EvidenceRead:
    try:
        with db.begin():
            evidence = complete_evidence_for_scope(
                db,
                tenant_id=tenant_id,
                service_event_id=service_event_id,
                evidence_id=evidence_id,
            )
            return EvidenceRead.model_validate(serialize_evidence(evidence))
    except UploadValidationError as exc:
        raise HTTPException(status_code=exc.status_code, detail=exc.detail) from exc
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except StorageFailure as exc:
        raise HTTPException(status_code=500, detail="Evidence storage failed") from exc
