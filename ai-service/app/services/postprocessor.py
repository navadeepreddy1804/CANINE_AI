import numpy as np
from app.postprocessing.mask_cleaning import MaskCleaner
from app.postprocessing.bounding_boxes import BoundingBoxExtractor
from loguru import logger

class Postprocessor:
    def clean_mask(self, mask: np.ndarray) -> np.ndarray:
        return MaskCleaner.clean_mask(mask)

    def generate_bounding_boxes(self, mask: np.ndarray) -> list:
        return BoundingBoxExtractor.extract_bounding_boxes(mask)

postprocessor = Postprocessor()
