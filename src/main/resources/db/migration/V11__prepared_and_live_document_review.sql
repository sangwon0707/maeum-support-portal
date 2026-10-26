-- 준비된 결과를 불러오는 진행 표시와 이번 요청의 실제 인식 실행을 구분한다.
ALTER TABLE document_intake
  ADD COLUMN execution_mode VARCHAR(20) NOT NULL DEFAULT 'LIVE',
  ADD COLUMN replay_started_at DATETIME(6),
  ADD CONSTRAINT chk_intake_execution CHECK(execution_mode IN ('LIVE','PREPARED'));
