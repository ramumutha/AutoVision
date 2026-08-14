from __future__ import annotations

import os
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Protocol


@dataclass(frozen=True)
class StoredObjectMetadata:
    size: int
    checksum_sha256: str | None = None


class EvidenceStorageProvider(Protocol):
    provider_name: str

    def temporary_path(self) -> Path: ...

    def store(self, temporary_path: Path, storage_key: str) -> StoredObjectMetadata: ...

    def get_metadata(self, storage_key: str) -> StoredObjectMetadata | None: ...

    def delete(self, storage_key: str) -> None: ...


class LocalEvidenceStorageProvider:
    provider_name = "local"

    def __init__(self, root: str | Path) -> None:
        self.root = Path(root).expanduser().resolve()

    def temporary_path(self) -> Path:
        self.root.mkdir(parents=True, exist_ok=True)
        return self.root / f".upload-{uuid.uuid4().hex}.tmp"

    def _resolve_key(self, storage_key: str) -> Path:
        relative_key = Path(storage_key)
        if relative_key.is_absolute() or any(part in {"", ".", ".."} for part in relative_key.parts):
            raise ValueError("Invalid storage key")
        resolved = (self.root / relative_key).resolve()
        if resolved != self.root and self.root not in resolved.parents:
            raise ValueError("Invalid storage key")
        return resolved

    def store(self, temporary_path: Path, storage_key: str) -> StoredObjectMetadata:
        destination = self._resolve_key(storage_key)
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists():
            raise FileExistsError("Storage object already exists")
        os.replace(temporary_path, destination)
        return StoredObjectMetadata(size=destination.stat().st_size)

    def get_metadata(self, storage_key: str) -> StoredObjectMetadata | None:
        try:
            path = self._resolve_key(storage_key)
        except ValueError:
            return None
        if not path.is_file():
            return None
        return StoredObjectMetadata(size=path.stat().st_size)

    def delete(self, storage_key: str) -> None:
        try:
            path = self._resolve_key(storage_key)
        except ValueError:
            return
        path.unlink(missing_ok=True)
