from __future__ import annotations

import inspect
from uuid import UUID, uuid4

import pytest
from pydantic import ValidationError

from app.prediction.worker_schemas import PredictionWorkItem


def _ids() -> tuple[UUID, UUID, UUID]:
    return uuid4(), uuid4(), uuid4()


def test_minimal_work_item_contains_only_required_identity() -> None:
    prediction_run_id, tenant_id, vehicle_id = _ids()
    item = PredictionWorkItem(
        predictionRunId=prediction_run_id,
        tenantId=tenant_id,
        vehicleId=vehicle_id,
    )
    assert item.predictionRunId == prediction_run_id
    assert item.tenantId == tenant_id
    assert item.vehicleId == vehicle_id
    assert item.serviceEventId is None
    assert item.analysisRunId is None
    assert item.correlationId is None


def test_optional_scope_and_correlation_fields_are_preserved() -> None:
    prediction_run_id, tenant_id, vehicle_id = _ids()
    service_event_id = uuid4()
    analysis_run_id = uuid4()
    item = PredictionWorkItem(
        predictionRunId=prediction_run_id,
        tenantId=tenant_id,
        vehicleId=vehicle_id,
        serviceEventId=service_event_id,
        analysisRunId=analysis_run_id,
        correlationId="corr-123",
    )
    assert item.serviceEventId == service_event_id
    assert item.analysisRunId == analysis_run_id
    assert item.correlationId == "corr-123"


def test_explicit_none_optional_fields_are_accepted() -> None:
    prediction_run_id, tenant_id, vehicle_id = _ids()
    item = PredictionWorkItem(
        predictionRunId=prediction_run_id,
        tenantId=tenant_id,
        vehicleId=vehicle_id,
        serviceEventId=None,
        analysisRunId=None,
        correlationId=None,
    )
    assert item.serviceEventId is None
    assert item.analysisRunId is None
    assert item.correlationId is None


def test_work_item_is_frozen() -> None:
    item = PredictionWorkItem(predictionRunId=uuid4(), tenantId=uuid4(), vehicleId=uuid4())
    with pytest.raises(ValidationError):
        item.vehicleId = uuid4()


def test_unknown_fields_are_rejected() -> None:
    with pytest.raises(ValidationError):
        PredictionWorkItem(
            predictionRunId=uuid4(),
            tenantId=uuid4(),
            vehicleId=uuid4(),
            status="QUEUED",
        )


def test_serialized_model_contains_only_expected_fields() -> None:
    item = PredictionWorkItem(
        predictionRunId=uuid4(),
        tenantId=uuid4(),
        vehicleId=uuid4(),
        correlationId="corr-123",
    )
    assert set(item.model_dump()) == {
        "predictionRunId",
        "tenantId",
        "vehicleId",
        "serviceEventId",
        "analysisRunId",
        "correlationId",
    }
    assert set(item.model_dump(exclude_none=True)) == {
        "predictionRunId",
        "tenantId",
        "vehicleId",
        "correlationId",
    }


def test_work_item_module_is_pure_and_needs_no_database_session() -> None:
    import app.prediction.worker_schemas as worker_schemas

    source = inspect.getsource(worker_schemas)
    assert "sqlalchemy" not in source.lower()
    assert "Session" not in source
    assert "fastapi" not in source.lower()
    assert "repository" not in source.lower()
