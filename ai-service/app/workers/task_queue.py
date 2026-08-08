import threading
import uuid
import time
from typing import Dict, Any, Optional
from loguru import logger

class TaskQueue:
    def __init__(self):
        self._jobs: Dict[str, Dict[str, Any]] = {}
        self._lock = threading.Lock()

    def create_job(self, study_id: str) -> str:
        job_id = str(uuid.uuid4())
        with self._lock:
            self._jobs[job_id] = {
                "id": job_id,
                "studyId": study_id,
                "status": "queued",
                "progressPercentage": 0,
                "createdAt": time.time(),
                "result": None,
                "errorMessage": None
            }
        logger.info(f"Registered background AI task run job: {job_id} for study: {study_id}")
        return job_id

    def update_job(self, job_id: str, status: str, progress: int, result: Optional[Dict[str, Any]] = None, error: Optional[str] = None):
        with self._lock:
            if job_id in self._jobs:
                self._jobs[job_id]["status"] = status
                self._jobs[job_id]["progressPercentage"] = progress
                if result is not None:
                    self._jobs[job_id]["result"] = result
                if error is not None:
                    self._jobs[job_id]["errorMessage"] = error
        logger.debug(f"Updated AI task job status: {job_id} -> {status} ({progress}%)")

    def get_job(self, job_id: str) -> Optional[Dict[str, Any]]:
        with self._lock:
            return self._jobs.get(job_id)

    def cancel_job(self, job_id: str):
        with self._lock:
            if job_id in self._jobs:
                self._jobs[job_id]["status"] = "cancelled"
                self._jobs[job_id]["errorMessage"] = "Task cancelled by clinician request"
        logger.info(f"Cancelled running AI task job: {job_id}")

    def list_jobs(self) -> list:
        with self._lock:
            return list(self._jobs.values())

task_queue = TaskQueue()
