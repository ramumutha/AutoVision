from __future__ import annotations

from datetime import datetime, timezone

from pydantic import ValidationError
from sqlalchemy.exc import SQLAlchemyError

from app.core.database import SessionLocal
from app.prediction.deterministic_provider import DeterministicPredictionProvider
from app.prediction.input_builder import PredictionInputBuilder
from app.prediction.models import PredictionInputQuality, PredictionRunStatus
from app.prediction.provider import (
    PredictionProvider,
    PredictionProviderConfigurationError,
    PredictionProviderError,
    PredictionProviderExecutionError,
)
from app.prediction.provider_schemas import PredictionRequest, PredictionResponse
from app.prediction.repository import (
    get_prediction_run_for_work_item,
    has_prediction_assessments,
    persist_prediction_response,
)
from app.prediction.semantic_validation import PredictionSemanticValidationError, PredictionSemanticValidator
from app.prediction.worker_schemas import PredictionWorkItem


class PredictionExecutionFailure(Exception):
    def __init__(self, error_code: str, error_message: str) -> None:
        super().__init__(error_message)
        self.error_code = error_code
        self.error_message = error_message


class PredictionPersistenceFailure(Exception):
    pass


class PredictionWorker:
    def __init__(
        self,
        *,
        session_factory=SessionLocal,
        input_builder: PredictionInputBuilder | None = None,
        provider: PredictionProvider | None = None,
        semantic_validator: PredictionSemanticValidator | None = None,
    ) -> None:
        self.session_factory = session_factory
        self.input_builder = input_builder if input_builder is not None else PredictionInputBuilder()
        self.provider = provider if provider is not None else DeterministicPredictionProvider()
        self.semantic_validator = (
            semantic_validator if semantic_validator is not None else PredictionSemanticValidator()
        )

    def execute(self, work_item: PredictionWorkItem) -> None:
        if not self._claim(work_item):
            return
        try:
            context = self._resolve_and_freeze_input(work_item)
            response = self._execute_provider(context)
            response = self._validate_provider_response(context=context, response=response)
            self._persist_success(work_item, context=context, response=response)
        except PredictionExecutionFailure as exc:
            self._mark_failed(work_item, exc.error_code, exc.error_message)
        except PredictionProviderConfigurationError:
            self._mark_failed(
                work_item,
                "PROVIDER_CONFIGURATION_ERROR",
                "Prediction provider configuration was invalid",
            )
        except PredictionProviderExecutionError:
            self._mark_failed(
                work_item,
                "PROVIDER_EXECUTION_ERROR",
                "Prediction provider execution failed",
            )
        except PredictionProviderError:
            self._mark_failed(
                work_item,
                "PROVIDER_EXECUTION_ERROR",
                "Prediction provider failed",
            )
        except ValidationError:
            self._mark_failed(
                work_item,
                "INVALID_PROVIDER_RESPONSE",
                "Prediction provider response was invalid",
            )
        except PredictionSemanticValidationError:
            self._mark_failed(
                work_item,
                "SEMANTIC_VALIDATION_ERROR",
                "Prediction provider response failed semantic validation",
            )
        except PredictionPersistenceFailure:
            self._mark_failed(
                work_item,
                "PREDICTION_PERSISTENCE_ERROR",
                "Prediction persistence failed",
            )
        except SQLAlchemyError:
            self._mark_failed(
                work_item,
                "PREDICTION_PERSISTENCE_ERROR",
                "Prediction persistence failed",
            )
        except LookupError:
            self._mark_failed(
                work_item,
                "PREDICTION_INPUT_ERROR",
                "Prediction input could not be resolved",
            )
        except ValueError:
            self._mark_failed(
                work_item,
                "PREDICTION_INPUT_ERROR",
                "Prediction input was invalid",
            )
        except Exception:
            self._mark_failed(
                work_item,
                "PREDICTION_WORKER_ERROR",
                "Prediction worker failed",
            )

    def _claim(self, work_item: PredictionWorkItem) -> bool:
        session = self.session_factory()
        try:
            with session.begin():
                prediction_run = get_prediction_run_for_work_item(
                    session,
                    work_item=work_item,
                    for_update=True,
                )
                if prediction_run is None or prediction_run.status != PredictionRunStatus.QUEUED:
                    return False

                now = datetime.now(timezone.utc)
                prediction_run.status = PredictionRunStatus.PROCESSING
                prediction_run.started_at = now
                prediction_run.updated_at = now
                session.flush()
                return True
        finally:
            session.close()

    def _execute_provider(self, context):
        request = PredictionRequest(schema_version="s3.3", context=context)
        provider_result = self.provider.predict(request)
        return PredictionResponse.model_validate(provider_result)

    def _validate_provider_response(self, *, context, response: PredictionResponse) -> PredictionResponse:
        request = PredictionRequest(schema_version="s3.3", context=context)
        return self.semantic_validator.validate(request=request, response=response)

    def _persist_success(self, work_item: PredictionWorkItem, *, context, response: PredictionResponse) -> None:
        session = self.session_factory()
        try:
            with session.begin():
                prediction_run = get_prediction_run_for_work_item(
                    session,
                    work_item=work_item,
                    for_update=True,
                )
                if prediction_run is None or prediction_run.status != PredictionRunStatus.PROCESSING:
                    return
                if has_prediction_assessments(
                    session,
                    tenant_id=work_item.tenantId,
                    prediction_run_id=work_item.predictionRunId,
                ):
                    raise PredictionExecutionFailure(
                        "PREDICTION_PERSISTENCE_ERROR",
                        "Prediction results already exist",
                    )
                if context.quality.quality == PredictionInputQuality.INSUFFICIENT:
                    raise PredictionExecutionFailure(
                        "INSUFFICIENT_INPUT",
                        "Prediction input was insufficient",
                    )
                try:
                    persist_prediction_response(
                        session,
                        tenant_id=work_item.tenantId,
                        prediction_run_id=work_item.predictionRunId,
                        response=response,
                    )
                except PredictionExecutionFailure:
                    raise
                except Exception as exc:
                    raise PredictionPersistenceFailure() from exc

                if context.quality.quality == PredictionInputQuality.COMPLETE:
                    prediction_run.status = PredictionRunStatus.COMPLETED
                elif context.quality.quality == PredictionInputQuality.PARTIAL:
                    prediction_run.status = PredictionRunStatus.PARTIALLY_COMPLETED
                else:
                    raise PredictionExecutionFailure(
                        "INSUFFICIENT_INPUT",
                        "Prediction input was insufficient",
                    )

                now = datetime.now(timezone.utc)
                prediction_run.provider_name = response.provider.strip()
                prediction_run.provider_version = response.model_version.strip()
                prediction_run.engine_name = response.model.strip()
                prediction_run.engine_version = response.model_version.strip()
                prediction_run.schema_version = response.schema_version
                prediction_run.configuration_version = "deterministic-rules-v1"
                prediction_run.completed_at = now
                prediction_run.updated_at = now
                prediction_run.error_code = None
                prediction_run.error_message = None
                session.flush()
        except PredictionExecutionFailure:
            raise
        except PredictionPersistenceFailure:
            raise
        except Exception as exc:
            raise PredictionPersistenceFailure() from exc
        finally:
            session.close()

    def _resolve_and_freeze_input(self, work_item: PredictionWorkItem):
        session = self.session_factory()
        try:
            context = self.input_builder.build(
                session,
                tenant_id=work_item.tenantId,
                vehicle_id=work_item.vehicleId,
                service_event_id=work_item.serviceEventId,
                analysis_run_id=work_item.analysisRunId,
            )
        finally:
            session.close()

        if context.quality.quality == PredictionInputQuality.INSUFFICIENT:
            raise PredictionExecutionFailure(
                "INSUFFICIENT_INPUT",
                "Prediction input was insufficient",
            )

        snapshot = context.model_dump(mode="json")
        session = self.session_factory()
        try:
            with session.begin():
                prediction_run = get_prediction_run_for_work_item(
                    session,
                    work_item=work_item,
                    for_update=True,
                )
                if prediction_run is None or prediction_run.status != PredictionRunStatus.PROCESSING:
                    return context

                prediction_run.input_snapshot = snapshot
                prediction_run.input_quality = context.quality.quality
                prediction_run.updated_at = datetime.now(timezone.utc)
                session.flush()
                return context
        finally:
            session.close()

    def _mark_failed(self, work_item: PredictionWorkItem, error_code: str, error_message: str) -> None:
        session = self.session_factory()
        try:
            with session.begin():
                prediction_run = get_prediction_run_for_work_item(
                    session,
                    work_item=work_item,
                    for_update=True,
                )
                if prediction_run is None or prediction_run.status not in {
                    PredictionRunStatus.QUEUED,
                    PredictionRunStatus.PROCESSING,
                }:
                    return

                now = datetime.now(timezone.utc)
                prediction_run.status = PredictionRunStatus.FAILED
                prediction_run.completed_at = now
                prediction_run.updated_at = now
                prediction_run.error_code = error_code
                prediction_run.error_message = error_message
                session.flush()
        finally:
            session.close()
