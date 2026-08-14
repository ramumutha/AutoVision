from __future__ import annotations

import enum
import uuid
from datetime import datetime
from typing import TYPE_CHECKING

from sqlalchemy import DateTime, Enum, ForeignKey, Index, JSON, Numeric, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.models import Tenant

if TYPE_CHECKING:
    from app.evidence.models import AnalysisRun
    from app.identity.models import UserRef
    from app.service_intake.models import ServiceEvent
    from app.vehicle.models import Vehicle


class PredictionRunStatus(str, enum.Enum):
    QUEUED = "QUEUED"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    PARTIALLY_COMPLETED = "PARTIALLY_COMPLETED"
    FAILED = "FAILED"


class PredictionInputQuality(str, enum.Enum):
    COMPLETE = "COMPLETE"
    PARTIAL = "PARTIAL"
    INSUFFICIENT = "INSUFFICIENT"


class PredictionSeverity(str, enum.Enum):
    INFO = "INFO"
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


class PredictionUrgency(str, enum.Enum):
    MONITOR = "MONITOR"
    ROUTINE = "ROUTINE"
    SOON = "SOON"
    IMMEDIATE = "IMMEDIATE"


class PredictionHorizonType(str, enum.Enum):
    DISTANCE = "DISTANCE"
    TIME = "TIME"
    DISTANCE_OR_TIME = "DISTANCE_OR_TIME"
    UNSPECIFIED = "UNSPECIFIED"


class PredictionFactorType(str, enum.Enum):
    VEHICLE = "VEHICLE"
    USAGE = "USAGE"
    SERVICE_HISTORY = "SERVICE_HISTORY"
    FINDING = "FINDING"
    CLIMATE = "CLIMATE"
    ROAD_CONDITION = "ROAD_CONDITION"
    DRIVING_BEHAVIOR = "DRIVING_BEHAVIOR"
    LOAD_PROFILE = "LOAD_PROFILE"
    RULE = "RULE"
    EXTERNAL_CONTEXT = "EXTERNAL_CONTEXT"
    OTHER = "OTHER"


class PredictionRun(Base):
    __tablename__ = "prediction_runs"
    __table_args__ = (
        Index("ix_prediction_runs_tenant_vehicle_requested", "tenant_id", "vehicle_id", "requested_at"),
        Index("ix_prediction_runs_tenant_status_requested", "tenant_id", "status", "requested_at"),
        Index("ix_prediction_runs_tenant_idempotency_key", "tenant_id", "idempotency_key", unique=True),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("tenants.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    vehicle_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("vehicles.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    service_event_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("service_events.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    analysis_run_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("analysis_runs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    status: Mapped[PredictionRunStatus] = mapped_column(
        Enum(PredictionRunStatus, name="prediction_run_status", native_enum=True, create_constraint=True),
        nullable=False,
        default=PredictionRunStatus.QUEUED,
        index=True,
    )
    input_quality: Mapped[PredictionInputQuality | None] = mapped_column(
        Enum(PredictionInputQuality, name="prediction_input_quality", native_enum=True, create_constraint=True),
        nullable=True,
        index=True,
    )
    provider_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    provider_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    engine_name: Mapped[str | None] = mapped_column(String(120), nullable=True)
    engine_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    schema_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    configuration_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    requested_by_user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    retry_of_prediction_run_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("prediction_runs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    idempotency_key: Mapped[str | None] = mapped_column(String(255), nullable=True, index=True)
    request_fingerprint: Mapped[str | None] = mapped_column(String(128), nullable=True)
    correlation_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    input_snapshot: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    error_code: Mapped[str | None] = mapped_column(String(120), nullable=True)
    error_message: Mapped[str | None] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
        nullable=False,
    )

    tenant: Mapped["Tenant"] = relationship(back_populates="prediction_runs")
    vehicle: Mapped["Vehicle"] = relationship(back_populates="prediction_runs")
    service_event: Mapped["ServiceEvent | None"] = relationship(back_populates="prediction_runs")
    analysis_run: Mapped["AnalysisRun | None"] = relationship(back_populates="prediction_runs")
    requested_by_user_ref: Mapped["UserRef | None"] = relationship(back_populates="prediction_runs")
    assessments: Mapped[list["PredictionAssessment"]] = relationship(back_populates="prediction_run")
    retry_of_prediction_run: Mapped["PredictionRun | None"] = relationship(
        back_populates="retry_runs",
        remote_side="PredictionRun.id",
    )
    retry_runs: Mapped[list["PredictionRun"]] = relationship(
        back_populates="retry_of_prediction_run",
        remote_side="PredictionRun.retry_of_prediction_run_id",
    )


class PredictionAssessment(Base):
    __tablename__ = "prediction_assessments"
    __table_args__ = (
        Index("ix_prediction_assessments_tenant_prediction_run", "tenant_id", "prediction_run_id"),
        Index("ix_prediction_assessments_prediction_run_severity", "prediction_run_id", "severity"),
        Index("ix_prediction_assessments_prediction_run_urgency", "prediction_run_id", "urgency"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("tenants.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    prediction_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("prediction_runs.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    prediction_type: Mapped[str] = mapped_column(String(120), nullable=False, index=True)
    system_code: Mapped[str | None] = mapped_column(String(120), nullable=True)
    component_code: Mapped[str | None] = mapped_column(String(160), nullable=True)
    predicted_condition: Mapped[str] = mapped_column(Text, nullable=False)
    severity: Mapped[PredictionSeverity] = mapped_column(
        Enum(PredictionSeverity, name="prediction_severity", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    urgency: Mapped[PredictionUrgency] = mapped_column(
        Enum(PredictionUrgency, name="prediction_urgency", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    confidence: Mapped[float | None] = mapped_column(Numeric(5, 4), nullable=True)
    horizon_type: Mapped[PredictionHorizonType] = mapped_column(
        Enum(PredictionHorizonType, name="prediction_horizon_type", native_enum=True, create_constraint=True),
        nullable=False,
        default=PredictionHorizonType.UNSPECIFIED,
        index=True,
    )
    horizon_distance: Mapped[float | None] = mapped_column(Numeric(12, 2), nullable=True)
    horizon_distance_unit: Mapped[str | None] = mapped_column(String(40), nullable=True)
    horizon_days: Mapped[int | None] = mapped_column(nullable=True)
    recommended_action: Mapped[str | None] = mapped_column(Text, nullable=True)
    explanation: Mapped[str] = mapped_column(Text, nullable=False)
    rule_code: Mapped[str | None] = mapped_column(String(120), nullable=True)
    rule_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped["Tenant"] = relationship(back_populates="prediction_assessments")
    prediction_run: Mapped[PredictionRun] = relationship(back_populates="assessments")
    factors: Mapped[list["PredictionFactor"]] = relationship(back_populates="prediction_assessment")


class PredictionFactor(Base):
    __tablename__ = "prediction_factors"
    __table_args__ = (
        Index("ix_prediction_factors_tenant_prediction_assessment", "tenant_id", "prediction_assessment_id"),
        Index("ix_prediction_factors_assessment_factor_type", "prediction_assessment_id", "factor_type"),
        Index("ix_prediction_factors_source_entity", "source_entity_type", "source_entity_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("tenants.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    prediction_assessment_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("prediction_assessments.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    factor_type: Mapped[PredictionFactorType] = mapped_column(
        Enum(PredictionFactorType, name="prediction_factor_type", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    source_entity_type: Mapped[str | None] = mapped_column(String(120), nullable=True, index=True)
    source_entity_id: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), nullable=True, index=True)
    factor_code: Mapped[str] = mapped_column(String(160), nullable=False, index=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    value_numeric: Mapped[float | None] = mapped_column(Numeric(18, 6), nullable=True)
    value_text: Mapped[str | None] = mapped_column(Text, nullable=True)
    unit: Mapped[str | None] = mapped_column(String(40), nullable=True)
    weight: Mapped[float | None] = mapped_column(Numeric(12, 6), nullable=True)
    metadata_json: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped["Tenant"] = relationship(back_populates="prediction_factors")
    prediction_assessment: Mapped[PredictionAssessment] = relationship(back_populates="factors")


__all__ = [
    "PredictionRun",
    "PredictionRunStatus",
    "PredictionInputQuality",
    "PredictionAssessment",
    "PredictionSeverity",
    "PredictionUrgency",
    "PredictionHorizonType",
    "PredictionFactor",
    "PredictionFactorType",
]
