-- ── CanineAI Database Reset Migration (V10) ─────────────────────────────────
-- Deletes all user-generated records, resets AUTO_INCREMENT counters,
-- and preserves master roles, permissions, and sequence definitions.

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM uploaded_files;
DELETE FROM upload_sessions;
DELETE FROM series;
DELETE FROM ai_jobs;
DELETE FROM clinical_reports;
DELETE FROM studies;
DELETE FROM patients;
DELETE FROM security_audit_logs;
DELETE FROM password_reset_tokens;
DELETE FROM refresh_tokens;
DELETE FROM user_roles;
DELETE FROM users;

ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE refresh_tokens AUTO_INCREMENT = 1;
ALTER TABLE password_reset_tokens AUTO_INCREMENT = 1;
ALTER TABLE security_audit_logs AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- Ensure standard system roles exist
INSERT INTO roles (name, description, created_at, created_by) VALUES
('ADMIN', 'Super administrator with full clinical system access', NOW(), 'System'),
('ORTHODONTIST', 'Orthodontic specialist dashboard access role', NOW(), 'System'),
('RADIOLOGIST', 'Radiology scanner systems operator role', NOW(), 'System'),
('RESEARCHER', 'Anonymized dataset clinical research access role', NOW(), 'System'),
('CLINICIAN', 'Standard clinician dashboard access role', NOW(), 'System')
ON DUPLICATE KEY UPDATE description = VALUES(description);
