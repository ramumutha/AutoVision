from __future__ import annotations

import argparse
from datetime import datetime, timezone
from urllib.parse import urlparse

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.database import engine, settings
from app.core.models import Tenant
from app.identity.models import RoleAssignment, UserRef
from app.vehicle.models import (
    AuditEvent,
    PowertrainType,
    UsageSnapshot,
    Vehicle,
    VehicleClass,
    VehicleIdentifier,
)


DATABASE_URL = settings.DATABASE_URL

TENANT_SLUG = "autovision-demo-org"


def _database_name_from_url(database_url: str) -> str:
    parsed = urlparse(database_url)
    return parsed.path.lstrip("/") if parsed.path else ""


def _require_test_reset_database(database_url: str | None = None) -> str:
    resolved_url = database_url or settings.DATABASE_URL
    database_name = _database_name_from_url(resolved_url)

    if database_name == "autovision":
        raise RuntimeError(
            "Refusing to reset the development database 'autovision'. "
            "Use TEST_DATABASE_URL=.../autovision_test for pytest cleanup."
        )

    if database_name != "autovision_test":
        raise RuntimeError(
            f"Test reset must target the isolated database 'autovision_test'; "
            f"active database is '{database_name or '<unknown>'}'."
        )

    return database_name


def ensure_tenant(session: Session) -> Tenant:
    tenant = session.execute(select(Tenant).where(Tenant.slug == TENANT_SLUG)).scalar_one_or_none()
    if tenant is None:
        tenant = Tenant(slug=TENANT_SLUG, name="AutoVision Demo Organization")
        session.add(tenant)
        session.flush()
    return tenant


def ensure_user(session: Session, tenant: Tenant, external_user_id: str, display_name: str, email: str | None = None) -> UserRef:
    user = session.execute(
        select(UserRef).where(
            UserRef.tenant_id == tenant.id,
            UserRef.external_user_id == external_user_id,
        )
    ).scalar_one_or_none()
    if user is None:
        user = UserRef(
            tenant_id=tenant.id,
            external_user_id=external_user_id,
            display_name=display_name,
            email=email,
            is_active=True,
        )
        session.add(user)
        session.flush()
    return user


def ensure_role(session: Session, tenant: Tenant, user: UserRef, role_name: str) -> None:
    exists = session.execute(
        select(RoleAssignment).where(
            RoleAssignment.tenant_id == tenant.id,
            RoleAssignment.user_ref_id == user.id,
            RoleAssignment.role_name == role_name,
        )
    ).scalar_one_or_none()
    if exists is None:
        session.add(RoleAssignment(tenant_id=tenant.id, user_ref_id=user.id, role_name=role_name))


def ensure_vehicle(session: Session, tenant: Tenant, demo_code: str, *, vehicle_class: VehicleClass, powertrain: PowertrainType, make: str, model: str, variant: str, year: int, mileage_km: int) -> Vehicle:
    identifier = session.execute(
        select(VehicleIdentifier).where(
            VehicleIdentifier.tenant_id == tenant.id,
            VehicleIdentifier.identifier_type == "demo_reference",
            VehicleIdentifier.identifier_value == demo_code,
        )
    ).scalar_one_or_none()

    if identifier is not None:
        vehicle = session.get(Vehicle, identifier.vehicle_id)
        if vehicle is not None:
            vehicle.vehicle_class = vehicle_class
            vehicle.powertrain = powertrain
            vehicle.model_name = model
            vehicle.year = year
            vehicle.color = variant
            vehicle.is_active = True
            session.flush()
            return vehicle

    vehicle = session.execute(
        select(Vehicle).where(
            Vehicle.tenant_id == tenant.id,
            Vehicle.model_name == model,
            Vehicle.year == year,
            Vehicle.color == variant,
        )
    ).scalar_one_or_none()
    if vehicle is None:
        vehicle = Vehicle(
            tenant_id=tenant.id,
            vehicle_class=vehicle_class,
            powertrain=powertrain,
            model_name=model,
            year=year,
            color=variant,
            is_active=True,
        )
        session.add(vehicle)
        session.flush()

    existing_identifier = session.execute(
        select(VehicleIdentifier).where(
            VehicleIdentifier.tenant_id == tenant.id,
            VehicleIdentifier.vehicle_id == vehicle.id,
            VehicleIdentifier.identifier_type == "demo_reference",
        )
    ).scalar_one_or_none()
    if existing_identifier is None:
        session.add(
            VehicleIdentifier(
                tenant_id=tenant.id,
                vehicle_id=vehicle.id,
                identifier_type="demo_reference",
                identifier_value=demo_code,
                is_primary=True,
            )
        )

    snapshot_recorded_at = datetime(2026, 1, 1, tzinfo=timezone.utc)
    existing_snapshot = session.execute(
        select(UsageSnapshot).where(
            UsageSnapshot.tenant_id == tenant.id,
            UsageSnapshot.vehicle_id == vehicle.id,
            UsageSnapshot.recorded_at == snapshot_recorded_at,
        )
    ).scalar_one_or_none()
    if existing_snapshot is None:
        session.add(
            UsageSnapshot(
                tenant_id=tenant.id,
                vehicle_id=vehicle.id,
                recorded_at=snapshot_recorded_at,
                odometer_km=float(mileage_km),
                engine_hours=float(mileage_km / 10),
                fuel_level_pct=85.0,
                data_source="demo_seed",
                payload={
                    "make": make,
                    "model": model,
                    "variant": variant,
                    "demo_reference": demo_code,
                },
            )
        )

    return vehicle


def ensure_audit_event(session: Session, tenant: Tenant, user: UserRef, vehicle: Vehicle, event_type: str, action: str, message: str) -> None:
    exists = session.execute(
        select(AuditEvent).where(
            AuditEvent.tenant_id == tenant.id,
            AuditEvent.vehicle_id == vehicle.id,
            AuditEvent.user_ref_id == user.id,
            AuditEvent.event_type == event_type,
            AuditEvent.action == action,
        )
    ).scalar_one_or_none()
    if exists is None:
        session.add(
            AuditEvent(
                tenant_id=tenant.id,
                vehicle_id=vehicle.id,
                user_ref_id=user.id,
                event_type=event_type,
                action=action,
                entity_type="vehicle",
                entity_id=str(vehicle.id),
                occurred_at=datetime.now(timezone.utc),
                event_metadata={"message": message},
            )
        )


def seed_demo_data() -> None:
    with Session(engine) as session:
        tenant = ensure_tenant(session)

        advisor = ensure_user(session, tenant, "svc-advisor-01", "Service Advisor", "service-advisor@example.demo")
        technician = ensure_user(session, tenant, "tech-01", "Technician", "technician@example.demo")
        fleet_manager = ensure_user(session, tenant, "fleet-manager-01", "Fleet Manager", "fleet-manager@example.demo")
        admin = ensure_user(session, tenant, "admin-viewer-01", "Admin / Viewer", "admin-viewer@example.demo")

        ensure_role(session, tenant, advisor, "SERVICE_ADVISOR")
        ensure_role(session, tenant, technician, "TECHNICIAN")
        ensure_role(session, tenant, fleet_manager, "FLEET_MANAGER")
        ensure_role(session, tenant, admin, "ADMIN")
        ensure_role(session, tenant, admin, "VIEWER")

        ice_vehicle = ensure_vehicle(
            session,
            tenant,
            "AV-DEMO-ICE-001",
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.ICE,
            make="Demo Motors",
            model="City Compact",
            variant="Premium",
            year=2022,
            mileage_km=45500,
        )
        ev_vehicle = ensure_vehicle(
            session,
            tenant,
            "AV-DEMO-EV-001",
            vehicle_class=VehicleClass.PASSENGER,
            powertrain=PowertrainType.EV,
            make="Voltline",
            model="Eclipse",
            variant="Long Range",
            year=2024,
            mileage_km=18500,
        )
        heavy_vehicle = ensure_vehicle(
            session,
            tenant,
            "AV-DEMO-CV-001",
            vehicle_class=VehicleClass.HEAVY_SPECIAL,
            powertrain=PowertrainType.ICE,
            make="NorthFleet",
            model="Cargo Max",
            variant="Rigid Body",
            year=2021,
            mileage_km=128400,
        )

        ensure_audit_event(session, tenant, advisor, ice_vehicle, "vehicle.seed", "seeded", "Passenger ICE demo vehicle loaded")
        ensure_audit_event(session, tenant, technician, ev_vehicle, "vehicle.seed", "seeded", "Passenger EV demo vehicle loaded")
        ensure_audit_event(session, tenant, fleet_manager, heavy_vehicle, "vehicle.seed", "seeded", "Commercial heavy demo vehicle loaded")

        session.commit()


def reset_demo_data() -> None:
    _require_test_reset_database()

    with Session(engine) as session:
        tenant = session.execute(select(Tenant).where(Tenant.slug == TENANT_SLUG)).scalar_one_or_none()
        if tenant is None:
            return

        session.execute(RoleAssignment.__table__.delete().where(RoleAssignment.tenant_id == tenant.id))
        session.execute(UserRef.__table__.delete().where(UserRef.tenant_id == tenant.id))
        session.execute(AuditEvent.__table__.delete().where(AuditEvent.tenant_id == tenant.id))
        session.execute(UsageSnapshot.__table__.delete().where(UsageSnapshot.tenant_id == tenant.id))
        session.execute(VehicleIdentifier.__table__.delete().where(VehicleIdentifier.tenant_id == tenant.id))
        session.execute(Vehicle.__table__.delete().where(Vehicle.tenant_id == tenant.id))
        session.commit()
        session.delete(tenant)
        session.commit()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AutoVision Sprint 0 demo seed")
    parser.add_argument("--reset", action="store_true", help="delete only the demo tenant and related Sprint 0 records")
    args = parser.parse_args()

    if args.reset:
        reset_demo_data()
        print("Demo data reset complete.")
    else:
        seed_demo_data()
        print("Demo data seed complete.")
