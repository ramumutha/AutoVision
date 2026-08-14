from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.evidence.analysis_dispatch import AnalysisDispatcher
from app.evidence.analysis_repository import (
    create_analysis_run,
    get_active_analysis_run,
    get_analysis_run_for_tenant_event,
    get_service_event_for_analysis_update,
    list_ready_evidence_for_analysis,
)
from app.evidence.analysis_schemas import AnalysisWorkItem
from app.evidence.models import AnalysisRun, AnalysisRunStatus


class AnalysisConflictError(Exception):
    pass


class AnalysisDispatchError(Exception):
    pass


def _serialize_analysis_run(analysis_run: AnalysisRun) -> dict:
    status = getattr(analysis_run.status, "value", analysis_run.status)
    return {
        "id": analysis_run.id,
        "tenantId": analysis_run.tenant_id,
        "serviceEventId": analysis_run.service_event_id,
        "status": status,
        "provider": analysis_run.provider,
        "model": analysis_run.model,
        "modelVersion": analysis_run.model_version,
        "promptName": analysis_run.prompt_name,
        "promptVersion": analysis_run.prompt_version,
        "schemaVersion": analysis_run.schema_version,
        "requestedByUserRefId": analysis_run.requested_by_user_ref_id,
        "requestedAt": analysis_run.requested_at,
        "startedAt": analysis_run.started_at,
        "completedAt": analysis_run.completed_at,
        "retryOfAnalysisRunId": analysis_run.retry_of_analysis_run_id,
        "inputEvidenceCount": analysis_run.input_evidence_count,
        "latencyMs": analysis_run.latency_ms,
        "inputTokens": analysis_run.input_tokens,
        "outputTokens": analysis_run.output_tokens,
        "estimatedCost": analysis_run.estimated_cost,
        "errorCode": analysis_run.error_code,
        "errorMessage": analysis_run.error_message,
        "createdAt": analysis_run.created_at,
        "updatedAt": analysis_run.updated_at,
    }


def request_analysis_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    dispatcher: AnalysisDispatcher,
) -> dict:
    with session.begin():
        event = get_service_event_for_analysis_update(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
        )
        if event is None:
            raise LookupError("Service event not found")

        active_run = get_active_analysis_run(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
        )
        if active_run is not None:
            raise AnalysisConflictError("An analysis run is already active")

        ready_evidence = list_ready_evidence_for_analysis(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
        )
        if not ready_evidence:
            raise AnalysisConflictError("No READY evidence available")

        analysis_run = create_analysis_run(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            input_evidence_count=len(ready_evidence),
        )
        response = _serialize_analysis_run(analysis_run)
        work_item = AnalysisWorkItem(
            analysisRunId=analysis_run.id,
            tenantId=tenant_id,
            serviceEventId=service_event_id,
            evidenceIds=tuple(evidence.id for evidence in ready_evidence),
        )

    try:
        dispatcher.dispatch(work_item)
    except Exception as exc:
        _mark_dispatch_failed(
            session,
            tenant_id=tenant_id,
            service_event_id=service_event_id,
            analysis_run_id=work_item.analysisRunId,
        )
        raise AnalysisDispatchError("Analysis work could not be dispatched") from exc

    return response


def _mark_dispatch_failed(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    analysis_run_id: uuid.UUID,
) -> None:
    try:
        with session.begin():
            analysis_run = get_analysis_run_for_tenant_event(
                session,
                tenant_id=tenant_id,
                service_event_id=service_event_id,
                analysis_run_id=analysis_run_id,
                for_update=True,
            )
            if analysis_run is not None and analysis_run.status == AnalysisRunStatus.QUEUED:
                now = datetime.now(timezone.utc)
                analysis_run.status = AnalysisRunStatus.FAILED
                analysis_run.completed_at = now
                analysis_run.updated_at = now
                analysis_run.error_code = "DISPATCH_ERROR"
                analysis_run.error_message = "Analysis work could not be dispatched"
                session.flush()
    except Exception:
        # The original dispatch failure remains the externally reported error.
        session.rollback()


def get_analysis_run_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    analysis_run_id: uuid.UUID,
) -> AnalysisRun:
    analysis_run = get_analysis_run_for_tenant_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        analysis_run_id=analysis_run_id,
    )
    if analysis_run is None:
        raise LookupError("Analysis run not found")
    return analysis_run


def serialize_analysis_run(analysis_run: AnalysisRun) -> dict:
    return _serialize_analysis_run(analysis_run)
