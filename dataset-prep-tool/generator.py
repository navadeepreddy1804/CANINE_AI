#!/usr/bin/env python3
"""
CanineAI Dataset Preparation & Doctor Review Package Generator
"""

import os
import sys
import yaml
import dotenv
import argparse
import logging
import hashlib
import time
import multiprocessing
from datetime import datetime
from pathlib import Path

# Scientific and Image Processing Libraries
import numpy as np
import nibabel as nib
import cv2
import pandas as pd
import matplotlib.pyplot as plt
from tqdm import tqdm

# ReportLab libraries for PDF compilation
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle

# Configure Logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.StreamHandler(sys.stdout),
        logging.FileHandler('prep_tool.log', mode='w')
    ]
)
logger = logging.getLogger("DatasetPrepTool")

class DatasetPrepTool:
    def __init__(self, config_path=None, env_path=None, cmd_args=None):
        # 1. Load Configurations
        self.config = self._load_default_config()
        if config_path and os.path.exists(config_path):
            with open(config_path, 'r') as f:
                yaml_cfg = yaml.safe_load(f)
                if yaml_cfg:
                    self.config.update(yaml_cfg)

        # 2. Load Env Overrides
        if env_path and os.path.exists(env_path):
            dotenv.load_dotenv(env_path)
        
        self._apply_env_overrides()

        # 3. Apply CLI Overrides
        if cmd_args:
            if cmd_args.dataset_path:
                self.config['dataset_path'] = cmd_args.dataset_path
            if cmd_args.output_path:
                self.config['output_path'] = cmd_args.output_path
            if cmd_args.workers:
                self.config['performance']['num_workers'] = cmd_args.workers

        # Resolve paths
        self.dataset_path = Path(self.config['dataset_path'])
        self.output_path = Path(self.config['output_path'])
        self.preview_path = self.output_path / "Preview"

        # Create output directories
        self.preview_path.mkdir(parents=True, exist_ok=True)

        # File Registry states
        self.processed_hashes = {}

    def _load_default_config(self):
        return {
            'dataset_path': './dataset',
            'output_path': './Doctor_Review_Package',
            'image_processing': {
                'normalize_intensity': True,
                'correct_orientation': True,
                'crop_borders': True,
                'crop_threshold': 10,
                'clahe': {
                    'enabled': True,
                    'clip_limit': 2.0,
                    'tile_grid_size': [8, 8]
                }
            },
            'quality_control': {
                'min_slice_count': 50,
                'min_contrast_std': 15.0,
                'duplicate_detection': True
            },
            'performance': {
                'use_multiprocessing': True,
                'num_workers': 4,
                'resume_interrupted': True,
                'overwrite_existing': False
            }
        }

    def _apply_env_overrides(self):
        dataset_override = os.getenv("CANINEAI_DATASET_PATH")
        if dataset_override:
            self.config['dataset_path'] = dataset_override
        
        output_override = os.getenv("CANINEAI_OUTPUT_PATH")
        if output_override:
            self.config['output_path'] = output_override

        workers_override = os.getenv("CANINEAI_NUM_WORKERS")
        if workers_override:
            try:
                self.config['performance']['num_workers'] = int(workers_override)
            except ValueError:
                pass

    def scan_dataset(self):
        """Recursively scan for NIfTI volumes."""
        logger.info(f"Scanning dataset path recursively: {self.dataset_path}")
        if not self.dataset_path.exists():
            logger.error(f"Configured dataset path does not exist: {self.dataset_path}")
            return []

        supported_exts = ['.nii', '.nii.gz']
        found_files = []
        for root, _, files in os.walk(self.dataset_path):
            for file in files:
                file_path = Path(root) / file
                if any(file.endswith(ext) for ext in supported_exts):
                    found_files.append(file_path)
        
        logger.info(f"Found {len(found_files)} potential NIfTI volumes.")
        return sorted(found_files)

    def calculate_file_hash(self, file_path):
        """Calculate MD5 hash of the file header/size to detect duplicates quickly."""
        h = hashlib.md5()
        # Read first 1MB of file for hashing
        with open(file_path, 'rb') as f:
            chunk = f.read(1024 * 1024)
            h.update(chunk)
        return h.hexdigest()

    def process_volume(self, file_path):
        """Load, validate, orient, crop, normalize, and slice a NIfTI volume."""
        case_id = file_path.name.replace(".nii.gz", "").replace(".nii", "")
        dataset_name = file_path.parent.relative_to(self.dataset_path).parts
        dataset_label = "/".join(dataset_name) if dataset_name else "Root"

        # Initialize Metadata & QC dictionaries
        meta = {
            'Case_ID': case_id,
            'Dataset': dataset_label,
            'File_Path': str(file_path),
            'Volume_Size_MB': round(os.path.getsize(file_path) / (1024 * 1024), 2)
        }
        
        qc = {
            'Case_ID': case_id,
            'Dataset': dataset_label,
            'Corrupted': False,
            'Unreadable': False,
            'Low_Contrast': False,
            'Incomplete_Scan': False,
            'Poor_Quality': False,
            'Duplicate': False
        }

        try:
            # 1. Load volume
            img = nib.load(file_path)
            data = img.get_fdata()
            header = img.header

            # Get dimensions & spacing
            dims = data.shape
            spacing = [float(s) for s in header.get_zooms()]
            
            meta['Dimensions'] = "x".join(map(str, dims))
            meta['Voxel_Spacing'] = "x".join(map(lambda s: f"{s:.2f}", spacing))
            meta['Slice_Count'] = dims[2] if len(dims) > 2 else 0
            meta['Bit_Depth'] = int(header.get_data_dtype().name.replace("int", "").replace("float", "").replace("uint", ""))
        except Exception as e:
            logger.error(f"Error loading volume {case_id}: {str(e)}")
            qc['Corrupted'] = True
            qc['Poor_Quality'] = True
            return case_id, None, meta, qc

        # 2. Validate basic specs
        if len(data.shape) < 3:
            qc['Unreadable'] = True
            qc['Poor_Quality'] = True
            return case_id, None, meta, qc

        # Check slices threshold
        min_slices = self.config['quality_control']['min_slice_count']
        if data.shape[2] < min_slices:
            qc['Incomplete_Scan'] = True
            qc['Poor_Quality'] = True

        # Check contrast standards
        voxel_std = np.std(data)
        min_std = self.config['quality_control']['min_contrast_std']
        if voxel_std < min_std:
            qc['Low_Contrast'] = True
            qc['Poor_Quality'] = True

        # 3. Orient & Preprocess
        # Correct orientation to standard RAS layout if enabled
        if self.config['image_processing']['correct_orientation']:
            # Mock axis transpose alignment
            pass

        # Crop borders of background values (intensity = 0 or low threshold)
        if self.config['image_processing']['crop_borders']:
            threshold = self.config['image_processing']['crop_threshold']
            non_zero_coords = np.argwhere(data > threshold)
            if non_zero_coords.size > 0:
                min_idx = non_zero_coords.min(axis=0)
                max_idx = non_zero_coords.max(axis=0)
                # Expand box slightly to preserve anatomical boundary details
                pad = 4
                z_dim, y_dim, x_dim = data.shape
                data = data[
                    max(0, min_idx[0]-pad):min(z_dim, max_idx[0]+pad),
                    max(0, min_idx[1]-pad):min(y_dim, max_idx[1]+pad),
                    max(0, min_idx[2]-pad):min(x_dim, max_idx[2]+pad)
                ]

        # Voxel intensity scaling to 0-255 range
        if self.config['image_processing']['normalize_intensity']:
            d_min, d_max = data.min(), data.max()
            if d_max > d_min:
                data = ((data - d_min) / (d_max - d_min) * 255.0).astype(np.uint8)
            else:
                data = np.zeros_like(data, dtype=np.uint8)

        # 4. Generate Slices
        center_z = data.shape[0] // 2
        center_y = data.shape[1] // 2
        center_x = data.shape[2] // 2

        # Extract 5 slices centered at coordinates
        slices = {
            'Axial': [data[min(data.shape[0]-1, max(0, center_z + offset)), :, :] for offset in [-2, -1, 0, 1, 2]],
            'Coronal': [data[:, min(data.shape[1]-1, max(0, center_y + offset)), :] for offset in [-2, -1, 0, 1, 2]],
            'Sagittal': [data[:, :, min(data.shape[2]-1, max(0, center_x + offset))] for offset in [-2, -1, 0, 1, 2]]
        }

        # Apply CLAHE to improve contrast on extracted slices
        if self.config['image_processing']['clahe']['enabled']:
            clip = self.config['image_processing']['clahe']['clip_limit']
            grid = tuple(self.config['image_processing']['clahe']['tile_grid_size'])
            clahe = cv2.createCLAHE(clipLimit=clip, tileGridSize=grid)
            for plane in ['Axial', 'Coronal', 'Sagittal']:
                slices[plane] = [clahe.apply(s) for s in slices[plane]]

        # Generate Maximum Intensity Projection (MIP)
        mip_axial = np.max(data, axis=0)
        if self.config['image_processing']['clahe']['enabled']:
            mip_axial = clahe.apply(mip_axial)
        slices['MIP'] = mip_axial

        return case_id, slices, meta, qc

    def generate_preview_sheet(self, case_id, slices, meta, qc_flagged):
        """Create a professional high-resolution clinical overview sheet (PNG)."""
        if slices is None:
            return

        fig, axes = plt.subplots(4, 5, figsize=(16, 14), facecolor='#090d16')
        plt.subplots_adjust(wspace=0.15, hspace=0.3, top=0.88, bottom=0.04, left=0.04, right=0.96)

        # Render Header Section details using standard text figures
        title_color = '#ef4444' if qc_flagged else '#22c55e'
        qual_text = 'QC WARNING (Low Quality)' if qc_flagged else 'Excellent Quality'

        header_text = (
            f"CanineAI CBCT Labeling Worksheet  |  Case ID: {case_id}  |  Dataset: {meta.get('Dataset')}\n"
            f"Dimensions: {meta.get('Dimensions')} px  |  Voxel Spacing: {meta.get('Voxel_Spacing')} mm  |  Slices: {meta.get('Slice_Count')}  |  QC Status: {qual_text}"
        )
        fig.suptitle(header_text, color='#f8fafc', fontsize=12, fontweight='bold', ha='center')

        # 1. Axial view rows
        for i, offset in enumerate(['Center-2', 'Center-1', 'Center', 'Center+1', 'Center+2']):
            ax = axes[0, i]
            ax.imshow(slices['Axial'][i], cmap='gray')
            ax.set_title(f"Axial {offset}", color='#94a3b8', fontsize=8)
            ax.axis('off')

        # 2. Coronal view rows
        for i, offset in enumerate(['Center-2', 'Center-1', 'Center', 'Center+1', 'Center+2']):
            ax = axes[1, i]
            ax.imshow(slices['Coronal'][i], cmap='gray')
            ax.set_title(f"Coronal {offset}", color='#94a3b8', fontsize=8)
            ax.axis('off')

        # 3. Sagittal view rows
        for i, offset in enumerate(['Center-2', 'Center-1', 'Center', 'Center+1', 'Center+2']):
            ax = axes[2, i]
            ax.imshow(slices['Sagittal'][i], cmap='gray')
            ax.set_title(f"Sagittal {offset}", color='#94a3b8', fontsize=8)
            ax.axis('off')

        # 4. MIP viewport pane
        ax_mip = axes[3, 2]
        ax_mip.imshow(slices['MIP'], cmap='gray')
        ax_mip.set_title("Axial MIP Projection", color='#f43f5e', fontsize=9, fontweight='bold')
        ax_mip.axis('off')

        # Clear remaining subplot placeholders in layout grid
        for i in [0, 1, 3, 4]:
            fig.delaxes(axes[3, i])

        # Save preview sheet
        output_png = self.preview_path / f"{case_id}_review.png"
        plt.savefig(output_png, facecolor=fig.get_facecolor(), edgecolor='none', dpi=180)
        plt.close()

    def generate_pdf_documents(self):
        """Compile README.pdf and Instructions.pdf using ReportLab."""
        readme_path = self.output_path / "README.pdf"
        instructions_path = self.output_path / "Instructions.pdf"

        styles = getSampleStyleSheet()
        
        # Define clean clinical styles
        body_style = ParagraphStyle(
            'ClinicalBody',
            parent=styles['Normal'],
            fontName='Helvetica',
            fontSize=10.5,
            leading=14,
            textColor=colors.HexColor('#334155')
        )
        
        title_style = ParagraphStyle(
            'ClinicalTitle',
            parent=styles['Heading1'],
            fontName='Helvetica-Bold',
            fontSize=22,
            leading=26,
            textColor=colors.HexColor('#0f172a'),
            spaceAfter=15
        )

        # 1. Compile README.pdf
        doc_readme = SimpleDocTemplate(str(readme_path), pagesize=letter, leftMargin=40, rightMargin=40, topMargin=40, bottomMargin=40)
        story_readme = []
        story_readme.append(Paragraph("CanineAI Supervised Labeling Package", title_style))
        story_readme.append(Spacer(1, 10))
        
        intro_text = (
            "Welcome to the CanineAI clinical annotation campaign. This folder contains the 3D CBCT volume "
            "review sheets and labeling directories prepared specifically for orthodontist analysis. The doctor "
            "does not need to install any medical visualization viewer libraries or open complex file formats. "
            "All volume details have been formatted as pre-arranged orthogonal slices in the Preview/ folder."
        )
        story_readme.append(Paragraph(intro_text, body_style))
        story_readme.append(Spacer(1, 15))

        story_readme.append(Paragraph("<b>Package Folder Contents:</b>", styles['Heading3']))
        story_readme.append(Spacer(1, 5))
        
        content_table_data = [
            ["Directory/File", "Purpose / Instructions"],
            ["Preview/", "Contains PNG review sheets for all CBCT cases (Axial, Coronal, Sagittal grids)."],
            ["metadata.csv", "Database list detailing image sizes, voxel spacing, slice thickness shape, etc."],
            ["quality_report.csv", "Contains validation checks flags (e.g. low quality, low contrast, incomplete slices)."],
            ["labels.xlsx", "Editable Excel sheet template for clinical annotations (Primary Return File)."],
            ["labels.csv", "Backup CSV template file matching Excel rows mappings."],
            ["Instructions.pdf", "Detailed reference guide explaining diagnosis values codes configurations."]
        ]
        
        t = Table(content_table_data, colWidths=[130, 390])
        t.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#f1f5f9')),
            ('TEXTCOLOR', (0,0), (-1,0), colors.HexColor('#0f172a')),
            ('FONTNAME', (0,0), (-1,0), 'Helvetica-Bold'),
            ('BOTTOMPADDING', (0,0), (-1,0), 6),
            ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#cbd5e1')),
            ('FONTNAME', (0,1), (-1,-1), 'Helvetica'),
            ('FONTSIZE', (0,0), (-1,-1), 9.5),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,1), (-1,-1), 6),
        ]))
        story_readme.append(t)
        
        doc_readme.build(story_readme)

        # 2. Compile Instructions.pdf
        doc_inst = SimpleDocTemplate(str(instructions_path), pagesize=letter, leftMargin=40, rightMargin=40, topMargin=40, bottomMargin=40)
        story_inst = []
        story_inst.append(Paragraph("Clinical Labeling Guidelines", title_style))
        story_inst.append(Spacer(1, 10))

        story_inst.append(Paragraph("<b>Step 1: Open the Case Preview</b>", styles['Heading3']))
        story_inst.append(Paragraph(
            "For each row in the Excel template, find the matching image inside the <code>Preview/</code> folder "
            "(e.g., matching <code>CASE_ID_review.png</code>). Use this visual workspace sheet to analyze the maxilla and canine paths.",
            body_style
        ))
        story_inst.append(Spacer(1, 10))

        story_inst.append(Paragraph("<b>Step 2: Fill out labels.xlsx</b>", styles['Heading3']))
        story_inst.append(Paragraph(
            "Open <code>labels.xlsx</code>. For every case row, fill in the following columns using the exact standardized value strings:",
            body_style
        ))
        story_inst.append(Spacer(1, 8))

        columns_data = [
            ["Column Name", "Standard Options", "Description"],
            ["Left_Canine", "Normal / Delayed / Impacted / Not Visible", "Eruption angle/path of left canine #23."],
            ["Right_Canine", "Normal / Delayed / Impacted / Not Visible", "Eruption angle/path of right canine #13."],
            ["Overall_Diagnosis", "Normal / Delayed / Impacted / Uncertain / Exclude", "Patient overall diagnostics assessment."],
            ["Image_Quality", "Excellent / Good / Fair / Poor", "Visual assessment of contrast and anatomical clarity."],
            ["Doctor_Notes", "Free text", "Enter clinical findings or reasoning for exclusion."],
            ["Reviewed_By", "Name / Initials", "Orthodontist name performing annotation."],
            ["Review_Date", "YYYY-MM-DD", "Date of review completion."]
        ]
        
        t_cols = Table(columns_data, colWidths=[110, 180, 230])
        t_cols.setStyle(TableStyle([
            ('BACKGROUND', (0,0), (-1,0), colors.HexColor('#f8fafc')),
            ('FONTNAME', (0,0), (-1,0), 'Helvetica-Bold'),
            ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor('#e2e8f0')),
            ('FONTSIZE', (0,0), (-1,-1), 9),
            ('VALIGN', (0,0), (-1,-1), 'TOP'),
            ('BOTTOMPADDING', (0,0), (-1,-1), 5),
        ]))
        story_inst.append(t_cols)
        story_inst.append(Spacer(1, 15))

        story_inst.append(Paragraph("<b>Step 3: Package Return</b>", styles['Heading3']))
        story_inst.append(Paragraph(
            "Once labeling is complete, save the <code>labels.xlsx</code> file and return it directly to the "
            "CanineAI system administration team for supervised learning ingestion.",
            body_style
        ))

        doc_inst.build(story_inst)
        logger.info("PDF documentation instructions compiled successfully.")

    def run(self):
        """Orchestrate the complete package generation workflow."""
        logger.info("Starting CanineAI dataset prep pipeline...")
        start_time = time.time()

        # 1. Scan files
        files = self.scan_dataset()
        if not files:
            logger.warning("No files found. Exiting.")
            return

        # Load processed database states if resume enabled
        metadata_file = self.output_path / "metadata.csv"
        quality_file = self.output_path / "quality_report.csv"
        processed_cases = set()
        
        if self.config['performance']['resume_interrupted'] and metadata_file.exists():
            try:
                df_existing = pd.read_csv(metadata_file)
                processed_cases = set(df_existing['Case_ID'].astype(str).tolist())
                logger.info(f"Resuming task. Found {len(processed_cases)} already processed cases. Skipping.")
            except Exception as e:
                logger.warning(f"Failed to read existing metadata.csv for resume: {e}")

        # Filter out already processed files
        files_to_process = [f for f in files if f.name.replace(".nii.gz", "").replace(".nii", "") not in processed_cases]
        logger.info(f"Need to process {len(files_to_process)} / {len(files)} volumes.")

        # Data collection buffers
        all_metadata = []
        all_qc = []
        label_rows = []

        # 2. Setup workers execution pool
        use_mp = self.config['performance']['use_multiprocessing']
        num_workers = self.config['performance']['num_workers']

        if use_mp and len(files_to_process) > 1:
            logger.info(f"Launching multiprocessing execution pool with {num_workers} workers.")
            pool = multiprocessing.Pool(processes=num_workers)
            
            # Map volumes processing
            results = []
            with tqdm(total=len(files_to_process), desc="Processing CBCT Volumes") as pbar:
                for r in pool.imap_unordered(self.process_volume, files_to_process):
                    results.append(r)
                    pbar.update(1)
            
            pool.close()
            pool.join()
        else:
            logger.info("Running sequential single-threaded volume processing.")
            results = []
            for f in tqdm(files_to_process, desc="Processing CBCT Volumes"):
                results.append(self.process_volume(f))

        # Check hashes for duplicate detection
        file_hashes = {}
        for f in files:
            cid = f.name.replace(".nii.gz", "").replace(".nii", "")
            try:
                file_hashes[cid] = self.calculate_file_hash(f)
            except Exception:
                file_hashes[cid] = None

        # 3. Process results and generate sheets
        for case_id, slices, meta, qc in results:
            # Check duplicate
            h = file_hashes.get(case_id)
            if h:
                if h in self.processed_hashes:
                    qc['Duplicate'] = True
                    qc['Poor_Quality'] = True
                else:
                    self.processed_hashes[h] = case_id
            
            all_metadata.append(meta)
            all_qc.append(qc)

            # Generate PNG Sheet
            self.generate_preview_sheet(case_id, slices, meta, qc['Poor_Quality'])

            # Compile templates rows
            label_rows.append({
                'Case_ID': case_id,
                'Dataset': meta.get('Dataset'),
                'Left_Canine': 'Normal',
                'Right_Canine': 'Normal',
                'Overall_Diagnosis': 'Normal',
                'Image_Quality': 'Excellent' if not qc['Poor_Quality'] else 'Fair',
                'Doctor_Notes': '',
                'Reviewed_By': '',
                'Review_Date': '',
                'Status': 'Unlabeled'
            })

        # 4. Save CSV & Excel outputs
        df_meta = pd.DataFrame(all_metadata)
        df_qc = pd.DataFrame(all_qc)
        df_labels = pd.DataFrame(label_rows)

        # Merge with existing files if resume was active
        if processed_cases and metadata_file.exists() and not df_meta.empty:
            try:
                df_meta = pd.concat([pd.read_csv(metadata_file), df_meta]).drop_duplicates(subset=['Case_ID'])
                df_qc = pd.concat([pd.read_csv(quality_file), df_qc]).drop_duplicates(subset=['Case_ID'])
                labels_file = self.output_path / "labels.csv"
                if labels_file.exists():
                    df_labels = pd.concat([pd.read_csv(labels_file), df_labels]).drop_duplicates(subset=['Case_ID'])
            except Exception as e:
                logger.error(f"Error merging with existing resume CSV files: {e}")

        if not df_meta.empty:
            df_meta.to_csv(metadata_file, index=False)
            df_qc.to_csv(quality_file, index=False)
            
            labels_csv = self.output_path / "labels.csv"
            df_labels.to_csv(labels_csv, index=False)

            # Save formatted Excel sheets
            labels_xlsx = self.output_path / "labels.xlsx"
            df_labels.to_excel(labels_xlsx, index=False)
            logger.info("Saved CSV and Excel labels templates sheets.")

        # 5. Compile PDFs
        self.generate_pdf_documents()

        elapsed = time.time() - start_time
        logger.info(f"CanineAI dataset review package compilation complete in {elapsed:.2f} seconds!")
        logger.info(f"Doctor review package directory: {self.output_path}")

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="CanineAI Dataset Review Package Compiler")
    parser.add_argument('--dataset-path', type=str, help="Override path to dataset")
    parser.add_argument('--output-path', type=str, help="Override output directory")
    parser.add_argument('--workers', type=int, help="Override multiprocessing workers count")
    args = parser.parse_args()

    tool = DatasetPrepTool(config_path='config.yaml', env_path='.env', cmd_args=args)
    tool.run()
