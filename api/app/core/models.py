from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base


class Tenant(Base):
    __tablename__ = "tenants"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True),
        primary_key=True,
        default=uuid.uuid4,
        nullable=False,
    )
    slug: Mapped[str] = mapped_column(String(80), unique=True, nullable=False, index=True)
    name: Mapped[str] = mapped_column(String(255), nullable=False)
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

    users: Mapped[list["UserRef"]] = relationship(back_populates="tenant")
    role_assignments: Mapped[list["RoleAssignment"]] = relationship(back_populates="tenant")
    vehicles: Mapped[list["Vehicle"]] = relationship(back_populates="tenant")
    vehicle_identifiers: Mapped[list["VehicleIdentifier"]] = relationship(back_populates="tenant")
    usage_snapshots: Mapped[list["UsageSnapshot"]] = relationship(back_populates="tenant")
    audit_events: Mapped[list["AuditEvent"]] = relationship(back_populates="tenant")
    service_events: Mapped[list["ServiceEvent"]] = relationship(back_populates="tenant")
    service_event_assignments: Mapped[list["ServiceEventAssignment"]] = relationship(back_populates="tenant")


__all__ = ["Tenant"]
