from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_returns_ok() -> None:
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_cors_preflight_get_vehicle_api_allows_local_frontend() -> None:
    response = client.options(
        "/v1/vehicles",
        headers={
            "Origin": "http://localhost:3000",
            "Access-Control-Request-Method": "GET",
            "Access-Control-Request-Headers": "X-Tenant-ID, Content-Type",
        },
    )

    assert response.status_code == 200
    assert response.headers.get("access-control-allow-origin") == "http://localhost:3000"
    assert "GET" in response.headers.get("access-control-allow-methods", "")
    assert "X-Tenant-ID" in response.headers.get("access-control-allow-headers", "")


def test_cors_preflight_post_service_event_allows_local_frontend() -> None:
    response = client.options(
        "/v1/service-events",
        headers={
            "Origin": "http://localhost:3000",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "X-Tenant-ID, Content-Type",
        },
    )

    assert response.status_code == 200
    assert response.headers.get("access-control-allow-origin") == "http://localhost:3000"
    assert "POST" in response.headers.get("access-control-allow-methods", "")
    assert "Content-Type" in response.headers.get("access-control-allow-headers", "")


def test_cors_preflight_patch_service_event_complaint_allows_local_frontend() -> None:
    response = client.options(
        "/v1/service-events/123e4567-e89b-12d3-a456-426614174000/complaint",
        headers={
            "Origin": "http://localhost:3000",
            "Access-Control-Request-Method": "PATCH",
            "Access-Control-Request-Headers": "X-Tenant-ID, Content-Type",
        },
    )

    assert response.status_code == 200
    assert response.headers.get("access-control-allow-origin") == "http://localhost:3000"
    assert "PATCH" in response.headers.get("access-control-allow-methods", "")
    assert "X-Tenant-ID" in response.headers.get("access-control-allow-headers", "")


def test_cors_preflight_rejects_disallowed_origin() -> None:
    response = client.options(
        "/v1/service-events",
        headers={
            "Origin": "http://localhost:3001",
            "Access-Control-Request-Method": "POST",
            "Access-Control-Request-Headers": "X-Tenant-ID, Content-Type",
        },
    )

    assert response.status_code == 400
    assert response.headers.get("access-control-allow-origin") is None
