from __future__ import annotations

import math
import time
import uuid
from datetime import datetime, timezone

from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.evidence.analysis_provider import AIFindingProvider, FakeFindingProvider, ProviderExecutionError
from app.evidence.analysis_repository import get_analysis_run_for_tenant_event
from app.evidence.analysis_schemas import (
    AnalysisWorkItem,
    EvidenceAnalysisInput,
    FindingAnalysisRequest,
    FindingAnalysisResponse,
)
from app.evidence.finding_repository import add_finding, add_finding_evidence
from app.evidence.models import AnalysisRunStatus, EvidenceStatus
from app.evidence.repository import get_evidence_asset_for_tenant, get_evidence_for_tenant_service_event
from app.evidence.service import get_evidence_storage_provider


class AnalysisExecutionFailure(Exception):
    def __init__(self, error_code: str, error_message: str) -> None:
        super().__init__(error_message)
        self.error_code = error_code
        self.error_message = error_message


class FindingPersistenceFailure(Exception):
    pass


class AnalysisWorker:
    def __init__(self, *, provider: AIFindingProvider | None = None, storage_provider=None) -> None:
        self.provider = provider or FakeFindingProvider()
        self.storage_provider = storage_provider

    def execute(self, work_item: AnalysisWorkItem) -> None:
        try:
            if not self._claim(work_item):
                return

            started = time.perf_counter()
            request = self._resolve_request(work_item)
            provider_result = self.provider.analyze(request)
            response = FindingAnalysisResponse.model_validate(provider_result)
            if response.schema_version != request.schema_version:
                raise AnalysisExecutionFailure(
                    "INVALID_PROVIDER_RESPONSE",
                    "Analysis provider response schema version was invalid",
                )
            latency_ms = max(0, int((time.perf_counter() - started) * 1000))
            self._persist_success(work_item, response, latency_ms)
        except AnalysisExecutionFailure as exc:
            self._mark_failed(work_item, exc.error_code, exc.error_message)
        except ProviderExecutionError:
            self._mark_failed(work_item, "PROVIDER_ERROR", "Fake analysis provider failed")
        except ValidationError:
            self._mark_failed(work_item, "INVALID_PROVIDER_RESPONSE", "Analysis provider response was invalid")
        except FindingPersistenceFailure:
            self._mark_failed(work_item, "FINDING_PERSISTENCE_ERROR", "Finding persistence failed")
        except SQLAlchemyError:
            self._mark_failed(work_item, "FINDING_PERSISTENCE_ERROR", "Finding persistence failed")
        except Exception:
            self._mark_failed(work_item, "WORKER_ERROR", "Analysis worker failed")

    def _claim(self, work_item: AnalysisWorkItem) -> bool:
        session = SessionLocal()
        try:
            with session.begin():
                analysis_run = get_analysis_run_for_tenant_event(
                    session,
                    tenant_id=work_item.tenantId,
                    service_event_id=work_item.serviceEventId,
                    analysis_run_id=work_item.analysisRunId,
                    for_update=True,
                )
                if analysis_run is None or analysis_run.status != AnalysisRunStatus.QUEUED:
                    return False
                now = datetime.now(timezone.utc)
                analysis_run.status = AnalysisRunStatus.PROCESSING
                analysis_run.started_at = now
                analysis_run.updated_at = now
                session.flush()
                return True
        finally:
            session.close()

    def _resolve_request(self, work_item: AnalysisWorkItem) -> FindingAnalysisRequest:
        session = SessionLocal()
        try:
            with session.begin():
                storage_provider = self.storage_provider or get_evidence_storage_provider()
                inputs: list[EvidenceAnalysisInput] = []
                for evidence_id in work_item.evidenceIds:
                    evidence = get_evidence_for_tenant_service_event(
                        session,
                        tenant_id=work_item.tenantId,
                        service_event_id=work_item.serviceEventId,
                        evidence_id=evidence_id,
                    )
                    if evidence is None or evidence.status != EvidenceStatus.READY:
                        raise AnalysisExecutionFailure("EVIDENCE_NOT_READY", "Required evidence is not READY")
                    asset = get_evidence_asset_for_tenant(
                        session,
                        tenant_id=work_item.tenantId,
                        evidence_id=evidence.id,
                    )
                    if asset is None:
                        raise AnalysisExecutionFailure("EVIDENCE_ASSET_NOT_FOUND", "Required evidence asset was not found")
                    metadata = storage_provider.get_metadata(asset.storage_key)
                    if metadata is None or metadata.size != asset.file_size_bytes:
                        raise AnalysisExecutionFailure("STORAGE_OBJECT_NOT_FOUND", "Required evidence object could not be verified")
                    inputs.append(
                        EvidenceAnalysisInput(
                            evidence_id=evidence.id,
                            evidence_type=evidence.evidence_type,
                            media_type=asset.media_type,
                            file_name=asset.file_name,
                            file_size_bytes=asset.file_size_bytes,
                            checksum_sha256=asset.checksum_sha256,
                            width=asset.width,
                            height=asset.height,
                            duration_ms=asset.duration_ms,
                        )
                    )
                return FindingAnalysisRequest(
                    schema_version="s2.4",
                    service_event_id=work_item.serviceEventId,
                    evidence=tuple(inputs),
                )
        finally:
            session.close()

    def _persist_success(
        self,
        work_item: AnalysisWorkItem,
        response: FindingAnalysisResponse,
        latency_ms: int,
    ) -> None:
        session = SessionLocal()
        try:
            try:
                with session.begin():
                    analysis_run = get_analysis_run_for_tenant_event(
                        session,
                        tenant_id=work_item.tenantId,
                        service_event_id=work_item.serviceEventId,
                        analysis_run_id=work_item.analysisRunId,
                        for_update=True,
                    )
                    if analysis_run is None or analysis_run.status != AnalysisRunStatus.PROCESSING:
                        return
                    evidence_by_id = self._validate_response_and_evidence(session, work_item, response)
                    persisted_findings = []
                    for observation in response.observations:
                        finding = add_finding(
                            session,
                            tenant_id=work_item.tenantId,
                            service_event_id=work_item.serviceEventId,
                            analysis_run_id=work_item.analysisRunId,
                            finding_code=observation.finding_code.strip(),
                            title=observation.title.strip(),
                            description=observation.description,
                            component=observation.component,
                            location=observation.location,
                            confidence=observation.confidence,
                            evidence_sufficiency=observation.evidence_sufficiency,
                        )
                        persisted_findings.append((finding, observation.supporting_evidence_ids))
                    session.flush()
                    for finding, evidence_ids in persisted_findings:
                        for evidence_id in evidence_ids:
                            if evidence_id not in evidence_by_id:
                                raise AnalysisExecutionFailure("INVALID_EVIDENCE_REFERENCE", "Finding referenced invalid evidence")
                            add_finding_evidence(
                                session,
                                tenant_id=work_item.tenantId,
                                finding_id=finding.id,
                                evidence_id=evidence_id,
                            )
                    now = datetime.now(timezone.utc)
                    analysis_run.provider = response.provider.strip()
                    analysis_run.model = response.model.strip()
                    analysis_run.model_version = response.model_version.strip()
                    analysis_run.prompt_name = "s2.4-fake-analysis"
                    analysis_run.prompt_version = "1"
                    analysis_run.schema_version = response.schema_version
                    analysis_run.latency_ms = latency_ms
                    analysis_run.input_tokens = response.usage.input_tokens
                    analysis_run.output_tokens = response.usage.output_tokens
                    analysis_run.status = AnalysisRunStatus.SUCCEEDED
                    analysis_run.completed_at = now
                    analysis_run.updated_at = now
                    session.flush()
            except AnalysisExecutionFailure:
                raise
            except Exception as exc:
                raise FindingPersistenceFailure from exc
        finally:
            session.close()

    def _validate_response_and_evidence(
        self,
        session: Session,
        work_item: AnalysisWorkItem,
        response: FindingAnalysisResponse,
    ) -> dict[uuid.UUID, object]:
        if not response.provider.strip() or not response.model.strip() or not response.model_version.strip():
            raise AnalysisExecutionFailure("INVALID_PROVIDER_RESPONSE", "Provider metadata is required")
        frozen_evidence_ids = set(work_item.evidenceIds)
        evidence_by_id: dict[uuid.UUID, object] = {}
        for observation in response.observations:
            if not observation.finding_code.strip() or not observation.title.strip():
                raise AnalysisExecutionFailure("INVALID_FINDING", "Finding code and title are required")
            if observation.confidence is not None and not math.isfinite(observation.confidence):
                raise AnalysisExecutionFailure("INVALID_CONFIDENCE", "Finding confidence was invalid")
            if len(set(observation.supporting_evidence_ids)) != len(observation.supporting_evidence_ids):
                raise AnalysisExecutionFailure("INVALID_EVIDENCE_REFERENCE", "Finding referenced duplicate evidence")
            if not observation.supporting_evidence_ids:
                raise AnalysisExecutionFailure("INVALID_EVIDENCE_REFERENCE", "Finding must reference supporting evidence")
            for evidence_id in observation.supporting_evidence_ids:
                if evidence_id not in frozen_evidence_ids:
                    raise AnalysisExecutionFailure("INVALID_EVIDENCE_REFERENCE", "Finding referenced evidence outside the analysis run")
                if evidence_id not in evidence_by_id:
                    evidence = get_evidence_for_tenant_service_event(
                        session,
                        tenant_id=work_item.tenantId,
                        service_event_id=work_item.serviceEventId,
                        evidence_id=evidence_id,
                    )
                    if evidence is None or evidence.status != EvidenceStatus.READY:
                        raise AnalysisExecutionFailure("EVIDENCE_NOT_READY", "Supporting evidence is not READY")
                    evidence_by_id[evidence_id] = evidence
        return evidence_by_id

    def _mark_failed(self, work_item: AnalysisWorkItem, error_code: str, error_message: str) -> None:
        session = SessionLocal()
        try:
            with session.begin():
                analysis_run = get_analysis_run_for_tenant_event(
                    session,
                    tenant_id=work_item.tenantId,
                    service_event_id=work_item.serviceEventId,
                    analysis_run_id=work_item.analysisRunId,
                    for_update=True,
                )
                if analysis_run is None or analysis_run.status not in {
                    AnalysisRunStatus.QUEUED,
                    AnalysisRunStatus.PROCESSING,
                }:
                    return
                now = datetime.now(timezone.utc)
                analysis_run.status = AnalysisRunStatus.FAILED
                analysis_run.completed_at = now
                analysis_run.updated_at = now
                analysis_run.error_code = error_code
                analysis_run.error_message = error_message
                session.flush()
        finally:
            session.close()
