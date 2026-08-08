-- Flyway Migration V9: Unify Database and Table Collation to utf8mb4_0900_ai_ci and Fix Foreign Keys

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

DROP PROCEDURE IF EXISTS convert_table_collation;
DROP PROCEDURE IF EXISTS add_foreign_key_if_missing;

DELIMITER //

CREATE PROCEDURE convert_table_collation(IN tbl_name VARCHAR(64))
BEGIN
    DECLARE tbl_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO tbl_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = tbl_name;

    IF tbl_exists > 0 THEN
        SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

CREATE PROCEDURE add_foreign_key_if_missing(
    IN tbl_name VARCHAR(64),
    IN fk_name VARCHAR(64),
    IN fk_col VARCHAR(64),
    IN ref_tbl VARCHAR(64),
    IN ref_col VARCHAR(64),
    IN on_delete_action VARCHAR(32)
)
BEGIN
    DECLARE fk_exists INT DEFAULT 0;
    DECLARE tbl_exists INT DEFAULT 0;
    DECLARE ref_exists INT DEFAULT 0;

    SELECT COUNT(*) INTO tbl_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = tbl_name;

    SELECT COUNT(*) INTO ref_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = ref_tbl;

    IF tbl_exists > 0 AND ref_exists > 0 THEN
        SELECT COUNT(*) INTO fk_exists
        FROM information_schema.table_constraints
        WHERE constraint_schema = DATABASE()
          AND table_name = tbl_name
          AND constraint_name = fk_name
          AND constraint_type = 'FOREIGN KEY';

        IF fk_exists = 0 THEN
            SET @sql = CONCAT('ALTER TABLE `', tbl_name, '` ADD CONSTRAINT `', fk_name,
                              '` FOREIGN KEY (`', fk_col, '`) REFERENCES `', ref_tbl,
                              '` (`', ref_col, '`) ON DELETE ', on_delete_action);
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
        END IF;
    END IF;
END //

DELIMITER ;

-- 1. Convert all tables to utf8mb4_0900_ai_ci
CALL convert_table_collation('permissions');
CALL convert_table_collation('roles');
CALL convert_table_collation('role_permissions');
CALL convert_table_collation('users');
CALL convert_table_collation('user_roles');
CALL convert_table_collation('refresh_tokens');
CALL convert_table_collation('password_reset_tokens');
CALL convert_table_collation('security_audit_logs');
CALL convert_table_collation('patients');
CALL convert_table_collation('upload_sessions');
CALL convert_table_collation('uploaded_files');
CALL convert_table_collation('studies');
CALL convert_table_collation('series');
CALL convert_table_collation('ai_jobs');
CALL convert_table_collation('clinical_reports');
CALL convert_table_collation('prompt_templates');
CALL convert_table_collation('patient_id_sequences');
CALL convert_table_collation('study_storage_records');

-- 2. Standardize column lengths and collations for UUID identifiers
ALTER TABLE patients MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE studies MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE studies MODIFY COLUMN patient_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE studies MODIFY COLUMN upload_session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL;
ALTER TABLE upload_sessions MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE upload_sessions MODIFY COLUMN patient_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE uploaded_files MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE uploaded_files MODIFY COLUMN session_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE series MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE series MODIFY COLUMN study_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE ai_jobs MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE ai_jobs MODIFY COLUMN study_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE clinical_reports MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE clinical_reports MODIFY COLUMN study_id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;
ALTER TABLE prompt_templates MODIFY COLUMN id CHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

-- 3. Ensure foreign keys exist with matching column specifications
CALL add_foreign_key_if_missing('studies', 'fk_studies_patient', 'patient_id', 'patients', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('uploaded_files', 'fk_uploaded_files_session', 'session_id', 'upload_sessions', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('series', 'fk_series_study', 'study_id', 'studies', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('role_permissions', 'fk_role_permissions_role', 'role_id', 'roles', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('role_permissions', 'fk_role_permissions_permission', 'permission_id', 'permissions', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('user_roles', 'fk_user_roles_user', 'user_id', 'users', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('user_roles', 'fk_user_roles_role', 'role_id', 'roles', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('refresh_tokens', 'fk_refresh_tokens_user', 'user_id', 'users', 'id', 'CASCADE');
CALL add_foreign_key_if_missing('password_reset_tokens', 'fk_password_reset_tokens_user', 'user_id', 'users', 'id', 'CASCADE');

DROP PROCEDURE IF EXISTS convert_table_collation;
DROP PROCEDURE IF EXISTS add_foreign_key_if_missing;
