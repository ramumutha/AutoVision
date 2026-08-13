from __future__ import annotations

from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field

from app.vehicle.models import PowertrainType, VehicleClass


class VehicleIdentifierRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    identifierType: str
    identifierValue: str
    isPrimary: bool


class UsageSnapshotRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    recordedAt: str
    odometerKm: float | None = None
    engineHours: float | None = None
    fuelLevelPct: float | None = None
    dataSource: str | None = None
    payload: dict | None = None


class VehicleRead(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    id: UUID
    vehicleClass: VehicleClass
    powertrain: PowertrainType
    modelName: str | None = None
    year: int | None = None
    color: str | None = None
    isActive: bool
    identifiers: list[VehicleIdentifierRead] = Field(default_factory=list)
    latestUsageSnapshot: UsageSnapshotRead | None = None


class VehicleListResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    vehicles: list[VehicleRead]
