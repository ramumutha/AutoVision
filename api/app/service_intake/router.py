from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.service_intake.schemas import ComplaintCreate, ComplaintPatch, ServiceEventRead
from app.service_intake.service import (
    create_service_event_for_scope,
    get_service_event_for_scope,
    open_service_event_for_scope,
    serialize_service_event,
    update_complaint_for_scope,
)

router = APIRouter(prefix="/v1", tags=["service-intake"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


@router.post("/service-events", response_model=ServiceEventRead, status_code=201)
def create_service_event(
    payload: ComplaintCreate,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> ServiceEventRead:
    try:
        with db.begin():
            event = create_service_event_for_scope(
                db,
                tenant_id=tenant_id,
                vehicle_id=payload.vehicleId,
                source=payload.source,
                original_complaint=payload.originalComplaint,
                structured_summary=payload.structuredSummary,
                language=payload.language,
                captured_by=payload.capturedBy,
            )
            return ServiceEventRead.model_validate(serialize_service_event(event))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail="Vehicle not found") from exc


@router.get("/service-events/{event_id}", response_model=ServiceEventRead)
def get_service_event(
    event_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> ServiceEventRead:
    event = get_service_event_for_scope(db, tenant_id, event_id)
    if event is None:
        raise HTTPException(status_code=404, detail="Service event not found")
    return ServiceEventRead.model_validate(serialize_service_event(event))


@router.patch("/service-events/{event_id}/complaint", response_model=ServiceEventRead)
def update_complaint(
    event_id: UUID,
    payload: ComplaintPatch,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> ServiceEventRead:
    updates = payload.model_dump(exclude_unset=True)
    if not any(value is not None for value in updates.values()):
        raise HTTPException(status_code=400, detail="No complaint fields supplied")

    try:
        with db.begin():
            event = update_complaint_for_scope(
                db,
                tenant_id=tenant_id,
                event_id=event_id,
                original_complaint=payload.originalComplaint,
                structured_summary=payload.structuredSummary,
                language=payload.language,
                captured_by=payload.capturedBy,
            )
            return ServiceEventRead.model_validate(serialize_service_event(event))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail="Service event not found") from exc
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/service-events/{event_id}/open", response_model=ServiceEventRead)
def open_service_event(
    event_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> ServiceEventRead:
    try:
        with db.begin():
            event = open_service_event_for_scope(
                db,
                tenant_id=tenant_id,
                event_id=event_id,
            )
            return ServiceEventRead.model_validate(serialize_service_event(event))
    except LookupError as exc:
        raise HTTPException(status_code=404, detail="Service event not found") from exc
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
