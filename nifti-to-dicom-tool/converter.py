#!/usr/bin/env python3
"""
CanineAI NIfTI-to-DICOM Conversion Utility
"""

import os
import sys
import time
import logging
import traceback
import csv
import yaml
import argparse
from pathlib import Path
from datetime import datetime
import numpy as np
import SimpleITK as sitk
import pydicom
from pydicom.dataset import FileDataset, FileMetaDataset
from pydicom.uid import generate_uid, ExplicitVRLittleEndian
from concurrent.futures import ProcessPoolExecutor, as_completed
from tqdm import tqdm

# Configure Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('conversion.log', mode='w')
    ]
)
logger = logging.getLogger("NiftiToDicom")


def verify_case_dicom_series(case_dir, expected_num_slices, expected_sop_uids):
    """
    Verify DICOM series integrity: slice count, UID uniqueness, instance continuity, 
    and general file readability with pydicom.
    """
    dicom_files = sorted(list(case_dir.glob("*.dcm")), key=lambda x: x.name)
    if len(dicom_files) != expected_num_slices:
        raise ValueError(f"Slice count mismatch: expected {expected_num_slices}, found {len(dicom_files)}")

    read_sop_uids = set()
    instance_numbers = set()

    for idx, f in enumerate(dicom_files):
        try:
            ds = pydicom.dcmread(str(f))
            # Accessing properties forces pydicom to parse elements
            _ = ds.PixelData
            _ = ds.Rows
            _ = ds.Columns
            sop_uid = ds.SOPInstanceUID
            instance_num = ds.InstanceNumber

            if not sop_uid:
                raise ValueError("SOPInstanceUID is empty")
            if sop_uid in read_sop_uids:
                raise ValueError(f"Duplicate SOPInstanceUID detected: {sop_uid}")
            read_sop_uids.add(sop_uid)

            if instance_num is None:
                raise ValueError("InstanceNumber is missing")
            instance_numbers.add(instance_num)

            # Check display/visual correction tags
            if ds.WindowCenter is None or ds.WindowWidth is None:
                raise ValueError("WindowCenter or WindowWidth is missing")
            if ds.FrameOfReferenceUID is None:
                raise ValueError("FrameOfReferenceUID is missing")
            if ds.SliceLocation is None:
                raise ValueError("SliceLocation is missing")
            if ds.RescaleType != "HU":
                raise ValueError(f"Expected RescaleType 'HU', got {ds.RescaleType}")

        except Exception as e:
            raise ValueError(f"File {f.name} validation failed: {str(e)}")

    # Check for missing slice instances
    expected_instances = set(range(1, expected_num_slices + 1))
    missing_instances = expected_instances - instance_numbers
    if missing_instances:
        raise ValueError(f"Missing slice instances: {sorted(list(missing_instances))}")


def convert_case(file_path, output_dir, config, overwrite, verify):
    """
    Convert a single 3D NIfTI CBCT volume into a DICOM series case folder.
    Returns a dictionary of execution metadata.
    """
    start_time = time.time()
    file_path = Path(file_path)
    case_id = file_path.name.replace(".nii.gz", "").replace(".nii", "")
    case_output_dir = Path(output_dir) / case_id

    report = {
        'Input file': str(file_path),
        'Output folder': str(case_output_dir),
        'Success/Failure': 'Failure',
        'Reason': '',
        'Time taken': 0.0
    }

    try:
        # Load NIfTI Image via SimpleITK
        reader = sitk.ImageFileReader()
        reader.SetFileName(str(file_path))
        image = reader.Execute()

        # Get array: returns (Z, Y, X) i.e. (slices, rows, cols)
        data = sitk.GetArrayFromImage(image)
        num_slices = data.shape[0]

        # Resume logic: skip already converted cases if overwrite is false
        if not overwrite and case_output_dir.exists():
            existing_dcms = list(case_output_dir.glob("*.dcm"))
            if len(existing_dcms) == num_slices:
                try:
                    # Test readability of a subset to verify integrity
                    pydicom.dcmread(str(existing_dcms[0]))
                    pydicom.dcmread(str(existing_dcms[-1]))
                    report['Success/Failure'] = 'Success'
                    report['Reason'] = 'Skipped: already converted'
                    report['Time taken'] = round(time.time() - start_time, 2)
                    return report
                except Exception:
                    pass

        case_output_dir.mkdir(parents=True, exist_ok=True)

        spacing = image.GetSpacing()  # (dx, dy, dz)
        direction = image.GetDirection()  # 9-element tuple of direction cosines

        if len(data.shape) != 3:
            raise ValueError(f"Expected 3D volume array shape, got shape: {data.shape}")

        # Compute volume-wide visual window parameters based on robust percentiles
        p1 = np.percentile(data, 1)
        p99 = np.percentile(data, 99)
        window_center = (p99 + p1) / 2.0
        window_width = max(1e-5, p99 - p1)

        # Detect integer vs floating point types, and map floats to signed 16-bit range safely
        v_min = np.min(data)
        v_max = np.max(data)
        is_integer_type = np.issubdtype(data.dtype, np.integer) or np.all(data == data.astype(np.int32))

        if is_integer_type and v_min >= -32768 and v_max <= 32767:
            rescale_slope = 1.0
            rescale_intercept = 0.0
            processed_data = np.nan_to_num(data)
            processed_data = np.clip(processed_data, -32768, 32767).astype(np.int16)
        else:
            if v_max == v_min:
                rescale_slope = 1.0
                rescale_intercept = float(v_min)
                processed_data = np.zeros_like(data, dtype=np.int16)
            else:
                rescale_slope = float(v_max - v_min) / 65535.0
                rescale_intercept = float(v_min) + 32768.0 * rescale_slope
                
                slope_divisor = rescale_slope if rescale_slope != 0 else 1.0
                scaled = (data - rescale_intercept) / slope_divisor
                processed_data = np.round(np.nan_to_num(scaled))
                processed_data = np.clip(processed_data, -32768, 32767).astype(np.int16)

        study_instance_uid = generate_uid()
        series_instance_uid = generate_uid()
        frame_of_reference_uid = generate_uid()

        now = datetime.now()
        date_str = now.strftime("%Y%m%d")
        time_str = now.strftime("%H%M%S")

        patient_cfg = config.get('patient_metadata', {})
        patient_name = patient_cfg.get('patient_name', 'Anonymized Patient')
        patient_id = patient_cfg.get('patient_id', 'ANON-STS-3D')
        institution_name = patient_cfg.get('institution_name', 'Metro Dental Diagnostics')
        study_description = patient_cfg.get('study_description', 'Orthodontic CBCT Imaging Study')
        referring_physician = patient_cfg.get('referring_physician_name', 'Dr. Darshan Shah')
        modality = patient_cfg.get('modality', 'CT')
        patient_position = patient_cfg.get('patient_position', 'HFS')

        sop_uids = []

        # Iterate and write slices
        for i in range(num_slices):
            # Get slice voxel grid coordinates
            physical_pos = image.TransformIndexToPhysicalPoint((0, 0, i))

            slice_arr = processed_data[i, :, :]
            filename = case_output_dir / f"IMG{i+1:04d}.dcm"

            file_meta = FileMetaDataset()
            file_meta.TransferSyntaxUID = ExplicitVRLittleEndian
            file_meta.MediaStorageSOPClassUID = '1.2.840.10008.5.1.4.1.1.2'  # CT Image SOP class
            sop_instance_uid = generate_uid()
            file_meta.MediaStorageSOPInstanceUID = sop_instance_uid
            file_meta.ImplementationClassUID = '1.2.826.0.1.3680043.8.498.1'

            ds = FileDataset(str(filename), {}, file_meta=file_meta, preamble=b"\0" * 128)

            # Metadata settings
            ds.PatientName = patient_name
            ds.PatientID = patient_id
            ds.PatientBirthDate = "19900101"
            ds.PatientSex = "O"

            ds.StudyInstanceUID = study_instance_uid
            ds.SeriesInstanceUID = series_instance_uid
            ds.FrameOfReferenceUID = frame_of_reference_uid
            ds.PositionReferenceIndicator = ""
            ds.SOPClassUID = '1.2.840.10008.5.1.4.1.1.2'
            ds.SOPInstanceUID = sop_instance_uid
            sop_uids.append(sop_instance_uid)

            ds.StudyDate = date_str
            ds.SeriesDate = date_str
            ds.AcquisitionDate = date_str
            ds.ContentDate = date_str

            ds.StudyTime = time_str
            ds.SeriesTime = time_str
            ds.AcquisitionTime = time_str
            ds.ContentTime = time_str

            ds.Modality = modality
            ds.Manufacturer = "CanineAI Diagnostics"
            ds.InstitutionName = institution_name
            ds.StudyDescription = study_description
            ds.ReferringPhysicianName = referring_physician
            ds.PatientPosition = patient_position

            ds.StudyID = "STUDY-1"
            ds.SeriesNumber = 1
            ds.InstanceNumber = i + 1

            ds.SamplesPerPixel = 1
            ds.PhotometricInterpretation = "MONOCHROME2"
            ds.Rows = slice_arr.shape[0]
            ds.Columns = slice_arr.shape[1]

            # Spacing details: [dy, dx]
            ds.PixelSpacing = [float(spacing[1]), float(spacing[0])]
            ds.SliceThickness = float(spacing[2])
            ds.SpacingBetweenSlices = float(spacing[2])
            ds.SliceLocation = float(physical_pos[2])

            # Patient coordinates positioning
            ds.ImagePositionPatient = [float(physical_pos[0]), float(physical_pos[1]), float(physical_pos[2])]
            
            # Row and column direction cosines matrix mapping
            ds.ImageOrientationPatient = [
                float(direction[0]), float(direction[3]), float(direction[6]),
                float(direction[1]), float(direction[4]), float(direction[7])
            ]

            ds.BitsAllocated = 16
            ds.BitsStored = 16
            ds.HighBit = 15
            ds.PixelRepresentation = 1  # Signed representation
            
            # Windowing and Rescaling Attributes
            ds.WindowCenter = float(window_center)
            ds.WindowWidth = float(window_width)
            ds.RescaleIntercept = float(rescale_intercept)
            ds.RescaleSlope = float(rescale_slope)
            ds.RescaleType = "HU"

            ds.PixelData = slice_arr.tobytes()

            ds.save_as(str(filename))

        # Verification validation step
        if verify:
            verify_case_dicom_series(case_output_dir, num_slices, sop_uids)

        report['Success/Failure'] = 'Success'
        report['Reason'] = 'Completed successfully'

    except Exception as e:
        stack_trace = traceback.format_exc()
        logger.error(f"Error during conversion of case {case_id}:\n{stack_trace}")
        report['Success/Failure'] = 'Failure'
        report['Reason'] = f"{type(e).__name__}: {str(e)}"

    report['Time taken'] = round(time.time() - start_time, 2)
    return report


def scan_for_nifti_files(input_dir):
    """Scan input path recursively for .nii and .nii.gz volume files."""
    input_path = Path(input_dir)
    if not input_path.exists():
        logger.error(f"Input directory does not exist: {input_path}")
        return []
    
    found_files = []
    supported_extensions = ['.nii', '.nii.gz']
    for root, _, files in os.walk(input_path):
        for file in files:
            file_path = Path(root) / file
            if any(file.endswith(ext) for ext in supported_extensions):
                found_files.append(file_path)
    return sorted(found_files)


def main():
    parser = argparse.ArgumentParser(description="CanineAI NIfTI-to-DICOM Converter")
    parser.add_argument('--config', type=str, default='config.yaml', help="Path to config.yaml")
    parser.add_argument('--input-dir', type=str, help="Override input NIfTI directory")
    parser.add_argument('--output-dir', type=str, help="Override DICOM output directory")
    parser.add_argument('--workers', type=int, help="Override number of concurrent worker processes")
    parser.add_argument('--overwrite', action='store_true', help="Force overwrite existing conversions")
    parser.add_argument('--no-verify', action='store_true', help="Disable validation check step")
    args = parser.parse_args()

    config_path = Path(args.config)
    config = {}
    if config_path.exists():
        try:
            with open(config_path, 'r') as f:
                config = yaml.safe_load(f) or {}
        except Exception as e:
            logger.error(f"Error reading configuration file: {e}")

    # Resolve arguments
    input_dir = args.input_dir or config.get('input_dir', './dataset')
    output_dir = args.output_dir or config.get('output_dir', './Output')
    workers = args.workers or config.get('workers', 4)
    overwrite = args.overwrite or config.get('overwrite', False)
    verify = not args.no_verify if args.no_verify else config.get('verify', True)

    logger.info(f"NIfTI Input Path: {input_dir}")
    logger.info(f"DICOM Output Path: {output_dir}")
    logger.info(f"Concurrency Workers: {workers}")
    logger.info(f"Overwrite Existing: {overwrite}")
    logger.info(f"Enable Validation: {verify}")

    # Scan files
    files = scan_for_nifti_files(input_dir)
    if not files:
        logger.warning(f"No NIfTI files found in dataset path. Exiting.")
        return

    logger.info(f"Found {len(files)} NIfTI volume cases to convert.")

    reports = []
    
    # Run conversion
    if workers > 1 and len(files) > 1:
        logger.info(f"Running multi-threaded conversion using ProcessPoolExecutor ({workers} workers).")
        with ProcessPoolExecutor(max_workers=workers) as executor:
            future_to_file = {
                executor.submit(convert_case, f, output_dir, config, overwrite, verify): f 
                for f in files
            }
            with tqdm(total=len(files), desc="Converting NIfTI to DICOM") as pbar:
                for future in as_completed(future_to_file):
                    rep = future.result()
                    reports.append(rep)
                    pbar.update(1)
    else:
        logger.info("Running sequential single-threaded conversion.")
        for f in tqdm(files, desc="Converting NIfTI to DICOM"):
            rep = convert_case(f, output_dir, config, overwrite, verify)
            reports.append(rep)

    # Save CSV report using built-in csv module
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    report_csv = output_path / "conversion_report.csv"

    csv_fields = ['Input file', 'Output folder', 'Success/Failure', 'Reason', 'Time taken']
    try:
        with open(report_csv, 'w', newline='', encoding='utf-8') as csv_file:
            writer = csv.DictWriter(csv_file, fieldnames=csv_fields)
            writer.writeheader()
            for r in reports:
                writer.writerow({k: r.get(k, '') for k in csv_fields})
        logger.info(f"Conversion complete. Report successfully saved to: {report_csv}")
    except Exception as e:
        logger.error(f"Failed to write conversion_report.csv: {e}")


if __name__ == '__main__':
    main()
