from fastapi import Request
from fastapi.responses import JSONResponse
from app.exceptions.custom_exceptions import AIException
from starlette.middleware.base import BaseHTTPMiddleware
from loguru import logger
import time

class ExceptionMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request: Request, call_next):
        start_time = time.time()
        try:
            response = await call_next(request)
            process_time = time.time() - start_time
            logger.info(f"Request: {request.method} {request.url.path} processed in {process_time:.4f}s")
            return response
        except AIException as ex:
            logger.warning(f"AI Exception caught in middleware: Status {ex.status_code} - {ex.message}")
            return JSONResponse(
                status_code=ex.status_code,
                content={
                    "success": False,
                    "code": ex.__class__.__name__,
                    "message": ex.message
                }
            )
        except Exception as ex:
            logger.error(f"Unhandled FastAPI exception caught in middleware: {ex}", exc_info=True)
            return JSONResponse(
                status_code=500,
                content={
                    "success": False,
                    "code": "INTERNAL_SERVER_ERROR",
                    "message": "An unexpected error occurred in AI engine: " + str(ex)
                }
            )
