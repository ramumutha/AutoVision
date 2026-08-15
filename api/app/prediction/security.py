from __future__ import annotations

from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.identity.models import RoleAssignment, UserRef

REQUEST_ROLES = frozenset({"ADMIN", "SERVICE_ADVISOR", "FLEET_MANAGER"})
READ_ROLES = frozenset({"ADMIN", "SERVICE_ADVISOR", "FLEET_MANAGER", "TECHNICIAN", "VIEWER"})


class PredictionAuthenticationError(Exception):
    pass


class PredictionAuthorizationError(Exception):
    pass


def get_prediction_user_for_tenant(
    session: Session,
    *,
    tenant_id: UUID,
    user_ref_id: UUID,
) -> UserRef:
    user = session.execute(
        select(UserRef).where(
            UserRef.tenant_id == tenant_id,
            UserRef.id == user_ref_id,
            UserRef.is_active.is_(True),
        )
    ).scalar_one_or_none()
    if user is None:
        raise PredictionAuthenticationError("Authenticated user was not found")
    return user


def require_prediction_request_access(
    session: Session,
    *,
    tenant_id: UUID,
    user_ref_id: UUID,
) -> UserRef:
    return _require_role(
        session,
        tenant_id=tenant_id,
        user_ref_id=user_ref_id,
        allowed_roles=REQUEST_ROLES,
    )


def require_prediction_read_access(
    session: Session,
    *,
    tenant_id: UUID,
    user_ref_id: UUID,
) -> UserRef:
    return _require_role(
        session,
        tenant_id=tenant_id,
        user_ref_id=user_ref_id,
        allowed_roles=READ_ROLES,
    )


def _require_role(
    session: Session,
    *,
    tenant_id: UUID,
    user_ref_id: UUID,
    allowed_roles: frozenset[str],
) -> UserRef:
    user = get_prediction_user_for_tenant(session, tenant_id=tenant_id, user_ref_id=user_ref_id)
    assignment = session.execute(
        select(RoleAssignment).where(
            RoleAssignment.tenant_id == tenant_id,
            RoleAssignment.user_ref_id == user.id,
            RoleAssignment.role_name.in_(allowed_roles),
        )
    ).scalar_one_or_none()
    if assignment is None:
        raise PredictionAuthorizationError("Prediction access is not authorized")
    return user
