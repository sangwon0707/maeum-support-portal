CREATE TABLE document_intake (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_id BIGINT NOT NULL,
  created_by BIGINT NOT NULL,
  layout VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
  stage VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
  detail VARCHAR(160) NOT NULL DEFAULT '분석 대기',
  progress INT NOT NULL DEFAULT 0,
  result_json LONGTEXT,
  error_code VARCHAR(80),
  claim_token CHAR(36),
  lease_until DATETIME(6),
  created_at DATETIME(6) NOT NULL DEFAULT UTC_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT UTC_TIMESTAMP(6),
  FOREIGN KEY(application_id) REFERENCES application(id),
  FOREIGN KEY(created_by) REFERENCES app_user(id),
  CHECK(layout IN ('BUNDLE','SEPARATE')),
  CHECK(status IN ('QUEUED','PROCESSING','READY','APPLIED','FAILED')),
  CHECK(progress BETWEEN 0 AND 100),
  INDEX ix_intake_application(application_id,id),
  INDEX ix_intake_queue(status,id)
);
CREATE TABLE intake_source (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  intake_id BIGINT NOT NULL,
  kind VARCHAR(30) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(80) NOT NULL,
  mime_type VARCHAR(60) NOT NULL,
  size_bytes BIGINT NOT NULL,
  FOREIGN KEY(intake_id) REFERENCES document_intake(id),
  CHECK(size_bytes BETWEEN 1 AND 10485760)
);
