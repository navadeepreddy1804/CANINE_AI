from pydantic import BaseModel, Field
from typing import Optional, Dict, Any

class InferenceRequest(BaseModel):
    studyId: str = Field(..., description="Unique EMR UUID identifier of the CBCT scan study dataset")
    sessionId: Optional[str] = Field(default=None, description="Upload session containing the actual CBCT source files")
    storagePath: Optional[str] = Field(default=None, description="Direct filesystem storage path of the study files")

class InferenceResponse(BaseModel):
    jobId: str = Field(..., description="FastAPI internal tracking UUID of the analysis task")
    status: str = Field(..., description="Current stage transition status: queued, running, completed")
    progressPercentage: int = Field(..., description="Task progress details percentage")
    result: Optional[Dict[str, Any]] = Field(default=None, description="Result payload details in structured JSON")
    errorMessage: Optional[str] = Field(default=None, description="Detailed explanation of execution errors")
