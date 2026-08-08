import numpy as np
from app.preprocessing.normalization import Normalizer
from app.preprocessing.resampling import Resampler
from app.preprocessing.metadata_validator import MetadataValidator
from loguru import logger

class Preprocessor:
    def window_hu(self, volume: np.ndarray, window_center: int = 400, window_width: int = 1500) -> np.ndarray:
        return Normalizer.window_hu(volume, window_center, window_width)

    def crop_volume(self, volume: np.ndarray, crop_shape: tuple = (96, 96, 96)) -> np.ndarray:
        return Resampler.crop_and_pad(volume, crop_shape)

    def validate_metadata(self, metadata: dict) -> bool:
        return MetadataValidator.validate_dicom_metadata(metadata)

preprocessor = Preprocessor()
