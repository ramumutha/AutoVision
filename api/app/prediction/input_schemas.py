from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.evidence.models import EvidenceSufficiency, FindingReviewDecision, FindingReviewStatus
from app.prediction.models import PredictionInputQuality
from app.service_intake.models import ServiceEventState
from app.vehicle.models import PowertrainType, VehicleClass


class PredictionVehicleContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    vehicle_id: UUID
    vehicle_class: VehicleClass
    powertrain: PowertrainType
    model_name: str | None = None
    year: int | None = None
    color: str | None = None


class PredictionUsageContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    usage_snapshot_id: UUID
    recorded_at: datetime
    odometer_km: Decimal | None = None
    engine_hours: Decimal | None = None
    fuel_level_pct: Decimal | None = None
    data_source: str | None = None
    payload: dict | None = None


class PredictionServiceHistoryItem(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    service_event_id: UUID
    source: str
    state: ServiceEventState
    revision: int
    opened_at: datetime | None = None
    created_at: datetime


class EffectiveFindingContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    finding_id: UUID
    service_event_id: UUID
    analysis_run_id: UUID
    finding_code: str
    original_title: str
    original_description: str | None = None
    original_component: str | None = None
    original_location: str | None = None
    effective_title: str
    effective_description: str | None = None
    effective_component: str | None = None
    effective_location: str | None = None
    confidence: Decimal | None = None
    evidence_sufficiency: EvidenceSufficiency
    review_status: FindingReviewStatus
    review_id: UUID | None = None
    review_decision: FindingReviewDecision | None = None
    reviewed_at: datetime | None = None
    supporting_evidence_ids: tuple[UUID, ...] = Field(default_factory=tuple)


class PredictionExternalContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    climate: dict | None = None
    road_condition: dict | None = None
    driving_behavior: dict | None = None
    load_profile: dict | None = None
    additional_context: dict = Field(default_factory=dict)


class PredictionInputQualityContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    quality: PredictionInputQuality
    missing_sources: tuple[str, ...] = Field(default_factory=tuple)
    warnings: tuple[str, ...] = Field(default_factory=tuple)


class CanonicalPredictionContext(BaseModel):
    model_config = ConfigDict(frozen=True, extra="forbid")

    schema_version: str
    tenant_id: UUID
    vehicle_id: UUID
    service_event_id: UUID | None = None
    analysis_run_id: UUID | None = None
    generated_at: datetime
    vehicle: PredictionVehicleContext
    usage: PredictionUsageContext | None = None
    service_history: tuple[PredictionServiceHistoryItem, ...] = Field(default_factory=tuple)
    findings: tuple[EffectiveFindingContext, ...] = Field(default_factory=tuple)
    external_context: PredictionExternalContext = Field(default_factory=PredictionExternalContext)
    quality: PredictionInputQualityContext = Field(default_factory=PredictionInputQualityContext)
