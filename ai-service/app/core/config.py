from pathlib import Path
from typing import Optional

try:
    from pydantic_settings import BaseSettings
    from pydantic import Field
except Exception:
    try:
        from pydantic.v1 import BaseSettings, Field  # type: ignore
    except Exception:
        from pydantic import BaseModel as BaseSettings, Field  # type: ignore


class Settings(BaseSettings):
    app_name: str = "CanineAI FastAPI Diagnostics microservice"
    app_version: str = "1.0.0"
    
    # Storage settings
    model_dir: str = Field(default="weights", validation_alias="MODEL_DIR")
    upload_dir: str = Field(default="uploads", validation_alias="UPLOAD_DIR")
    
    # ToothSeg & nnUNet Paths
    nnunet_results: str = Field(default=r"E:\nnUNet\nnUNet_results", validation_alias="NNUNET_RESULTS")
    nnunet_raw: str = Field(default=r"E:\nnUNet\nnUNet_raw", validation_alias="NNUNET_RAW")
    nnunet_preprocessed: str = Field(default=r"E:\nnUNet\nnUNet_preprocessed", validation_alias="NNUNET_PREPROCESSED")
    toothseg_distributions_path: str = Field(
        default=r"E:\AI-Models\ToothSeg\toothseg\datasets\toothfairy2\fdi_pair_distrs.json",
        validation_alias="TOOTHSEG_FDI_DISTRIBUTIONS_PATH"
    )

    # Compute config
    gpu_enabled: bool = Field(default=True, validation_alias="GPU_ENABLED")
    cpu_fallback: bool = Field(default=True, validation_alias="CPU_FALLBACK")
    inference_threads: int = Field(default=4, validation_alias="INFERENCE_THREADS")
    batch_size: int = Field(default=1, validation_alias="BATCH_SIZE")
    
    # Security properties
    internal_gateway_key: str = Field(
        default="sk-canine-local-dev-key", 
        validation_alias="INTERNAL_GATEWAY_KEY"
    )

    ai_mode: str = Field(default="real", validation_alias="AI_MODE")

    def setup_nnunet_environment(self) -> None:
        """Sets required nnU-Net environment variables into process environment."""
        import os
        os.environ["nnUNet_raw"] = str(self.nnunet_raw)
        os.environ["nnUNet_preprocessed"] = str(self.nnunet_preprocessed)
        os.environ["nnUNet_results"] = str(self.nnunet_results)
        os.environ.setdefault("nnUNet_compile", "F")

    def candidate_upload_roots(self) -> list[str]:
        configured = (self.upload_dir or "uploads").strip()
        cwd = Path.cwd().resolve()
        repo_root = Path(__file__).resolve().parents[3]

        candidates: list[Path] = []

        def add_candidate(raw_value: str):
            if not raw_value:
                return
            path = Path(raw_value).expanduser()
            if not path.is_absolute():
                path = (cwd / path).resolve()
            candidates.append(path)

        add_candidate(configured)
        add_candidate(str(cwd / "uploads"))
        add_candidate(str(cwd / "backend" / "uploads"))
        add_candidate(str(repo_root / configured))
        add_candidate(str(repo_root / "uploads"))
        add_candidate(str(repo_root / "backend" / "uploads"))
        add_candidate(str(repo_root / "ai-service" / "uploads"))

        normalized: list[str] = []
        seen: set[str] = set()
        for candidate in candidates:
            key = str(candidate)
            if key not in seen:
                seen.add(key)
                normalized.append(key)
        return normalized

    class Config:
        try:
            import dotenv
            env_file = ".env"
        except ImportError:
            env_file = None
        extra = "ignore"

settings = Settings()
settings.setup_nnunet_environment()

