#!/bin/bash
set -e

echo "=========================================================="
echo "Installing NVIDIA Drivers & CUDA Toolkit on Ubuntu 22.04 LTS"
echo "=========================================================="

# Update system packages
sudo apt-get update -y
sudo apt-get upgrade -y

# Install essential dependencies
sudo apt-get install -y build-essential dkms freeglut3-dev libxmu-dev libxi-dev

# Add NVIDIA driver repository
sudo add-apt-repository ppa:graphics-drivers/ppa -y
sudo apt-get update -y

# Install recommended NVIDIA driver (e.g. 535)
sudo apt-get install -y nvidia-driver-535

# Download and install CUDA 12.1 toolkit
wget https://developer.download.nvidia.com/compute/cuda/12.1.1/local_installers/cuda_12.1.1_530.30.02_linux.run
sudo sh cuda_12.1.1_530.30.02_linux.run --silent --toolkit --driver

# Configure paths
echo 'export PATH=/usr/local/cuda-12.1/bin${PATH:+:${PATH}}' >> ~/.bashrc
echo 'export LD_LIBRARY_PATH=/usr/local/cuda-12.1/lib64\${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}' >> ~/.bashrc

echo "CUDA 12.1 installation completed successfully."
