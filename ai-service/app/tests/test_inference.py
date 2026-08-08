import pytest

def test_root_endpoint(client):
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"

def test_health_endpoint(client):
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "UP"
    assert "gpuStatus" in data
    assert "loadedModels" in data

def test_inference_unauthorized(client):
    # Missing gateway key header should return 401
    payload = {"studyId": "123e4567-e89b-12d3-a456-426614174000"}
    response = client.post("/api/v1/inference", json=payload)
    assert response.status_code == 401

def test_inference_success(client, auth_header):
    payload = {"studyId": "123e4567-e89b-12d3-a456-426614174000"}
    response = client.post("/api/v1/inference", json=payload, headers=auth_header)
    assert response.status_code == 200
    data = response.json()
    assert "jobId" in data
    assert data["status"] == "queued"

def test_list_models(client):
    response = client.get("/api/v1/models")
    assert response.status_code == 200
    assert len(response.json()) > 0
