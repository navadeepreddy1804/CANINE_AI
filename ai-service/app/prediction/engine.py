from abc import ABC, abstractmethod
from typing import Any
import numpy as np


class PredictionEngine(ABC):
    """Prediction boundary; replace this implementation when trained weights are available."""

    @abstractmethod
    def predict(self, volume: np.ndarray, metadata: dict[str, Any]) -> dict[str, Any]:
        raise NotImplementedError
