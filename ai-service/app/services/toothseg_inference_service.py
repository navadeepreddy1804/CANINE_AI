"""
ToothSeg Native In-Process Python Inference Service
===================================================
Provides the enterprise segmentation foundation for CANINE_AI.
Directly invokes nnUNetPredictor and ToothSeg algorithmic modules in Python
without subprocesses, os.system, or shell scripts.
"""

import os
import gc
import json
import time
import tempfile
import threading
from pathlib import Path
from dataclasses import dataclass, field
from typing import Tuple, Dict, Any, Optional, Union, List

import numpy as np
import SimpleITK as sitk
import nibabel
import pandas as pd
import torch
from scipy.stats import multivariate_normal
try:
    from loguru import logger
except ImportError:
    import logging
    logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
    logger = logging.getLogger("ToothSeg")


from app.core.config import settings
from app.services.volume_loader import load_uploaded_volume

# Ensure nnUNet environment variables are registered
settings.setup_nnunet_environment()

try:
    from nnunetv2.inference.predict_from_raw_data import nnUNetPredictor
    from nnunetv2.preprocessing.preprocessors.default_preprocessor import compute_new_shape
    from nnunetv2.preprocessing.resampling.resample_torch import resample_torch_simple
    from acvl_utils.instance_segmentation.instance_as_semantic_seg import (
        convert_semantic_to_instanceseg,
        postprocess_instance_segmentation,
    )
    _NNUNET_AVAILABLE = True
except ImportError:
    _NNUNET_AVAILABLE = False
    nnUNetPredictor = None
    compute_new_shape = None
    resample_torch_simple = None
    convert_semantic_to_instanceseg = None
    postprocess_instance_segmentation = None


@dataclass
class ToothSegSegmentationResult:
    """Standardized output structure for ToothSeg segmentation operations."""
    labeled_volume: np.ndarray
    spacing: Tuple[float, float, float]
    origin: Tuple[float, float, float]
    direction: Tuple[float, ...]
    orientation: str
    metadata: Dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "spacing": list(self.spacing),
            "origin": list(self.origin),
            "direction": list(self.direction),
            "orientation": self.orientation,
            "metadata": self.metadata,
        }


class ToothSegInferenceService:
    """
    Enterprise-grade, in-process ToothSeg multi-class dental segmentation engine.
    Executes the 5-stage inference workflow directly in Python:
      1. Target spacing normalization (0.2 x 0.2 x 0.2 mm)
      2. 33-class Semantic Segmentation (Dataset 121)
      3. Border-core Instance Segmentation (Dataset 123)
      4. Morphological instance extraction
      5. Reference resampling & Dynamic Programming FDI tooth assignment
    """

    def __init__(self):
        self._lock = threading.Lock()
        self._device = self._resolve_device()
        self._model_121_path: Optional[Path] = None
        self._model_123_path: Optional[Path] = None
        self._fdi_distributions_path: Optional[Path] = None
        self._fdi_normals: Optional[List[List[Any]]] = None
        self._initialize_paths()

    def _resolve_device(self) -> torch.device:
        if settings.gpu_enabled and torch.cuda.is_available():
            dev_name = torch.cuda.get_device_name(0)
            logger.info(f"ToothSegInferenceService: Utilizing CUDA device -> {dev_name}")
            return torch.device("cuda:0")
        if settings.cpu_fallback:
            logger.warning("ToothSegInferenceService: CUDA unavailable/disabled. Defaulting to CPU fallback.")
            return torch.device("cpu")
        raise RuntimeError("GPU execution requested but CUDA is not available and CPU fallback is disabled.")

    def _initialize_paths(self) -> None:
        """Resolves model checkpoint directories and FDI distribution prior paths."""
        results_dir = Path(settings.nnunet_results).resolve()
        
        # Dataset 121 (Semantic Branch)
        self._model_121_path = results_dir / "Dataset121_ToothFairy2_Teeth" / "nnUNetTrainer_onlyMirror01_DASegOrd0__nnUNetPlans__3d_fullres_resample_torch_256_bs8_ctnorm"
        
        # Dataset 123 (Instance Branch)
        self._model_123_path = results_dir / "Dataset123_ToothFairy2fixed_teeth_spacing02_brd3px" / "nnUNetTrainer__nnUNetPlans__3d_fullres_resample_torch_192_bs8_ctnorm"
        
        # FDI distribution priors
        self._fdi_distributions_path = Path(settings.toothseg_distributions_path).resolve()
        if not self._fdi_distributions_path.is_file():
            # Attempt relative lookup fallback
            repo_lookup = Path(__file__).resolve().parents[3] / "E:/AI-Models/ToothSeg/toothseg/datasets/toothfairy2/fdi_pair_distrs.json"
            if repo_lookup.is_file():
                self._fdi_distributions_path = repo_lookup

    def _verify_readiness(self) -> None:
        """Validates that all checkpoints, modules, and distribution assets exist before execution."""
        if not _NNUNET_AVAILABLE:
            raise ImportError(
                "nnunetv2 or acvl_utils is not installed in the active Python environment. "
                "Ensure the ToothSeg/nnUNet environment is activated."
            )
        if not self._model_121_path or not (self._model_121_path / "fold_5" / "checkpoint_final.pth").is_file():
            raise FileNotFoundError(
                f"ToothSeg Dataset 121 checkpoint missing at: {self._model_121_path / 'fold_5' / 'checkpoint_final.pth'}. "
                "Ensure NNUNET_RESULTS environment variable points to a valid nnUNet_results directory."
            )
        if not self._model_123_path or not (self._model_123_path / "fold_5" / "checkpoint_final.pth").is_file():
            raise FileNotFoundError(
                f"ToothSeg Dataset 123 checkpoint missing at: {self._model_123_path / 'fold_5' / 'checkpoint_final.pth'}. "
                "Ensure NNUNET_RESULTS environment variable points to a valid nnUNet_results directory."
            )
        if not self._fdi_distributions_path or not self._fdi_distributions_path.is_file():
            raise FileNotFoundError(
                f"FDI distribution priors JSON missing at: {self._fdi_distributions_path}. "
                "Configure TOOTHSEG_FDI_DISTRIBUTIONS_PATH in .env or settings."
            )

    def _load_fdi_priors(self) -> List[List[Any]]:
        """Loads and memoizes Gaussian pairwise transition distributions for FDI labeling."""
        if self._fdi_normals is not None:
            return self._fdi_normals

        with open(self._fdi_distributions_path, "r", encoding="utf-8") as f:
            pair_dists = json.load(f)

        normals = []
        for i in range(32):
            normals.append([])
            for j in range(32):
                if i // 16 != j // 16:
                    normals[-1].append(None)
                    continue
                normal = multivariate_normal(
                    mean=pair_dists["means"][i][j][:2],
                    cov=np.array(pair_dists["covs"][i][j])[:2, :2],
                )
                normals[-1].append(normal)

        self._fdi_normals = normals
        return self._fdi_normals

    def _cleanup_gpu(self) -> None:
        """Aggressively reclaims device memory between neural network inferences."""
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()

    def segment_study(self, study_path: Union[str, Path]) -> ToothSegSegmentationResult:
        """
        Public API: Accepts a path to a directory (DICOM series) or a 3D medical volume file
        (.nii, .nii.gz, .dcm, .mha, .nrrd) and executes the full ToothSeg segmentation workflow.
        """
        path = Path(study_path).resolve()
        if not path.exists():
            raise FileNotFoundError(f"Input study path does not exist: {path}")

        if path.is_dir():
            image, volume = load_uploaded_volume(path)
        else:
            if path.suffix.lower() == ".dcm":
                image = sitk.ReadImage(str(path))
            else:
                image = sitk.ReadImage(str(path))
            volume = sitk.GetArrayFromImage(image)

        return self.segment_volume(image, volume)

    def segment_volume(
        self,
        image: sitk.Image,
        volume: Optional[np.ndarray] = None
    ) -> ToothSegSegmentationResult:
        """
        Public API: Segments a preloaded SimpleITK image / NumPy array directly in-process.
        Thread-safe execution protected by a mutex lock.
        """
        with self._lock:
            start_time = time.time()
            self._verify_readiness()

            if volume is None:
                volume = sitk.GetArrayFromImage(image)

            if volume.ndim != 3 or min(volume.shape) < 2:
                raise ValueError(f"Invalid 3D CBCT volume dimensions: {volume.shape}. Expected 3D array.")

            spacing = image.GetSpacing()
            origin = image.GetOrigin()
            direction = image.GetDirection()

            # Execute pipeline inside an isolated temporary directory
            with tempfile.TemporaryDirectory(prefix="toothseg_inproc_") as tmp_dir_str:
                tmp_dir = Path(tmp_dir_str)
                dir_raw = tmp_dir / "00_raw_input"
                dir_resized_02 = tmp_dir / "01_resized_02mm"
                dir_sem_out = tmp_dir / "02_sem_predictions"
                dir_inst_out = tmp_dir / "03_inst_predictions"
                dir_inst_02 = tmp_dir / "04_inst_02mm"
                dir_inst_orig = tmp_dir / "05_inst_orig"
                dir_final = tmp_dir / "06_final"

                for d in [dir_raw, dir_resized_02, dir_sem_out, dir_inst_out, dir_inst_02, dir_inst_orig, dir_final]:
                    d.mkdir(parents=True, exist_ok=True)

                case_id = "case_0001"
                raw_nii_path = dir_raw / f"{case_id}_0000.nii.gz"
                sitk.WriteImage(image, str(raw_nii_path))

                # -------------------------------------------------------------
                # Stage 1: Resample to 0.2 x 0.2 x 0.2 mm target spacing
                # -------------------------------------------------------------
                logger.info("ToothSeg [Stage 1/5]: Normalizing input scan to 0.2mm isotropic spacing...")
                resized_02_path = dir_resized_02 / f"{case_id}_0000.nii.gz"
                self._resample_scan_02mm(image, resized_02_path)

                # -------------------------------------------------------------
                # Stage 2: 33-Class Semantic Segmentation (Dataset 121)
                # -------------------------------------------------------------
                logger.info("ToothSeg [Stage 2/5]: Running Semantic Model (Dataset 121) with probability export...")
                self._run_nnunet_predictor(
                    model_dir=self._model_121_path,
                    input_folder=dir_raw,
                    output_folder=dir_sem_out,
                    save_probabilities=True
                )
                self._cleanup_gpu()

                # -------------------------------------------------------------
                # Stage 3: Border-Core Instance Segmentation (Dataset 123)
                # -------------------------------------------------------------
                logger.info("ToothSeg [Stage 3/5]: Running Instance Model (Dataset 123) on 0.2mm volume...")
                self._run_nnunet_predictor(
                    model_dir=self._model_123_path,
                    input_folder=dir_resized_02,
                    output_folder=dir_inst_out,
                    save_probabilities=False
                )
                self._cleanup_gpu()

                # -------------------------------------------------------------
                # Stage 4: Morphological Border-Core to Instances
                # -------------------------------------------------------------
                logger.info("ToothSeg [Stage 4/5]: Converting border-core maps to connected instances...")
                inst_raw_nii = dir_inst_out / f"{case_id}.nii.gz"
                inst_02_nii = dir_inst_02 / f"{case_id}.nii.gz"
                self._border_core_to_instances(inst_raw_nii, inst_02_nii)

                # -------------------------------------------------------------
                # Stage 5: Coordinate Resampling & FDI Dynamic Programming
                # -------------------------------------------------------------
                logger.info("ToothSeg [Stage 5/5]: Resampling instances & solving FDI dynamic programming graph...")
                inst_orig_nii = dir_inst_orig / f"{case_id}.nii.gz"
                self._resample_seg_to_ref(inst_02_nii, raw_nii_path, inst_orig_nii)

                final_nii_path = dir_final / f"{case_id}.nii.gz"
                sem_npz_path = dir_sem_out / f"{case_id}.npz"
                labeled_arr = self._solve_fdi_labels(inst_orig_nii, sem_npz_path, final_nii_path)

            elapsed = round(time.time() - start_time, 2)
            unique_teeth = [int(x) for x in np.unique(labeled_arr) if x > 0]
            logger.info(f"ToothSeg: Segmentation successfully completed in {elapsed}s. Detected teeth count: {len(unique_teeth)}")

            return ToothSegSegmentationResult(
                labeled_volume=labeled_arr,
                spacing=spacing,
                origin=origin,
                direction=direction,
                orientation="RAS",
                metadata={
                    "engine": "ToothSeg_InProcess_nnUNetv2",
                    "device": str(self._device),
                    "executionTimeSeconds": elapsed,
                    "detectedTeethCount": len(unique_teeth),
                    "detectedFdiLabels": unique_teeth,
                    "dataset121Trainer": "nnUNetTrainer_onlyMirror01_DASegOrd0",
                    "dataset123Trainer": "nnUNetTrainer",
                }
            )

    def _resample_scan_02mm(self, image: sitk.Image, output_path: Path) -> None:
        """Resamples the input scan to 0.2mm target spacing using Torch / SimpleITK."""
        arr = sitk.GetArrayFromImage(image).astype(np.float32)
        source_spacing = list(image.GetSpacing())
        target_spacing = (0.2, 0.2, 0.2)
        target_shape = compute_new_shape(arr.shape, source_spacing[::-1], target_spacing)

        try:
            arr_resampled = resample_torch_simple(
                torch.from_numpy(arr)[None],
                target_shape,
                is_seg=False,
                num_threads=2,
                device=self._device
            )[0].numpy()
        except Exception:
            arr_resampled = resample_torch_simple(
                torch.from_numpy(arr)[None],
                target_shape,
                is_seg=False,
                num_threads=2,
                device=torch.device("cpu")
            )[0].numpy()

        target_itk = sitk.GetImageFromArray(arr_resampled)
        target_itk.SetSpacing(tuple(list(target_spacing)[::-1]))
        target_itk.SetOrigin(image.GetOrigin())
        target_itk.SetDirection(image.GetDirection())
        sitk.WriteImage(target_itk, str(output_path))

    def _run_nnunet_predictor(
        self,
        model_dir: Path,
        input_folder: Path,
        output_folder: Path,
        save_probabilities: bool
    ) -> None:
        """Initializes and runs the in-process nnUNetPredictor with memory optimization and automatic device management."""
        self._cleanup_gpu()
        predictor = nnUNetPredictor(
            tile_step_size=0.75,
            use_gaussian=True,
            use_mirroring=False, # Disabled TTA mirroring to reduce GPU VRAM peak overhead by ~60% and prevent CUDA OOM on RTX 2050
            perform_everything_on_device=False,
            device=self._device,
            verbose=False,
            verbose_preprocessing=False,
            allow_tqdm=False
        )
        logger.info(f"[PROVE_BOUNDARY] BEFORE calling nnUNet predictor initialization for model folder: {model_dir.name}...")
        predictor.initialize_from_trained_model_folder(
            str(model_dir),
            use_folds=(5,),
            checkpoint_name="checkpoint_final.pth"
        )
        logger.info(f"[PROVE_BOUNDARY] BEFORE calling nnUNet predict_from_files (Input: {input_folder.name} -> Output: {output_folder.name})...")
        t_start_nnunet = time.time()
        predictor.predict_from_files(
            str(input_folder),
            str(output_folder),
            save_probabilities=save_probabilities,
            overwrite=True,
            num_processes_preprocessing=1,
            num_processes_segmentation_export=1,
            folder_with_segs_from_prev_stage=None,
            num_parts=1,
            part_id=0
        )
        t_end_nnunet = round(time.time() - t_start_nnunet, 2)
        logger.info(f"[PROVE_BOUNDARY] AFTER nnUNet predict_from_files returned cleanly (Duration: {t_end_nnunet}s)!")
        del predictor
        self._cleanup_gpu()

    def _border_core_to_instances(
        self,
        input_path: Path,
        output_path: Path,
        small_center_threshold: float = 16.0,
        isolated_border_threshold: float = 0.0,
        min_instance_size: float = 16.0
    ) -> None:
        """Morphological transformation converting border-core predictions to individual tooth components."""
        itk_img = sitk.ReadImage(str(input_path))
        npy_img = sitk.GetArrayFromImage(itk_img)
        spacing = np.array(itk_img.GetSpacing())[::-1]

        instance_seg = convert_semantic_to_instanceseg(
            npy_img, spacing, small_center_threshold, isolated_border_threshold
        )

        if min_instance_size > 0:
            vol_per_voxel = np.prod(spacing)
            n_pixel_cutoff = min_instance_size / vol_per_voxel
            instances = [i for i in pd.unique(instance_seg.ravel()) if i != 0]
            for i in instances:
                mask = instance_seg == i
                if np.sum(mask) < n_pixel_cutoff:
                    instance_seg[mask] = 0

        instance_seg = postprocess_instance_segmentation(instance_seg)
        itk_res = sitk.GetImageFromArray(instance_seg)
        itk_res.SetSpacing(itk_img.GetSpacing())
        itk_res.SetOrigin(itk_img.GetOrigin())
        itk_res.SetDirection(itk_img.GetDirection())
        sitk.WriteImage(itk_res, str(output_path))

    def _resample_seg_to_ref(self, seg_path: Path, ref_path: Path, out_path: Path) -> None:
        """Resamples an instance segmentation map back to the native reference coordinate grid."""
        seg_itk = sitk.ReadImage(str(seg_path))
        ref_itk = sitk.ReadImage(str(ref_path))

        seg_arr = sitk.GetArrayFromImage(seg_itk).astype(np.uint8)
        target_shape = sitk.GetArrayFromImage(ref_itk).shape

        try:
            seg_resampled = resample_torch_simple(
                torch.from_numpy(seg_arr)[None],
                target_shape,
                is_seg=True,
                num_threads=1,
                device=self._device
            )[0].numpy()
        except Exception:
            seg_resampled = resample_torch_simple(
                torch.from_numpy(seg_arr)[None],
                target_shape,
                is_seg=True,
                num_threads=2,
                device=torch.device("cpu")
            )[0].numpy()

        res_itk = sitk.GetImageFromArray(seg_resampled)
        res_itk.SetSpacing(ref_itk.GetSpacing())
        res_itk.SetOrigin(ref_itk.GetOrigin())
        res_itk.SetDirection(ref_itk.GetDirection())
        sitk.WriteImage(res_itk, str(out_path))

    def _solve_fdi_labels(self, inst_file: Path, sem_npz_file: Path, output_file: Path) -> np.ndarray:
        """Solves optimal tooth FDI numbering (1..32) using dynamic programming over anatomical priors."""
        normals = self._load_fdi_priors()

        inst_nii = nibabel.load(str(inst_file))
        inst_seg = np.asarray(inst_nii.dataobj)
        orientation = nibabel.io_orientation(inst_nii.affine)
        spacing = np.array(inst_nii.header.get_zooms())

        inst_seg_oriented = nibabel.apply_orientation(inst_seg, orientation)
        instances, inverse = np.unique(inst_seg_oriented, return_inverse=True)
        inst_seg_oriented = inverse.reshape(inst_seg_oriented.shape).astype(inst_seg_oriented.dtype)

        sem_data = np.load(str(sem_npz_file))["probabilities"]
        sem_data[sem_data == 0] = 1e-6
        sem_seg_oriented = nibabel.apply_orientation(
            sem_data.transpose(0, 3, 2, 1),
            np.concatenate((
                np.array([[0, 1]]),
                np.column_stack((orientation[:, 0] + 1, orientation[:, 1])),
            ))
        )

        inst_centroids = np.zeros((0, 3))
        inst_probs = np.zeros((0, 33))
        out_seg = np.zeros_like(inst_seg_oriented)

        for inst_idx in range(1, instances.shape[0]):
            inst_mask = inst_seg_oriented == inst_idx
            voxel_probs = sem_seg_oriented[:, inst_mask]
            class_idxs = voxel_probs.argmax(0)
            scores = np.zeros(33)

            for class_idx in np.nonzero(voxel_probs.mean(1) >= 0.1)[0]:
                if not np.any(class_idxs == class_idx):
                    continue
                scores[class_idx] = voxel_probs[class_idx, class_idxs == class_idx].mean()

            if (scores[1:] >= 0.95).sum() <= 1:
                split_idxs = np.zeros(inst_mask.sum(), dtype=int)
            else:
                class_idxs = np.nonzero(scores[1:] >= 0.95)[0] + 1
                split_idxs = sem_seg_oriented[class_idxs][:, inst_mask].argmax(0)

            voxel_idxs = np.column_stack(np.nonzero(inst_mask))
            for split_idx in np.unique(split_idxs):
                inst_centroid = voxel_idxs[split_idxs == split_idx].mean(0) * spacing
                inst_centroids = np.concatenate((inst_centroids, [inst_centroid]))
                prob_dist = voxel_probs[:, split_idxs == split_idx].mean(1)
                inst_probs = np.concatenate((inst_probs, [prob_dist]))
                out_seg[tuple(voxel_idxs[split_idxs == split_idx].T)] = out_seg.max() + 1

        out_seg = nibabel.apply_orientation(
            out_seg, nibabel.io_orientation(np.linalg.inv(inst_nii.affine))
        )

        is_background = inst_probs[:, 0] >= 0.95 if inst_probs.shape[0] > 0 else np.array([], dtype=bool)
        inst_centroids = inst_centroids[~is_background]
        inst_probs = inst_probs[~is_background]

        inst_fdis = np.zeros(inst_centroids.shape[0], dtype=int)
        if inst_centroids.shape[0] > 0:
            is_inst_lower = inst_probs[:, 17:].sum(-1) > inst_probs[:, 1:17].sum(-1)
            for is_arch_lower in [False, True]:
                if not np.any(is_arch_lower == is_inst_lower):
                    continue
                arch_idxs = np.nonzero(is_arch_lower == is_inst_lower)[0]
                arch_centroids = inst_centroids[arch_idxs]
                arch_probs = inst_probs[arch_idxs]
                arch_probs = arch_probs[:, 17:] if is_arch_lower else arch_probs[:, 1:17]
                arch_probs /= np.maximum(arch_probs.sum(axis=1, keepdims=True), 1e-6)

                idxs = np.full(arch_centroids.shape[0], -1)
                first_idx = arch_centroids[:, 1].argmin()
                idxs[0] = first_idx
                for i in range(1, arch_centroids.shape[0]):
                    dists = np.linalg.norm(arch_centroids[idxs == -1] - arch_centroids[idxs[i - 1]], axis=-1)
                    idxs[i] = np.nonzero(idxs == -1)[0][dists.argmin()]

                index = np.arange(16, 32) if is_arch_lower else np.arange(16)
                trans_log_probs = np.zeros((arch_centroids.shape[0] - 1, 16, 16))
                for i, (idx1, idx2) in enumerate(zip(idxs[:-1], idxs[1:])):
                    offsets = arch_centroids[idx2] - arch_centroids[idx1]
                    for j in range(16):
                        for k in range(16):
                            trans_log_probs[i, j, k] = normals[index[j]][index[k]].logpdf(offsets[:2])

                tooth_log_probs = np.log(np.maximum(arch_probs, 1e-6))
                q = np.zeros_like(tooth_log_probs)
                q[0] = -4.0 * tooth_log_probs[idxs[0]]
                p = np.zeros_like(q, dtype=int)
                p[0] = np.arange(16)

                for i in range(1, arch_probs.shape[0]):
                    for j in range(16):
                        costs = q[i - 1] - trans_log_probs[i - 1, :, j]
                        q[i, j] = costs.min() - 4.0 * tooth_log_probs[idxs[i], j]
                        p[i, j] = costs.argmin()

                path = q[-1].argmin(keepdims=True)
                for i in range(arch_probs.shape[0] - 1):
                    path = np.concatenate(([p[-1 - i, path[0]]], path))

                inst_fdis[arch_idxs[idxs]] = path + 16 * is_arch_lower + 1

        inst_map = np.zeros(is_background.shape[0] + 1)
        inst_map[np.nonzero(~is_background)[0] + 1] = inst_fdis

        fdi_seg = inst_map[out_seg].astype(np.uint8)
        fdi_nii = nibabel.Nifti1Image(fdi_seg, inst_nii.affine, dtype=np.uint8)
        nibabel.save(fdi_nii, str(output_file))
        return fdi_seg


# Global Singleton Instance
toothseg_service = ToothSegInferenceService()
