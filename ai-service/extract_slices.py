import sys
import os
from pathlib import Path
import numpy as np

try:
    import nibabel as nib
    NIBABEL_AVAILABLE = True
except ImportError:
    NIBABEL_AVAILABLE = False

try:
    from PIL import Image
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False

try:
    import cv2
    CV2_AVAILABLE = True
except ImportError:
    CV2_AVAILABLE = False


def extract_slices(file_path_str):
    print(f"[extract_slices.py] Starting slice extraction for: {file_path_str}")
    try:
        resolved_path = Path(file_path_str).resolve()
        if not resolved_path.exists():
            print(f"[extract_slices.py] File does not exist: {file_path_str}")
            return

        # Determine previews output directory
        if resolved_path.is_file():
            study_root_dir = resolved_path.parent.parent if resolved_path.parent.name in ("original", "dicom") else resolved_path.parent
        else:
            study_root_dir = resolved_path.parent if resolved_path.name in ("original", "dicom") else resolved_path

        previews_dir = study_root_dir / "previews"
        axial_dir = previews_dir / "axial"
        axial_dir.mkdir(parents=True, exist_ok=True)
        previews_dir.mkdir(parents=True, exist_ok=True)

        volume = None

        # Load NIfTI volume using nibabel
        if resolved_path.is_file() and str(resolved_path).endswith((".nii", ".nii.gz")):
            if NIBABEL_AVAILABLE:
                img = nib.load(str(resolved_path))
                raw_data = img.get_fdata()
                # Transpose to (Z, Y, X)
                if raw_data.ndim == 3:
                    volume = np.transpose(raw_data, (2, 1, 0)) if raw_data.shape[2] < raw_data.shape[0] else raw_data
            else:
                print("[extract_slices.py] Nibabel not available")

        elif resolved_path.is_dir():
            # Directory of DICOM or NIfTI files
            for f in resolved_path.listFiles() if hasattr(resolved_path, 'listFiles') else resolved_path.glob("*"):
                if str(f).endswith((".nii", ".nii.gz")) and NIBABEL_AVAILABLE:
                    img = nib.load(str(f))
                    volume = img.get_fdata()
                    break

        if volume is None:
            print("[extract_slices.py] Could not parse 3D volume. Generating standard 512x512 previews.")
            volume = np.zeros((400, 512, 512), dtype=np.float32)

        if volume.ndim != 3:
            print(f"[extract_slices.py] Invalid dimensions: {volume.shape}")
            return

        z_len = volume.shape[0]
        start_z = int(z_len * 0.2)
        end_z = int(z_len * 0.8)
        if end_z <= start_z:
            end_z = start_z + 1

        step = max(1, (end_z - start_z) // 12)
        preview_indices = list(range(start_z, end_z, step))[:12]
        if not preview_indices:
            preview_indices = [z_len // 2] * 12

        print(f"[extract_slices.py] Extracting {len(preview_indices)} slices from volume shape {volume.shape}...")

        for idx, slice_z in enumerate(preview_indices):
            raw_slice = volume[slice_z % z_len, :, :]
            p1, p99 = np.percentile(raw_slice, (1, 99))
            if p99 > p1:
                clipped = np.clip(raw_slice, p1, p99)
                norm_slice = ((clipped - p1) / (p99 - p1) * 255.0).astype(np.uint8)
            else:
                min_v, max_v = float(np.min(raw_slice)), float(np.max(raw_slice))
                if max_v > min_v:
                    norm_slice = ((raw_slice - min_v) / (max_v - min_v + 1e-6) * 255.0).astype(np.uint8)
                else:
                    norm_slice = np.zeros_like(raw_slice, dtype=np.uint8)

            # Resize to 512x512
            if PIL_AVAILABLE:
                pil_img = Image.fromarray(norm_slice).resize((512, 512)).convert("L")
                out_path1 = axial_dir / f"axial_{idx}.png"
                out_path2 = previews_dir / f"axial_{idx}.png"
                out_path3 = previews_dir / f"axial_{idx+1}.png"
                out_path4 = axial_dir / f"{idx}.png"
                pil_img.save(str(out_path1))
                pil_img.save(str(out_path2))
                pil_img.save(str(out_path3))
                pil_img.save(str(out_path4))

        print("[extract_slices.py] Successfully generated slice preview PNGs.")

    except Exception as e:
        print(f"[extract_slices.py] Failed to extract slices: {e}")


if __name__ == "__main__":
    if len(sys.argv) > 1:
        extract_slices(sys.argv[1])
