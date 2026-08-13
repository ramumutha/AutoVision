from __future__ import annotations

import enum
import uuid
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Index, JSON, Numeric, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.models import Tenant


class VehicleClass(str, enum.Enum):
    TWO_WHEELER = "TWO_WHEELER"
    PASSENGER = "PASSENGER"
    LCV = "LCV"
    TRUCK = "TRUCK"
    BUS_COACH = "BUS_COACH"
    HEAVY_SPECIAL = "HEAVY_SPECIAL"


class PowertrainType(str, enum.Enum):
    ICE = "ICE"
    HYBRID = "HYBRID"
    EV = "EV"


class Vehicle(Base):
    __tablename__ = "vehicles"
    __table_args__ = (
        Index("ix_vehicles_tenant_canonical_id", "tenant_id", "id", unique=True),
        Index("ix_vehicles_tenant_class", "tenant_id", "vehicle_class"),
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
    vehicle_class: Mapped[VehicleClass] = mapped_column(
        Enum(VehicleClass, name="vehicle_class", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    powertrain: Mapped[PowertrainType] = mapped_column(
        Enum(PowertrainType, name="powertrain_type", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    model_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    year: Mapped[int | None] = mapped_column(nullable=True)
    color: Mapped[str | None] = mapped_column(String(80), nullable=True)
    is_active: Mapped[bool] = mapped_column(default=True, nullable=False)
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

    tenant: Mapped[Tenant] = relationship(back_populates="vehicles")
    vehicle_identifiers: Mapped[list["VehicleIdentifier"]] = relationship(back_populates="vehicle")
    usage_snapshots: Mapped[list["UsageSnapshot"]] = relationship(back_populates="vehicle")
    audit_events: Mapped[list["AuditEvent"]] = relationship(back_populates="vehicle")


class VehicleIdentifier(Base):
    __tablename__ = "vehicle_identifiers"
    __table_args__ = (
        Index("ix_vehicle_identifiers_tenant_type_value", "tenant_id", "identifier_type", "identifier_value", unique=True),
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
    identifier_type: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    identifier_value: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    is_primary: Mapped[bool] = mapped_column(default=False, nullable=False)
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

    tenant: Mapped[Tenant] = relationship(back_populates="vehicle_identifiers")
    vehicle: Mapped[Vehicle] = relationship(back_populates="vehicle_identifiers")


class UsageSnapshot(Base):
    __tablename__ = "usage_snapshots"
    __table_args__ = (
        Index("ix_usage_snapshots_tenant_vehicle_recorded", "tenant_id", "vehicle_id", "recorded_at", unique=True),
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
    recorded_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        index=True,
    )
    odometer_km: Mapped[float | None] = mapped_column(Numeric(12, 2), nullable=True)
    engine_hours: Mapped[float | None] = mapped_column(Numeric(12, 2), nullable=True)
    fuel_level_pct: Mapped[float | None] = mapped_column(Numeric(5, 2), nullable=True)
    data_source: Mapped[str | None] = mapped_column(String(120), nullable=True)
    payload: Mapped[dict | None] = mapped_column(JSON, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="usage_snapshots")
    vehicle: Mapped[Vehicle] = relationship(back_populates="usage_snapshots")


class AuditEvent(Base):
    __tablename__ = "audit_events"
    __table_args__ = (
        Index("ix_audit_events_tenant_occurred_at", "tenant_id", "occurred_at"),
        Index("ix_audit_events_vehicle_occurred_at", "vehicle_id", "occurred_at"),
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
    vehicle_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("vehicles.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    event_type: Mapped[str] = mapped_column(String(120), nullable=False, index=True)
    action: Mapped[str] = mapped_column(String(255), nullable=False)
    entity_type: Mapped[str | None] = mapped_column(String(120), nullable=True)
    entity_id: Mapped[str | None] = mapped_column(String(255), nullable=True)
    occurred_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        nullable=False,
        index=True,
    )
    event_metadata: Mapped[dict | None] = mapped_column(JSON, nullable=True, default=dict)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="audit_events")
    vehicle: Mapped["Vehicle | None"] = relationship(back_populates="audit_events")
    user_ref: Mapped["UserRef | None"] = relationship(back_populates="audit_events")


__all__ = [
    "Vehicle",
    "VehicleClass",
    "PowertrainType",
    "VehicleIdentifier",
    "UsageSnapshot",
    "AuditEvent",
]
