from __future__ import annotations

from datetime import datetime, timezone
from decimal import Decimal
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.prediction.api_schemas import (
    PredictionAssessmentRead,
    PredictionFactorRead,
    PredictionRunCreateRequest,
    PredictionRunRead,
)
from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionInputQuality,
    PredictionRunStatus,
    PredictionSeverity,
    PredictionUrgency,
)


def test_run_create_request_uses_camel_case_and_excludes_transport_context() -> None:
    vehicle_id = uuid4()
    service_event_id = uuid4()
    request = PredictionRunCreateRequest(vehicleId=vehicle_id, serviceEventId=service_event_id)
    assert request.vehicleId == vehicle_id
    assert request.serviceEventId == service_event_id
    assert request.analysisRunId is None
    assert set(request.model_dump()) == {"vehicleId", "serviceEventId", "analysisRunId"}
    with pytest.raises(ValidationError):
        PredictionRunCreateRequest(vehicleId=vehicle_id, tenantId=uuid4())
    with pytest.raises(ValidationError):
        PredictionRunCreateRequest(vehicleId=vehicle_id, idempotencyKey="transport-only")


def test_optional_scopes_are_supported() -> None:
    request = PredictionRunCreateRequest(vehicleId=uuid4(), analysisRunId=uuid4())
    assert request.serviceEventId is None
    assert request.analysisRunId is not None


def test_nested_run_assessment_factor_serialization() -> None:
    factor = PredictionFactorRead(
        id=uuid4(),
        tenantId=uuid4(),
        predictionAssessmentId=uuid4(),
        factorType=PredictionFactorType.VEHICLE,
        sourceEntityType="VEHICLE",
        sourceEntityId=uuid4(),
        factorCode="POWERTRAIN_TYPE",
        description="Powertrain context",
        valueNumeric=Decimal("12.50"),
        valueText="ICE",
        unit="KM",
        weight=Decimal("0.25"),
        metadataJson={"source": "test"},
        createdAt=datetime.now(timezone.utc),
    )
    assessment = PredictionAssessmentRead(
        id=uuid4(),
        tenantId=uuid4(),
        predictionRunId=uuid4(),
        predictionType="POWERTRAIN_CONTEXT",
        systemCode="ICE_POWERTRAIN_CONTEXT",
        predictedCondition="Powertrain context",
        severity=PredictionSeverity.INFO,
        urgency=PredictionUrgency.MONITOR,
        confidence=Decimal("0.8"),
        horizonType=PredictionHorizonType.UNSPECIFIED,
        explanation="Powertrain context",
        ruleCode="POWERTRAIN_CONTEXT",
        ruleVersion="1",
        createdAt=datetime.now(timezone.utc),
        factors=(factor,),
    )
    run = PredictionRunRead(
        id=uuid4(),
        tenantId=uuid4(),
        vehicleId=uuid4(),
        status=PredictionRunStatus.COMPLETED,
        inputQuality=PredictionInputQuality.COMPLETE,
        requestedAt=datetime.now(timezone.utc),
        createdAt=datetime.now(timezone.utc),
        updatedAt=datetime.now(timezone.utc),
        assessments=(assessment,),
    )
    payload = run.model_dump()
    assert payload["status"] == PredictionRunStatus.COMPLETED
    assert payload["assessments"][0]["systemCode"] == "ICE_POWERTRAIN_CONTEXT"
    assert payload["assessments"][0]["factors"][0]["factorCode"] == "POWERTRAIN_TYPE"
    assert payload["assessments"][0]["factors"][0]["valueNumeric"] == Decimal("12.50")


def test_response_schemas_reject_unknown_fields() -> None:
    with pytest.raises(ValidationError):
        PredictionRunCreateRequest(vehicleId=uuid4(), status="QUEUED")
