# CanineAI Global Engineering Rules

These rules govern the development of the CanineAI enterprise Healthcare SaaS platform.

## General Rules
- **No placeholders**: Never generate placeholder code, pseudo code, TODO comments, incomplete implementations, or sample implementations.
- **No duplicates**: Never generate duplicate code or regenerate already completed modules.
- **Clean Architecture & SOLID**: Follow SOLID principles, DRY, KISS, Repository Pattern, and Dependency Injection. Use feature-first package organization.

## Code Quality
- All code must be readable, maintainable, extensible, reusable, modular, highly cohesive, loosely coupled, thread-safe, and null-safe.
- Avoid dead code, unused imports, circular dependencies, hardcoded values, magic numbers, or code duplication.

## Security
- **Credentials & Keys**: Read all secrets from environment variables. Never store credentials, API keys, JWT secrets, database passwords, or private keys in source code.
- **Data Sanity**: Input validation on every request. Protect endpoints appropriately.
- **OWASP Vulnerabilities**: Prevent SQL Injection, Path Traversal (directory traversal), XSS, CSRF, broken authentication, and broken authorization.

## Performance
- **Non-blocking Request Threads**: All long-running tasks must execute asynchronously. Never block request threads or FastAPI workers.
- **Android Client**:
  - Use MVVM, Kotlin Coroutines + Flow, StateFlow, Hilt, Navigation Compose, Material 3.
  - Never perform network, database, or file operations on the Main thread. Preserve UI states during uploads.
- **Web Client**:
  - Use responsive layout with Bootstrap 5, AdminLTE templates.
  - Avoid full-page reloads using Fetch/AJAX calls.
- **Spring Boot Backend**:
  - Keep controllers thin; delegate all business logic to service layers.
  - Use connection pooling (HikariCP) and efficient non-blocking I/O.
- **FastAPI AI Service**:
  - Run inference operations as background queue tasks using async endpoints.
  - Return progress updates periodically.
  - Use GPU first with CPU fallback.

## Database Design
- Use JPA best practices. Never expose Entities; always map to DTOs using MapStruct.
- Use Transactions correctly. Use optimistic locking where appropriate.
- Dev database on SQLite, production database ready for MySQL without code modifications.

## AI & LLM Architectures
- **Registry & Loader**: Model registry and loaders configurations. Never hardcode models.
- **FastAPI Outputs**: FastAPI microservice performs only AI inference and returns ONLY structured machine-readable JSON (never HTML, Markdown, or PDF).
- **LLM Integrations**: Clinicians clients must never call LLM endpoints directly. All communication with LLM runs through Spring Boot.
- **Provider Abstraction**: Abstract LLM provider clients (OpenAI, Claude, Gemini, OpenRouter, Ollama) and implement prompt templating and version controls.

## Error Handling & Diagnostics
- **Exception Interceptors**: Implement global Rest controllers advices mapping error responses.
- **Correlation IDs**: Inject UUID tracking variables into logging contexts (MDC correlation IDs) and HTTP headers.
- **Structured logs**: Log requests, responses, performance metrics, AI inference stages, and LLM completions.

## Testing Architecture
- Prepare architectures compatible with JUnit 5, Mockito, Spring Boot tests, Pytest, and integration/E2E test harnesses.
