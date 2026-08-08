-- 1. Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create roles table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Create role_permissions join table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(255),
    role_title VARCHAR(255) NOT NULL,
    hospital VARCHAR(255) NOT NULL,
    department VARCHAR(255) NOT NULL,
    medical_registration_number VARCHAR(255) NULL UNIQUE,
    years_of_experience INT NULL,
    blood_group VARCHAR(50) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    account_expired BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Create user_roles join table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Create security_audit_logs table
CREATE TABLE IF NOT EXISTS security_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    ip_address VARCHAR(255),
    device_info VARCHAR(255),
    timestamp DATETIME(6) NOT NULL,
    details VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Create patients table
CREATE TABLE IF NOT EXISTS patients (
    id CHAR(36) NOT NULL PRIMARY KEY,
    hospital_patient_id VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    country VARCHAR(255),
    blood_group VARCHAR(50) NULL,
    medical_notes VARCHAR(1000) NULL,
    orthodontist VARCHAR(255) NOT NULL,
    hospital VARCHAR(255) NOT NULL,
    registration_date DATE NOT NULL,
    status VARCHAR(50) NOT NULL,
    version BIGINT NULL,
    deleted_by VARCHAR(255) NULL,
    deleted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Create upload_sessions table
CREATE TABLE IF NOT EXISTS upload_sessions (
    id CHAR(36) NOT NULL PRIMARY KEY,
    patient_id CHAR(36) NOT NULL,
    username VARCHAR(255) NOT NULL,
    total_size_bytes BIGINT NOT NULL,
    total_files_count INT NOT NULL,
    uploaded_files_count INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    checksum_md5 VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    error_message VARCHAR(500) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Create uploaded_files table
CREATE TABLE IF NOT EXISTS uploaded_files (
    id CHAR(36) NOT NULL PRIMARY KEY,
    session_id CHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_location_path VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    sop_instance_uid VARCHAR(255) NULL,
    checksum_sha256 VARCHAR(255) NULL,
    CONSTRAINT fk_uploaded_files_session FOREIGN KEY (session_id) REFERENCES upload_sessions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Create studies table
CREATE TABLE IF NOT EXISTS studies (
    id CHAR(36) NOT NULL PRIMARY KEY,
    patient_id CHAR(36) NOT NULL,
    study_instance_uid VARCHAR(255) NOT NULL UNIQUE,
    study_date DATE NULL,
    study_time VARCHAR(50) NULL,
    modality VARCHAR(50) NOT NULL,
    study_description VARCHAR(255) NULL,
    manufacturer VARCHAR(255) NULL,
    device_model VARCHAR(255) NULL,
    voxel_size VARCHAR(255) NULL,
    pixel_spacing VARCHAR(255) NULL,
    slice_thickness DOUBLE NULL,
    rows_count INT NULL,
    columns_count INT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_studies_patient FOREIGN KEY (patient_id) REFERENCES patients (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. Create series table
CREATE TABLE IF NOT EXISTS series (
    id CHAR(36) NOT NULL PRIMARY KEY,
    study_id CHAR(36) NOT NULL,
    series_instance_uid VARCHAR(255) NOT NULL UNIQUE,
    series_number INT NULL,
    series_description VARCHAR(255) NULL,
    slice_count INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_series_study FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. Create ai_jobs table
CREATE TABLE IF NOT EXISTS ai_jobs (
    id CHAR(36) NOT NULL PRIMARY KEY,
    study_id CHAR(36) NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    active_model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(255) NOT NULL,
    start_time DATETIME(6) NULL,
    end_time DATETIME(6) NULL,
    progress_percentage INT NOT NULL,
    result_json VARCHAR(5000) NULL,
    error_message VARCHAR(500) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. Create clinical_reports table
CREATE TABLE IF NOT EXISTS clinical_reports (
    id CHAR(36) NOT NULL PRIMARY KEY,
    study_id CHAR(36) NOT NULL,
    status VARCHAR(50) NOT NULL,
    report_style VARCHAR(50) NOT NULL,
    report_markdown TEXT NULL,
    active_provider VARCHAR(255) NOT NULL,
    template_version VARCHAR(255) NOT NULL,
    prompt_version VARCHAR(255) NOT NULL,
    prompt_template_key VARCHAR(255) NOT NULL,
    prompt_token_usage INT NULL,
    completion_token_usage INT NULL,
    generation_latency_ms BIGINT NULL,
    doctor_comments VARCHAR(1000) NULL,
    approved_by VARCHAR(255) NULL,
    approved_at DATETIME(6) NULL,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. Create prompt_templates table
CREATE TABLE IF NOT EXISTS prompt_templates (
    id CHAR(36) NOT NULL PRIMARY KEY,
    template_key VARCHAR(255) NOT NULL UNIQUE,
    template_version VARCHAR(255) NOT NULL,
    prompt_template VARCHAR(5000) NOT NULL,
    hospital_override_key VARCHAR(255) NULL,
    language VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. Seed system roles
INSERT INTO roles (name, description, created_at, created_by) VALUES
('ADMIN', 'Super administrator with full clinical system access', NOW(), 'System'),
('ORTHODONTIST', 'Orthodontic specialist dashboard access role', NOW(), 'System'),
('RADIOLOGIST', 'Radiology scanner systems operator role', NOW(), 'System'),
('RESEARCHER', 'Anonymized dataset clinical research access role', NOW(), 'System'),
('CLINICIAN', 'Standard clinician dashboard access role', NOW(), 'System')
ON DUPLICATE KEY UPDATE description=VALUES(description);
