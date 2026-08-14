from __future__ import annotations

import enum
import uuid
from datetime import datetime

from sqlalchemy import DateTime, Enum, ForeignKey, Index, Numeric, String, Text, UniqueConstraint, func
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.core.database import Base
from app.core.models import Tenant


class EvidenceType(str, enum.Enum):
    IMAGE = "IMAGE"
    VIDEO = "VIDEO"
    DOCUMENT = "DOCUMENT"
    OTHER = "OTHER"


class CaptureSource(str, enum.Enum):
    UPLOAD = "UPLOAD"
    CAMERA = "CAMERA"
    MOBILE_CAPTURE = "MOBILE_CAPTURE"
    SYSTEM_IMPORT = "SYSTEM_IMPORT"


class EvidenceStatus(str, enum.Enum):
    PENDING_UPLOAD = "PENDING_UPLOAD"
    UPLOADED = "UPLOADED"
    READY = "READY"
    PROCESSING = "PROCESSING"
    PROCESSED = "PROCESSED"
    UPLOAD_FAILED = "UPLOAD_FAILED"
    PROCESSING_FAILED = "PROCESSING_FAILED"


class AnalysisRunStatus(str, enum.Enum):
    QUEUED = "QUEUED"
    PROCESSING = "PROCESSING"
    SUCCEEDED = "SUCCEEDED"
    FAILED = "FAILED"


class EvidenceSufficiency(str, enum.Enum):
    SUFFICIENT = "SUFFICIENT"
    INSUFFICIENT = "INSUFFICIENT"
    PARTIAL = "PARTIAL"


class FindingReviewStatus(str, enum.Enum):
    PENDING_REVIEW = "PENDING_REVIEW"
    CONFIRMED = "CONFIRMED"
    MODIFIED = "MODIFIED"
    REJECTED = "REJECTED"


class FindingReviewDecision(str, enum.Enum):
    CONFIRMED = "CONFIRMED"
    MODIFIED = "MODIFIED"
    REJECTED = "REJECTED"


class Evidence(Base):
    __tablename__ = "evidence"
    __table_args__ = (
        Index("ix_evidence_tenant_event", "tenant_id", "service_event_id"),
        Index("ix_evidence_type_status", "tenant_id", "evidence_type", "status"),
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
    service_event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    evidence_type: Mapped[EvidenceType] = mapped_column(
        Enum(EvidenceType, name="evidence_type", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    capture_source: Mapped[CaptureSource] = mapped_column(
        Enum(CaptureSource, name="capture_source", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
    )
    title: Mapped[str | None] = mapped_column(String(255), nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    captured_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    captured_by_user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    status: Mapped[EvidenceStatus] = mapped_column(
        Enum(EvidenceStatus, name="evidence_status", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
        default=EvidenceStatus.PENDING_UPLOAD,
    )
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

    tenant: Mapped[Tenant] = relationship(back_populates="evidence")
    event: Mapped["ServiceEvent"] = relationship(back_populates="evidence")
    assets: Mapped[list["EvidenceAsset"]] = relationship(back_populates="evidence")
    finding_links: Mapped[list["FindingEvidence"]] = relationship(back_populates="evidence")


class EvidenceAsset(Base):
    __tablename__ = "evidence_assets"
    __table_args__ = (
        Index("ix_evidence_assets_evidence_storage", "evidence_id", "storage_provider"),
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
    evidence_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("evidence.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    storage_provider: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    storage_key: Mapped[str] = mapped_column(String(1024), nullable=False)
    file_name: Mapped[str] = mapped_column(String(255), nullable=False)
    media_type: Mapped[str] = mapped_column(String(120), nullable=False)
    file_size_bytes: Mapped[int] = mapped_column(nullable=False)
    checksum_sha256: Mapped[str] = mapped_column(String(128), nullable=False)
    width: Mapped[int | None] = mapped_column(nullable=True)
    height: Mapped[int | None] = mapped_column(nullable=True)
    duration_ms: Mapped[int | None] = mapped_column(nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="evidence_assets")
    evidence: Mapped[Evidence] = relationship(back_populates="assets")


class AnalysisRun(Base):
    __tablename__ = "analysis_runs"
    __table_args__ = (
        Index("ix_analysis_runs_tenant_status_requested", "tenant_id", "status", "requested_at"),
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
    service_event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    status: Mapped[AnalysisRunStatus] = mapped_column(
        Enum(AnalysisRunStatus, name="analysis_run_status", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
        default=AnalysisRunStatus.QUEUED,
    )
    provider: Mapped[str | None] = mapped_column(String(120), nullable=True)
    model: Mapped[str | None] = mapped_column(String(160), nullable=True)
    model_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    prompt_name: Mapped[str | None] = mapped_column(String(160), nullable=True)
    prompt_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    schema_version: Mapped[str | None] = mapped_column(String(80), nullable=True)
    requested_by_user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    requested_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    retry_of_analysis_run_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("analysis_runs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    input_evidence_count: Mapped[int] = mapped_column(default=0, nullable=False)
    latency_ms: Mapped[int | None] = mapped_column(nullable=True)
    input_tokens: Mapped[int | None] = mapped_column(nullable=True)
    output_tokens: Mapped[int | None] = mapped_column(nullable=True)
    estimated_cost: Mapped[float | None] = mapped_column(Numeric(12, 6), nullable=True)
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

    tenant: Mapped[Tenant] = relationship(back_populates="analysis_runs")
    event: Mapped["ServiceEvent"] = relationship(back_populates="analysis_runs")
    findings: Mapped[list["Finding"]] = relationship(back_populates="analysis_run")
    prediction_runs: Mapped[list["PredictionRun"]] = relationship(back_populates="analysis_run")
    retry_of: Mapped["AnalysisRun | None"] = relationship(back_populates="retries", remote_side="AnalysisRun.id")
    retries: Mapped[list["AnalysisRun"]] = relationship(back_populates="retry_of", remote_side="AnalysisRun.retry_of_analysis_run_id")


class Finding(Base):
    __tablename__ = "findings"
    __table_args__ = (
        Index("ix_findings_tenant_event_review", "tenant_id", "service_event_id", "review_status"),
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
    service_event_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("service_events.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    analysis_run_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("analysis_runs.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    finding_code: Mapped[str] = mapped_column(String(80), nullable=False, index=True)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    component: Mapped[str | None] = mapped_column(String(255), nullable=True)
    location: Mapped[str | None] = mapped_column(String(255), nullable=True)
    confidence: Mapped[float | None] = mapped_column(Numeric(5, 2), nullable=True)
    evidence_sufficiency: Mapped[EvidenceSufficiency] = mapped_column(
        Enum(EvidenceSufficiency, name="evidence_sufficiency", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
        default=EvidenceSufficiency.PARTIAL,
    )
    review_status: Mapped[FindingReviewStatus] = mapped_column(
        Enum(FindingReviewStatus, name="finding_review_status", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
        default=FindingReviewStatus.PENDING_REVIEW,
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="findings")
    event: Mapped["ServiceEvent"] = relationship(back_populates="findings")
    analysis_run: Mapped[AnalysisRun] = relationship(back_populates="findings")
    evidence_links: Mapped[list["FindingEvidence"]] = relationship(back_populates="finding")
    reviews: Mapped[list["FindingReview"]] = relationship(back_populates="finding")


class FindingEvidence(Base):
    __tablename__ = "finding_evidence"
    __table_args__ = (
        Index("ix_finding_evidence_tenant_finding", "tenant_id", "finding_id"),
        Index("ix_finding_evidence_tenant_evidence", "tenant_id", "evidence_id"),
        UniqueConstraint("finding_id", "evidence_id", name="uq_finding_evidence_link"),
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
    finding_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("findings.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    evidence_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("evidence.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    relationship_type: Mapped[str] = mapped_column(String(80), nullable=False, default="supporting")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="finding_evidence")
    finding: Mapped[Finding] = relationship(back_populates="evidence_links")
    evidence: Mapped[Evidence] = relationship(back_populates="finding_links")


class FindingReview(Base):
    __tablename__ = "finding_reviews"
    __table_args__ = (
        Index("ix_finding_reviews_finding_reviewed_at", "finding_id", "reviewed_at"),
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
    finding_id: Mapped[uuid.UUID] = mapped_column(
        ForeignKey("findings.id", ondelete="CASCADE"),
        nullable=False,
        index=True,
    )
    decision: Mapped[FindingReviewDecision] = mapped_column(
        Enum(FindingReviewDecision, name="finding_review_decision", native_enum=True, create_constraint=True),
        nullable=False,
        index=True,
        default=FindingReviewDecision.CONFIRMED,
    )
    reviewed_by_user_ref_id: Mapped[uuid.UUID | None] = mapped_column(
        ForeignKey("user_refs.id", ondelete="SET NULL"),
        nullable=True,
        index=True,
    )
    reviewed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True), nullable=True, index=True)
    reason_code: Mapped[str | None] = mapped_column(String(80), nullable=True, index=True)
    comment: Mapped[str | None] = mapped_column(Text, nullable=True)
    modified_title: Mapped[str | None] = mapped_column(String(255), nullable=True)
    modified_description: Mapped[str | None] = mapped_column(Text, nullable=True)
    modified_component: Mapped[str | None] = mapped_column(String(255), nullable=True)
    modified_location: Mapped[str | None] = mapped_column(String(255), nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        server_default=func.now(),
        nullable=False,
    )

    tenant: Mapped[Tenant] = relationship(back_populates="finding_reviews")
    finding: Mapped[Finding] = relationship(back_populates="reviews")


__all__ = [
    "Evidence",
    "EvidenceType",
    "CaptureSource",
    "EvidenceStatus",
    "EvidenceAsset",
    "AnalysisRun",
    "AnalysisRunStatus",
    "EvidenceSufficiency",
    "Finding",
    "FindingEvidence",
    "FindingReview",
    "FindingReviewStatus",
    "FindingReviewDecision",
]
