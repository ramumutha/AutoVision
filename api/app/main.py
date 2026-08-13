import os

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.vehicle.router import router as vehicle_router


allowed_origins = [
    origin.strip()
    for origin in os.getenv("CORS_ALLOWED_ORIGINS", "http://localhost:3000").split(",")
    if origin.strip()
]


app = FastAPI(title="AutoVision API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins,
    allow_methods=["GET", "OPTIONS"],
    allow_headers=["X-Tenant-ID", "Content-Type"],
    allow_credentials=False,
)

app.include_router(vehicle_router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
