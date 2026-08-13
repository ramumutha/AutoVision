from __future__ import annotations

import uuid
from typing import Any

from app.vehicle.models import Vehicle, UsageSnapshot
from app.vehicle.repository import get_vehicle_for_tenant, list_vehicles_for_tenant
from app.vehicle.schemas import UsageSnapshotRead, VehicleIdentifierRead, VehicleRead


def _serialize_identifiers(vehicle: Vehicle) -> list[VehicleIdentifierRead]:
    identifiers = sorted(
        vehicle.vehicle_identifiers,
        key=lambda item: (0 if item.is_primary else 1, item.identifier_type, item.identifier_value),
    )
    return [
        VehicleIdentifierRead.model_validate(
            {
                "id": item.id,
                "identifierType": item.identifier_type,
                "identifierValue": item.identifier_value,
                "isPrimary": item.is_primary,
            }
        )
        for item in identifiers
    ]


def _latest_usage_snapshot(vehicle: Vehicle) -> UsageSnapshotRead | None:
    latest_snapshot: UsageSnapshot | None = None
    for snapshot in vehicle.usage_snapshots:
        if latest_snapshot is None or snapshot.recorded_at > latest_snapshot.recorded_at:
            latest_snapshot = snapshot
    if latest_snapshot is None:
        return None
    return UsageSnapshotRead.model_validate(
        {
            "id": latest_snapshot.id,
            "recordedAt": latest_snapshot.recorded_at.isoformat(),
            "odometerKm": float(latest_snapshot.odometer_km) if latest_snapshot.odometer_km is not None else None,
            "engineHours": float(latest_snapshot.engine_hours) if latest_snapshot.engine_hours is not None else None,
            "fuelLevelPct": float(latest_snapshot.fuel_level_pct) if latest_snapshot.fuel_level_pct is not None else None,
            "dataSource": latest_snapshot.data_source,
            "payload": latest_snapshot.payload,
        }
    )


def _serialize_vehicle(vehicle: Vehicle) -> VehicleRead:
    latest_snapshot = _latest_usage_snapshot(vehicle)
    return VehicleRead.model_validate(
        {
            "id": vehicle.id,
            "vehicleClass": vehicle.vehicle_class,
            "powertrain": vehicle.powertrain,
            "modelName": vehicle.model_name,
            "year": vehicle.year,
            "color": vehicle.color,
            "isActive": vehicle.is_active,
            "identifiers": [
                {
                    "id": item.id,
                    "identifierType": item.identifier_type,
                    "identifierValue": item.identifier_value,
                    "isPrimary": item.is_primary,
                }
                for item in sorted(
                    vehicle.vehicle_identifiers,
                    key=lambda item: (0 if item.is_primary else 1, item.identifier_type, item.identifier_value),
                )
            ],
            "latestUsageSnapshot": latest_snapshot.model_dump() if latest_snapshot else None,
        }
    )


def get_vehicle_for_scope(session, tenant_id: uuid.UUID, vehicle_id: uuid.UUID) -> VehicleRead | None:
    vehicle = get_vehicle_for_tenant(session, tenant_id, vehicle_id)
    if vehicle is None:
        return None
    return _serialize_vehicle(vehicle)


def list_vehicles_for_scope(
    session,
    tenant_id: uuid.UUID,
    *,
    identifier: str | None = None,
    model_name: str | None = None,
    vehicle_class: str | None = None,
    powertrain: str | None = None,
    is_active: bool | None = None,
) -> list[VehicleRead]:
    vehicles = list_vehicles_for_tenant(
        session,
        tenant_id,
        identifier=identifier,
        model_name=model_name,
        vehicle_class=vehicle_class,
        powertrain=powertrain,
        is_active=is_active,
    )
    return [_serialize_vehicle(vehicle) for vehicle in vehicles]
