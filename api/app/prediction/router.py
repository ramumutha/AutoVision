from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response, status
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.prediction.api_schemas import (
    PredictionAssessmentRead,
    PredictionRunCreateRequest,
    PredictionRunRead,
)
from app.prediction.dispatch import PredictionDispatchError, PredictionDispatcher
from app.prediction.repository import get_prediction_run_for_api, list_prediction_assessments_for_run
from app.prediction.service import dispatch_prediction_run, request_prediction_run
from app.prediction.security import (
    PredictionAuthenticationError,
    PredictionAuthorizationError,
    require_prediction_read_access,
    require_prediction_request_access,
)

router = APIRouter(prefix="/v1", tags=["prediction"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


def get_user_ref_id(x_user_id: UUID | None = Header(default=None, alias="X-User-ID")) -> UUID | None:
    return x_user_id


def get_dispatcher(request: Request) -> PredictionDispatcher:
    return request.app.state.prediction_dispatcher


def _serialize_factor(factor) -> dict:
    return {
        "id": factor.id,
        "tenantId": factor.tenant_id,
        "predictionAssessmentId": factor.prediction_assessment_id,
        "factorType": factor.factor_type,
        "sourceEntityType": factor.source_entity_type,
        "sourceEntityId": factor.source_entity_id,
        "factorCode": factor.factor_code,
        "description": factor.description,
        "valueNumeric": factor.value_numeric,
        "valueText": factor.value_text,
        "unit": factor.unit,
        "weight": factor.weight,
        "metadataJson": factor.metadata_json,
        "createdAt": factor.created_at,
    }


def _serialize_assessment(assessment) -> dict:
    return {
        "id": assessment.id,
        "tenantId": assessment.tenant_id,
        "predictionRunId": assessment.prediction_run_id,
        "predictionType": assessment.prediction_type,
        "systemCode": assessment.system_code,
        "componentCode": assessment.component_code,
        "predictedCondition": assessment.predicted_condition,
        "severity": assessment.severity,
        "urgency": assessment.urgency,
        "confidence": assessment.confidence,
        "horizonType": assessment.horizon_type,
        "horizonDistance": assessment.horizon_distance,
        "horizonDistanceUnit": assessment.horizon_distance_unit,
        "horizonDays": assessment.horizon_days,
        "recommendedAction": assessment.recommended_action,
        "explanation": assessment.explanation,
        "ruleCode": assessment.rule_code,
        "ruleVersion": assessment.rule_version,
        "createdAt": assessment.created_at,
        "factors": [_serialize_factor(factor) for factor in assessment.factors],
    }


def _serialize_run(run) -> dict:
    return {
        "id": run.id,
        "tenantId": run.tenant_id,
        "vehicleId": run.vehicle_id,
        "serviceEventId": run.service_event_id,
        "analysisRunId": run.analysis_run_id,
        "status": run.status,
        "inputQuality": run.input_quality,
        "providerName": run.provider_name,
        "providerVersion": run.provider_version,
        "engineName": run.engine_name,
        "engineVersion": run.engine_version,
        "schemaVersion": run.schema_version,
        "configurationVersion": run.configuration_version,
        "requestedByUserRefId": run.requested_by_user_ref_id,
        "requestedAt": run.requested_at,
        "startedAt": run.started_at,
        "completedAt": run.completed_at,
        "retryOfPredictionRunId": run.retry_of_prediction_run_id,
        "idempotencyKey": run.idempotency_key,
        "requestFingerprint": run.request_fingerprint,
        "correlationId": run.correlation_id,
        "inputSnapshot": run.input_snapshot,
        "errorCode": run.error_code,
        "errorMessage": run.error_message,
        "createdAt": run.created_at,
        "updatedAt": run.updated_at,
        "assessments": [_serialize_assessment(assessment) for assessment in run.assessments],
    }


def _safe_prediction_error(exc: Exception) -> HTTPException:
    if isinstance(exc, LookupError):
        return HTTPException(status_code=404, detail=str(exc))
    if isinstance(exc, ValueError):
        if str(exc) == "Idempotency key was already used for a different prediction request":
            return HTTPException(status_code=409, detail=str(exc))
        return HTTPException(status_code=400, detail=str(exc))
    if isinstance(exc, PredictionDispatchError):
        return HTTPException(status_code=503, detail="Prediction work could not be dispatched")
    return HTTPException(status_code=500, detail="Prediction request failed")


@router.post(
    "/prediction-runs",
    response_model=PredictionRunRead,
    status_code=status.HTTP_201_CREATED,
)
def create_prediction_run_endpoint(
    payload: PredictionRunCreateRequest,
    response: Response,
    idempotency_key: str | None = Header(default=None, alias="Idempotency-Key"),
    correlation_id: str | None = Header(default=None, alias="X-Correlation-ID"),
    tenant_id: UUID = Depends(get_tenant_id),
    user_ref_id: UUID | None = Depends(get_user_ref_id),
    db: Session = Depends(get_db),
    dispatcher: PredictionDispatcher = Depends(get_dispatcher),
) -> PredictionRunRead:
    try:
        if user_ref_id is None:
            raise PredictionAuthenticationError("Authentication required")
        require_prediction_request_access(db, tenant_id=tenant_id, user_ref_id=user_ref_id)
        result = request_prediction_run(
            db,
            tenant_id=tenant_id,
            payload=payload,
            requested_by_user_ref_id=user_ref_id,
            idempotency_key=idempotency_key,
            correlation_id=correlation_id,
        )
        db.commit()
        dispatch_prediction_run(result=result, dispatcher=dispatcher)
        db.expire_all()
        run = get_prediction_run_for_api(db, tenant_id=tenant_id, prediction_run_id=result.run.id)
        if run is None:
            raise LookupError("Prediction run not found")
        response.status_code = status.HTTP_201_CREATED if result.created else status.HTTP_200_OK
        return PredictionRunRead.model_validate(_serialize_run(run))
    except PredictionAuthenticationError as exc:
        raise HTTPException(status_code=401, detail="Authentication required") from exc
    except PredictionAuthorizationError as exc:
        raise HTTPException(status_code=403, detail="Prediction access is not authorized") from exc
    except (LookupError, ValueError, PredictionDispatchError) as exc:
        raise _safe_prediction_error(exc) from exc


@router.get("/prediction-runs/{prediction_run_id}", response_model=PredictionRunRead)
def get_prediction_run_endpoint(
    prediction_run_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    user_ref_id: UUID | None = Depends(get_user_ref_id),
    db: Session = Depends(get_db),
) -> PredictionRunRead:
    run = get_prediction_run_for_api(db, tenant_id=tenant_id, prediction_run_id=prediction_run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Prediction run not found")
    try:
        if user_ref_id is None:
            raise PredictionAuthenticationError("Authentication required")
        require_prediction_read_access(db, tenant_id=tenant_id, user_ref_id=user_ref_id)
    except PredictionAuthenticationError as exc:
        raise HTTPException(status_code=401, detail="Authentication required") from exc
    except PredictionAuthorizationError as exc:
        raise HTTPException(status_code=403, detail="Prediction access is not authorized") from exc
    return PredictionRunRead.model_validate(_serialize_run(run))


@router.get(
    "/prediction-runs/{prediction_run_id}/assessments",
    response_model=list[PredictionAssessmentRead],
)
def list_prediction_assessments_endpoint(
    prediction_run_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    user_ref_id: UUID | None = Depends(get_user_ref_id),
    db: Session = Depends(get_db),
) -> list[PredictionAssessmentRead]:
    run = get_prediction_run_for_api(db, tenant_id=tenant_id, prediction_run_id=prediction_run_id)
    if run is None:
        raise HTTPException(status_code=404, detail="Prediction run not found")
    try:
        if user_ref_id is None:
            raise PredictionAuthenticationError("Authentication required")
        require_prediction_read_access(db, tenant_id=tenant_id, user_ref_id=user_ref_id)
    except PredictionAuthenticationError as exc:
        raise HTTPException(status_code=401, detail="Authentication required") from exc
    except PredictionAuthorizationError as exc:
        raise HTTPException(status_code=403, detail="Prediction access is not authorized") from exc
    assessments = list_prediction_assessments_for_run(
        db,
        tenant_id=tenant_id,
        prediction_run_id=prediction_run_id,
    )
    return [PredictionAssessmentRead.model_validate(_serialize_assessment(assessment)) for assessment in assessments]
