-- Patient IDs are server-assigned and the database enforces owner-scoped records.
-- Remove any single-column unique index that Hibernate may have recreated from
-- an older entity mapping, while preserving the composite ownership constraint.
DELIMITER $$
CREATE PROCEDURE drop_legacy_patient_id_unique_index()
BEGIN
    DECLARE legacy_index VARCHAR(255);

    SELECT index_name INTO legacy_index
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'patients'
      AND non_unique = 0
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING COUNT(*) = 1 AND MAX(column_name) = 'hospital_patient_id'
    LIMIT 1;

    IF legacy_index IS NOT NULL THEN
        SET @statement = CONCAT('ALTER TABLE patients DROP INDEX `', legacy_index, '`');
        PREPARE statement_to_run FROM @statement;
        EXECUTE statement_to_run;
        DEALLOCATE PREPARE statement_to_run;
    END IF;
END$$
DELIMITER ;

CALL drop_legacy_patient_id_unique_index();
DROP PROCEDURE drop_legacy_patient_id_unique_index;

-- The sequence stores the last allocated number; the service increments it
-- under a row lock. Synchronize it with legacy PT-xxxxx records before use.
UPDATE patient_id_sequence
SET next_value = GREATEST(
    next_value,
    (SELECT COALESCE(MAX(CASE
        WHEN hospital_patient_id REGEXP '^PT-[0-9]+$'
            THEN CAST(SUBSTRING(hospital_patient_id, 4) AS UNSIGNED)
        ELSE 0
    END), 0) FROM patients)
)
WHERE id = 1;

DELIMITER $$
CREATE PROCEDURE add_patient_owner_active_index()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'patients'
          AND index_name = 'idx_patients_owner_active'
    ) THEN
        CREATE INDEX idx_patients_owner_active ON patients (created_by, deleted);
    END IF;
END$$
DELIMITER ;

CALL add_patient_owner_active_index();
DROP PROCEDURE add_patient_owner_active_index;
