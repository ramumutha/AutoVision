"""add service intake foundation

Revision ID: a2e7ef7d9a10
Revises: 9bca69d202bc
Create Date: 2026-08-13 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "a2e7ef7d9a10"
down_revision: Union[str, Sequence[str], None] = "9bca69d202bc"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "service_events",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("vehicle_id", sa.UUID(), nullable=False),
        sa.Column("source", sa.String(length=120), nullable=False),
        sa.Column(
            "state",
            sa.Enum("DRAFT", "OPEN", name="service_event_state", create_constraint=True),
            nullable=False,
        ),
        sa.Column("revision", sa.Integer(), nullable=False),
        sa.Column("opened_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["vehicle_id"], ["vehicles.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_service_events_tenant_id"), "service_events", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_service_events_vehicle_id"), "service_events", ["vehicle_id"], unique=False)
    op.create_index(op.f("ix_service_events_source"), "service_events", ["source"], unique=False)
    op.create_index(op.f("ix_service_events_state"), "service_events", ["state"], unique=False)
    op.create_index(op.f("ix_service_events_opened_at"), "service_events", ["opened_at"], unique=False)
    op.create_index(
        "ix_service_events_tenant_vehicle_opened",
        "service_events",
        ["tenant_id", "vehicle_id", "opened_at"],
        unique=False,
    )
    op.create_index(
        "ix_service_events_tenant_state_rev",
        "service_events",
        ["tenant_id", "state", "revision"],
        unique=False,
    )

    op.create_table(
        "complaints",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("event_id", sa.UUID(), nullable=False),
        sa.Column("original_text", sa.Text(), nullable=False),
        sa.Column("structured_summary", sa.Text(), nullable=True),
        sa.Column("language", sa.String(length=20), nullable=True),
        sa.Column("captured_by", sa.String(length=120), nullable=True),
        sa.Column("captured_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("revision", sa.Integer(), nullable=False),
        sa.ForeignKeyConstraint(["event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_complaints_event_id"), "complaints", ["event_id"], unique=False)
    op.create_index(op.f("ix_complaints_language"), "complaints", ["language"], unique=False)
    op.create_index(op.f("ix_complaints_captured_by"), "complaints", ["captured_by"], unique=False)
    op.create_index(op.f("ix_complaints_captured_at"), "complaints", ["captured_at"], unique=False)
    op.create_index(
        "ix_complaints_event_revision",
        "complaints",
        ["event_id", "revision"],
        unique=True,
    )

    op.create_table(
        "service_event_assignments",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("event_id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("role_code", sa.String(length=80), nullable=False),
        sa.Column("user_ref_id", sa.UUID(), nullable=True),
        sa.Column(
            "assigned_at",
            sa.DateTime(timezone=True),
            server_default=sa.text("now()"),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(["event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["user_ref_id"], ["user_refs.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_service_event_assignments_event_id"), "service_event_assignments", ["event_id"], unique=False)
    op.create_index(op.f("ix_service_event_assignments_tenant_id"), "service_event_assignments", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_service_event_assignments_role_code"), "service_event_assignments", ["role_code"], unique=False)
    op.create_index(op.f("ix_service_event_assignments_user_ref_id"), "service_event_assignments", ["user_ref_id"], unique=False)
    op.create_index(op.f("ix_service_event_assignments_assigned_at"), "service_event_assignments", ["assigned_at"], unique=False)
    op.create_index(
        "ix_service_event_assignments_event_role",
        "service_event_assignments",
        ["event_id", "role_code"],
        unique=True,
    )
    op.create_index(
        "ix_service_event_assignments_tenant_user",
        "service_event_assignments",
        ["tenant_id", "user_ref_id"],
        unique=False,
    )

    op.create_table(
        "service_event_contexts",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("event_id", sa.UUID(), nullable=False),
        sa.Column("context_type", sa.String(length=80), nullable=False),
        sa.Column("source_ref", sa.String(length=255), nullable=True),
        sa.Column("snapshot_json", sa.JSON(), nullable=False),
        sa.Column("captured_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_service_event_contexts_event_id"), "service_event_contexts", ["event_id"], unique=False)
    op.create_index(op.f("ix_service_event_contexts_context_type"), "service_event_contexts", ["context_type"], unique=False)
    op.create_index(op.f("ix_service_event_contexts_source_ref"), "service_event_contexts", ["source_ref"], unique=False)
    op.create_index(op.f("ix_service_event_contexts_captured_at"), "service_event_contexts", ["captured_at"], unique=False)
    op.create_index(
        "ix_service_event_contexts_event_type",
        "service_event_contexts",
        ["event_id", "context_type"],
        unique=False,
    )


def downgrade() -> None:
    op.drop_index("ix_service_event_contexts_event_type", table_name="service_event_contexts")
    op.drop_index(op.f("ix_service_event_contexts_captured_at"), table_name="service_event_contexts")
    op.drop_index(op.f("ix_service_event_contexts_source_ref"), table_name="service_event_contexts")
    op.drop_index(op.f("ix_service_event_contexts_context_type"), table_name="service_event_contexts")
    op.drop_index(op.f("ix_service_event_contexts_event_id"), table_name="service_event_contexts")
    op.drop_table("service_event_contexts")

    op.drop_index("ix_service_event_assignments_tenant_user", table_name="service_event_assignments")
    op.drop_index("ix_service_event_assignments_event_role", table_name="service_event_assignments")
    op.drop_index(op.f("ix_service_event_assignments_assigned_at"), table_name="service_event_assignments")
    op.drop_index(op.f("ix_service_event_assignments_user_ref_id"), table_name="service_event_assignments")
    op.drop_index(op.f("ix_service_event_assignments_role_code"), table_name="service_event_assignments")
    op.drop_index(op.f("ix_service_event_assignments_tenant_id"), table_name="service_event_assignments")
    op.drop_index(op.f("ix_service_event_assignments_event_id"), table_name="service_event_assignments")
    op.drop_table("service_event_assignments")

    op.drop_index("ix_complaints_event_revision", table_name="complaints")
    op.drop_index(op.f("ix_complaints_captured_at"), table_name="complaints")
    op.drop_index(op.f("ix_complaints_captured_by"), table_name="complaints")
    op.drop_index(op.f("ix_complaints_language"), table_name="complaints")
    op.drop_index(op.f("ix_complaints_event_id"), table_name="complaints")
    op.drop_table("complaints")

    op.drop_index("ix_service_events_tenant_state_rev", table_name="service_events")
    op.drop_index("ix_service_events_tenant_vehicle_opened", table_name="service_events")
    op.drop_index(op.f("ix_service_events_opened_at"), table_name="service_events")
    op.drop_index(op.f("ix_service_events_state"), table_name="service_events")
    op.drop_index(op.f("ix_service_events_source"), table_name="service_events")
    op.drop_index(op.f("ix_service_events_vehicle_id"), table_name="service_events")
    op.drop_index(op.f("ix_service_events_tenant_id"), table_name="service_events")
    op.drop_table("service_events")
