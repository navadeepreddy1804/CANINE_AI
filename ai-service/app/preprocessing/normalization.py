import numpy as np
from loguru import logger

class Normalizer:
    @staticmethod
    def window_hu(volume: np.ndarray, window_center: int = 400, window_width: int = 1500) -> np.ndarray:
        """
        Applies Hounsfield Unit (HU) windowing for bone windowing.
        Clips the intensities between min and max levels and scales to [0, 1].
        """
        logger.info(f"Normalizing HU: Center={window_center}, Width={window_width}")
        min_value = window_center - (window_width / 2.0)
        max_value = window_center + (window_width / 2.0)
        
        normalized = np.clip(volume, min_value, max_value)
        normalized = (normalized - min_value) / (max_value - min_value)
        return normalized.astype(np.float32)

    @staticmethod
    def intensity_scaling(volume: np.ndarray, scale_factor: float = 1.0) -> np.ndarray:
        """
        Scales intensity values by a multiplier factor.
        """
        logger.info(f"Scaling intensities by factor: {scale_factor}")
        return volume * scale_factor
