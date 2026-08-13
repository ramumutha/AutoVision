from __future__ import annotations

from uuid import UUID

from fastapi import APIRouter, Depends, Header, HTTPException, Query
from sqlalchemy.orm import Session

from app.core.database import SessionLocal
from app.vehicle.models import PowertrainType, VehicleClass
from app.vehicle.schemas import VehicleRead
from app.vehicle.service import get_vehicle_for_scope, list_vehicles_for_scope


router = APIRouter(prefix="/v1", tags=["vehicles"])


def get_db() -> Session:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


def get_tenant_id(x_tenant_id: UUID = Header(..., alias="X-Tenant-ID")) -> UUID:
    return x_tenant_id


@router.get("/vehicles", response_model=list[VehicleRead])
def list_vehicles(
    identifier: str | None = Query(default=None),
    modelName: str | None = Query(default=None, alias="modelName"),
    vehicleClass: VehicleClass | None = Query(default=None, alias="vehicleClass"),
    powertrain: PowertrainType | None = Query(default=None),
    isActive: bool | None = Query(default=None, alias="isActive"),
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> list[VehicleRead]:
    return list_vehicles_for_scope(
        db,
        tenant_id,
        identifier=identifier,
        model_name=modelName,
        vehicle_class=vehicleClass.value if vehicleClass else None,
        powertrain=powertrain.value if powertrain else None,
        is_active=isActive,
    )


@router.get("/vehicles/{vehicle_id}", response_model=VehicleRead)
def get_vehicle(
    vehicle_id: UUID,
    tenant_id: UUID = Depends(get_tenant_id),
    db: Session = Depends(get_db),
) -> VehicleRead:
    vehicle = get_vehicle_for_scope(db, tenant_id, vehicle_id)
    if vehicle is None:
        raise HTTPException(status_code=404, detail="Vehicle not found")
    return vehicle
