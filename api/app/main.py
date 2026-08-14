import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.evidence.analysis_dispatch import InProcessAnalysisDispatcher
from app.evidence.analysis_router import router as analysis_router
from app.evidence.router import router as evidence_router
from app.service_intake.router import router as service_event_router
from app.vehicle.router import router as vehicle_router


allowed_origins = [
    origin.strip()
    for origin in os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:3000").split(",")
    if origin.strip()
]


app = FastAPI(title="AutoVision API")
app.state.analysis_dispatcher = InProcessAnalysisDispatcher()

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_methods=["GET", "POST", "PATCH", "OPTIONS"],
    allow_headers=["X-Tenant-ID", "Content-Type"],
    allow_credentials=False,
)

app.include_router(vehicle_router)
app.include_router(service_event_router)
app.include_router(evidence_router)
app.include_router(analysis_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
