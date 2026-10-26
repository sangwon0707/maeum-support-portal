-- 만료 수거가 사전 결과 저장 행의 보조 인덱스를 먼저 잠그지 않도록 실제 작업만 좁혀 찾는다.
CREATE INDEX IF NOT EXISTS ix_intake_live_expiry ON document_intake(execution_mode,status,lease_until,id);
