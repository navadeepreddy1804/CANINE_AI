from typing import Dict, Any, List
from loguru import logger

class ModelRegistry:
    def __init__(self):
        # Maps registered AI model names to configuration settings
        self._registry: Dict[str, Dict[str, Any]] = {
            "ToothSeg": {
                "name": "ToothSeg",
                "version": "v1.2.0",
                "framework": "PyTorch/MONAI",
                "task": "CBCT segmentation of maxilla/mandible structures",
                "parameters": 32_400_000,
                "inputShape": [1, 1, 96, 96, 96]
            },
            "ToothSegV2": {
                "name": "ToothSegV2",
                "version": "v2.0.0",
                "framework": "PyTorch/nnUNet",
                "task": "Enhanced CBCT segmentation of multi-class dental datasets",
                "parameters": 48_100_000,
                "inputShape": [1, 1, 128, 128, 128]
            }
        }

    def get_model_info(self, model_name: str) -> Dict[str, Any]:
        info = self._registry.get(model_name)
        if not info:
            logger.warning(f"AI Model {model_name} not found in registry. Falling back to ToothSeg.")
            return self._registry["ToothSeg"]
        return info

    def list_models(self) -> List[Dict[str, Any]]:
        return list(self._registry.values())

model_registry = ModelRegistry()
