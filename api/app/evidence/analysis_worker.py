from __future__ import annotations

import time
import uuid
from datetime import datetime, timezone

from pydantic import ValidationError
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
from app.evidence.models import AnalysisRunStatus, EvidenceStatus
from app.evidence.repository import get_evidence_asset_for_tenant, get_evidence_for_tenant_service_event
from app.evidence.service import get_evidence_storage_provider


class AnalysisExecutionFailure(Exception):
    def __init__(self, error_code: str, error_message: str) -> None:
        super().__init__(error_message)
        self.error_code = error_code
        self.error_message = error_message


class AnalysisWorker:
    def __init__(self, *, provider: AIFindingProvider | None = None, storage_provider=None) -> None:
        self.provider = provider or FakeFindingProvider()
        self.storage_provider = storage_provider

    def execute(self, work_item: AnalysisWorkItem) -> None:
        claimed = False
        try:
            claimed = self._claim(work_item)
            if not claimed:
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
            self._mark_succeeded(work_item, response, latency_ms)
        except AnalysisExecutionFailure as exc:
            self._mark_failed(work_item, exc.error_code, exc.error_message)
        except ProviderExecutionError:
            self._mark_failed(work_item, "PROVIDER_ERROR", "Fake analysis provider failed")
        except ValidationError:
            self._mark_failed(work_item, "INVALID_PROVIDER_RESPONSE", "Analysis provider response was invalid")
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
                    if metadata is None:
                        raise AnalysisExecutionFailure("STORAGE_OBJECT_NOT_FOUND", "Required evidence object was not found")
                    if metadata.size != asset.file_size_bytes:
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

    def _mark_succeeded(
        self,
        work_item: AnalysisWorkItem,
        response: FindingAnalysisResponse,
        latency_ms: int,
    ) -> None:
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
                if analysis_run is None or analysis_run.status != AnalysisRunStatus.PROCESSING:
                    return
                now = datetime.now(timezone.utc)
                analysis_run.provider = response.provider
                analysis_run.model = response.model
                analysis_run.model_version = response.model_version
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
        finally:
            session.close()

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
