from pydantic import BaseModel, Field
from typing import List, Dict

class HealthResponse(BaseModel):
    status: str = Field(..., description="FastAPI microservice status flag", example="UP")
    gpuStatus: str = Field(..., description="CUDA capability status info string", example="CUDA 12.1 Enabled")
    cpuUsagePercent: float = Field(..., description="Host CPU workload utilization rate")
    memoryUsagePercent: float = Field(..., description="Host RAM load utilization rate")
    loadedModels: List[str] = Field(..., description="List of PyTorch weights checkpoints preloaded in memory")
    queueSize: int = Field(..., description="Background scheduler tasks waiting in queue")
