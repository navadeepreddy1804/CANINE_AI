#!/bin/bash

# Port mapping parameters
PORT=${PORT:-8000}

echo "Checking Cloud AI Service health on port: ${PORT}..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:${PORT}/api/v1/health)

if [ "$RESPONSE" -eq 200 ]; then
    echo "Cloud AI Service is healthy."
    exit 0
else
    echo "ERROR: Cloud AI Service health check failed with status: ${RESPONSE}"
    exit 1
fi
