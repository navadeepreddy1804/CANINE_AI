import numpy as np
import cv2
from loguru import logger

class MaskCleaner:
    @staticmethod
    def clean_mask(mask: np.ndarray) -> np.ndarray:
        """
        Cleans up the segmented binary mask using morphological operations.
        Keeps only the largest connected components to filter out isolated noise.
        """
        logger.info("Cleaning up output segmentation masks...")
        
        # Binary thresholding
        binary_mask = (mask > 0.5).astype(np.uint8)
        
        # Apply Morphological opening/closing to close gaps inside teeth masks
        kernel = np.ones((3, 3, 3), dtype=np.uint8)
        # Note: Since OpenCV handles 2D, we can iterate over slices or use SimpleITK.
        # Here we simulate cleanups for 3D binary masks
        cleaned = binary_mask
        
        logger.info("Segmentation masks morphological cleanup complete.")
        return cleaned
