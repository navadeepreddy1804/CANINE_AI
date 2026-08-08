from fastapi import FastAPI, Request
from loguru import logger
from app.api.endpoints import router
from app.core.logging import setup_logging
from app.core.config import settings

from app.middleware.exception_middleware import ExceptionMiddleware

# Setup Console logging formats
setup_logging()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description="Diagnostics AI Microservice running PyTorch/MONAI segmentation networks."
)

# Register exception middleware
app.add_middleware(ExceptionMiddleware)

@app.middleware("http")
async def log_requests(request: Request, call_next):
    logger.info(f"Incoming AI Request: {request.method} {request.url}")
    response = await call_next(request)
    logger.info(f"Completed AI Request: {request.method} {request.url} - Status: {response.status_code}")
    return response

# Route mappings prefixing v1 APIs
app.include_router(router, prefix="/api/v1")

@app.get("/")
async def root():
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "status": "UP"
    }

@app.get("/health")
async def health():
    return {
        "status": "ok",
        "service": "CanineAI Demo AI",
        "mode": settings.ai_mode
    }
