from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from typing import Protocol

from app.evidence.analysis_schemas import AnalysisWorkItem


class AnalysisDispatcher(Protocol):
    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        ...


class InProcessAnalysisDispatcher:
    def __init__(self, *, max_workers: int = 1, worker=None) -> None:
        if worker is None:
            from app.evidence.analysis_worker import AnalysisWorker

            worker = AnalysisWorker()
        self._worker = worker
        self._executor = ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="autovision-analysis")

    def dispatch(self, work_item: AnalysisWorkItem) -> None:
        self._executor.submit(self._worker.execute, work_item)

    def shutdown(self) -> None:
        self._executor.shutdown(wait=False, cancel_futures=True)
