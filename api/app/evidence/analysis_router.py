from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.evidence.analysis_dispatch import AnalysisDispatcher
from app.evidence.analysis_schemas import AnalysisRunRead
from app.evidence.analysis_service import (
    AnalysisConflictError,
    AnalysisDispatchError,
    get_analysis_run_for_scope,
    request_analysis_for_scope,
    serialize_analysis_run,
)


router = APIRouter(prefix="/v1", tags=["analysis"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


def get_dispatcher(request: Request) -> AnalysisDispatcher:
    return request.app.state.analysis_dispatcher


@router.post(
    "/service-events/{service_event_id}/analysis-runs",
    response_model=AnalysisRunRead,
    status_code=status.HTTP_202_ACCEPTED,
)
def request_analysis(
    service_event_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
    dispatcher: AnalysisDispatcher = Depends(get_dispatcher),
) -> AnalysisRunRead:
    try:
        queued_run = request_analysis_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            dispatcher=dispatcher,
        )
    except AnalysisConflictError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except AnalysisDispatchError as exc:
        raise HTTPException(status_code=500, detail="Analysis work could not be dispatched") from exc
    return AnalysisRunRead.model_validate(queued_run)


@router.get(
    "/service-events/{service_event_id}/analysis-runs/{analysis_run_id}",
    response_model=AnalysisRunRead,
)
def get_analysis_run(
    service_event_id: UUID,
    analysis_run_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> AnalysisRunRead:
    try:
        analysis_run = get_analysis_run_for_scope(
            db,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            analysis_run_id=analysis_run_id,
        )
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    return AnalysisRunRead.model_validate(serialize_analysis_run(analysis_run))