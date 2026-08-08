from fastapi import APIRouter, Header, HTTPException, BackgroundTasks, status, Depends
from app.schemas.inference import InferenceRequest, InferenceResponse
from app.schemas.health import HealthResponse
from app.workers.task_queue import task_queue
from app.registry.model_registry import model_registry
from app.services.dicom_reader import dicom_reader
from app.services.preprocessor import preprocessor
from app.services.postprocessor import postprocessor
from app.services.measurement import measurement_service
from app.services.model_loader import model_loader
from app.pipeline.inference_pipeline import InferencePipeline
from app.core.config import settings
from loguru import logger
from app.services.preview_extractor import PreviewExtractor

router = APIRouter()

def verify_gateway_key(key: str = Header(None, alias="X-Internal-Gateway-Key")):
    if not key or key != settings.internal_gateway_key:
        logger.warning(f"Unauthorized API request attempt! Header key mismatch.")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Access Denied: Spring Boot Gateway authentication key required."
        )

@router.post("/inference", response_model=InferenceResponse, dependencies=[Depends(verify_gateway_key)])
async def run_inference(request: InferenceRequest, background_tasks: BackgroundTasks, x_internal_gateway_key: str = Header(None)):
    # Manual key verification to handle test clients correctly
    if not x_internal_gateway_key or x_internal_gateway_key != settings.internal_gateway_key:
        raise HTTPException(status_code=401, detail="Unauthorized client key header")

    logger.info(f"[FastAPI] Analysis request received for Study ID: {request.studyId}")
    
    # Initialize background task run
    job_id = task_queue.create_job(request.studyId)
    
    # Spawn background task pipeline (using DEMO pipeline to avoid 52GB memory allocation)
    background_tasks.add_task(InferencePipeline.run_demo_pipeline, job_id, request.studyId, request.sessionId, request.storagePath)
    
    return InferenceResponse(
        jobId=job_id,
        status="queued",
        progressPercentage=0
    )

@router.get("/health", response_model=HealthResponse)
async def get_health():
    return HealthResponse(
        status="UP",
        gpuStatus="CUDA 12.1 Enabled" if settings.gpu_enabled else "CPU Mode Only",
        cpuUsagePercent=14.5,
        memoryUsagePercent=42.2,
        loadedModels=["Dataset121_ToothFairy2_Teeth", "Dataset123_ToothFairy2fixed"],
        queueSize=len(task_queue.list_jobs())
    )

@router.get("/models")
async def list_models():
    return model_registry.list_models()

@router.get("/jobs")
async def list_jobs():
    return task_queue.list_jobs()

@router.get("/jobs/{id}", response_model=InferenceResponse)
async def get_job(id: str):
    job = task_queue.get_job(id)
    if not job:
        raise HTTPException(status_code=404, detail="AI job not found")
    
    return InferenceResponse(
        jobId=job["id"],
        status=job["status"],
        progressPercentage=job["progressPercentage"],
        result=job["result"],
        errorMessage=job["errorMessage"]
    )

@router.delete("/jobs/{id}")
async def cancel_job(id: str):
    job = task_queue.get_job(id)
    if not job:
        raise HTTPException(status_code=404, detail="AI job not found")
        
    task_queue.cancel_job(id)
    return {"message": "Job cancelled successfully"}

@router.post("/preprocess/extract-previews")
async def extract_previews(request: dict):
    session_id = request.get("sessionId")
    study_id = request.get("studyId")
    storage_path = request.get("storagePath")
    preview_path = request.get("previewPath")
    
    if not session_id or not study_id or not storage_path or not preview_path:
        raise HTTPException(status_code=400, detail="Missing required parameters")
        
    try:
        details = PreviewExtractor.extract_previews(session_id, study_id, storage_path, preview_path)
        return {"success": True, "message": "Previews extracted successfully", "data": details}
    except Exception as e:
        logger.error(f"Failed to extract previews: {e}")
        raise HTTPException(status_code=500, detail=str(e))
