from __future__ import annotations

from typing import Protocol, runtime_checkable

from app.prediction.models import PredictionRun
from app.prediction.worker import PredictionWorker
from app.prediction.worker_schemas import PredictionWorkItem


class PredictionDispatchError(Exception):
    pass


@runtime_checkable
class PredictionDispatcher(Protocol):
    def dispatch(self, work_item: PredictionWorkItem) -> None:
        ...


def build_prediction_work_item(*, run: PredictionRun) -> PredictionWorkItem:
    return PredictionWorkItem(
        predictionRunId=run.id,
        tenantId=run.tenant_id,
        vehicleId=run.vehicle_id,
        serviceEventId=run.service_event_id,
        analysisRunId=run.analysis_run_id,
        correlationId=run.correlation_id,
    )


class InProcessPredictionDispatcher:
    def __init__(self, *, worker: PredictionWorker | None = None) -> None:
        self.worker = worker if worker is not None else PredictionWorker()

    def dispatch(self, work_item: PredictionWorkItem) -> None:
        try:
            self.worker.execute(work_item)
        except PredictionDispatchError:
            raise
        except Exception as exc:
            raise PredictionDispatchError("Prediction dispatch failed") from exc
