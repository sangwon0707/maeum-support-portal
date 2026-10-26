-- 동일 내용 파일을 여러 신청에서 참조한다. 권한은 파일명이 아니라 신청 소유권으로 검사한다.
ALTER TABLE document DROP INDEX storage_key, ADD INDEX ix_document_storage_key(storage_key);
-- 기존 자료는 기능 도입 직후 삭제하지 않고 다음 초기화 시점까지 보존한다.
ALTER TABLE application ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT UTC_TIMESTAMP(6);
CREATE INDEX ix_application_cleanup ON application(owner_id,created_at,id);
CREATE TABLE intake_analysis_cache (
  workspace_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  request_hash CHAR(64) NOT NULL,
  pipeline_version VARCHAR(100) NOT NULL,
  payload LONGTEXT NOT NULL,
  created_at DATETIME(6) NOT NULL DEFAULT UTC_TIMESTAMP(6),
  PRIMARY KEY(workspace_id,owner_id,request_hash,pipeline_version),
  FOREIGN KEY(owner_id) REFERENCES app_user(id),
  FOREIGN KEY(workspace_id) REFERENCES workspace(id)
);
