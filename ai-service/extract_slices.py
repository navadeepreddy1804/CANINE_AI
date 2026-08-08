import sys
import os
from pathlib import Path
import cv2
import numpy as np
import SimpleITK as sitk

def extract_slices(file_path_str):
    try:
        resolved_path = Path(file_path_str)
        if not resolved_path.exists():
            return
            
        study_root_dir = resolved_path.parent if (resolved_path.is_file() or (resolved_path.is_dir() and resolved_path.name in ("original", "dicom"))) else resolved_path
        previews_dir = study_root_dir / "previews"
        axial_dir = previews_dir / "axial"
        
        if axial_dir.exists() and len(list(axial_dir.glob("*.png"))) >= 6:
            return
            
        axial_dir.mkdir(parents=True, exist_ok=True)
        
        # Load NIfTI
        if resolved_path.is_file() and str(resolved_path).endswith((".nii", ".nii.gz")):
            reader = sitk.ImageFileReader()
            reader.SetFileName(str(resolved_path))
            image = reader.Execute()
        elif resolved_path.is_dir():
            reader = sitk.ImageSeriesReader()
            dicom_names = reader.GetGDCMSeriesFileNames(str(resolved_path))
            if not dicom_names:
                return
            reader.SetFileNames(dicom_names)
            image = reader.Execute()
        else:
            return
            
        volume = sitk.GetArrayFromImage(image)
        z_len, y_len, x_len = volume.shape
        
        # Compute 6 preview indices
        start_z = int(z_len * 0.25)
        end_z = int(z_len * 0.85)
        if end_z <= start_z:
            end_z = start_z + 1
        step = max(1, (end_z - start_z) // 6)
        preview_indices = list(range(start_z, end_z, step))[:6]
        if not preview_indices:
            preview_indices = [z_len // 2]
            
        for idx, slice_z in enumerate(preview_indices):
            raw_slice = volume[slice_z, :, :]
            min_v, max_v = float(np.min(raw_slice)), float(np.max(raw_slice))
            if max_v > min_v:
                norm_slice = ((raw_slice - min_v) / (max_v - min_v + 1e-6) * 255.0).astype(np.uint8)
            else:
                norm_slice = np.zeros_like(raw_slice, dtype=np.uint8)
                
            color_slice = cv2.cvtColor(norm_slice, cv2.COLOR_GRAY2BGR)
            out_png = axial_dir / f"axial_{idx}.png"
            cv2.imwrite(str(out_png), color_slice)
            
        print("Successfully extracted slices.")
    except Exception as e:
        print(f"Failed to extract slices: {e}")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        extract_slices(sys.argv[1])
