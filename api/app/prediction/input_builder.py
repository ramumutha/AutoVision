from __future__ import annotations

import uuid
from datetime import datetime, timezone

from sqlalchemy.orm import Session

from app.evidence.finding_repository import list_finding_evidence_for_tenant, list_finding_reviews_for_tenant
from app.evidence.models import Finding, FindingReviewDecision, FindingReviewStatus
from app.prediction.input_repository import (
    DEFAULT_SERVICE_HISTORY_LIMIT,
    get_latest_usage_snapshot_for_vehicle,
    get_prediction_analysis_run_for_tenant_event,
    get_prediction_vehicle_for_tenant,
    list_prediction_findings_for_tenant_vehicle,
    list_prediction_service_history,
)
from app.prediction.input_schemas import (
    CanonicalPredictionContext,
    EffectiveFindingContext,
    PredictionExternalContext,
    PredictionInputQualityContext,
    PredictionServiceHistoryItem,
    PredictionUsageContext,
    PredictionVehicleContext,
)
from app.prediction.models import PredictionInputQuality
from app.service_intake.models import ServiceEvent
from app.vehicle.models import UsageSnapshot, Vehicle


class PredictionInputBuilder:
    def __init__(self, *, service_history_limit: int = DEFAULT_SERVICE_HISTORY_LIMIT) -> None:
        if service_history_limit <= 0:
            raise ValueError("service_history_limit must be greater than zero")
        self.service_history_limit = service_history_limit

    def build(
        self,
        session: Session,
        *,
        tenant_id: uuid.UUID,
        vehicle_id: uuid.UUID,
        service_event_id: uuid.UUID | None = None,
        analysis_run_id: uuid.UUID | None = None,
        generated_at: datetime | None = None,
    ) -> CanonicalPredictionContext:
        if analysis_run_id is not None and service_event_id is None:
            raise ValueError("service_event_id is required when analysis_run_id is supplied")

        vehicle = get_prediction_vehicle_for_tenant(session=session, tenant_id=tenant_id, vehicle_id=vehicle_id)
        if vehicle is None:
            raise LookupError("Vehicle not found")

        if service_event_id is not None:
            service_event = session.execute(
                ServiceEvent.__table__.select().where(
                    ServiceEvent.id == service_event_id,
                    ServiceEvent.tenant_id == tenant_id,
                    ServiceEvent.vehicle_id == vehicle_id,
                )
            ).first()
            if service_event is None:
                raise LookupError("Service event not found")

        if analysis_run_id is not None:
            if get_prediction_analysis_run_for_tenant_event(
                session,
                tenant_id=tenant_id,
                analysis_run_id=analysis_run_id,
                service_event_id=service_event_id,
            ) is None:
                raise LookupError("Analysis run not found")

        usage_snapshot = get_latest_usage_snapshot_for_vehicle(
            session,
            tenant_id=tenant_id,
            vehicle_id=vehicle_id,
        )
        service_history = list_prediction_service_history(
            session,
            tenant_id=tenant_id,
            vehicle_id=vehicle_id,
            limit=self.service_history_limit,
        )
        findings = list_prediction_findings_for_tenant_vehicle(
            session,
            tenant_id=tenant_id,
            vehicle_id=vehicle_id,
            service_event_id=service_event_id,
            analysis_run_id=analysis_run_id,
        )

        context_findings = self._build_findings_context(
            session=session,
            tenant_id=tenant_id,
            findings=findings,
        )

        missing_sources: list[str] = []
        usage_is_usable = usage_snapshot is not None and (
            usage_snapshot.odometer_km is not None or usage_snapshot.engine_hours is not None
        )
        if not usage_is_usable:
            missing_sources.append("USAGE")
        if not service_history:
            missing_sources.append("SERVICE_HISTORY")
        if not context_findings:
            missing_sources.append("FINDINGS")

        if usage_is_usable:
            quality = PredictionInputQuality.COMPLETE
        elif context_findings or service_history:
            quality = PredictionInputQuality.PARTIAL
        else:
            quality = PredictionInputQuality.INSUFFICIENT

        return CanonicalPredictionContext(
            schema_version="s3.1",
            tenant_id=tenant_id,
            vehicle_id=vehicle_id,
            service_event_id=service_event_id,
            analysis_run_id=analysis_run_id,
            generated_at=generated_at if generated_at is not None else datetime.now(timezone.utc),
            vehicle=self._build_vehicle_context(vehicle),
            usage=self._build_usage_context(usage_snapshot),
            service_history=tuple(
                PredictionServiceHistoryItem(
                    service_event_id=item.id,
                    source=item.source,
                    state=item.state,
                    revision=item.revision,
                    opened_at=item.opened_at,
                    created_at=item.created_at,
                )
                for item in service_history
            ),
            findings=tuple(context_findings),
            external_context=PredictionExternalContext(),
            quality=PredictionInputQualityContext(
                quality=quality,
                missing_sources=tuple(missing_sources),
                warnings=(),
            ),
        )

    def _build_vehicle_context(self, vehicle: Vehicle) -> PredictionVehicleContext:
        return PredictionVehicleContext(
            vehicle_id=vehicle.id,
            vehicle_class=vehicle.vehicle_class,
            powertrain=vehicle.powertrain,
            model_name=vehicle.model_name,
            year=vehicle.year,
            color=vehicle.color,
        )

    def _build_usage_context(self, usage_snapshot: UsageSnapshot | None) -> PredictionUsageContext | None:
        if usage_snapshot is None:
            return None
        return PredictionUsageContext(
            usage_snapshot_id=usage_snapshot.id,
            recorded_at=usage_snapshot.recorded_at,
            odometer_km=usage_snapshot.odometer_km,
            engine_hours=usage_snapshot.engine_hours,
            fuel_level_pct=usage_snapshot.fuel_level_pct,
            data_source=usage_snapshot.data_source,
            payload=usage_snapshot.payload,
        )

    def _build_findings_context(
        self,
        *,
        session: Session,
        tenant_id: uuid.UUID,
        findings: list[Finding],
    ) -> list[EffectiveFindingContext]:
        if not findings:
            return []

        finding_ids = [finding.id for finding in findings]
        evidence_links = list_finding_evidence_for_tenant(
            session,
            tenant_id=tenant_id,
            finding_ids=finding_ids,
        )
        reviews = list_finding_reviews_for_tenant(
            session,
            tenant_id=tenant_id,
            finding_ids=finding_ids,
        )

        evidence_by_finding: dict[uuid.UUID, list[uuid.UUID]] = {}
        for link in evidence_links:
            evidence_by_finding.setdefault(link.finding_id, []).append(link.evidence_id)

        latest_review_by_finding: dict[uuid.UUID, object] = {}
        for review in reviews:
            latest_review_by_finding.setdefault(review.finding_id, review)

        result: list[EffectiveFindingContext] = []
        for finding in findings:
            if finding.review_status in {
                FindingReviewStatus.PENDING_REVIEW,
                FindingReviewStatus.REJECTED,
            }:
                continue

            latest_review = latest_review_by_finding.get(finding.id)
            if latest_review is None or latest_review.decision != finding.review_status:
                raise ValueError("Finding review provenance is inconsistent")

            review_decision = latest_review.decision
            review_status = finding.review_status

            effective_title = finding.title
            effective_description = finding.description
            effective_component = finding.component
            effective_location = finding.location

            if latest_review.decision == FindingReviewDecision.MODIFIED:
                effective_title = latest_review.modified_title or effective_title
                effective_description = latest_review.modified_description or effective_description
                effective_component = latest_review.modified_component or effective_component
                effective_location = latest_review.modified_location or effective_location

            result.append(
                EffectiveFindingContext(
                    finding_id=finding.id,
                    service_event_id=finding.service_event_id,
                    analysis_run_id=finding.analysis_run_id,
                    finding_code=finding.finding_code,
                    original_title=finding.title,
                    original_description=finding.description,
                    original_component=finding.component,
                    original_location=finding.location,
                    effective_title=effective_title,
                    effective_description=effective_description,
                    effective_component=effective_component,
                    effective_location=effective_location,
                    confidence=finding.confidence,
                    evidence_sufficiency=finding.evidence_sufficiency,
                    review_status=review_status,
                    review_id=getattr(latest_review, "id", None),
                    review_decision=review_decision,
                    reviewed_at=getattr(latest_review, "reviewed_at", None),
                    supporting_evidence_ids=tuple(evidence_by_finding.get(finding.id, [])),
                )
            )

        return result
