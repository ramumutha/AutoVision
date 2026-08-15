from __future__ import annotations

from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.prediction.input_schemas import CanonicalPredictionContext
from app.prediction.models import (
    PredictionFactorType,
    PredictionHorizonType,
    PredictionSeverity,
    PredictionUrgency,
)


class PredictionRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    schema_version: str
    context: CanonicalPredictionContext


class PredictionFactorCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    factor_type: PredictionFactorType
    factor_code: str
    label: str
    description: str | None = None
    value_numeric: Decimal | None = None
    value_text: str | None = None
    unit: str | None = None
    weight: Decimal | None = None
    source_entity_type: str | None = None
    source_entity_id: UUID | None = None
    metadata: dict = Field(default_factory=dict)


class PredictionAssessmentCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    prediction_type: str
    prediction_code: str
    title: str
    description: str
    severity: PredictionSeverity
    urgency: PredictionUrgency
    confidence: Decimal | None = Field(default=None, ge=0, le=1)
    horizon_type: PredictionHorizonType
    horizon_distance_km: Decimal | None = Field(default=None, ge=0)
    horizon_time_days: int | None = Field(default=None, ge=0)
    recommended_action: str | None = None
    factors: tuple[PredictionFactorCandidate, ...] = Field(default_factory=tuple)
    metadata: dict = Field(default_factory=dict)


class PredictionProviderUsage(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    rules_evaluated: int | None = Field(default=None, ge=0)
    rules_matched: int | None = Field(default=None, ge=0)
    duration_ms: int | None = Field(default=None, ge=0)


class PredictionResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    schema_version: str
    provider: str
    model: str
    model_version: str
    assessments: tuple[PredictionAssessmentCandidate, ...] = Field(default_factory=tuple)
    warnings: tuple[str, ...] = Field(default_factory=tuple)
    usage: PredictionProviderUsage = Field(default_factory=PredictionProviderUsage)
