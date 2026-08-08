# CanineAI Dataset Preparation & Doctor Review Package Generator

A production-grade standalone utility tool that prepares and formats CBCT datasets for remote orthodontist review and clinical labeling.

The remote doctor does not need to install any medical imaging software or read complex `.nii` / `.nii.gz` file paths. The tool processes the datasets, extracts representative slices, applies adaptive contrast equalization, and generates high-resolution review sheets.

---

## Features
- **NIfTI Processing**: Load NIfTI and compressed NIfTI volumes, normalize intensity values, correct orientations, and crop empty margins.
- **Slice Extraction Grid**: Slice 5 consecutive Axial, Coronal, and Sagittal frames centered around the volume, plus the Maximum Intensity Projection (MIP).
- **Quality Control**: Automatically flags corrupted files, incomplete slices, duplicate scans (via MD5 file hash checks), and low-contrast scans.
- **Excel & CSV Templates**: Compiles metadata details and templates pre-filled clinical annotation grids for diagnostic labeling.
- **ReportLab PDF Compilation**: Automates compile-time generation of `README.pdf` and `Instructions.pdf` guidelines for remote orthodontists.
- **Performance Optimized**: Built-in multiprocessing and task interruption resuming capability.

---

## Installation & Setup

1. **Install requirements**:
   ```bash
   pip install -r requirements.txt
   ```

2. **Configure options**:
   Update `config.yaml` or create a `.env` file to specify the target directories:
   ```yaml
   dataset_path: "D:\\DentalDatasets\\STS-3D-Tooth"
   output_path: "C:\\Users\\darsi\\Downloads\\CANINE_AI\\dataset-prep-tool\\Doctor_Review_Package"
   ```

---

## Usage

Run the utility script to generate the complete package:
```bash
python generator.py
```

### CLI Arguments overrides:
- `--dataset-path`: Override path to scan dataset.
- `--output-path`: Override output directory.
- `--workers`: Override multiprocessing workers count.

Example:
```bash
python generator.py --dataset-path D:\MyData --workers 6
```

---

## Output Package Structure
The generated `Doctor_Review_Package/` contains:
```
Doctor_Review_Package/
├── Preview/                  # High-res PNG worksheets grid per volume
│   ├── CASE-001_review.png
│   └── CASE-002_review.png
├── metadata.csv              # CBCT resolution dimensions, bit depths, spacing...
├── quality_report.csv        # Flagged anomalies (corrupted, low quality, duplicates)
├── labels.csv                # Ingestible CSV annotations sheet template
├── labels.xlsx               # Editable Excel annotations sheet template for the clinician
├── README.pdf                # Medical review package introduction guide
└── Instructions.pdf          # Standardization labeling guidelines reference
```

---

## Running Unit Tests

Execute the unit tests to verify the processing engine parameters:
```bash
python -m unittest test_generator.py
```
