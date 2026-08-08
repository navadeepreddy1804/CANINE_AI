#!/bin/bash

echo "=========================================================="
echo "Running GPU Hardware check: CUDA & Driver validation"
echo "=========================================================="

if ! command -v nvidia-smi &> /dev/null
then
    echo "WARNING: nvidia-smi could not be found. NVIDIA Drivers are not loaded."
    exit 1
fi

# Print GPU properties
nvidia-smi --query-gpu=name,memory.total,driver_version --format=csv

# Verify docker GPU pass-through is active
if ! docker run --rm --gpus all nvidia/cuda:12.1.1-base-ubuntu22.04 nvidia-smi &> /dev/null
then
    echo "ERROR: Docker GPU pass-through is not working."
    exit 2
fi

echo "GPU Verification success: NVIDIA hardware is ready for Docker execution."
exit 0
