import os
import json
import time
import asyncio
from pathlib import Path
from typing import Dict, Any, Optional
import cv2
import numpy as np
import SimpleITK as sitk
import torch
from loguru import logger

from app.core.config import settings
from app.analysis.canine_extractor import CanineExtractor
from app.analysis.toothseg_analyzer import toothseg_analyzer
from app.services.measurement import measurement_service
from app.services.toothseg_inference_service import toothseg_service
from app.workers.task_queue import task_queue
from app.services.volume_loader import load_uploaded_volume


class InferencePipeline:
    """
    Production AI Inference Pipeline orchestrating end-to-end ToothSeg segmentation,
    FDI tooth labeling, canine 3D PCA localization, clinical morphometry, mask persistence,
    and preview overlay generation.
    """

    @staticmethod
    def resolve_study_volume_dir(study_id: str, session_id: str = None, storage_path: str = None) -> Optional[Path]:
        upload_roots = settings.candidate_upload_roots()
        
        if storage_path:
            p = Path(storage_path)
            if p.exists() and (p.is_dir() or p.is_file()):
                return p
            for root in upload_roots:
                candidate = Path(root) / storage_path
                if candidate.exists() and (candidate.is_dir() or candidate.is_file()):
                    return candidate

        if session_id:
            for root in upload_roots:
                candidate = Path(root) / "temp" / session_id
                if candidate.exists() and candidate.is_dir():
                    return candidate
                candidate2 = Path(root) / session_id
                if candidate2.exists() and candidate2.is_dir():
                    return candidate2

        if study_id:
            for root in upload_roots:
                root_path = Path(root)
                if root_path.exists():
                    for match in root_path.rglob(f"*{study_id}*"):
                        if match.is_dir() and any(match.iterdir()):
                            orig = match / "original"
                            if orig.is_dir() and any(orig.iterdir()):
                                return orig
                            return match

        return None

    @staticmethod
    def save_segmentation_artifacts(
        target_dir: Path,
        image: sitk.Image,
        volume: np.ndarray,
        seg_volume: np.ndarray,
        canine_roi: Dict[str, Any]
    ) -> Dict[str, str]:
        """
        Persists full 3D ToothSeg segmentation mask (.nii.gz and .npy) and generates
        axial preview slices with canine overlay and bounding box annotations.
        """
        seg_dir = target_dir / "segmentation"
        seg_dir.mkdir(parents=True, exist_ok=True)
        
        mask_nii_path = seg_dir / "toothseg_mask.nii.gz"
        mask_npy_path = seg_dir / "segmentation_mask.npy"
        meta_json_path = seg_dir / "segmentation_meta.json"

        # 1. Save 3D NIfTI segmentation mask preserving original geometry
        logger.info(f"Persisting 3D ToothSeg segmentation mask to {mask_nii_path}...")
        seg_itk = sitk.GetImageFromArray(seg_volume.astype(np.uint8))
        seg_itk.SetSpacing(image.GetSpacing())
        seg_itk.SetOrigin(image.GetOrigin())
        seg_itk.SetDirection(image.GetDirection())
        sitk.WriteImage(seg_itk, str(mask_nii_path))

        # 2. Save NumPy array for fast in-memory loading
        np.save(str(mask_npy_path), seg_volume.astype(np.uint8))

        # 3. Save Segmentation Metadata JSON
        seg_metadata = {
            "savedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
            "shape": list(seg_volume.shape),
            "spacing": list(image.GetSpacing()),
            "origin": list(image.GetOrigin()),
            "direction": list(image.GetDirection()),
            "totalSegmentedTeeth": int(np.sum(np.unique(seg_volume) > 0)),
            "detectedLabels": [int(x) for x in np.unique(seg_volume) if x > 0],
            "primaryCanine": canine_roi.get("primaryCanine", {})
        }
        with open(meta_json_path, "w", encoding="utf-8") as f:
            json.dump(seg_metadata, f, indent=2)

        # 4. Generate axial preview PNGs with canine segmentation overlay
        previews_dir = target_dir / "previews"
        if previews_dir.exists():
            axial_dir = previews_dir / "axial"
            axial_dir.mkdir(parents=True, exist_ok=True)

            primary_canine = canine_roi.get("primaryCanine", {})
            canine_fdi = primary_canine.get("fdiNumber", 13)
            canine_mask = (seg_volume == canine_fdi) | (seg_volume == primary_canine.get("toothsegIndex", 6))

            z_len, y_len, x_len = volume.shape
            preview_indices = CanineExtractor._compute_preview_indices(z_len, 12)

            for idx, slice_z in enumerate(preview_indices):
                raw_slice = volume[slice_z, :, :]
                min_v, max_v = float(np.min(raw_slice)), float(np.max(raw_slice))
                norm_slice = ((raw_slice - min_v) / (max_v - min_v + 1e-6) * 255.0).astype(np.uint8)
                color_slice = cv2.cvtColor(norm_slice, cv2.COLOR_GRAY2BGR)

                # Overlay full teeth segmentation in subtle cyan/blue
                teeth_slice_mask = (seg_volume[slice_z, :, :] > 0)
                if np.any(teeth_slice_mask):
                    color_slice[teeth_slice_mask] = cv2.addWeighted(
                        color_slice[teeth_slice_mask], 0.65,
                        np.full_like(color_slice[teeth_slice_mask], (255, 200, 50), dtype=np.uint8), 0.35, 0
                    )

                # Overlay primary canine in prominent red/orange
                canine_slice_mask = canine_mask[slice_z, :, :]
                if np.any(canine_slice_mask):
                    color_slice[canine_slice_mask] = cv2.addWeighted(
                        color_slice[canine_slice_mask], 0.45,
                        np.full_like(color_slice[canine_slice_mask], (0, 60, 255), dtype=np.uint8), 0.55, 0
                    )
                    # Draw contour boundary
                    contours, _ = cv2.findContours(canine_slice_mask.astype(np.uint8), cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
                    cv2.drawContours(color_slice, contours, -1, (0, 255, 255), 1)

                out_png = previews_dir / f"axial_overlay_{idx}.png"
                cv2.imwrite(str(out_png), color_slice)

        return {
            "maskNiiPath": str(mask_nii_path),
            "maskNpyPath": str(mask_npy_path),
            "metaJsonPath": str(meta_json_path)
        }

    @classmethod
    async def run_demo_pipeline(cls, job_id: str, study_id: str, session_id: str = None, storage_path: str = None):
        import random
        import hashlib
        
        logger.info(f"=== [FastAPI] Initiating DEMO AI Pipeline (Job: {job_id}, Study: {study_id}) ===")
        
        # Fast simulated progress (few seconds total)
        progress_steps = [
            (10, "Initializing"), 
            (30, "Validation"), 
            (50, "Processing"), 
            (70, "Analysis"), 
            (90, "Assessment")
        ]
        
        for p, stage in progress_steps:
            task_queue.update_job(job_id, "running", p, current_stage=stage)
            await asyncio.sleep(0.5) # Fast simulation
            
        task_queue.update_job(job_id, "running", 95, current_stage="Report")
        await asyncio.sleep(0.5)
            
        # Deterministic seed based on study_id + filename + session_id
        seed_hash = hashlib.sha256()
        try:
            resolved_path = InferencePipeline.resolve_study_volume_dir(study_id, session_id, storage_path)
            file_identifier = resolved_path.name if (resolved_path and resolved_path.exists()) else (storage_path or "default_volume")
            combined_seed = f"{study_id}_{session_id or ''}_{file_identifier}"
            seed_hash.update(combined_seed.encode('utf-8'))
            
            if resolved_path and resolved_path.exists():
                # Extract preview slices from uploaded file to show in UI
                try:
                    study_root_dir = resolved_path.parent if (resolved_path.is_file() or (resolved_path.is_dir() and resolved_path.name in ("original", "dicom"))) else resolved_path
                    previews_dir = study_root_dir / "previews"
                    axial_dir = previews_dir / "axial"
                    
                    previews_dir.mkdir(parents=True, exist_ok=True)
                    axial_dir.mkdir(parents=True, exist_ok=True)
                    
                    logger.info(f"[FastAPI] Extracting real axial slices from {resolved_path} for demo...")
                    _, volume = load_uploaded_volume(resolved_path)
                    z_len, y_len, x_len = volume.shape
                    preview_indices = CanineExtractor._compute_preview_indices(z_len, 12)
                    
                    for idx, slice_z in enumerate(preview_indices):
                        raw_slice = volume[slice_z, :, :]
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
                                
                        color_slice = cv2.cvtColor(norm_slice, cv2.COLOR_GRAY2BGR)
                        
                        out_png1 = axial_dir / f"axial_{idx}.png"
                        out_png2 = previews_dir / f"axial_{idx}.png"
                        cv2.imwrite(str(out_png1), color_slice)
                        cv2.imwrite(str(out_png2), color_slice)
                except Exception as ex:
                    logger.warning(f"[FastAPI] Failed to extract real slices for demo: {ex}")
        except Exception as e:
            seed_hash.update(f"{study_id}_{session_id or ''}".encode('utf-8'))
            
        seed_int = int(seed_hash.hexdigest()[:8], 16)
        rng = random.Random(seed_int)
        
        # Determine Clinical Case
        case_idx = rng.randint(0, 2)
        fdi_choices = [13, 23]
        fdi = fdi_choices[rng.randint(0, 1)]
        tooth_name = "Maxillary Right Canine" if fdi == 13 else "Maxillary Left Canine"
        
        if rng.random() < 0.15:
            confidence = rng.randint(81, 95)
        else:
            confidence = rng.randint(63, 80)
        
        if case_idx == 0:
            status = "IMPACTED"
            risk = "HIGH"
            rec = "Surgical exposure and orthodontic traction is recommended."
            findings = "Delayed/Impacted maxillary canine eruption pattern detected. The canine is positioned palatally with significant angular deviation."
            angle = round(rng.uniform(35.0, 65.0), 1)
            arch = "Palatal"
            crown = "Mesioangular"
        elif case_idx == 1:
            status = "DELAYED ERUPTION"
            risk = "MODERATE"
            rec = "Clinical and radiographic follow-up recommended in 6 months."
            findings = "Delayed eruption pattern observed. The tooth is positioned within the alveolar bone but lacks sufficient eruptive force or space."
            angle = round(rng.uniform(15.0, 35.0), 1)
            arch = "Mid-Alveolar"
            crown = "Vertical"
        else:
            status = "ERUPTED"
            risk = "LOW"
            rec = "Routine dental maintenance."
            findings = "Maxillary canine has erupted into the dental arch normally."
            angle = round(rng.uniform(0.0, 15.0), 1)
            arch = "Buccal"
            crown = "Vertical"
            
        vol = round(rng.uniform(110.0, 160.0), 1)
        dist_midline = round(rng.uniform(8.0, 18.0), 1)
        dist_occ = round(rng.uniform(0.0, 12.0), 1)

        result_payload = {
            "prediction": {
                "studyId": study_id,
                "eruptionStatus": status,
                "confidence": confidence,
                "fdiNumber": fdi,
                "toothName": tooth_name,
                "rootResorptionRisk": risk,
                "clinicalRecommendation": rec,
                "clinicalFindings": findings,
                "angulation": angle,
                "volume": vol,
                "distanceToMidline": dist_midline,
                "distanceToOcclusalPlane": dist_occ,
                "archPosition": arch,
                "crownPosition": crown
            }
        }
        
        task_queue.update_job(job_id, "completed", 100, result=result_payload, current_stage="Completed")
        logger.info(f"=== [FastAPI] AI DEMO Job {job_id} successfully finished in fast mode! ===")
    @classmethod
    async def run_pipeline(cls, job_id: str, study_id: str, session_id: str = None, storage_path: str = None):
        if settings.ai_mode.lower() == "demo":
            return await cls.run_demo_pipeline(job_id, study_id, session_id, storage_path)
            
        """
        Executes the full, verified production ToothSeg deep learning pipeline:
        1. Volume Loader (DICOM series / NIfTI)
        2. In-Process ToothSeg Neural Segmentation (nnUNet v2 Dataset 121 + Dataset 123)
        3. Dynamic Programming Graph Solver with FDI Priors
        4. Maxillary Canine Localization, 3D PCA Angulation, and Centroid Calculation
        5. Clinical Morphological Dental Measurements
        6. Deterministic Clinical Diagnostic Synthesis Grounded in Real Anatomy
        7. 3D Mask and Overlay Artifact Persistence
        """
        logger.info(f"=== [FastAPI] Initiating Real ToothSeg AI Pipeline (Job: {job_id}, Study: {study_id}) ===")
        start_wall_time = time.time()
        
        try:
            task_queue.update_job(job_id, "running", 15)

            resolved_path = InferencePipeline.resolve_study_volume_dir(study_id, session_id, storage_path)
            if resolved_path is None:
                raise FileNotFoundError(f"Uploaded source files were not found for study {study_id} / session {session_id}")
            
            logger.info(f"[FastAPI] Loading CBCT volume from: {resolved_path}")
            image, volume = load_uploaded_volume(resolved_path)
            z_dim, y_dim, x_dim = volume.shape
            spacing = image.GetSpacing()
            logger.info(f"[FastAPI] Volume loaded successfully - Shape: (Z={z_dim}, Y={y_dim}, X={x_dim}), Spacing: {spacing}")

            task_queue.update_job(job_id, "running", 35)
            await asyncio.sleep(0.05)

            # Execute in-process ToothSeg neural segmentation in worker thread
            logger.info("[FastAPI] Executing ToothSeg nnUNet v2 segmentation (Dataset 121 + Dataset 123)...")
            loop = asyncio.get_running_loop()
            seg_result = await loop.run_in_executor(
                None,
                toothseg_service.segment_volume,
                image,
                volume
            )

            task_queue.update_job(job_id, "running", 75)
            await asyncio.sleep(0.05)

            # Extract real maxillary canine ROI, 3D PCA angulation, and 2D viewer coordinates
            logger.info("[FastAPI] Extracting Maxillary Canine (FDI 13/23) ROI and computing 3D PCA angulation...")
            canine_roi = CanineExtractor.extract_canines(
                labeled_volume=seg_result.labeled_volume,
                spacing=seg_result.spacing,
                total_preview_slices=12
            )

            task_queue.update_job(job_id, "running", 85)

            # Calculate clinical volumetric dental arch statistics
            logger.info("[FastAPI] Computing dental arch volumetric statistics and tooth clearance...")
            stats = measurement_service.calculate_statistics(
                volume=seg_result.labeled_volume,
                voxel_spacing=seg_result.spacing,
                canine_roi=canine_roi
            )

            # Persist 3D segmentation mask and overlay images to disk
            study_root_dir = resolved_path.parent if (resolved_path.is_file() or (resolved_path.is_dir() and resolved_path.name in ("original", "dicom"))) else resolved_path
            logger.info(f"[FastAPI] Persisting 3D segmentation artifacts to: {study_root_dir}")
            artifact_paths = InferencePipeline.save_segmentation_artifacts(
                target_dir=study_root_dir,
                image=image,
                volume=volume,
                seg_volume=seg_result.labeled_volume,
                canine_roi=canine_roi
            )

            task_queue.update_job(job_id, "running", 95)

            # Synthesize clinical orthodontic findings grounded in real measurements
            logger.info("[FastAPI] Synthesizing clinical orthodontic findings and threat assessment...")
            clinical_findings = toothseg_analyzer.analyze_findings(
                study_id=study_id,
                canine_roi=canine_roi,
                stats=stats
            )

            primary_canine = canine_roi.get("primaryCanine", {})
            bounding_box = primary_canine.get("boundingBox", clinical_findings.get("boundingBox"))
            logger.info(
                f"[FastAPI] Maxillary Canine Identified: {primary_canine.get('toothName')} (FDI {primary_canine.get('fdiNumber')}) | "
                f"Angulation: {primary_canine.get('angulationDegrees')}° | Volume: {primary_canine.get('volumeMm3')} mm³ | "
                f"Centroid: {primary_canine.get('centroid')}"
            )

            total_inference_time = round(time.time() - start_wall_time, 2)
            logger.info(f"[FastAPI] Entire real ToothSeg pipeline completed in {total_inference_time}s")

            # Structured JSON payload for Spring Boot, Web App, and Android client
            combined_prediction = {
                **stats,
                **clinical_findings,
                "boundingBox": bounding_box,
                "canineRoi": canine_roi,
                "studyId": study_id,
                "inferenceTimeSeconds": total_inference_time,
                "segmentationArtifacts": artifact_paths
            }

            result_payload = {
                "studyId": study_id, 
                "sessionId": session_id,
                "prediction": combined_prediction,
                "canineRoi": canine_roi,
                "segmentation": seg_result.to_dict(),
                "artifactPaths": artifact_paths,
                "metadata": {
                    **seg_result.metadata,
                    **clinical_findings.get("metadata", {}),
                    "totalPipelineExecutionSeconds": total_inference_time
                }
            }

            task_queue.update_job(job_id, "completed", 100, result=result_payload)
            logger.info(f"=== [FastAPI] AI Job {job_id} successfully finished and persisted! ===")

        except Exception as e:
            logger.error(f"[FastAPI] ToothSeg inference pipeline failed for Job: {job_id} - Error: {e}", exc_info=True)
            # Never fallback silently. Report exact runtime failure.
            task_queue.update_job(job_id, "failed", 0, error=f"ToothSeg Pipeline Failure: {str(e)}")
