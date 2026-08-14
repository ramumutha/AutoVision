from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.evidence.models import AnalysisRunStatus, EvidenceSufficiency, EvidenceType


class AnalysisRunRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    tenantId: UUID
    serviceEventId: UUID
    status: AnalysisRunStatus
    provider: str | None = None
    model: str | None = None
    modelVersion: str | None = None
    promptName: str | None = None
    promptVersion: str | None = None
    schemaVersion: str | None = None
    requestedByUserRefId: UUID | None = None
    requestedAt: datetime
    startedAt: datetime | None = None
    completedAt: datetime | None = None
    retryOfAnalysisRunId: UUID | None = None
    inputEvidenceCount: int
    latencyMs: int | None = None
    inputTokens: int | None = None
    outputTokens: int | None = None
    estimatedCost: Decimal | None = None
    errorCode: str | None = None
    errorMessage: str | None = None
    createdAt: datetime
    updatedAt: datetime


class AnalysisWorkItem(BaseModel):
    model_config = ConfigDict(frozen=True, populate_by_name=True)

    analysisRunId: UUID
    tenantId: UUID
    serviceEventId: UUID
    evidenceIds: tuple[UUID, ...] = Field(default_factory=tuple)


class EvidenceAnalysisInput(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    evidence_id: UUID
    evidence_type: EvidenceType
    media_type: str
    file_name: str
    file_size_bytes: int
    checksum_sha256: str
    width: int | None = None
    height: int | None = None
    duration_ms: int | None = None


class FindingAnalysisRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    schema_version: str
    service_event_id: UUID
    evidence: tuple[EvidenceAnalysisInput, ...]


class ProviderUsage(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    input_tokens: int | None = None
    output_tokens: int | None = None


class AnalysisObservation(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True)

    finding_code: str
    title: str
    description: str
    component: str | None = None
    location: str | None = None
    confidence: float | None = Field(default=None, ge=0.0, le=1.0)
    evidence_sufficiency: EvidenceSufficiency
    supporting_evidence_ids: tuple[UUID, ...] = Field(
        default_factory=tuple,
        alias="supportingEvidenceIds",
    )


class FindingAnalysisResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True)

    schema_version: str
    provider: str
    model: str
    model_version: str
    observations: tuple[AnalysisObservation, ...]
    evidence_assessment: EvidenceSufficiency
    warnings: tuple[str, ...] = Field(default_factory=tuple)
    usage: ProviderUsage = Field(default_factory=ProviderUsage)
