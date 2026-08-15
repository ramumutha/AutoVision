from __future__ import annotations

from decimal import Decimal

from app.prediction.provider_schemas import PredictionRequest, PredictionResponse
from app.prediction.models import PredictionHorizonType


class PredictionSemanticValidationError(Exception):
    pass


class PredictionSemanticValidator:
    expected_schema_version = "s3.3"

    def validate(
        self,
        *,
        request: PredictionRequest,
        response: PredictionResponse,
    ) -> PredictionResponse:
        self._validate_provider_metadata(request, response)
        self._validate_assessments(request, response)
        self._validate_usage(response)
        return response

    def _validate_provider_metadata(self, request: PredictionRequest, response: PredictionResponse) -> None:
        if any(
            not isinstance(value, str) or not value.strip()
            for value in (response.provider, response.model, response.model_version)
        ):
            raise PredictionSemanticValidationError("Prediction provider metadata is invalid")
        if response.schema_version != self.expected_schema_version:
            raise PredictionSemanticValidationError("Prediction response schema version is invalid")
        if request.schema_version != response.schema_version:
            raise PredictionSemanticValidationError("Prediction request and response schema versions do not match")

    def _validate_assessments(self, request: PredictionRequest, response: PredictionResponse) -> None:
        prediction_codes: set[str] = set()
        for assessment in response.assessments:
            if any(
                not isinstance(value, str) or not value.strip()
                for value in (
                    assessment.prediction_type,
                    assessment.prediction_code,
                    assessment.title,
                    assessment.description,
                )
            ):
                raise PredictionSemanticValidationError("Prediction assessment metadata is invalid")
            if assessment.prediction_code in prediction_codes:
                raise PredictionSemanticValidationError("Duplicate prediction assessment code")
            prediction_codes.add(assessment.prediction_code)

            if assessment.confidence is not None and not assessment.confidence.is_finite():
                raise PredictionSemanticValidationError("Prediction confidence is invalid")
            self._validate_horizon(assessment.horizon_type, assessment.horizon_distance_km, assessment.horizon_time_days)
            if not assessment.factors:
                raise PredictionSemanticValidationError("Prediction assessment requires explainability factors")
            self._validate_rule_provenance(assessment.metadata)
            self._validate_factors(request, assessment.factors)

    def _validate_horizon(
        self,
        horizon_type: PredictionHorizonType,
        distance_km: Decimal | None,
        time_days: int | None,
    ) -> None:
        valid = {
            PredictionHorizonType.DISTANCE: distance_km is not None and time_days is None,
            PredictionHorizonType.TIME: time_days is not None and distance_km is None,
            PredictionHorizonType.DISTANCE_OR_TIME: distance_km is not None or time_days is not None,
            PredictionHorizonType.UNSPECIFIED: distance_km is None and time_days is None,
        }[horizon_type]
        if not valid:
            raise PredictionSemanticValidationError("Prediction horizon is invalid")

    def _validate_rule_provenance(self, metadata: dict) -> None:
        if any(
            not isinstance(metadata.get(key), str) or not metadata[key].strip()
            for key in ("rule_code", "rule_version")
        ):
            raise PredictionSemanticValidationError("Prediction rule provenance is missing")

    def _validate_factors(self, request: PredictionRequest, factors: tuple) -> None:
        identities: set[tuple[object, str, str | None, object | None]] = set()
        for factor in factors:
            if not isinstance(factor.factor_code, str) or not factor.factor_code.strip() or not isinstance(factor.label, str) or not factor.label.strip():
                raise PredictionSemanticValidationError("Prediction factor metadata is invalid")

            source_type = factor.source_entity_type
            source_id = factor.source_entity_id
            if (source_type is None) != (source_id is None):
                raise PredictionSemanticValidationError("Prediction factor source reference is invalid")
            if source_type is not None and not source_type.strip():
                raise PredictionSemanticValidationError("Prediction factor source reference is invalid")

            identity = (factor.factor_type, factor.factor_code, source_type, source_id)
            if identity in identities:
                raise PredictionSemanticValidationError("Duplicate prediction factor")
            identities.add(identity)
            self._validate_source_lineage(request, source_type, source_id)

    def _validate_source_lineage(self, request: PredictionRequest, source_type: str | None, source_id: object | None) -> None:
        if source_type is None:
            return
        context = request.context
        if source_type == "VEHICLE":
            valid = source_id == context.vehicle.vehicle_id
        elif source_type == "USAGE_SNAPSHOT":
            valid = context.usage is not None and source_id == context.usage.usage_snapshot_id
        elif source_type == "FINDING":
            valid = any(source_id == finding.finding_id for finding in context.findings)
        elif source_type == "SERVICE_EVENT":
            service_event_ids = {item.service_event_id for item in context.service_history}
            if context.service_event_id is not None:
                service_event_ids.add(context.service_event_id)
            valid = source_id in service_event_ids
        else:
            return
        if not valid:
            raise PredictionSemanticValidationError("Prediction factor source lineage is invalid")

    def _validate_usage(self, response: PredictionResponse) -> None:
        usage = response.usage
        if usage.rules_evaluated is None or usage.rules_matched is None or usage.rules_matched > usage.rules_evaluated:
            raise PredictionSemanticValidationError("Prediction provider usage is invalid")
