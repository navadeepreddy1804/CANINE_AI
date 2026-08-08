import json
import tempfile
from pathlib import Path
import pytest
from app.services.toothseg_inference_service import ToothSegInferenceService

def test_reproduce_load_fdi_priors_bug():
    # Create a minimal mock fdi distributions json
    mock_data = {
        "means": [[ [0.0, 0.0, 0.0] for _ in range(32)] for _ in range(32)],
        "covs": [[ [[1.0, 0.0], [0.0, 1.0]] for _ in range(32)] for _ in range(32)]
    }
    with tempfile.NamedTemporaryFile("w+", suffix=".json", delete=False) as f:
        json.dump(mock_data, f)
        tmp_path = f.name

    try:
        service = ToothSegInferenceService()
        service._fdi_distributions_path = Path(tmp_path)
        # This should fail with NameError: name 'multivariate_normal' is not defined
        service._load_fdi_priors()
    finally:
        Path(tmp_path).unlink(missing_ok=True)
