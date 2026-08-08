from typing import Dict, Any, Optional
import numpy as np
from app.analysis.clinical_measurements import ClinicalMeasurementsCalculator

class MeasurementService:
    def calculate_statistics(
        self,
        volume: np.ndarray,
        voxel_spacing: Any = 0.3,
        canine_roi: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        return ClinicalMeasurementsCalculator.calculate_statistics(volume, voxel_spacing, canine_roi)

measurement_service = MeasurementService()
