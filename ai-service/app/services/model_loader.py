import torch
from app.models.model_manager import ModelWeightsManager
from app.models.gpu_manager import GPUManager
from loguru import logger

class ModelLoader:
    def __init__(self):
        self._manager = ModelWeightsManager()

    def get_device(self) -> torch.device:
        return self._manager.device

    def load_model(self, model_name: str, version: str) -> str:
        return self._manager.load_checkpoint(model_name, version)

    def list_loaded_models(self) -> list:
        return self._manager.get_loaded_models()

model_loader = ModelLoader()
