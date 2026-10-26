ALTER TABLE document_intake
  ADD COLUMN request_key CHAR(36) NULL,
  ADD COLUMN request_hash CHAR(64) NULL,
  ADD UNIQUE KEY uq_intake_request(application_id, request_key);
