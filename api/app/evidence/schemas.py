from __future__ import annotations

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict

from app.evidence.models import CaptureSource, EvidenceStatus, EvidenceType


class EvidenceCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    evidenceType: EvidenceType
    captureSource: CaptureSource
    title: str | None = None
    description: str | None = None
    capturedAt: datetime | None = None


class EvidenceRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    tenantId: UUID
    serviceEventId: UUID
    evidenceType: EvidenceType
    captureSource: CaptureSource
    title: str | None = None
    description: str | None = None
    capturedAt: datetime | None = None
    capturedByUserRefId: UUID | None = None
    status: EvidenceStatus
    createdAt: datetime
    updatedAt: datetime
