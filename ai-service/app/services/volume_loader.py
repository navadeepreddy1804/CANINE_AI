from pathlib import Path
from typing import Tuple

import SimpleITK as sitk
import numpy as np


SUPPORTED_VOLUME_SUFFIXES = (".nii", ".nii.gz", ".mha", ".mhd", ".nrrd", ".dcm")


def load_uploaded_volume(study_directory: Path) -> Tuple[sitk.Image, np.ndarray]:
    """Load the actual uploaded CBCT volume from any supported medical-image format."""
    if study_directory.is_file():
        if not study_directory.name.lower().endswith(SUPPORTED_VOLUME_SUFFIXES):
            raise ValueError(f"Unsupported file format for uploaded volume: {study_directory}")
        image = sitk.ReadImage(str(study_directory))
    else:
        if not study_directory.is_dir():
            raise FileNotFoundError(f"Uploaded study directory does not exist: {study_directory}")

        files = sorted(path for path in study_directory.rglob("*") if path.is_file())
        volume_file = next((path for path in files if path.name.lower().endswith(SUPPORTED_VOLUME_SUFFIXES)), None)
        if volume_file and not volume_file.name.lower().endswith(".dcm"):
            image = sitk.ReadImage(str(volume_file))
        else:
            reader = sitk.ImageSeriesReader()
            dicom_names = reader.GetGDCMSeriesFileNames(str(study_directory))
            if not dicom_names:
                if volume_file and volume_file.name.lower().endswith(".dcm"):
                    image = sitk.ReadImage(str(volume_file))
                else:
                    raise ValueError("No readable DICOM series or supported 3D volume was uploaded")
            else:
                reader.SetFileNames(dicom_names)
                image = reader.Execute()

    volume = sitk.GetArrayFromImage(image)
    if volume.ndim != 3 or min(volume.shape) < 2:
        raise ValueError(f"Uploaded study is not a readable 3D CBCT volume: {volume.shape}")
    return image, volume
