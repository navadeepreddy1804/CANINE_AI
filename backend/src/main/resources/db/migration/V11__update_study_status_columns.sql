-- Convert strict ENUM status and state columns to VARCHAR(50) to accommodate extended enum states such as ANALYSIS_RUNNING and PREVIEW_READY
ALTER TABLE studies MODIFY COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE upload_sessions MODIFY COLUMN status VARCHAR(50) NOT NULL;
ALTER TABLE study_storage_records MODIFY COLUMN upload_status VARCHAR(50) NOT NULL;
ALTER TABLE ai_jobs MODIFY COLUMN state VARCHAR(50) NOT NULL;
ALTER TABLE ai_jobs MODIFY COLUMN task_type VARCHAR(50) NOT NULL;
