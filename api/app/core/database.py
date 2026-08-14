from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, sessionmaker


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=Path(__file__).resolve().parents[3] / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    DATABASE_URL: str
    TEST_DATABASE_URL: str | None = None
    EVIDENCE_STORAGE_PROVIDER: str = "local"
    EVIDENCE_LOCAL_STORAGE_ROOT: Path = Path("./var/evidence")
    EVIDENCE_MAX_FILE_SIZE_BYTES: int = 25 * 1024 * 1024


settings = Settings()
engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


class Base(DeclarativeBase):
    pass
