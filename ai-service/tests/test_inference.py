import uuid
from fastapi.testclient import TestClient
from app.core.config import settings


def test_health_endpoint(test_client: TestClient):
    response = test_client.get("/api/v1/health")
    assert response.status_code == 200
    json_data = response.json()
    assert json_data["status"] == "UP"
    assert "gpuStatus" in json_data
    assert "loadedModels" in json_data


def test_models_list_endpoint(test_client: TestClient):
    response = test_client.get("/api/v1/models")
    assert response.status_code == 200
    json_data = response.json()
    assert any(m.get("name") == "ToothSeg" for m in json_data)


def test_inference_unauthorized(test_client: TestClient):
    study_id = str(uuid.uuid4())
    payload = {
        "studyId": study_id,
        "sessionId": "test-session-001"
    }
    # Call without internal gateway authorization key header
    response = test_client.post("/api/v1/inference", json=payload)
    assert response.status_code == 401


def test_inference_authorized(test_client: TestClient):
    study_id = str(uuid.uuid4())
    payload = {
        "studyId": study_id,
        "sessionId": "test-session-001"
    }
    # Pass valid internal gateway authorization key header from settings
    headers = {"X-Internal-Gateway-Key": settings.internal_gateway_key}
    response = test_client.post("/api/v1/inference", json=payload, headers=headers)
    assert response.status_code == 200
    json_data = response.json()
    assert "jobId" in json_data
    assert json_data["status"] == "queued"
