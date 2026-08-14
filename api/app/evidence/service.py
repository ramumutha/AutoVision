from __future__ import annotations

import hashlib
import re
import uuid
from datetime import datetime
from pathlib import Path

from fastapi import UploadFile
from sqlalchemy.orm import Session

from app.core.database import settings
from app.evidence.models import CaptureSource, Evidence, EvidenceStatus, EvidenceType
from app.evidence.repository import (
    create_evidence_asset_for_tenant,
    create_evidence_for_tenant,
    get_evidence_asset_for_tenant,
    get_evidence_for_tenant_service_event,
    get_service_event_for_tenant,
    list_evidence_for_tenant_service_event,
)
from app.evidence.storage import EvidenceStorageProvider, LocalEvidenceStorageProvider


MEDIA_TYPES_BY_EVIDENCE_TYPE = {
    EvidenceType.IMAGE: {"image/jpeg", "image/png", "image/webp"},
    EvidenceType.VIDEO: {"video/mp4", "video/webm", "video/quicktime"},
    EvidenceType.DOCUMENT: {"application/pdf", "text/plain"},
}
MEDIA_EXTENSIONS = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
    "video/mp4": ".mp4",
    "video/webm": ".webm",
    "video/quicktime": ".mov",
    "application/pdf": ".pdf",
    "text/plain": ".txt",
}
CHUNK_SIZE = 1024 * 1024


class UploadValidationError(Exception):
    def __init__(self, detail: str, status_code: int) -> None:
        super().__init__(detail)
        self.detail = detail
        self.status_code = status_code


class StorageFailure(Exception):
    pass


def get_evidence_storage_provider() -> EvidenceStorageProvider:
    if settings.EVIDENCE_STORAGE_PROVIDER != "local":
        raise StorageFailure("Unsupported evidence storage provider")
    return LocalEvidenceStorageProvider(settings.EVIDENCE_LOCAL_STORAGE_ROOT)


def _safe_display_filename(filename: str | None) -> str:
    if not filename:
        raise UploadValidationError("Invalid filename", 400)
    normalized = filename.replace("\\", "/").split("/")[-1]
    normalized = re.sub(r"[\x00-\x1f\x7f]", "", normalized).strip()
    if not normalized or normalized in {".", ".."}:
        raise UploadValidationError("Invalid filename", 400)
    return normalized[:255]


def _storage_key(tenant_id: uuid.UUID, service_event_id: uuid.UUID, evidence_id: uuid.UUID, media_type: str) -> str:
    return Path(str(tenant_id), str(service_event_id), str(evidence_id), f"{uuid.uuid4().hex}{MEDIA_EXTENSIONS[media_type]}").as_posix()


def _validate_media(evidence: Evidence, media_type: str | None) -> str:
    normalized = (media_type or "").split(";", 1)[0].strip().lower()
    accepted = MEDIA_TYPES_BY_EVIDENCE_TYPE.get(evidence.evidence_type, set())
    if normalized not in accepted:
        raise UploadValidationError("Unsupported or incompatible media type", 415)
    return normalized


def _serialize_evidence(evidence: Evidence) -> dict:
    evidence_type = getattr(evidence.evidence_type, "value", evidence.evidence_type)
    capture_source = getattr(evidence.capture_source, "value", evidence.capture_source)
    status = getattr(evidence.status, "value", evidence.status)
    return {
        "id": evidence.id,
        "tenantId": evidence.tenant_id,
        "serviceEventId": evidence.service_event_id,
        "evidenceType": evidence_type,
        "captureSource": capture_source,
        "title": evidence.title,
        "description": evidence.description,
        "capturedAt": evidence.captured_at,
        "capturedByUserRefId": evidence.captured_by_user_ref_id,
        "status": status,
        "createdAt": evidence.created_at,
        "updatedAt": evidence.updated_at,
    }


def create_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_type: EvidenceType,
    capture_source: CaptureSource,
    title: str | None = None,
    description: str | None = None,
    captured_at: datetime | None = None,
) -> Evidence:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")

    evidence = create_evidence_for_tenant(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_type=evidence_type,
        capture_source=capture_source,
        title=title,
        description=description,
        captured_at=captured_at,
    )
    return evidence


def list_evidence_for_scope(session: Session, *, tenant_id: uuid.UUID, service_event_id: uuid.UUID) -> list[Evidence]:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")
    return list_evidence_for_tenant_service_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
    )


def get_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> Evidence | None:
    event = get_service_event_for_tenant(session, tenant_id, service_event_id)
    if event is None:
        raise LookupError("Service event not found")
    evidence = get_evidence_for_tenant_service_event(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_id=evidence_id,
    )
    if evidence is None:
        raise LookupError("Evidence not found")
    return evidence


def serialize_evidence(evidence: Evidence) -> dict:
    return _serialize_evidence(evidence)


def mark_upload_failed_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
) -> None:
    evidence = get_evidence_for_scope(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_id=evidence_id,
    )
    if evidence.status == EvidenceStatus.PENDING_UPLOAD:
        evidence.status = EvidenceStatus.UPLOAD_FAILED
        evidence.updated_at = datetime.now(evidence.updated_at.tzinfo)
        session.flush()


def upload_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
    upload: UploadFile,
    client_checksum: str | None = None,
    storage_provider: EvidenceStorageProvider | None = None,
) -> Evidence:
    evidence = get_evidence_for_scope(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_id=evidence_id,
    )
    if evidence.status != EvidenceStatus.PENDING_UPLOAD:
        raise UploadValidationError("Evidence is not pending upload", 409)

    media_type = _validate_media(evidence, upload.content_type)
    display_filename = _safe_display_filename(upload.filename)
    provider = storage_provider or get_evidence_storage_provider()
    temporary_path: Path | None = None
    final_key: str | None = None
    try:
        temporary_path = provider.temporary_path()
        file_size = 0
        digest = hashlib.sha256()
        with temporary_path.open("wb") as temporary_file:
            while True:
                chunk = upload.file.read(CHUNK_SIZE)
                if not chunk:
                    break
                file_size += len(chunk)
                if file_size > settings.EVIDENCE_MAX_FILE_SIZE_BYTES:
                    raise UploadValidationError("Evidence file is too large", 413)
                digest.update(chunk)
                temporary_file.write(chunk)

        if file_size == 0:
            raise UploadValidationError("Evidence file is empty", 400)
        checksum = digest.hexdigest()
        if client_checksum and client_checksum.strip().lower() != checksum:
            raise UploadValidationError("Checksum mismatch", 400)

        final_key = _storage_key(tenant_id, service_event_id, evidence_id, media_type)
        stored = provider.store(temporary_path, final_key)
        try:
            asset = create_evidence_asset_for_tenant(
                session,
                tenant_id=tenant_id,
                evidence_id=evidence.id,
                storage_provider=provider.provider_name,
                storage_key=final_key,
                file_name=display_filename,
                media_type=media_type,
                file_size_bytes=file_size,
                checksum_sha256=checksum,
            )
            if stored.size != asset.file_size_bytes:
                raise StorageFailure("Stored object metadata mismatch")
            evidence.status = EvidenceStatus.UPLOADED
            evidence.updated_at = datetime.now(evidence.updated_at.tzinfo)
            session.flush()
        except Exception as exc:
            provider.delete(final_key)
            raise StorageFailure("Evidence persistence failed") from exc
        return evidence
    except UploadValidationError:
        raise
    except (OSError, ValueError, StorageFailure) as exc:
        if final_key is not None:
            provider.delete(final_key)
        raise StorageFailure("Evidence storage failed") from exc
    finally:
        if temporary_path is not None:
            temporary_path.unlink(missing_ok=True)


def complete_evidence_for_scope(
    session: Session,
    *,
    tenant_id: uuid.UUID,
    service_event_id: uuid.UUID,
    evidence_id: uuid.UUID,
    storage_provider: EvidenceStorageProvider | None = None,
) -> Evidence:
    evidence = get_evidence_for_scope(
        session,
        tenant_id=tenant_id,
        service_event_id=service_event_id,
        evidence_id=evidence_id,
    )
    if evidence.status != EvidenceStatus.UPLOADED:
        raise UploadValidationError("Evidence is not uploaded", 409)
    asset = get_evidence_asset_for_tenant(session, tenant_id=tenant_id, evidence_id=evidence.id)
    if asset is None:
        raise LookupError("Evidence asset not found")
    provider = storage_provider or get_evidence_storage_provider()
    metadata = provider.get_metadata(asset.storage_key)
    if metadata is None or metadata.size != asset.file_size_bytes:
        raise StorageFailure("Evidence object could not be verified")
    evidence.status = EvidenceStatus.READY
    evidence.updated_at = datetime.now(evidence.updated_at.tzinfo)
    session.flush()
    return evidence
