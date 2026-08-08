-- Display patient IDs are unique within each clinician account. The UUID remains
-- the global primary key and all display IDs are allocated by the backend.
CREATE TABLE IF NOT EXISTS patient_id_sequences (
    created_by VARCHAR(255) NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO patient_id_sequences (created_by, next_value)
SELECT created_by,
       COALESCE(MAX(CASE
           WHEN hospital_patient_id REGEXP '^PT-[0-9]+$'
               THEN CAST(SUBSTRING(hospital_patient_id, 4) AS UNSIGNED)
           ELSE 0
       END), 0)
FROM patients
WHERE created_by IS NOT NULL
GROUP BY created_by
ON DUPLICATE KEY UPDATE next_value = GREATEST(next_value, VALUES(next_value));
