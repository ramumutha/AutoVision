from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict

from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionInputQuality,
    PredictionRunStatus,
    PredictionSeverity,
    PredictionUrgency,
)


class PredictionRunCreateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True)

    vehicleId: UUID
    serviceEventId: UUID | None = None
    analysisRunId: UUID | None = None


class PredictionFactorRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    tenantId: UUID
    predictionAssessmentId: UUID
    factorType: PredictionFactorType
    sourceEntityType: str | None = None
    sourceEntityId: UUID | None = None
    factorCode: str
    description: str | None = None
    valueNumeric: Decimal | None = None
    valueText: str | None = None
    unit: str | None = None
    weight: Decimal | None = None
    metadataJson: dict | None = None
    createdAt: datetime


class PredictionAssessmentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    tenantId: UUID
    predictionRunId: UUID
    predictionType: str
    systemCode: str | None = None
    componentCode: str | None = None
    predictedCondition: str
    severity: PredictionSeverity
    urgency: PredictionUrgency
    confidence: Decimal | None = None
    horizonType: PredictionHorizonType
    horizonDistance: Decimal | None = None
    horizonDistanceUnit: str | None = None
    horizonDays: int | None = None
    recommendedAction: str | None = None
    explanation: str
    ruleCode: str | None = None
    ruleVersion: str | None = None
    createdAt: datetime
    factors: tuple[PredictionFactorRead, ...] = ()


class PredictionRunRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    tenantId: UUID
    vehicleId: UUID
    serviceEventId: UUID | None = None
    analysisRunId: UUID | None = None
    status: PredictionRunStatus
    inputQuality: PredictionInputQuality | None = None
    providerName: str | None = None
    providerVersion: str | None = None
    engineName: str | None = None
    engineVersion: str | None = None
    schemaVersion: str | None = None
    configurationVersion: str | None = None
    requestedByUserRefId: UUID | None = None
    requestedAt: datetime
    startedAt: datetime | None = None
    completedAt: datetime | None = None
    retryOfPredictionRunId: UUID | None = None
    idempotencyKey: str | None = None
    requestFingerprint: str | None = None
    correlationId: str | None = None
    inputSnapshot: dict | None = None
    errorCode: str | None = None
    errorMessage: str | None = None
    createdAt: datetime
    updatedAt: datetime
    assessments: tuple[PredictionAssessmentRead, ...] = ()
