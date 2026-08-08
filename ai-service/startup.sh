#!/bin/bash
echo "Starting Enterprise FastAPI AI Diagnostics microservice..."
# Start Gunicorn server binding to port 8000 with Uvicorn worker threads
exec gunicorn app.main:app \
    --workers 2 \
    --worker-class uvicorn.workers.UvicornWorker \
    --bind 0.0.0.0:8000 \
    --timeout 120 \
    --keep-alive 5
