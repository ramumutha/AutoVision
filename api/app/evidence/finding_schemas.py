from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, model_validator

from app.evidence.models import FindingReviewDecision, FindingReviewStatus, EvidenceSufficiency


class FindingReviewCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="forbid")

    decision: FindingReviewDecision
    reasonCode: str | None = Field(default=None, max_length=80)
    comment: str | None = None
    modifiedTitle: str | None = Field(default=None, max_length=255)
    modifiedDescription: str | None = None
    modifiedComponent: str | None = Field(default=None, max_length=255)
    modifiedLocation: str | None = Field(default=None, max_length=255)

    @model_validator(mode="after")
    def validate_decision_fields(self) -> FindingReviewCreate:
        modified_fields = (
            self.modifiedTitle,
            self.modifiedDescription,
            self.modifiedComponent,
            self.modifiedLocation,
        )
        if self.decision == FindingReviewDecision.MODIFIED and not any(value is not None for value in modified_fields):
            raise ValueError("MODIFIED review requires at least one modified field")
        if self.decision in {FindingReviewDecision.CONFIRMED, FindingReviewDecision.REJECTED} and any(
            value is not None for value in modified_fields
        ):
            raise ValueError(f"{self.decision.value} review cannot include modified fields")
        return self


class FindingReviewRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    decision: FindingReviewDecision
    reviewedByUserRefId: UUID | None = None
    reviewedAt: datetime | None = None
    reasonCode: str | None = None
    comment: str | None = None
    modifiedTitle: str | None = None
    modifiedDescription: str | None = None
    modifiedComponent: str | None = None
    modifiedLocation: str | None = None
    createdAt: datetime


class FindingRead(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    id: UUID
    tenantId: UUID
    serviceEventId: UUID
    analysisRunId: UUID
    findingCode: str
    title: str
    description: str | None = None
    component: str | None = None
    location: str | None = None
    confidence: Decimal | None = None
    evidenceSufficiency: EvidenceSufficiency
    reviewStatus: FindingReviewStatus
    createdAt: datetime
    supportingEvidenceIds: list[UUID] = Field(default_factory=list)
    latestReview: FindingReviewRead | None = None
