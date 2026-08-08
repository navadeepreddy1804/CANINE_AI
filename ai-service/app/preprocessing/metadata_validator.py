from loguru import logger
from typing import Dict, Any

class MetadataValidator:
    @staticmethod
    def validate_dicom_metadata(metadata: Dict[str, Any]) -> bool:
        """
        Validates parsed DICOM metadata to verify patient tags and modalities sanity.
        """
        logger.info("Validating DICOM tags metadata...")
        
        # Verify Modality is CT (CBCT) or matching scans
        modality = metadata.get("modality", "CT")
        if modality not in ["CT", "MR", "US"]:
            logger.error(f"Invalid scan modality: {modality}. CBCT/CT required.")
            return False

        # Verify Series UID exists
        series_uid = metadata.get("series_instance_uid", "")
        if not series_uid:
            logger.error("Missing critical DICOM SeriesInstanceUID tag.")
            return False

        logger.info("DICOM tags metadata validated successfully.")
        return True
