# Use PyTorch with CUDA support as base image
FROM pytorch/pytorch:2.1.0-cuda12.1-cudnn8-runtime

WORKDIR /app

# Install system dependencies (including SimpleITK, OpenCV libraries)
RUN apt-get update && apt-get install -y \
    libglib2.0-0 \
    libgl1-mesa-glx \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy python dependencies list from FastAPI app requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code files
COPY . .

EXPOSE 8000

ENV PYTHONUNBUFFERED=1

# Run the FastAPI server with production configurations
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "-c", "gunicorn_config.py", "app.main:app"]
