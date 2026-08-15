from __future__ import annotations

from datetime import datetime, timezone
from uuid import UUID

from sqlalchemy.orm import Session

from app.vehicle.models import AuditEvent

PREDICTION_RUN_REQUESTED_EVENT_TYPE = "PREDICTION_RUN_REQUESTED"
PREDICTION_RUN_REQUESTED_ACTION = "prediction.run.requested"
PREDICTION_RUN_REPLAYED_EVENT_TYPE = "PREDICTION_RUN_REPLAYED"
PREDICTION_RUN_REPLAYED_ACTION = "prediction.run.replayed"


def record_prediction_audit_event(
    session: Session,
    *,
    tenant_id: UUID,
    vehicle_id: UUID,
    prediction_run_id: UUID,
    event_type: str,
    action: str,
    actor_user_ref_id: UUID | None,
    service_event_id: UUID | None = None,
    analysis_run_id: UUID | None = None,
    correlation_id: str | None = None,
    idempotency_key: str | None = None,
    occurred_at: datetime | None = None,
) -> AuditEvent:
    event_metadata = {
        "predictionRunId": str(prediction_run_id),
        "serviceEventId": str(service_event_id) if service_event_id is not None else None,
        "analysisRunId": str(analysis_run_id) if analysis_run_id is not None else None,
        "correlationId": correlation_id,
        "idempotencyKey": idempotency_key,
    }
    audit_event = AuditEvent(
        tenant_id=tenant_id,
        vehicle_id=vehicle_id,
        user_ref_id=actor_user_ref_id,
        event_type=event_type,
        action=action,
        entity_type="prediction_run",
        entity_id=str(prediction_run_id),
        occurred_at=occurred_at if occurred_at is not None else datetime.now(timezone.utc),
        event_metadata=event_metadata,
    )
    session.add(audit_event)
    session.flush()
    return audit_event
