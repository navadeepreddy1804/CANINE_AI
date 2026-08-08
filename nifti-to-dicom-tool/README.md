# NIfTI-to-DICOM Dataset Conversion Utility

A production-grade Python utility designed to convert 3D NIfTI CBCT datasets (.nii/.nii.gz) into valid, slice-indexed DICOM series. The generated output conforms to standard medical DICOM specifications, allowing clinicians to load, review, and label datasets directly in clinical viewers.

---

## Supported Viewers
The generated DICOM series have been verified for compatibility and open successfully in:
- **RadiAnt DICOM Viewer**
- **MicroDicom**
- **3D Slicer**
- **OnDemand3D**
- **Romexis**

---

## Features
- **3D Volume Slicing**: Converts 3D NIfTI volumes into corresponding DICOM series (one Case folder per input volume, containing one `.dcm` file per slice).
- **Physical Parameter Preservation**: Extracts and preserves voxel spacing (mapped to Pixel Spacing and Slice Thickness), origin coordinate mapping, image dimensions, slice orientation direction cosines, and intensity values.
- **Dynamic UID Generation**: Generates cryptographically unique Study, Series, and SOP Instance UIDs to ensure PACS compatibility and prevent namespace conflicts.
- **Configurable Metadata**: Includes placeholders for anonymized patient data fields (`PatientName`, `PatientID`, `InstitutionName`, `StudyDescription`, `ReferringPhysicianName`).
- **Resumable Conversion**: Skips previously completed cases unless `overwrite` is set to `true`.
- **Integrity Validation**: Automatically verifies slice count, checks UID uniqueness, identifies missing slices, detects corrupted files, and tests readability using `pydicom`. Writes outcomes to `conversion_report.csv`.
- **Concurrency**: Parallelizes volume conversion using multi-process pool executors (`concurrent.futures.ProcessPoolExecutor`) with real-time `tqdm` progress tracking.

---

## Dependencies
This utility conforms strictly to the allowed libraries list (no `nibabel`, `pandas`, or `dotenv`):
- `SimpleITK`
- `pydicom`
- `numpy`
- `tqdm`
- `pyyaml`
- `concurrent.futures` (Standard Library)

---

## Setup & Configuration

1. **Install required packages**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure Settings**:
   Edit `config.yaml` to specify paths and execution parameters:
   ```yaml
   input_dir: "C:\\Users\\darsi\\Downloads\\CANINE_AI\\samples"
   output_dir: "C:\\Users\\darsi\\Downloads\\CANINE_AI\\nifti-to-dicom-tool\\Output"
   workers: 8
   overwrite: false
   verify: true
   ```

---

## CLI Execution
Run the conversion tool from the command line:
```bash
python converter.py [options]
```

### Options:
* `--config`: Path to custom yaml configuration file (defaults to `config.yaml`).
* `--input-dir`: Override NIfTI input directory path.
* `--output-dir`: Override DICOM output directory path.
* `--workers`: Override process pool size.
* `--overwrite`: Force overwrite previously completed case outputs.
* `--no-verify`: Disable self-validation checking.

---

## Validation & Logging
* **Reports**: Results are saved to `conversion_report.csv` in the output folder.
* **Logs**: Execution milestones and full exception stack traces are saved to `conversion.log`.

---

## Running Unit Tests
Execute the test suite to verify code correctness and SimpleITK array calculations:
```bash
python -m unittest test_converter.py
```
