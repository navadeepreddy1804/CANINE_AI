CREATE TABLE IF NOT EXISTS patient_id_sequence (
    id BIGINT NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO patient_id_sequence (id, next_value)
VALUES (1, 1)
ON DUPLICATE KEY UPDATE id = id;
