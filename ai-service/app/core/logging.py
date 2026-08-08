import sys
from loguru import logger

def setup_logging():
    if hasattr(sys.stdout, "reconfigure"):
        try:
            sys.stdout.reconfigure(encoding="utf-8", errors="backslashreplace")
        except Exception:
            pass

    # Remove standard default logger handlers
    logger.remove()
    
    # Configure production Loguru JSON serialization output
    logger.add(
        sys.stdout,
        serialize=True,
        level="INFO"
    )
    logger.info("Structured JSON Loguru logger successfully initialized.")
