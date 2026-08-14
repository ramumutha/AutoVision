from __future__ import annotations

from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field


class ComplaintCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    vehicleId: UUID
    source: str = Field(min_length=1, max_length=120)
    originalComplaint: str = Field(min_length=1)
    structuredSummary: str | None = None
    language: str | None = Field(default=None, max_length=20)
    capturedBy: str | None = Field(default=None, max_length=120)


class ComplaintPatch(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    originalComplaint: str | None = None
    structuredSummary: str | None = None
    language: str | None = Field(default=None, max_length=20)
    capturedBy: str | None = Field(default=None, max_length=120)


class ComplaintRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    originalText: str
    structuredSummary: str | None = None
    language: str | None = None
    capturedBy: str | None = None
    capturedAt: datetime
    revision: int


class ServiceEventAssignmentCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    roleCode: str = Field(min_length=1, max_length=80)
    userRef: UUID | None = None


class ServiceEventAssignmentRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    eventId: UUID
    roleCode: str
    userRef: UUID | None = None
    assignedAt: datetime


class ServiceEventContextCreate(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    contextType: str = Field(min_length=1, max_length=80)
    sourceRef: str | None = Field(default=None, max_length=255)
    snapshotJson: dict = Field(default_factory=dict)


class ServiceEventContextRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    eventId: UUID
    contextType: str
    sourceRef: str | None = None
    snapshotJson: dict
    capturedAt: datetime


class ServiceEventRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    vehicleId: UUID
    source: str
    state: str
    revision: int
    openedAt: datetime | None = None
    createdAt: datetime
    updatedAt: datetime
    complaint: ComplaintRead | None = None
    assignments: list[ServiceEventAssignmentRead] = []
    contexts: list[ServiceEventContextRead] = []
