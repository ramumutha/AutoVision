"""add s2.1 evidence capture and findings

Revision ID: b6d8b5d3d5b9
Revises: a2e7ef7d9a10
Create Date: 2026-08-14 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql


revision: str = "b6d8b5d3d5b9"
down_revision: Union[str, Sequence[str], None] = "a2e7ef7d9a10"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


evidence_type_enum = postgresql.ENUM(
    "IMAGE",
    "VIDEO",
    "DOCUMENT",
    "OTHER",
    name="evidence_type",
    create_type=False,
)
capture_source_enum = postgresql.ENUM(
    "UPLOAD",
    "CAMERA",
    "MOBILE_CAPTURE",
    "SYSTEM_IMPORT",
    name="capture_source",
    create_type=False,
)
evidence_status_enum = postgresql.ENUM(
    "PENDING_UPLOAD",
    "UPLOADED",
    "READY",
    "PROCESSING",
    "PROCESSED",
    "UPLOAD_FAILED",
    "PROCESSING_FAILED",
    name="evidence_status",
    create_type=False,
)
analysis_run_status_enum = postgresql.ENUM(
    "QUEUED",
    "PROCESSING",
    "SUCCEEDED",
    "FAILED",
    name="analysis_run_status",
    create_type=False,
)
evidence_sufficiency_enum = postgresql.ENUM(
    "SUFFICIENT",
    "INSUFFICIENT",
    "PARTIAL",
    name="evidence_sufficiency",
    create_type=False,
)
finding_review_status_enum = postgresql.ENUM(
    "PENDING_REVIEW",
    "CONFIRMED",
    "MODIFIED",
    "REJECTED",
    name="finding_review_status",
    create_type=False,
)
finding_review_decision_enum = postgresql.ENUM(
    "CONFIRMED",
    "MODIFIED",
    "REJECTED",
    name="finding_review_decision",
    create_type=False,
)


def upgrade() -> None:
    evidence_type_enum.create(op.get_bind(), checkfirst=True)
    capture_source_enum.create(op.get_bind(), checkfirst=True)
    evidence_status_enum.create(op.get_bind(), checkfirst=True)
    analysis_run_status_enum.create(op.get_bind(), checkfirst=True)
    evidence_sufficiency_enum.create(op.get_bind(), checkfirst=True)
    finding_review_status_enum.create(op.get_bind(), checkfirst=True)
    finding_review_decision_enum.create(op.get_bind(), checkfirst=True)

    op.create_table(
        "evidence",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("service_event_id", sa.UUID(), nullable=False),
        sa.Column("evidence_type", evidence_type_enum, nullable=False),
        sa.Column("capture_source", capture_source_enum, nullable=False),
        sa.Column("title", sa.String(length=255), nullable=True),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("captured_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("captured_by_user_ref_id", sa.UUID(), nullable=True),
        sa.Column("status", evidence_status_enum, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), onupdate=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["service_event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["captured_by_user_ref_id"], ["user_refs.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_evidence_tenant_id"), "evidence", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_evidence_service_event_id"), "evidence", ["service_event_id"], unique=False)
    op.create_index(op.f("ix_evidence_evidence_type"), "evidence", ["evidence_type"], unique=False)
    op.create_index(op.f("ix_evidence_capture_source"), "evidence", ["capture_source"], unique=False)
    op.create_index(op.f("ix_evidence_captured_by_user_ref_id"), "evidence", ["captured_by_user_ref_id"], unique=False)
    op.create_index(op.f("ix_evidence_status"), "evidence", ["status"], unique=False)
    op.create_index(op.f("ix_evidence_captured_at"), "evidence", ["captured_at"], unique=False)
    op.create_index("ix_evidence_tenant_event", "evidence", ["tenant_id", "service_event_id"], unique=False)
    op.create_index("ix_evidence_type_status", "evidence", ["tenant_id", "evidence_type", "status"], unique=False)

    op.create_table(
        "evidence_assets",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("evidence_id", sa.UUID(), nullable=False),
        sa.Column("storage_provider", sa.String(length=80), nullable=False),
        sa.Column("storage_key", sa.String(length=1024), nullable=False),
        sa.Column("file_name", sa.String(length=255), nullable=False),
        sa.Column("media_type", sa.String(length=120), nullable=False),
        sa.Column("file_size_bytes", sa.BigInteger(), nullable=False),
        sa.Column("checksum_sha256", sa.String(length=128), nullable=False),
        sa.Column("width", sa.Integer(), nullable=True),
        sa.Column("height", sa.Integer(), nullable=True),
        sa.Column("duration_ms", sa.Integer(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["evidence_id"], ["evidence.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_evidence_assets_tenant_id"), "evidence_assets", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_evidence_assets_evidence_id"), "evidence_assets", ["evidence_id"], unique=False)
    op.create_index(op.f("ix_evidence_assets_storage_provider"), "evidence_assets", ["storage_provider"], unique=False)
    op.create_index("ix_evidence_assets_evidence_storage", "evidence_assets", ["evidence_id", "storage_provider"], unique=False)

    op.create_table(
        "analysis_runs",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("service_event_id", sa.UUID(), nullable=False),
        sa.Column("status", analysis_run_status_enum, nullable=False),
        sa.Column("provider", sa.String(length=120), nullable=True),
        sa.Column("model", sa.String(length=160), nullable=True),
        sa.Column("model_version", sa.String(length=80), nullable=True),
        sa.Column("prompt_name", sa.String(length=160), nullable=True),
        sa.Column("prompt_version", sa.String(length=80), nullable=True),
        sa.Column("schema_version", sa.String(length=80), nullable=True),
        sa.Column("requested_by_user_ref_id", sa.UUID(), nullable=True),
        sa.Column("requested_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("started_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("completed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("retry_of_analysis_run_id", sa.UUID(), nullable=True),
        sa.Column("input_evidence_count", sa.Integer(), nullable=False, server_default="0"),
        sa.Column("latency_ms", sa.Integer(), nullable=True),
        sa.Column("input_tokens", sa.Integer(), nullable=True),
        sa.Column("output_tokens", sa.Integer(), nullable=True),
        sa.Column("estimated_cost", sa.Numeric(precision=12, scale=6), nullable=True),
        sa.Column("error_code", sa.String(length=120), nullable=True),
        sa.Column("error_message", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), onupdate=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["service_event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["requested_by_user_ref_id"], ["user_refs.id"], ondelete="SET NULL"),
        sa.ForeignKeyConstraint(["retry_of_analysis_run_id"], ["analysis_runs.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_analysis_runs_tenant_id"), "analysis_runs", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_analysis_runs_service_event_id"), "analysis_runs", ["service_event_id"], unique=False)
    op.create_index(op.f("ix_analysis_runs_status"), "analysis_runs", ["status"], unique=False)
    op.create_index(op.f("ix_analysis_runs_requested_by_user_ref_id"), "analysis_runs", ["requested_by_user_ref_id"], unique=False)
    op.create_index(op.f("ix_analysis_runs_requested_at"), "analysis_runs", ["requested_at"], unique=False)
    op.create_index(op.f("ix_analysis_runs_started_at"), "analysis_runs", ["started_at"], unique=False)
    op.create_index(op.f("ix_analysis_runs_completed_at"), "analysis_runs", ["completed_at"], unique=False)
    op.create_index(op.f("ix_analysis_runs_retry_of_analysis_run_id"), "analysis_runs", ["retry_of_analysis_run_id"], unique=False)
    op.create_index("ix_analysis_runs_tenant_status_requested", "analysis_runs", ["tenant_id", "status", "requested_at"], unique=False)

    op.create_table(
        "findings",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("service_event_id", sa.UUID(), nullable=False),
        sa.Column("analysis_run_id", sa.UUID(), nullable=False),
        sa.Column("finding_code", sa.String(length=80), nullable=False),
        sa.Column("title", sa.String(length=255), nullable=False),
        sa.Column("description", sa.Text(), nullable=True),
        sa.Column("component", sa.String(length=255), nullable=True),
        sa.Column("location", sa.String(length=255), nullable=True),
        sa.Column("confidence", sa.Numeric(precision=5, scale=2), nullable=True),
        sa.Column("evidence_sufficiency", evidence_sufficiency_enum, nullable=False),
        sa.Column("review_status", finding_review_status_enum, nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["service_event_id"], ["service_events.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["analysis_run_id"], ["analysis_runs.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_findings_tenant_id"), "findings", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_findings_service_event_id"), "findings", ["service_event_id"], unique=False)
    op.create_index(op.f("ix_findings_analysis_run_id"), "findings", ["analysis_run_id"], unique=False)
    op.create_index(op.f("ix_findings_finding_code"), "findings", ["finding_code"], unique=False)
    op.create_index(op.f("ix_findings_evidence_sufficiency"), "findings", ["evidence_sufficiency"], unique=False)
    op.create_index(op.f("ix_findings_review_status"), "findings", ["review_status"], unique=False)
    op.create_index("ix_findings_tenant_event_review", "findings", ["tenant_id", "service_event_id", "review_status"], unique=False)

    op.create_table(
        "finding_evidence",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("finding_id", sa.UUID(), nullable=False),
        sa.Column("evidence_id", sa.UUID(), nullable=False),
        sa.Column("relationship_type", sa.String(length=80), nullable=False, server_default="supporting"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["finding_id"], ["findings.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["evidence_id"], ["evidence.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("finding_id", "evidence_id", name="uq_finding_evidence_link"),
    )
    op.create_index(op.f("ix_finding_evidence_tenant_id"), "finding_evidence", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_finding_evidence_finding_id"), "finding_evidence", ["finding_id"], unique=False)
    op.create_index(op.f("ix_finding_evidence_evidence_id"), "finding_evidence", ["evidence_id"], unique=False)
    op.create_index("ix_finding_evidence_tenant_finding", "finding_evidence", ["tenant_id", "finding_id"], unique=False)
    op.create_index("ix_finding_evidence_tenant_evidence", "finding_evidence", ["tenant_id", "evidence_id"], unique=False)

    op.create_table(
        "finding_reviews",
        sa.Column("id", sa.UUID(), nullable=False),
        sa.Column("tenant_id", sa.UUID(), nullable=False),
        sa.Column("finding_id", sa.UUID(), nullable=False),
        sa.Column("decision", finding_review_decision_enum, nullable=False),
        sa.Column("reviewed_by_user_ref_id", sa.UUID(), nullable=True),
        sa.Column("reviewed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("reason_code", sa.String(length=80), nullable=True),
        sa.Column("comment", sa.Text(), nullable=True),
        sa.Column("modified_title", sa.String(length=255), nullable=True),
        sa.Column("modified_description", sa.Text(), nullable=True),
        sa.Column("modified_component", sa.String(length=255), nullable=True),
        sa.Column("modified_location", sa.String(length=255), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("now()"), nullable=False),
        sa.ForeignKeyConstraint(["tenant_id"], ["tenants.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["finding_id"], ["findings.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["reviewed_by_user_ref_id"], ["user_refs.id"], ondelete="SET NULL"),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(op.f("ix_finding_reviews_tenant_id"), "finding_reviews", ["tenant_id"], unique=False)
    op.create_index(op.f("ix_finding_reviews_finding_id"), "finding_reviews", ["finding_id"], unique=False)
    op.create_index(op.f("ix_finding_reviews_decision"), "finding_reviews", ["decision"], unique=False)
    op.create_index(op.f("ix_finding_reviews_reviewed_by_user_ref_id"), "finding_reviews", ["reviewed_by_user_ref_id"], unique=False)
    op.create_index(op.f("ix_finding_reviews_reviewed_at"), "finding_reviews", ["reviewed_at"], unique=False)
    op.create_index("ix_finding_reviews_finding_reviewed_at", "finding_reviews", ["finding_id", "reviewed_at"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_finding_reviews_finding_reviewed_at", table_name="finding_reviews")
    op.drop_index(op.f("ix_finding_reviews_reviewed_at"), table_name="finding_reviews")
    op.drop_index(op.f("ix_finding_reviews_reviewed_by_user_ref_id"), table_name="finding_reviews")
    op.drop_index(op.f("ix_finding_reviews_decision"), table_name="finding_reviews")
    op.drop_index(op.f("ix_finding_reviews_finding_id"), table_name="finding_reviews")
    op.drop_index(op.f("ix_finding_reviews_tenant_id"), table_name="finding_reviews")
    op.drop_table("finding_reviews")

    op.drop_index("ix_finding_evidence_tenant_evidence", table_name="finding_evidence")
    op.drop_index("ix_finding_evidence_tenant_finding", table_name="finding_evidence")
    op.drop_index(op.f("ix_finding_evidence_evidence_id"), table_name="finding_evidence")
    op.drop_index(op.f("ix_finding_evidence_finding_id"), table_name="finding_evidence")
    op.drop_index(op.f("ix_finding_evidence_tenant_id"), table_name="finding_evidence")
    op.drop_table("finding_evidence")

    op.drop_index("ix_findings_tenant_event_review", table_name="findings")
    op.drop_index(op.f("ix_findings_review_status"), table_name="findings")
    op.drop_index(op.f("ix_findings_evidence_sufficiency"), table_name="findings")
    op.drop_index(op.f("ix_findings_finding_code"), table_name="findings")
    op.drop_index(op.f("ix_findings_analysis_run_id"), table_name="findings")
    op.drop_index(op.f("ix_findings_service_event_id"), table_name="findings")
    op.drop_index(op.f("ix_findings_tenant_id"), table_name="findings")
    op.drop_table("findings")

    op.drop_index("ix_analysis_runs_tenant_status_requested", table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_retry_of_analysis_run_id"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_completed_at"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_started_at"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_requested_at"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_requested_by_user_ref_id"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_status"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_service_event_id"), table_name="analysis_runs")
    op.drop_index(op.f("ix_analysis_runs_tenant_id"), table_name="analysis_runs")
    op.drop_table("analysis_runs")

    op.drop_index("ix_evidence_assets_evidence_storage", table_name="evidence_assets")
    op.drop_index(op.f("ix_evidence_assets_storage_provider"), table_name="evidence_assets")
    op.drop_index(op.f("ix_evidence_assets_evidence_id"), table_name="evidence_assets")
    op.drop_index(op.f("ix_evidence_assets_tenant_id"), table_name="evidence_assets")
    op.drop_table("evidence_assets")

    op.drop_index("ix_evidence_type_status", table_name="evidence")
    op.drop_index("ix_evidence_tenant_event", table_name="evidence")
    op.drop_index(op.f("ix_evidence_captured_at"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_status"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_captured_by_user_ref_id"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_capture_source"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_evidence_type"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_service_event_id"), table_name="evidence")
    op.drop_index(op.f("ix_evidence_tenant_id"), table_name="evidence")
    op.drop_table("evidence")

    evidence_type_enum.drop(op.get_bind(), checkfirst=True)
    capture_source_enum.drop(op.get_bind(), checkfirst=True)
    evidence_status_enum.drop(op.get_bind(), checkfirst=True)
    analysis_run_status_enum.drop(op.get_bind(), checkfirst=True)
    evidence_sufficiency_enum.drop(op.get_bind(), checkfirst=True)
    finding_review_status_enum.drop(op.get_bind(), checkfirst=True)
    finding_review_decision_enum.drop(op.get_bind(), checkfirst=True)
