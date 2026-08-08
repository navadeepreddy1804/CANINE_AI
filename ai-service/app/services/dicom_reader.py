import os
import pydicom
import numpy as np
from typing import List, Dict, Any, Tuple
from loguru import logger
from app.core.config import settings

class DicomReader:
    def read_metadata(self, file_path: str) -> Dict[str, Any]:
        logger.info(f"Reading DICOM headers from: {file_path}")
        try:
            ds = pydicom.dcmread(file_path, stop_before_pixels=True)
            return {
                "patient_name": str(ds.get("PatientName", "Unknown")),
                "patient_id": str(ds.get("PatientID", "Unknown")),
                "study_instance_uid": str(ds.get("StudyInstanceUID", "")),
                "series_instance_uid": str(ds.get("SeriesInstanceUID", "")),
                "modality": str(ds.get("Modality", "CT")),
                "study_date": str(ds.get("StudyDate", "")),
                "manufacturer": str(ds.get("Manufacturer", "Carestream"))
            }
        except Exception as e:
            logger.error(f"Failed to read DICOM headers at: {file_path}", e)
            raise RuntimeError(f"Corrupted DICOM file header metadata: {file_path}", e)

    def load_3d_volume(self, folder_path: str) -> Tuple[np.ndarray, Dict[str, Any]]:
        logger.info(f"Assembling 3D volume from folder: {folder_path}")
        
        # Guard directory traversal
        abs_folder = os.path.abspath(folder_path)
        if not abs_folder.startswith(os.path.abspath(settings.upload_dir)):
            raise PermissionError("Access denied: Invalid folder directory context.")

        slices = []
        for root, _, files in os.walk(folder_path):
            for file in files:
                if file.endswith(".dcm"):
                    slices.append(os.path.join(root, file))

        if not slices:
            raise FileNotFoundError(f"No DICOM slices found in path: {folder_path}")

        slices_data = []
        for file_path in slices:
            try:
                ds = pydicom.dcmread(file_path)
                slices_data.append(ds)
            except Exception as e:
                logger.warning(f"Skipping corrupted slice: {file_path} - {e}")

        # Sort slices by Instance Number or Image Position (Patient)
        slices_data.sort(key=lambda s: int(s.get("InstanceNumber", 0)))
        
        if not slices_data:
            raise ValueError(f"No valid slices could be parsed from folder: {folder_path}")

        # Compile 3D volume stack
        volume = np.stack([s.pixel_array for s in slices_data], axis=0)
        logger.info(f"Assembled NumPy volume with dimensions shape: {volume.shape}")

        sample = slices_data[0]
        meta = {
            "spacing": [float(x) for x in sample.get("PixelSpacing", [0.075, 0.075])],
            "thickness": float(sample.get("SliceThickness", 0.3)),
            "rows": int(sample.get("Rows", 512)),
            "columns": int(sample.get("Columns", 512))
        }

        return volume, meta

dicom_reader = DicomReader()
