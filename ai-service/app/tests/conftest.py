import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.core.config import settings

@pytest.fixture(scope="module")
def client():
    # Setup test gateway keys environment settings
    settings.internal_gateway_key = "testInternalGatewayApiKey"
    with TestClient(app) as c:
        yield c

@pytest.fixture
def auth_header():
    return {"X-Internal-Gateway-Key": "testInternalGatewayApiKey"}
