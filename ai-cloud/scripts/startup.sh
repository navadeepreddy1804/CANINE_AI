#!/bin/bash
set -e

echo "Starting CanineAI Cloud Infrastructure Orchestrator..."

# Run GPU checks
./gpu-check.sh || echo "GPU check failed: running with CPU fallback configurations."

# Spin up Docker containers
docker compose -f ../deployment/docker-compose.yml up -d --build

echo "CanineAI Cloud Containers launched successfully."
