from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.evidence.models import AnalysisRun
from app.identity.models import UserRef
from app.prediction.api_schemas import PredictionRunCreateRequest
from app.prediction.audit import (
    PREDICTION_RUN_REPLAYED_ACTION,
    PREDICTION_RUN_REPLAYED_EVENT_TYPE,
    PREDICTION_RUN_REQUESTED_ACTION,
    PREDICTION_RUN_REQUESTED_EVENT_TYPE,
    record_prediction_audit_event,
)
from app.prediction.dispatch import PredictionDispatcher, build_prediction_work_item
from app.prediction.models import PredictionRun
from app.prediction.repository import (
    create_prediction_run,
    get_prediction_run_by_idempotency_key,
)
from app.service_intake.models import ServiceEvent
from app.vehicle.repository import get_vehicle_for_tenant


@dataclass(frozen=True)
class PredictionRunRequestResult:
    run: PredictionRun
    created: bool


def build_prediction_request_fingerprint(
    *,
    vehicle_id: UUID,
    service_event_id: UUID | None,
    analysis_run_id: UUID | None,
) -> str:
    canonical_source = {
        "vehicleId": str(vehicle_id),
        "serviceEventId": str(service_event_id) if service_event_id is not None else None,
        "analysisRunId": str(analysis_run_id) if analysis_run_id is not None else None,
    }
    serialized = json.dumps(canonical_source, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(serialized.encode("utf-8")).hexdigest()


def request_prediction_run(
    session: Session,
    *,
    tenant_id: UUID,
    payload: PredictionRunCreateRequest,
    requested_by_user_ref_id: UUID | None = None,
    idempotency_key: str | None = None,
    correlation_id: str | None = None,
    requested_at: datetime | None = None,
) -> PredictionRunRequestResult:
    vehicle = get_vehicle_for_tenant(session, tenant_id, payload.vehicleId)
    if vehicle is None:
        raise LookupError("Vehicle not found")

    if payload.serviceEventId is not None:
        service_event = session.execute(
            select(ServiceEvent).where(
                ServiceEvent.tenant_id == tenant_id,
                ServiceEvent.id == payload.serviceEventId,
                ServiceEvent.vehicle_id == payload.vehicleId,
            )
        ).scalar_one_or_none()
        if service_event is None:
            raise LookupError("Service event not found")

    if payload.analysisRunId is not None:
        if payload.serviceEventId is None:
            raise ValueError("serviceEventId is required when analysisRunId is supplied")
        analysis_run = session.execute(
            select(AnalysisRun).where(
                AnalysisRun.tenant_id == tenant_id,
                AnalysisRun.id == payload.analysisRunId,
                AnalysisRun.service_event_id == payload.serviceEventId,
            )
        ).scalar_one_or_none()
        if analysis_run is None:
            raise LookupError("Analysis run not found")

    if requested_by_user_ref_id is not None:
        user = session.execute(
            select(UserRef).where(
                UserRef.tenant_id == tenant_id,
                UserRef.id == requested_by_user_ref_id,
            )
        ).scalar_one_or_none()
        if user is None:
            raise LookupError("User not found")

    normalized_idempotency_key = _normalize_idempotency_key(idempotency_key)
    normalized_correlation_id = _normalize_correlation_id(correlation_id)
    request_fingerprint = build_prediction_request_fingerprint(
        vehicle_id=payload.vehicleId,
        service_event_id=payload.serviceEventId,
        analysis_run_id=payload.analysisRunId,
    )

    if normalized_idempotency_key is not None:
        replay = _resolve_idempotency_replay(
            session,
            tenant_id=tenant_id,
            idempotency_key=normalized_idempotency_key,
            request_fingerprint=request_fingerprint,
        )
        if replay is not None:
            _record_request_audit(
                session,
                result=replay,
                actor_user_ref_id=requested_by_user_ref_id,
                correlation_id=normalized_correlation_id,
                idempotency_key=normalized_idempotency_key,
                replay=True,
            )
            return replay

        try:
            with session.begin_nested():
                run = create_prediction_run(
                    session,
                    tenant_id=tenant_id,
                    vehicle_id=payload.vehicleId,
                    service_event_id=payload.serviceEventId,
                    analysis_run_id=payload.analysisRunId,
                    requested_by_user_ref_id=requested_by_user_ref_id,
                    requested_at=requested_at if requested_at is not None else datetime.now(timezone.utc),
                    idempotency_key=normalized_idempotency_key,
                    request_fingerprint=request_fingerprint,
                    correlation_id=normalized_correlation_id,
                )
            result = PredictionRunRequestResult(run=run, created=True)
            _record_request_audit(
                session,
                result=result,
                actor_user_ref_id=requested_by_user_ref_id,
                correlation_id=normalized_correlation_id,
                idempotency_key=normalized_idempotency_key,
                replay=False,
            )
            return result
        except IntegrityError:
            replay = _resolve_idempotency_replay(
                session,
                tenant_id=tenant_id,
                idempotency_key=normalized_idempotency_key,
                request_fingerprint=request_fingerprint,
            )
            if replay is not None:
                _record_request_audit(
                    session,
                    result=replay,
                    actor_user_ref_id=requested_by_user_ref_id,
                    correlation_id=normalized_correlation_id,
                    idempotency_key=normalized_idempotency_key,
                    replay=True,
                )
                return replay
            raise

    run = create_prediction_run(
        session,
        tenant_id=tenant_id,
        vehicle_id=payload.vehicleId,
        service_event_id=payload.serviceEventId,
        analysis_run_id=payload.analysisRunId,
        requested_by_user_ref_id=requested_by_user_ref_id,
        requested_at=requested_at if requested_at is not None else datetime.now(timezone.utc),
        idempotency_key=normalized_idempotency_key,
        request_fingerprint=request_fingerprint,
        correlation_id=normalized_correlation_id,
    )
    result = PredictionRunRequestResult(run=run, created=True)
    _record_request_audit(
        session,
        result=result,
        actor_user_ref_id=requested_by_user_ref_id,
        correlation_id=normalized_correlation_id,
        idempotency_key=normalized_idempotency_key,
        replay=False,
    )
    return result


def _record_request_audit(
    session: Session,
    *,
    result: PredictionRunRequestResult,
    actor_user_ref_id: UUID | None,
    correlation_id: str | None,
    idempotency_key: str | None,
    replay: bool,
) -> None:
    record_prediction_audit_event(
        session,
        tenant_id=result.run.tenant_id,
        vehicle_id=result.run.vehicle_id,
        prediction_run_id=result.run.id,
        event_type=PREDICTION_RUN_REPLAYED_EVENT_TYPE if replay else PREDICTION_RUN_REQUESTED_EVENT_TYPE,
        action=PREDICTION_RUN_REPLAYED_ACTION if replay else PREDICTION_RUN_REQUESTED_ACTION,
        actor_user_ref_id=actor_user_ref_id,
        service_event_id=result.run.service_event_id,
        analysis_run_id=result.run.analysis_run_id,
        correlation_id=correlation_id,
        idempotency_key=idempotency_key,
    )


def dispatch_prediction_run(
    *,
    result: PredictionRunRequestResult,
    dispatcher: PredictionDispatcher,
) -> None:
    """Dispatch a newly-created, already-committed run.

    The caller must complete the creation transaction before invoking this
    helper so an in-process worker can observe the persisted run.
    """
    if not result.created:
        return
    dispatcher.dispatch(build_prediction_work_item(run=result.run))


def _resolve_idempotency_replay(
    session: Session,
    *,
    tenant_id: UUID,
    idempotency_key: str,
    request_fingerprint: str,
) -> PredictionRunRequestResult | None:
    existing = get_prediction_run_by_idempotency_key(
        session,
        tenant_id=tenant_id,
        idempotency_key=idempotency_key,
    )
    if existing is None:
        return None
    if existing.request_fingerprint == request_fingerprint:
        return PredictionRunRequestResult(run=existing, created=False)
    raise ValueError("Idempotency key was already used for a different prediction request")


def _normalize_idempotency_key(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    if not normalized:
        raise ValueError("Idempotency key must not be blank")
    return normalized


def _normalize_correlation_id(value: str | None) -> str | None:
    if value is None:
        return None
    normalized = value.strip()
    return normalized or None
