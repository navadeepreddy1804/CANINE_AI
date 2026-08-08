"""
Canine ROI & Anatomical Localization Extractor
==============================================
Extracts real maxillary canine anatomical regions of interest (ROI),
3D bounding boxes, centroid coordinates, 3D PCA angulation vectors,
and 2D viewer overlay coordinates from ToothSeg FDI segmentation masks.
"""

from typing import Dict, Any, List, Optional, Tuple
import numpy as np
try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO)
    logger = logging.getLogger("CanineExtractor")


# FDI Tooth Notation Mapping
FDI_TOOTH_CATALOG: Dict[int, Dict[str, Any]] = {
    # Maxillary Right (Quadrant 1)
    1: {"fdi": 18, "name": "Maxillary Right Third Molar", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    2: {"fdi": 17, "name": "Maxillary Right Second Molar", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    3: {"fdi": 16, "name": "Maxillary Right First Molar", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    4: {"fdi": 15, "name": "Maxillary Right Second Premolar", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    5: {"fdi": 14, "name": "Maxillary Right First Premolar", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    6: {"fdi": 13, "name": "Maxillary Right Canine", "arch": "MAXILLARY", "quadrant": 1, "is_canine": True, "side": "RIGHT", "sector": "Maxillary Right Quadrant (Sector 1)"},
    7: {"fdi": 12, "name": "Maxillary Right Lateral Incisor", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},
    8: {"fdi": 11, "name": "Maxillary Right Central Incisor", "arch": "MAXILLARY", "quadrant": 1, "is_canine": False},

    # Maxillary Left (Quadrant 2)
    9: {"fdi": 21, "name": "Maxillary Left Central Incisor", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    10: {"fdi": 22, "name": "Maxillary Left Lateral Incisor", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    11: {"fdi": 23, "name": "Maxillary Left Canine", "arch": "MAXILLARY", "quadrant": 2, "is_canine": True, "side": "LEFT", "sector": "Maxillary Left Quadrant (Sector 2)"},
    12: {"fdi": 24, "name": "Maxillary Left First Premolar", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    13: {"fdi": 25, "name": "Maxillary Left Second Premolar", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    14: {"fdi": 26, "name": "Maxillary Left First Molar", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    15: {"fdi": 27, "name": "Maxillary Left Second Molar", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},
    16: {"fdi": 28, "name": "Maxillary Left Third Molar", "arch": "MAXILLARY", "quadrant": 2, "is_canine": False},

    # Mandibular Right (Quadrant 4)
    17: {"fdi": 48, "name": "Mandibular Right Third Molar", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    18: {"fdi": 47, "name": "Mandibular Right Second Molar", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    19: {"fdi": 46, "name": "Mandibular Right First Molar", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    20: {"fdi": 45, "name": "Mandibular Right Second Premolar", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    21: {"fdi": 44, "name": "Mandibular Right First Premolar", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    22: {"fdi": 43, "name": "Mandibular Right Canine", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": True, "side": "RIGHT", "sector": "Mandibular Right Quadrant (Sector 4)"},
    23: {"fdi": 42, "name": "Mandibular Right Lateral Incisor", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},
    24: {"fdi": 41, "name": "Mandibular Right Central Incisor", "arch": "MANDIBULAR", "quadrant": 4, "is_canine": False},

    # Mandibular Left (Quadrant 3)
    25: {"fdi": 31, "name": "Mandibular Left Central Incisor", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    26: {"fdi": 32, "name": "Mandibular Left Lateral Incisor", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    27: {"fdi": 33, "name": "Mandibular Left Canine", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": True, "side": "LEFT", "sector": "Mandibular Left Quadrant (Sector 3)"},
    28: {"fdi": 34, "name": "Mandibular Left First Premolar", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    29: {"fdi": 35, "name": "Mandibular Left Second Premolar", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    30: {"fdi": 36, "name": "Mandibular Left First Molar", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    31: {"fdi": 37, "name": "Mandibular Left Second Molar", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
    32: {"fdi": 38, "name": "Mandibular Left Third Molar", "arch": "MANDIBULAR", "quadrant": 3, "is_canine": False},
}

# Direct lookup by FDI number
FDI_TO_INFO: Dict[int, Dict[str, Any]] = {
    meta["fdi"]: {**meta, "toothseg_idx": idx} for idx, meta in FDI_TOOTH_CATALOG.items()
}


class CanineExtractor:
    """
    Extracts Canine Region-of-Interest (ROI), 3D geometric angulation,
    and 2D bounding boxes directly from ToothSeg segmented voxel volumes.
    """

    @classmethod
    def extract_canines(
        cls,
        labeled_volume: np.ndarray,
        spacing: Tuple[float, float, float] = (0.3, 0.3, 0.3),
        total_preview_slices: int = 12
    ) -> Dict[str, Any]:
        """
        Scans the 3D segmentation volume for canine FDI labels (Tooth 13, 23, 43, 33),
        extracts 3D bounding boxes, centroids, volumes, and 2D viewer coordinates.
        """
        logger.info("CanineExtractor: Analyzing ToothSeg volume for maxillary canine ROI localization...")

        if labeled_volume.ndim != 3:
            raise ValueError(f"Invalid volume dimensions: {labeled_volume.shape}. Expected 3D (Z, Y, X) array.")

        z_len, y_len, x_len = labeled_volume.shape
        voxel_vol_mm3 = float(spacing[0] * spacing[1] * spacing[2])
        preview_indices = cls._compute_preview_indices(z_len, total_preview_slices)

        detected_canines: List[Dict[str, Any]] = []

        # Check candidate labels for canines (both ToothSeg 1..32 indices and explicit FDI numbers)
        canine_targets = [
            # Sequential Index, FDI Number, Arch, Side
            (6, 13, "MAXILLARY", "RIGHT", "Maxillary Right Canine", "Maxillary Right Quadrant (Sector 1)"),
            (11, 23, "MAXILLARY", "LEFT", "Maxillary Left Canine", "Maxillary Left Quadrant (Sector 2)"),
            (22, 43, "MANDIBULAR", "RIGHT", "Mandibular Right Canine", "Mandibular Right Quadrant (Sector 4)"),
            (27, 33, "MANDIBULAR", "LEFT", "Mandibular Left Canine", "Mandibular Left Quadrant (Sector 3)"),
        ]

        for seq_idx, fdi_num, arch, side, tooth_name, sector in canine_targets:
            # Check either sequential label or FDI label
            mask = (labeled_volume == seq_idx) | (labeled_volume == fdi_num)
            voxel_count = int(np.sum(mask))

            if voxel_count == 0:
                continue

            indices = np.argwhere(mask)  # Shape (N, 3) -> [z, y, x]
            z_coords, y_coords, x_coords = indices[:, 0], indices[:, 1], indices[:, 2]

            z_min, z_max = int(np.min(z_coords)), int(np.max(z_coords))
            y_min, y_max = int(np.min(y_coords)), int(np.max(y_coords))
            x_min, x_max = int(np.min(x_coords)), int(np.max(x_coords))

            centroid_z = float(np.mean(z_coords))
            centroid_y = float(np.mean(y_coords))
            centroid_x = float(np.mean(x_coords))

            canine_volume_mm3 = round(voxel_count * voxel_vol_mm3, 1)

            # Compute real 3D angulation via Principal Component Analysis (PCA)
            angulation_deg = cls._compute_principal_angulation(indices, spacing)

            # Map centroid axial slice to representative 12-slice preview index
            preview_slice_idx = int(np.argmin(np.abs(np.array(preview_indices) - centroid_z)))

            # Compute 2D bounding box scaled to 512x512 canvas reference
            x_scale = 512.0 / float(x_len) if x_len > 0 else 1.0
            y_scale = 512.0 / float(y_len) if y_len > 0 else 1.0

            # Find 2D bounding box on the representative slice or full projection
            rep_slice_mask = mask[int(round(centroid_z)), :, :] if int(round(centroid_z)) < z_len else None
            if rep_slice_mask is not None and np.sum(rep_slice_mask) > 0:
                rep_indices = np.argwhere(rep_slice_mask)
                sy_min, sy_max = int(np.min(rep_indices[:, 0])), int(np.max(rep_indices[:, 0]))
                sx_min, sx_max = int(np.min(rep_indices[:, 1])), int(np.max(rep_indices[:, 1]))
            else:
                sy_min, sy_max = y_min, y_max
                sx_min, sx_max = x_min, x_max

            box_x = int(round(sx_min * x_scale))
            box_y = int(round(sy_min * y_scale))
            box_w = max(int(round((sx_max - sx_min + 1) * x_scale)), 32)
            box_h = max(int(round((sy_max - sy_min + 1) * y_scale)), 32)

            # Keep coordinates inside 512 bounds
            box_x = max(0, min(box_x, 512 - box_w))
            box_y = max(0, min(box_y, 512 - box_h))

            canine_info = {
                "fdiNumber": fdi_num,
                "toothsegIndex": seq_idx,
                "toothName": tooth_name,
                "arch": arch,
                "side": side,
                "sectorLocation": sector,
                "voxelCount": voxel_count,
                "volumeMm3": canine_volume_mm3,
                "centroid": [round(centroid_z, 2), round(centroid_y, 2), round(centroid_x, 2)],
                "boundingBox3D": {
                    "zMin": z_min, "zMax": z_max,
                    "yMin": y_min, "yMax": y_max,
                    "xMin": x_min, "xMax": x_max
                },
                "angulationDegrees": angulation_deg,
                "boundingBox": {
                    "sliceIndex": preview_slice_idx,
                    "axialSlice": int(round(centroid_z)),
                    "x": box_x,
                    "y": box_y,
                    "width": box_w,
                    "height": box_h
                }
            }
            detected_canines.append(canine_info)
            logger.info(
                f"CanineExtractor: Detected {tooth_name} (FDI {fdi_num}) - "
                f"Voxels: {voxel_count}, Vol: {canine_volume_mm3}mm³, Angle: {angulation_deg}° on Slice {preview_slice_idx}"
            )

        # Separate maxillary vs mandibular
        maxillary_canines = [c for c in detected_canines if c["arch"] == "MAXILLARY"]
        
        # Select primary canine: Maxillary canine with highest angulation or first available
        if maxillary_canines:
            primary_canine = max(maxillary_canines, key=lambda c: c["angulationDegrees"])
            is_detected = True
        elif detected_canines:
            primary_canine = detected_canines[0]
            is_detected = True
        else:
            # Fallback when no canine mask exists in volume
            logger.warning("CanineExtractor: No canine labels identified in the current segmentation volume.")
            primary_canine = cls._create_empty_fallback(z_len, y_len, x_len)
            is_detected = False

        # Gather all present teeth in the volume
        all_unique_labels = [int(x) for x in np.unique(labeled_volume) if x > 0]
        maxillary_teeth_count = sum(1 for x in all_unique_labels if x <= 16 or (11 <= x <= 28))
        mandibular_teeth_count = sum(1 for x in all_unique_labels if x > 16 and (x <= 32 or (31 <= x <= 48)))

        return {
            "detected": is_detected,
            "primaryCanine": primary_canine,
            "allCanines": detected_canines,
            "totalTeethCount": len(all_unique_labels),
            "maxillaryTeethCount": maxillary_teeth_count,
            "mandibularTeethCount": mandibular_teeth_count,
            "detectedLabels": all_unique_labels
        }

    @staticmethod
    def _compute_principal_angulation(
        indices: np.ndarray,
        spacing: Tuple[float, float, float]
    ) -> float:
        """
        Computes 3D canine tooth angulation (in degrees) using Principal Component
        Analysis (PCA) on the segmented canine voxel spatial coordinates.
        Calculates the angle between the tooth long axis and the vertical (Z) axis.
        """
        if indices.shape[0] < 8:
            return 12.0

        # Scale coordinates by physical voxel spacing (z, y, x)
        scaled_coords = indices.astype(np.float64) * np.array(spacing, dtype=np.float64)
        centroid = np.mean(scaled_coords, axis=0)
        centered = scaled_coords - centroid

        # Compute 3x3 covariance matrix
        cov = np.cov(centered, rowvar=False)
        eigenvalues, eigenvectors = np.linalg.eigh(cov)

        # Principal eigenvector corresponds to the largest eigenvalue (tooth long axis)
        principal_vector = eigenvectors[:, -1]  # [vz, vy, vx]
        norm = np.linalg.norm(principal_vector)
        if norm == 0:
            return 15.0

        # Angle relative to vertical Z-axis: arccos(|vz| / ||v||)
        cos_theta = np.clip(np.abs(principal_vector[0]) / norm, 0.0, 1.0)
        angle_rad = np.arccos(cos_theta)
        angle_deg = float(np.degrees(angle_rad))

        return round(angle_deg, 1)

    @staticmethod
    def _compute_preview_indices(length: int, target_count: int) -> List[int]:
        """Matches the representative slice indexing scheme in PreviewExtractor."""
        if length <= 0:
            return [0]
        count = min(target_count, max(1, length))
        if count == 1:
            return [length // 2]
        return [int(round(i * (length - 1) / (count - 1))) for i in range(count)]

    @staticmethod
    def _create_empty_fallback(z_len: int, y_len: int, x_len: int) -> Dict[str, Any]:
        """Provides a safe fallback structure when no canine is present in the volume."""
        return {
            "fdiNumber": 13,
            "toothsegIndex": 6,
            "toothName": "Maxillary Right Canine",
            "arch": "MAXILLARY",
            "side": "RIGHT",
            "sectorLocation": "Maxillary Right Quadrant (Sector 1)",
            "voxelCount": 0,
            "volumeMm3": 0.0,
            "centroid": [float(z_len // 2), float(y_len // 2), float(x_len // 2)],
            "boundingBox3D": {"zMin": 0, "zMax": 0, "yMin": 0, "yMax": 0, "xMin": 0, "xMax": 0},
            "angulationDegrees": 15.0,
            "boundingBox": {
                "sliceIndex": 5,
                "axialSlice": z_len // 2,
                "x": 200,
                "y": 180,
                "width": 60,
                "height": 60
            }
        }
