from __future__ import annotations

from fastapi.testclient import TestClient

from api.app import create_app


def test_service_starts_and_health_check_is_stable(configured_env) -> None:
    app = create_app()

    with TestClient(app) as client:
        for _ in range(10):
            response = client.get("/health")
            assert response.status_code == 200
            payload = response.json()
            assert payload["status"] == "ok"
            assert payload["service"] == "zzyl-ai-service-test"
            assert payload["environment"] == "test"

    assert configured_env["knowledge_base_dir"].exists()
    assert configured_env["chroma_persist_dir"].exists()
