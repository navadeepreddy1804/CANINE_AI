import json
import os
import shutil
from pathlib import Path
import cv2
import numpy as np
import SimpleITK as sitk
from loguru import logger
from app.core.config import settings
from app.services.volume_loader import load_uploaded_volume

class PreviewExtractor:
    @staticmethod
    def extract_previews(session_id: str, study_id: str, storage_path: str, preview_path: str):
        """
        Reads a 3D scan volume (DICOM series folder or NIfTI file), extracts
        representative axial, coronal, and sagittal slices, normalizes intensities,
        and writes a preview manifest and PNG assets.
        """
        session_dir = None
        if Path(storage_path).exists():
            session_dir = Path(storage_path)
        else:
            upload_roots = settings.candidate_upload_roots()
            for root in upload_roots:
                candidate_dir = Path(root) / storage_path
                if candidate_dir.exists():
                    session_dir = candidate_dir
                    break

        if session_dir is None:
            raise FileNotFoundError(f"Session upload folder/file not found for session: {session_id} (path: {storage_path})")

        upload_roots = settings.candidate_upload_roots()
        previews_dirs = [Path(root) / preview_path for root in upload_roots]
        for previews_dir in previews_dirs:
            if previews_dir.exists() and previews_dir.is_dir():
                # Discard previous previews to prevent reuse
                shutil.rmtree(previews_dir)
            previews_dir.mkdir(parents=True, exist_ok=True)

        logger.info(f"Extracting representative slices for session {session_id} -> study {study_id}")

        image, volume = load_uploaded_volume(session_dir)
        logger.info("Loaded 3D volume with shape Z, Y, X: %s", volume.shape)

        if volume.ndim != 3:
            raise ValueError(f"Unsupported volume shape for preview extraction: {volume.shape}")

        z_len, y_len, x_len = volume.shape
        axial_slice = volume[z_len // 2, :, :]
        coronal_slice = volume[:, y_len // 2, :]
        sagittal_slice = volume[:, :, x_len // 2]

        # Generate representative index sets for each plane
        preview_indices = PreviewExtractor.compute_preview_indices(z_len, 12)
        coronal_indices = PreviewExtractor.compute_preview_indices(y_len, 8)
        sagittal_indices = PreviewExtractor.compute_preview_indices(x_len, 8)

        metadata = {
            "dimensions": {"z": z_len, "y": y_len, "x": x_len},
            "spacing": {
                "x": float(image.GetSpacing()[0]) if len(image.GetSpacing()) > 0 else 1.0,
                "y": float(image.GetSpacing()[1]) if len(image.GetSpacing()) > 1 else 1.0,
                "z": float(image.GetSpacing()[2]) if len(image.GetSpacing()) > 2 else 1.0
            },
            "sliceCounts": {"axial": z_len, "coronal": y_len, "sagittal": x_len},
            "previews": {
                "axial": {"middle": "axial.png", "indexedCount": len(preview_indices)},
                "coronal": {"middle": "coronal.png", "indexedCount": len(coronal_indices)},
                "sagittal": {"middle": "sagittal.png", "indexedCount": len(sagittal_indices)}
            }
        }

        for previews_dir in previews_dirs:
            PreviewExtractor.save_slice_as_png(axial_slice, str(previews_dir / "axial.png"))
            PreviewExtractor.save_slice_as_png(coronal_slice, str(previews_dir / "coronal.png"))
            PreviewExtractor.save_slice_as_png(sagittal_slice, str(previews_dir / "sagittal.png"))

            for idx, slice_idx in enumerate(preview_indices):
                rep = volume[slice_idx, :, :]
                PreviewExtractor.save_slice_as_png(rep, str(previews_dir / f"axial_{idx}.png"))

            for idx, slice_idx in enumerate(coronal_indices):
                rep = volume[:, slice_idx, :]
                PreviewExtractor.save_slice_as_png(rep, str(previews_dir / f"coronal_{idx}.png"))

            for idx, slice_idx in enumerate(sagittal_indices):
                rep = volume[:, :, slice_idx]
                PreviewExtractor.save_slice_as_png(rep, str(previews_dir / f"sagittal_{idx}.png"))

            with open(previews_dir / "preview_manifest.json", "w", encoding="utf-8") as manifest_file:
                json.dump(metadata, manifest_file, indent=2)

        logger.info("Previews and manifest saved successfully in: %s", previews_dirs[0])
        return {
            "dimensions": metadata["dimensions"],
            "spacing": metadata["spacing"],
            "output_folder": str(previews_dirs[0])
        }

    @staticmethod
    def compute_preview_indices(length: int, target_count: int) -> list[int]:
        if length <= 0:
            return [0]
        count = min(target_count, max(1, length))
        if count == 1:
            return [length // 2]
        return [int(round(i * (length - 1) / (count - 1))) for i in range(count)]

    @staticmethod
    def save_slice_as_png(slice_data: np.ndarray, output_path: str):
        min_val = float(np.min(slice_data))
        max_val = float(np.max(slice_data))
        if max_val - min_val > 0:
            normalized = ((slice_data - min_val) / (max_val - min_val) * 255.0).astype(np.uint8)
        else:
            normalized = np.zeros(slice_data.shape, dtype=np.uint8)

        cv2.imwrite(output_path, normalized)
