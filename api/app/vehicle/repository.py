from __future__ import annotations

import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session, selectinload

from app.vehicle.models import Vehicle, VehicleIdentifier


def get_vehicle_for_tenant(session: Session, tenant_id: uuid.UUID, vehicle_id: uuid.UUID) -> Vehicle | None:
    return session.execute(
        select(Vehicle)
        .options(selectinload(Vehicle.vehicle_identifiers), selectinload(Vehicle.usage_snapshots))
        .where(Vehicle.tenant_id == tenant_id, Vehicle.id == vehicle_id)
    ).scalar_one_or_none()


def list_vehicles_for_tenant(
    session: Session,
    tenant_id: uuid.UUID,
    *,
    identifier: str | None = None,
    model_name: str | None = None,
    vehicle_class: str | None = None,
    powertrain: str | None = None,
    is_active: bool | None = None,
) -> list[Vehicle]:
    query = (
        select(Vehicle)
        .options(selectinload(Vehicle.vehicle_identifiers), selectinload(Vehicle.usage_snapshots))
        .where(Vehicle.tenant_id == tenant_id)
    )

    if identifier:
        query = (
            query.join(VehicleIdentifier, VehicleIdentifier.vehicle_id == Vehicle.id)
            .where(VehicleIdentifier.identifier_value == identifier)
            .distinct()
        )
    if model_name:
        query = query.where(Vehicle.model_name == model_name)
    if vehicle_class:
        query = query.where(Vehicle.vehicle_class == vehicle_class)
    if powertrain:
        query = query.where(Vehicle.powertrain == powertrain)
    if is_active is not None:
        query = query.where(Vehicle.is_active == is_active)

    return list(session.execute(query).scalars().all())
