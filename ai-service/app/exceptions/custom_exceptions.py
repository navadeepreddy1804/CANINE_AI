class AIException(Exception):
    def __init__(self, message: str, status_code: int = 500):
        super().__init__(message)
        self.message = message
        self.status_code = status_code

class ModelMissingException(AIException):
    def __init__(self, message: str = "Configured weights checkpoint files missing"):
        super().__init__(message, status_code=404)

class CorruptedDicomException(AIException):
    def __init__(self, message: str = "Invalid or corrupted DICOM file structure"):
        super().__init__(message, status_code=400)

class GPUFailureException(AIException):
    def __init__(self, message: str = "CUDA hardware allocation failed"):
        super().__init__(message, status_code=500)
