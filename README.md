# CanineAI: AI-Assisted CBCT Dental Analysis Platform

CanineAI is a production-grade, enterprise-ready dental analysis solution designed to process CBCT (Cone Beam Computed Tomography) dental medical imaging files, execute sophisticated deep learning models for maxillary canine localization and dental segmentation, and generate dental diagnostic reports.

This repository implements the complete foundational architecture for all layers of the CanineAI ecosystem:
- **`android-app/`**: Android application (Kotlin, Jetpack Compose, MVVM, Clean Architecture, Hilt, Retrofit, Room).
- **`web-app/`**: Web application (Java Spring Boot MVC, Thymeleaf, Bootstrap 5, AdminLTE).
- **`backend/`**: REST API core backend server (Java 21, Spring Boot 3, Spring Security, SQLite, JPA).
- **`ai-service/`**: Microservice pipeline for PyTorch and MONAI-based CBCT segmentation models (Python, FastAPI).
- **`shared-api/`**: Module housing OpenAPI specifications and generated API clients.
- **`deployment/`**: Docker configs for orchestration.
- **`docs/`**: Comprehensive system diagrams, coding rules, strategies, and package designs.

For the full system design, system topologies, package layout, and diagrams, see [architecture.md](file:///c:/Users/darsi/Downloads/CANINE_AI/docs/architecture.md).
