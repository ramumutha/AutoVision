from __future__ import annotations

from sqlalchemy import UniqueConstraint

from app.core.database import Base
from app.evidence.models import (
    AnalysisRun,
    AnalysisRunStatus,
    CaptureSource,
    Evidence,
    EvidenceAsset,
    EvidenceStatus,
    EvidenceType,
    Finding,
    FindingEvidence,
    FindingReview,
    FindingReviewDecision,
    FindingReviewStatus,
)


def test_s2_1_tables_are_registered() -> None:
    table_names = {table.name for table in Base.metadata.sorted_tables}
    expected = {"evidence", "evidence_assets", "analysis_runs", "findings", "finding_evidence", "finding_reviews"}
    assert expected.issubset(table_names)

    for model in (Evidence, EvidenceAsset, AnalysisRun, Finding, FindingEvidence, FindingReview):
        assert model.__table__.name in table_names


def test_s2_1_evidence_contract() -> None:
    evidence_columns = set(Evidence.__table__.columns.keys())
    required = {
        "id",
        "tenant_id",
        "service_event_id",
        "evidence_type",
        "capture_source",
        "title",
        "description",
        "captured_at",
        "captured_by_user_ref_id",
        "status",
        "created_at",
        "updated_at",
    }
    assert required.issubset(evidence_columns)
    assert Evidence.__table__.columns["service_event_id"].nullable is False
    assert Evidence.__table__.columns["captured_by_user_ref_id"].nullable is True
    assert "content_ref" not in evidence_columns
    assert "metadata_json" not in evidence_columns
    assert "vehicle_id" not in evidence_columns

    assert EvidenceType.IMAGE.value == "IMAGE"
    assert CaptureSource.UPLOAD.value == "UPLOAD"
    assert EvidenceStatus.PROCESSED.value == "PROCESSED"


def test_s2_1_evidence_asset_contract() -> None:
    asset_columns = set(EvidenceAsset.__table__.columns.keys())
    required = {
        "id",
        "tenant_id",
        "evidence_id",
        "storage_provider",
        "storage_key",
        "file_name",
        "media_type",
        "file_size_bytes",
        "checksum_sha256",
        "created_at",
    }
    assert required.issubset(asset_columns)
    assert EvidenceAsset.__table__.columns["evidence_id"].nullable is False
    assert EvidenceAsset.__table__.columns["tenant_id"].nullable is False
    assert "storage_uri" not in asset_columns
    assert "mime_type" not in asset_columns
    assert "is_primary" not in asset_columns
    assert "binary_data" not in asset_columns


def test_s2_1_analysis_run_contract() -> None:
    run_columns = set(AnalysisRun.__table__.columns.keys())
    required = {
        "id",
        "tenant_id",
        "service_event_id",
        "status",
        "provider",
        "model",
        "model_version",
        "prompt_name",
        "prompt_version",
        "schema_version",
        "requested_by_user_ref_id",
        "requested_at",
        "started_at",
        "completed_at",
        "retry_of_analysis_run_id",
        "input_evidence_count",
        "latency_ms",
        "input_tokens",
        "output_tokens",
        "estimated_cost",
        "error_code",
        "error_message",
        "created_at",
        "updated_at",
    }
    assert required.issubset(run_columns)
    assert AnalysisRun.__table__.columns["service_event_id"].nullable is False
    assert AnalysisRun.__table__.columns["input_evidence_count"].nullable is False
    assert AnalysisRun.status.type.enums == ["QUEUED", "PROCESSING", "SUCCEEDED", "FAILED"]
    assert "run_name" not in run_columns
    assert "model_name" not in run_columns
    assert "prompt_ref" not in run_columns
    assert "summary" not in run_columns

    retry_fk_targets = {fk.target_fullname for fk in AnalysisRun.__table__.foreign_keys}
    assert "analysis_runs.id" in retry_fk_targets


def test_s2_1_finding_and_review_contract() -> None:
    finding_columns = set(Finding.__table__.columns.keys())
    required = {
        "id",
        "tenant_id",
        "service_event_id",
        "analysis_run_id",
        "finding_code",
        "title",
        "description",
        "component",
        "location",
        "confidence",
        "evidence_sufficiency",
        "review_status",
        "created_at",
    }
    assert required.issubset(finding_columns)
    assert Finding.__table__.columns["analysis_run_id"].nullable is False
    assert "severity" not in finding_columns
    assert "priority" not in finding_columns
    assert "repair_recommendation" not in finding_columns
    assert "warranty_decision" not in finding_columns

    review_columns = set(FindingReview.__table__.columns.keys())
    review_required = {
        "id",
        "tenant_id",
        "finding_id",
        "decision",
        "reviewed_by_user_ref_id",
        "reviewed_at",
        "reason_code",
        "comment",
        "modified_title",
        "modified_description",
        "modified_component",
        "modified_location",
        "created_at",
    }
    assert review_required.issubset(review_columns)
    assert "severity" not in review_columns
    assert "finding_status" not in review_columns

    unique_constraints = {
        tuple(constraint.columns.keys()) for constraint in FindingEvidence.__table__.constraints if isinstance(constraint, UniqueConstraint)
    }
    assert ("finding_id", "evidence_id") in unique_constraints

    finding_fk_targets = {fk.target_fullname for fk in Finding.__table__.foreign_keys}
    assert "analysis_runs.id" in finding_fk_targets
    assert "service_events.id" in finding_fk_targets

    evidence_fk_targets = {fk.target_fullname for fk in Evidence.__table__.foreign_keys}
    assert "service_events.id" in evidence_fk_targets
    assert "vehicles.id" not in evidence_fk_targets


def test_s2_1_forbidden_concepts_are_absent() -> None:
    forbidden_tables = {"repair_orders", "jobs", "parts", "outcomes", "soe_events", "inspection_findings"}
    assert forbidden_tables.isdisjoint({table.name for table in Base.metadata.sorted_tables})

    forbidden_columns = {
        "finding_severity",
        "finding_status",
        "severity",
        "status",
        "repair_priority",
        "repair_recommendation",
        "estimated_cost",
        "risk_level",
        "safe_to_drive",
        "recommended_action",
    }
    assert not forbidden_columns.intersection(Finding.__table__.columns.keys())

    assert FindingReviewStatus.PENDING_REVIEW.value == "PENDING_REVIEW"
    assert FindingReviewDecision.CONFIRMED.value == "CONFIRMED"
