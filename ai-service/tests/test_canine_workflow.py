import uuid
import numpy as np
import pytest
from app.analysis.canine_extractor import CanineExtractor
from app.analysis.toothseg_analyzer import ToothSegClinicalAnalyzer
from app.analysis.clinical_measurements import ClinicalMeasurementsCalculator


def test_canine_extractor_maxillary_detection():
    # Create 3D synthetic volume (Z=40, Y=100, X=100)
    vol = np.zeros((40, 100, 100), dtype=np.uint8)

    # Place Maxillary Right Canine (FDI 13 / ToothSeg idx 6)
    vol[15:25, 40:60, 30:45] = 6
    # Place Maxillary Left Canine (FDI 23 / ToothSeg idx 11)
    vol[15:25, 40:60, 55:70] = 11
    # Place Incisors (idx 7, 8, 9, 10)
    vol[10:14, 40:50, 45:55] = 7

    res = CanineExtractor.extract_canines(vol, spacing=(0.3, 0.3, 0.3), total_preview_slices=12)

    assert res["detected"] is True
    assert res["totalTeethCount"] >= 3
    assert len(res["allCanines"]) == 2

    primary = res["primaryCanine"]
    assert primary["fdiNumber"] in (13, 23)
    assert primary["arch"] == "MAXILLARY"
    assert primary["voxelCount"] > 0
    assert primary["volumeMm3"] > 0.0
    assert primary["angulationDegrees"] > 0.0

    bbox = primary["boundingBox"]
    assert "sliceIndex" in bbox
    assert 0 <= bbox["sliceIndex"] < 12
    assert 0 <= bbox["x"] <= 512
    assert 0 <= bbox["y"] <= 512
    assert bbox["width"] > 0
    assert bbox["height"] > 0


def test_canine_extractor_empty_volume():
    vol = np.zeros((30, 80, 80), dtype=np.uint8)
    res = CanineExtractor.extract_canines(vol, spacing=(0.3, 0.3, 0.3))

    assert res["detected"] is False
    assert res["totalTeethCount"] == 0
    assert "primaryCanine" in res
    assert res["primaryCanine"]["voxelCount"] == 0


def test_toothseg_clinical_analyzer_deterministic():
    study_id = str(uuid.uuid4())
    canine_roi = {
        "detected": True,
        "primaryCanine": {
            "fdiNumber": 13,
            "toothName": "Maxillary Right Canine",
            "sectorLocation": "Maxillary Right Quadrant (Sector 1)",
            "angulationDegrees": 34.5,
            "volumeMm3": 450.0,
            "boundingBox": {"sliceIndex": 4, "x": 180, "y": 210, "width": 64, "height": 64}
        }
    }

    pred1 = ToothSegClinicalAnalyzer.analyze(study_id, canine_roi)
    pred2 = ToothSegClinicalAnalyzer.analyze(study_id, canine_roi)

    assert pred1["prediction"] == pred2["prediction"]
    assert pred1["confidence"] == pred2["confidence"]
    assert pred1["angle"] == pred2["angle"]
    assert pred1["eruptionProbability"] == pred2["eruptionProbability"]
    assert pred1["threatLevel"] == pred2["threatLevel"]
    assert pred1["metadata"]["engine"] == "ToothSeg_v2.1_Anatomical_Clinical"
    assert pred1["metadata"]["diagnosticSource"] == "ToothSegClinicalAnalyzer"


def test_toothseg_clinical_analyzer_medical_logic():
    study_id = "test-study-impacted-001"
    high_angle_roi = {
        "detected": True,
        "primaryCanine": {
            "fdiNumber": 13,
            "toothName": "Maxillary Right Canine",
            "sectorLocation": "Maxillary Right Quadrant (Sector 1)",
            "angulationDegrees": 36.2,
            "volumeMm3": 480.0,
            "boundingBox": {"sliceIndex": 6, "x": 190, "y": 200, "width": 70, "height": 70}
        }
    }

    pred = ToothSegClinicalAnalyzer.analyze(study_id, high_angle_roi)
    assert pred["prediction"] == "IMPACTED_CANINE"
    assert pred["threatLevel"] == "HIGH"
    assert pred["angle"] == 36.2

    low_angle_roi = {
        "detected": True,
        "primaryCanine": {
            "fdiNumber": 13,
            "toothName": "Maxillary Right Canine",
            "sectorLocation": "Maxillary Right Quadrant (Sector 1)",
            "angulationDegrees": 11.5,
            "volumeMm3": 390.0,
            "boundingBox": {"sliceIndex": 6, "x": 190, "y": 200, "width": 70, "height": 70}
        }
    }

    pred_low = ToothSegClinicalAnalyzer.analyze(study_id, low_angle_roi)
    assert pred_low["prediction"] == "NORMAL_ERUPTION"
    assert pred_low["threatLevel"] == "LOW"


def test_clinical_measurements_calculator():
    vol = np.zeros((30, 80, 80), dtype=np.uint8)
    vol[10:20, 20:40, 20:40] = 6  # FDI 13
    vol[10:20, 20:40, 45:65] = 18 # Mandibular tooth

    stats = ClinicalMeasurementsCalculator.calculate_statistics(vol, voxel_spacing=0.3)
    assert stats["volumeMm3"] > 0
    assert stats["toothCount"] == 2
    assert stats["maxillaryTeethCount"] >= 1
    assert stats["mandibularTeethCount"] >= 1
    assert "threatVectorAssessment" in stats
