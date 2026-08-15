from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict


class PredictionWorkItem(BaseModel):
    model_config = ConfigDict(
        frozen=True,
        extra="forbid",
        populate_by_name=True,
    )

    predictionRunId: UUID
    tenantId: UUID
    vehicleId: UUID
    serviceEventId: UUID | None = None
    analysisRunId: UUID | None = None
    correlationId: str | None = None
