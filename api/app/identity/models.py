from __future__ import annotations

import uuid
from datetime import datetime

from sqlalchemy import DateTime, ForeignKey, Index, String, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.models import Tenant


class UserRef(Base):
    __tablename__ = "user_refs"
    __table_args__ = (
        Index("ix_user_refs_tenant_external_user_id", "tenant_id", "external_user_id", unique=True),
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
    external_user_id: Mapped[str | None] = mapped_column(String(255), nullable=True, index=True)
    display_name: Mapped[str | None] = mapped_column(String(255), nullable=True)
    email: Mapped[str | None] = mapped_column(String(255), nullable=True)
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

    tenant: Mapped[Tenant] = relationship(back_populates="users")
    role_assignments: Mapped[list["RoleAssignment"]] = relationship(back_populates="user_ref")
    audit_events: Mapped[list["AuditEvent"]] = relationship(back_populates="user_ref")
    service_event_assignments: Mapped[list["ServiceEventAssignment"]] = relationship(back_populates="user_ref")


class RoleAssignment(Base):
    __tablename__ = "role_assignments"
    __table_args__ = (
        Index("ix_role_assignments_tenant_user_role", "tenant_id", "user_ref_id", "role_name", unique=True),
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
    user_ref_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("user_refs.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    role_name: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="role_assignments")
    user_ref: Mapped[UserRef] = relationship(back_populates="role_assignments")


__all__ = ["UserRef", "RoleAssignment"]
