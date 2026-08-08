ALTER TABLE studies ADD COLUMN upload_session_id CHAR(36) NULL;
CREATE INDEX idx_studies_upload_session ON studies (upload_session_id);
