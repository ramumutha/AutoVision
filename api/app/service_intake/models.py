from __future__ import annotations

import enum
import uuid
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Index, JSON, String, Text, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.models import Tenant
from app.identity.models import UserRef
from app.vehicle.models import Vehicle


class ServiceEventState(str, enum.Enum):
    DRAFT = "DRAFT"
    OPEN = "OPEN"


class ServiceEvent(Base):
    __tablename__ = "service_events"
    __table_args__ = (
        Index("ix_service_events_tenant_vehicle_opened", "tenant_id", "vehicle_id", "opened_at"),
        Index("ix_service_events_tenant_state_rev", "tenant_id", "state", "revision"),
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
    source: Mapped[str] = mapped_column(String(120), nullable=False, index=True)
    state: Mapped[ServiceEventState] = mapped_column(
        Enum(ServiceEventState, name="service_event_state", native_enum=True, create_constraint=True),
        nullable=False,
        default=ServiceEventState.DRAFT,
        index=True,
    )
    revision: Mapped[int] = mapped_column(default=1, nullable=False)
    opened_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
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

    tenant: Mapped[Tenant] = relationship(back_populates="service_events")
    vehicle: Mapped[Vehicle] = relationship(back_populates="service_events")
    complaints: Mapped[list["Complaint"]] = relationship(back_populates="event")
    assignments: Mapped[list["ServiceEventAssignment"]] = relationship(back_populates="event")
    contexts: Mapped[list["ServiceEventContext"]] = relationship(back_populates="event")
    evidence: Mapped[list["Evidence"]] = relationship(back_populates="event")
    analysis_runs: Mapped[list["AnalysisRun"]] = relationship(back_populates="event")
    findings: Mapped[list["Finding"]] = relationship(back_populates="event")
    prediction_runs: Mapped[list["PredictionRun"]] = relationship(back_populates="service_event")


class Complaint(Base):
    __tablename__ = "complaints"
    __table_args__ = (
        Index("ix_complaints_event_revision", "event_id", "revision", unique=True),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    original_text: Mapped[str] = mapped_column(Text, nullable=False)
    structured_summary: Mapped[str | None] = mapped_column(Text, nullable=True)
    language: Mapped[str | None] = mapped_column(String(20), nullable=True, index=True)
    captured_by: Mapped[str | None] = mapped_column(String(120), nullable=True, index=True)
    captured_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=datetime.utcnow,
        index=True,
    )
    revision: Mapped[int] = mapped_column(default=1, nullable=False)

    event: Mapped[ServiceEvent] = relationship(back_populates="complaints")


class ServiceEventAssignment(Base):
    __tablename__ = "service_event_assignments"
    __table_args__ = (
        Index("ix_service_event_assignments_event_role", "event_id", "role_code", unique=True),
        Index("ix_service_event_assignments_tenant_user", "tenant_id", "user_ref_id"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    tenant_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("tenants.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    role_code: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    assigned_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        server_default=func.now(),
        index=True,
    )

    event: Mapped[ServiceEvent] = relationship(back_populates="assignments")
    tenant: Mapped[Tenant] = relationship(back_populates="service_event_assignments")
    user_ref: Mapped[UserRef | None] = relationship(back_populates="service_event_assignments")


class ServiceEventContext(Base):
    __tablename__ = "service_event_contexts"
    __table_args__ = (
        Index("ix_service_event_contexts_event_type", "event_id", "context_type"),
    )

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    context_type: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    source_ref: Mapped[str | None] = mapped_column(String(255), nullable=True, index=True)
    snapshot_json: Mapped[dict] = mapped_column(JSON, nullable=False, default=dict)
    captured_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        default=datetime.utcnow,
        index=True,
    )

    event: Mapped[ServiceEvent] = relationship(back_populates="contexts")


__all__ = [
    "ServiceEvent",
    "ServiceEventState",
    "Complaint",
    "ServiceEventAssignment",
    "ServiceEventContext",
]
