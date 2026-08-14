from __future__ import annotations

from typing import Protocol

from app.evidence.analysis_schemas import FindingAnalysisRequest, FindingAnalysisResponse
from app.evidence.models import EvidenceSufficiency


class ProviderExecutionError(Exception):
    pass


class AIFindingProvider(Protocol):
    def analyze(self, request: FindingAnalysisRequest) -> FindingAnalysisResponse:
        ...


class FakeFindingProvider:
    provider_name = "fake"
    model_name = "autovision-fake"
    model_version = "1"
    schema_version = "s2.4"

    def __init__(self, *, fail: bool = False) -> None:
        self.fail = fail

    def analyze(self, request: FindingAnalysisRequest) -> FindingAnalysisResponse:
        if self.fail:
            raise ProviderExecutionError("Configured fake provider failure")

        observations = tuple(
            {
                "finding_code": "FAKE_EVIDENCE_OBSERVATION",
                "title": "Deterministic evidence observation",
                "description": f"Evidence {item.evidence_id} was included in the fake analysis.",
                "component": item.media_type,
                "location": None,
                "confidence": 0.5,
                "evidence_sufficiency": EvidenceSufficiency.SUFFICIENT,
                "supporting_evidence_ids": (item.evidence_id,),
            }
            for item in request.evidence
        )
        return FindingAnalysisResponse(
            schema_version=self.schema_version,
            provider=self.provider_name,
            model=self.model_name,
            model_version=self.model_version,
            observations=observations,
            evidence_assessment=EvidenceSufficiency.SUFFICIENT,
            warnings=(),
            usage={
                "input_tokens": len(request.evidence),
                "output_tokens": len(observations),
            },
        )
